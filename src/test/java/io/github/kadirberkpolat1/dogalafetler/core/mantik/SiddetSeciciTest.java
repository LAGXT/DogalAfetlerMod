package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class SiddetSeciciTest {
    private static final SiddetAyarlari A = SiddetAyarlari.VARSAYILAN;

    private static int[] say(double gun, int maks, int adet, long tohum) {
        SplittableRandom rastgele = new SplittableRandom(tohum);
        int[] sayilar = new int[6];
        for (int i = 0; i < adet; i++) {
            sayilar[SiddetSecici.sec(A, gun, maks, false, rastgele)]++;
        }
        return sayilar;
    }

    @Test
    void gunSifirIleDokuzArasiYalnizSeviyeBir() {
        for (int gun = 0; gun <= 9; gun++) {
            double[] agirlik = SiddetSecici.agirliklar(A, gun + 0.99, 5, false);
            assertTrue(agirlik[0] > 0);
            for (int i = 1; i < 5; i++) {
                assertEquals(0.0, agirlik[i], "gün " + gun + " seviye " + (i + 1));
            }
            int[] sayilar = say(gun, 5, 2000, gun);
            assertEquals(2000, sayilar[1], "gün " + gun + " yalnız S1 çıkmalı");
        }
    }

    @Test
    void gunEsikleriSeviyeleriAcar() {
        assertTrue(SiddetSecici.agirliklar(A, 10, 5, false)[1] > 0, "gün 10'da S2 açılır");
        assertEquals(0.0, SiddetSecici.agirliklar(A, 24.9, 5, false)[2], "gün 25'ten önce S3 yok");
        assertTrue(SiddetSecici.agirliklar(A, 25, 5, false)[2] > 0, "gün 25'te S3 açılır");
        assertEquals(0.0, SiddetSecici.agirliklar(A, 49.9, 5, false)[3], "gün 50'den önce S4 yok");
        assertTrue(SiddetSecici.agirliklar(A, 50, 5, false)[3] > 0, "gün 50'de S4 açılır");
        assertEquals(0.0, SiddetSecici.agirliklar(A, 79.9, 5, false)[4], "gün 80'den önce S5 yok");
        assertTrue(SiddetSecici.agirliklar(A, 80, 5, false)[4] > 0, "gün 80'de S5 açılır");
    }

    @Test
    void acilanSeviyeHemenBaskinOlmaz() {
        for (double gun : new double[] {10, 25, 50, 80, 81, 100, 200, 1000}) {
            double[] agirlik = SiddetSecici.agirliklar(A, gun, 5, false);
            for (int i = 1; i < 5; i++) {
                assertTrue(agirlik[i] < agirlik[i - 1] || agirlik[i] == 0,
                        "gün " + gun + ": S" + (i + 1) + " S" + i + "'den nadir olmalı");
            }
        }
    }

    @Test
    void gunSeksenUstundeS5OlasiAmaS1IleS3tenNadir() {
        int[] sayilar = say(90, 5, 200_000, 7);
        assertTrue(sayilar[5] > 0, "S5 hiç çıkmadı");
        assertTrue(sayilar[5] < sayilar[3], "S5 S3'ten nadir olmalı");
        assertTrue(sayilar[5] < sayilar[2], "S5 S2'den nadir olmalı");
        assertTrue(sayilar[5] < sayilar[1], "S5 S1'den nadir olmalı");
    }

    @Test
    void agirliklarZamanlaYuksekSeviyelereKayar() {
        double[] erken = SiddetSecici.olasiliklar(A, 85, 5, false);
        double[] gec = SiddetSecici.olasiliklar(A, 200, 5, false);
        assertTrue(gec[4] > erken[4], "gün 200'de S5 payı gün 85'ten büyük olmalı");
        assertTrue(gec[0] < erken[0], "gün 200'de S1 payı küçülmeli");
        double toplam = 0;
        for (double p : gec) {
            toplam += p;
        }
        assertEquals(1.0, toplam, 1e-9);
    }

    @Test
    void maksSiddetUcIseDortVeBesHicCikmaz() {
        int[] sayilar = say(500, 3, 100_000, 9);
        assertEquals(0, sayilar[4]);
        assertEquals(0, sayilar[5]);
        assertTrue(sayilar[3] > 0);
        double[] agirlik = SiddetSecici.agirliklar(A, 500, 3, false);
        assertEquals(0.0, agirlik[3]);
        assertEquals(0.0, agirlik[4]);
        assertEquals(1, SiddetSecici.sec(A, 500, 1, false, new SplittableRandom(1)));
    }

    @Test
    void artArdaDortVeyaBesOlmaz() {
        SplittableRandom rastgele = new SplittableRandom(13);
        boolean oncekiYuksek = false;
        int yuksekSayisi = 0;
        for (int i = 0; i < 200_000; i++) {
            int seviye = SiddetSecici.sec(A, 300, 5, oncekiYuksek, rastgele);
            boolean yuksek = SiddetSecici.yuksek(seviye);
            assertFalse(oncekiYuksek && yuksek, "art arda iki yüksek seviye (4-5) çıktı");
            if (yuksek) {
                yuksekSayisi++;
            }
            oncekiYuksek = yuksek;
        }
        assertTrue(yuksekSayisi > 0, "yüksek seviye hiç çıkmadı");
        double[] agirlik = SiddetSecici.agirliklar(A, 300, 5, true);
        assertEquals(0.0, agirlik[3]);
        assertEquals(0.0, agirlik[4]);
        assertTrue(agirlik[2] > 0);
    }

    @Test
    void gelisimKriteriEtkinGunuKisar() {
        SiddetAyarlari gelisimli = A.gelisimIle(true, 2.0);
        assertEquals(100, SiddetSecici.etkinGun(A, 100, 0), 1e-9, "kriter kapalıyken gün değişmez");
        assertEquals(20, SiddetSecici.etkinGun(gelisimli, 100, 10), 1e-9, "puan 10 × 2 = 20 gün");
        assertEquals(100, SiddetSecici.etkinGun(gelisimli, 100, 80), 1e-9, "puan yüksekse gerçek gün");
    }

    @Test
    void yuksekSeviyeTanimi() {
        assertFalse(SiddetSecici.yuksek(0));
        assertFalse(SiddetSecici.yuksek(3));
        assertTrue(SiddetSecici.yuksek(4));
        assertTrue(SiddetSecici.yuksek(5));
    }
}
