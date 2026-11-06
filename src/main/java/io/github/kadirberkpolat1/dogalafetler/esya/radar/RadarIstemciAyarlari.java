package io.github.kadirberkpolat1.dogalafetler.esya.radar;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.GostergeYerlesimi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CLIENT config {@code esya.radar}: göstergenin görünürlüğü ve yeri. İstemciye özel API kullanmadığı için sunucuda da
 * kaydedilir; değerler yalnız istemcide okunur.
 */
public final class RadarIstemciAyarlari {
    private static AyarKaydi.Bolum<RadarIstemciAyarlari> bolum;

    private final ForgeConfigSpec.BooleanValue gostergeGorunur;
    private final ForgeConfigSpec.EnumValue<GostergeYerlesimi.Kose> kose;
    private final ForgeConfigSpec.IntValue xOfset;
    private final ForgeConfigSpec.IntValue yOfset;

    private RadarIstemciAyarlari(ForgeConfigSpec.Builder k) {
        k.comment("Hava Radarı göstergesi (madde 13a)").push("radar");
        gostergeGorunur = k.comment("Radar elde tutulunca gösterge çizilsin mi.").define("gostergeGorunur", true);
        kose = k.comment("Göstergenin oturduğu köşe. Alt köşelerde hotbar, kalpler ve sıcaklık barıyla çakışırsa"
                + " kendiliğinden onların üstüne kalkar.").defineEnum("gostergeKosesi", GostergeYerlesimi.Kose.SAG_ALT);
        xOfset = k.comment("Köşeden yatay içeri kaydırma (GUI pikseli).").defineInRange("gostergeXOfset", 0, -1000, 1000);
        yOfset = k.comment("Köşeden dikey içeri kaydırma (GUI pikseli).").defineInRange("gostergeYOfset", 0, -1000, 1000);
        k.pop();
    }

    public static void kaydet() {
        bolum = AyarKaydi.istemciBolumu("esya", "Özel eşyaların göstergeleri (madde 13)", RadarIstemciAyarlari::new);
    }

    public static boolean gostergeGorunur() {
        return bolum.get().gostergeGorunur.get();
    }

    public static GostergeYerlesimi.Kose kose() {
        return bolum.get().kose.get();
    }

    public static int xOfset() {
        return bolum.get().xOfset.get();
    }

    public static int yOfset() {
        return bolum.get().yOfset.get();
    }
}
