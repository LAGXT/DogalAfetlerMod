package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Ortak gökyüzü çizicisi (madde 16): bütün evreler ve ay var/yok kombinasyonları tek yerden çizilir. Sıra: kubbe →
 * gün doğumu yelpazesi → derin uzay (Samanyolu, galaksi, nebula, kalıntı, yıldız) → güneş evresi → ay → ufuk altı
 * karanlık disk. Yıldızlar ve şerit kalite ya da yıldız yoğunluğu değişince bir kez derlenen VBO'lardandır. Süpernova
 * koreografisi (çökme → parlama → kabuk) evre başlangıcından beri geçen saniyeyle, uzun sönüm evre ilerlemesiyle çizilir.
 */
final class GokyuzuCizici {
    private static final ResourceLocation GUNES_VANILLA =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/sun.png");
    private static final ResourceLocation AY_VANILLA =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/environment/moon_phases.png");
    private static final ResourceLocation SAMANYOLU = doku("samanyolu");
    private static final ResourceLocation GUNES = doku("gunes");
    private static final ResourceLocation HALE = doku("hale");
    private static final ResourceLocation SN_ISIN = doku("supernova_isin");
    private static final ResourceLocation SN_HALKA = doku("supernova_halka");
    private static final ResourceLocation YILDIZ_ISIN = doku("yildiz_isin");

    /**
     * Vanilla güneş diskinin yarı boyu (100 birim uzaklıkta): {@code sun.png} 32 pikselin ortadaki 8'i parlak disk, kare 30
     * birim yarı boylu → 7,5. Kendi dokumuzda disk yarıçapı dokunun 0,42'si.
     */
    private static final float GUNES_DISK = 7.5f;
    private static final float DOKU_DISK_ORANI = 0.42f;

    private static final Vector3f SAMANYOLU_N;
    private static final Vector3f SAMANYOLU_E1;
    private static final Vector3f SAMANYOLU_E2;

    static {
        float egim = (float) Math.toRadians(20);
        float az = (float) Math.toRadians(30);
        SAMANYOLU_N = new Vector3f(Mth.cos(egim) * Mth.cos(az), Mth.sin(egim), Mth.cos(egim) * Mth.sin(az)).normalize();
        Vector3f y = new Vector3f(0, 1, 0);
        SAMANYOLU_E1 = new Vector3f(y).sub(new Vector3f(SAMANYOLU_N).mul(y.dot(SAMANYOLU_N))).normalize();
        SAMANYOLU_E2 = new Vector3f(SAMANYOLU_N).cross(SAMANYOLU_E1).normalize();
    }

    /** Gök küresinde sabit bir cisim (gece yüzü −Y, gündüz yüzü +Y kutbu çevresinde). */
    private record Cisim(int doku, float az, float el, float derece, float donus, float alfa) {
        Vector3f yon() {
            float a = (float) Math.toRadians(az);
            float e = (float) Math.toRadians(el);
            return new Vector3f(Mth.cos(e) * Mth.cos(a), Mth.sin(e), Mth.cos(e) * Mth.sin(a));
        }
    }

    // doku: 0 büyük sarmal galaksi, 1 küçük galaksi, 2 kırmızı nebula, 3 mavi nebula, 4 karışık nebula
    private static final Cisim[] CISIMLER = {
            new Cisim(0, 40, -62, 9f, 35f, 0.95f),
            new Cisim(1, 205, -52, 4f, 0f, 0.9f),
            new Cisim(2, 120, -72, 6.5f, 10f, 0.9f),
            new Cisim(3, 300, -60, 4.5f, 70f, 0.85f),
            new Cisim(4, 250, -78, 5.5f, 140f, 0.85f),
            new Cisim(0, 160, 64, 7f, -20f, 0.8f),
            new Cisim(4, 330, 66, 5f, 40f, 0.75f),
            new Cisim(1, 70, 58, 3.5f, 0f, 0.8f),
    };

