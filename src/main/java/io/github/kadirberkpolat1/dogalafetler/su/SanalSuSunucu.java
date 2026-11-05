package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.HashSet;
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

/** Sunucu tarafı (spike): alanın tembel hesabı, senkron, mağara hash kanıtı, olay yolu fiziği ve izleme log'u. */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class SanalSuSunucu {
    static final int YARICAP_CHUNK = 6;
    static boolean izle;
    /** Mağara cebi -> {x0, y0, z0, x1, y1, z1, önceki hash}. */
    static final Map<MagaraCebi, long[]> MAGARA_HASH = new HashMap<>();
    private static int sonHesap;
    private static int invalidSayisi;

    private SanalSuSunucu() {
    }

    public static void kaydet() {
        AgKanali.sunucudanIstemciye(SuPaketi.class, SuPaketi::yaz, SuPaketi::oku, SuPaketi::isle);
        KomutKaydi.ekle(SanalSuKomutu.olustur());
    }

    static String f(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    static void yayinla(ServerLevel l) {
        SuPaketi p = SuPaketi.durumdan(SanalSu.SUNUCU);
        FriendlyByteBuf b = new FriendlyByteBuf(Unpooled.buffer());
        SuPaketi.yaz(p, b);
        int boyut = b.writerIndex();
        b.release();
        DogalAfetler.LOGGER.info("SPIKE-SU paket boyut={} bayt deniz={} dalga={} magara={} cekilme={} yol={}", boyut,
                SanalSu.SUNUCU.denizHedef(), SanalSu.SUNUCU.dalgalar().size(), SanalSu.SUNUCU.magaralar().size(),
                SanalSu.SUNUCU.cekilme() != null, SanalSu.yol);
        AgKanali.boyuttakilereGonder(l.dimension(), p);
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
            DenizSeviyesiAlani alan = d.alan();
            Set<Long> istenen = new HashSet<>();
            for (ServerPlayer p : l.players()) {
                int pcx = p.getBlockX() >> 4;
                int pcz = p.getBlockZ() >> 4;
                for (int dx = -YARICAP_CHUNK; dx <= YARICAP_CHUNK; dx++) {
                    for (int dz = -YARICAP_CHUNK; dz <= YARICAP_CHUNK; dz++) {
                        istenen.add(DenizSeviyesiAlani.anahtar(pcx + dx, pcz + dz));
                        alan.iste(pcx + dx, pcz + dz);
                    }
                }
            }
            for (long k : alan.istenenler()) {
                if (!istenen.contains(k)) {
                    alan.birak(k);
                }
            }
            long t0 = System.nanoTime();
            int okunan = alan.isle(SanalSu.okuyucu(l), 32);
            if (okunan > 0) {
                DogalAfetler.LOGGER.info("SPIKE-SU alan(sunucu) okunan={} okuma={} ms parca={} invalid_toplam={}", okunan,
                        f((System.nanoTime() - t0) / 1e6), alan.parcaSayisi(), invalidSayisi);
            }
            if (alan.hesapSayisi() != sonHesap) {
                sonHesap = alan.hesapSayisi();
                DogalAfetler.LOGGER.info("SPIKE-SU alan(sunucu) BFS#{} sutun={} sure={} ms (arka plan)", sonHesap,
                        alan.sonSutunSayisi(), f(alan.sonHesapNs() / 1e6));
            }
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

    private static void izleLog(ServerLevel l) {
        for (ServerPlayer p : l.players()) {
            DogalAfetler.LOGGER.info(
                    "SPIKE-SU izle oyuncu pos=({} {} {}) hava={} can={} isInWater={} isUnderWater={} isSwimming={} poz={} batiklikOrani={} yol={}",
                    f(p.getX()), f(p.getY()), f(p.getZ()), p.getAirSupply(), f(p.getHealth()), p.isInWater(),
                    p.isUnderWater(), p.isSwimming(), p.getPose(), f(SanalSu.batiklikOrani(p)), SanalSu.yol);
            for (LivingEntity en : l.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(80),
                    x -> x.getTags().contains("spike"))) {
                Vec3 v = en.getDeltaMovement();
                DogalAfetler.LOGGER.info(
                        "SPIKE-SU izle mob tur={} pos=({} {} {}) hiz=({} {} {}) hava={} can={} isInWater={} batiklikOrani={}",
                        en.getType().toShortString(), f(en.getX()), f(en.getY()), f(en.getZ()), f(v.x), f(v.y), f(v.z),
                        en.getAirSupply(), f(en.getHealth()), en.isInWater(), f(SanalSu.batiklikOrani(en)));
            }
        }
    }

    @SubscribeEvent
    public static void giris(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            AgKanali.oyuncuyaGonder(p, SuPaketi.durumdan(SanalSu.SUNUCU));
        }
    }

    /** Yalnız yüzeye yakın değişiklik alanı etkiler (deniz altında büyüyen yosun, mağara blokları etkilemez). */
    @SubscribeEvent
    public static void blokDegisti(BlockEvent.NeighborNotifyEvent e) {
        LevelAccessor l = e.getLevel();
        if (l.isClientSide() || !SanalSu.SUNUCU.alanGerekli()) {
            return;
        }
        BlockPos p = e.getPos();
        if (p.getY() >= l.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ()) - 2) {
            invalidSayisi++;
            SanalSu.SUNUCU.alan().gecersizKil(p.getX() >> 4, p.getZ() >> 4);
        }
    }

    @SubscribeEvent
    public static void hasar(LivingHurtEvent e) {
        if (e.getSource().is(DamageTypes.DROWN)) {
            LivingEntity en = e.getEntity();
            DogalAfetler.LOGGER.info("SPIKE-SU BOGULMA hasari tur={} miktar={} can={} sanalSuda={} batiklikOrani={} yol={}",
                    en.getType().toShortString(), e.getAmount(), f(en.getHealth()), SanalSu.icindeMi(en),
                    f(SanalSu.batiklikOrani(en)), SanalSu.yol);
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
        double b = SanalSu.batiklikOrani(en);
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
