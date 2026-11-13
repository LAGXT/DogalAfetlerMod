package io.github.kadirberkpolat1.dogalafetler.afet.sinkhole;

import io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeAyarGocu;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

/** Sinkhole SERVER config bölümü ({@code sinkhole}; madde 10 "çukurun çapı ve derinliği artar", N17). */
public final class SinkholeAyarlari {
    private static AyarKaydi.Bolum<SinkholeAyarlari> bolum;

    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> cap;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> derinlik;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> catlamaSaniye;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> cokusSaniye;
    private final ForgeConfigSpec.ConfigValue<List<? extends Integer>> basamak;
    private final ForgeConfigSpec.ConfigValue<List<? extends Double>> hasar;
    private final ForgeConfigSpec.IntValue blokButcesi;
    private final ForgeConfigSpec.IntValue dusenBlokTavani;

    private SinkholeAyarlari(ForgeConfigSpec.Builder k) {
        k.comment("Config göçünün sürümü (K-E61): dosya bundan eskiyse açılışta eski varsayılanında kalan sinkhole seviye listeleri "
                + "yeni varsayılana çekilir, elle değiştirilenlere dokunulmaz. Elle değiştirmeyin.")
                .defineInRange("ayarSurumu", SinkholeAyarGocu.GUNCEL, 0, Integer.MAX_VALUE);
        cap = DepremAyarlari.tamsayilar(k, "cap",
                "Çukur çapı (blok), S1…S5", 6, 10, 16, 28, 48);
        derinlik = DepremAyarlari.tamsayilar(k, "derinlik",
                "Merkez derinliği (yüzeyden blok; en alt Y -50), S1…S5", 6, 10, 16, 28, 46);
        catlamaSaniye = DepremAyarlari.tamsayilar(k,
                "catlamaSuresi", "Çökmeden önceki titreme ve çatlama (saniye; kaçma fırsatı), S1…S5", 4, 4, 5, 6, 8);
        cokusSaniye = DepremAyarlari.tamsayilar(k, "cokusSuresi",
                "Çekirdek çöküşü + basamaklı geri çekilme (saniye), S1…S5", 3, 4, 6, 9, 13);
        basamak = DepremAyarlari.tamsayilar(k, "basamak",
                "Basamak (halka) sayısı, S1…S5", 1, 1, 2, 3, 4);
        hasar = DepremAyarlari.ondaliklar(k, "dusenBlokHasari",
                "Çöken halkadaki canlılara bir kez hasar (can puanı; düşme hasarı ayrıca), S1…S5", 0.0, 0.0, 4.0, 7.0, 12.0);
        blokButcesi = k.comment("Tick başına en çok blok kaldırma (TPS düşünce yarıya/çeyreğe iner)")
                .defineInRange("blokButcesi", 400, 16, 4000);
        dusenBlokTavani = k.comment("Aynı anda havadaki düşen blok (FallingBlockEntity) tavanı; aşılınca blok kaldırılır")
                .defineInRange("dusenBlokTavani", 60, 0, 400);
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("sinkhole", "Sinkhole (bölüm F, madde 10)", SinkholeAyarlari::new);
        AyarKaydi.sunucuGocu(SinkholeAyarGocu.PLAN);
    }

    public static SinkholeSeviyesi seviye(int seviye) {
        SinkholeAyarlari a = bolum.get();
        SinkholeSeviyesi v = SinkholeSeviyesi.varsayilan(seviye);
        int i = v.seviye() - 1;
        return new SinkholeSeviyesi(v.seviye(), Math.max(3, al(a.cap, i, v.cap())), Math.max(2, al(a.derinlik, i, v.derinlik())),
                al(a.catlamaSaniye, i, v.catlamaTick() / 20) * 20, Math.max(1, al(a.cokusSaniye, i, v.cokusTick() / 20)) * 20,
                Math.max(1, al(a.basamak, i, v.basamak())), (float) al(a.hasar, i, (double) v.dusenBlokHasari()));
    }

    public static int blokButcesi() {
        return bolum.get().blokButcesi.get();
    }

    public static int dusenBlokTavani() {
        return bolum.get().dusenBlokTavani.get();
    }

    private static int al(ForgeConfigSpec.ConfigValue<List<? extends Integer>> d, int i, int varsayilan) {
        List<? extends Integer> l = d.get();
        return i < l.size() ? l.get(i) : varsayilan;
    }

    private static double al(ForgeConfigSpec.ConfigValue<List<? extends Double>> d, int i, double varsayilan) {
        List<? extends Double> l = d.get();
        return i < l.size() ? l.get(i) : varsayilan;
    }
}