    private static final Vector3f TEPE = new Vector3f(0, 1, 0);
    private static EfektKalitesi kurulanKalite;
    private static double kurulanYogunluk;
    private static VertexBuffer kubbe;
    private static VertexBuffer karanlik;
    private static VertexBuffer samanyoluVbo;
    private static VertexBuffer parlakVbo;
    private static VertexBuffer[] yildizGruplari = new VertexBuffer[0];
    private static final ResourceLocation[] cisimDokulari = new ResourceLocation[5];
    private static ResourceLocation kalintiDokusu;
    private static boolean ilkCizimYazildi;
    private static long cizimSuresiNs;
    private static long cizimSayisi;

    private GokyuzuCizici() {
    }

    private static ResourceLocation doku(String ad) {
        return DogalAfetler.konum("textures/environment/gokyuzu/" + ad + ".png");
    }

    // ------------------------------------------------------------------ kurulum (kaliteye göre)

    private static void hazirla() {
        EfektKalitesi k = IstemciAyarlari.efektKalitesi();
        double yogunluk = GokyuzuAyarlari.yildizYogunlugu();
        if (k == kurulanKalite && yogunluk == kurulanYogunluk && kubbe != null) {
            return;
        }
        long bas = System.nanoTime();
        kapat();
        int yildiz;
        int dilim;
        int grup;
        int cisimBoy;
        int kucukBoy;
        int kalintiBoy;
        switch (k) {
            case DUSUK -> {
                yildiz = 2000; dilim = 24; grup = 1; cisimBoy = 128; kucukBoy = 64; kalintiBoy = 512;
            }
            case ORTA -> {
                yildiz = 4000; dilim = 48; grup = 2; cisimBoy = 256; kucukBoy = 128; kalintiBoy = 512;
            }
            case ULTRA -> {
                yildiz = 10000; dilim = 96; grup = 4; cisimBoy = 512; kucukBoy = 256; kalintiBoy = 2048;
            }
            default -> {
                yildiz = 6000; dilim = 64; grup = 4; cisimBoy = 256; kucukBoy = 128; kalintiBoy = 1024;
            }
        }
        cisimDokulari[0] = doku("galaksi_sarmal_" + cisimBoy);
        cisimDokulari[1] = doku("galaksi_kucuk_" + kucukBoy);
        cisimDokulari[2] = doku("nebula_kirmizi_" + cisimBoy);
        cisimDokulari[3] = doku("nebula_mavi_" + cisimBoy);
        cisimDokulari[4] = doku("nebula_karisik_" + cisimBoy);
        kalintiDokusu = doku("kalinti_" + kalintiBoy);

        kubbe = disk(16f);
        karanlik = disk(-16f);
        samanyoluVbo = samanyoluSeridi(dilim);
        yildiz = Math.max(200, (int) Math.round(yildiz * yogunluk));
        int parlak = yildizlariKur(yildiz, grup);
        kurulanKalite = k;
        kurulanYogunluk = yogunluk;
        DogalAfetler.LOGGER.info("[gokyuzu] VBO'lar kuruldu: kalite={}, yildiz={}, parlak={}, samanyolu dilim={}, grup={}, "
                + "kalinti={}px, süre={} ms", k, yildiz, parlak, dilim, grup, kalintiBoy,
                (System.nanoTime() - bas) / 1_000_000);
    }

    private static void kapat() {
        for (VertexBuffer v : new VertexBuffer[] {kubbe, karanlik, samanyoluVbo, parlakVbo}) {
            if (v != null) {
                v.close();
            }
        }
        for (VertexBuffer v : yildizGruplari) {
            v.close();
        }
        kubbe = karanlik = samanyoluVbo = parlakVbo = null;
        yildizGruplari = new VertexBuffer[0];
    }

    private static VertexBuffer yukle(BufferBuilder.RenderedBuffer veri) {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(veri);
        VertexBuffer.unbind();
        return vb;
    }

