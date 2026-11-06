package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class OdulZariTest {
    private static final int DENEME = 400_000;

    @Test
    void varsayilanSanslar() {
        assertEquals(0.03, OdulZari.Sanslar.VARSAYILAN.parca());
        assertEquals(0.05, OdulZari.Sanslar.VARSAYILAN.buyukParca());
        assertEquals(0.003, OdulZari.Sanslar.VARSAYILAN.cekirdek());
    }

    @Test
    void kucukMeteorYuzdeUcParcaHicCekirdek() {
        Random r = new Random(1);
        int parca = 0;
        int cekirdek = 0;
        for (int i = 0; i < DENEME; i++) {
            OdulZari.Sonuc s = OdulZari.at(false, OdulZari.Sanslar.VARSAYILAN, r);
            parca += s.parca() ? 1 : 0;
            cekirdek += s.cekirdek() ? 1 : 0;
        }
        assertEquals(0.03, parca / (double) DENEME, 0.0015);
        assertEquals(0, cekirdek, "Kozmik Çekirdek yalnız büyük meteorda");
    }

    @Test
    void buyukMeteorYuzdeBesParcaBinde3Cekirdek() {
        Random r = new Random(2);
        int parca = 0;
        int cekirdek = 0;
        for (int i = 0; i < DENEME; i++) {
            OdulZari.Sonuc s = OdulZari.at(true, OdulZari.Sanslar.VARSAYILAN, r);
            parca += s.parca() ? 1 : 0;
            cekirdek += s.cekirdek() ? 1 : 0;
        }
        assertEquals(0.05, parca / (double) DENEME, 0.002);
        assertEquals(0.003, cekirdek / (double) DENEME, 0.0004);
    }

    @Test
    void tohumluZarTekrarlanirSiniraBagli() {
        OdulZari.Sanslar hep = new OdulZari.Sanslar(1, 1, 1);
        assertEquals(new OdulZari.Sonuc(true, true), OdulZari.at(true, hep, new Random(5)));
        assertEquals(new OdulZari.Sonuc(true, false), OdulZari.at(false, hep, new Random(5)));
        OdulZari.Sanslar hic = new OdulZari.Sanslar(0, 0, 0);
        assertEquals(OdulZari.Sonuc.YOK, OdulZari.at(true, hic, new Random(5)));
        for (long t = 0; t < 50; t++) {
            assertEquals(OdulZari.at(true, OdulZari.Sanslar.VARSAYILAN, new Random(t)),
                    OdulZari.at(true, OdulZari.Sanslar.VARSAYILAN, new Random(t)));
        }
        assertThrows(IllegalArgumentException.class, () -> new OdulZari.Sanslar(1.5, 0, 0));
        assertTrue(OdulZari.Sonuc.YOK.bos());
    }
}
