package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarKavurma.Hedef;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarKavurma.Kategori;
import org.junit.jupiter.api.Test;

/** PL-3 a: pulsar kavurmasının kül dönüşüm tablosu. */
class PulsarKavurmaTest {
    @Test
    void tabloUcNoktalari() {
        assertEquals(Hedef.KABA_TOPRAK, PulsarKavurma.hedef(Kategori.CIMEN, 1f, 0.99));
        assertEquals(Hedef.HAVA_VE_KUL, PulsarKavurma.hedef(Kategori.YAPRAK, 0.1f, 0.5));
        assertEquals(Hedef.KOMURLESMIS_KUTUK, PulsarKavurma.hedef(Kategori.KUTUK, 0.1f, 0.5));
        assertEquals(Hedef.HAVA, PulsarKavurma.hedef(Kategori.BITKI, 0.1f, 0.5));
        assertEquals(Hedef.HAVA, PulsarKavurma.hedef(Kategori.KAR, 0.1f, 0.5));
        assertEquals(Hedef.SU, PulsarKavurma.hedef(Kategori.BUZ, 0.1f, 0.5));
        assertEquals(Hedef.YOK, PulsarKavurma.hedef(Kategori.DIGER, 1f, 0.0));
        assertEquals(Hedef.YOK, PulsarKavurma.hedef(null, 1f, 0.0));
    }

    @Test
    void topragOlasiligiGuctur() {
        assertEquals(Hedef.KUL_BLOGU, PulsarKavurma.hedef(Kategori.TOPRAK, 0.5f, 0.49));
        assertEquals(Hedef.YOK, PulsarKavurma.hedef(Kategori.TOPRAK, 0.5f, 0.5));
        assertEquals(Hedef.KUL_BLOGU, PulsarKavurma.hedef(Kategori.TOPRAK, 1f, 0.999));
    }

    @Test
    void kumYuzdeIkiCamOlur() {
        assertEquals(Hedef.CAM, PulsarKavurma.hedef(Kategori.KUM, 1f, 0.0199));
        assertEquals(Hedef.YOK, PulsarKavurma.hedef(Kategori.KUM, 1f, 0.02));
        int cam = 0;
        for (int i = 0; i < 10_000; i++) {
            if (PulsarKavurma.hedef(Kategori.KUM, 1f, i / 10_000.0) == Hedef.CAM) {
                cam++;
            }
        }
        assertEquals(200, cam);
    }

    @Test
    void gucSifirkenHicbirSeyDegismez() {
        for (Kategori k : Kategori.values()) {
            assertEquals(Hedef.YOK, PulsarKavurma.hedef(k, 0f, 0.0), k.name());
            assertEquals(Hedef.YOK, PulsarKavurma.hedef(k, Float.NaN, 0.0), k.name());
        }
    }

    @Test
    void yaprakIkiIslemDigerleriBir() {
        assertEquals(2, Hedef.HAVA_VE_KUL.islem());
        assertEquals(0, Hedef.YOK.islem());
        for (Hedef h : Hedef.values()) {
            assertTrue(h.islem() <= 2, h.name());
        }
    }

    @Test
    void birikimSeyrekVeBirIleUcKatArasi() {
        assertEquals(0, PulsarKavurma.birikimKati(0f, 0.0));
        assertEquals(0, PulsarKavurma.birikimKati(1f, PulsarKavurma.BIRIKIM_OLASILIGI));
        assertEquals(1, PulsarKavurma.birikimKati(1f, 0.0));
        assertEquals(3, PulsarKavurma.birikimKati(1f, PulsarKavurma.BIRIKIM_OLASILIGI - 1e-9));
        assertEquals(0, PulsarKavurma.birikimKati(0.5f, PulsarKavurma.BIRIKIM_OLASILIGI * 0.5));
        int[] sayi = new int[4];
        for (int i = 0; i < 100_000; i++) {
            sayi[PulsarKavurma.birikimKati(1f, i / 100_000.0)]++;
        }
        assertEquals(96_000, sayi[0]);
        assertTrue(sayi[1] > 0 && sayi[2] > 0 && sayi[3] > 0);
    }

