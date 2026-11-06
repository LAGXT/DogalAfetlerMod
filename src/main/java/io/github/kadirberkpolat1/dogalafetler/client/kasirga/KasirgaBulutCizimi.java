package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarligi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Kasırganın yarı saydam bulut katmanlarını opak dünya, varlıklar, yarı saydam bloklar ve parçacıklar çizildikten sonra
 * ({@code AFTER_PARTICLES}, spike SK-11) tek tampona yazar ve bir çizim çağrısıyla boşaltır. Bulut derinlik yazmaz: varlık
 * geçişinde çizilseydi, geçişin sonunda boşaltılan blok tamponları (uçan bloklar, enkaz kopyaları) huninin arkasında kalsa
 * bile üstüne binerdi.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class KasirgaBulutCizimi {
    private KasirgaBulutCizimi() {
    }

    @SubscribeEvent
    public static void asama(RenderLevelStageEvent olay) {
        if (olay.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || IstemciKasirga.KASIRGALAR.isEmpty()
                || IstemciKasirga.gizli) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dagitici = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource tampon = mc.renderBuffers().bufferSource();
        PoseStack poz = olay.getPoseStack();
        Vec3 kamera = olay.getCamera().getPosition();
        float kismi = olay.getPartialTick();
        boolean cizildi = false;
        for (KasirgaVarligi k : IstemciKasirga.KASIRGALAR.values()) {
            if (k.isRemoved() || !k.shouldRender(kamera.x, kamera.y, kamera.z)
                    || !olay.getFrustum().isVisible(k.getBoundingBoxForCulling())
                    || !(dagitici.getRenderer(k) instanceof KasirgaRenderer renderer)) {
                continue;
            }
            double x = Mth.lerp(kismi, k.xOld, k.getX());
            double y = Mth.lerp(kismi, k.yOld, k.getY());
            double z = Mth.lerp(kismi, k.zOld, k.getZ());
            poz.pushPose();
            poz.translate(x - kamera.x, y - kamera.y, z - kamera.z);
            renderer.bulut(k, x, y, z, kismi, poz, tampon, dagitici.getPackedLightCoords(k, kismi));
            poz.popPose();
            cizildi = true;
        }
        if (cizildi) {
            tampon.endBatch(KasirgaRenderTipi.BULUT);
        }
    }
}
