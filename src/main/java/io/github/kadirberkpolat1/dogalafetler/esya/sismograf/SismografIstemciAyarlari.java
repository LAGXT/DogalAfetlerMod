package io.github.kadirberkpolat1.dogalafetler.esya.sismograf;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** CLIENT config {@code esya.sismograf}: titreme, ses ve grafik ekranı (madde 13b). Sunucuda da kaydedilir, okunmaz. */
public final class SismografIstemciAyarlari {
    private static AyarKaydi.Bolum<SismografIstemciAyarlari> bolum;

    private final ForgeConfigSpec.BooleanValue titreme;
    private final ForgeConfigSpec.BooleanValue ses;
    private final ForgeConfigSpec.BooleanValue grafik;

    private SismografIstemciAyarlari(ForgeConfigSpec.Builder k) {
        k.comment("Sismograf (madde 13b)").push("sismograf");
        titreme = k.comment("Öncü sarsıntıda çok hafif kamera titremesi.").define("titreme", true);
        ses = k.comment("Öncü sarsıntı sesi (kısık zemin gürlemesi).").define("ses", true);
        grafik = k.comment("Sağ tıkla açılan 60 saniyelik sismik dalga grafiği.").define("grafik", true);
        k.pop();
    }

    public static void kaydet() {
        bolum = AyarKaydi.istemciBolumu("esya", "Özel eşyaların göstergeleri (madde 13)", SismografIstemciAyarlari::new);
    }

    public static boolean titreme() {
        return bolum.get().titreme.get();
    }

    public static boolean ses() {
        return bolum.get().ses.get();
    }

    public static boolean grafik() {
        return bolum.get().grafik.get();
    }
}
