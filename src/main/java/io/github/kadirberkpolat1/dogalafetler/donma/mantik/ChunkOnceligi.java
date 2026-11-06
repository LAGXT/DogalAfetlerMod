package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Chunk iş sırası (madde 9 ÖNCELİK): en yakın oyuncuya chunk cinsinden uzaklığın karesi küçük olan önce gelir. Eşit
 * uzaklıkta ve oyuncu yokken eklenme sırası korunur (FIFO). Anahtar {@code ChunkPos.asLong} ile aynı kodlamadır.
 */
public final class ChunkOnceligi {
    private ChunkOnceligi() {
    }

    public static long anahtar(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((long) z << 32);
    }

    public static int x(long anahtar) {
        return (int) anahtar;
    }

    public static int z(long anahtar) {
        return (int) (anahtar >>> 32);
    }

    public static long mesafeKaresi(int chunkX, int chunkZ, int[] oyuncuX, int[] oyuncuZ) {
        long enKucuk = Long.MAX_VALUE;
        for (int i = 0; i < oyuncuX.length; i++) {
            long dx = chunkX - oyuncuX[i];
            long dz = chunkZ - oyuncuZ[i];
            enKucuk = Math.min(enKucuk, dx * dx + dz * dz);
        }
        return enKucuk;
    }

    /** Chunk anahtarlarını öncelik sırasına dizer (kararlı sıralama). */
    public static long[] sirala(long[] chunklar, int[] oyuncuX, int[] oyuncuZ) {
        int n = chunklar.length;
        long[] mesafe = new long[n];
        Integer[] sira = new Integer[n];
        for (int i = 0; i < n; i++) {
            mesafe[i] = mesafeKaresi(x(chunklar[i]), z(chunklar[i]), oyuncuX, oyuncuZ);
            sira[i] = i;
        }
        Arrays.sort(sira, Comparator.comparingLong(i -> mesafe[i]));
        long[] sonuc = new long[n];
        for (int i = 0; i < n; i++) {
            sonuc[i] = chunklar[sira[i]];
        }
        return sonuc;
    }
}
