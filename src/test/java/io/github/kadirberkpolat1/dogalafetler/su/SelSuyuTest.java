package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Sel suyunun parametrik eğrisi (K-E80): başlamadan önce 0, yükselir, tepede bekler, çekilir, bitince 0. */
class SelSuyuTest {
    /** 100'de başlar; 200 tick yükselir, 400 tick tepede, 300 tick çekilir; tepe 4 blok. */
    private static final SelSuyu S = new SelSuyu(100, 200, 400, 300, 4.0);

    @Test
    void baslamadanOnceVeBittiktenSonraSifirdir() {
        assertEquals(0, S.artis(0));
        assertEquals(0, S.artis(100));
        assertEquals(0, S.artis(S.bitis()));
        assertEquals(0, S.artis(1e9));
        assertEquals(1000, S.bitis());
        assertEquals(900, S.toplamSure());
        assertTrue(!S.bitti(999.9) && S.bitti(1000));
    }

    @Test
    void yukselirTepedeBekleyipCekilir() {
        assertEquals(4.0 * Math.sin(Math.PI / 4), S.artis(200), 1e-9, "yükselme sinüs biçiminde: yarı sürede üçte iki");
        assertEquals(4.0, S.artis(300), 1e-9);
        assertEquals(4.0, S.artis(500), 1e-9);
        assertEquals(4.0, S.artis(700), 1e-9);
        assertEquals(2.0, S.artis(850), 1e-9, "çekilmenin yarısında yarıya indi");
    }

    @Test
    void yukselirkenMonotonArtarCekilirkenMonotonAzalirVeTepeyiAsmaz() {
        double onceki = 0;
        for (double t = 100; t <= 300; t += 2) {
            double a = S.artis(t);
            assertTrue(a >= onceki - 1e-12 && a <= 4.0 + 1e-12, "yükselme t=" + t + " " + a);
            onceki = a;
        }
        for (double t = 700; t <= 1000; t += 2) {
            double a = S.artis(t);
            assertTrue(a <= onceki + 1e-12 && a >= 0, "çekilme t=" + t + " " + a);
            onceki = a;
        }
    }

    @Test
    void suIlkSaniyelerdeGorunurSekildeKalkar() {
        double gecikme = S.gorunmeGecikmesi();
        assertTrue(gecikme > 0 && gecikme < 20, "sel hemen kalkmaya başlar: " + gecikme + " tick");
        assertEquals(SelSuyu.GORUNUR_ARTIS, S.artis(S.baslangic() + gecikme), 1e-9);
        assertTrue(S.artis(S.baslangic() + gecikme - 1) < SelSuyu.GORUNUR_ARTIS, "gecikmeden önce görünür değil");
        assertEquals(Double.POSITIVE_INFINITY, new SelSuyu(0, 100, 100, 100, 0.05).gorunmeGecikmesi(), "hedef görünür değil");
    }

    @Test
    void negatifDegerlerSifirlanir() {
        SelSuyu s = new SelSuyu(0, -5, -5, -5, -3);
        assertEquals(0, s.toplamSure());
        assertEquals(0, s.artis(1));
    }
}
