package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class MeteorOlcegiTest {
    private static final double EPS = 1e-9;

    @Test
    void histogramVarsayilanAgirliklarlaAltmisOtuzOn() {
        Random r = new Random(20261107);
        int n = 100_000;
        int[] bant = new int[3];
        int[] kova = new int[20];
        for (int i = 0; i < n; i++) {
            double o = MeteorOlcegi.ornekle(r, 0.10, 0.30);
            assertTrue(o >= MeteorOlcegi.KUCUK_ALT && o <= MeteorOlcegi.UST, "aralık dışı: " + o);
            bant[MeteorOlcegi.bant(o)]++;
            kova[(int) Math.min(19, (o - MeteorOlcegi.KUCUK_ALT) / 0.1)]++;
        }
        assertEquals(0.60, bant[0] / (double) n, 0.01);
        assertEquals(0.30, bant[1] / (double) n, 0.01);
        assertEquals(0.10, bant[2] / (double) n, 0.01);
        // Bant içinde düzgün: küçük bantın 4 kovasının her biri ~%15, büyük bantın 9 kovasının her biri ~%1,1.
        for (int k = 0; k < 4; k++) {
            assertEquals(0.15, kova[k] / (double) n, 0.01, "küçük kova " + k);
        }
        for (int k = 11; k < 20; k++) {
            assertEquals(0.10 / 9, kova[k] / (double) n, 0.004, "büyük kova " + k);
        }
    }

    @Test
    void ayniTohumAyniDiziOranlarKirpilir() {
        Random a = new Random(7);
        Random b = new Random(7);
        for (int i = 0; i < 100; i++) {
            assertEquals(MeteorOlcegi.ornekle(a, 0.1, 0.3), MeteorOlcegi.ornekle(b, 0.1, 0.3), EPS);
        }
        Random r = new Random(1);
        for (int i = 0; i < 1000; i++) {
            assertTrue(MeteorOlcegi.buyuk(MeteorOlcegi.ornekle(r, 1.0, 0.3)), "büyük oran 1: hepsi büyük");
            assertEquals(1, MeteorOlcegi.bant(MeteorOlcegi.ornekle(r, 0.0, 5.0)), "orta oran 1'e kırpılır");
            assertEquals(0, MeteorOlcegi.bant(MeteorOlcegi.ornekle(r, -1, -1)), "eksi oran: hepsi küçük");
        }
    }

    @Test
    void araTipikNoktalardaConfigDegeriUclardaUzatilirKirpilir() {
        assertEquals(2.5, MeteorOlcegi.ara(MeteorOlcegi.KUCUK_TIPIK, 2.5, 5.5), EPS);
        assertEquals(5.5, MeteorOlcegi.ara(MeteorOlcegi.BUYUK_TIPIK, 2.5, 5.5), EPS);
        assertTrue(MeteorOlcegi.ara(MeteorOlcegi.UST, 2.5, 5.5) > 5.5);
        assertTrue(MeteorOlcegi.ara(MeteorOlcegi.KUCUK_ALT, 2.5, 5.5) < 2.5);
        assertEquals(1.0, MeteorOlcegi.ara(5, 0.35, 0.8, 0.2, 1.0), EPS);
        assertTrue(MeteorOlcegi.buyuk(MeteorOlcegi.BUYUK_ALT));
        assertFalse(MeteorOlcegi.buyuk(1.59));
        assertEquals(MeteorOlcegi.KUCUK_TIPIK, MeteorOlcegi.tipik(false), EPS);
        assertEquals(0, MeteorOlcegi.bant(MeteorOlcegi.tipik(false)));
        assertEquals(2, MeteorOlcegi.bant(MeteorOlcegi.tipik(true)));
    }

    /**
     * MT2-25/28: varsayılan 180 tick'le her bantta uçuş 8–10 sn; 180/dk (3/sn) yağmurda oyuncu başına gökte ortalama
     * 24–30 meteor, 16 oyuncuda {@code meteor.varlikTavani} 448'in altında.
     */
    @Test
    void ucusSuresiSekizOnSaniyeGoktekiYukYirmiDortOtuz() {
        for (double o = MeteorOlcegi.KUCUK_ALT; o <= MeteorOlcegi.UST; o += 0.05) {
            double sn = MeteorOlcegi.ucusSuresi(180, o) / 20;
            assertTrue(sn >= 8.0 && sn <= 10.0, "ölçek " + o + ": " + sn + " sn");
        }
        assertTrue(MeteorOlcegi.ucusSuresi(180, MeteorOlcegi.UST) < MeteorOlcegi.ucusSuresi(180, MeteorOlcegi.KUCUK_ALT),
                "büyük meteor daha kısa uçar");
        Random r = new Random(20261107);
        int n = 100_000;
        double toplam = 0;
        for (int i = 0; i < n; i++) {
            toplam += MeteorOlcegi.ucusSuresi(180, MeteorOlcegi.ornekle(r, 0.10, 0.30));
        }
        double gokte = 3.0 * toplam / n / 20;
        assertTrue(gokte >= 24 && gokte <= 30, "oyuncu başına gökte " + gokte);
        assertTrue(16 * gokte < 448, "16 oyuncu: " + 16 * gokte);
    }
}
