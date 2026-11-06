package io.github.kadirberkpolat1.dogalafetler.esya.radar;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** SERVER config {@code esya.radar} (madde 13a; N17 §C radar-sismograf). */
public final class RadarAyarlari {
    private static AyarKaydi.Bolum<RadarAyarlari> bolum;

    private final ForgeConfigSpec.IntValue menzil;
    private final ForgeConfigSpec.IntValue gelismisMenzil;
    private final ForgeConfigSpec.IntValue paketAraligiTick;

    private RadarAyarlari(ForgeConfigSpec.Builder k) {
        k.comment("Hava Radarı (madde 13a)").push("radar");
        menzil = k.comment("Hava Radarı'nın menzili (blok; afet merkezine yatay mesafe).").defineInRange("menzil", 300, 16, 4096);
        gelismisMenzil = k.comment("Gelişmiş Hava Radarı'nın menzili (blok).").defineInRange("gelismisMenzil", 800, 16, 8192);
        paketAraligiTick = k.comment("Radar durumunun denetlenme aralığı (tick). Paket yalnız radarı tutan oyuncuya ve durum"
                + " değişince gider; 20 = saniyede en çok bir.").defineInRange("paketAraligiTick", 20, 20, 200);
        k.pop();
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("esya", "Özel eşyalar (madde 13)", RadarAyarlari::new);
    }

    public static int menzil(boolean gelismis) {
        return gelismis ? bolum.get().gelismisMenzil.get() : bolum.get().menzil.get();
    }

    public static int paketAraligiTick() {
        return bolum.get().paketAraligiTick.get();
    }
}
