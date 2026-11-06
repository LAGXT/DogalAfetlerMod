package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Tsunami dalgasının köpük katmanı (tasarım notu §2 "Köpük"): ortak su renderer'ının üstüne, tsunami sürerken her dalganın
 * cephesi boyunca üç şerit çizilir: tepe boyunca beyaz şerit, ön yüzden aşağı akan kesik köpük çizgileri ve cephe tabanında
 * yuvarlanan beyaz bore. Yükseklik dalganın analitik yüzeyinden ({@link Dalga#yuzey}) alınır; parçacık değil geometri
 * olduğu için maliyeti cephe uzunluğuyla doğrusaldır. Kalite (N13): Düşük'te yalnız tepe şeridi, 2 blok adım; diğerlerinde
 * üç şerit, 1 blok adım (kameradan 64 blok ötede 2). Renk gün ışığıyla ölçeklenir.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class TsunamiKopugu {
    /** Tepe: yüzey bu {@code s} aralığında en yüksek ({@code Dalga} ön yüzü 2,5 blok). */
    private static final double TEPE_ON = 1.4;
    private static final double TEPE_ARKA = 4.5;
    private static int sonSegment;

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
        float pt = e.getPartialTick();
        double t = IstemciDonem.saat(pt);
        Vec3 kam = e.getCamera().getPosition();
        boolean dusuk = IstemciAyarlari.efektKalitesi() == EfektKalitesi.DUSUK;
        float isik = Mth.clamp(mc.level.getSkyDarken(pt), 0.3f, 1f);
        Matrix4f m = e.getPoseStack().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int segment = 0;
        for (Dalga w : d.dalgaDizisi()) {
            if (w.bitti(t) || w.yukseklik() < 1.5) {
                continue;
            }
            segment += dalga(b, m, w, t, kam, dusuk, isik);
        }
        if (segment > 0) {
            Tesselator.getInstance().end();
        } else {
            b.end().release();
        }
        sonSegment = segment;
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
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
            n += serit(b, m, w, t, c, u, u1, kam, dusuk, isik);
            u = u1;
        }
        return n;
    }

    /** Yanal {@code [u0, u1]} aralığında üç şerit; yüzeyi olmayan (yan kenar) aralık atlanır. */
    private static int serit(BufferBuilder b, Matrix4f m, Dalga w, double t, double c, double u0, double u1, Vec3 kam,
            boolean dusuk, float isik) {
        double h0 = yukseklik(w, t, c, TEPE_ON + 1.1, u0);
        double h1 = yukseklik(w, t, c, TEPE_ON + 1.1, u1);
        if (Double.isNaN(h0) || Double.isNaN(h1)) {
            return 0;
        }
        float k = isik;
        // Tepe şeridi: yüzeyin 0,12 blok üstünde, önde opak beyaz, arkaya doğru saydamlaşır.
        dortgen(b, m, w, t, c, u0, u1, TEPE_ON, TEPE_ON + 1.1, 0.12, kam, k, 0.92f, 0.92f);
        dortgen(b, m, w, t, c, u0, u1, TEPE_ON + 1.1, TEPE_ARKA, 0.10, kam, k, 0.92f, 0f);
        if (dusuk) {
            return 1;
        }
        // Ön yüz çizgileri: yanal konuma bağlı sabit desenle kesik (her ~3 bloktan biri boş), aşağı doğru söner.
        double desen = Math.sin(u0 * 1.7 + Math.floor(t / 6) * 0.9) * Math.sin(u0 * 0.43 + 1.3);
        if (desen > -0.35) {
            float a = (float) (0.55 + 0.3 * desen);
            dortgen(b, m, w, t, c, u0, u1, 0.35, TEPE_ON, 0.06, kam, k, a * 0.4f, a);
        }
        // Bore: cephe tabanında yuvarlanan beyaz basamak, yüksekliği dalganın %12'si (en çok 1,6 blok), titreşen üst kenar.
        double bore = Math.min(1.6, 0.12 * w.yukseklik()) * (0.8 + 0.2 * Math.sin(t * 0.6 + u0 * 0.8));
        double taban = w.taban() + 0.05;
        double s0 = c + 0.6;
        double s1 = c - 0.4;
        float x0 = (float) (w.x0() + w.yonX() * s0 - w.yonZ() * u0 - kam.x);
        float z0 = (float) (w.z0() + w.yonZ() * s0 + w.yonX() * u0 - kam.z);
        float x1 = (float) (w.x0() + w.yonX() * s0 - w.yonZ() * u1 - kam.x);
        float z1 = (float) (w.z0() + w.yonZ() * s0 + w.yonX() * u1 - kam.z);
        float x2 = (float) (w.x0() + w.yonX() * s1 - w.yonZ() * u1 - kam.x);
        float z2 = (float) (w.z0() + w.yonZ() * s1 + w.yonX() * u1 - kam.z);
        float x3 = (float) (w.x0() + w.yonX() * s1 - w.yonZ() * u0 - kam.x);
        float z3 = (float) (w.z0() + w.yonZ() * s1 + w.yonX() * u0 - kam.z);
        float yt = (float) (taban - kam.y);
        float yu = (float) (taban + bore - kam.y);
        b.vertex(m, x0, yt, z0).color(k, k, k, 0.75f).endVertex();
        b.vertex(m, x1, yt, z1).color(k, k, k, 0.75f).endVertex();
        b.vertex(m, x1, yu, z1).color(k, k, k, 0.35f).endVertex();
        b.vertex(m, x0, yu, z0).color(k, k, k, 0.35f).endVertex();
        b.vertex(m, x0, yu, z0).color(k, k, k, 0.35f).endVertex();
        b.vertex(m, x1, yu, z1).color(k, k, k, 0.35f).endVertex();
        b.vertex(m, x2, yu, z2).color(k, k, k, 0.6f).endVertex();
        b.vertex(m, x3, yu, z3).color(k, k, k, 0.6f).endVertex();
        return 1;
    }

    /** Cephenin {@code s} blok arkasında, yanal {@code u}'daki yüzey (mutlak Y); su yoksa NaN. */
    private static double yukseklik(Dalga w, double t, double c, double s, double u) {
        double x = w.x0() + w.yonX() * (c - s) - w.yonZ() * u;
        double z = w.z0() + w.yonZ() * (c - s) + w.yonX() * u;
        return w.yuzey(x, z, t);
    }

    /** Yüzeyi izleyen dörtgen: {@code [sOn, sArka] × [u0, u1]}, yüzeyin {@code ust} blok üstünde, alfa önden arkaya. */
    private static void dortgen(BufferBuilder b, Matrix4f m, Dalga w, double t, double c, double u0, double u1, double sOn,
            double sArka, double ust, Vec3 kam, float k, float aOn, float aArka) {
        double y00 = yukseklik(w, t, c, sOn, u0);
        double y01 = yukseklik(w, t, c, sOn, u1);
        double y11 = yukseklik(w, t, c, sArka, u1);
        double y10 = yukseklik(w, t, c, sArka, u0);
        if (Double.isNaN(y00) || Double.isNaN(y01) || Double.isNaN(y11) || Double.isNaN(y10)) {
            return;
        }
        kose(b, m, w, c - sOn, u0, y00 + ust, kam, k, aOn);
        kose(b, m, w, c - sOn, u1, y01 + ust, kam, k, aOn);
        kose(b, m, w, c - sArka, u1, y11 + ust, kam, k, aArka);
        kose(b, m, w, c - sArka, u0, y10 + ust, kam, k, aArka);
    }

    private static void kose(BufferBuilder b, Matrix4f m, Dalga w, double boyuna, double u, double y, Vec3 kam, float k,
            float a) {
        b.vertex(m, (float) (w.x0() + w.yonX() * boyuna - w.yonZ() * u - kam.x), (float) (y - kam.y),
                (float) (w.z0() + w.yonZ() * boyuna + w.yonX() * u - kam.z)).color(k, k, k, a).endVertex();
    }
}
