package io.github.kadirberkpolat1.dogalafetler.donma;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DonmaEsikleriTest {
    private final DonmaEsikleri e = DonmaEsikleri.VARSAYILAN;

    @Test
    void karKatmaniIlerlemeyleArtarCimendeTekKattaKalir() {
        assertEquals(0, e.karKatmani(24, false));
        assertEquals(1, e.karKatmani(25, false));
        assertEquals(5, e.karKatmani(100, false));
        assertEquals(1, e.karKatmani(100, true));
        int onceki = 0;
        for (int l = 0; l <= 110; l++) {
            int k = e.karKatmani(l, false);
            assertTrue(k >= onceki, "kar katmanı azaldı: " + l);
            onceki = k;
        }
    }

    @Test
    void kiragiEsigi() {
        assertFalse(e.kiragi(9));
        assertTrue(e.kiragi(10));
    }

    @Test
    void kiyidaOnceVeKalinDonar() {
        assertEquals(1, e.buzKalinligi(25, 0));
        assertEquals(0, e.buzKalinligi(25, 10));
        assertEquals(4, e.buzKalinligi(100, 0));
        assertEquals(3, e.buzKalinligi(100, 15));
        assertEquals(e.buzKalinligi(100, 15), e.buzKalinligi(100, 40), "uzaklık sınırda kesilmeli");
        for (int c = 0; c <= 15; c++) {
            int onceki = 0;
            for (int l = 0; l <= 110; l++) {
                int k = e.buzKalinligi(l, c);
                assertTrue(k >= onceki && k <= 4, "kalınlık tekdüze değil: l=" + l + " c=" + c);
                onceki = k;
            }
        }
    }

    @Test
    void sifirSeviyedeEnBuyukTitresimleBileHicbirSeyDonmaz() {
        int l = e.titresimGenligi();
        assertEquals(0, e.karKatmani(l, false));
        assertFalse(e.kiragi(l));
        assertEquals(0, e.buzKalinligi(l, 0));
    }

    @Test
    void titresimDeterministikVeSinirli() {
        for (int i = 0; i < 2000; i++) {
            int t = DonmaEsikleri.titresim(99L, i * 31 - 9000, i * 17 + 4000, 8);
            assertTrue(t >= -8 && t <= 8, "titreşim sınır dışı: " + t);
            assertEquals(t, DonmaEsikleri.titresim(99L, i * 31 - 9000, i * 17 + 4000, 8));
        }
    }

    @Test
    void titresimEnDusukEsigeUlasamaz() {
        assertThrows(IllegalArgumentException.class,
                () -> new DonmaEsikleri(10, new int[] {25, 55, 70, 85, 95}, 20, 20, 2, 15, 4, 10));
        assertThrows(IllegalArgumentException.class,
                () -> new DonmaEsikleri(10, new int[] {25, 20}, 20, 20, 2, 15, 4, 8));
    }
}
