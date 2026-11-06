package io.github.kadirberkpolat1.dogalafetler.client.asit_yagmuru;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Asit yağmurunun yağış çizimi (tasarım notu §2 "Yağmur"): vanilla {@code renderSnowAndRain} ile aynı sütun geometrisi
 * (kameraya dönük dikey dörtgen, sütun başına tohumlu kayma ve hız), kendi dokumuz {@code asit_yagmuru.png} (sarımsı yeşil,
 * vanilla'dan %15 sık damla) ve %20 kalın damla. Yalnız asit alanındaki sütunlar çizilir; alfa alan kenarında 16 blokta
 * söner. Gerçek hava durumu değişmez. Yağmur yarıçapı efekt kalitesiyle 5 / 10 / 10 / 15 (maliyet vanilla ile aynı).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class AsitYagmuruCizici {
    private static final ResourceLocation DOKU = DogalAfetler.konum("textures/environment/asit_yagmuru.png");
    private static final double KALINLIK = 1.2;

    private AsitYagmuruCizici() {
    }

    @SubscribeEvent
    public static void ciz(RenderLevelStageEvent olay) {
        if (olay.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        double yogunluk = IstemciAsitYagmuru.yogunluk();
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (yogunluk <= 0.01 || level == null) {
            return;
        }
        Vec3 kam = olay.getCamera().getPosition();
        int r = IstemciAsitYagmuru.kademe().yagmurYaricapi;
        if (IstemciAsitYagmuru.alanAgirligi(kam.x, kam.z) <= 0 && !kenardaMi(kam, r)) {
            return;
        }
        float pt = olay.getPartialTick();
        int tick = olay.getRenderTick();
        LightTexture isik = mc.gameRenderer.lightTexture();
        isik.turnOnLightLayer();
        PoseStack gorus = RenderSystem.getModelViewStack();
        gorus.pushPose();
        gorus.setIdentity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShader(GameRenderer::getParticleShader);
        RenderSystem.setShaderTexture(0, DOKU);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        Matrix4f m = olay.getPoseStack().last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        int ix = Mth.floor(kam.x);
        int iy = Mth.floor(kam.y);
        int iz = Mth.floor(kam.z);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int dortgen = 0;
        for (int cz = iz - r; cz <= iz + r; cz++) {
            for (int cx = ix - r; cx <= ix + r; cx++) {
                double alan = IstemciAsitYagmuru.alanAgirligi(cx + 0.5, cz + 0.5);
                if (alan <= 0) {
                    continue;
                }
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
                int alt = Math.max(iy - r, h);
                int ust = Math.max(iy + r, h);
                if (alt == ust) {
                    continue;
                }
                double dx = cx + 0.5 - kam.x;
                double dz = cz + 0.5 - kam.z;
                double uzaklik = Math.sqrt(dx * dx + dz * dz);
                float oran = (float) (uzaklik / r);
                float alfa = (float) (((1 - oran * oran) * 0.5 + 0.5) * yogunluk * alan);
                if (alfa <= 0.01f) {
                    continue;
                }
                int tohum = cx * cx * 3121 + cx * 45238971 ^ cz * cz * 418711 + cz * 13761;
                RandomSource rs = RandomSource.create(tohum);
                int evre = tick + cx * cx * 3121 + cx * 45238971 + cz * cz * 418711 + cz * 13761 & 31;
                float kayma = -((float) evre + pt) / 32.0F * (3.0F + rs.nextFloat());
                double sx = uzaklik < 1e-3 ? 0.5 * KALINLIK : -dz / uzaklik * 0.5 * KALINLIK;
                double sz = uzaklik < 1e-3 ? 0 : dx / uzaklik * 0.5 * KALINLIK;
                p.set(cx, Math.max(h, iy), cz);
                int isikRengi = LevelRenderer.getLightColor(level, p);
                float x0 = (float) (dx - sx);
                float x1 = (float) (dx + sx);
                float z0 = (float) (dz - sz);
                float z1 = (float) (dz + sz);
                float yUst = (float) (ust - kam.y);
                float yAlt = (float) (alt - kam.y);
                float vUst = alt * 0.25F + kayma;
                float vAlt = ust * 0.25F + kayma;
                bb.vertex(m, x0, yUst, z0).uv(0f, vUst).color(1f, 1f, 1f, alfa).uv2(isikRengi).endVertex();
                bb.vertex(m, x1, yUst, z1).uv(1f, vUst).color(1f, 1f, 1f, alfa).uv2(isikRengi).endVertex();
                bb.vertex(m, x1, yAlt, z1).uv(1f, vAlt).color(1f, 1f, 1f, alfa).uv2(isikRengi).endVertex();
                bb.vertex(m, x0, yAlt, z0).uv(0f, vAlt).color(1f, 1f, 1f, alfa).uv2(isikRengi).endVertex();
                dortgen++;
            }
        }
        if (dortgen > 0) {
            BufferUploader.drawWithShader(bb.end());
        } else {
            bb.end().release();
        }
        gorus.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        isik.turnOffLightLayer();
    }

    /** Kamera alanın dışında ama kenara yağmur yarıçapı kadar yakınsa dışarıdan da perde görünür. */
    private static boolean kenardaMi(Vec3 kam, int r) {
        return IstemciAsitYagmuru.alanAgirligi(kam.x + r, kam.z) > 0 || IstemciAsitYagmuru.alanAgirligi(kam.x - r, kam.z) > 0
                || IstemciAsitYagmuru.alanAgirligi(kam.x, kam.z + r) > 0 || IstemciAsitYagmuru.alanAgirligi(kam.x, kam.z - r) > 0;
    }
}
