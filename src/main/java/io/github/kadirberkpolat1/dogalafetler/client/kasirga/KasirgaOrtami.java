package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarligi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Ortam (tasarım notu §2 katman 6 ve "Kamera"): kasırga huni kenarına 64 bloktan yakınken sis yeşilimsi griye
 * ({@code #5E6B5A}) kayar ve görüş %60'a iner; 40 blok içinde kamera 6,3 + 8,7 Hz gürültüyle seviye × ±0,4° yuvarlanır;
 * huniye bakarken görüş açısı en çok 4° daralır. Mesafe huni taban kenarına göre; siren evresinde (huni yok) etki yarıdır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class KasirgaOrtami {
    private static final float SIS_R = 0x5E / 255f;
    private static final float SIS_G = 0x6B / 255f;
    private static final float SIS_B = 0x5A / 255f;

    private KasirgaOrtami() {
    }

    /** En yakın kasırga: {huni kenarına yatay mesafe, seviye, evre gücü, varlık kimliği}; yoksa null. */
    private static double[] enYakin(Vec3 kamera) {
        double[] sonuc = null;
        for (KasirgaVarligi k : IstemciKasirga.KASIRGALAR.values()) {
            double d = Math.max(0, Math.hypot(kamera.x - k.getX(), kamera.z - k.getZ()) - k.seviyeBilgisi().tabanYaricapi());
            if (sonuc == null || d < sonuc[0]) {
                double guc = switch (k.evre()) {
                    case OLUSUM -> 0.5 * k.evreIlerleme();
                    case DOGUS -> 0.5 + 0.5 * k.evreIlerleme();
                    case OLGUN -> 1;
                    case IP -> 1 - 0.6 * k.evreIlerleme();
                    case DAGILMA -> 0.4 * (1 - k.evreIlerleme());
                };
                sonuc = new double[] {d, k.seviye(), guc, k.getId()};
            }
        }
        return sonuc;
    }

    private static float sisEtkisi(Camera kamera) {
        if (IstemciKasirga.KASIRGALAR.isEmpty() || IstemciKasirga.yalnizTozGizli || !KasirgaAyarlari.sisEtkisi()) {
            return 0;
        }
        double[] y = enYakin(kamera.getPosition());
        if (y == null) {
            return 0;
        }
        float f = (float) Mth.clamp(1 - y[0] / 64, 0, 1);
        return (float) (f * f * (3 - 2 * f) * y[2]);
    }

    @SubscribeEvent
    public static void sisRengi(ViewportEvent.ComputeFogColor olay) {
        float f = sisEtkisi(olay.getCamera());
        if (f > 0) {
            olay.setRed(Mth.lerp(f, olay.getRed(), SIS_R));
            olay.setGreen(Mth.lerp(f, olay.getGreen(), SIS_G));
            olay.setBlue(Mth.lerp(f, olay.getBlue(), SIS_B));
        }
    }

    /**
     * Forge iptal edilmeyen {@code RenderFog}'daki mesafe değişikliğini uygulamaz (spike SK-04). Yalnız kara sisi ve sıvı dışında
     * (K-E86): eskiden her mod (gök sisi dahil) iptal edilip ölçekleniyordu; diğer afet sisleri de aynı korumayı kullanır.
     */
    @SubscribeEvent
    public static void sis(ViewportEvent.RenderFog olay) {
        if (olay.getMode() != FogRenderer.FogMode.FOG_TERRAIN || olay.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float f = sisEtkisi(olay.getCamera());
        if (f > 0) {
            olay.scaleFarPlaneDistance(1 - 0.4f * f);
            olay.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
        double olcek = KasirgaAyarlari.kameraSarsintisi();
        if (IstemciKasirga.KASIRGALAR.isEmpty() || olcek <= 0 || Minecraft.getInstance().level == null) {
            return;
        }
        double[] y = enYakin(olay.getCamera().getPosition());
        if (y == null || y[0] > 40) {
            return;
        }
        double genlik = 0.4 * y[1] * (1 - y[0] / 40) * y[2] * olcek;
        double t = (Minecraft.getInstance().level.getGameTime() + olay.getPartialTick()) / 20.0;
        double gurultu = 0.6 * Math.sin(Math.PI * 2 * 6.3 * t) + 0.4 * Math.sin(Math.PI * 2 * 8.7 * t + 1.3);
        olay.setRoll((float) (olay.getRoll() + genlik * gurultu));
    }

    @SubscribeEvent
    public static void gorusAcisi(ViewportEvent.ComputeFov olay) {
        if (IstemciKasirga.KASIRGALAR.isEmpty()) {
            return;
        }
        Camera kamera = olay.getCamera();
        Vec3 p = kamera.getPosition();
        double[] y = enYakin(p);
        if (y == null || y[0] > 64) {
            return;
        }
        KasirgaVarligi k = IstemciKasirga.KASIRGALAR.get((int) y[3]);
        if (k == null || k.evre() == KasirgaEvresi.OLUSUM) {
            return;
        }
        Vector3f bakis = kamera.getLookVector();
        double dx = k.getX() - p.x;
        double dz = k.getZ() - p.z;
        double uzunluk = Math.hypot(dx, dz) + 1e-4;
        double hiza = (bakis.x() * dx + bakis.z() * dz) / uzunluk;
        if (hiza > 0.7) {
            double f = (hiza - 0.7) / 0.3 * (1 - y[0] / 64) * y[2];
            olay.setFOV(olay.getFOV() - 4 * f);
        }
    }
}
