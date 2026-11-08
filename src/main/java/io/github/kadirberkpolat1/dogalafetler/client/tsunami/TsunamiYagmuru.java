package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.su.SuGizleme;
import io.github.kadirberkpolat1.dogalafetler.client.su.SuRenderer;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
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
 * 1,5 kat uzun ince çizgiler iki katmanda (yakın hızlı ve parlak, uzak yavaş ve soluk) düşer; gerçek bir yüzeye (katı bloğun
 * görünen şekli, görünen gerçek sıvı, bu karede çizilen sanal su; hangisi yüksekse) üstten değen yakın damla, halkanın
 * sığdığı düz yüzeyde büyüyen bir halka bırakır; sanal sudaki halka çizilen su ağıyla iner kalkar. Gri-mavi sis görüşü %70'e
 * indirir. Cephe 24 blok içindeyken yerden gelen titreme (±0,3°·seviye), dalganın içinde ±8° yuvarlanma. Damla sayısı
 * kaliteye bağlı (N13: Düşük 400 … Ultra 2600 /sn); yağmur AKTIF ve BITIS boyunca, 3 sn'de girer ve çıkar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class TsunamiYagmuru {
    static final int MAKS_DAMLA = 3200;
    static final int MAKS_HALKA = 480;
    private static final double EGIM = Math.tan(Math.toRadians(25));
    private static final int HALKA_OMRU = 8;
    /** Halkanın en büyük dış yarıçapı (ömrünün sonunda 0,06 + 0,32 + 0,025 blok). */
    static final float HALKA_YARICAPI = 0.405f;
    /** Halkanın dört ucundaki yüzeyin merkezdekinden en çok farkı (iki piksel; kıyıda su 0,89, kum 1). */
    private static final double DESTEK_PAYI = 0.125;

    private static final float[] DX = new float[MAKS_DAMLA];
    private static final float[] DY = new float[MAKS_DAMLA];
    private static final float[] DZ = new float[MAKS_DAMLA];
    private static final float[] VY = new float[MAKS_DAMLA];
    private static final boolean[] YAKIN = new boolean[MAKS_DAMLA];
    private static final float[] HX = new float[MAKS_HALKA];
    private static final float[] HY = new float[MAKS_HALKA];
    private static final float[] HZ = new float[MAKS_HALKA];
    private static final int[] HYAS = new int[MAKS_HALKA];
    private static final boolean[] HSANAL = new boolean[MAKS_HALKA];
    private static int damla;
    private static int halka;
    private static double yogunluk;
    private static double birikim;
    private static float yonX = 1;
    private static float yonZ;
    private static final BlockPos.MutableBlockPos P = new BlockPos.MutableBlockPos();
    private static int seviye = 1;
    private static ClientLevel sonLevel;
    /** Son {@link #carpma} sonucu sanal su yüzeyi mi. */
    private static boolean sonSanal;
    /** Yalnız otomasyonun kök neden ölçümü için: yağmuru kapatır. */
    static volatile boolean kapali;
    private static final RandomSource RASTGELE = RandomSource.create();

    private TsunamiYagmuru() {
    }

    /**
     * Otomasyon ölçümü: canlı damla ve halka sayısı, kabul ölçütü ({@link #olcut}), şimdiki yoğunluk, çizilen yakın su
     * ağının yaşı ({@link SuRenderer#agYasi}).
     */
    static String ozet() {
        ClientLevel l = Minecraft.getInstance().level;
        return "damla=" + damla + " halka=" + halka + " " + olcut() + " yogunluk="
                + String.format(Locale.ROOT, "%.2f", yogunluk) + (l == null ? "" : " " + SuRenderer.agYasi(l.getGameTime()));
    }

    /** Bir halkanın ölçüt durumu (yalnız otomasyon). */
    private record HalkaDurumu(String kaynak, String destek, double x, double y, double z, double gozFark, boolean gorunur,
            boolean cizilen, int kenar, boolean dalgaAlti) {
    }

    /**
     * Otomasyon kabul ölçütü (inceleme Af2, Af13), halkayı koyan fizik sorgusundan ayrı, çizim durumuna bakan yoldan: kaynak
     * sayıları ({@code blok}, {@code sivi}, {@code sanal}, yüzeyi bulunamayan {@code yok}; su yüzeyindeki halka kayboldu mu,
     * Af12); {@code goz_ustu} göz hizasının üstünde olup kameradan görünen halka (merkezine ya da bir ucuna ışın engelsiz;
     * yüzeyin tepesi alttan görünmez, halka asılı durur); {@code cizimsiz} altında bu karede çizilen yüzey olmayan halka
     * (bloğun çizim şekli yok, gerçek suyun üst yüzü çizilmiyor, sanal su halkası o sütunda çizilen su üstünde değil:
     * {@link SuRenderer#cizilenUst}); {@code kenarsiz} dört ucundan birinin altında 0,5 blok içinde çizilen yüzey olmayan (ve
     * ucu bir bloka ya da suya gömülü olmayan) halka. {@code dalga_alti} üstünde sanal su olan halka (çizilen suyun altındaki
     * tick başında söner, karede kalanı aynı karede yenilenen ağın ya da henüz çizilmeyen sanal suyun altındadır; havada
     * değil), üç ana sayıya girmez; {@code goz_ustu_ham} göz hizasının üstündeki bütün halkalar.
     */
    static String olcut() {
        int blok = 0;
        int sivi = 0;
        int sanal = 0;
        int yok = 0;
        int gozUstu = 0;
        int gozUstuHam = 0;
        int cizimsiz = 0;
        int kenarsiz = 0;
        int dalgaAlti = 0;
        for (HalkaDurumu d : halkaDurumlari()) {
            switch (d.kaynak()) {
                case "BLOK" -> blok++;
                case "SIVI" -> sivi++;
                case "SANAL" -> sanal++;
                default -> yok++;
            }
            gozUstuHam += d.gozFark() > 0 ? 1 : 0;
            if (d.dalgaAlti()) {
                dalgaAlti++;
                continue;
            }
            gozUstu += d.gozFark() > 0 && d.gorunur() ? 1 : 0;
            cizimsiz += d.cizilen() ? 0 : 1;
            kenarsiz += d.kenar() < 4 ? 1 : 0;
        }
        return "blok=" + blok + " sivi=" + sivi + " sanal=" + sanal + " yok=" + yok + " goz_ustu=" + gozUstu + " cizimsiz="
                + cizimsiz + " kenarsiz=" + kenarsiz + " dalga_alti=" + dalgaAlti + " goz_ustu_ham=" + gozUstuHam;
    }

    /** Otomasyon: her halka için kaynak, destek, dünya konumu, göz hizasına göre yükseklik ve ölçüt durumu. */
    static List<String> halkaDokumu() {
        List<String> satirlar = new ArrayList<>();
        for (HalkaDurumu d : halkaDurumlari()) {
            satirlar.add(String.format(Locale.ROOT, "%s %s (%.2f %.2f %.2f) goz%+.2f gorunur=%s cizilen=%s kenar=%d/4%s",
                    d.kaynak(), d.destek(), d.x(), d.y(), d.z(), d.gozFark(), d.gorunur() ? "E" : "H",
                    d.cizilen() ? "E" : "H", d.kenar(), d.dalgaAlti() ? " dalga_alti" : ""));
        }
        return satirlar;
    }

    private static List<HalkaDurumu> halkaDurumlari() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        List<HalkaDurumu> sonuc = new ArrayList<>();
        if (l == null || mc.player == null) {
            return sonuc;
        }
        Vec3 goz = mc.gameRenderer.getMainCamera().getPosition();
        for (int i = 0; i < halka; i++) {
            double hy = halkaY(i);
            if (Double.isNaN(hy)) {
                continue;
            }
            double x = HX[i];
            double h = hy - 0.02;
            double z = HZ[i];
            int bx = Mth.floor(x);
            int bz = Mth.floor(z);
            double su = SanalSu.suSeviyesi(l, bx, bz);
            double ciz = SuRenderer.cizilenUst(bx, bz);
            String kaynak;
            String destek;
            boolean cizilen;
            boolean dalgaAlti;
            if (HSANAL[i]) {
                kaynak = "SANAL";
                destek = String.format(Locale.ROOT, "su=%.2f cizilen=%.2f", su, ciz);
                cizilen = Math.abs(ciz - h) < 0.01;
                dalgaAlti = ciz > h + 0.05;
            } else {
                P.set(bx, Mth.floor(h - 1e-3), bz);
                BlockState s = l.getBlockState(P);
                FluidState f = s.getFluidState();
                destek = BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString();
                double ust = P.getY() + s.getShape(l, P).max(Direction.Axis.Y, z - bz, x - bx);
                if (!s.getCollisionShape(l, P).isEmpty() && Math.abs(ust - h) <= 0.05) {
                    kaynak = "BLOK";
                    cizilen = s.getRenderShape() != RenderShape.INVISIBLE;
                } else if (!f.isEmpty() && Math.abs(P.getY() + f.getHeight(l, P) - h) <= 0.05) {
                    kaynak = "SIVI";
                    double t = siviUstu(l, P, f);
                    cizilen = !Double.isNaN(t) && t != YagmurCarpmasi.GIZLI;
                } else {
                    kaynak = "YOK";
                    cizilen = false;
                }
                dalgaAlti = su > h + 0.05 || ciz > h + 0.05;
            }
            int kenar = 0;
            for (int u = 0; u < 4; u++) {
                double px = x + (u == 0 ? HALKA_YARICAPI : u == 1 ? -HALKA_YARICAPI : 0);
                double pz = z + (u == 2 ? HALKA_YARICAPI : u == 3 ? -HALKA_YARICAPI : 0);
                kenar += cizilenDestek(l, px, h, pz) ? 1 : 0;
            }
            boolean gorunur = false;
            for (int u = 0; u < 5 && h > goz.y && !gorunur; u++) {
                Vec3 hedef = new Vec3(x + (u == 1 ? 0.36 : u == 2 ? -0.36 : 0), h + 0.03,
                        z + (u == 3 ? 0.36 : u == 4 ? -0.36 : 0));
                gorunur = l.clip(new ClipContext(goz, hedef, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, mc.player))
                        .getType() == HitResult.Type.MISS;
            }
            sonuc.add(new HalkaDurumu(kaynak, destek, x, h, z, h - goz.y, gorunur, cizilen, kenar, dalgaAlti));
        }
        return sonuc;
    }

    /**
     * Ölçüt: halkanın {@code (px, h, pz)} ucunun altında 0,5 blok içinde ya da üstünde çizilen bir yüzey: herhangi bir ağın
     * çizdiği sanal su üstü, bloğun çizilen şekli (çarpışmasız da olsa), üst yüzü çizilen gerçek sıvı. Ucu bir bloka gömülü
     * olan da desteklidir (görünmez, asılı değil).
     */
    private static boolean cizilenDestek(ClientLevel l, double px, double h, double pz) {
        int bx = Mth.floor(px);
        int bz = Mth.floor(pz);
        if (SuRenderer.cizilenUst(bx, bz) >= h - 0.5) {
            return true;
        }
        for (int by = Mth.floor(h); by >= Mth.floor(h - 0.5); by--) {
            P.set(bx, by, bz);
            BlockState s = l.getBlockState(P);
            if (s.getRenderShape() != RenderShape.INVISIBLE
                    && by + s.getShape(l, P).max(Direction.Axis.Y, pz - bz, px - bx) >= h - 0.5) {
                return true;
            }
            FluidState f = s.getFluidState();
            if (!f.isEmpty()) {
                double t = siviUstu(l, P, f);
                if (t != YagmurCarpmasi.GIZLI && t >= h - 0.5) {
                    return true;
                }
            }
        }
        return false;
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
        return IstemciAfetDurumu.aktif(AfetTuru.TSUNAMI).filter(p -> (p.asama() == AfetAsamasi.AKTIF || p.asama() == AfetAsamasi.BITIS) && p.ozel().getBoolean("yagmur"))
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
        ilerlet(l, kam.y);
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
     * Damlaları bir tick ilerletir; gerçek bir yüzeye üstten değen yakın damla göz hizasının altında
     * ({@link YagmurCarpmasi#ustYuzGorunur}) halka bırakır ({@link #carpma}): blok ve gerçek su halkası sığdığı düz yüzeyde
     * ({@link #tabanli}) ve çizilen suyun altında değilse, sanal su halkası dört ucu çizilen suyun üstündeyse
     * ({@link #sanalTabanli}). Yandan çarpan ya da dar tepeye (direk, fener, çatı kenarı) düşen halkasız söner. Sanal su
     * halkası her tick çizilen su üstüne taşınır ({@link #yuzeydeKal}); üstünde çizilen su olan ya da gözün üstünde kalan halka
     * söner.
     */
    private static void ilerlet(ClientLevel l, double gozY) {
        for (int i = 0; i < halka; ) {
            if (++HYAS[i] >= HALKA_OMRU || (HSANAL[i] ? !yuzeydeKal(i) : suAltinda(HX[i], HY[i] - 0.02, HZ[i]))
                    || !YagmurCarpmasi.ustYuzGorunur(HY[i] - 0.02, gozY)) {
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
            if (yuzey != YagmurCarpmasi.YAN && YAKIN[i] && halka < MAKS_HALKA && YagmurCarpmasi.ustYuzGorunur(yuzey, gozY)
                    && (sonSanal ? sanalTabanli(DX[i], yuzey, DZ[i])
                            : tabanli(l, DX[i], yuzey, DZ[i]) && !suAltinda(DX[i], yuzey, DZ[i]))) {
                HX[halka] = DX[i];
                HY[halka] = (float) yuzey + 0.02f;
                HZ[halka] = DZ[i];
                HYAS[halka] = 0;
                HSANAL[halka] = sonSanal;
                halka++;
            }
            damlaSil(i);
        }
    }

    /**
     * Sanal su halkası o sütunda bu karede çizilen su üstünde durur (yakın ağ yenilenince onunla iner kalkar; çizimde
     * {@link #halkaY}); çizilen su kalmayınca ya da bir ucu çizilen suyun kenarından taşınca söner.
     */
    private static boolean yuzeydeKal(int i) {
        double ciz = SuRenderer.cizilenUst(Mth.floor(HX[i]), Mth.floor(HZ[i]));
        if (Double.isNaN(ciz)) {
            return false;
        }
        HY[i] = (float) ciz + 0.02f;
        return sanalTabanli(HX[i], ciz, HZ[i]);
    }

    /**
     * {@code h} yüzeyindeki blok ya da gerçek su halkasının üstünde bu karede çizilen sanal su var: halka batık zeminde kalır,
     * su ağının içinden görünürdü. Çizilmeyen sanal su (yakın ağ kurulmadan) zemini örtmez.
     */
    private static boolean suAltinda(double x, double h, double z) {
        return SuRenderer.cizilenUst(Mth.floor(x), Mth.floor(z)) > h + 0.05;
    }

    /**
     * Sanal su halkasının dört ucu (en büyük yarıçapta) o sütunlarda çizilen suyun üstünde mi
     * ({@link YagmurCarpmasi#sanalUcDestekli}): dalga cephesinde ve kıyıda halka suyun kenarından havaya taşmasın.
     */
    private static boolean sanalTabanli(double x, double h, double z) {
        return sanalUcu(x + HALKA_YARICAPI, h, z) && sanalUcu(x - HALKA_YARICAPI, h, z) && sanalUcu(x, h, z + HALKA_YARICAPI)
                && sanalUcu(x, h, z - HALKA_YARICAPI);
    }

    private static boolean sanalUcu(double px, double h, double pz) {
        return YagmurCarpmasi.sanalUcDestekli(SuRenderer.cizilenUst(Mth.floor(px), Mth.floor(pz)), h, DESTEK_PAYI);
    }

    /**
     * Damlanın {@code once}'den {@code y}'ye inerken çarptığı yüzey ({@link YagmurCarpmasi#karar}); {@link #sonSanal} sanal su
     * mu. Sanal su yüzeyi o sütunda bu karede çizilen su üstüdür ({@link SuRenderer#cizilenUst}); analitik yüzey
     * ({@link SanalSu#suSeviyesi}) yalnız sütunda sanal su var mı kapısıdır. Çizilmeyen sanal suyu (yakın ağ kurulmadan) damla
     * görmez, altındaki görünen zemine ya da gerçek suya iner. Bloklar gerçekten taranır (yolundaki en çok iki hücre,
     * {@link #noktaYuzeyi}). Yükseklik haritası yalnız ucuz bir kapıdır: tepesinin bir blok üstünden yukarıdaki damlada
     * hücre, 24 blok üstünde sanal su sorulmaz (sanal su zeminden en çok ~20 blok yükselir).
     */
    private static double carpma(ClientLevel l, double x, double once, double y, double z) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int tepe = l.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        double su = y > tepe + 24 || Double.isNaN(SanalSu.suSeviyesi(l, bx, bz)) ? Double.NaN : SuRenderer.cizilenUst(bx, bz);
        double ust = YagmurCarpmasi.YAN;
        if (y <= tepe + 1) {
            for (int by = Mth.floor(once); by >= Mth.floor(y); by--) {
                double h = noktaYuzeyi(l, x, by, z);
                if (!Double.isNaN(h)) {
                    ust = h;
                    break;
                }
            }
        }
        double sonuc = YagmurCarpmasi.karar(once, y, ust, su);
        sonSanal = sonuc == su && sonuc != ust;
        return sonuc;
    }

    /**
     * Halkanın dört ucu (en büyük yarıçapta) aynı yüzeyde mi ({@link #DESTEK_PAYI} içinde). Halkadan dar tepeye (çit ve duvar
     * direği, fener) ya da kenara düşen damla halkasız söner: alttan bakınca halka direğin çevresinde gökyüzünün önünde asılı
     * kalıyordu.
     */
    private static boolean tabanli(ClientLevel l, double x, double h, double z) {
        int by = Mth.floor(h - 1e-3);
        return ayniYuzey(l, x + HALKA_YARICAPI, by, z, h) && ayniYuzey(l, x - HALKA_YARICAPI, by, z, h)
                && ayniYuzey(l, x, by, z + HALKA_YARICAPI, h) && ayniYuzey(l, x, by, z - HALKA_YARICAPI, h);
    }

    private static boolean ayniYuzey(ClientLevel l, double px, int by, double pz, double h) {
        return Math.abs(noktaYuzeyi(l, px, by, pz) - h) <= DESTEK_PAYI;
    }

    /**
     * {@code by} hücresinde {@code (px, pz)} noktasının görünen tepesi: katı bloğun çizilen şekli (çit ve duvarın çarpışma kutusu
     * 1,5 blok, görünen direği 1 blok) ya da üst yüzü çizilen gerçek sıvı ({@link #siviUstu}); ikisi de yoksa NaN, yalnız üst
     * yüzü çizilmeyen gerçek su varsa {@link YagmurCarpmasi#GIZLI}.
     */
    private static double noktaYuzeyi(ClientLevel l, double px, int by, double pz) {
        int bx = Mth.floor(px);
        int bz = Mth.floor(pz);
        P.set(bx, by, bz);
        BlockState s = l.getBlockState(P);
        double h = Double.NaN;
        if (!s.getCollisionShape(l, P).isEmpty()) {
            double t = s.getShape(l, P).max(Direction.Axis.Y, pz - bz, px - bx);
            if (t != Double.NEGATIVE_INFINITY) {
                h = by + t;
            }
        }
        FluidState f = s.getFluidState();
        if (!f.isEmpty()) {
            double t = siviUstu(l, P, f);
            if (t == YagmurCarpmasi.GIZLI) {
                return Double.isNaN(h) ? t : h;
            }
            if (!Double.isNaN(t)) {
                h = Double.isNaN(h) ? t : Math.max(h, t);
            }
        }
        return h;
    }

    /**
     * {@code p}'deki gerçek sıvının bu karede çizilen üst yüzü: çekilmenin gizlediği katmandaysa NaN (damla görünmez hücreden
     * geçer, {@link SuGizleme#gizliMi}); üstünde aynı sıvı varsa (gizli katmanın altı, üst yüz çizilmez)
     * {@link YagmurCarpmasi#GIZLI}; yoksa yüzey yüksekliği. {@code p} çağrıdan sonra aynı konumdadır.
     */
    private static double siviUstu(ClientLevel l, BlockPos.MutableBlockPos p, FluidState f) {
        if (SuGizleme.gizliMi(l, p, f)) {
            return Double.NaN;
        }
        boolean ortulu = l.getFluidState(p.move(Direction.UP)).getType().isSame(f.getType());
        p.move(Direction.DOWN);
        return ortulu ? YagmurCarpmasi.GIZLI : p.getY() + f.getHeight(l, p);
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
        HSANAL[i] = HSANAL[halka];
    }

    /**
     * Halkanın bu karedeki yüksekliği: sanal su halkası sütununun bu karede çizilen su üstünde ({@link SuRenderer#cizilenUst};
     * yakın ağ bu karede yenilendiyse yeni yüzeyde), diğeri konduğu yüzeyde. Çizilen su kalmadıysa NaN, halka çizilmez.
     */
    private static double halkaY(int i) {
        return HSANAL[i] ? SuRenderer.cizilenUst(Mth.floor(HX[i]), Mth.floor(HZ[i])) + 0.02 : HY[i];
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
            double hy = halkaY(i);
            if (Double.isNaN(hy)) {
                continue;
            }
            float t = (HYAS[i] + pt) / HALKA_OMRU;
            float r = 0.06f + 0.32f * t;
            float alfa = 0.5f * (1 - t) * a;
            float x = (float) (HX[i] - kam.x);
            float y = (float) (hy - kam.y);
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
        if (d == null || d.dalgalar().isEmpty() || mc.player == null || IstemciAfetDurumu.aktif(AfetTuru.TSUNAMI).isEmpty()) {
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
