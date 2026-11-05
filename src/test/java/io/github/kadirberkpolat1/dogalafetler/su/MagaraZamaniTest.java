package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MagaraZamaniTest {
    private final MagaraZamani zaman = new MagaraZamani(1000, 10, 20, 0.1, 200, 0.05);

    @Test
    void dolarBeklerBuharlasirVeBiter() {
        assertEquals(MagaraZamani.Asama.DOLUYOR, zaman.asama(1000));
        assertEquals(10, zaman.seviye(1000), 1e-9);
        assertEquals(15, zaman.seviye(1050), 1e-9);
        assertEquals(MagaraZamani.Asama.BEKLIYOR, zaman.asama(1100));
        assertEquals(20, zaman.seviye(1250), 1e-9);
        assertEquals(MagaraZamani.Asama.BUHARLASIYOR, zaman.asama(1400));
        assertEquals(15, zaman.seviye(1400), 1e-9);
        assertEquals(MagaraZamani.Asama.BITTI, zaman.asama(1500));
        assertTrue(Double.isNaN(zaman.seviye(1600)));
    }

    @Test
    void baslamadanOnceSuYok() {
        assertTrue(Double.isNaN(zaman.seviye(999)));
    }
}
