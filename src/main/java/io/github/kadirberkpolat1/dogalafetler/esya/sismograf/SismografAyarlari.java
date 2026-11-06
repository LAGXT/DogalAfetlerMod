package io.github.kadirberkpolat1.dogalafetler.esya.sismograf;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** SERVER config {@code esya.sismograf} (madde 13b; N17 §C radar-sismograf). */
public final class SismografAyarlari {
    private static AyarKaydi.Bolum<SismografAyarlari> bolum;

    private final ForgeConfigSpec.IntValue menzil;
    private final ForgeConfigSpec.IntValue hassasMenzil;
    private final ForgeConfigSpec.IntValue merkezHatasi;
    private final ForgeConfigSpec.IntValue hassasMerkezHatasi;
    private final ForgeConfigSpec.IntValue titremeSiddeti;
    private final ForgeConfigSpec.IntValue paketAraligiTick;

    private SismografAyarlari(ForgeConfigSpec.Builder k) {
        k.comment("Sismograf (madde 13b)").push("sismograf");
        menzil = k.comment("Sismografın algılama menzili (blok; afet merkezine yatay mesafe).").defineInRange("menzil", 300, 16,
                4096);
        hassasMenzil = k.comment("Hassas Sismografın algılama menzili (blok).").defineInRange("hassasMenzil", 800, 16, 8192);
        merkezHatasi = k.comment("Sismografın gösterdiği merkezin gerçek merkezden en büyük sapması (blok).")
                .defineInRange("merkezHatasi", 40, 0, 512);
        hassasMerkezHatasi = k.comment("Hassas Sismografın merkez sapması (blok).").defineInRange("hassasMerkezHatasi", 6, 0,
                512);
        titremeSiddeti = k.comment("Öncü sarsıntıdaki kamera titremesinin şiddeti (%; 100 = en çok 0,6°). İstemci ayrıca"
                + " kapatabilir.").defineInRange("titremeSiddeti", 50, 0, 100);
        paketAraligiTick = k.comment("Sismograf durumunun denetlenme aralığı (tick); paket yalnız taşıyana ve durum değişince.")
                .defineInRange("paketAraligiTick", 20, 20, 200);
        k.pop();
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("esya", "Özel eşyalar (madde 13)", SismografAyarlari::new);
    }

    public static int menzil(boolean hassas) {
        return hassas ? bolum.get().hassasMenzil.get() : bolum.get().menzil.get();
    }

    public static int merkezHatasi(boolean hassas) {
        return hassas ? bolum.get().hassasMerkezHatasi.get() : bolum.get().merkezHatasi.get();
    }

    /** 0–1. */
    public static float titreme() {
        return bolum.get().titremeSiddeti.get() / 100f;
    }

    public static int paketAraligiTick() {
        return bolum.get().paketAraligiTick.get();
    }
}
