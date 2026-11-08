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
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
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
 * görüş mesafesi, kara delik parlamasının ekran örtüsü ile ışık haritası, yutumdan sonra ufukta kor ve kenarlarda kırağı
 * vinyeti, pulsar yolundaki buzulda yakın tipi sisi (sis rengi {@link GokyuzuRenkleri}'nde).
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
    /** Kenarlardan içeri büyüyen buz kristalleri (sıcaklık ekran efektinin dokusu; yeni doku yok). */
    private static final ResourceLocation KIRAGI = DogalAfetler.konum("textures/misc/sicaklik_buz.png");
    /** Kırağı vinyetinin donma %100'deki gücü (pulsar yolundaki buzul). */
    static final float KIRAGI_TAM = 0.9f;
    /** Buzul tipisinde görüş mesafesi ve sis başlangıcı çarpanları (yakın, beyaz sis). */
    private static final float BUZUL_SIS_UZAK = 0.45f;
    private static final float BUZUL_SIS_YAKIN = 0.1f;
    /** Ufuk şeridinin rengi: pulsarda sıcak serap, yutumdan sonra külün korları. */
    private static final float[] ISI_RENGI = {1f, 0.5f, 0.25f};
    private static final float[] KOR_RENGI = {1f, 0.2f, 0.06f};

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

    /** Pulsar yolundaki buzul kıyamet (GÜNEŞSİZ, eski güneş dönecek). */
    private static boolean buzulda(ClientLevel level, DonemPaketi d) {
        return level != null && level.dimension() == Level.OVERWORLD && d.gunesEvresi() == GunesEvresi.GUNESSIZ
                && d.donusYolu();
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
            ekranOrtuleri(olay.getPartialTick());
        }
    }

    /**
     * Ufkun 0–6° bandında kamera çevresinde halka: yatayda kayan, üst kenarı titreyen şerit; pulsarda sıcak serap, yutumdan
     * sonra karanlık ufukta külün kızıl korları (şadersiz; tek çizim, kademeye göre 8/12/16/24 dilim).
     */
    private static void isiSeridi(RenderLevelStageEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        float pt = olay.getPartialTick();
        if (level == null || level.dimension() != Level.OVERWORLD || olay.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        GunesEvresi evre = IstemciDonem.durum().gunesEvresi();
        float alfa;
        float[] renk;
        if (evre == GunesEvresi.PULSAR) {
            float gun = GokyuzuRenkleri.gunIsigi(level.getTimeOfDay(pt));
            alfa = 0.35f * guc(pt) * (0.6f + 0.4f * gun) * (1f - 0.6f * level.getRainLevel(pt));
            renk = ISI_RENGI;
        } else if (evre == GunesEvresi.KARADELIK_YUTMA) {
            alfa = 0.5f * KaraDelikYutmaEgrileri.korUfku(IstemciGokyuzu.gorunum(pt).saniye());
            renk = KOR_RENGI;
        } else {
            return;
        }
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
                        .uv((float) s / n * 4f + kayma, ust ? 0f : 1f).color(renk[0], renk[1], renk[2], alfa).endVertex();
            }
        }
        BufferUploader.drawWithShader(b.end());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
    }

    /**
     * Dünya çizimi bitince tam ekran örtüler (G2-4 kalıbı; GUI katmanı değil, GUI gizliyken de çizilir): önce kenarlardan
     * kırağı vinyeti, sonra kara delik parlamasının karışımlı (toplamalı değil) mor-beyaz örtüsü.
     */
    private static void ekranOrtuleri(float pt) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null || level.dimension() != Level.OVERWORLD) {
            return;
        }
        IstemciGokyuzu.Gorunum g = IstemciGokyuzu.gorunum(pt);
        float kiragi = kiragi(IstemciDonem.durum(), g);
        float ortu = g.evre() == GunesEvresi.KARADELIK_YUTMA ? KaraDelikYutmaEgrileri.ekranOrtusu(g.saniye()) : 0f;
        if (kiragi > 0.004f) {
            tamEkran(KIRAGI, 1f, 1f, 1f, kiragi);
        }
        if (ortu > 0.004f) {
            tamEkran(null, 0.93f, 0.86f, 1f, ortu);
        }
    }

    /**
     * Kırağı vinyeti 0–{@value #KIRAGI_TAM}: yutumdan sonra ilk soğuk, pulsar yolundaki buzulda donmayla tam, kızıl cüce
     * doğuşunun ilk %15'inde söner. Normal süpernova yolunda yok.
     */
    static float kiragi(DonemPaketi d, IstemciGokyuzu.Gorunum g) {
        float donma = Mth.clamp(d.donmaIlerlemesi() / 100f, 0f, 1f);
        return switch (g.evre()) {
            case KARADELIK_YUTMA -> KaraDelikYutmaEgrileri.kiragi(g.saniye());
            case GUNESSIZ -> g.pulsarYolu() ? Mth.lerp(donma, KaraDelikYutmaEgrileri.KIRAGI_ILK, KIRAGI_TAM) : 0f;
            case KIZIL_CUCE -> g.pulsarYolu()
                    ? KIRAGI_TAM * donma * (1f - GokyuzuEgrileri.yumusak(0f, 0.15f, g.ilerleme())) : 0f;
            default -> 0f;
        };
    }

    /** Tam ekran kare, normal karışım: {@code doku} null ise düz renk, değilse doku ekranı kaplar. */
    private static void tamEkran(ResourceLocation doku, float r, float g, float b, float a) {
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
        BufferBuilder tampon = Tesselator.getInstance().getBuilder();
        if (doku == null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            tampon.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            tampon.vertex(-1, -1, 0).color(r, g, b, a).endVertex();
            tampon.vertex(1, -1, 0).color(r, g, b, a).endVertex();
            tampon.vertex(1, 1, 0).color(r, g, b, a).endVertex();
            tampon.vertex(-1, 1, 0).color(r, g, b, a).endVertex();
        } else {
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, doku);
            tampon.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            tampon.vertex(-1, -1, 0).uv(0f, 1f).color(r, g, b, a).endVertex();
            tampon.vertex(1, -1, 0).uv(1f, 1f).color(r, g, b, a).endVertex();
            tampon.vertex(1, 1, 0).uv(1f, 0f).color(r, g, b, a).endVertex();
            tampon.vertex(-1, 1, 0).uv(0f, 0f).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(tampon.end());
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        gorus.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
    }

    /**
     * Pulsar döneminde görüş mesafesi güçle yarıya, sis başlangıcı yakına çekilir; pulsar yolundaki buzulda tipi sisi yakın
     * (Forge yalnız iptal edilen olayı uygular).
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sis(ViewportEvent.RenderFog olay) {
        if (olay.getMode() != FogRenderer.FogMode.FOG_TERRAIN || olay.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        DonemPaketi d = IstemciDonem.durum();
        if (pulsarda(level, d)) {
            float g = guc((float) olay.getPartialTick());
            if (g <= 0f) {
                return;
            }
            olay.scaleFarPlaneDistance(1f - 0.5f * g);
            olay.scaleNearPlaneDistance(1f - 0.8f * g);
        } else if (buzulda(level, d)) {
            olay.scaleFarPlaneDistance(BUZUL_SIS_UZAK);
            olay.scaleNearPlaneDistance(BUZUL_SIS_YAKIN);
        } else {
            return;
        }
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
