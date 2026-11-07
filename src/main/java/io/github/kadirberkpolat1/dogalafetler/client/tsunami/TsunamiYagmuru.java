package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.su.SuGizleme;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

/**
 * Tsunaminin kendi yağmuru (B.6, tasarım notu §2 "Özel yağmur"): vanilla yağmur kullanılmaz; dalga yönüne 25° eğik, vanilla'dan
 * 1,5 kat uzun ince çizgiler iki katmanda (yakın hızlı ve parlak, uzak yavaş ve soluk) düşer; gerçek bir yüzeye (bloğun
 * çarpışma şekli, görünen gerçek sıvı, sanal su; hangisi yüksekse) üstten değen yakın damla büyüyen bir halka bırakır.
 * Gri-mavi sis görüşü %70'e indirir. Cephe 24 blok içindeyken yerden gelen titreme (±0,3°·seviye), dalganın içinde ±8°
 * yuvarlanma. Damla sayısı kaliteye bağlı (N13: Düşük 400 … Ultra 2600 /sn); yağmur AKTIF ve BITIS boyunca, 3 sn'de girer
 * ve çıkar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class TsunamiYagmuru {
    static final int MAKS_DAMLA = 3200;
    static final int MAKS_HALKA = 480;
    private static final double EGIM = Math.tan(Math.toRadians(25));
    private static final int HALKA_OMRU = 8;

    private static final float[] DX = new float[MAKS_DAMLA];
    private static final float[] DY = new float[MAKS_DAMLA];
    private static final float[] DZ = new float[MAKS_DAMLA];
    private static final float[] VY = new float[MAKS_DAMLA];
    private static final boolean[] YAKIN = new boolean[MAKS_DAMLA];
    private static final float[] HX = new float[MAKS_HALKA];
    private static final float[] HY = new float[MAKS_HALKA];
    private static final float[] HZ = new float[MAKS_HALKA];
    private static final int[] HYAS = new int[MAKS_HALKA];
    private static int damla;
    private static int halka;
    private static double yogunluk;
    private static double birikim;
    private static float yonX = 1;
    private static float yonZ;
    private static final BlockPos.MutableBlockPos P = new BlockPos.MutableBlockPos();
    private static int seviye = 1;
    private static ClientLevel sonLevel;
    /** Yalnız otomasyonun kök neden ölçümü için: yağmuru kapatır. */
    static volatile boolean kapali;
    private static final RandomSource RASTGELE = RandomSource.create();

    private TsunamiYagmuru() {
    }

    /** Otomasyon ölçümü: canlı damla ve halka sayısı, havada asılı halka ({@link #havadaHalka}), şimdiki yoğunluk. */
    static String ozet() {
        return "damla=" + damla + " halka=" + halka + " havada=" + havadaHalka() + " yogunluk="
                + String.format(java.util.Locale.ROOT, "%.2f", yogunluk);
    }

    /**
     * Yüzeyi 0,25 bloktan uzak halka: halkanın altındaki hücrede çarpışma şeklinin o noktadaki tepesi, görünen gerçek sıvı ya da
     * sanal su yüzeyi halkanın yüksekliğinde değil. Yalnız otomasyon sorar ({@link #ozet}); halka ömrü (8 tick) boyunca
     * kayan dalga yüzeyi payı 0,25.
     */
    private static int havadaHalka() {
        ClientLevel l = Minecraft.getInstance().level;
        if (l == null) {
            return 0;
        }
        int n = 0;
        for (int i = 0; i < halka; i++) {
            double y = HY[i] - 0.02;
            double yuzey = carpma(l, HX[i], y + 0.25, y - 0.25, HZ[i]);
            if (Double.isNaN(yuzey) || yuzey == YagmurCarpmasi.YAN || Math.abs(yuzey - y) > 0.25) {
                n++;
            }
        }
        return n;
    }

    static int saniyeBasinaDamla() {
        return switch (IstemciAyarlari.efektKalitesi()) {
            case DUSUK -> 400;
            case ORTA -> 900;
            case YUKSEK -> 1600;
            case ULTRA -> 2600;
        };
    }

    private static boolean aktif() {
        if (kapali) {
            return false;
        }
        return IstemciAfetDurumu.aktif().filter(p -> p.tur() == AfetTuru.TSUNAMI
                && (p.asama() == AfetAsamasi.AKTIF || p.asama() == AfetAsamasi.BITIS) && p.ozel().getBoolean("yagmur"))
                .map(TsunamiYagmuru::oku).isPresent();
    }

    private static AfetDurumPaketi oku(AfetDurumPaketi p) {
        yonX = p.ozel().getFloat("yx");
        yonZ = p.ozel().getFloat("yz");
        seviye = p.seviye();
        return p;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l != sonLevel) {
            sonLevel = l;
            damla = 0;
            halka = 0;
            yogunluk = 0;
        }
        if (l == null || mc.isPaused()) {
            return;
        }
        yogunluk = Mth.clamp(yogunluk + (aktif() ? 1 / 60.0 : -1 / 60.0), 0, 1);
        Vec3 kam = mc.gameRenderer.getMainCamera().getPosition();
        ilerlet(l);
        if (yogunluk <= 0) {
            return;
        }
        birikim += saniyeBasinaDamla() / 20.0 * yogunluk;
        for (; birikim >= 1 && damla < MAKS_DAMLA; birikim--) {
            boolean yakin = RASTGELE.nextFloat() < 0.6f;
            double r = yakin ? 2 + 12 * Math.sqrt(RASTGELE.nextDouble()) : 14 + 26 * Math.sqrt(RASTGELE.nextDouble());
            double a = RASTGELE.nextDouble() * Math.PI * 2;
            float vy = yakin ? -1.15f : -0.8f;
            // Eğim düşüş boyunca kaymayı önceden telafi eder: damla kameranın çevresine iner.
            double yukari = (yakin ? 10 : 14) + 8 * RASTGELE.nextDouble();
            double kayma = yukari * EGIM;
            double x = kam.x + Math.cos(a) * r - yonX * kayma;
            double z = kam.z + Math.sin(a) * r - yonZ * kayma;
            double y = kam.y + yukari;
            // Çatı, ağaç ya da çıkıntı altında (sütunun tepesinin altında) damla doğmaz: içeride ve mağarada yağmur yok.
            if (y < l.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z))) {
                continue;
            }
            DX[damla] = (float) x;
            DZ[damla] = (float) z;
            DY[damla] = (float) y;
            VY[damla] = vy;
            YAKIN[damla] = yakin;
            damla++;
        }
        birikim = Math.min(birikim, 1);
        if (l.getGameTime() % 8 == 0) {
            l.playLocalSound(kam.x, kam.y, kam.z, SoundEvents.WEATHER_RAIN, SoundSource.WEATHER, (float) (0.35 * yogunluk),
                    0.85f, false);
        }
    }

    /**
     * Damlaları bir tick ilerletir; gerçek bir yüzeye üstten değen yakın damla halka bırakır ({@link #carpma}), yandan
     * çarpan halkasız söner.
     */
    private static void ilerlet(ClientLevel l) {
        for (int i = 0; i < halka; ) {
            if (++HYAS[i] >= HALKA_OMRU) {
                halkaSil(i);
            } else {
                i++;
            }
        }
        for (int i = 0; i < damla; ) {
            float vy = VY[i];
            double once = DY[i];
            DY[i] += vy;
            DX[i] += (float) (yonX * -vy * EGIM);
            DZ[i] += (float) (yonZ * -vy * EGIM);
            double yuzey = carpma(l, DX[i], once, DY[i], DZ[i]);
            if (Double.isNaN(yuzey)) {
                i++;
                continue;
            }
            if (yuzey != YagmurCarpmasi.YAN && YAKIN[i] && halka < MAKS_HALKA) {
                HX[halka] = DX[i];
                HY[halka] = (float) yuzey + 0.02f;
                HZ[halka] = DZ[i];
                HYAS[halka] = 0;
                halka++;
            }
            damlaSil(i);
        }
    }

    /**
     * Damlanın {@code once}'den {@code y}'ye inerken çarptığı yüzey ({@link YagmurCarpmasi#karar}). Bloklar gerçekten taranır
     * (yolundaki en çok iki hücre): çarpışma şeklinin damlanın düştüğü noktadaki tepesi (çit direğinin yanından geçen damla
     * yere iner) ve görünen gerçek sıvının yüzeyi; çekilmenin gizlediği gerçek su sayılmaz ({@link SuGizleme#gizliMi}). Yükseklik
     * haritası yalnız ucuz bir kapıdır: tepesinin bir blok üstünden yukarıdaki damlada hücre, 24 blok üstünde sanal su
     * sorulmaz (sanal su zeminden en çok ~20 blok yükselir). Haritaya bakılarak konan halka, gizli suyun ve ince direğin
     * üstünde havada asılı kalıyordu.
     */
    private static double carpma(ClientLevel l, double x, double once, double y, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int tepe = l.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        double su = y > tepe + 24 ? Double.NaN : SanalSu.suSeviyesi(l, bx, bz);
        double ust = YagmurCarpmasi.YAN;
        if (y <= tepe + 1) {
            for (int by = Mth.floor(once); by >= Mth.floor(y); by--) {
                P.set(bx, by, bz);
                BlockState s = l.getBlockState(P);
                double h = YagmurCarpmasi.YAN;
                var sekil = s.getCollisionShape(l, P);
                if (!sekil.isEmpty()) {
                    h = by + sekil.max(Direction.Axis.Y, z - bz, x - bx);
                }
                FluidState f = s.getFluidState();
                if (!f.isEmpty() && !SuGizleme.gizliMi(l, P, f)) {
                    h = Math.max(h, by + f.getHeight(l, P));
                }
                if (h != YagmurCarpmasi.YAN) {
                    ust = h;
                    break;
                }
            }
        }
        return YagmurCarpmasi.karar(once, y, ust, su);
    }

    private static void damlaSil(int i) {
        damla--;
        DX[i] = DX[damla];
        DY[i] = DY[damla];
        DZ[i] = DZ[damla];
        VY[i] = VY[damla];
        YAKIN[i] = YAKIN[damla];
    }

    private static void halkaSil(int i) {
        halka--;
        HX[i] = HX[halka];
        HY[i] = HY[halka];
        HZ[i] = HZ[halka];
        HYAS[i] = HYAS[halka];
    }

    @SubscribeEvent
    public static void ciz(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || (damla == 0 && halka == 0)) {
            return;
        }
        Vec3 kam = e.getCamera().getPosition();
        float pt = e.getPartialTick();
        Matrix4f m = e.getPoseStack().last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float a = (float) yogunluk;
        for (int i = 0; i < damla; i++) {
            float vy = VY[i];
            float hx = (float) (yonX * -vy * EGIM);
            float hz = (float) (yonZ * -vy * EGIM);
            float x = (float) (DX[i] + hx * pt - kam.x);
            float y = DY[i] + vy * pt - (float) kam.y;
            float z = (float) (DZ[i] + hz * pt - kam.z);
            // Vanilla yağmur çizgisinin ~1,5 katı: yakın damla 1,8 blok, uzak 1,3 blok.
            float boy = YAKIN[i] ? 1.8f : 1.3f;
            float hiz = (float) Math.sqrt(hx * hx + vy * vy + hz * hz);
            float kx = -hx / hiz * boy;
            float ky = -vy / hiz * boy;
            float kz = -hz / hiz * boy;
            // Kameraya bakan yan vektör: (düşüş yönü) × (kameraya yön).
            float sx = ky * -z - kz * -y;
            float sz = kx * -y - ky * -x;
            float sy = kz * -x - kx * -z;
            float sb = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sb < 1e-4f) {
                continue;
            }
            float w = (YAKIN[i] ? 0.022f : 0.016f) / sb;
            sx *= w;
            sy *= w;
            sz *= w;
            float alfa = (YAKIN[i] ? 0.42f : 0.2f) * a;
            b.vertex(m, x - sx, y - sy, z - sz).color(0.78f, 0.84f, 0.9f, alfa).endVertex();
            b.vertex(m, x + sx, y + sy, z + sz).color(0.78f, 0.84f, 0.9f, alfa).endVertex();
            b.vertex(m, x + kx + sx, y + ky + sy, z + kz + sz).color(0.7f, 0.78f, 0.86f, 0f).endVertex();
            b.vertex(m, x + kx - sx, y + ky - sy, z + kz - sz).color(0.7f, 0.78f, 0.86f, 0f).endVertex();
        }
        for (int i = 0; i < halka; i++) {
            float t = (HYAS[i] + pt) / HALKA_OMRU;
            float r = 0.06f + 0.32f * t;
            float alfa = 0.5f * (1 - t) * a;
            float x = (float) (HX[i] - kam.x);
            float y = (float) (HY[i] - kam.y);
            float z = (float) (HZ[i] - kam.z);
            for (int s = 0; s < 8; s++) {
                double a0 = s * Math.PI / 4;
                double a1 = (s + 1) * Math.PI / 4;
                float c0 = (float) Math.cos(a0);
                float s0 = (float) Math.sin(a0);
                float c1 = (float) Math.cos(a1);
                float s1 = (float) Math.sin(a1);
                float ic = r - 0.025f;
                float dis = r + 0.025f;
                b.vertex(m, x + c0 * ic, y, z + s0 * ic).color(0.85f, 0.9f, 0.95f, alfa).endVertex();
                b.vertex(m, x + c0 * dis, y, z + s0 * dis).color(0.85f, 0.9f, 0.95f, alfa).endVertex();
                b.vertex(m, x + c1 * dis, y, z + s1 * dis).color(0.85f, 0.9f, 0.95f, alfa).endVertex();
                b.vertex(m, x + c1 * ic, y, z + s1 * ic).color(0.85f, 0.9f, 0.95f, alfa).endVertex();
            }
        }
        Tesselator.getInstance().end();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Gri-mavi sis: görüş %70. */
    @SubscribeEvent
    public static void sis(ViewportEvent.RenderFog e) {
        if (yogunluk > 0 && e.getMode() == FogRenderer.FogMode.FOG_TERRAIN) {
            float k = (float) (1 - 0.3 * yogunluk);
            e.setFarPlaneDistance(e.getFarPlaneDistance() * k);
            e.setNearPlaneDistance(e.getNearPlaneDistance() * k);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void sisRengi(ViewportEvent.ComputeFogColor e) {
        if (yogunluk > 0) {
            float k = (float) (0.6 * yogunluk);
            e.setRed(Mth.lerp(k, e.getRed(), 0.42f));
            e.setGreen(Mth.lerp(k, e.getGreen(), 0.5f));
            e.setBlue(Mth.lerp(k, e.getBlue(), 0.57f));
        }
    }

    /** Cephe 24 blok içinde yaklaşırken titreme, dalganın içinde yuvarlanma. */
    @SubscribeEvent
    public static void kamera(ViewportEvent.ComputeCameraAngles e) {
        SanalSuDunyasi d = SanalSu.istemci;
        Minecraft mc = Minecraft.getInstance();
        if (d == null || d.dalgalar().isEmpty() || mc.player == null || IstemciAfetDurumu.aktif().isEmpty()) {
            return;
        }
        double t = SanalSu.istemciSaati.getAsDouble() + e.getPartialTick();
        Vec3 k = e.getCamera().getPosition();
        double titreme = 0;
        for (Dalga w : d.dalgalar().values()) {
            double s = w.cepheyeUzaklik(k.x, k.z, t);
            double yan = Math.abs(-(k.x - w.x0()) * w.yonZ() + (k.z - w.z0()) * w.yonX());
            if (s > -24 && s < 0 && yan < w.genislik() / 2) {
                titreme = Math.max(titreme, 0.3 * seviye * (1 + s / 24));
            }
        }
        float roll = (float) (titreme * Math.sin(t * 1.7) + titreme * 0.5 * Math.sin(t * 3.1));
        if (SanalSu.itmeHesapla(mc.player, new double[2]) != null && SanalSu.batiklikOrani(mc.player) > 0.5) {
            roll += (float) (8 * Math.sin(t * 0.25));
        }
        if (roll != 0) {
            e.setRoll(e.getRoll() + roll);
        }
    }
}
