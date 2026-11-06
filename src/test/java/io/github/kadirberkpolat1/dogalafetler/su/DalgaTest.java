package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DalgaTest {
    /** +x yonunde, 0.3 blok/tick, 8 blok yuksek, 80 genis, 30 uzun, taban 63. */
    private final Dalga dalga = new Dalga(0, 0, 1, 0, 100, 0.3, 8, 80, 30, 63, 200);

    @Test
    void cepheZamanlaIlerler() {
        assertEquals(0, dalga.cephe(100), 1e-9);
        assertEquals(30, dalga.cephe(200), 1e-9);
    }

    @Test
    void cephenOnundeSuYok() {
        assertTrue(Double.isNaN(dalga.yuzey(31, 0, 200)));
        assertTrue(Double.isNaN(dalga.yuzey(5, 0, 99)));
    }

    @Test
    void cephenArkasindaTabandanYuksekVeArdindaAlcalir() {
        double tepe = dalga.yuzey(26, 0, 200);
        double arka = dalga.yuzey(0, 0, 200);
        assertTrue(tepe > 63 + 6 && tepe <= 71, "tepe=" + tepe);
        assertTrue(arka > 63 && arka < tepe, "arka=" + arka);
    }

    @Test
    void yanGenisligiDisindaSuYok() {
        assertFalse(Double.isNaN(dalga.yuzey(26, 30, 200)));
        assertTrue(Double.isNaN(dalga.yuzey(26, 41, 200)));
    }

    @Test
    void menzilAsilincaBiter() {
        assertFalse(dalga.bitti(200));
        assertTrue(dalga.bitti(100 + (200 + 5 * 30) / 0.3 + 1));
    }
}
