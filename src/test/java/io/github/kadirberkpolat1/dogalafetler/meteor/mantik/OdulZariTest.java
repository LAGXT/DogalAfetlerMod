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
        assertEquals(0.10, OdulZari.Sanslar.VARSAYILAN.buyukParca());
        assertEquals(0.008, OdulZari.Sanslar.VARSAYILAN.cekirdek());
        assertEquals(0.25, OdulZari.Sanslar.VARSAYILAN.buyukEkParca());
    }

    @Test
    void kucukMeteorYuzdeUcParcaHicCekirdekHicEkParca() {
        Random r = new Random(1);
        int parca = 0;
        int cekirdek = 0;
        int ek = 0;
        for (int i = 0; i < DENEME; i++) {
            OdulZari.Sonuc s = OdulZari.at(false, OdulZari.Sanslar.VARSAYILAN, r);
            parca += s.parca() ? 1 : 0;
            cekirdek += s.cekirdek() ? 1 : 0;
            ek += s.ekParca();
        }
        assertEquals(0.03, parca / (double) DENEME, 0.0015);
        assertEquals(0, cekirdek, "Kozmik Çekirdek yalnız büyük meteorda");
        assertEquals(0, ek, "ek parça yalnız büyük meteorda");
    }

    @Test
    void buyukMeteorYuzdeOnParcaBindeSekizCekirdekParcaBasinaYarimEk() {
        Random r = new Random(2);
        int parca = 0;
        int cekirdek = 0;
        int parcaAdedi = 0;
        int toplam = 0;
        for (int i = 0; i < DENEME; i++) {
            OdulZari.Sonuc s = OdulZari.at(true, OdulZari.Sanslar.VARSAYILAN, r);
            parca += s.parca() ? 1 : 0;
            cekirdek += s.cekirdek() ? 1 : 0;
            parcaAdedi += s.parcaAdedi();
            toplam += s.adet();
        }
        assertEquals(0.10, parca / (double) DENEME, 0.003);
        assertEquals(0.008, cekirdek / (double) DENEME, 0.0007);
        assertEquals(0.15, parcaAdedi / (double) DENEME, 0.004, "parça başına ortalama 1,5 parça");
        // K-E74: büyük meteor başına beklenen eşya eski 0,053'ün yaklaşık üç katı (0,158).
        assertEquals(0.158, toplam / (double) DENEME, 0.004);
        assertTrue(toplam / (double) DENEME > 2.5 * 0.053, "belirgin artış");
    }

    @Test
    void tohumluZarTekrarlanirSiniraBagli() {
        OdulZari.Sanslar hep = new OdulZari.Sanslar(1, 1, 1, 0);
        assertEquals(new OdulZari.Sonuc(true, true), OdulZari.at(true, hep, new Random(5)));
        assertEquals(new OdulZari.Sonuc(true, false), OdulZari.at(false, hep, new Random(5)));
        OdulZari.Sanslar bol = new OdulZari.Sanslar(1, 1, 1, 1);
        assertEquals(new OdulZari.Sonuc(true, true, OdulZari.EN_COK_EK), OdulZari.at(true, bol, new Random(5)));
        assertEquals(new OdulZari.Sonuc(true, false), OdulZari.at(false, bol, new Random(5)), "küçükte ek parça yok");
        OdulZari.Sanslar hic = new OdulZari.Sanslar(0, 0, 0, 1);
        assertEquals(OdulZari.Sonuc.YOK, OdulZari.at(true, hic, new Random(5)), "parça yoksa ek parça da yok");
        for (long t = 0; t < 50; t++) {
            assertEquals(OdulZari.at(true, OdulZari.Sanslar.VARSAYILAN, new Random(t)),
                    OdulZari.at(true, OdulZari.Sanslar.VARSAYILAN, new Random(t)));
        }
        assertThrows(IllegalArgumentException.class, () -> new OdulZari.Sanslar(1.5, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new OdulZari.Sanslar(0, 0, 0, -0.1));
        assertTrue(OdulZari.Sonuc.YOK.bos());
    }

    @Test
    void sonucAdetleriVeSiniri() {
        OdulZari.Sonuc bol = new OdulZari.Sonuc(true, true, 2);
        assertEquals(3, bol.parcaAdedi());
        assertEquals(4, bol.adet());
        assertEquals(0, new OdulZari.Sonuc(false, true).parcaAdedi());
        assertEquals(1, new OdulZari.Sonuc(false, true).adet());
        assertThrows(IllegalArgumentException.class, () -> new OdulZari.Sonuc(false, true, 1));
        assertThrows(IllegalArgumentException.class, () -> new OdulZari.Sonuc(true, false, OdulZari.EN_COK_EK + 1));
    }
}
