package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KiyiUzakligiTest {
    private static final byte S = KiyiUzakligi.SU;
    private static final byte K = KiyiUzakligi.KARA;
    private static final byte B = KiyiUzakligi.BILINMIYOR;

    @Test
    void golKenardanIceriSayilir() {
        // 7x7 bölge: dışta 1 blok kara, içte 5x5 göl. Halka (bölge dışı) da kara.
        int g = 7;
        byte[] tur = new byte[(g + 2) * (g + 2)];
        for (int z = 0; z < g + 2; z++) {
            for (int x = 0; x < g + 2; x++) {
                boolean gol = x >= 2 && x <= 6 && z >= 2 && z <= 6;
                tur[z * (g + 2) + x] = gol ? S : K;
            }
        }
        int[] u = KiyiUzakligi.hesapla(g, g, tur, 15);
        assertEquals(-1, u[0]);
        assertEquals(0, u[1 * g + 1]);
        assertEquals(1, u[2 * g + 2]);
        assertEquals(2, u[3 * g + 3]);
        assertEquals(0, u[5 * g + 5]);
    }

    @Test
    void kiyiGorulmeyenSuSinirDegeriAlir() {
        int g = 4;
        byte[] tur = new byte[(g + 2) * (g + 2)];
        java.util.Arrays.fill(tur, B);
        for (int z = 1; z <= g; z++) {
            for (int x = 1; x <= g; x++) {
                tur[z * (g + 2) + x] = S;
            }
        }
        int[] u = KiyiUzakligi.hesapla(g, g, tur, 15);
        int[] beklenen = new int[g * g];
        java.util.Arrays.fill(beklenen, 15);
        assertArrayEquals(beklenen, u);
    }

    @Test
    void halkadakiKaraKomsuSutunuKiyiYapar() {
        int g = 3;
        byte[] tur = new byte[(g + 2) * (g + 2)];
        java.util.Arrays.fill(tur, S);
        tur[2 * (g + 2)] = K; // bölgenin batısındaki halka hücresi (x=-1, z=1)
        int[] u = KiyiUzakligi.hesapla(g, g, tur, 15);
        assertEquals(0, u[1 * g]);
        assertEquals(1, u[1 * g + 1]);
        assertEquals(2, u[1 * g + 2]);
    }
}
