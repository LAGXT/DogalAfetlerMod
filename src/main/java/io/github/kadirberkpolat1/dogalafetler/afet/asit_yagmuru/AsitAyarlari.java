package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru;

import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitAyarGocu;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraftforge.common.ForgeConfigSpec;

/** SERVER {@code asit_yagmuru} bölümü: seviye tablosu ve sınırlar (madde 10, N17). Listeler seviye 1–5 sırasındadır. */
public final class AsitAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;

    private AsitAyarlari() {
    }

    static void kaydet() {
        AyarKaydi.sunucuGocu(AsitAyarGocu.PLAN);
        sunucu = AyarKaydi.sunucuBolumu("asit_yagmuru", "Asit yağmuru (bölüm E, madde 10)", Sunucu::new);
    }

    /** Seviye tablosu; beş öğeli olmayan liste varsayılana düşer. */
    public static AsitKurallari.Tablo tablo() {
        Sunucu s = sunucu.get();
        AsitKurallari.Tablo v = AsitKurallari.Tablo.VARSAYILAN;
        return new AsitKurallari.Tablo(liste(s.yaricap, v.yaricap(), Number::intValue),
                liste(s.sureSaniye, v.sureSaniye(), Number::intValue), liste(s.asindirma, v.asindirma(), Number::doubleValue),
                s.tasSeviyesi.get());
    }

    /** Gökyüzüne açık oyuncunun saniyede aldığı hasar (can puanı; 1 = yarım kalp), seviyeden bağımsız. */
    public static float hasarSaniye() {
        return sunucu.get().hasarSaniye.get().floatValue();
    }

    public static float minimumCan() {
        return sunucu.get().minimumCan.get().floatValue();
    }

    public static int eszamanliAsindirma() {
        return sunucu.get().eszamanliAsindirma.get();
    }

    /** Tick başına oyuncuların çevresinden örneklenen en çok yüzey sütunu (TPS kısmasıyla çarpılır; AF2-05). */
    public static int blokButcesi() {
        return sunucu.get().blokButcesi.get();
    }

    private static <T> List<T> liste(ForgeConfigSpec.ConfigValue<List<? extends Number>> deger, List<T> varsayilan,
            Function<Number, T> cevir) {
        List<? extends Number> ham = deger.get();
        if (ham == null || ham.size() != varsayilan.size()) {
            return varsayilan;
        }
        List<T> sonuc = new ArrayList<>(ham.size());
        for (Number n : ham) {
            sonuc.add(cevir.apply(n));
        }
        return sonuc;
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends Number>> liste(ForgeConfigSpec.Builder k, String ad,
            String aciklama, List<? extends Number> varsayilan, double enAz, double enCok) {
        return k.comment(aciklama + " Seviye 1–5 sırasıyla beş değer.").defineList(ad, List.copyOf(varsayilan),
                o -> o instanceof Number n && n.doubleValue() >= enAz && n.doubleValue() <= enCok);
    }

    private static final class Sunucu {
        private final ForgeConfigSpec.ConfigValue<List<? extends Number>> yaricap;
        private final ForgeConfigSpec.ConfigValue<List<? extends Number>> sureSaniye;
        private final ForgeConfigSpec.ConfigValue<List<? extends Number>> asindirma;
        private final ForgeConfigSpec.DoubleValue hasarSaniye;
        private final ForgeConfigSpec.IntValue tasSeviyesi;
        private final ForgeConfigSpec.DoubleValue minimumCan;
        private final ForgeConfigSpec.IntValue eszamanliAsindirma;
        private final ForgeConfigSpec.IntValue blokButcesi;

        private Sunucu(ForgeConfigSpec.Builder k) {
            k.comment("Config göçünün sürümü (K-E61): dosya bundan eskiyse açılışta eski varsayılanında kalan asit seviye listeleri "
                    + "yeni varsayılana çekilir, elle değiştirilenlere dokunulmaz. Elle değiştirmeyin.")
                    .defineInRange("ayarSurumu", AsitAyarGocu.GUNCEL, 0, Integer.MAX_VALUE);
            AsitKurallari.Tablo v = AsitKurallari.Tablo.VARSAYILAN;
            yaricap = liste(k, "yaricap", "Yağmur bütün dünyada yağar; aşınma her oyuncunun bu yarıçapı içinden örneklenir (blok).",
                    v.yaricap(), 4, 256);
            sureSaniye = liste(k, "sureSaniye", "AKTIF yağış süresi (sn).", v.sureSaniye(), 10, 3600);
            asindirma = liste(k, "asindirmaBlokSaniye", "Tam yoğunlukta her oyuncunun çevresinde saniyede aşınan en çok blok.",
                    v.asindirma(), 0, 256);
            hasarSaniye = k.comment("Gökyüzüne açık oyuncunun maruz kaldığı her saniye aldığı hasar (can puanı; 1 = yarım"
                    + " kalp). S1–S3 bu değer, S4 ×1,25, S5 ×2 (AsitKurallari.hasarCarpani); çatı altında 0.").defineInRange("hasarSaniye", 1.0, 0.0, 20.0);
            tasSeviyesi = k.comment("Kırıktaş ve taş tuğlanın aşınmaya başladığı seviye (6: hiç).")
                    .defineInRange("tasSeviyesi", v.tasSeviyesi(), 1, 6);
            minimumCan = k.comment("Asit hasarı canı bunun altına indirmez (madde 10: Seviye 5'te bile öldürücü değil).")
                    .defineInRange("asitMinimumCan", 4.0, 0.0, 20.0);
            eszamanliAsindirma = k.comment("Aynı anda çatlak örtüsü taşıyan (aşınmakta olan) en çok blok; S5 hızı × ortalama çatlak süresi (4 sn).")
                    .defineInRange("eszamanliAsindirma", AsitAyarGocu.ESZAMANLI, 1, 256);
            blokButcesi = k.comment("Tick başına oyuncuların çevresindeki yüklü chunk'lardan örneklenen en çok yüzey sütunu"
                    + " (oyuncu başına 3–16, toplamı bu sayı × TPS kısması; 0: aşınma yok).")
                    .defineInRange("blokButcesi", 32, 0, 512);
        }
    }
}
