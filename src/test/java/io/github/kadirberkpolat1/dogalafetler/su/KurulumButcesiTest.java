package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KurulumButcesiTest {
    private static final long MS = 1_000_000L;

    @Test
    void ilkTickTamButce() {
        KurulumButcesi b = new KurulumButcesi();
        b.tickBasla(1, 2 * MS);
        assertTrue(b.calisabilir());
        b.harca(MS + MS / 2);
        assertTrue(b.calisabilir());
        b.harca(MS);
        assertFalse(b.calisabilir());
        assertEquals(MS / 2 * 5, b.harcanan());
    }

    @Test
    void asimSonrakiTicktenDusulur() {
        KurulumButcesi b = new KurulumButcesi();
        b.tickBasla(1, 2 * MS);
        b.harca(MS * 5 / 2);
        b.tickBasla(2, 2 * MS);
        assertEquals(MS / 2, b.harcanan());
        assertEquals(MS * 3 / 2, b.kalan());
    }

    @Test
    void buyukAsimBirkacTickeYayilir() {
        KurulumButcesi b = new KurulumButcesi();
        b.tickBasla(1, 2 * MS);
        b.harca(5 * MS);
        b.tickBasla(2, 2 * MS);
        assertFalse(b.calisabilir(), "3 ms borç 2 ms bütçeyi aşar");
        b.tickBasla(3, 2 * MS);
        assertEquals(MS, b.kalan());
    }

    @Test
    void ayniTickteYenidenBaslamaSifirlamaz() {
        KurulumButcesi b = new KurulumButcesi();
        b.tickBasla(7, 2 * MS);
        b.harca(MS);
        b.tickBasla(7, 2 * MS);
        assertEquals(MS, b.harcanan());
    }

    @Test
    void uzunSuredeOrtalamaButceyiAsmaz() {
        KurulumButcesi b = new KurulumButcesi();
        long parca = MS * 7 / 10;
        long toplam = 0;
        for (long t = 0; t < 100; t++) {
            b.tickBasla(t, 2 * MS);
            while (b.calisabilir()) {
                b.harca(parca);
                toplam += parca;
            }
        }
        assertTrue(toplam <= 100 * 2 * MS + parca, "toplam " + toplam);
        assertTrue(toplam >= 99 * 2 * MS, "bütçe boşa gitmemeli: " + toplam);
    }
}
