package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
