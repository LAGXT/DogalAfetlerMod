package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.su.MagaraDolumu.BlokOzu;
import io.github.kadirberkpolat1.dogalafetler.su.MagaraDolumu.Tur;
import org.junit.jupiter.api.Test;

class MagaraDolumuTest {
    @FunctionalInterface
    private interface Kosul {
        boolean test(int x, int y, int z);
    }

    private static boolean kutuda(int x, int y, int z, int x0, int y0, int z0, int x1, int y1, int z1) {
        return x >= x0 && x < x1 && y >= y0 && y < y1 && z >= z0 && z < z1;
    }

    /** Yalnız boş/katı: dışarıya açıklığı olmayan sentetik dünya. */
    private static MagaraDolumu.Hucre ikili(Kosul bos) {
        return (x, y, z) -> bos.test(x, y, z) ? Tur.BOS : Tur.KATI;
    }

    @Test
    void kapaliKutuTamamenDolar() {
        MagaraCebi cep = MagaraDolumu.doldur(ikili((x, y, z) -> kutuda(x, y, z, 0, 10, 0, 5, 13, 5)), 2, 11, 2, 1000,
                10_000);
        assertEquals(75, cep.hacim());
        assertEquals(10, cep.minY());
        assertEquals(13, cep.ustY());
        assertEquals(Integer.MAX_VALUE, cep.agizY());
        assertTrue(cep.icerir(4, 12, 4));
    }

