package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
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

    @Test
    void asamaIleriBirSonrakiAsamayiHemenBaslatir() {
        MagaraZamani z = new MagaraZamani(100, 10, 14, 0.1, 200, 0.05);
        MagaraZamani dolu = z.ileri(110);
        assertEquals(MagaraZamani.Asama.BEKLIYOR, dolu.asama(110));
        assertEquals(14, dolu.seviye(110), 1e-9);
        MagaraZamani kuruyor = dolu.ileri(120);
        assertEquals(MagaraZamani.Asama.BUHARLASIYOR, kuruyor.asama(120));
        assertEquals(14, kuruyor.seviye(120), 1e-9);
        assertEquals(13, kuruyor.seviye(140), 1e-9);
        MagaraZamani bitti = kuruyor.ileri(141);
        assertEquals(MagaraZamani.Asama.BITTI, bitti.asama(141));
        assertSame(bitti, bitti.ileri(150));
    }

    @Test
    void kaydirmaDuraklatmayiTelafiEder() {
        MagaraZamani kayik = zaman.kaydir(500);
        assertEquals(zaman.seviye(1250), kayik.seviye(1750), 1e-9);
        assertEquals(MagaraZamani.Asama.BUHARLASIYOR, kayik.asama(1900));
    }

    @Test
    void kurumaKademeliVeSonundaSuKalmaz() {
        // 4 blok, 0,25 blok / 6 sn (120 tick): kuruma 1920 tick, her 120 tickte seviye iner.
        MagaraZamani z = new MagaraZamani(0, 10, 14, 0.1, 100, 0.25 / 120);
        long kurumaBasi = 40 + 100;
        double onceki = Double.MAX_VALUE;
        for (long t = kurumaBasi + 1; t < kurumaBasi + 1920; t += 120) {
            double seviye = z.seviye(t);
            assertTrue(seviye < onceki, "seviye iniyor: " + seviye);
            onceki = seviye;
        }
        assertEquals(MagaraZamani.Asama.BITTI, z.asama(kurumaBasi + 1921));
        assertTrue(Double.isNaN(z.seviye(kurumaBasi + 1921)));
    }
}
