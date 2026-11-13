package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.core.mantik.MerkezHalkasi;
import java.util.random.RandomGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Doğal afet merkezi için halka noktası (kullanıcı isteği: afet oyuncunun eski konumuna ya da üstüne değil, seçim anındaki
 * taze konumundan 3–10 chunk uzağa kurulur). Tüm doğal başlatmalar yöneticide bu noktadan geçer; afetler
 * {@link Afet#merkezSec(ServerLevel, ServerPlayer, int, BlockPos, net.minecraft.util.RandomSource)} ile noktayı rafine eder.
 */
public final class AfetMerkezi {
    private AfetMerkezi() {
    }

    /**
     * Hedef oyuncunun şu anki konumundan {@code halka} içinde, rastgele yönde, yüzeye oturtulmuş bir nokta. Chunk yüklemez:
     * yüklenmemiş chunk'ta yükseklik oyuncunun yüksekliği sayılır.
     */
    public static BlockPos halkaNoktasi(ServerLevel level, ServerPlayer hedef, MerkezHalkasi halka,
            RandomGenerator rastgele) {
        double[] ofset = halka.ofset(rastgele);
        int x = Mth.floor(hedef.getX() + ofset[0]);
        int z = Mth.floor(hedef.getZ() + ofset[1]);
        return new BlockPos(x, yuzeyY(level, x, z, hedef.getBlockY()), z);
    }

    /**
     * Başlangıç noktası komutla (oyuncunun bulunduğu blok) mu verildi, yoksa doğal halka noktası mı. Doğal nokta oyuncunun
     * bloğuna denk gelmez (halka en az 3 chunk uzakta).
     */
    public static boolean komutNoktasi(ServerPlayer hedef, BlockPos nokta) {
        return hedef.blockPosition().equals(nokta);
    }

    /**
     * {@code (x, z)} sütununun yüzey yüksekliği (yapraksız, yüksek blok üstü). Chunk yüklü değilse ya da boyutun tavanı varsa
     * (Nether) {@code yedekY}.
     */
    public static int yuzeyY(ServerLevel level, int x, int z, int yedekY) {
        if (level.dimensionType().hasCeiling() || !level.hasChunk(x >> 4, z >> 4)) {
            return yedekY;
        }
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }
}
