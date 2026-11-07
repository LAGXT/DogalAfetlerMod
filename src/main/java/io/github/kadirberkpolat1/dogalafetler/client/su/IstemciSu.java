package io.github.kadirberkpolat1.dogalafetler.client.su;

import com.mojang.blaze3d.shaders.FogShape;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import io.github.kadirberkpolat1.dogalafetler.su.DenizAlani;
import io.github.kadirberkpolat1.dogalafetler.su.MagaraZamani;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import io.github.kadirberkpolat1.dogalafetler.su.SuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.su.SuPaketi;
import io.github.kadirberkpolat1.dogalafetler.su.TazelemePlani;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * İstemcideki sanal su kopyası ({@link SanalSu#istemci}): sunucu paketleri, deniz artışı (çekirdeğin dönem paketinden),
 * deniz alanının yerel hesabı ve tazelenmesi (K0-30), çekilmede chunk yeniden derleme (SU-05), mağara buharı, su altı sisi.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciSu {
    private static final TazelemePlani PLAN = new TazelemePlani(2, 20, 16);
    private static ClientLevel seviye;
    private static SanalSuDunyasi dunya;
    private static SuPaketi bekleyen;
    private static int sayac;
    /** Gerçek suyu gizlenmiş (yeniden derlenmiş) çekilmeler ve hedef. */
    private static Cekilme[] derlenen = new Cekilme[0];
    private static Cekilme[] hedef = new Cekilme[0];
    private static long hedefGelis;
    /** Son su altı sisi (kare tanısı): sıcak pay ve uzak düzlem; kamera suda değilse NaN. */
    private static float sonSicak = Float.NaN;
    private static float sonSisUzak = Float.NaN;

    private IstemciSu() {
    }

    public static Consumer<SuPaketi> paketAlicisi() {
        return paket -> bekleyen = paket;
    }

    private static void hazirla(ClientLevel l) {
        if (l != seviye) {
            seviye = l;
            dunya = SanalSu.yeniDunya(l.getSeaLevel(), SanalSu.istemciYurutucu());
            dunya.canliZemin(SanalSu.canliZemin(l));
            SanalSu.istemciSaati = () -> IstemciDonem.saat(0f);
            SanalSu.istemci = dunya;
            derlenen = new Cekilme[0];
            hedef = new Cekilme[0];
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut e) {
        seviye = null;
        dunya = null;
        bekleyen = null;
        SanalSu.istemci = null;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.player == null) {
            return;
        }
        hazirla(l);
        SuPaketi p = bekleyen;
        if (p != null && p.boyut().equals(l.dimension())) {
            p.uygula(dunya);
            bekleyen = null;
        }
        dunya.denizArtisiAyarla(l.dimension() == Level.OVERWORLD ? IstemciDonem.durum().deniz().mevcutArtis() : 0);
        double t = IstemciDonem.saat(0f);
        if (dunya.alanGerekli()) {
            if (++sayac % 10 == 0) {
                alanGuncelle(mc, l, dunya.alan());
            }
            dunya.alan().isle(SanalSu.okuyucu(l), TazelemePlani.tickButcesi(SuAyarlari.istemci().okumaButcesi.get(), 1, 1.0));
        }
        cekilmeGuncelle(mc, l, t);
        for (int id : dunya.bitenDalgalar(t)) {
            dunya.dalgaKaldir(id);
        }
        buhar(l, t);
        serpinti(mc, l, t);
    }

    /**
     * Dalga tepesi sıçrantısı (SU-28): tepe hattı boyunca beyaz serpinti bulutu (dalga yönünde savrulur), sıçrama damlaları
     * ve kıvrılan dudağın ucundan düşen su; yalnız oyuncuya 96 blok yakın tepe noktaları, kalite çarpanıyla.
     */
    private static void serpinti(Minecraft mc, ClientLevel l, double t) {
        double carpan = parcacikCarpani();
        if (carpan <= 0) {
            return;
        }
        RandomSource r = l.random;
        for (Dalga w : dunya.dalgaDizisi()) {
            if (w.bitti(t) || w.yukseklik() < 2) {
                continue;
            }
            double c = w.cephe(t) - 2.5;
            double mx = w.x0() + w.yonX() * c;
            double mz = w.z0() + w.yonZ() * c;
            int adet = (int) Math.ceil(14 * carpan);
            for (int i = 0; i < adet; i++) {
                double u = (r.nextDouble() - 0.5) * w.genislik() * 0.9;
                double px = mx - w.yonZ() * u;
                double pz = mz + w.yonX() * u;
                if (mc.player.distanceToSqr(px, mc.player.getY(), pz) > 96 * 96) {
                    continue;
                }
                double y = w.yuzey(px, pz, t);
                if (Double.isNaN(y) || y - w.taban() < 1.5) {
                    continue;
                }
                l.addParticle(ParticleTypes.CLOUD, px + r.nextGaussian() * 0.3, y + 0.1, pz + r.nextGaussian() * 0.3,
                        w.yonX() * w.hiz() * 0.6 + r.nextGaussian() * 0.03, 0.05 + r.nextDouble() * 0.12,
                        w.yonZ() * w.hiz() * 0.6 + r.nextGaussian() * 0.03);
                if (r.nextInt(2) == 0) {
                    l.addParticle(ParticleTypes.SPLASH, px, y + 0.2, pz, w.yonX() * 0.3, 0.2, w.yonZ() * 0.3);
                }
                if (r.nextInt(3) == 0) {
                    double boy = Math.min(3.0, 0.28 * (y - w.taban()));
                    l.addParticle(ParticleTypes.FALLING_WATER, px + w.yonX() * boy, y - 0.3 * boy, pz + w.yonZ() * boy, 0, 0, 0);
                }
            }
        }
    }

    /** Kalite ve config çarpanıyla parçacık yoğunluğu. */
    static double parcacikCarpani() {
        double k = switch (IstemciAyarlari.efektKalitesi()) {
            case DUSUK -> 0.4;
            case ORTA -> 0.7;
            case YUKSEK -> 1.0;
            case ULTRA -> 1.5;
        };
        return k * SuAyarlari.istemci().parcacikYogunlugu.get();
    }

    /** Kuruyan mağaranın yüzeyinden beyaz buhar (yüzey alanıyla orantılı). */
    private static void buhar(ClientLevel l, double t) {
        RandomSource r = l.random;
        for (SanalSuDunyasi.Magara m : dunya.magaralar().values()) {
            if (m.asama(t) != MagaraZamani.Asama.BUHARLASIYOR) {
                continue;
            }
            long[] s = m.cep().sutunAnahtarlari();
            double seviye = m.seviye(t);
            int adet = (int) Math.ceil(Math.min(40, s.length * 0.04) * parcacikCarpani());
            for (int i = 0; i < adet; i++) {
                long k = s[r.nextInt(s.length)];
                int x = (int) (k >> 32);
                int z = (int) k;
                if (m.cep().icerir(x, (int) Math.floor(seviye - 0.5), z)) {
                    l.addParticle(ParticleTypes.CLOUD, x + r.nextDouble(), seviye, z + r.nextDouble(), 0, 0.03, 0);
                }
            }
        }
    }

    private static void alanGuncelle(Minecraft mc, ClientLevel l, DenizAlani alan) {
        int r = SuRenderer.lodChunk(mc) + 1;
        int pcx = mc.player.getBlockX() >> 4;
        int pcz = mc.player.getBlockZ() >> 4;
        Set<Long> istenen = new HashSet<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                istenen.add(DenizAlani.anahtar(pcx + dx, pcz + dz));
            }
        }
        long odak = DenizAlani.anahtar(pcx, pcz);
        for (long k : TazelemePlani.yakindanUzaga(istenen, new long[] {odak})) {
            alan.iste((int) (k >> 32), (int) k);
        }
        for (long k : alan.istenenler()) {
            if (!istenen.contains(k)) {
                alan.birak(k);
            }
        }
        for (long k : PLAN.sec(l.getGameTime(), new long[] {DenizAlani.anahtar(pcx, pcz)}, alan.istenenler())) {
            alan.gecersizKil((int) (k >> 32), (int) k);
        }
    }

    /** Uzaktan yeniden yüklenen chunk hemen okunur. */
    @SubscribeEvent
    public static void chunkYuklendi(ChunkEvent.Load e) {
        if (e.getLevel().isClientSide() && dunya != null && dunya.alanGerekli()) {
            dunya.alan().gecersizKil(e.getChunk().getPos().x, e.getChunk().getPos().z);
        }
    }

    /**
     * Çekilme kümesi değişince eski ve yeni bölgelerin su içeren bölümleri bir kez yeniden derlenir (değerle
     * karşılaştırma); Y aralığı gerçek su sütunlarından, alan merkez chunk'ı okuyana kadar (en çok 100 tick) beklenir.
     */
    private static void cekilmeGuncelle(Minecraft mc, ClientLevel l, double t) {
        Cekilme[] simdi = dunya.cekilmeDizisi();
        if (!Arrays.equals(simdi, hedef)) {
            hedef = simdi;
            hedefGelis = l.getGameTime();
        }
        if (Arrays.equals(hedef, derlenen)) {
            return;
        }
        boolean zorla = l.getGameTime() - hedefGelis > 100;
        for (Cekilme c : hedef) {
            if (!zorla && !dunya.alan().yuklu((int) Math.floor(c.x()), (int) Math.floor(c.z()))) {
                return;
            }
        }
        for (Cekilme c : derlenen) {
            kirlet(mc, l, c);
        }
        for (Cekilme c : hedef) {
            kirlet(mc, l, c);
        }
        derlenen = hedef;
    }

    /** Yalnız çemberle kesişen ve gerçek su içeren Y aralığındaki bölümler; bölüm başına bir çağrı. */
    private static void kirlet(Minecraft mc, ClientLevel l, Cekilme c) {
        DenizAlani alan = dunya.alan();
        int r = (int) Math.ceil(c.yaricap());
        int y0 = Integer.MAX_VALUE;
        int y1 = Integer.MIN_VALUE;
        for (int x = (int) Math.floor(c.x()) - r; x <= (int) Math.floor(c.x()) + r; x++) {
            for (int z = (int) Math.floor(c.z()) - r; z <= (int) Math.floor(c.z()) + r; z++) {
                int k = c.gizlenenKatman(x + 0.5, z + 0.5);
                if (k > 0 && alan.su(x, z)) {
                    y0 = Math.min(y0, alan.zemin(x, z) - k - 1);
                    y1 = Math.max(y1, alan.zemin(x, z));
                }
            }
        }
        if (y0 > y1) {
            y0 = l.getSeaLevel() - 8;
            y1 = l.getSeaLevel() + 1;
        }
        for (int sx = ((int) Math.floor(c.x()) - r) >> 4; sx <= ((int) Math.floor(c.x()) + r) >> 4; sx++) {
            for (int sz = ((int) Math.floor(c.z()) - r) >> 4; sz <= ((int) Math.floor(c.z()) + r) >> 4; sz++) {
                double nx = Math.max(sx * 16, Math.min(c.x(), sx * 16 + 16));
                double nz = Math.max(sz * 16, Math.min(c.z(), sz * 16 + 16));
                if ((nx - c.x()) * (nx - c.x()) + (nz - c.z()) * (nz - c.z()) > c.yaricap() * c.yaricap()) {
                    continue;
                }
                for (int sy = y0 >> 4; sy <= y1 >> 4; sy++) {
                    mc.levelRenderer.setSectionDirty(sx, sy, sz);
                }
            }
        }
    }

    static boolean kameraSuda(Camera cam) {
        Minecraft mc = Minecraft.getInstance();
        SanalSuDunyasi d = SanalSu.dunya(mc.level);
        if (d == null || cam.getFluidInCamera() != FogType.NONE) {
            return false;
        }
        Vec3 p = cam.getPosition();
        return !Double.isNaN(d.ustu(p.x, p.y, p.z, IstemciDonem.saat(0f)));
    }

    /** Su altı sisi; Forge değişikliği yalnız iptal edilen olayda uygular. */
    @SubscribeEvent
    public static void sis(ViewportEvent.RenderFog e) {
        if (kameraSuda(e.getCamera())) {
            sonSicak = sicaklik(Minecraft.getInstance().level, e.getCamera().getBlockPosition());
            sonSisUzak = sisUzakligi(sonSicak);
            e.setNearPlaneDistance(-8f);
            e.setFarPlaneDistance(sonSisUzak);
            e.setFogShape(FogShape.SPHERE);
            e.setCanceled(true);
        } else {
            sonSicak = Float.NaN;
            sonSisUzak = Float.NaN;
        }
    }

    /** Su altı sisinin uzak düzlemi: açık suda 56 blok; mağara 10-20 blok olduğu için sıcak suda yakına, en yakın 24 blok (SU-40). */
    static float sisUzakligi(float sicak) {
        return Math.max(24f, 56f - 64f * sicak);
    }

    static float sonSicak() {
        return sonSicak;
    }

    static float sonSisUzak() {
        return sonSisUzak;
    }

    /**
     * Su altı sisinin sıcak payı 0..0,85: blok ışığı var, gökyüzü ışığı yok (mağara, fener). Biyom suyu maviyken 0,5 tavanı sisi
     * ancak mavi-griye çekiyordu (SU-41); blok ışığı ~12'de pay doyar.
     */
    private static float sicaklik(ClientLevel l, BlockPos kp) {
        return Math.min(1f, l.getBrightness(LightLayer.BLOCK, kp) / 15f * 1.3f) * (1 - l.getBrightness(LightLayer.SKY, kp) / 15f)
                * 0.85f;
    }

    @SubscribeEvent
    public static void sisRengi(ViewportEvent.ComputeFogColor e) {
        if (kameraSuda(e.getCamera())) {
            // Biyom su sisi (çoğu biyomda neredeyse siyah lacivert) su rengine kaydırılır, kamera noktasının ışığıyla ölçeklenir:
            // gündüz sığ su turkuaz-mavi ve okunur, gece karanlık kalır (SU-28). Gökyüzü görmeyen ama blok ışığı alan suda
            // (mağara, fener) sis sıcak amber tona kayar.
            ClientLevel l = Minecraft.getInstance().level;
            var biyom = l.getBiome(e.getCamera().getBlockPosition()).value();
            int sis = biyom.getWaterFogColor();
            int su = biyom.getWaterColor();
            float isik = l.getMaxLocalRawBrightness(e.getCamera().getBlockPosition()) / 15f;
            float k = 0.25f + 0.75f * isik;
            float r = ((sis >> 16 & 255) * 0.3f + (su >> 16 & 255) * 0.7f) / 255f * k;
            float g = ((sis >> 8 & 255) * 0.3f + (su >> 8 & 255) * 0.7f) / 255f * k;
            float b = ((sis & 255) * 0.3f + (su & 255) * 0.7f) / 255f * k;
            float sicak = sicaklik(l, e.getCamera().getBlockPosition());
            r += (0.85f * k - r) * sicak;
            g += (0.55f * k - g) * sicak;
            b += (0.3f * k - b) * sicak;
            e.setRed(r);
            e.setGreen(g);
            e.setBlue(b);
        }
    }
}
