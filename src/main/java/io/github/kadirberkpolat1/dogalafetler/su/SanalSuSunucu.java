package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingBreatheEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/** Sunucu tarafı (spike): alanın tembel hesabı ve tazelenmesi, senkron, mağara hash kanıtı, olay yolu fiziği ve izleme log'u. */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class SanalSuSunucu {
    /** Blok olayı bu derinliğe kadar alanı etkiler: okuyucu gerçek su sütununda dibi 64 blok aşağıya kadar arar. */
    private static final int ETKI_DERINLIGI = 66;
    /** Yakın halka (±2 chunk) her saniye, uzak chunk'lardan 16'sı her saniye yeniden okunur (istemciyle aynı plan). */
    private static final TazelemePlani PLAN = new TazelemePlani(2, 20, 16);
    static boolean izle;
    /** Mağara cebi -> {x0, y0, z0, x1, y1, z1, önceki hash}. */
    static final Map<MagaraCebi, long[]> MAGARA_HASH = new HashMap<>();
    /**
     * Oyuncu olmasa da alanı hesaplanan chunk'lar (tsunami hedef bölgesi, GameTest). Odak sayılır: yakın halkada tazelenir.
     * Yalnız sunucu thread'i.
     */
    static final Set<Long> SABIT = new HashSet<>();
    private static int sonHesap;
    private static int sonDegisen;
    private static int olaySayisi;

    private SanalSuSunucu() {
    }

    public static void kaydet() {
        AgKanali.sunucudanIstemciye(SuPaketi.class, SuPaketi::yaz, SuPaketi::oku, SuPaketi::isle);
        KomutKaydi.ekle(SanalSuKomutu.olustur());
    }

    static String f(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** Tam durum herkese gider: başka boyuttaki oyuncu da Overworld'e döndüğünde güncel durumu bilir (N11). */
    static void yayinla(ServerLevel l) {
        SuPaketi p = SuPaketi.durumdan(SanalSu.SUNUCU);
        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
        SuPaketi.yaz(p, b);
        int boyut = b.writerIndex();
        b.release();
        DogalAfetler.LOGGER.info("SPIKE-SU paket boyut={} bayt deniz={} dalga={} magara={} cekilme={} yol={}", boyut,
                SanalSu.SUNUCU.denizHedef(), SanalSu.SUNUCU.dalgalar().size(), SanalSu.SUNUCU.magaralar().size(),
                SanalSu.SUNUCU.cekilme() != null, SanalSu.yol);
        AgKanali.herkeseGonder(p);
    }

    @SubscribeEvent
    public static void seviyeTick(TickEvent.LevelTickEvent e) {
        if (e.phase != TickEvent.Phase.END || e.side != LogicalSide.SERVER || e.level.dimension() != Level.OVERWORLD) {
            return;
        }
        ServerLevel l = (ServerLevel) e.level;
        long t = l.getGameTime();
        SanalSuDunyasi d = SanalSu.SUNUCU;
        if (t % 10 == 0 && d.alanGerekli()) {
            alanIsle(l, t, d.alan());
        }
        if (d.aktif()) {
            for (SanalSuDunyasi.Magara m : d.magaralar()) {
                if (m.zaman().asama(t) == MagaraZamani.Asama.BITTI) {
                    long[] k = MAGARA_HASH.remove(m.cep());
                    if (k != null) {
                        long sonra = SanalSu.bolgeHash(l, (int) k[0], (int) k[1], (int) k[2], (int) k[3], (int) k[4],
                                (int) k[5]);
                        DogalAfetler.LOGGER.info("SPIKE-SU MAGARA HASH once={} sonra={} esit={}",
                                Long.toHexString(k[6]), Long.toHexString(sonra), k[6] == sonra);
                    }
                }
            }
            if (d.temizlikYap(t)) {
                yayinla(l);
            }
        }
        if (izle && t % 20 == 0) {
            izleLog(l);
        }
    }

    /**
     * İstenen chunk'lar: her oyuncunun simülasyon mesafesi + 1 chunk'ı (o mesafedeki mob'lar da sanal suyu görsün) ve
     * {@link #SABIT}. Periyodik tazeleme bayrak-2 blok değişikliklerini (donma, yapı yerleşimi) de yakalar.
     */
    private static void alanIsle(ServerLevel l, long t, DenizSeviyesiAlani alan) {
        int r = l.getServer().getPlayerList().getSimulationDistance() + 1;
        Set<Long> istenen = new HashSet<>(SABIT);
        List<Long> odak = new ArrayList<>(SABIT);
        for (ServerPlayer p : l.players()) {
            int pcx = p.getBlockX() >> 4;
            int pcz = p.getBlockZ() >> 4;
            odak.add(DenizSeviyesiAlani.anahtar(pcx, pcz));
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    istenen.add(DenizSeviyesiAlani.anahtar(pcx + dx, pcz + dz));
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
        long[] odaklar = odak.stream().mapToLong(Long::longValue).toArray();
        for (long k : PLAN.sec(t, odaklar, alan.istenenler())) {
            alan.gecersizKil((int) (k >> 32), (int) k);
        }
        long t0 = System.nanoTime();
        int okunan = alan.isle(SanalSu.okuyucu(l), 32);
        if (alan.degisenOkumaSayisi() != sonDegisen) {
            sonDegisen = alan.degisenOkumaSayisi();
            DogalAfetler.LOGGER.info("SPIKE-SU alan(sunucu) okunan={} okuma={} ms degisen_toplam={} parca={} blok_olayi_toplam={}",
                    okunan, f((System.nanoTime() - t0) / 1e6), sonDegisen, alan.parcaSayisi(), olaySayisi);
        }
        if (alan.hesapSayisi() != sonHesap) {
            sonHesap = alan.hesapSayisi();
            DogalAfetler.LOGGER.info("SPIKE-SU alan(sunucu) BFS#{} sutun={} sure={} ms (arka plan)", sonHesap,
                    alan.sonSutunSayisi(), f(alan.sonHesapNs() / 1e6));
        }
    }

    private static void izleLog(ServerLevel l) {
        for (ServerPlayer p : l.players()) {
            DogalAfetler.LOGGER.info(
                    "SPIKE-SU izle oyuncu pos=({} {} {}) hava={} can={} isInWater={} isUnderWater={} isSwimming={} poz={} sanalBatiklik={} yol={}",
                    f(p.getX()), f(p.getY()), f(p.getZ()), p.getAirSupply(), f(p.getHealth()), p.isInWater(),
                    p.isUnderWater(), p.isSwimming(), p.getPose(), f(SanalSu.sanalBatiklik(p)), SanalSu.yol);
            for (LivingEntity en : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(80),
                    x -> x.getTags().contains("spike"))) {
                Vec3 v = en.getDeltaMovement();
                DogalAfetler.LOGGER.info(
                        "SPIKE-SU izle mob tur={} pos=({} {} {}) hiz=({} {} {}) hava={} can={} isInWater={} sanalBatiklik={}",
                        en.getType().toShortString(), f(en.getX()), f(en.getY()), f(en.getZ()), f(v.x), f(v.y), f(v.z),
                        en.getAirSupply(), f(en.getHealth()), en.isInWater(), f(SanalSu.sanalBatiklik(en)));
            }
        }
    }

    @SubscribeEvent
    public static void giris(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            AgKanali.oyuncuyaGonder(p, SuPaketi.durumdan(SanalSu.SUNUCU));
        }
    }

    @SubscribeEvent
    public static void boyutDegisti(PlayerEvent.PlayerChangedDimensionEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            AgKanali.oyuncuyaGonder(p, SuPaketi.durumdan(SanalSu.SUNUCU));
        }
    }

    @SubscribeEvent
    public static void yenidenDogdu(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            AgKanali.oyuncuyaGonder(p, SuPaketi.durumdan(SanalSu.SUNUCU));
        }
    }

    /**
     * Bütün blok olayları (kırma, yerleştirme, sıvının blok oluşturması, komşu bildirimi, bitki büyümesi ...) chunk'ı yeniden
     * okutur; değişmeyen okuma BFS başlatmaz. {@code NeighborNotifyEvent} yalnız {@code UPDATE_NEIGHBORS} bayrağıyla gelir:
     * bayrak-2 değişiklikleri periyodik tazeleme ya da {@link SanalSu#chunkDegisti} yakalar.
     */
    @SubscribeEvent
    public static void blokDegisti(BlockEvent e) {
        LevelAccessor l = e.getLevel();
        if (l == null || l.isClientSide() || !SanalSu.SUNUCU.alanGerekli()) {
            return;
        }
        BlockPos p = e.getPos();
        if (p.getY() >= l.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ()) - ETKI_DERINLIGI) {
            olaySayisi++;
            SanalSu.chunkDegisti(l, p.getX() >> 4, p.getZ() >> 4);
        }
    }

    @SubscribeEvent
    public static void hasar(LivingHurtEvent e) {
        if (e.getSource().is(DamageTypes.DROWN)) {
            LivingEntity en = e.getEntity();
            DogalAfetler.LOGGER.info("SPIKE-SU BOGULMA hasari tur={} miktar={} can={} sanalSuda={} sanalBatiklik={} yol={}",
                    en.getType().toShortString(), e.getAmount(), f(en.getHealth()), SanalSu.sanalSudaMi(en),
                    f(SanalSu.sanalBatiklik(en)), SanalSu.yol);
        }
    }

    /** Olay yolu (karşılaştırma): kaldırma kuvveti, sürükleme ve düşme hasarı sıfırlama elle. */
    @SubscribeEvent
    public static void canliTick(LivingEvent.LivingTickEvent e) {
        if (SanalSu.yol != SanalSu.Yol.OLAY) {
            return;
        }
        LivingEntity en = e.getEntity();
        if ((en instanceof Player) != en.level().isClientSide()) {
            return;
        }
        double b = SanalSu.sanalBatiklik(en);
        if (b <= 0) {
            return;
        }
        Vec3 v = en.getDeltaMovement();
        en.setDeltaMovement(v.x * 0.8, v.y * 0.8 + 0.07 * b, v.z * 0.8);
        en.resetFallDistance();
        en.clearFire();
        SanalSu.itmeUygula(en);
    }

    @SubscribeEvent
    public static void nefes(LivingBreatheEvent e) {
        if (SanalSu.yol == SanalSu.Yol.OLAY && SanalSu.gozSudaMi(e.getEntity())) {
            e.setCanBreathe(false);
            e.setCanRefillAir(false);
        }
    }
}
