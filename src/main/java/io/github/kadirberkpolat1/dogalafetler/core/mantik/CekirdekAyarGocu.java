package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Çekirdek zamanlayıcı config anahtarlarının göç zinciri (K-E61, K-C2 rev. 2): var olan dünyanın SERVER config'inde eski
 * varsayılanda kalan bekleme yeni varsayılana çekilir; hiçbir eski varsayılana eşit olmayan (elle değiştirilmiş) değere
 * dokunulmaz. Dosyanın sürümü {@value #SURUM_YOLU}'ndadır, dosyada yoksa 0 sayılır. Sonraki bir varsayılan değişikliği yeni
 * sürümlü {@link Adim} olarak eklenir ve {@link #GUNCEL} artar. Dosya işlemi ortak {@code config.AyarGocu}'dadır.
 */
public final class CekirdekAyarGocu {
    public static final String SURUM_YOLU = "cekirdek.ayarSurumu";
    public static final int GUNCEL = 1;

    /** {@code surum}'de {@code yol}'un varsayılanı {@code eski}'den {@code yeni}'ye geçti. */
    public record Adim(int surum, String yol, long yeni, long eski) {
    }

    /** Sürüme göre sıralı. Sürüm 1: kullanıcı ilk afeti ilk oyun gününde görsün (AS-11). */
    public static final List<Adim> ADIMLAR = adimlar();

    private CekirdekAyarGocu() {
    }

    private static List<Adim> adimlar() {
        List<Adim> liste = new ArrayList<>();
        liste.add(new Adim(1, "cekirdek.zamanlayici.baslangicBeklemeTick", 6_000L, 24_000L));
        liste.add(new Adim(1, "cekirdek.zamanlayici.genelBeklemeTick", 6_000L, 12_000L));
        for (AfetTuru tur : AfetTuru.values()) {
            if (!tur.sunucuGeneli()) {
                liste.add(new Adim(1, "cekirdek.afetler." + tur.kimlik() + ".beklemeTick", 24_000L, 48_000L));
            }
        }
        return List.copyOf(liste);
    }

    /**
     * {@code surum}'den {@link #GUNCEL}'e geçerken değişecek anahtarlar, tablo sırasıyla: yol → yeni değer. {@code oku} dosyadaki
     * değeri verir (yoksa null: Forge eksik anahtarı zaten yeni varsayılanla doldurur).
     */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        Map<String, Number> degisen = new LinkedHashMap<>();
        for (Adim a : ADIMLAR) {
            if (a.surum() <= surum || a.surum() > GUNCEL) {
                continue;
            }
            Object deger = degisen.containsKey(a.yol()) ? degisen.get(a.yol()) : oku.apply(a.yol());
            if (deger instanceof Number n && n.doubleValue() == a.eski()) {
                degisen.put(a.yol(), a.yeni());
            }
        }
        return degisen;
    }
}
