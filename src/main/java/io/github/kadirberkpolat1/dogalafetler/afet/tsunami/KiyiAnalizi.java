package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;

/**
 * N14 ve N10: hedefin çevresinde açık deniz var mı, varsa kıyı ve dalga yönü. Deniz sütunu: en üst bloğu durgun su olan ve en
 * az {@value #DERIN} blok derin sütun (buz, sığ gölet ve akan su sayılmaz; donmuş deniz tsunamiyi erteler). Yalnız yüklü
 * chunk'lar okunur; geniş aramada iki blokta bir örneklenir (örnek dört sütun sayılır).
 */
final class KiyiAnalizi {
    static final int DERIN = 3;

    private KiyiAnalizi() {
    }

    /** Yeterli deniz yoksa boş. */
    static Optional<TsunamiGeometrisi> analiz(ServerLevel l, BlockPos merkez, int yaricap, int enAz) {
        int adim = yaricap > 48 ? 2 : 1;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        long sayi = 0;
        double toplamX = 0;
        double toplamZ = 0;
        int ornek = 0;
        int yakinX = 0;
        int yakinZ = 0;
        long yakinD = Long.MAX_VALUE;
        for (int dz = -yaricap; dz <= yaricap; dz += adim) {
            for (int dx = -yaricap; dx <= yaricap; dx += adim) {
                if (dx * dx + dz * dz > yaricap * yaricap) {
                    continue;
                }
                int x = merkez.getX() + dx;
                int z = merkez.getZ() + dz;
                if (suDerinligi(l, x, z, m) < DERIN) {
                    continue;
                }
                sayi += (long) adim * adim;
                toplamX += dx;
                toplamZ += dz;
                ornek++;
                long d = (long) dx * dx + (long) dz * dz;
                if (d < yakinD) {
                    yakinD = d;
                    yakinX = x;
                    yakinZ = z;
                }
            }
        }
        if (sayi < enAz) {
            return Optional.empty();
        }
        double[] yon = TsunamiGeometrisi.yonHesapla(toplamX, toplamZ, ornek);
        int[] kiyi = kiyiBul(l, merkez, yon, yaricap, m);
        if (kiyi == null) {
            kiyi = new int[] {yakinX, yakinZ};
        }
        double taban = l.getHeight(Heightmap.Types.WORLD_SURFACE, kiyi[0], kiyi[1]) - 1;
        return Optional.of(new TsunamiGeometrisi(kiyi[0] + 0.5, kiyi[1] + 0.5, yon[0], yon[1], taban, (int) Math.min(sayi,
                Integer.MAX_VALUE)));
    }

    /**
     * Merkezden denize doğru (yönün tersi) ilk su sütunu; merkez suyun üstündeyse karaya doğru son su sütunu. Bulunmazsa null.
     */
    private static int[] kiyiBul(ServerLevel l, BlockPos merkez, double[] yon, int yaricap, BlockPos.MutableBlockPos m) {
        boolean sudaBasla = suDerinligi(l, merkez.getX(), merkez.getZ(), m) > 0;
        double ax = sudaBasla ? yon[0] : -yon[0];
        double az = sudaBasla ? yon[1] : -yon[1];
        int[] son = sudaBasla ? new int[] {merkez.getX(), merkez.getZ()} : null;
        for (int t = 1; t <= yaricap; t++) {
            int x = (int) Math.floor(merkez.getX() + 0.5 + ax * t);
            int z = (int) Math.floor(merkez.getZ() + 0.5 + az * t);
            boolean su = suDerinligi(l, x, z, m) > 0;
            if (!sudaBasla && su) {
                return new int[] {x, z};
            }
            if (sudaBasla) {
                if (!su) {
                    return son;
                }
                son = new int[] {x, z};
            }
        }
        return sudaBasla ? son : null;
    }

    /** Sütunun en üstündeki durgun su derinliği (en çok {@value #DERIN}); chunk yüklü değilse ya da üstte su yoksa 0. */
    static int suDerinligi(ServerLevel l, int x, int z, BlockPos.MutableBlockPos m) {
        if (l.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
            return 0;
        }
        int y = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int derin = 0;
        while (derin < DERIN && y - derin >= l.getMinBuildHeight()
                && l.getFluidState(m.set(x, y - derin, z)).isSourceOfType(Fluids.WATER)) {
            derin++;
        }
        return derin;
    }
}
