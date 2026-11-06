package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Çarpma sarsıntısında haritanın sallanması (bölüm H "harita sallandığında"; tasarım §2 63–75 sn, §5): her oyuncunun
 * {@code sarsintiYaricapi} çevresinde yüzeye yakın, altı boş {@link AyCarpmasiEtiketleri#SARSINTIDA_DUSER} blokları
 * {@code sarsintiYogunlugu} olasılığıyla düşen blok varlığına döner; oyuncu başına en çok {@code sarsintiBlokSayisi},
 * pencereye yayılmış kuyrukla ve TPS kısmasıyla. {@code afetBlokHasari} kapalıyken hiçbir blok düşmez. Değişen her chunk
 * sanal suya bildirilir. Temiz durdurmada havadaki bloklar kökenlerine geri konur (geride varlık kalmaz).
 */
final class SarsintiDusmeleri {
    /** Bir yüzey sütununda yukarıdan aşağı aranan en çok blok. */
    private static final int ARAMA_DERINLIGI = 12;

    private record Dusen(FallingBlockEntity varlik, BlockPos koken, BlockState durum) {
    }

    private final Map<UUID, Integer> sayilar = new HashMap<>();
    private final Map<UUID, Double> birikim = new HashMap<>();
    private final List<Dusen> havadakiler = new ArrayList<>();
    private int toplam;

    /** Bu çarpmada düşen blok sayısı (bütün oyuncular). */
    int toplam() {
        return toplam;
    }

    /**
     * Pencerenin bir adımı: oyuncu başına deneme sayısı tavan / yoğunluk'un pencereye düşen payıdır ({@code adim} /
     * {@code pencereTick}).
     */
    void tick(ServerLevel level, int adim, int pencereTick) {
        tick(level, level.players(), adim, pencereTick, AyCarpmasiAyarlari.sarsintiBlokSayisi(),
                AyCarpmasiAyarlari.sarsintiYaricapi(), AyCarpmasiAyarlari.sarsintiYogunlugu());
    }

    /** Ayarları ve oyuncuları açıkça verilen adım (GameTest kendi sahnesini sınırlar). */
    void tick(ServerLevel level, Collection<ServerPlayer> oyuncular, int adim, int pencereTick, int tavan, int yaricap,
            double yogunluk) {
        havadakiler.removeIf(d -> !d.varlik().isAlive());
        if (adim <= 0 || tavan <= 0 || yogunluk <= 0 || !Kurallar.blokHasariAcik(level.getGameRules())) {
            return;
        }
        double denemePayi = tavan / yogunluk * adim / Math.max(1, pencereTick) * TpsOlcer.kismaCarpani();
        RandomSource r = level.getRandom();
        Set<ChunkPos> degisen = new HashSet<>();
        for (ServerPlayer oyuncu : oyuncular) {
            if (oyuncu.isSpectator()) {
                continue;
            }
            UUID kimlik = oyuncu.getUUID();
            int sayi = sayilar.getOrDefault(kimlik, 0);
            if (sayi >= tavan) {
                continue;
            }
            double b = birikim.getOrDefault(kimlik, 0.0) + denemePayi;
            int deneme = (int) b;
            birikim.put(kimlik, b - deneme);
            for (int i = 0; i < deneme && sayi < tavan; i++) {
                BlockPos aday = aday(level, oyuncu, yaricap, r);
                if (aday != null && r.nextDouble() < yogunluk) {
                    dusur(level, aday);
                    degisen.add(new ChunkPos(aday));
                    sayi++;
                }
            }
            sayilar.put(kimlik, sayi);
        }
        for (ChunkPos chunk : degisen) {
            SanalSu.chunkDegisti(level, chunk);
        }
    }

    private static BlockPos aday(ServerLevel level, ServerPlayer oyuncu, int yaricap, RandomSource r) {
        int dx = r.nextInt(2 * yaricap + 1) - yaricap;
        int dz = r.nextInt(2 * yaricap + 1) - yaricap;
        if (dx * dx + dz * dz > yaricap * yaricap) {
            return null;
        }
        int x = oyuncu.getBlockX() + dx;
        int z = oyuncu.getBlockZ() + dz;
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return null;
        }
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, ust, z);
        for (int i = 0; i < ARAMA_DERINLIGI && p.getY() > level.getMinBuildHeight(); i++, p.move(0, -1, 0)) {
            if (level.getBlockState(p).is(AyCarpmasiEtiketleri.SARSINTIDA_DUSER)
                    && FallingBlock.isFree(level.getBlockState(p.below()))) {
                return p.immutable();
            }
        }
        return null;
    }

    private void dusur(ServerLevel level, BlockPos konum) {
        BlockState durum = level.getBlockState(konum);
        FallingBlockEntity varlik = FallingBlockEntity.fall(level, konum, durum);
        havadakiler.add(new Dusen(varlik, konum, durum));
        toplam++;
    }

    /** Temiz durdurma: havadaki bloklar kökenlerine geri döner, varlıklar kalkar. */
    void geriAl(ServerLevel level) {
        Set<ChunkPos> degisen = new HashSet<>();
        for (Dusen d : havadakiler) {
            if (d.varlik().isAlive()) {
                d.varlik().discard();
                if (level.getBlockState(d.koken()).isAir()) {
                    level.setBlock(d.koken(), d.durum(), 3);
                    degisen.add(new ChunkPos(d.koken()));
                }
            }
        }
        havadakiler.clear();
        for (ChunkPos chunk : degisen) {
            SanalSu.chunkDegisti(level, chunk);
        }
    }
}
