package io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini;

import io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik.OrmanKurallari;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * SERVER {@code orman_yangini} bölümü (madde 10, N17). Yayılma hızı, tavan, bütçe ve yağmur sönmesi ortak yangının
 * {@code yangin.*} ayarlarındadır; burada yalnız afetin seviye tablosu ve koreografisi durur.
 */
public final class OrmanAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;

    private OrmanAyarlari() {
    }

    static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("orman_yangini", "Orman yangını afeti (bölüm G, madde 10)", Sunucu::new);
    }

    /** Seviye tablosu; beş öğeli olmayan liste varsayılana düşer. */
    public static OrmanKurallari.Tablo tablo() {
        Sunucu s = sunucu.get();
        OrmanKurallari.Tablo v = OrmanKurallari.Tablo.VARSAYILAN;
        return new OrmanKurallari.Tablo(liste(s.yaricap, v.yaricap(), Number::intValue),
                liste(s.yogunluk, v.yogunluk(), Number::doubleValue), liste(s.sureSaniye, v.sureSaniye(), Number::intValue));
    }

    public static int aramaEnAz() {
        return sunucu.get().aramaEnAz.get();
    }

    public static int aramaEnCok() {
        return Math.max(aramaEnAz(), sunucu.get().aramaEnCok.get());
    }

    public static int yanikIziButcesi() {
        return sunucu.get().yanikIziButcesi.get();
    }

    public static int erkenBitisSaniye() {
        return sunucu.get().erkenBitisSaniye.get();
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
        private final ForgeConfigSpec.ConfigValue<List<? extends Number>> yogunluk;
        private final ForgeConfigSpec.ConfigValue<List<? extends Number>> sureSaniye;
        private final ForgeConfigSpec.IntValue aramaEnAz;
        private final ForgeConfigSpec.IntValue aramaEnCok;
        private final ForgeConfigSpec.IntValue yanikIziButcesi;
        private final ForgeConfigSpec.IntValue erkenBitisSaniye;

        private Sunucu(ForgeConfigSpec.Builder k) {
            OrmanKurallari.Tablo v = OrmanKurallari.Tablo.VARSAYILAN;
            yaricap = liste(k, "baslangicYaricapi", "Başlangıç alanı yarıçapı (blok, ortak yangın en çok 24).", v.yaricap(),
                    1, 24);
            yogunluk = liste(k, "yogunluk",
                    "Yangın yoğunluğu 0–1: yayılma çarpanı 0,5 + 1,3·y, kıvılcım sıçraması round(12·y) blok, alan tutuşma oranı.",
                    v.yogunluk(), 0, 1);
            sureSaniye = liste(k, "sureSaniye", "Yeni tutuşmanın sürdüğü AKTIF süre (sn).", v.sureSaniye(), 10, 3600);
            aramaEnAz = k.comment("Başlangıç alanı hedef oyuncudan en az bu uzaklıkta aranır (blok).")
                    .defineInRange("aramaEnAz", 40, 0, 256);
            aramaEnCok = k.comment("Başlangıç alanı hedef oyuncudan en çok bu uzaklıkta aranır (blok).")
                    .defineInRange("aramaEnCok", 90, 0, 256);
            yanikIziButcesi = k.comment("Yanık izi için tick başına yoklanan sütun (TPS kısmasıyla çarpılır; 0 kapalı).")
                    .defineInRange("yanikIziButcesi", 8, 0, 64);
            erkenBitisSaniye = k.comment("Alanda bu kadar saniye yanan hücre kalmazsa afet doğal biter (0: yalnız süreyle).")
                    .defineInRange("erkenBitisSaniye", 15, 0, 600);
        }
    }
}
