package io.github.kadirberkpolat1.dogalafetler.afet.deprem;

import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Deprem ve sinkhole'un ortak blok işleri (sunucu ana thread'i): kaldırma, düşen blok (tavanlı), koyma, değişen chunk'ların
 * sanal suya bildirimi ({@code SanalSu.chunkDegisti}, tick sonunda bir kez), yüzey çatlak örtüleri
 * ({@code destroyBlockProgress}) ve uzak oyunculara da giden parçacıklar. Blok varlıklı, kırılmaz bloklar ve sıvılar korunur;
 * config'teki {@code deprem.enAltY} (vars. −50) altına inilmez (ana kaya ve deepslate tabanı, tasarım notu deprem §6).
 */
public final class BlokIsleri {
    private static final double PARCACIK_MENZILI = 96;
    private static final int ORTU_TAZELEME = 300;

    private final ServerLevel level;
    private final int ortuTabani;
    private final int enAltY;
    private final LongOpenHashSet degisenChunklar = new LongOpenHashSet();
    private final List<FallingBlockEntity> dusenler = new ArrayList<>();
    private final Map<Integer, Ortu> ortuler = new HashMap<>();
    private int degisenBlok;

    private record Ortu(BlockPos konum, int asama, long zaman) {
    }

    /** {@code ortuTabani}: çatlak örtülerinin kimlik tabanı (negatif; oyuncu ve varlık kimlikleriyle çakışmaz). */
    public BlokIsleri(ServerLevel level, int ortuTabani) {
        this.level = level;
        this.ortuTabani = ortuTabani;
        this.enAltY = DepremAyarlari.enAltY();
    }

    public int enAltY() {
        return enAltY;
    }

    public ServerLevel level() {
        return level;
    }

    /** Bu nesnenin bugüne kadar değiştirdiği blok sayısı (test, log). */
    public int degisenBlok() {
        return degisenBlok;
    }

    public boolean yuklu(int x, int z) {
        return level.hasChunk(x >> 4, z >> 4);
    }

    /** Sütunun en üst bloğu (bitki, yaprak dahil); yüklü değilse {@code Integer.MIN_VALUE}. */
    public int ust(int x, int z) {
        return yuklu(x, z) ? level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1 : Integer.MIN_VALUE;
    }

