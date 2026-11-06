package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Faz 2 kabulündeki ezilme denemesi (K0-26, spike G1): yalnız geliştirme ortamında ve otomasyon bayrağı
 * {@code ezilme}/{@code ezilme-yansimasiz} iken, bizim kaydımızdan SONRA ({@link EventPriority#LOWEST}) overworld için
 * kolay tanınan bir sahte efekt kaydeder; Forge {@code register} haritadaki değeri ezdiği için sonuç bizden sonra yüklenen
 * bir gökyüzü moduyla aynıdır. {@code ezilme-yansimasiz} 2. kademenin yansımasını bilerek bozar (3. kademe sınanır).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SahteGokyuzuModu {
    private SahteGokyuzuModu() {
    }

    /** 2. kademenin yansıyla yeniden kurduğu Forge alanı; yalnız {@code ezilme-yansimasiz} denemesinde bilerek yanlış. */
    static String efektAlaniAdi() {
        return GokyuzuOtomasyonu.mod().equals("ezilme-yansimasiz") ? "EFFECTS_BILEREK_YANLIS" : "EFFECTS";
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void kaydet(RegisterDimensionSpecialEffectsEvent olay) {
        if (!GokyuzuOtomasyonu.mod().startsWith("ezilme")) {
            return;
        }
        olay.register(BuiltinDimensionTypes.OVERWORLD_EFFECTS, new SahteEfekt());
        DogalAfetler.LOGGER.warn("[gokyuzu-sahte] sahte ikinci gökyüzü modu overworld efektini kaydetti (bizim kaydımızdan "
                + "sonra, EventPriority.LOWEST): {}", SahteEfekt.class.getName());
    }

    /**
     * Kolay tanınan gök: düz yeşil kubbe, macenta kare güneş, cam göbeği kare ay (vanilla yörüngesiyle döner). Vanilla
     * gökyüzünü hiç çizmez, tıpkı kendi göğünü çizen gerçek bir gökyüzü modu gibi.
     */
    public static final class SahteEfekt extends DimensionSpecialEffects.OverworldEffects {
        private boolean ilkCizim;

        @Override
        public boolean renderSky(ClientLevel level, int ticks, float pt, PoseStack poz, Camera kamera, Matrix4f izdusum,
                boolean sisli, Runnable sisKur) {
            sisKur.run();
            if (sisli) {
                return true;
            }
            if (!ilkCizim) {
                ilkCizim = true;
                DogalAfetler.LOGGER.info("[gokyuzu-sahte] sahte modun kendi gökyüzü çiziliyor");
            }
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            Matrix4f m = poz.last().pose();
            bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            bb.vertex(m, 0f, 16f, 0f).color(0.15f, 0.75f, 0.45f, 1f).endVertex();
            for (int i = -180; i <= 180; i += 45) {
                bb.vertex(m, 512f * Mth.cos(i * Mth.DEG_TO_RAD), 16f, 512f * Mth.sin(i * Mth.DEG_TO_RAD))
                        .color(0.15f, 0.75f, 0.45f, 1f).endVertex();
            }
            BufferUploader.drawWithShader(bb.end());

            poz.pushPose();
            poz.mulPose(Axis.YP.rotationDegrees(-90f));
            poz.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(pt) * 360f));
            Matrix4f g = poz.last().pose();
            kare(bb, g, 100f, 14f, 1f, 0.1f, 0.9f);
            kare(bb, g, -100f, 10f, 0.1f, 0.95f, 1f);
            poz.popPose();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            return true;
        }

        private static void kare(BufferBuilder bb, Matrix4f m, float y, float s, float r, float g, float b) {
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            bb.vertex(m, -s, y, -s).color(r, g, b, 1f).endVertex();
            bb.vertex(m, s, y, -s).color(r, g, b, 1f).endVertex();
            bb.vertex(m, s, y, s).color(r, g, b, 1f).endVertex();
            bb.vertex(m, -s, y, s).color(r, g, b, 1f).endVertex();
            BufferUploader.drawWithShader(bb.end());
        }
    }
}