    /** Vanilla {@code buildSkyDisc} ile aynı disk (kubbe y=16, ufuk altı y=-16). */
    private static VertexBuffer disk(float y) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        float r = Math.signum(y) * 512f;
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        bb.vertex(0, y, 0).endVertex();
        for (int i = -180; i <= 180; i += 45) {
            bb.vertex(r * Mth.cos(i * Mth.DEG_TO_RAD), y, 512f * Mth.sin(i * Mth.DEG_TO_RAD)).endVertex();
        }
        return yukle(bb.end());
    }

    private static Vector3f seritNoktasi(float teta, float enlem) {
        Vector3f c = new Vector3f(SAMANYOLU_E1).mul(Mth.cos(teta)).add(new Vector3f(SAMANYOLU_E2).mul(Mth.sin(teta)));
        return c.mul(Mth.cos(enlem)).add(new Vector3f(SAMANYOLU_N).mul(Mth.sin(enlem)));
    }

    /** Gök küresinde eğik büyük çember boyunca 24° geniş şerit; u=0,5 galaksi merkezi (gece tepe noktasına yakın). */
    private static VertexBuffer samanyoluSeridi(int dilim) {
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float w = (float) Math.toRadians(12);
        for (int i = 0; i < dilim; i++) {
            float u0 = (float) i / dilim;
            float u1 = (float) (i + 1) / dilim;
            float t0 = u0 * Mth.TWO_PI;
            float t1 = u1 * Mth.TWO_PI;
            Vector3f a = seritNoktasi(t0, -w).mul(100f);
            Vector3f b = seritNoktasi(t0, w).mul(100f);
            Vector3f c = seritNoktasi(t1, w).mul(100f);
            Vector3f d = seritNoktasi(t1, -w).mul(100f);
            bb.vertex(a.x, a.y, a.z).uv(u0, 0f).color(1f, 1f, 1f, 1f).endVertex();
            bb.vertex(b.x, b.y, b.z).uv(u0, 1f).color(1f, 1f, 1f, 1f).endVertex();
            bb.vertex(c.x, c.y, c.z).uv(u1, 1f).color(1f, 1f, 1f, 1f).endVertex();
            bb.vertex(d.x, d.y, d.z).uv(u1, 0f).color(1f, 1f, 1f, 1f).endVertex();
        }
        return yukle(bb.end());
    }

    private record Yildiz(Vector3f yon, float yari, float aci, float r, float g, float b, float a) {
    }

    /** Yıldızları üretir ve gruplara bölünmüş VBO'lara yazar; en parlak %1 ayrıca ışınlı sprite olur. */
    private static int yildizlariKur(int adet, int grup) {
        Random r = new Random(20261105L);
        float[][] renkler = {GokyuzuEgrileri.rgb(0xF4F6FF), GokyuzuEgrileri.rgb(0xAFC8FF), GokyuzuEgrileri.rgb(0xFFE7B0),
                GokyuzuEgrileri.rgb(0xFFB08A)};
        List<List<Yildiz>> gruplar = new ArrayList<>();
        for (int g = 0; g < grup; g++) {
            gruplar.add(new ArrayList<>());
        }
        List<Yildiz> parlaklar = new ArrayList<>();
        for (int i = 0; i < adet; i++) {
            Vector3f yon;
            if (r.nextFloat() < 0.35f) {
                float teta = r.nextFloat() * Mth.TWO_PI;
                float enlem = (float) Mth.clamp(r.nextGaussian() * Math.toRadians(7), -Math.toRadians(14),
                        Math.toRadians(14));
                yon = seritNoktasi(teta, enlem);
            } else {
                yon = new Vector3f((float) r.nextGaussian(), (float) r.nextGaussian(), (float) r.nextGaussian());
                if (yon.lengthSquared() < 1e-4f) {
                    continue;
                }
                yon.normalize();
            }
            float m = (float) -Math.log(1.0 - r.nextFloat());
            float yari = 0.10f + 0.28f * Math.min(1f, m / 4.5f);
            float alfa = Math.min(1f, 0.25f + m * 0.22f);
            float sec = r.nextFloat();
            float[] c = renkler[sec < 0.70f ? 0 : sec < 0.82f ? 1 : sec < 0.92f ? 2 : 3];
            float aci = r.nextFloat() * Mth.TWO_PI;
            gruplar.get(i % grup).add(new Yildiz(yon, yari, aci, c[0], c[1], c[2], alfa));
            if (m > 4.6f) {
                parlaklar.add(new Yildiz(yon, 1.1f + 0.35f * Math.min(3f, m - 4.6f), aci, c[0], c[1], c[2], 1f));
            }
        }
        yildizGruplari = new VertexBuffer[grup];
        for (int g = 0; g < grup; g++) {
            BufferBuilder bb = Tesselator.getInstance().getBuilder();
            bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (Yildiz y : gruplar.get(g)) {
                yildizKuadi(bb, y, false);
            }
            yildizGruplari[g] = yukle(bb.end());
        }
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        for (Yildiz y : parlaklar) {
            yildizKuadi(bb, y, true);
        }
        parlakVbo = yukle(bb.end());
        return parlaklar.size();
    }

    /** Vanilla {@code drawStars} ile aynı yönelim: kuad küre merkezine bakar, kendi ekseninde rastgele döner. */
    private static void yildizKuadi(BufferBuilder bb, Yildiz y, boolean dokulu) {
        double x = y.yon.x;
        double yy = y.yon.y;
        double z = y.yon.z;
        double cx = x * 100;
        double cy = yy * 100;
        double cz = z * 100;
        double d8 = Math.atan2(x, z);
        double s8 = Math.sin(d8);
        double c8 = Math.cos(d8);
        double d11 = Math.atan2(Math.sqrt(x * x + z * z), yy);
        double s11 = Math.sin(d11);
        double c11 = Math.cos(d11);
        double s14 = Math.sin(y.aci);
        double c14 = Math.cos(y.aci);
        for (int j = 0; j < 4; j++) {
            double a = ((j & 2) - 1) * (double) y.yari;
            double b = ((j + 1 & 2) - 1) * (double) y.yari;
            double d21 = a * c14 - b * s14;
            double d22 = b * c14 + a * s14;
            double d23 = d21 * s11;
            double d24 = -d21 * c11;
            double d25 = d24 * s8 - d22 * c8;
            double d26 = d22 * s8 + d24 * c8;
            bb.vertex(cx + d25, cy + d23, cz + d26);
            if (dokulu) {
                bb.uv((j & 2) == 0 ? 0f : 1f, (j + 1 & 2) == 0 ? 0f : 1f);
            }
            bb.color(y.r, y.g, y.b, y.a).endVertex();
        }
    }

    // ------------------------------------------------------------------ çizim

    private static boolean gokGizli(Camera kamera) {
        FogType sivi = kamera.getFluidInCamera();
        if (sivi == FogType.POWDER_SNOW || sivi == FogType.LAVA) {
            return true;
        }
        return kamera.getEntity() instanceof LivingEntity l
                && (l.hasEffect(MobEffects.BLINDNESS) || l.hasEffect(MobEffects.DARKNESS));
    }

    /** Kanca (b) yolu: gökyüzünün tamamını biz çizeriz. */
    static void ciz(ClientLevel level, float pt, PoseStack poz, Camera kamera, Matrix4f izdusum, boolean sisli,
            Runnable sisKur) {
        long bas = System.nanoTime();
        sisKur.run();
        if (sisli || gokGizli(kamera)) {
            return;
        }
        hazirla();
        IstemciGokyuzu.Gorunum d = IstemciGokyuzu.gorunum(pt);
        float zaman = level.getTimeOfDay(pt);
        float gun = GokyuzuRenkleri.gunIsigi(zaman);
        float yagmursuz = 1f - level.getRainLevel(pt);
        float sn = ((level.getGameTime() % 72000L) + pt) / 20f;
        Vec3 v = level.getSkyColor(kamera.getPosition(), pt);
        float[] gok = GokyuzuRenkleri.gokRengi(d, new float[] {(float) v.x, (float) v.y, (float) v.z}, gun);
        if (!ilkCizimYazildi) {
            ilkCizimYazildi = true;
            DogalAfetler.LOGGER.info("[gokyuzu] ilk özel gökyüzü çizimi (kanca b, sarmalayıcı): {}", d);
        }

        RenderSystem.disableCull();
        FogRenderer.levelFogColor();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(gok[0], gok[1], gok[2], 1f);
        kubbe.bind();
        kubbe.drawWithShader(poz.last().pose(), izdusum, GameRenderer.getPositionShader());
        VertexBuffer.unbind();
        // Kubbe rengi shader rengine yazıldı; sıfırlanmazsa gündüz (uzay katmanı atlanınca) güneş gök rengiyle çarpılır.
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        gunDogumu(level, poz, zaman, pt);

        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        poz.pushPose();
        poz.mulPose(Axis.YP.rotationDegrees(-90f));
        poz.mulPose(Axis.XP.rotationDegrees(zaman * 360f));
        float uzay = GokyuzuRenkleri.uzayAlfa(d, zaman) * yagmursuz;
        if (uzay > 0.003f) {
            FogRenderer.setupNoFog();
            derinUzay(poz.last().pose(), izdusum, d, uzay, sn);
            sisKur.run();
        }
        gunesEvresi(poz.last().pose(), d, yagmursuz, sn);
        if (d.ayVar()) {
            ay(poz.last().pose(), level, d, yagmursuz);
        }
        poz.popPose();

        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(0f, 0f, 0f, 1f);
        Minecraft mc = Minecraft.getInstance();
        double ufuk = mc.player.getEyePosition(pt).y - level.getLevelData().getHorizonHeight(level);
        if (ufuk < 0.0) {
            poz.pushPose();
            poz.translate(0f, 12f, 0f);
            karanlik.bind();
            karanlik.drawWithShader(poz.last().pose(), izdusum, GameRenderer.getPositionShader());
            VertexBuffer.unbind();
            poz.popPose();
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        cizimSuresiNs += System.nanoTime() - bas;
        cizimSayisi++;
    }

    /** Kanca (b) çiziminin kare başına ortalama CPU süresini (µs) verir ve sayacı sıfırlar. GPU süresi dahil değil. */
    static double ortalamaCizimMikrosaniye() {
        double sonuc = cizimSayisi == 0 ? 0 : cizimSuresiNs / 1000.0 / cizimSayisi;
        cizimSuresiNs = 0;
        cizimSayisi = 0;
        return sonuc;
    }

    /** Kanca (c) yolu: vanilla gökyüzü çizildikten sonra yalnız derin uzay eklenir; güneş ve ay gizlenemez. */
    static void afterSkyCiz(PoseStack poz, Matrix4f izdusum, float pt, Camera kamera) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        IstemciGokyuzu.Gorunum d = IstemciGokyuzu.gorunum(pt);
        if (level == null || d.vanillaMi() || gokGizli(kamera)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        hazirla();
        float zaman = level.getTimeOfDay(pt);
        float sn = ((level.getGameTime() % 72000L) + pt) / 20f;
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        poz.pushPose();
        poz.mulPose(Axis.YP.rotationDegrees(-90f));
        poz.mulPose(Axis.XP.rotationDegrees(zaman * 360f));
        Matrix4f m = poz.last().pose();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        FogRenderer.setupNoFog();
        derinUzay(m, izdusum, d, GokyuzuRenkleri.uzayAlfa(d, zaman) * (1f - level.getRainLevel(pt)), sn);
        poz.popPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    private static void gunDogumu(ClientLevel level, PoseStack poz, float zaman, float pt) {
        float[] renk = level.effects().getSunriseColor(zaman, pt);
        if (renk == null) {
            return;
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        poz.pushPose();
        poz.mulPose(Axis.XP.rotationDegrees(90f));
        poz.mulPose(Axis.ZP.rotationDegrees(Mth.sin(level.getSunAngle(pt)) < 0f ? 180f : 0f));
        poz.mulPose(Axis.ZP.rotationDegrees(90f));
        Matrix4f m = poz.last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, 0f, 100f, 0f).color(renk[0], renk[1], renk[2], renk[3]).endVertex();
        for (int j = 0; j <= 16; j++) {
            float a = j * Mth.TWO_PI / 16f;
            float s = Mth.sin(a);
            float c = Mth.cos(a);
            bb.vertex(m, s * 120f, c * 120f, -c * 40f * renk[3]).color(renk[0], renk[1], renk[2], 0f).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
        poz.popPose();
    }

    private static void derinUzay(Matrix4f m, Matrix4f izdusum, IstemciGokyuzu.Gorunum d, float uzay, float sn) {
        boolean zengin = GokyuzuRenkleri.zengin(d);
        if (zengin && GokyuzuAyarlari.nebulaKatmani()) {
            RenderSystem.setShaderTexture(0, SAMANYOLU);
            RenderSystem.setShaderColor(1f, 1f, 1f, uzay * 0.6f);
            samanyoluVbo.bind();
            samanyoluVbo.drawWithShader(m, izdusum, GameRenderer.getPositionTexColorShader());
            VertexBuffer.unbind();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            for (Cisim c : CISIMLER) {
                float yari = 100f * (float) Math.tan(Math.toRadians(c.derece) / 2);
                kuad(m, cisimDokulari[c.doku], c.yon(), yari, c.donus, 1f, 1f, 1f, c.alfa * uzay);
            }
        }
        float kalinti = GokyuzuRenkleri.kalintiAlfa(d);
        if (kalinti > 0f) {
            float genisleme = d.evre() == GunesEvresi.GUNESSIZ ? GokyuzuEgrileri.kalintiGenisleme(d.ilerleme())
                    : d.evre() == GunesEvresi.KIZIL_CUCE ? GokyuzuEgrileri.kalintiGenisleme(1f) : 1f;
            float yari = 100f * (float) Math.tan(Math.toRadians(20)) * genisleme;
            kuad(m, kalintiDokusu, TEPE, yari, 15f, 1f, 1f, 1f, kalinti * Math.max(uzay, 0.35f));
            // Ortadaki pulsar: 0,5 Hz hafif yanıp sönme.
            float nabiz = 0.55f + 0.45f * Mth.sin(sn * Mth.TWO_PI * 0.5f);
            kuad(m, YILDIZ_ISIN, TEPE, 2.2f, 45f, 0.7f, 0.85f, 1f, kalinti * nabiz);
        }
        float carpan = zengin ? 1f : 0.6f;
        float[] frekans = {1.1f, 1.5f, 1.9f, 2.3f};
        float[] faz = {0f, 1.7f, 3.1f, 4.4f};
        for (int g = 0; g < yildizGruplari.length; g++) {
            float titreme = yildizGruplari.length > 1 ? 1f + 0.15f * Mth.sin(sn * Mth.TWO_PI * frekans[g] + faz[g]) : 1f;
            RenderSystem.setShaderColor(1f, 1f, 1f, Math.min(1f, uzay * carpan * titreme));
            yildizGruplari[g].bind();
            yildizGruplari[g].drawWithShader(m, izdusum, GameRenderer.getPositionColorShader());
            VertexBuffer.unbind();
        }
        RenderSystem.setShaderTexture(0, YILDIZ_ISIN);
        RenderSystem.setShaderColor(1f, 1f, 1f, uzay * carpan);
        parlakVbo.bind();
        parlakVbo.drawWithShader(m, izdusum, GameRenderer.getPositionTexColorShader());
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void gunesEvresi(Matrix4f m, IstemciGokyuzu.Gorunum d, float yagmursuz, float sn) {
        if (d.onceki() == GunesEvresi.SUPERNOVA && d.gecis() < 1f) {
            supernova(m, 1f, 1e6f, yagmursuz * (1f - d.gecis()), sn);
        }
        float p = d.ilerleme();
        switch (d.evre()) {
            case NORMAL -> kuad(m, GUNES_VANILLA, TEPE, 30f, 0f, 1f, 1f, 1f, yagmursuz);
            case BUYUYEN -> buyuyen(m, GokyuzuEgrileri.buyuyenOlcek(p), GokyuzuEgrileri.buyuyenRenk(p), p, yagmursuz, sn);
            case SUPERNOVA -> {
                float s = d.saniye();
                float cokme = GokyuzuEgrileri.SN_COKME_SN;
                if (s < cokme + 1f) {
                    // Çöken şişmiş güneş: turuncudan mavi-beyaza döner, küçülür, parlamayla kaybolur.
                    float c = GokyuzuEgrileri.yumusak(0f, cokme, s);
                    float[] r = GokyuzuEgrileri.karistir(GokyuzuEgrileri.buyuyenRenk(1f), new float[] {0.9f, 0.95f, 1f}, c);
                    float sonum = 1f - GokyuzuEgrileri.yumusak(cokme, cokme + 1f, s);
                    buyuyen(m, GokyuzuEgrileri.supernovaCekirdekOlcek(s), r, 1f - c, yagmursuz * sonum, sn);
                }
                supernova(m, p, s, yagmursuz, sn);
            }
            case GUNESSIZ -> {
            }
            case KIZIL_CUCE -> {
                float disk = GUNES_DISK * d.boyutOrani();
                float dogus = 1f - p;
                float da = GokyuzuEgrileri.kizilCuceDiskAlfa(p) * yagmursuz;
                kuad(m, HALE, TEPE, disk * (5f + 3f * dogus), 0f, 1f, 0.3f, 0.15f, (0.4f + 0.3f * dogus) * yagmursuz);
                kuadOrtu(m, GUNES, TEPE, disk / DOKU_DISK_ORANI, sn * 2f, 1f, 0.416f, 0.227f, da);
                kuad(m, GUNES, TEPE, disk / DOKU_DISK_ORANI, sn * 2f, 1f, 0.416f, 0.227f, (0.25f + 0.6f * dogus) * da);
                kuad(m, HALE, TEPE, disk * 1.8f, 0f, 1f, 0.5f, 0.28f, 0.5f * dogus * da);
            }
        }
    }

    /** Şişen güneş: disk normal karışım + %30 toplamalı (parlak gökte beyaza doymasın), etrafında toplamalı hale. */
    private static void buyuyen(Matrix4f m, float olcek, float[] c, float haleP, float a, float sn) {
        float disk = GUNES_DISK * olcek;
        kuad(m, HALE, TEPE, disk * (3f + 3f * haleP), 0f, c[0], c[1] * 0.9f, c[2] * 0.7f, (0.2f + 0.3f * haleP) * a);
        kuadOrtu(m, GUNES, TEPE, disk / DOKU_DISK_ORANI, sn * 2f, c[0], c[1], c[2], a);
        kuad(m, GUNES, TEPE, disk / DOKU_DISK_ORANI, sn * 2f, c[0], c[1], c[2], 0.3f * a);
    }

    /** Süpernova: çekirdek hale, 12 ışın, genişleyen kabuk; Düşük kalitede büyük toplamalı kareler küçülür (G6). */
    private static void supernova(Matrix4f m, float p, float s, float a, float sn) {
        float cokme = GokyuzuEgrileri.SN_COKME_SN;
        float acilis = GokyuzuEgrileri.yumusak(cokme - 0.2f, cokme + 0.6f, s);
        if (acilis <= 0f) {
            return;
        }
        float ga = a * acilis;
        float f = GokyuzuEgrileri.supernovaFlas(s);
        float bp = GokyuzuEgrileri.supernovaParlaklik(p);
        float k = IstemciAyarlari.efektKalitesi() == EfektKalitesi.DUSUK ? 0.6f : 1f;
        float sicak = GokyuzuEgrileri.yumusak(0.05f, 0.6f, p);
        kuad(m, HALE, TEPE, (40f + 50f * f) * k, 0f, 0.8f, 0.88f, 1f, (0.3f * bp + 0.35f * f) * ga);
        kuad(m, SN_HALKA, TEPE, GokyuzuEgrileri.supernovaHalka(s, p), sn * 0.5f, Mth.lerp(sicak, 0.85f, 1f),
                Mth.lerp(sicak, 0.9f, 0.93f), Mth.lerp(sicak, 1f, 0.78f), (0.35f + 0.65f * bp) * ga);
        kuad(m, SN_ISIN, TEPE, (30f + 70f * f + 15f * bp) * k, sn * 1.5f, 0.85f, 0.92f, 1f,
                Math.min(1f, 0.25f + 0.5f * bp + 0.4f * f) * ga);
        kuad(m, HALE, TEPE, 4f + 6f * bp + 8f * f, 0f, 1f, 1f, 1f, ga);
    }

    private static void ay(Matrix4f m, ClientLevel level, IstemciGokyuzu.Gorunum d, float yagmursuz) {
        float r = 1f;
        float g = 1f;
        float b = 1f;
        int evre = level.getMoonPhase();
        switch (d.evre()) {
            case SUPERNOVA -> {
                r = 0.75f;
                g = 0.85f;
            }
            case GUNESSIZ -> {
                // Güneş yok: hilal yerine yalnız kalıntı ışığıyla soluk gri dolunay diski (#5A6070), evre döngüsü durur.
                r = 0.353f;
                g = 0.376f;
                b = 0.439f;
                evre = 0;
            }
            case KIZIL_CUCE -> {
                g = 0.72f;
                b = 0.55f;
            }
            default -> {
            }
        }
        float u0 = (evre % 4) / 4f;
        float v0 = (evre / 4 % 2) / 2f;
        float u1 = (evre % 4 + 1) / 4f;
        float v1 = (evre / 4 % 2 + 1) / 2f;
        float s = 20f;
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, AY_VANILLA);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.vertex(m, -s, -100f, s).uv(u1, v1).color(r, g, b, yagmursuz).endVertex();
        bb.vertex(m, s, -100f, s).uv(u0, v1).color(r, g, b, yagmursuz).endVertex();
        bb.vertex(m, s, -100f, -s).uv(u0, v0).color(r, g, b, yagmursuz).endVertex();
        bb.vertex(m, -s, -100f, -s).uv(u1, v0).color(r, g, b, yagmursuz).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }

    /** {@link #kuad} ama normal alfa karışımıyla (örten disk); sonra toplamalı karışıma döner. */
    private static void kuadOrtu(Matrix4f m, ResourceLocation doku, Vector3f yon, float yari, float donus, float r,
            float g, float b, float a) {
        RenderSystem.defaultBlendFunc();
        kuad(m, doku, yon, yari, donus, r, g, b, a);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
    }

    /** Gök küresine teğet dokulu kare: merkez {@code yon*100}, yarı boy {@code yari}, kendi ekseninde {@code donus}. */
    private static void kuad(Matrix4f m, ResourceLocation doku, Vector3f yon, float yari, float donus, float r, float g,
            float b, float a) {
        if (a <= 0.002f) {
            return;
        }
        Vector3f ref = Math.abs(yon.y) > 0.9f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f u = new Vector3f(ref).cross(yon).normalize();
        Vector3f v = new Vector3f(yon).cross(u).normalize();
        float dr = (float) Math.toRadians(donus);
        Vector3f uu = new Vector3f(u).mul(Mth.cos(dr)).add(new Vector3f(v).mul(Mth.sin(dr))).mul(yari);
        Vector3f vv = new Vector3f(v).mul(Mth.cos(dr)).sub(new Vector3f(u).mul(Mth.sin(dr))).mul(yari);
        Vector3f c = new Vector3f(yon).mul(100f);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, doku);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.vertex(m, c.x - uu.x - vv.x, c.y - uu.y - vv.y, c.z - uu.z - vv.z).uv(0f, 0f).color(r, g, b, a).endVertex();
        bb.vertex(m, c.x + uu.x - vv.x, c.y + uu.y - vv.y, c.z + uu.z - vv.z).uv(1f, 0f).color(r, g, b, a).endVertex();
        bb.vertex(m, c.x + uu.x + vv.x, c.y + uu.y + vv.y, c.z + uu.z + vv.z).uv(1f, 1f).color(r, g, b, a).endVertex();
        bb.vertex(m, c.x - uu.x + vv.x, c.y - uu.y + vv.y, c.z - uu.z + vv.z).uv(0f, 1f).color(r, g, b, a).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }
}
