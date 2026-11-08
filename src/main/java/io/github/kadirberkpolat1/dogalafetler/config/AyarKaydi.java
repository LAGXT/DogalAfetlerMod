package io.github.kadirberkpolat1.dogalafetler.config;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ConfigFileTypeHandler;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Config kayıt kancası (K0-08). Tek SERVER spec ({@value #SUNUCU_DOSYASI}, dünya başına, istemciye senkron) ve tek CLIENT
 * spec ({@value #ISTEMCI_DOSYASI}) vardır; alt sistemler kendi bölümlerini <b>yalnız {@code DogalAfetler} kurucusundan
 * çağrılan açık bir {@code kaydet()} metoduyla</b>, koşulsuz ekler (statik başlatıcı ya da {@code static final} alanı kullanılmaz:
 * sınıf spec kurulduktan sonra yüklenirse ekleme reddedilir):
 *
 * <pre>{@code
 * public final class SicaklikAyarlari {
 *     private static AyarKaydi.Bolum<SicaklikAyarlari> bolum;
 *     private final ForgeConfigSpec.IntValue esik;
 *     private SicaklikAyarlari(ForgeConfigSpec.Builder k) { esik = k.comment("...").defineInRange("esik", 10, 0, 50); }
 *     public static void kaydet() { bolum = AyarKaydi.sunucuBolumu("sicaklik", "Sıcaklık (madde 7)", SicaklikAyarlari::new); }
 *     public static int esik() { return bolum.get().esik.get(); }
 * }
 * // DogalAfetler kurucusunda, AyarKaydi.kaydet(baglam) satırından önce: SicaklikAyarlari.kaydet();
 * }</pre>
 *
 * Kurucu, bölüm adıyla {@code push} edilmiş builder'ı alır ve ittiği alt bölümleri kendisi kapatır. Aynı bölüme birden
 * çok katkı eklenebilir (anahtarlar farklı olmalı). Spec'ler {@link #kaydet} ile mod kurucusunun sonunda kurulur; ondan
 * sonra eklenen bölüm reddedilir. Değerler config yüklendikten sonra (sunucu açılışı) okunur. Yeni dosyalar varsayılanlarla
 * yazılır ({@link VarsayilanYazanIsleyici}); var olan SERVER dosyasının eski varsayılanları {@link #sunucuGocu} ile
 * kaydedilen göçlerle Forge okumadan önce yenisine çekilir ({@link AyarGocu}).
 */
public final class AyarKaydi {
    public static final String SUNUCU_DOSYASI = "dogalafetler-server.toml";
    public static final String ISTEMCI_DOSYASI = "dogalafetler-client.toml";

    /** Spec kurulunca dolan bir config bölümü. */
    public static final class Bolum<T> implements Supplier<T> {
        private final String ad;
        private volatile T deger;

        private Bolum(String ad) {
            this.ad = ad;
        }

        @Override
        public T get() {
            T sonuc = deger;
            if (sonuc == null) {
                throw new IllegalStateException("Config bölümü henüz kurulmadı: " + ad);
            }
            return sonuc;
        }

        @SuppressWarnings("unchecked")
        private void ayarla(Object yeni) {
            deger = (T) Objects.requireNonNull(yeni, "config bölümü kurucusu null döndü: " + ad);
        }
    }

    private record Katki(String bolum, String aciklama, Function<ForgeConfigSpec.Builder, ?> kurucu, Bolum<?> hedef) {
    }

    private static final List<Katki> SUNUCU = new ArrayList<>();
    private static final List<Katki> ISTEMCI = new ArrayList<>();
    private static final List<SurumluGocPlani> SUNUCU_GOCLERI = new ArrayList<>();
    private static volatile List<SurumluGocPlani> sunucuGocleri = List.of();
    private static boolean kuruldu;

    private AyarKaydi() {
    }

    /** SERVER config'e bir bölüm ekler (oynanış değerleri). */
    public static synchronized <T> Bolum<T> sunucuBolumu(String ad, String aciklama,
            Function<ForgeConfigSpec.Builder, T> kurucu) {
        return ekle(SUNUCU, ad, aciklama, kurucu);
    }

    /** CLIENT config'e bir bölüm ekler (görsel, ses, HUD). */
    public static synchronized <T> Bolum<T> istemciBolumu(String ad, String aciklama,
            Function<ForgeConfigSpec.Builder, T> kurucu) {
        return ekle(ISTEMCI, ad, aciklama, kurucu);
    }

    /**
     * SERVER config'e bir bölümün göç tablosunu ekler: var olan dosyada {@code plan.surumYolu()} {@code plan.guncel()}'den
     * küçükse (anahtar yoksa 0), Forge dosyayı okumadan önce planın değerleri yazılır ve sürüm güncel olur. Sürüm anahtarı
     * bölümün spec'inde varsayılanı {@code plan.guncel()} olarak tanımlanır: yeni dosya güncel sürümle yazılır, göç
     * ettirilmez.
     */
    public static synchronized void sunucuGocu(SurumluGocPlani plan) {
        Objects.requireNonNull(plan, "plan");
        if (kuruldu) {
            throw new IllegalStateException("Config spec'leri kuruldu; göç mod kurucusunda eklenmeli: " + plan.surumYolu());
        }
        SUNUCU_GOCLERI.add(plan);
    }

    /**
     * Var olan SERVER config dosyasını kayıtlı göçlerle günceller (dosya işleyicisi Forge'dan önce çağırır; GameTest geçici
     * dosyayla). Değişiklik olduysa tek satırlık özeti, olmadıysa null döndürür.
     */
    public static String sunucuDosyasiniGocEt(Path dosya) {
        return AyarGocu.uygula(dosya, sunucuGocleri);
    }

    private static <T> Bolum<T> ekle(List<Katki> liste, String ad, String aciklama,
            Function<ForgeConfigSpec.Builder, T> kurucu) {
        if (kuruldu) {
            throw new IllegalStateException("Config spec'leri kuruldu; bölüm mod kurucusunda eklenmeli: " + ad);
        }
        Objects.requireNonNull(kurucu, "kurucu");
        if (ad == null || ad.isBlank() || !ad.equals(ad.toLowerCase(Locale.ROOT)) || ad.contains(".")) {
            throw new IllegalArgumentException("Bölüm adı küçük harfli, noktasız olmalı: " + ad);
        }
        Bolum<T> bolum = new Bolum<>(ad);
        liste.add(new Katki(ad, aciklama, kurucu, bolum));
        return bolum;
    }

    /** Spec'leri kurar ve Forge'a kaydeder; mod kurucusunun sonunda bir kez çağrılır. */
    public static synchronized void kaydet(ModLoadingContext baglam) {
        if (kuruldu) {
            throw new IllegalStateException("Config spec'leri zaten kuruldu");
        }
        kuruldu = true;
        sunucuGocleri = List.copyOf(SUNUCU_GOCLERI);
        ModContainer kapsayici = baglam.getContainer();
        kapsayici.addConfig(new VarsayilanliConfig(ModConfig.Type.SERVER, kur(SUNUCU), kapsayici, SUNUCU_DOSYASI));
        kapsayici.addConfig(new VarsayilanliConfig(ModConfig.Type.CLIENT, kur(ISTEMCI), kapsayici, ISTEMCI_DOSYASI));
    }

    /** Forge'un {@code registerConfig}'iyle aynı kayıt; yalnız dosya işleyicisi yeni dosyayı varsayılanlarla yazar. */
    private static final class VarsayilanliConfig extends ModConfig {
        private VarsayilanliConfig(Type tur, ForgeConfigSpec spec, ModContainer kapsayici, String dosya) {
            super(tur, spec, kapsayici, dosya);
        }

        @Override
        public ConfigFileTypeHandler getHandler() {
            return VarsayilanYazanIsleyici.ORNEK;
        }
    }

    private static ForgeConfigSpec kur(List<Katki> katkilar) {
        Map<String, List<Katki>> gruplar = new LinkedHashMap<>();
        for (Katki katki : katkilar) {
            gruplar.computeIfAbsent(katki.bolum(), anahtar -> new ArrayList<>()).add(katki);
        }
        ForgeConfigSpec.Builder kurucu = new ForgeConfigSpec.Builder();
        for (Map.Entry<String, List<Katki>> grup : gruplar.entrySet()) {
            String aciklama = grup.getValue().stream().map(Katki::aciklama).filter(Objects::nonNull).findFirst()
                    .orElse(null);
            if (aciklama != null) {
                kurucu.comment(aciklama);
            }
            kurucu.push(grup.getKey());
            for (Katki katki : grup.getValue()) {
                katki.hedef().ayarla(katki.kurucu().apply(kurucu));
            }
            kurucu.pop();
        }
        return kurucu.build();
    }
}
