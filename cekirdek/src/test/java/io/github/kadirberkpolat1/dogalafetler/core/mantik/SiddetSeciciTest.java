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

    /** K-E72 hedef eğrisi: S2 gün 1, S3 gün 2, S4 gün 4, S5 gün 7 (oyun günü 20 dk). */
    @Test
    void gunEsikleriSeviyeleriErkenAcar() {
        assertEquals(1, A.seviye2Gunu());
        assertEquals(2, A.seviye3Gunu());
        assertEquals(4, A.seviye4Gunu());
        assertEquals(7, A.seviye5Gunu());
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

    /** Olasılık tablosu kilidi, ayar sürümü 4 (art arda seyreltmesi ve yoğunluk yokken; k 100 günde 0,9 → 1,8). */
    @Test
    void hedefOlasilikTablosu() {
        System.out.println("| Gün | S1 | S2 | S3 | S4 | S5 |");
        System.out.println("|---|---|---|---|---|---|");
        for (int gun : new int[] {0, 1, 2, 4, 9, 10, 16, 20, 30, 40, 60, 100}) {
            double[] o = p(gun);
            System.out.printf(Locale.ROOT, "| %d | %.1f | %.1f | %.1f | %.1f | %.1f |%n", gun, o[0] * 100, o[1] * 100,
                    o[2] * 100, o[3] * 100, o[4] * 100);
        }
        double[] gun2 = p(1.5);
        assertEquals(1.0, gun2[0] + gun2[1], 1e-12, "ilk günler yalnız S1–S2");
        assertTrue(gun2[1] > 0.15, "S2 ilk günlerde anlamlı pay alır: " + gun2[1]);
        assertTrue(p(20)[3] >= 0.15 && p(20)[4] >= 0.15, "gün 20: S4 ve S5 ≥ %15 (hesap %21 / %23)");
        assertTrue(p(40)[4] >= 0.25, "gün 40: S5 ≥ %25 (hesap %30)");
        for (int gun : new int[] {100, 200, 1000}) {
            assertTrue(p(gun)[3] + p(gun)[4] >= 0.6, "gün " + gun + ": S4+S5 ≥ %60");
        }
        assertTrue(p(10)[4] >= 0.15, "gün 10: S5 ≥ %15 (hesap %19,6): " + p(10)[4]);
        assertTrue(p(100)[4] > p(60)[4] && p(60)[4] > p(30)[4], "S5 payı 60. günden sonra da 5 günlük basamaklarla artar");
        assertTrue(p(100)[4] >= 0.4, "gün 100: S5 ≥ %40 (hesap %47): " + p(100)[4]);
        assertEquals(p(60)[4], p(64.9)[4], 1e-12, "basamak 5 gün boyunca sabit");
    }

    @Test
    void yuksekSeviyeErkenGunlerdeDahaNadir() {
        // k < 1 olduğu sürece (gün 15'e dek) yüksek seviye düşük seviyeden nadirdir; sonra k > 1 yüksek seviyeleri baskın yapar.
        for (double gun : new double[] {1, 4, 9}) {
            double[] agirlik = SiddetSecici.agirliklar(A, gun, 5, false);
            for (int i = 1; i < 5; i++) {
                assertTrue(agirlik[i] < agirlik[i - 1] || agirlik[i] == 0,
                        "gün " + gun + ": S" + (i + 1) + " S" + i + "'den nadir olmalı");
            }
        }
        int[] sayilar = say(9, 5, 200_000, 7);
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

    /**
     * Ayar sürümü 4: art arda 4-5 yasak değil, ağırlığı yarıya iner; gerçekleşen S5 payı (Markov ortalaması) gün 30'da en az %20,
     * gün 60'ta en az %29'dur (kullanıcı isteği; eski kesin yasakla %11 ve %15).
     */
    @Test
    void artArdaDortVeBesSeyreltilirYasaklanmaz() {
        SplittableRandom rastgele = new SplittableRandom(13);
        boolean oncekiYuksek = false;
        int yuksekSayisi = 0;
        int artArda = 0;
        for (int i = 0; i < 200_000; i++) {
            int seviye = SiddetSecici.sec(A, 300, 5, oncekiYuksek, rastgele);
            boolean yuksek = SiddetSecici.yuksek(seviye);
            if (yuksek) {
                yuksekSayisi++;
                if (oncekiYuksek) {
                    artArda++;
                }
            }
            oncekiYuksek = yuksek;
        }
        assertTrue(artArda > 0, "art arda yüksek seviye yasak olmamalı");
        assertTrue(yuksekSayisi > artArda, "yüksek seviye önceki yüksekten bağımsız da çıkmalı");
        double[] serbest = SiddetSecici.agirliklar(A, 300, 5, false);
        double[] seyrek = SiddetSecici.agirliklar(A, 300, 5, true);
        for (int i = 0; i < 5; i++) {
            double beklenen = SiddetSecici.yuksek(i + 1) ? serbest[i] * SiddetSecici.ART_ARDA_CARPANI : serbest[i];
            assertEquals(beklenen, seyrek[i], 1e-12, "S" + (i + 1) + " ağırlığı");
        }
        assertTrue(gerceklesenS5(30) >= 0.20, "gün 30 gerçekleşen S5: " + gerceklesenS5(30));
        assertTrue(gerceklesenS5(60) >= 0.29, "gün 60 gerçekleşen S5: " + gerceklesenS5(60));
    }

    /** Önceki seviyenin yüksek olup olmamasına göre iki durumlu zincirin durgun dağılımındaki S5 payı. */
    private static double gerceklesenS5(double gun) {
        double[] serbest = SiddetSecici.olasiliklar(A, gun, 5, false);
        double[] seyrek = SiddetSecici.olasiliklar(A, gun, 5, true);
        double yuksekSonrasiSerbest = serbest[3] + serbest[4];
        double yuksekSonrasiSeyrek = seyrek[3] + seyrek[4];
        double yuksekDurumu = yuksekSonrasiSerbest / (1 - yuksekSonrasiSeyrek + yuksekSonrasiSerbest);
        return (1 - yuksekDurumu) * serbest[4] + yuksekDurumu * seyrek[4];
    }

    /** K-E67: olasılığı 0 olan seviyenin nedeni (gamerule önce, sonra gün, gelişim). */
    @Test
    void kapaliNedeniSiraylaBelirlenir() {
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.MAKS), SiddetSecici.kapaliNedeni(A, 100, 100, 5, 4));
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.GUN), SiddetSecici.kapaliNedeni(A, 3, 3, 4, 5));
        assertEquals(Optional.of(SiddetSecici.KapaliNeden.GELISIM), SiddetSecici.kapaliNedeni(A, 100, 3, 4, 5));
        assertEquals(Optional.empty(), SiddetSecici.kapaliNedeni(A, 100, 100, 5, 5));
        assertEquals(Optional.empty(), SiddetSecici.kapaliNedeni(A, 100, 100, 3, 5));
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
