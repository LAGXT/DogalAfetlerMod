package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** K-E67 "çok gelirse güçsüz": hedef aralık eğrisi, yük ve seviye çarpanı. */
class AfetYogunluguTest {
    @Test
    void hedefAralikSekizBesUcDakika() {
        assertEquals(9_600, AfetYogunlugu.hedefAralik(0));
        assertEquals(9_600, AfetYogunlugu.hedefAralik(1));
        assertEquals(6_000, AfetYogunlugu.hedefAralik(10));
        assertEquals(3_600, AfetYogunlugu.hedefAralik(30));
        assertEquals(3_600, AfetYogunlugu.hedefAralik(500));
        assertEquals(4_800, AfetYogunlugu.hedefAralik(20));
        for (double gun = 0; gun < 40; gun += 0.25) {
            assertTrue(AfetYogunlugu.hedefAralik(gun + 0.25) <= AfetYogunlugu.hedefAralik(gun), "aralık günle kısalır");
        }
        assertEquals(2.5, AfetYogunlugu.normalGunluk(1), 1e-9);
        assertEquals(24_000.0 / 3_600, AfetYogunlugu.normalGunluk(60), 1e-9);
    }

    @Test
    void yukSonGunSayisiVeSiklikinBuyugu() {
        assertEquals(1.0, AfetYogunlugu.yuk(0, 10, 1.0), 1e-12);
        assertEquals(2.0, AfetYogunlugu.yuk(8, 10, 1.0), 1e-12, "gün 10 normali 4/gün");
        assertEquals(3.0, AfetYogunlugu.yuk(0, 10, 3.0), 1e-12, "afetSikligi %300");
        assertEquals(0.5, AfetYogunlugu.yuk(2, 10, 0.25), 1e-12);
    }

    @Test
    void zayiflatmaEsikUstundeBaslarSinirli() {
        assertEquals(1.0, AfetYogunlugu.zayiflatma(1.0), 1e-12);
        assertEquals(1.0, AfetYogunlugu.zayiflatma(AfetYogunlugu.ESIK), 1e-12);
        assertEquals(2.0, AfetYogunlugu.zayiflatma(AfetYogunlugu.ESIK * 2), 1e-12);
        assertEquals(AfetYogunlugu.EN_COK_ZAYIFLATMA, AfetYogunlugu.zayiflatma(1_000), 1e-12);
        assertEquals(1.0, AfetYogunlugu.seviyeCarpani(1, 100), 1e-12, "S1 hiç zayıflamaz");
        assertEquals(0.25, AfetYogunlugu.seviyeCarpani(5, AfetYogunlugu.ESIK * 2), 1e-12, "z=2: S5 × 2^-2");
        assertTrue(AfetYogunlugu.seviyeCarpani(5, 1_000) > 0, "sıfırlamaz");
    }
}