    @Test
    void katTavaniGecilmez() {
        assertEquals(3, PulsarKavurma.yeniKat(1, 3, PulsarKavurma.BIRIKIM_TAVANI));
        assertEquals(5, PulsarKavurma.yeniKat(5, 2, PulsarKavurma.BIRIKIM_TAVANI));
        assertEquals(8, PulsarKavurma.yeniKat(7, 1, PulsarKavurma.KUL_EN_COK_KAT));
        assertEquals(8, PulsarKavurma.yeniKat(8, 1, PulsarKavurma.KUL_EN_COK_KAT));
        assertEquals(1, PulsarKavurma.yeniKat(0, 1, 99));
    }

    /** Ps5: Σ deneme · olasılık / alan = sütun başına isabet; kesir taşınır, yarıçaptan bağımsız. */
    private static double ucGunIsabeti(int yaricap, int adim) {
        double artan = 0;
        long deneme = 0;
        double etkin = 0;
        for (long t = 0; t < PulsarKavurma.UC_GUN_TICK; t += adim) {
            PulsarKavurma.Denemeler d = PulsarKavurma.denemeler(artan, adim, yaricap, 64);
            artan = d.artan();
            deneme += d.sayi();
            etkin += d.etkinAdim();
        }
        assertEquals(PulsarKavurma.UC_GUN_TICK, etkin, 1e-6, "tavanın altında etkin adım = adım");
        return deneme * PulsarKavurma.VARSAYILAN_OLASILIK / PulsarKavurma.alan(yaricap);
    }

    @Test
    void ucGundeSutunBasinaAyniIsabetOlcektenBagimsiz() {
        assertEquals(PulsarKavurma.UC_GUN_ISABET, ucGunIsabeti(96, 1), 1e-3, "üretim: 96 blok, hiz 1");
        assertEquals(PulsarKavurma.UC_GUN_ISABET, ucGunIsabeti(4, 2_000), 0.05, "GameTest: 4 blok, tick başına 2000");
        assertEquals(PulsarKavurma.UC_GUN_ISABET, ucGunIsabeti(32, 30), 1e-2, "hiz 30");
    }

    @Test
    void ucGundeCimenKalmazYuzeyKulOlur() {
        double sutun = PulsarKavurma.alan(96);
        double lambda = PulsarKavurma.UC_GUN_ISABET;
        assertTrue(sutun * Math.exp(-lambda) < 0.01, "96 blokta isabetsiz sütun beklentisi");
        assertTrue(1 - Math.exp(-lambda) * (1 + lambda) >= 0.9, "iki isabet (kaba toprak → kül bloğu) ≥ %90");
        assertTrue(1 - Math.exp(-lambda / 3) >= 0.99, "ilk gün sonunda bile çimenin çoğu kalkmış");
    }

    @Test
    void tavanFazlayiAtarEtkinAdimiKucultur() {
        PulsarKavurma.Denemeler hiz1 = PulsarKavurma.denemeler(0, 1, 96, 64);
        assertEquals(10, hiz1.sayi());
        assertEquals(PulsarKavurma.DENEME_YOGUNLUGU * PulsarKavurma.alan(96) - 10, hiz1.artan(), 1e-9);
        assertEquals(1.0, hiz1.etkinAdim(), 1e-12);
        PulsarKavurma.Denemeler hiz1000 = PulsarKavurma.denemeler(0.5, 1_000, 96, 64);
        double istenen = 0.5 + 1_000 * PulsarKavurma.DENEME_YOGUNLUGU * PulsarKavurma.alan(96);
        assertEquals(64, hiz1000.sayi());
        assertEquals(0.0, hiz1000.artan(), 0.0, "tavan aşılınca kesir birikmez");
        assertEquals(1_000 * 64 / istenen, hiz1000.etkinAdim(), 1e-9);
        PulsarKavurma.Denemeler yok = PulsarKavurma.denemeler(0.3, 0, 96, 64);
        assertEquals(0, yok.sayi());
        assertEquals(0.0, yok.etkinAdim(), 0.0);
        assertEquals(0.3, yok.artan(), 0.0);
    }

    @Test
    void yananOrtuAltindaTaramaSurer() {
        assertTrue(PulsarKavurma.altinaSurer(Hedef.HAVA));
        assertTrue(PulsarKavurma.altinaSurer(Hedef.HAVA_VE_KUL));
        assertTrue(PulsarKavurma.altinaSurer(Hedef.KOMURLESMIS_KUTUK));
        for (Hedef zemin : new Hedef[] {Hedef.YOK, Hedef.KABA_TOPRAK, Hedef.KUL_BLOGU, Hedef.SU, Hedef.CAM}) {
            assertFalse(PulsarKavurma.altinaSurer(zemin), zemin.name());
        }
    }
}
