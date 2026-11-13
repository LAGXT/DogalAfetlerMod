package io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini;

import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Başlangıç alanı seçimi (tasarım notu §2 "Afet koreografisi"): hedef oyuncunun 40–90 blok uzağında, yüzeyi en çok
 * yanıcı olan (orman biyomu önde) yüklü nokta. Chunk yüklemez; uygun yer yoksa oyuncunun kendi çevresi denenir ve
 * orada da yakıt yoksa {@code yerUygunMu} reddeder (N14 deseni: yönetici başka oyuncu dener ya da erteler).
 */
final class OrmanYeri {
    private static final int DENEME = 24;
    private static final int IZGARA = 2;
    private static final int ORMAN_PUANI = 6;

    private OrmanYeri() {
    }

    static BlockPos sec(ServerLevel level, BlockPos oyuncu, RandomSource r) {
        return sec(level, oyuncu, r, OrmanAyarlari.aramaEnAz(), OrmanAyarlari.aramaEnCok());
    }

    /** {@code nokta} çevresinde {@code enAz}–{@code enCok} blok halkasında en yanıcı yüzey (doğal başlatmada nokta zaten halkadadır). */
    static BlockPos sec(ServerLevel level, BlockPos oyuncu, RandomSource r, int enAz, int enCok) {
        BlockPos en = null;
        int enPuan = 0;
        for (int i = 0; i < DENEME; i++) {
            double aci = r.nextDouble() * Math.PI * 2;
            double uzaklik = enAz + r.nextDouble() * (enCok - enAz);
            int x = oyuncu.getX() + (int) Math.round(Math.cos(aci) * uzaklik);
            int z = oyuncu.getZ() + (int) Math.round(Math.sin(aci) * uzaklik);
            int puan = puan(level, x, z, 4);
            if (puan > enPuan) {
                enPuan = puan;
                en = yuzey(level, x, z);
            }
        }
        return en != null ? en : yuzey(level, oyuncu.getX(), oyuncu.getZ());
    }

    /** Merkez çevresindeki 5×5 ızgarada yanıcı yüzey var mı. */
    static boolean yakitVar(ServerLevel level, BlockPos merkez, int yaricap) {
        return puan(level, merkez.getX(), merkez.getZ(), Math.max(1, yaricap / IZGARA)) > 0;
    }

    /** 5×5 ızgara (aralık {@code adim}): yanıcı yüzey başına 1, orman biyomu ek puan; yüklü olmayan nokta 0. */
    private static int puan(ServerLevel level, int x, int z, int adim) {
        int puan = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = -IZGARA; i <= IZGARA; i++) {
            for (int j = -IZGARA; j <= IZGARA; j++) {
                int px = x + i * adim;
                int pz = z + j * adim;
                if (!level.getChunkSource().hasChunk(px >> 4, pz >> 4)) {
                    continue;
                }
                int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, px, pz);
                for (int dy = -1; dy <= 0; dy++) {
                    p.set(px, h + dy, pz);
                    if (YanginYoneticisi.yanabilir(level.getBlockState(p), level, p)) {
                        puan++;
                        break;
                    }
                }
            }
        }
        if (puan > 0 && level.getChunkSource().hasChunk(x >> 4, z >> 4)
                && level.getBiome(yuzey(level, x, z)).is(BiomeTags.IS_FOREST)) {
            puan += ORMAN_PUANI;
        }
        return puan;
    }

    /** Sütunun en üst katı bloğu (yaprak dahil); yüklü değilse oyuncu yüksekliğinde sayılır. */
    private static BlockPos yuzey(ServerLevel level, int x, int z) {
        if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
            return new BlockPos(x, level.getSeaLevel(), z);
        }
        return new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
    }
}
