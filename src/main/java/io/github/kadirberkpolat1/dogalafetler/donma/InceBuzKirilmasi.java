package io.github.kadirberkpolat1.dogalafetler.donma;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import io.github.kadirberkpolat1.dogalafetler.basarim.OzelBasarim;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;

/**
 * İnce buzun zerinde zıplamak onu kırar: oyuncunun ince buzun (donma sisteminin {@code ince_buz} bloğu) üzerinde yaptığı her
 * zıplama o bloğun sayıcısını artırır; eşiğe ({@value #ESIK_EN_AZ}–{@value #ESIK_EN_COK} zıplama, blok konumundan
 * türetilir) varan buz cam kırılma sesiyle kırılır, yerine altındaki su gelir ve oyuncu düşer. Yürümek ya da durmak
 * saymaz. Sayıcı {@value #UNUTMA_TICK} tick zıplama gelmezse sıfırlanır; harita yalnız zıplanan blokları tutar ve 100
 * tick'te bir süresi dolanlardan temizlenir.
 *
 * <p>Zıplama olayı sunucuda oyuncunun yerden kalktığı an gelir (oyuncu henüz eski konumunda, ayaklarının altındaki blok
 * {@code getOnPos}). Buz kırılınca donma sütun biti kalır: sonraki donma ilerlemesi sütunu yeniden planlar (su yeniden donar).
 */
final class InceBuzKirilmasi {
    static final int ESIK_EN_AZ = 3;
    static final int ESIK_EN_COK = 5;
    static final long UNUTMA_TICK = 100;
    private static final int TEMIZLIK_ARALIGI = 100;
    private static final Map<ResourceKey<Level>, Long2ObjectOpenHashMap<Sayac>> SAYACLAR = new HashMap<>();

    /** Bir bloktaki zıplama sayısı ve sonuncusunun oyun saati. */
    private static final class Sayac {
        int sayi;
        long son;
    }

    private InceBuzKirilmasi() {
    }

    /** Bloğun kırılma eşiği: konumdan türetilen {@code [ESIK_EN_AZ, ESIK_EN_COK]} (her blok sabit, birbirinden farklı). */
    static int esik(BlockPos konum) {
        return ESIK_EN_AZ + Math.floorMod(Mth.getSeed(konum.getX(), konum.getY(), konum.getZ()), ESIK_EN_COK - ESIK_EN_AZ + 1);
    }

    static void sunucuBasliyor(ServerAboutToStartEvent olay) {
        SAYACLAR.clear();
    }

    static void ziplama(LivingEvent.LivingJumpEvent olay) {
        if (!(olay.getEntity() instanceof ServerPlayer oyuncu) || oyuncu.isSpectator()) {
            return;
        }
        ServerLevel level = oyuncu.serverLevel();
        BlockPos konum = oyuncu.getOnPos().immutable();
        BlockState durum = level.getBlockState(konum);
        if (!durum.is(DonmaBloklari.INCE_BUZ.get())) {
            return;
        }
        long simdi = level.getGameTime();
        Long2ObjectOpenHashMap<Sayac> harita = SAYACLAR.computeIfAbsent(level.dimension(), k -> new Long2ObjectOpenHashMap<>());
        long anahtar = konum.asLong();
        Sayac s = harita.get(anahtar);
        if (s == null) {
            s = new Sayac();
            harita.put(anahtar, s);
        } else if (simdi - s.son > UNUTMA_TICK) {
            s.sayi = 0;
        }
        s.sayi++;
        s.son = simdi;
        if (s.sayi >= esik(konum)) {
            harita.remove(anahtar);
            kir(level, konum, durum);
            OzelBasarim.ver(oyuncu, OzelBasarim.INCE_BUZ_KIRICI);
        }
    }

    /** Buzu kaldırır: altı hava değilse su kaynağı (buzun üst blok olarak değiştirdiği su), değilse hava; cam kırılma sesi. */
    private static void kir(ServerLevel level, BlockPos konum, BlockState durum) {
        boolean altiBos = level.getBlockState(konum.below()).isAir();
        level.setBlock(konum, altiBos ? Blocks.AIR.defaultBlockState() : Blocks.WATER.defaultBlockState(), Block.UPDATE_ALL);
        level.playSound(null, konum, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f,
                0.85f + level.random.nextFloat() * 0.3f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, durum), konum.getX() + 0.5, konum.getY() + 0.6,
                konum.getZ() + 0.5, 18, 0.3, 0.15, 0.3, 0.05);
    }

    /** Süresi dolan sayıları siler (zaman aşımı temizliği); kapanmış boyutun haritasını bırakır. */
    static void sunucuTick(TickEvent.ServerTickEvent olay) {
        MinecraftServer server = olay.getServer();
        if (olay.phase != TickEvent.Phase.END || SAYACLAR.isEmpty() || server.getTickCount() % TEMIZLIK_ARALIGI != 0) {
            return;
        }
        for (Iterator<Map.Entry<ResourceKey<Level>, Long2ObjectOpenHashMap<Sayac>>> it = SAYACLAR.entrySet().iterator();
                it.hasNext();) {
            Map.Entry<ResourceKey<Level>, Long2ObjectOpenHashMap<Sayac>> e = it.next();
            ServerLevel level = server.getLevel(e.getKey());
            if (level == null) {
                it.remove();
                continue;
            }
            long simdi = level.getGameTime();
            e.getValue().values().removeIf(s -> simdi - s.son > UNUTMA_TICK);
            if (e.getValue().isEmpty()) {
                it.remove();
            }
        }
    }
}
