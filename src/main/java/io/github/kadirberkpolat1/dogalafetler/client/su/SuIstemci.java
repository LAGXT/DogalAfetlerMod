package io.github.kadirberkpolat1.dogalafetler.client.su;

import com.mojang.blaze3d.shaders.FogShape;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.DenizSeviyesiAlani;
import io.github.kadirberkpolat1.dogalafetler.su.MagaraZamani;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSuDunyasi;
import io.github.kadirberkpolat1.dogalafetler.su.TazelemePlani;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * İstemci tarafı (spike): alanın yerel hesabı ve tazelenmesi, çekilmede chunk yeniden derleme, buhar parçacığı, su altı
 * sisi.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SuIstemci {
    /**
     * Sunucudan gelen blok güncellemeleri istemcide Forge olayı atmaz ({@code LevelAccessor.blockUpdated} istemcide boş);
     * oyuncunun ±2 chunk'ı her saniye, uzak chunk'lar dönüşümlü yeniden okunur (sunucuyla aynı plan).
     */
    private static final TazelemePlani PLAN = new TazelemePlani(2, 20, 16);
    /** Gerçek suyu gizlenmiş (yeniden derlenmiş) çekilme bölgesi ve Y aralığı. */
    private static Cekilme sonCekilme;
    private static int[] sonAralik;
    /** Paketle gelen, alan verisi bekleyen çekilme. */
    private static Cekilme hedef;
    private static boolean hedefBekliyor;
    private static long hedefGelis;
    private static long dogrulamaTick = Long.MAX_VALUE;
    private static long gizlemeOnce;
    private static int sayac;
    private static int sonHesap;
    private static int sonDegisen;

    private SuIstemci() {
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
        SanalSuDunyasi d = SanalSu.ISTEMCI;
        long t = l.getGameTime();
        if (d.alanGerekli() && ++sayac % 10 == 0) {
            alanGuncelle(mc, l, d.alan());
        }
        cekilmeGuncelle(mc, l, d, t);
        d.temizlikYap(t);
        RandomSource r = l.random;
        for (SanalSuDunyasi.Magara m : d.magaralar()) {
            if (m.zaman().asama(t) != MagaraZamani.Asama.BUHARLASIYOR) {
                continue;
            }
            long[] s = m.cep().sutunAnahtarlari();
            double seviye = m.zaman().seviye(t);
            for (int i = 0; i < 8; i++) {
                long k = s[r.nextInt(s.length)];
                int x = (int) (k >> 32);
                int z = (int) k;
                if (m.cep().icerir(x, (int) Math.floor(seviye - 0.5), z)) {
                    l.addParticle(ParticleTypes.CLOUD, x + r.nextDouble(), seviye, z + r.nextDouble(), 0, 0.03, 0);
                }
            }
        }
    }

    private static void alanGuncelle(Minecraft mc, ClientLevel l, DenizSeviyesiAlani alan) {
        int r = Math.min(mc.options.getEffectiveRenderDistance(), 12) + 1;
        int pcx = mc.player.getBlockX() >> 4;
        int pcz = mc.player.getBlockZ() >> 4;
        Set<Long> istenen = new HashSet<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                istenen.add(DenizSeviyesiAlani.anahtar(pcx + dx, pcz + dz));
                alan.iste(pcx + dx, pcz + dz);
            }
        }
        for (long k : alan.istenenler()) {
            if (!istenen.contains(k)) {
                alan.birak(k);
            }
        }
        for (long k : PLAN.sec(l.getGameTime(), new long[] {DenizSeviyesiAlani.anahtar(pcx, pcz)}, alan.istenenler())) {
            alan.gecersizKil((int) (k >> 32), (int) k);
        }
        long t0 = System.nanoTime();
        int okunan = alan.isle(SanalSu.okuyucu(l), 64);
        if (alan.degisenOkumaSayisi() != sonDegisen) {
            sonDegisen = alan.degisenOkumaSayisi();
            DogalAfetler.LOGGER.info("SPIKE-SU alan(istemci) okunan={} okuma={} ms degisen_toplam={} parca={} bekleyen={}",
                    okunan, String.format(Locale.ROOT, "%.2f", (System.nanoTime() - t0) / 1e6), sonDegisen,
                    alan.parcaSayisi(), alan.bekleyen());
        }
        if (alan.hesapSayisi() != sonHesap) {
            sonHesap = alan.hesapSayisi();
            DogalAfetler.LOGGER.info("SPIKE-SU alan(istemci) BFS#{} sutun={} sure={} ms (arka plan)", sonHesap,
                    alan.sonSutunSayisi(), String.format(Locale.ROOT, "%.2f", alan.sonHesapNs() / 1e6));
        }
    }

    /** Uzaktan yeniden yüklenen chunk hemen okunur (periyodik tazelemeyi beklemez). */
    @SubscribeEvent
    public static void chunkYuklendi(ChunkEvent.Load e) {
        if (e.getLevel().isClientSide() && SanalSu.ISTEMCI.alanGerekli()) {
            SanalSu.ISTEMCI.alan().gecersizKil(e.getChunk().getPos().x, e.getChunk().getPos().z);
        }
    }

    /**
     * Çekilme bölgesi değişince eski ve yeni bölgenin su içeren bölümleri bir kez yeniden derlenir. Karşılaştırma değerle
     * (her tam durum paketi yeni {@code Cekilme} örneği getirir); Y aralığı bölgedeki gerçek su sütunlarından, bu yüzden
     * alan o chunk'ı okuyana kadar (en çok 100 tick) beklenir. Derlemeden 100 tick sonra sıvı kancası hiç tetiklenmediyse
     * (sıvı render'ını başka bir mod üstlenmiş) bir kez uyarır.
     */
    private static void cekilmeGuncelle(Minecraft mc, ClientLevel l, SanalSuDunyasi d, long t) {
        Cekilme c = d.cekilme();
        if (!Objects.equals(c, hedef)) {
            hedef = c;
            hedefBekliyor = true;
            hedefGelis = t;
        }
        if (hedefBekliyor) {
            int[] aralik = c == null ? null : yAraligi(l, d.alan(), c, t - hedefGelis > 100);
            if (c == null || aralik != null) {
                if (sonCekilme != null) {
                    kirlet(mc, sonCekilme, sonAralik);
                }
                if (c != null) {
                    kirlet(mc, c, aralik);
                    gizlemeOnce = SuGizleme.gizlenenSayisi();
                    dogrulamaTick = aralik[2] > 0 ? t + 100 : Long.MAX_VALUE;
                }
                sonCekilme = c;
                sonAralik = aralik;
                hedefBekliyor = false;
            }
        }
        if (t >= dogrulamaTick) {
            dogrulamaTick = Long.MAX_VALUE;
            if (SuGizleme.gizlenenSayisi() == gizlemeOnce) {
                DogalAfetler.LOGGER.warn("SPIKE-SU cekilme: LiquidBlockRenderer kancasi 100 tick'te hic tetiklenmedi; sivi "
                        + "render'ini baska bir mod (Embeddium/Sodium turevi?) ustlenmis olabilir, cekilme gorunmez");
            }
        }
    }

    /**
     * Bölgedeki gizlenecek gerçek su bloklarının Y aralığı ve gizlenecek sütun sayısı: {@code {y0, y1, sutun}}. Merkez
     * chunk henüz okunmadıysa {@code null} (bekle); {@code zorla} ise deniz seviyesi çevresi.
     */
    private static int[] yAraligi(ClientLevel l, DenizSeviyesiAlani alan, Cekilme c, boolean zorla) {
        if (!alan.yuklu((int) Math.floor(c.x()), (int) Math.floor(c.z())) && !zorla) {
            return null;
        }
        int y0 = Integer.MAX_VALUE;
        int y1 = Integer.MIN_VALUE;
        int sutun = 0;
        int r = (int) Math.ceil(c.yaricap());
        for (int x = (int) Math.floor(c.x()) - r; x <= (int) Math.floor(c.x()) + r; x++) {
            for (int z = (int) Math.floor(c.z()) - r; z <= (int) Math.floor(c.z()) + r; z++) {
                int k = c.gizlenenKatman(x + 0.5, z + 0.5);
                if (k <= 0 || !alan.su(x, z)) {
                    continue;
                }
                int zemin = alan.zemin(x, z);
                y0 = Math.min(y0, zemin - k - 1);
                y1 = Math.max(y1, zemin);
                sutun++;
            }
        }
        if (sutun == 0) {
            return new int[] {l.getSeaLevel() - 8, l.getSeaLevel() + 1, 0};
        }
        return new int[] {y0, y1, sutun};
    }

    /** Yalnız çemberle kesişen ve Y aralığındaki bölümler; bölüm başına bir çağrı. */
    private static void kirlet(Minecraft mc, Cekilme c, int[] aralik) {
        long t0 = System.nanoTime();
        int r = (int) Math.ceil(c.yaricap());
        int sx0 = ((int) Math.floor(c.x()) - r) >> 4;
        int sx1 = ((int) Math.floor(c.x()) + r) >> 4;
        int sz0 = ((int) Math.floor(c.z()) - r) >> 4;
        int sz1 = ((int) Math.floor(c.z()) + r) >> 4;
        int bolum = 0;
        for (int sx = sx0; sx <= sx1; sx++) {
            for (int sz = sz0; sz <= sz1; sz++) {
                double nx = Math.max(sx * 16, Math.min(c.x(), sx * 16 + 16));
                double nz = Math.max(sz * 16, Math.min(c.z(), sz * 16 + 16));
                if ((nx - c.x()) * (nx - c.x()) + (nz - c.z()) * (nz - c.z()) > c.yaricap() * c.yaricap()) {
                    continue;
                }
                for (int sy = aralik[0] >> 4; sy <= aralik[1] >> 4; sy++) {
                    mc.levelRenderer.setSectionDirty(sx, sy, sz);
                    bolum++;
                }
            }
        }
        DogalAfetler.LOGGER.info("SPIKE-SU cekilme yeniden derleme: bolum={} y={}..{} sutun={} isaretleme={} ms", bolum,
                aralik[0], aralik[1], aralik[2], String.format(Locale.ROOT, "%.3f", (System.nanoTime() - t0) / 1e6));
        SuRenderer.pencereBasla("cekilme_derleme", 3000);
    }

    static boolean kameraSuda(Camera cam) {
        Minecraft mc = Minecraft.getInstance();
        SanalSuDunyasi d = SanalSu.dunya(mc.level);
        if (d == null || cam.getFluidInCamera() != FogType.NONE) {
            return false;
        }
        Vec3 p = cam.getPosition();
        return !Double.isNaN(d.ustu(p.x, p.y, p.z, mc.level.getGameTime()));
    }

    @SubscribeEvent
    public static void sis(ViewportEvent.RenderFog e) {
        if (kameraSuda(e.getCamera())) {
            e.setNearPlaneDistance(-8f);
            e.setFarPlaneDistance(40f);
            e.setFogShape(FogShape.SPHERE);
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void sisRengi(ViewportEvent.ComputeFogColor e) {
        if (kameraSuda(e.getCamera())) {
            int c = Minecraft.getInstance().level.getBiome(e.getCamera().getBlockPosition()).value().getWaterFogColor();
            e.setRed((c >> 16 & 255) / 255f);
            e.setGreen((c >> 8 & 255) / 255f);
            e.setBlue((c & 255) / 255f);
        }
    }
}
