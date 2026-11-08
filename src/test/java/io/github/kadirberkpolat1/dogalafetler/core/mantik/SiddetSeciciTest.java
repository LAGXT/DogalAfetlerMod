package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import java.util.Optional;
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

    private static double[] p(double gun) {
        return SiddetSecici.olasiliklar(A, gun, 5, false);
    }

    @Test
    void gunSifirdaYalnizSeviyeBir() {
        double[] agirlik = SiddetSecici.agirliklar(A, 0.99, 5, false);
        assertTrue(agirlik[0] > 0);
        for (int i = 1; i < 5; i++) {
            assertEquals(0.0, agirlik[i], "gün 0 seviye " + (i + 1));
        }
        assertEquals(2000, say(0, 5, 2000, 1)[1], "gün 0'da yalnız S1 çıkmalı");
    }

    /** K-E67 hedef eğrisi: S2 gün 1, S3 gün 4, S4 gün 9 (8–10), S5 gün 16 (15–18). */
    @Test
    void gunEsikleriSeviyeleriErkenAcar() {
        assertEquals(1, A.seviye2Gunu());
        assertEquals(4, A.seviye3Gunu());
        assertTrue(A.seviye4Gunu() >= 8 && A.seviye4Gunu() <= 10, "S4 gün 8–10");
        assertTrue(A.seviye5Gunu() >= 15 && A.seviye5Gunu() <= 18, "S5 gün 15–18");
        for (int seviye = 2; seviye <= 5; seviye++) {
            int esik = A.esik(seviye);
            assertEquals(0.0, SiddetSecici.agirliklar(A, esik - 0.01, 5, false)[seviye - 1], "S" + seviye + " eşikten önce");
            assertTrue(SiddetSecici.agirliklar(A, esik, 5, false)[seviye - 1] > 0, "S" + seviye + " eşikte açılır");
        }
    }

    /** Açılış rampası kısa (birkaç gün) ve başlangıç payı anlamlı: açıldığı gün S4 ≥ %2, S5 ≥ %1. */
    @Test
    void acilisRampasiKisaVeBaslangicPayiAnlamli() {
        assertTrue(A.acilisRampaGunu() <= 5, "rampa birkaç gün");
        assertTrue(p(A.seviye4Gunu())[3] >= 0.02, "S4 açıldığı gün: " + p(A.seviye4Gunu())[3]);
        assertTrue(p(A.seviye5Gunu())[4] >= 0.01, "S5 açıldığı gün: " + p(A.seviye5Gunu())[4]);
        double tam = SiddetSecici.agirliklar(A, A.seviye4Gunu() + A.acilisRampaGunu(), 5, false)[3];
        double sonra = SiddetSecici.agirliklar(A, A.seviye4Gunu() + A.acilisRampaGunu() + 0.001, 5, false)[3];
        assertEquals(tam, sonra, 1e-4, "rampa bitince tam ağırlık");
    }

    /** K-E67 olasılık tablosu kilidi (art arda kuralı ve yoğunluk yokken). */
    @Test
    void hedefOlasilikTablosu() {
        System.out.println("| Gün | S1 | S2 | S3 | S4 | S5 |");
        System.out.println("|---|---|---|---|---|---|");
        for (int gun : new int[] {0, 1, 2, 4, 9, 10, 16, 20, 30, 40, 60, 100}) {
            double[] o = p(gun);
            System.out.printf(Locale.ROOT, "| %d | %.1f | %.1f | %.1f | %.1f | %.1f |%n", gun, o[0] * 100, o[1] * 100,
                    o[2] * 100, o[3] * 100, o[4] * 100);
        }
        double[] gun2 = p(2);
        assertEquals(1.0, gun2[0] + gun2[1], 1e-12, "ilk günler yalnız S1–S2");
        assertTrue(gun2[1] > 0.15, "S2 ilk günlerde anlamlı pay alır: " + gun2[1]);
        assertTrue(p(20)[3] >= 0.08 && p(20)[4] > 0, "gün 20: S4 ≥ %8 ve S5 > 0");
        assertTrue(p(40)[4] >= 0.08, "gün 40: S5 ≥ %8");
        for (int gun : new int[] {100, 200, 1000}) {
            assertTrue(p(gun)[3] + p(gun)[4] >= 0.25, "gün " + gun + ": S4+S5 ≥ %25");
        }
    }

    @Test
    void yuksekSeviyeHerZamanDahaNadir() {
        for (double gun : new double[] {1, 4, 9, 16, 20, 40, 100, 1000}) {
            double[] agirlik = SiddetSecici.agirliklar(A, gun, 5, false);
            for (int i = 1; i < 5; i++) {
                assertTrue(agirlik[i] < agirlik[i - 1] || agirlik[i] == 0,
                        "gün " + gun + ": S" + (i + 1) + " S" + i + "'den nadir olmalı");
            }
        }
        int[] sayilar = say(30, 5, 200_000, 7);
        assertTrue(sayilar[5] > 0 && sayilar[5] < sayilar[4], "S5 çıkar ama S4'ten nadirdir");
    }

    @Test
    void agirliklarZamanlaYuksekSeviyelereKayar() {
        double[] erken = p(20);
        double[] gec = p(100);
        assertTrue(gec[4] > erken[4], "gün 100'de S5 payı gün 20'den büyük olmalı");
        assertTrue(gec[0] < erken[0], "gün 100'de S1 payı küçülmeli");
        double toplam = 0;
        for (double o : gec) {
            toplam += o;
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

    /** K-E67: olasılığı 0 olan seviyenin nedeni (gamerule önce, sonra gün, gelişim, art arda). */
    @Test
    void kapaliNedeniSiraylaBelirlenir() {
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.MAKS), SiddetSecici.kapaliNedeni(A, 100, 100, 5, 4, true));
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.GUN), SiddetSecici.kapaliNedeni(A, 5, 5, 4, 5, true));
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.GELISIM), SiddetSecici.kapaliNedeni(A, 100, 5, 4, 5, false));
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.ART_ARDA), SiddetSecici.kapaliNedeni(A, 100, 100, 5, 5, true));
        assertEquals(Optional.empty(), SiddetSecici.kapaliNedeni(A, 100, 100, 5, 5, false));
        assertEquals(Optional.empty(), SiddetSecici.kapaliNedeni(A, 100, 100, 3, 5, true), "S3 art arda kuralına girmez");
    }

    /** Yoğunluk zayıflatması yüksek seviyeleri azaltır, hiçbirini sıfırlamaz; yük eşiğin altındaysa etkisiz. */
    @Test
    void yogunlukDagilimiAsagiKaydirir() {
        double[] normal = SiddetSecici.olasiliklar(A, 60, 5, false, 1.0);
        double[] esikAlti = SiddetSecici.olasiliklar(A, 60, 5, false, AfetYogunlugu.ESIK);
        double[] yogun = SiddetSecici.olasiliklar(A, 60, 5, false, 6.0);
        for (int i = 0; i < 5; i++) {
            assertEquals(normal[i], esikAlti[i], 1e-12, "eşikte etki yok");
            assertTrue(yogun[i] > 0, "zayıflatma S" + (i + 1) + "'i sıfırlamaz");
        }
        assertTrue(yogun[0] > normal[0]);
        assertTrue(yogun[3] < normal[3] && yogun[4] < normal[4]);
        assertTrue(yogun[4] / normal[4] < yogun[3] / normal[3], "en yüksek seviye en çok azalır");
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
