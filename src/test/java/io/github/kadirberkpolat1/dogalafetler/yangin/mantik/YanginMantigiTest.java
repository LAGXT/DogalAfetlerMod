package io.github.kadirberkpolat1.dogalafetler.yangin.mantik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YanginMantigiTest {
    @Test
    void butceKismaCarpaniylaDuser() {
        assertEquals(64, YanginMantigi.butce(64, 1.0));
        assertEquals(32, YanginMantigi.butce(64, 0.5));
        assertEquals(16, YanginMantigi.butce(64, 0.25));
        assertEquals(1, YanginMantigi.butce(2, 0.25), "kısma bütçeyi sıfırlamaz");
        assertEquals(0, YanginMantigi.butce(0, 1.0));
    }

    @Test
    void tavanAsilincaEnEskilerSoner() {
        assertEquals(0, YanginMantigi.tasanSayisi(1500, 1500));
        assertEquals(7, YanginMantigi.tasanSayisi(1507, 1500));
        assertEquals(0, YanginMantigi.tasanSayisi(3, 1500));
    }

    @Test
    void yanmazBlokAslaTutusmaz() {
        assertEquals(0, YanginMantigi.tutusmaOlasiligi(0, 10, 2.5, 1.6, 1));
        assertEquals(0, YanginMantigi.tutusmaOlasiligi(60, 0, 1, 1, 1), "hız çarpanı 0 yayılmayı durdurur");
    }

    @Test
    void olasilikVanillaYayilmaHizindanGelir() {
        double cimen = YanginMantigi.tutusmaOlasiligi(60, 1, 1, 1, 1);
        double yaprak = YanginMantigi.tutusmaOlasiligi(30, 1, 1, 1, 1);
        double odun = YanginMantigi.tutusmaOlasiligi(5, 1, 1, 1, 1);
        assertEquals(0.6, cimen, 1e-9);
        assertTrue(cimen > yaprak && yaprak > odun);
        assertEquals(1.0, YanginMantigi.tutusmaOlasiligi(60, 5, 2, 1.6, 1), "olasılık 1'i aşmaz");
    }

    @Test
    void ruzgarEgimVeNemKatsayilari() {
        double ruzgarYonunde = YanginMantigi.ruzgarKatsayisi(0.6, 0, 1, 0);
        double karsi = YanginMantigi.ruzgarKatsayisi(0.6, 0, -1, 0);
        double yan = YanginMantigi.ruzgarKatsayisi(0.6, 0, 0, 1);
        assertEquals(1.6, ruzgarYonunde, 1e-9);
        assertEquals(0.4, karsi, 1e-9);
        assertEquals(1.0, yan, 1e-9);
        assertEquals(1.0, YanginMantigi.ruzgarKatsayisi(0, 0, 1, 0), "rüzgârsızken etkisiz");
        assertEquals(1.0, YanginMantigi.ruzgarKatsayisi(0.6, 0, 0, 0), "dikey yayılmada etkisiz");
        assertTrue(YanginMantigi.egimKatsayisi(1) > YanginMantigi.egimKatsayisi(0));
        assertTrue(YanginMantigi.egimKatsayisi(0) > YanginMantigi.egimKatsayisi(-1));
        assertEquals(1.0, YanginMantigi.nemKatsayisi(0), 1e-9);
        assertEquals(0.3, YanginMantigi.nemKatsayisi(1), 1e-9);
        assertEquals(0.3, YanginMantigi.nemKatsayisi(5), 1e-9);
    }

    @Test
    void yaprakOduncanCabukYanar() {
        int yaprak = YanginMantigi.yanmaSuresiTick(60);
        int odun = YanginMantigi.yanmaSuresiTick(5);
        int cimen = YanginMantigi.yanmaSuresiTick(100);
        assertTrue(cimen < yaprak && yaprak < odun, cimen + " < " + yaprak + " < " + odun);
        assertEquals(120, yaprak);
    }

    @Test
    void yagmurdaSonmeYaslaArtar() {
        assertEquals(0.25, YanginMantigi.yagmurSonmeOlasiligi(0.25, 0), 1e-9);
        assertEquals(0.55, YanginMantigi.yagmurSonmeOlasiligi(0.25, 1), 1e-9);
        assertEquals(0.55, YanginMantigi.yagmurSonmeOlasiligi(0.25, 3), 1e-9);
        assertEquals(1.0, YanginMantigi.yagmurSonmeOlasiligi(0.9, 1), 1e-9);
        assertEquals(0.0, YanginMantigi.yagmurSonmeOlasiligi(0, 0), 1e-9);
    }

    @Test
    void blokHasariKapaliykenYakitDegismez() {
        assertEquals(YanginMantigi.YanmaSonucu.HAVA, YanginMantigi.yanmaSonucu(true));
        assertEquals(YanginMantigi.YanmaSonucu.DEGISMEZ, YanginMantigi.yanmaSonucu(false));
    }

    @Test
    void oncelikYakinOnceUzakSonundaIslenir() {
        assertTrue(YanginMantigi.oncelik(4 * 4, 0) < YanginMantigi.oncelik(100 * 100, 0));
        assertTrue(YanginMantigi.oncelik(100 * 100, 2000) < YanginMantigi.oncelik(10 * 10, 0),
                "uzun bekleyen uzak hücre öne geçer");
    }

    @Test
    void seciciEnKucukleriSiraylaVerir() {
        double[] puan = {5, 1, 9, 1, 3, 7};
        assertArrayEquals(new int[] {1, 3, 4}, OncelikSecici.enIyiler(puan, puan.length, 3));
        assertArrayEquals(new int[] {1, 3, 4, 0, 5, 2}, OncelikSecici.enIyiler(puan, puan.length, 10));
        assertArrayEquals(new int[] {1}, OncelikSecici.enIyiler(puan, 2, 1), "yalnız ilk n girdi");
        assertEquals(0, OncelikSecici.enIyiler(puan, puan.length, 0).length);
    }

    @Test
    void kivilcimVeRuzgar() {
        assertEquals(1, YanginMantigi.kivilcimYaricapi(0));
        assertEquals(2, YanginMantigi.kivilcimYaricapi(0.5));
        assertEquals(3, YanginMantigi.kivilcimYaricapi(1));
        assertArrayEquals(YanginMantigi.gununRuzgari(7), YanginMantigi.gununRuzgari(7));
        double[] r = YanginMantigi.gununRuzgari(3);
        assertEquals(0.6, Math.hypot(r[0], r[1]), 1e-9);
    }
}
