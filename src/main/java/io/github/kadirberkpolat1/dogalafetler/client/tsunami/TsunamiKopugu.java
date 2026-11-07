package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Tsunami dalgasının köpük katmanı (tasarım notu §2 "Köpük"): ortak su renderer'ının üstüne, tsunami sürerken her dalganın
 * cephesi boyunca üç şerit çizilir: tepe boyunca şerit, ön yüzden aşağı akan kesik köpük çizgileri ve cephe tabanında yuvarlanan
 * bore; üçü de {@code su_kopuk} dokulu (Ts2-1), parlaklık ve alfa yanal öbeklerle değişir. Yükseklik dalganın analitik
 * yüzeyinden ({@link Dalga#yuzey}) alınır; parçacık değil geometri olduğu için maliyeti cephe uzunluğuyla doğrusaldır. Kalite
 * (N13): Düşük'te yalnız tepe şeridi, 2 blok adım; diğerlerinde üç şerit, 1 blok adım (kameradan 64 blok ötede 2). Renk gün
 * ışığıyla ölçeklenir.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class TsunamiKopugu {
    /** Tepe: yüzey bu {@code s} aralığında en yüksek ({@code Dalga} ön yüzü 2,5 blok). */
    private static final double TEPE_ON = 1.4;
    private static final double TEPE_ORTA = TEPE_ON + 1.1;
    private static final double TEPE_ARKA = 4.5;
    /** Ön yüz çizgilerinin alt ucu ({@code s}). */
    private static final double CIZGI_ALT = 0.35;
    /** Köpük dokusu (animasyonlu, blok atlası). */
    private static final ResourceLocation KOPUK = DogalAfetler.konum("block/su_kopuk");
    private static int sonSegment;
    private static float ku0;
    private static float ku1;
    private static float kv0;
    private static float kv1;
    private static final ThreadMXBean IPLIK = ManagementFactory.getThreadMXBean();
    /**
     * 10 sn'lik ölçüm: köpüğün kendi render thread CPU'sı (köşe üretimi, tampon yükleme, çizim çağrısı), kare başına. Yalnız
     * {@code -Potomasyon} ile ({@link GorselOtomasyon#AKTIF}); üretimde CPU okuması ve log satırı yok.
     */
    private static long logCpuNs;
    private static long logEnCokNs;
    private static int logKare;
    private static long logTick = Long.MIN_VALUE;

    private TsunamiKopugu() {
    }

    /** Otomasyon ölçümü: son karede çizilen köpük segmenti. */
    static int sonSegment() {
        return sonSegment;
    }

    @SubscribeEvent
    public static void ciz(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS) {
            return;
        }
        sonSegment = 0;
        SanalSuDunyasi d = SanalSu.istemci;
        Minecraft mc = Minecraft.getInstance();
        if (d == null || mc.level == null || d.dalgalar().isEmpty()
                || IstemciAfetDurumu.aktif().filter(p -> p.tur() == AfetTuru.TSUNAMI).isEmpty()) {
            return;
        }
        long cpu0 = GorselOtomasyon.AKTIF ? IPLIK.getCurrentThreadCpuTime() : 0;
        float pt = e.getPartialTick();
        double t = IstemciDonem.saat(pt);
        Vec3 kam = e.getCamera().getPosition();
        boolean dusuk = IstemciAyarlari.efektKalitesi() == EfektKalitesi.DUSUK;
        float isik = Mth.clamp(mc.level.getSkyDarken(pt), 0.3f, 1f);
        Matrix4f m = e.getPoseStack().last().pose();
        TextureAtlasSprite sp = mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(KOPUK);
        ku0 = sp.getU0();
        ku1 = sp.getU1();
        kv0 = sp.getV0();
        kv1 = sp.getV1();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int segment = 0;
        for (Dalga w : d.dalgaDizisi()) {
            if (w.bitti(t) || w.yukseklik() < 1.5) {
                continue;
            }
            segment += dalga(b, m, w, t, kam, dusuk, isik);
        }
        // Boş tamponu BufferUploader çizmeden bırakır.
        Tesselator.getInstance().end();
        sonSegment = segment;
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        if (GorselOtomasyon.AKTIF) {
            olcum(IPLIK.getCurrentThreadCpuTime() - cpu0, mc.level.getGameTime(), segment);
        }
    }

    private static void olcum(long g, long tick, int segment) {
        logCpuNs += g;
        logEnCokNs = Math.max(logEnCokNs, g);
        logKare++;
        if (tick - logTick >= 200) {
            DogalAfetler.LOGGER.info("Tsunami köpüğü: kare_cpu_ort={} ms en_cok={} ms kare={} segment={}",
                    String.format(Locale.ROOT, "%.3f", logCpuNs / 1e6 / logKare), String.format(Locale.ROOT, "%.3f", logEnCokNs / 1e6),
                    logKare, segment);
            logTick = tick;
            logCpuNs = 0;
            logEnCokNs = 0;
            logKare = 0;
        }
    }

    private static int dalga(BufferBuilder b, Matrix4f m, Dalga w, double t, Vec3 kam, boolean dusuk, float isik) {
        double c = w.cephe(t);
        double px = -w.yonZ();
        double pz = w.yonX();
        double yarim = w.genislik() / 2 - 1;
        int n = 0;
        double u = -yarim;
        while (u < yarim) {
            double mx = w.x0() + w.yonX() * c + px * u;
            double mz = w.z0() + w.yonZ() * c + pz * u;
            double uzak = Math.hypot(mx - kam.x, mz - kam.z);
            double adim = dusuk || uzak > 64 ? 2 : 1;
            if (uzak > 220) {
                adim = 4;
            }
            double u1 = Math.min(yarim, u + adim);
            // Doku yanal 2 blokta bir döşenir (1 blokluk desen uzakta mipmap'le düz banda dönüyordu): 1 blokluk segment
            // karonun tek/çift yarısını alır, daha uzun segment karonun tamamını.
            float ta = adim > 1 ? 0f : 0.5f * ((int) Math.floor(u + yarim) & 1);
            float tb = adim > 1 ? 1f : ta + 0.5f * (float) (u1 - u);
            n += serit(b, m, w, t, c, u, u1, ta, tb, kam, dusuk, isik);
            u = u1;
        }
        return n;
    }

    /** Yanal {@code [u0, u1]} aralığında üç şerit (doku {@code [ta, tb]}); yüzeyi olmayan (yan kenar) aralık atlanır. */
    private static int serit(BufferBuilder b, Matrix4f m, Dalga w, double t, double c, double u0, double u1, float ta, float tb,
            Vec3 kam, boolean dusuk, float isik) {
        double h0 = yukseklik(w, t, c, TEPE_ORTA, u0);
        double h1 = yukseklik(w, t, c, TEPE_ORTA, u1);
        if (Double.isNaN(h0) || Double.isNaN(h1)) {
            return 0;
        }
        float k0 = isik * (float) (0.8 + 0.2 * gurultu(u0, t, 6));
        float k1 = isik * (float) (0.8 + 0.2 * gurultu(u1, t, 6));
        tepe(b, m, w, t, c, u0, u1, h0, h1, ta, tb, kam, k0, k1);
        if (dusuk) {
            return 1;
        }
        // Ön yüz çizgileri: yanal konuma bağlı sabit desenle kesik (her ~3 bloktan biri boş), aşağı doğru söner.
        double desen = Math.sin(u0 * 1.7 + Math.floor(t / 6) * 0.9) * Math.sin(u0 * 0.43 + 1.3);
        if (desen > -0.35) {
            float a = (float) Math.min(1, 1.3 * (0.55 + 0.3 * desen));
            onYuz(b, m, w, t, c, u0, u1, ta, tb, kam, k0, k1, a * (float) (0.5 + 0.5 * gurultu(u0, t, 8)),
                    a * (float) (0.5 + 0.5 * gurultu(u1, t, 8)));
        }
        // Bore: cephe tabanında yuvarlanan basamak, yüksekliği dalganın %12'si (en çok 1,6 blok), titreşen üst kenar.
        double bore = Math.min(1.6, 0.12 * w.yukseklik()) * (0.8 + 0.2 * Math.sin(t * 0.6 + u0 * 0.8));
        double taban = w.taban() + 0.05;
        double s0 = c + 0.6;
        double s1 = c - 0.4;
        kose(b, m, w, s0, u0, taban, kam, k0, 1f, ta, 1f);
        kose(b, m, w, s0, u1, taban, kam, k1, 1f, tb, 1f);
        kose(b, m, w, s0, u1, taban + bore, kam, k1, 0.55f, tb, 0.6f);
        kose(b, m, w, s0, u0, taban + bore, kam, k0, 0.55f, ta, 0.6f);
        kose(b, m, w, s0, u0, taban + bore, kam, k0, 0.55f, ta, 0.6f);
        kose(b, m, w, s0, u1, taban + bore, kam, k1, 0.55f, tb, 0.6f);
        kose(b, m, w, s1, u1, taban + bore, kam, k1, 0.9f, tb, 0f);
        kose(b, m, w, s1, u0, taban + bore, kam, k0, 0.9f, ta, 0f);
        return 1;
    }

    /**
     * Tepe şeridi (Ts2-1): doku iki katman (dokunun ortalama alfasıyla tek katman ~%43 örter, aynalı ikinci katmanla ~%67),
     * yüzeyin 0,12 / 0,10 blok üstünde; önde opak, arkaya doğru saydamlaşır. Ön kenarın ve orta çizginin alfası ile arka
     * kenarın yeri (3,5–5,5 blok) yanal gürültüyle değişir; aynı {@code u}'da komşu segmentler aynı değeri alır (dikiş yok).
     */
    private static void tepe(BufferBuilder b, Matrix4f m, Dalga w, double t, double c, double u0, double u1, double ym0,
            double ym1, float ta, float tb, Vec3 kam, float k0, float k1) {
        float on0 = (float) (0.35 + 0.65 * gurultu(u0, t, 0));
        float on1 = (float) (0.35 + 0.65 * gurultu(u1, t, 0));
        float orta0 = (float) (0.5 + 0.5 * gurultu(u0, t, 2));
        float orta1 = (float) (0.5 + 0.5 * gurultu(u1, t, 2));
        double arka0 = TEPE_ARKA + 2 * (gurultu(u0, t, 4) - 0.5);
        double arka1 = TEPE_ARKA + 2 * (gurultu(u1, t, 4) - 0.5);
        double yo0 = yukseklik(w, t, c, TEPE_ON, u0);
        double yo1 = yukseklik(w, t, c, TEPE_ON, u1);
        double ya0 = yukseklik(w, t, c, arka0, u0);
        double ya1 = yukseklik(w, t, c, arka1, u1);
        boolean on = !Double.isNaN(yo0) && !Double.isNaN(yo1);
        boolean arka = !Double.isNaN(ya0) && !Double.isNaN(ya1);
        for (int katman = 0; katman < 2; katman++) {
            float a = katman == 0 ? 1f : 0.8f;
            float ua = katman == 0 ? ta : 1 - ta;
            float ub = katman == 0 ? tb : 1 - tb;
            float vOn = katman;
            float vArka = 1 - katman;
            if (on) {
                kose(b, m, w, c - TEPE_ON, u0, yo0 + 0.12, kam, k0, on0 * a, ua, vOn);
                kose(b, m, w, c - TEPE_ON, u1, yo1 + 0.12, kam, k1, on1 * a, ub, vOn);
                kose(b, m, w, c - TEPE_ORTA, u1, ym1 + 0.12, kam, k1, orta1 * a, ub, 0.5f);
                kose(b, m, w, c - TEPE_ORTA, u0, ym0 + 0.12, kam, k0, orta0 * a, ua, 0.5f);
            }
            if (arka) {
                kose(b, m, w, c - TEPE_ORTA, u0, ym0 + 0.10, kam, k0, orta0 * a, ua, 0.5f);
                kose(b, m, w, c - TEPE_ORTA, u1, ym1 + 0.10, kam, k1, orta1 * a, ub, 0.5f);
                kose(b, m, w, c - arka1, u1, ya1 + 0.10, kam, k1, 0f, ub, vArka);
                kose(b, m, w, c - arka0, u0, ya0 + 0.10, kam, k0, 0f, ua, vArka);
            }
        }
    }

    /** Ön yüz çizgisi: {@code s} {@link #CIZGI_ALT}–{@link #TEPE_ON}, yüzeyin 0,06 üstünde; alfa tepede tam, aşağıda %40. */
    private static void onYuz(BufferBuilder b, Matrix4f m, Dalga w, double t, double c, double u0, double u1, float ta, float tb,
            Vec3 kam, float k0, float k1, float a0, float a1) {
        double y00 = yukseklik(w, t, c, CIZGI_ALT, u0);
        double y01 = yukseklik(w, t, c, CIZGI_ALT, u1);
        double y11 = yukseklik(w, t, c, TEPE_ON, u1);
        double y10 = yukseklik(w, t, c, TEPE_ON, u0);
        if (Double.isNaN(y00) || Double.isNaN(y01) || Double.isNaN(y11) || Double.isNaN(y10)) {
            return;
        }
        kose(b, m, w, c - CIZGI_ALT, u0, y00 + 0.06, kam, k0, a0 * 0.4f, ta, 1f);
        kose(b, m, w, c - CIZGI_ALT, u1, y01 + 0.06, kam, k1, a1 * 0.4f, tb, 1f);
        kose(b, m, w, c - TEPE_ON, u1, y11 + 0.06, kam, k1, a1, tb, 0f);
        kose(b, m, w, c - TEPE_ON, u0, y10 + 0.06, kam, k0, a0, ta, 0f);
    }

    /** Yanal gürültü 0..1: üç sürüklenen sinüs (birkaç blokluk öbek + pürüz); {@code faz} ayrı kenarların desenlerini ayırır. */
    private static double gurultu(double u, double t, double faz) {
        return 0.5 + 0.28 * Math.sin(u * 0.83 + t * 0.05 + faz) + 0.14 * Math.sin(u * 2.31 - t * 0.08 + 1.7 * faz)
                + 0.08 * Math.sin(u * 5.3 + t * 0.11 + 2.9 * faz);
    }

    /** Cephenin {@code s} blok arkasında, yanal {@code u}'daki yüzey (mutlak Y); su yoksa NaN. */
    private static double yukseklik(Dalga w, double t, double c, double s, double u) {
        double x = w.x0() + w.yonX() * (c - s) - w.yonZ() * u;
        double z = w.z0() + w.yonZ() * (c - s) + w.yonX() * u;
        return w.yuzey(x, z, t);
    }

    /** Dokulu köşe: cephe ekseninde {@code boyuna}, yanal {@code u}, mutlak {@code y}; {@code (tu, tv)} köpük sprite'ı içinde 0..1. */
    private static void kose(BufferBuilder b, Matrix4f m, Dalga w, double boyuna, double u, double y, Vec3 kam, float k, float a,
            float tu, float tv) {
        b.vertex(m, (float) (w.x0() + w.yonX() * boyuna - w.yonZ() * u - kam.x), (float) (y - kam.y),
                (float) (w.z0() + w.yonZ() * boyuna + w.yonX() * u - kam.z)).uv(ku0 + (ku1 - ku0) * tu, kv0 + (kv1 - kv0) * tv)
                .color(k, k, k, a).endVertex();
    }
}
