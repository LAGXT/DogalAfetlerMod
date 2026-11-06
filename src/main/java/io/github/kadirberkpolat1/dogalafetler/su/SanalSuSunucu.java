package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.DonemAlani;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.core.olay.DonemDegistiEvent;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Sanal suyun sunucu tarafı: deniz alanının tembel hesabı ve tazelenmesi (K0-30), deniz artışının dönemden okunması, biten
 * dalga ve mağaraların temizliği (harita bütünlüğü hash'i log'a), dalga hasarı ve çarpma hasarı, senkron.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class SanalSuSunucu {
    /** Blok olayı bu derinliğe kadar alanı etkiler: okuyucu gerçek su sütununda dibi 64 blok aşağıya kadar arar. */
    private static final int ETKI_DERINLIGI = 66;
    /** Sürüklenen canlının bir önceki tick konumu (çarpma hızı). */
    private static final Map<LivingEntity, Vec3> ONCEKI = new WeakHashMap<>();

    private SanalSuSunucu() {
    }

    @SubscribeEvent
    public static void sunucuTick(TickEvent.ServerTickEvent e) {
        MinecraftServer server = e.getServer();
        if (e.phase == TickEvent.Phase.START) {
            SanalSu.sunucuSaati = DunyaDonemi.al(server).saat();
            return;
        }
        DenizYukselmesiSureci.ORNEK.tick(server);
    }

    @SubscribeEvent
    public static void seviyeTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER) {
            return;
        }
        ServerLevel l = (ServerLevel) e.level;
        float artis = l.dimension() == Level.OVERWORLD ? DunyaDonemi.al(l.getServer()).deniz().mevcutArtis() : 0f;
        SanalSu.Boyut b = artis > 0 ? SanalSu.boyut(l) : SanalSu.BOYUTLAR.get(l.dimension());
        if (b == null) {
            return;
        }
        SanalSuDunyasi d = b.dunya;
        d.denizArtisiAyarla(artis);
        long t = l.getGameTime();
        if (t % 10 == 0 && d.alanGerekli()) {
            alanIsle(l, t, b);
        }
        double saat = SanalSu.sunucuSaati;
        boolean degisti = false;
        for (int id : d.bitenDalgalar(saat)) {
            degisti |= d.dalgaKaldir(id);
        }
        for (int id : d.bitenMagaralar(saat)) {
            long[] k = b.magaraKutusu.remove(id);
            if (k != null) {
                long sonra = SanalSu.bolgeHash(l, (int) k[0], (int) k[1], (int) k[2], (int) k[3], (int) k[4], (int) k[5]);
                DogalAfetler.LOGGER.info("Sanal su: mağara {} kurudu; cep kutusu hash önce={} sonra={} eşit={}", id,
                        Long.toHexString(k[6]), Long.toHexString(sonra), k[6] == sonra);
            }
            degisti |= d.magaraKaldir(id);
        }
        if (degisti) {
            SanalSu.yayinla(l);
        }
        if (!d.dalgalar().isEmpty()) {
            dalgaEtkileri(l, d, saat);
        }
    }

    /** İstenen chunk'lar: oyuncuların simülasyon mesafesi + 1 ve sabitlenmiş bölge; okuma bütçesi TPS ile kısılır. */
    private static void alanIsle(ServerLevel l, long t, SanalSu.Boyut b) {
        DenizAlani alan = b.dunya.alan();
        int r = l.getServer().getPlayerList().getSimulationDistance() + 1;
        Set<Long> istenen = new HashSet<>(b.sabit);
        List<Long> odak = new ArrayList<>(b.sabit);
        for (ServerPlayer p : l.players()) {
            int pcx = p.getBlockX() >> 4;
            int pcz = p.getBlockZ() >> 4;
            odak.add(DenizAlani.anahtar(pcx, pcz));
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    istenen.add(DenizAlani.anahtar(pcx + dx, pcz + dz));
                }
            }
        }
        for (long k : istenen) {
            alan.iste((int) (k >> 32), (int) k);
        }
        for (long k : alan.istenenler()) {
            if (!istenen.contains(k)) {
                alan.birak(k);
            }
        }
        SuAyarlari.Sunucu a = SuAyarlari.sunucu();
        b.plan.uzakButce(a.tazelemeUzakButce.get());
        for (long k : b.plan.sec(t, odak.stream().mapToLong(Long::longValue).toArray(), alan.istenenler())) {
            alan.gecersizKil((int) (k >> 32), (int) k);
        }
        int butce = Math.max(4, (int) (a.alanOkumaButcesi.get() * TpsOlcer.kismaCarpani()));
        alan.isle(SanalSu.okuyucu(l), butce);
    }

    /** Dalganın içindeki canlılar: saniyede {@code hasar}, sürüklenirken engele çarpan {@code (hız - 0,2) * 10} hasar. */
    private static void dalgaEtkileri(ServerLevel l, SanalSuDunyasi d, double saat) {
        double[] it = new double[2];
        for (Dalga w : d.dalgalar().values()) {
            double c = w.cephe(saat) - w.uzunluk() / 2;
            double mx = w.x0() + w.yonX() * c;
            double mz = w.z0() + w.yonZ() * c;
            double r = Math.max(w.genislik(), w.uzunluk()) / 2 + 2;
            AABB kutu = new AABB(mx - r, w.taban() - 4, mz - r, mx + r, w.taban() + w.yukseklik() + 4, mz + r);
            for (LivingEntity en : l.getEntitiesOfClass(LivingEntity.class, kutu, LivingEntity::isAlive)) {
                Vec3 onceki = ONCEKI.put(en, en.position());
                if (SanalSu.itmeHesapla(en, it) == null) {
                    continue;
                }
                if (w.hasar() > 0 && l.getGameTime() % 20 == 0) {
                    en.hurt(l.damageSources().drown(), (float) w.hasar());
                }
                if (onceki != null && en.horizontalCollision) {
                    double hiz = Math.hypot(en.getX() - onceki.x, en.getZ() - onceki.z);
                    if (hiz > 0.2) {
                        en.hurt(l.damageSources().flyIntoWall(), (float) ((hiz - 0.2) * 10));
                    }
                }
            }
        }
    }

    private static void gonder(PlayerEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            SanalSu.Boyut b = SanalSu.BOYUTLAR.get(p.level().dimension());
            AgKanali.oyuncuyaGonder(p, SuPaketi.durumdan(p.level().dimension(),
                    b != null ? b.dunya : new SanalSuDunyasi(new DenizAlani(1, 1, 0))));
        }
    }

    @SubscribeEvent
    public static void giris(PlayerEvent.PlayerLoggedInEvent e) {
        gonder(e);
    }

    @SubscribeEvent
    public static void boyutDegisti(PlayerEvent.PlayerChangedDimensionEvent e) {
        gonder(e);
    }

    @SubscribeEvent
    public static void yenidenDogdu(PlayerEvent.PlayerRespawnEvent e) {
        gonder(e);
    }

    /**
     * Bütün blok olayları chunk'ı yeniden okutur (değişmeyen okuma hesap başlatmaz). Bayrak-2 değişiklikleri periyodik
     * tazeleme ya da {@link SanalSu#chunkDegisti} yakalar.
     */
    @SubscribeEvent
    public static void blokDegisti(BlockEvent e) {
        LevelAccessor la = e.getLevel();
        if (la == null || la.isClientSide() || !(la instanceof ServerLevel l)) {
            return;
        }
        SanalSu.Boyut b = SanalSu.BOYUTLAR.get(l.dimension());
        BlockPos p = e.getPos();
        if (b != null && b.dunya.alanGerekli()
                && p.getY() >= l.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ()) - ETKI_DERINLIGI) {
            b.dunya.alan().gecersizKil(p.getX() >> 4, p.getZ() >> 4);
        }
    }

    /** {@code /dogalafet sifirla}: bütün dalga, mağara ve çekilmeler kalkar (deniz artışı dönemle sıfırlandı). */
    @SubscribeEvent
    public static void donemDegisti(DonemDegistiEvent e) {
        if (e.alan() == DonemAlani.SIFIRLAMA) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            hepsiniTemizle(server);
        } else if (e.alan() == DonemAlani.AY) {
            DenizYukselmesiSureci.ORNEK.ayDegisti(e.donem());
        }
    }

    static void hepsiniTemizle(MinecraftServer server) {
        for (SanalSu.Boyut b : SanalSu.BOYUTLAR.values()) {
            b.dunya.temizle();
            b.magaraKutusu.clear();
        }
        if (server != null) {
            for (ServerLevel l : server.getAllLevels()) {
                SanalSu.yayinla(l);
            }
        }
    }

    @SubscribeEvent
    public static void sunucuDurdu(ServerStoppedEvent e) {
        SanalSu.BOYUTLAR.clear();
        ONCEKI.clear();
    }
}