    @Test
    void hacimSiniriDuzBirYuzeyleKeser() {
        MagaraCebi cep = MagaraDolumu.doldur(ikili((x, y, z) -> kutuda(x, y, z, 0, 10, 0, 5, 13, 5)), 2, 12, 2, 60,
                10_000);
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
                ikili((x, y, z) -> kutuda(x, y, z, 0, 10, 0, 3, 12, 3) || kutuda(x, y, z, 4, 10, 0, 7, 12, 3)),
                1, 10, 1, 1000, 10_000);
        assertEquals(18, cep.hacim());
        assertFalse(cep.icerir(5, 10, 1));
    }

    @Test
    void ziyaretSiniriSonsuzHavadaDurur() {
        MagaraCebi cep = MagaraDolumu.doldur(ikili((x, y, z) -> true), 0, 0, 0, 100, 5_000);
        assertTrue(cep.hacim() <= 100);
        assertTrue(cep.ziyaretEdilen() <= 5_000);
    }

    @Test
    void tohumKatiysaCepBostur() {
        MagaraCebi cep = MagaraDolumu.doldur(ikili((x, y, z) -> false), 0, 0, 0, 100, 5_000);
        assertTrue(cep.bos());
        assertEquals(0, cep.hacim());
    }

    @Test
    void alcaktakiKolOnceDolar() {
        MagaraDolumu.Hucre hucre = ikili((x, y, z) -> kutuda(x, y, z, 0, 20, 0, 5, 23, 5)
                || (x == 0 && z == 0 && y >= 12 && y < 20)
                || kutuda(x, y, z, 0, 10, 0, 3, 12, 3));
        MagaraCebi cep = MagaraDolumu.doldur(hucre, 2, 21, 2, 20, 10_000);
        assertEquals(20, cep.hacim());
        assertEquals(14, cep.ustY());
        assertTrue(cep.icerir(1, 10, 1));
        assertTrue(cep.icerir(0, 13, 0));
        assertFalse(cep.icerir(2, 21, 2));
    }

    @Test
    void sutunAraliklariSeviyeyleKirpilir() {
        MagaraCebi cep = MagaraDolumu.doldur(ikili((x, y, z) -> kutuda(x, y, z, 0, 10, 0, 5, 13, 5)), 2, 11, 2, 1000,
                10_000);
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

    /**
     * 8x4x8 oda (y 10..13), y 12..13 hizasında x yönünde 3 blokluk tünel, tünelin ucu (x >= 11) gökyüzünü gören dış hava.
     * Su ağzın eşiğine (y 12) kadar kalır: oda y 10..11 dolar, tünel ve dışarısı kuru.
     */
    private static MagaraDolumu.Hucre yanAgizliMagara() {
        return (x, y, z) -> {
            if (x >= 11 && y >= 0 && y < 30) {
                return Tur.ACIK;
            }
            if (kutuda(x, y, z, 0, 10, 0, 8, 14, 8) || kutuda(x, y, z, 8, 12, 3, 11, 14, 5)) {
                return Tur.BOS;
            }
            return Tur.KATI;
        };
    }

    @Test
    void yanAgizliMagaraEsigeKadarDolar() {
        MagaraCebi cep = MagaraDolumu.doldur(yanAgizliMagara(), 2, 10, 2, 4000, 20_000);
        assertEquals(12, cep.agizY());
        assertEquals(12, cep.ustY());
        assertEquals(8 * 2 * 8, cep.hacim());
        assertTrue(cep.icerir(4, 11, 4));
        assertFalse(cep.icerir(4, 12, 4), "eşiğin üstünde su yok");
        for (int x = 8; x < 11; x++) {
            assertFalse(cep.icerir(x, 12, 3), "tünelde su yok x=" + x);
        }
        SuAraliklari a = new SuAraliklari();
        cep.aralikEkle(4, 4, 100, a);
        a.birlestir();
        assertEquals(1, a.adet());
        assertEquals(10, a.alt(0));
        assertEquals(12, a.ust(0));
        for (int x = 8; x < 14; x++) {
            a.temizle();
            cep.aralikEkle(x, 3, 100, a);
            assertEquals(0, a.adet(), "ağız ve dışarısı kuru x=" + x);
        }
    }

    @Test
    void zeminHizasindaAcikDelikVarsaDolmaz() {
        MagaraDolumu.Hucre hucre = (x, y, z) -> {
            if (x >= 6) {
                return Tur.ACIK;
            }
            if (kutuda(x, y, z, 0, 10, 0, 5, 14, 5) || (x == 5 && y == 10 && z == 2)) {
                return Tur.BOS;
            }
            return Tur.KATI;
        };
        MagaraCebi cep = MagaraDolumu.doldur(hucre, 2, 12, 2, 4000, 20_000);
        assertEquals(10, cep.agizY());
        assertTrue(cep.bos(), "su zemindeki delikten akar");
    }

    @Test
    void tavaniAcikKuyuHicDolmaz() {
        MagaraDolumu.Hucre kuyu = (x, y, z) -> x == 0 && z == 0 && y >= 10 && y < 40 ? Tur.ACIK : Tur.KATI;
        assertTrue(MagaraDolumu.doldur(kuyu, 0, 10, 0, 4000, 20_000).bos());
        assertTrue(MagaraDolumu.doldur(kuyu, 0, 25, 0, 4000, 20_000).bos());
    }

    @Test
    void gecirgenBloklarSuyunIcindeKalir() {
        // Kapalı 5x3x5 oda; zeminde meşale ve ot (çarpışmasız), ortada sarkıt (su tutabilir).
        MagaraDolumu.Hucre hucre = (x, y, z) -> {
            BlokOzu ozu;
            if ((x == 1 && y == 10 && z == 1) || (x == 2 && y == 10 && z == 2) || (x == 3 && y == 11 && z == 3)) {
                ozu = BlokOzu.GECIRGEN;
            } else if (kutuda(x, y, z, 0, 10, 0, 5, 13, 5)) {
                ozu = BlokOzu.HAVA;
            } else {
                ozu = BlokOzu.KATI;
            }
            return MagaraDolumu.sinifla(true, ozu, y, 100);
        };
        MagaraCebi cep = MagaraDolumu.doldur(hucre, 4, 10, 4, 4000, 20_000);
        assertEquals(75, cep.hacim(), "geçirgen bloklar suda delik açmaz");
        assertTrue(cep.icerir(1, 10, 1));
        assertTrue(cep.icerir(2, 10, 2));
        assertTrue(cep.icerir(3, 11, 3));
    }

    @Test
    void siniflandirma() {
        assertEquals(Tur.ACIK, MagaraDolumu.sinifla(false, BlokOzu.HAVA, 5, 100), "yüklü olmayan chunk açık");
        assertEquals(Tur.ACIK, MagaraDolumu.sinifla(false, BlokOzu.KATI, 5, 100), "yüklü olmayan chunk açık");
        assertEquals(Tur.KATI, MagaraDolumu.sinifla(true, BlokOzu.KATI, 80, 64));
        assertEquals(Tur.BOS, MagaraDolumu.sinifla(true, BlokOzu.HAVA, 40, 64));
        assertEquals(Tur.BOS, MagaraDolumu.sinifla(true, BlokOzu.GECIRGEN, 63, 64));
        assertEquals(Tur.ACIK, MagaraDolumu.sinifla(true, BlokOzu.HAVA, 64, 64), "yüzey hizası açık");
        assertEquals(Tur.ACIK, MagaraDolumu.sinifla(true, BlokOzu.GECIRGEN, 64, 64), "yüzeydeki ot açık");
    }

    @Test
    void yaprakAltiAcikSayilir() {
        // Yüzey yaprak hariç ölçülür (MOTION_BLOCKING_NO_LEAVES = 64); taç y 70'te, altındaki hava y 66 gökyüzü
        // ışığı 14 alsa da açıktır, yaprak bloğunun kendisi katıdır.
        assertEquals(Tur.ACIK, MagaraDolumu.sinifla(true, BlokOzu.HAVA, 66, 64));
        assertEquals(Tur.KATI, MagaraDolumu.sinifla(true, BlokOzu.KATI, 70, 64));
    }
}
