package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MagaraDolumuTest {
    private static boolean kutuda(int x, int y, int z, int x0, int y0, int z0, int x1, int y1, int z1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1 && z >= z0 && z < z1;
    }

    @Test
    void kapaliKutuTamamenDolar() {
        MagaraCebi cep = MagaraDolumu.doldur((x, y, z) -> kutuda(x, y, z, 0, 10, 0, 5, 13, 5), 2, 11, 2, 1000, 10_000);
        assertEquals(75, cep.hacim());
        assertEquals(10, cep.minY());
        assertEquals(13, cep.ustY());
        assertTrue(cep.icerir(4, 12, 4));
    }

    @Test
    void hacimSiniriDuzBirYuzeyleKeser() {
        MagaraCebi cep = MagaraDolumu.doldur((x, y, z) -> kutuda(x, y, z, 0, 10, 0, 5, 13, 5), 2, 12, 2, 60, 10_000);
        assertEquals(50, cep.hacim());
        assertEquals(12, cep.ustY());
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                assertTrue(cep.icerir(x, 11, z));
                assertFalse(cep.icerir(x, 12, z));
            }
        }
    }

    @Test
    void katiBloktanGecmez() {
        MagaraCebi cep = MagaraDolumu.doldur(
                (x, y, z) -> kutuda(x, y, z, 0, 10, 0, 3, 12, 3) || kutuda(x, y, z, 4, 10, 0, 7, 12, 3),
                1, 10, 1, 1000, 10_000);
        assertEquals(18, cep.hacim());
        assertFalse(cep.icerir(5, 10, 1));
    }

    @Test
    void ziyaretSiniriSonsuzHavadaDurur() {
        MagaraCebi cep = MagaraDolumu.doldur((x, y, z) -> true, 0, 0, 0, 100, 5_000);
        assertTrue(cep.hacim() <= 100);
        assertTrue(cep.ziyaretEdilen() <= 5_000);
    }

    @Test
    void tohumKatiysaCepBostur() {
        MagaraCebi cep = MagaraDolumu.doldur((x, y, z) -> false, 0, 0, 0, 100, 5_000);
        assertTrue(cep.bos());
        assertEquals(0, cep.hacim());
    }

    @Test
    void alcaktakiKolOnceDolar() {
        MagaraDolumu.Hucre hucre = (x, y, z) -> kutuda(x, y, z, 0, 20, 0, 5, 23, 5)
                || (x == 0 && z == 0 && y >= 12 && y < 20)
                || kutuda(x, y, z, 0, 10, 0, 3, 12, 3);
        MagaraCebi cep = MagaraDolumu.doldur(hucre, 2, 21, 2, 20, 10_000);
        assertEquals(20, cep.hacim());
        assertEquals(14, cep.ustY());
        assertTrue(cep.icerir(1, 10, 1));
        assertTrue(cep.icerir(0, 13, 0));
        assertFalse(cep.icerir(2, 21, 2));
    }

    @Test
    void sutunAraliklariSeviyeyleKirpilir() {
        MagaraCebi cep = MagaraDolumu.doldur((x, y, z) -> kutuda(x, y, z, 0, 10, 0, 5, 13, 5), 2, 11, 2, 1000, 10_000);
        SuAraliklari a = new SuAraliklari();
        cep.aralikEkle(3, 3, 11.5, a);
        a.birlestir();
        assertEquals(1, a.adet());
        assertEquals(10, a.alt(0));
        assertEquals(11.5, a.ust(0));
        a.temizle();
        cep.aralikEkle(7, 7, 13, a);
        assertEquals(0, a.adet());
    }
}
