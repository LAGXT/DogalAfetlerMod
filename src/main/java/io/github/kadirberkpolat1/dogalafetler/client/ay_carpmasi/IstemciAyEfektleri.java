package io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.AyCarpmasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Ay çarpmasının ekran efektleri (bölüm H "çarpma anında tüm oyuncuların ekranı sallansın"): çarpma saatinden
 * hesaplanan 0,5 sn %70 beyaz parlama (Overworld), 3–15 sn kamera sarsıntısı (her boyutta, mesafeden bağımsız), son
 * yaklaşmada ve parlamada sisin turuncuya boyanması; halka döneminde kayan yıldız zamanlayıcısı. Ekrana yazı yoktur (N6).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciAyEfektleri {
    private static long saat;

    private IstemciAyEfektleri() {
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.isPaused() || mc.level == null) {
            return;
        }
        saat++;
        AyCarpmasiGokyuzu.tick(mc.level.random);
    }

    /** Mesafeden bağımsız sarsıntı (deprem S4 genliği, 2,5°): yaw/pitch/roll düzensiz titreşimi. */
    @SubscribeEvent
    public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
        IstemciAy.Durum d = IstemciAy.durum((float) olay.getPartialTick());
        if (!d.carpti()) {
            return;
        }
        float a = AyEgrileri.SARSINTI_DERECE * AyEgrileri.sarsinti(d.saniye())
                * (float) IstemciAy.ayar(AyCarpmasiAyarlari::ekranSarsintisiOlcegi);
        if (a <= 0f) {
            return;
        }
        double z = saat + olay.getPartialTick();
        olay.setYaw(olay.getYaw() + a * (float) (Math.sin(z * 1.7) * 0.55 + Math.sin(z * 3.9 + 1) * 0.45));
        olay.setPitch(olay.getPitch() + a * (float) (Math.sin(z * 2.1 + 2) * 0.55 + Math.sin(z * 4.3) * 0.45));
        olay.setRoll(olay.getRoll() + a * 0.6f * (float) Math.sin(z * 2.7 + 0.5));
    }

    /** Çarpma parlaması: dünya çizimi bitince tam ekran sıcak beyaz örtü (GUI katmanı değil, yazı yok). */
    @SubscribeEvent
    public static void parlama(RenderLevelStageEvent olay) {
        if (olay.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        IstemciAy.Durum d = IstemciAy.durum(olay.getPartialTick());
        if (level == null || level.dimension() != Level.OVERWORLD || !d.carpti()) {
            return;
        }
        float guc = AyEgrileri.parlama(d.saniye()) * (float) IstemciAy.ayar(AyCarpmasiAyarlari::parlamaOlcegi);
        if (guc <= 0.004f) {
            return;
        }
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f(), VertexSorting.ORTHOGRAPHIC_Z);
        PoseStack gorus = RenderSystem.getModelViewStack();
        gorus.pushPose();
        gorus.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder tampon = Tesselator.getInstance().getBuilder();
        tampon.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        tampon.vertex(-1, -1, 0).color(1f, 0.97f, 0.93f, guc).endVertex();
        tampon.vertex(1, -1, 0).color(1f, 0.97f, 0.93f, guc).endVertex();
        tampon.vertex(1, 1, 0).color(1f, 0.97f, 0.93f, guc).endVertex();
        tampon.vertex(-1, 1, 0).color(1f, 0.97f, 0.93f, guc).endVertex();
        BufferUploader.drawWithShader(tampon.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        gorus.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    /** Son yaklaşmada sis alttan turuncuya, parlamada sıcak beyaza çekilir (gökyüzü sis renginden sonra). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sis(ViewportEvent.ComputeFogColor olay) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.dimension() != Level.OVERWORLD
                || olay.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        IstemciAy.Durum d = IstemciAy.durum((float) olay.getPartialTick());
        float k;
        float r;
        float g;
        float b;
        if (d.yaklasiyor()) {
            k = 0.55f * AyEgrileri.kizarma(d.saniye());
            r = 1f;
            g = 0.45f;
            b = 0.2f;
        } else if (d.carpti()) {
            k = 0.45f * AyEgrileri.ufukIsigi(d.saniye());
            r = 1f;
            g = 0.8f;
            b = 0.6f;
        } else {
            return;
        }
        if (k <= 0.002f) {
            return;
        }
        olay.setRed(Mth.lerp(k, olay.getRed(), r));
        olay.setGreen(Mth.lerp(k, olay.getGreen(), g));
        olay.setBlue(Mth.lerp(k, olay.getBlue(), b));
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        AyCarpmasiGokyuzu.sifirla();
    }
}
