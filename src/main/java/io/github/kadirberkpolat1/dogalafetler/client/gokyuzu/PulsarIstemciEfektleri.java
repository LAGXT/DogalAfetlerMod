package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KaraDelikYutmaEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarEgrileri;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Pulsar döneminin istemci efektleri (pulsar-2, PL-2/PL-4): kamera çevresinde göğe açık yerlere kül yağışı (yalnız
 * PULSAR ve blok hasarı açıkken), ufukta titreyen ısı dalgası şeridi (gök aşaması: arazi önüne geçer), güçle kısalan
 * görüş mesafesi ve kara delik parlamasının ekran örtüsü ile ışık haritası (sis rengi {@link GokyuzuRenkleri}'nde).
 * Durum yalnız {@link IstemciDonem} ve {@link IstemciGokyuzu}'nden; yeni paket yok, ekrana yazı yok (N6).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class PulsarIstemciEfektleri {
    private static final ResourceLocation ISI_DALGASI =
            DogalAfetler.konum("textures/environment/gokyuzu/isi_dalgasi.png");
    private static final int KUL_YARICAP = 24;
    /** Son saniyenin (20 tick) tick başına kül parçacığı sayıları. */
    private static final int[] KUL_SAYAC = new int[20];
    private static int sayacIndis;
    private static float kulBirikimi;
    /** Işık haritasında parlamanın çektiği mor-beyaz. */
    private static final float[] PARLAMA_RENGI = {0.95f, 0.9f, 1f};

    private PulsarIstemciEfektleri() {
    }

    /** Son saniyede doğan kül yağışı parçacığı (otomasyon log'u). */
    public static int kulYagisiSayaci() {
        int toplam = 0;
        for (int s : KUL_SAYAC) {
            toplam += s;
        }
        return toplam;
    }

    /** Tam güçte tick başına kül parçacığı (kademe Düşük/Orta/Yüksek/Ultra). */
    static int kulTickBasina(EfektKalitesi k) {
        return switch (k) {
            case DUSUK -> 2;
            case ORTA -> 4;
            case YUKSEK -> 7;
            case ULTRA -> 10;
        };
    }

    static int isiDilimi(EfektKalitesi k) {
        return switch (k) {
            case DUSUK -> 8;
            case ORTA -> 12;
            case YUKSEK -> 16;
            case ULTRA -> 24;
        };
    }

    private static boolean pulsarda(ClientLevel level, DonemPaketi d) {
        return level != null && level.dimension() == Level.OVERWORLD && d.gunesEvresi() == GunesEvresi.PULSAR;
    }

    private static float guc(float pt) {
        return PulsarEgrileri.yogunluk(IstemciGokyuzu.gorunum(pt).ilerleme());
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        if (olay.phase != TickEvent.Phase.END || mc.level == null || mc.isPaused()) {
            return;
        }
        sayacIndis = (sayacIndis + 1) % KUL_SAYAC.length;
        KUL_SAYAC[sayacIndis] = 0;
        DonemPaketi d = IstemciDonem.durum();
        if (!pulsarda(mc.level, d) || !d.blokHasari()) {
            kulBirikimi = 0f;
            return;
        }
        kulBirikimi += kulTickBasina(IstemciAyarlari.efektKalitesi()) * guc(0f);
        int adet = (int) kulBirikimi;
        kulBirikimi -= adet;
        Vec3 kamera = mc.gameRenderer.getMainCamera().getPosition();
        RandomSource r = mc.level.random;
        for (int i = 0; i < adet; i++) {
            double aci = r.nextDouble() * Math.PI * 2;
            double uzak = Math.sqrt(r.nextDouble()) * KUL_YARICAP;
            double x = kamera.x + Math.cos(aci) * uzak;
            double z = kamera.z + Math.sin(aci) * uzak;
            double y = kamera.y + 4.0 + r.nextDouble() * 14.0;
            if (mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)) > y) {
                continue;
            }
            mc.level.addParticle(PulsarKayitlari.KUL_YAGISI.get(), x, y, z, r.nextGaussian() * 0.01,
                    -0.05 - r.nextDouble() * 0.04, r.nextGaussian() * 0.01);
            KUL_SAYAC[sayacIndis]++;
        }
    }

    @SubscribeEvent
    public static void asama(RenderLevelStageEvent olay) {
        if (olay.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            isiSeridi(olay);
        } else if (olay.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            parlamaOrtusu(olay.getPartialTick());
        }
    }

    /**
     * Ufkun 0–6° bandında kamera çevresinde halka: yatayda kayan, üst kenarı titreyen sıcak şerit (şadersiz serap; tek çizim,
     * kademeye göre 8/12/16/24 dilim).
     */
    private static void isiSeridi(RenderLevelStageEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        float pt = olay.getPartialTick();
        if (!pulsarda(level, IstemciDonem.durum()) || olay.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float gun = GokyuzuRenkleri.gunIsigi(level.getTimeOfDay(pt));
        float alfa = 0.35f * guc(pt) * (0.6f + 0.4f * gun) * (1f - 0.6f * level.getRainLevel(pt));
        if (alfa <= 0.003f) {
            return;
        }
        float t = (level.getGameTime() % 72000L + pt) / 20f;
        int n = isiDilimi(IstemciAyarlari.efektKalitesi());
        Matrix4f m = olay.getPoseStack().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, ISI_DALGASI);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float kayma = t * 0.03f;
        for (int j = 0; j < n; j++) {
            for (int k = 0; k < 4; k++) {
                int s = k == 1 || k == 2 ? j + 1 : j;
                boolean ust = k >= 2;
                float az = (float) s / n * Mth.TWO_PI;
                float el = ust ? 6f * (0.82f + 0.18f * Mth.sin(t * 3.1f + s * 1.7f)) : -0.5f;
                float er = el * Mth.DEG_TO_RAD;
                float yatay = 100f * Mth.cos(er);
                b.vertex(m, Mth.sin(az) * yatay, 100f * Mth.sin(er), -Mth.cos(az) * yatay)
                        .uv((float) s / n * 4f + kayma, ust ? 0f : 1f).color(1f, 0.5f, 0.25f, alfa).endVertex();
            }
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /** Kara delik parlaması: dünya çizimi bitince tam ekran mor-beyaz örtü (G2-4 kalıbı; GUI katmanı değil). */
    private static void parlamaOrtusu(float pt) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }
        float guc = 0.85f * parlama(IstemciGokyuzu.gorunum(pt));
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
        tampon.vertex(-1, -1, 0).color(0.93f, 0.86f, 1f, guc).endVertex();
        tampon.vertex(1, -1, 0).color(0.93f, 0.86f, 1f, guc).endVertex();
        tampon.vertex(1, 1, 0).color(0.93f, 0.86f, 1f, guc).endVertex();
        tampon.vertex(-1, 1, 0).color(0.93f, 0.86f, 1f, guc).endVertex();
        BufferUploader.drawWithShader(tampon.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        gorus.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    /** Pulsar döneminde görüş mesafesi güçle yarıya, sis başlangıcı yakına çekilir (Forge yalnız iptal edilen olayı uygular). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sis(ViewportEvent.RenderFog olay) {
        if (olay.getMode() != FogRenderer.FogMode.FOG_TERRAIN || olay.getCamera().getFluidInCamera() != FogType.NONE
                || !pulsarda(Minecraft.getInstance().level, IstemciDonem.durum())) {
            return;
        }
        float g = guc((float) olay.getPartialTick());
        if (g <= 0f) {
            return;
        }
        olay.scaleFarPlaneDistance(1f - 0.5f * g);
        olay.scaleNearPlaneDistance(1f - 0.8f * g);
        olay.setCanceled(true);
    }

    private static float parlama(IstemciGokyuzu.Gorunum d) {
        return d.evre() == GunesEvresi.KARADELIK_YUTMA ? KaraDelikYutmaEgrileri.parlama(d.saniye()) : 0f;
    }

    /**
     * Kara delik parlaması ışık haritasının her hanesini mor-beyaza çeker (yalnız aydınlatır; ışık seviyeleri ve spawn
     * değişmez). Gökyüzü sarmalayıcısının {@code adjustLightmapColors}'ı çağırır.
     */
    static void isikHaritasi(IstemciGokyuzu.Gorunum d, Vector3f renk) {
        float t = 0.85f * parlama(d);
        if (t > 0.002f) {
            renk.set(Math.max(renk.x, Mth.lerp(t, renk.x, PARLAMA_RENGI[0])),
                    Math.max(renk.y, Mth.lerp(t, renk.y, PARLAMA_RENGI[1])),
                    Math.max(renk.z, Mth.lerp(t, renk.z, PARLAMA_RENGI[2])));
        }
    }
}
