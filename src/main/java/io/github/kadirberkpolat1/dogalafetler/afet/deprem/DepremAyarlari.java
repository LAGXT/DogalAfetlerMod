package io.github.kadirberkpolat1.dogalafetler.afet.deprem;

import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.DepremSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import java.util.List;
import java.util.function.Predicate;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Deprem SERVER config bölümü ({@code deprem}; madde 10 "seviyenin ne değiştirdiğini config'ten ayarlanabilir yap", N17).
 * Seviye tabloları beşer değerli listedir (S1…S5).
 */
public final class DepremAyarlari {
    private static AyarKaydi.Bolum<DepremAyarlari> bolum;

    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> uzunluk;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> genislik;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> derinlik;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> sureSaniye;
    private final ForgeConfigSpec.ConfigValue<List<? extends Double>> kameraGenligi;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> yikimYaricapi;
    private final ForgeConfigSpec.ConfigValue<List<? extends Double>> hasar;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> blokButcesi;
    private final ForgeConfigSpec.IntValue dusenBlokTavani;
    private final ForgeConfigSpec.IntValue enAltY;
    private static volatile Integer testEnAltY;

    private DepremAyarlari(ForgeConfigSpec.Builder k) {
        uzunluk = tamsayilar(k, "yarikUzunlugu", "Yarık uzunluğu (blok), S1…S5", 24, 48, 96, 160, 256);
        genislik = tamsayilar(k, "yarikGenisligi", "Merkezdeki yarık genişliği (blok), S1…S5", 1, 2, 3, 4, 6);
        derinlik = tamsayilar(k, "yarikDerinligi", "Merkezdeki yarık derinliği (yüzeyden blok; en alt Y -50), S1…S5", 4, 8,
                14, 22, 32);
        sureSaniye = tamsayilar(k, "sarsintiSuresi", "Sarsıntı süresi (saniye), S1…S5", 8, 15, 25, 40, 60);
        kameraGenligi = ondaliklar(k, "kameraGenligi", "Yakındaki oyuncunun ekran titremesi (derece), S1…S5", 0.3, 0.8,
                1.5, 2.5, 4.0);
        yikimYaricapi = tamsayilar(k, "yikimYaricapi", "Sağlam olmayan blokların yıkıldığı yarıçap (blok), S1…S5", 16, 32,
                48, 72, 100);
        hasar = ondaliklar(k, "yakinHasar", "Yarığa 8 blok içindeki canlılara 2 sn'de bir hasar (can puanı), S1…S5", 0.0,
                0.0, 0.0, 1.0, 2.0);
        blokButcesi = tamsayilar(k, "blokButcesi", "Tick başına en çok blok değişimi (TPS düşünce yarıya/çeyreğe iner), S1…S5",
                32, 48, 64, 96, 128);
        dusenBlokTavani = k.comment("Aynı anda havadaki düşen blok (FallingBlockEntity) tavanı; aşılınca blok kaldırılır")
                .defineInRange("dusenBlokTavani", 60, 0, 400);
        enAltY = k.comment("Deprem yarığının ve sinkhole çukurunun inebileceği en alt Y (ana kaya ve deepslate tabanı korunur)")
                .defineInRange("enAltY", -50, -64, 320);
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("deprem", "Deprem (bölüm C, madde 10)", DepremAyarlari::new);
    }

    public static DepremSeviyesi seviye(int seviye) {
        DepremAyarlari a = bolum.get();
        DepremSeviyesi v = DepremSeviyesi.varsayilan(seviye);
        int i = v.seviye() - 1;
        return new DepremSeviyesi(v.seviye(), al(a.uzunluk, i, v.uzunluk()), al(a.genislik, i, v.genislik()),
                al(a.derinlik, i, v.derinlik()), al(a.sureSaniye, i, v.sureTick() / 20) * 20,
                al(a.kameraGenligi, i, v.kameraGenligi()), al(a.yikimYaricapi, i, v.yikimYaricapi()),
                (float) al(a.hasar, i, (double) v.hasar()));
    }

    public static int blokButcesi(int seviye) {
        return Math.max(1, al(bolum.get().blokButcesi, Math.max(1, Math.min(5, seviye)) - 1, 64));
    }

    /** Beş elemandan kısa listede eksik seviye varsayılana düşer. */
    static int al(ForgeConfigSpec.ConfigValue<List<? extends Integer>> d, int i, int varsayilan) {
        List<? extends Integer> l = d.get();
        return i < l.size() ? l.get(i) : varsayilan;
    }

    static double al(ForgeConfigSpec.ConfigValue<List<? extends Double>> d, int i, double varsayilan) {
        List<? extends Double> l = d.get();
        return i < l.size() ? l.get(i) : varsayilan;
    }

    public static int enAltY() {
        Integer test = testEnAltY;
        return test != null ? test : bolum.get().enAltY.get();
    }

    /**
     * Yalnız GameTest: GameTest dünyası y ≈ −60'tadır ve varsayılan tabanın (−50) altında kalır. {@code null} config'e
     * döner; sahneyi kuran test temizlikte geri alır (K-E15).
     */
    public static void geciciEnAltY(Integer y) {
        testEnAltY = y;
    }

    public static int dusenBlokTavani() {
        return bolum.get().dusenBlokTavani.get();
    }

    public static ForgeConfigSpec.ConfigValue<List<? extends Integer>> tamsayilar(ForgeConfigSpec.Builder k, String ad,
            String aciklama, Integer... varsayilan) {
        return k.comment(aciklama).defineList(ad, List.of(varsayilan), uygun(Integer.class));
    }

    public static ForgeConfigSpec.ConfigValue<List<? extends Double>> ondaliklar(ForgeConfigSpec.Builder k, String ad,
            String aciklama, Double... varsayilan) {
        return k.comment(aciklama).defineList(ad, List.of(varsayilan), uygun(Double.class));
    }

    /** Forge listeyi eleman eleman denetler: negatif olmayan sayı. */
    private static Predicate<Object> uygun(Class<?> tur) {
        return o -> tur.isInstance(o) && ((Number) o).doubleValue() >= 0;
    }
}
