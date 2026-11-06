package io.github.kadirberkpolat1.dogalafetler.afet.deprem;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** Deprem ve sinkhole CLIENT config bölümü ({@code deprem}): erişilebilirlik için ekran sarsıntısı çarpanı. */
public final class DepremIstemciAyarlari {
    private static AyarKaydi.Bolum<DepremIstemciAyarlari> bolum;

    private final ForgeConfigSpec.IntValue sarsintiCarpani;

    private DepremIstemciAyarlari(ForgeConfigSpec.Builder k) {
        sarsintiCarpani = k.comment("Deprem ve sinkhole ekran sarsıntısı çarpanı (yüzde; 0 kapalı)")
                .defineInRange("sarsintiCarpani", 100, 0, 200);
    }

    public static void kaydet() {
        bolum = AyarKaydi.istemciBolumu("deprem", "Deprem ve sinkhole (istemci)", DepremIstemciAyarlari::new);
    }

    /** 0–2. */
    public static double sarsintiCarpani() {
        return bolum.get().sarsintiCarpani.get() / 100.0;
    }
}
