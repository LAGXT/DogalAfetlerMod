package io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarligi;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Ortam (tasarım notu §2 katman 6 ve "Kamera"): kasırga huni kenarına 64 bloktan yakınken sis yeşilimsi griye
 * ({@code #5E6B5A}) kayar ve sis mesafesi %60'a iner; 40 blok içinde kamera 6-9 Hz'lik gürültüyle seviye × ±0,4°
 * yuvarlanır. Mesafe huni taban kenarına göre ölçülür (büyük kasırganın etkisi daha uzaktan başlar).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class KasirgaOrtami {
    private static final float SIS_R = 0x5E / 255f;
    private static final float SIS_G = 0x6B / 255f;
    private static final float SIS_B = 0x5A / 255f;

    private KasirgaOrtami() {
    }

    /** En yakın kasırganın huni kenarına yatay mesafe ve seviyesi; kasırga yoksa null. */
    private static double[] enYakin() {
        if (KasirgaIstemciDurumu.KASIRGALAR.isEmpty()) {
            return null;
        }
        Vec3 kamera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double[] sonuc = null;
        for (KasirgaVarligi k : KasirgaIstemciDurumu.KASIRGALAR.values()) {
            double d = Math.hypot(kamera.x - k.getX(), kamera.z - k.getZ()) - k.seviyeBilgisi().tabanYaricapi();
            if (sonuc == null || d < sonuc[0]) {
                sonuc = new double[] {Math.max(0, d), k.seviye()};
            }
        }
        return sonuc;
    }

    private static float sisEtkisi() {
        double[] y = enYakin();
        if (y == null) {
            return 0;
        }
        float f = (float) Mth.clamp(1 - y[0] / 64, 0, 1);
        return f * f * (3 - 2 * f);
    }

    @SubscribeEvent
    public static void sisRengi(ViewportEvent.ComputeFogColor olay) {
        float f = sisEtkisi();
        if (f > 0) {
            olay.setRed(Mth.lerp(f, olay.getRed(), SIS_R));
            olay.setGreen(Mth.lerp(f, olay.getGreen(), SIS_G));
            olay.setBlue(Mth.lerp(f, olay.getBlue(), SIS_B));
        }
    }

    @SubscribeEvent
    public static void sis(ViewportEvent.RenderFog olay) {
        float f = sisEtkisi();
        if (f > 0) {
            olay.scaleFarPlaneDistance(1 - 0.4f * f);
            olay.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
        double[] y = enYakin();
        if (y == null || y[0] > 40) {
            return;
        }
        double genlik = 0.4 * y[1] * (1 - y[0] / 40);
        double t = (Minecraft.getInstance().level.getGameTime() + olay.getPartialTick()) / 20.0;
        double gurultu = 0.6 * Math.sin(Math.PI * 2 * 6.3 * t) + 0.4 * Math.sin(Math.PI * 2 * 8.7 * t + 1.3);
        olay.setRoll((float) (olay.getRoll() + genlik * gurultu));
    }
}
