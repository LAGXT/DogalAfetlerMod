package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru;

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
        sunucu = AyarKaydi.sunucuBolumu("asit_yagmuru", "Asit yağmuru (bölüm E, madde 10)", Sunucu::new);
    }

    /** Seviye tablosu; beş öğeli olmayan liste varsayılana düşer. */
    public static AsitKurallari.Tablo tablo() {
        Sunucu s = sunucu.get();
        AsitKurallari.Tablo v = AsitKurallari.Tablo.VARSAYILAN;
        return new AsitKurallari.Tablo(liste(s.yaricap, v.yaricap(), Number::intValue),
                liste(s.sureSaniye, v.sureSaniye(), Number::intValue), liste(s.asindirma, v.asindirma(), Number::doubleValue),
                liste(s.hasar, v.hasar(), Number::doubleValue), liste(s.hasarAraligi, v.hasarAraligi(), Number::intValue),
                s.tasSeviyesi.get());
    }

    public static float minimumCan() {
        return sunucu.get().minimumCan.get().floatValue();
    }

    public static int eszamanliAsindirma() {
        return sunucu.get().eszamanliAsindirma.get();
    }

    public static double kaymaHizi() {
        return sunucu.get().kaymaHizi.get();
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
        private final ForgeConfigSpec.ConfigValue<List<? extends Number>> hasar;
        private final ForgeConfigSpec.ConfigValue<List<? extends Number>> hasarAraligi;
        private final ForgeConfigSpec.IntValue tasSeviyesi;
        private final ForgeConfigSpec.DoubleValue minimumCan;
        private final ForgeConfigSpec.IntValue eszamanliAsindirma;
        private final ForgeConfigSpec.DoubleValue kaymaHizi;

        private Sunucu(ForgeConfigSpec.Builder k) {
            AsitKurallari.Tablo v = AsitKurallari.Tablo.VARSAYILAN;
            yaricap = liste(k, "yaricap", "Yağış alanı yarıçapı (blok).", v.yaricap(), 4, 256);
            sureSaniye = liste(k, "sureSaniye", "AKTIF yağış süresi (sn).", v.sureSaniye(), 10, 3600);
            asindirma = liste(k, "asindirmaBlokSaniye", "Tam yoğunlukta saniyede aşınan en çok blok.", v.asindirma(), 0,
                    256);
            hasar = liste(k, "hasar", "Gökyüzüne açık oyuncuya her aralıkta verilen hasar (can puanı; 1 = yarım kalp).",
                    v.hasar(), 0, 20);
            hasarAraligi = liste(k, "hasarAraligiTick", "Açıkta kalan oyuncunun iki hasarı arası (tick).", v.hasarAraligi(),
                    4, 1200);
            tasSeviyesi = k.comment("Kırıktaş ve taş tuğlanın aşınmaya başladığı seviye (6: hiç).")
                    .defineInRange("tasSeviyesi", v.tasSeviyesi(), 1, 6);
            minimumCan = k.comment("Asit hasarı canı bunun altına indirmez (madde 10: Seviye 5'te bile öldürücü değil).")
                    .defineInRange("asitMinimumCan", 4.0, 0.0, 20.0);
            eszamanliAsindirma = k.comment("Aynı anda çatlak örtüsü taşıyan (aşınmakta olan) en çok blok; S5 hızı × ortalama çatlak süresi (4 sn).")
                    .defineInRange("eszamanliAsindirma", 64, 1, 256);
            kaymaHizi = k.comment("Yağış alanının günün rüzgârı yönünde kayma hızı (blok/sn).")
                    .defineInRange("kaymaHizi", 0.3, 0.0, 5.0);
        }
    }
}
