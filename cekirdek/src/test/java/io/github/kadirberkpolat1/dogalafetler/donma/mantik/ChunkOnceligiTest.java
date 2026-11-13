package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChunkOnceligiTest {
    private static long a(int x, int z) {
        return ChunkOnceligi.anahtar(x, z);
    }

    @Test
    void anahtarGidipGelir() {
        for (int[] p : new int[][] {{0, 0}, {-1, 5}, {125, -125}, {-1_875_000, 1_875_000}}) {
            long k = a(p[0], p[1]);
            assertEquals(p[0], ChunkOnceligi.x(k));
            assertEquals(p[1], ChunkOnceligi.z(k));
        }
    }

    @Test
    void enYakinOyuncuyaUzaklikKaresi() {
        assertEquals(25, ChunkOnceligi.mesafeKaresi(3, 4, new int[] {0, 100}, new int[] {0, 100}));
        assertEquals(Long.MAX_VALUE, ChunkOnceligi.mesafeKaresi(3, 4, new int[0], new int[0]));
    }

    @Test
    void oyuncuyaYakinChunkOnceGelir() {
        long[] sira = ChunkOnceligi.sirala(new long[] {a(10, 10), a(1, 0), a(4, -3)}, new int[] {0}, new int[] {0});
        assertArrayEquals(new long[] {a(1, 0), a(4, -3), a(10, 10)}, sira);
    }

    @Test
    void oyuncuYoksaEklenmeSirasiKorunur() {
        long[] girdi = {a(10, 10), a(1, 0), a(4, -3)};
        assertArrayEquals(girdi, ChunkOnceligi.sirala(girdi, new int[0], new int[0]));
    }

    @Test
    void esitUzakliktaEklenmeSirasiKorunur() {
        long[] girdi = {a(0, 2), a(2, 0), a(-2, 0), a(0, -2)};
        assertArrayEquals(girdi, ChunkOnceligi.sirala(girdi, new int[] {0}, new int[] {0}));
    }
}