    /** Sütunun zemini: en üstten aşağı yaprak, kütük, bitki ve hava geçilerek bulunan ilk sağlam blok. */
    public int zemin(int x, int z) {
        int y = ust(x, z);
        if (y == Integer.MIN_VALUE) {
            return y;
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        for (int i = 0; i < 40 && y > level.getMinBuildHeight(); i++, y--) {
            BlockState s = level.getBlockState(p.setY(y));
            if (!(s.isAir() || s.is(BlockTags.LEAVES) || s.is(BlockTags.LOGS) || s.canBeReplaced()
                    || !s.blocksMotion())) {
                return y;
            }
        }
        return y;
    }

    /** Afet bu bloğu değiştirebilir mi: hava, sıvı, kırılmaz ve blok varlıklı bloklar korunur. */
    public boolean kirilabilir(BlockPos p, BlockState s) {
        return !s.isAir() && s.getFluidState().isEmpty() && !s.hasBlockEntity() && s.getDestroySpeed(level, p) >= 0
                && p.getY() >= enAltY;
    }

    /** Bloğu kaldırır; {@code komsu}: komşu fizik güncellemesi (yarık kenarı), yoksa yalnız istemciye. */
    public boolean kaldir(BlockPos p, boolean komsu) {
        BlockState s = level.getBlockState(p);
        if (!kirilabilir(p, s)) {
            return false;
        }
        level.setBlock(p, Blocks.AIR.defaultBlockState(), komsu ? Block.UPDATE_ALL : Block.UPDATE_CLIENTS);
        isaretle(p);
        return true;
    }

    /** Yaprak kırılma parçacığı ve sesiyle (eşya düşmeden) kırar. */
    public boolean kir(BlockPos p) {
        if (!kirilabilir(p, level.getBlockState(p))) {
            return false;
        }
        level.destroyBlock(p, false);
        isaretle(p);
        return true;
    }

    public void koy(BlockPos p, BlockState s) {
        if (p.getY() >= enAltY && level.getBlockState(p).canBeReplaced()) {
            level.setBlock(p, s, Block.UPDATE_ALL);
            isaretle(p);
        }
    }

    /** Kırılabilir bloğun yerine başka blok koyar (sinkhole sarkması, kenar halkası). */
    public boolean degistir(BlockPos p, BlockState yeni) {
        if (!kirilabilir(p, level.getBlockState(p))) {
            return false;
        }
        level.setBlock(p, yeni, Block.UPDATE_ALL);
        isaretle(p);
        return true;
    }

    /**
     * Bloğu {@code FallingBlockEntity} olarak düşürür; düşen blok tavanı doluysa blok kaldırılır ve yalnız kırılma
     * parçacığı gönderilir (tasarım notu deprem §5). {@code hiz} düşen bloka verilen ilk hız. Varlık işlemeyen chunk'ta bloğa
     * dokunmaz ve false döner: oradaki düşen blok görünmez ya da havada asılı kalırdı (AY-12 ailesi).
     */
    public boolean dusur(BlockPos p, Vec3 hiz, int tavan) {
        BlockState s = level.getBlockState(p);
        if (!kirilabilir(p, s) || !level.isPositionEntityTicking(p)) {
            return false;
        }
        dusenler.removeIf(e -> !e.isAlive());
        if (dusenler.size() >= tavan) {
            level.levelEvent(2001, p, Block.getId(s));
            return kaldir(p, true);
        }
        FallingBlockEntity e = FallingBlockEntity.fall(level, p, s);
        e.setDeltaMovement(hiz);
        e.dropItem = false;
        dusenler.add(e);
        isaretle(p);
        return true;
    }

    public int canliDusenSayisi() {
        dusenler.removeIf(e -> !e.isAlive());
        return dusenler.size();
    }

    /** Temiz durdurma: havadaki düşen bloklar kaldırılır. */
    public void dusenleriKaldir() {
        for (FallingBlockEntity e : dusenler) {
            e.discard();
        }
        dusenler.clear();
    }

    /** Yüzey çatlak örtüsü (0–9); değişmeyen örtü yalnız sönmesin diye ({@value #ORTU_TAZELEME} tick'te bir) yeniden gönderilir. */
    public void ortu(int sira, BlockPos p, int asama) {
        int kimlik = ortuTabani - sira;
        Ortu eski = ortuler.get(kimlik);
        long simdi = level.getGameTime();
        if (eski != null && eski.asama() == asama && eski.konum().equals(p) && simdi - eski.zaman() < ORTU_TAZELEME) {
            return;
        }
        level.destroyBlockProgress(kimlik, p, asama);
        ortuler.put(kimlik, new Ortu(p.immutable(), asama, simdi));
    }

    public void ortuKaldir(int sira) {
        Ortu eski = ortuler.remove(ortuTabani - sira);
        if (eski != null) {
            level.destroyBlockProgress(ortuTabani - sira, eski.konum(), -1);
        }
    }

    public void ortuleriKaldir() {
        for (Map.Entry<Integer, Ortu> g : ortuler.entrySet()) {
            level.destroyBlockProgress(g.getKey(), g.getValue().konum(), -1);
        }
        ortuler.clear();
    }

    public int ortuSayisi() {
        return ortuler.size();
    }

    /** Parçacık: {@value #PARCACIK_MENZILI} blok içindeki oyunculara (vanilla 32 blok sınırı yerine). */
    public void parcacik(ParticleOptions tur, double x, double y, double z, int adet, double yayilma, double hiz) {
        for (ServerPlayer o : level.players()) {
            if (o.distanceToSqr(x, y, z) < PARCACIK_MENZILI * PARCACIK_MENZILI) {
                level.sendParticles(o, tur, true, x, y, z, adet, yayilma, yayilma, yayilma, hiz);
            }
        }
    }

    /** Bu tick'te değişen chunk'ları sanal suya bildirir (Faz 4 kuralı). */
    public void bildir() {
        if (degisenChunklar.isEmpty()) {
            return;
        }
        var it = degisenChunklar.iterator();
        while (it.hasNext()) {
            SanalSu.chunkDegisti(level, new ChunkPos(it.nextLong()));
        }
        degisenChunklar.clear();
    }

    private void isaretle(BlockPos p) {
        degisenBlok++;
        degisenChunklar.add(ChunkPos.asLong(p.getX() >> 4, p.getZ() >> 4));
    }
}
