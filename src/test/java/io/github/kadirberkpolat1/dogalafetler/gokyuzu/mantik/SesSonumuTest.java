package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pulsar ses sönümü (pulsar-2 tur 4 D): derinlik, k, kuru/yankılı çapraz geçiş ve ses yüksekliği tavanı. */
class SesSonumuTest {
    private static final float TOLERANS = 1e-3f;

    @Test
    void derinlikSozlesmeDegerleri() {
        assertEquals(1f, SesSonumu.derinlik(64), TOLERANS);
        assertEquals(0.766f, SesSonumu.derinlik(48), TOLERANS);
        assertEquals(0.266f, SesSonumu.derinlik(16), TOLERANS);
        assertEquals(0f, SesSonumu.derinlik(-20), TOLERANS);
    }

    @Test
    void derinlikUclari() {
        assertEquals(1f, SesSonumu.derinlik(63), 0f, "deniz seviyesi tam");
        assertEquals(1f, SesSonumu.derinlik(320), 0f, "yükseklik 1'i aşmaz");
        assertEquals(0.75f, SesSonumu.derinlik(47), 1e-6f, "bir katman aşağıda %25 eksik");
        assertEquals(0f, SesSonumu.derinlik(-1), 1e-6f, "y = −1'de 0");
        assertEquals(0f, SesSonumu.derinlik(-64), 0f, "dünya tabanı 0");
        assertEquals(0f, SesSonumu.derinlik(Double.NaN), 0f);
        float onceki = 1f;
        for (int y = 63; y >= -1; y--) {
            float d = SesSonumu.derinlik(y);
            assertTrue(d <= onceki, "aşağı indikçe azalır: y=" + y);
            onceki = d;
        }
    }

    @Test
    void kGokOraniVeDerinlikCarpimi() {
        assertEquals(1f, SesSonumu.k(1f, 64), TOLERANS);
        assertEquals(0.5f * 0.766f, SesSonumu.k(0.5f, 48), TOLERANS);
        assertEquals(0f, SesSonumu.k(0f, 100), 0f, "üstü tamamen kapalı");
        assertEquals(0f, SesSonumu.k(1f, -20), 0f, "derinde");
        assertEquals(1f, SesSonumu.k(3f, 70), 0f, "oran kenetlenir");
        assertEquals(0f, SesSonumu.k(-1f, 70), 0f);
        assertEquals(0f, SesSonumu.k(Float.NaN, 70), 0f);
    }

    @Test
    void kuruVeYankiCaprazGecisi() {
        assertEquals(1f, SesSonumu.kuruKazanc(1f), 0f);
        assertEquals(0f, SesSonumu.yankiKazanc(1f), 0f, "k = 1 yalnız kuru");
        assertEquals(0f, SesSonumu.kuruKazanc(0f), 0f);
        assertEquals(0.35f, SesSonumu.yankiKazanc(0f), 1e-6f, "k = 0 yalnız boğuk-yankılı %35");
        assertEquals(0.766f, SesSonumu.kuruKazanc(SesSonumu.k(1f, 48)), TOLERANS);
        assertEquals(0.35f * (1f - 0.766f), SesSonumu.yankiKazanc(SesSonumu.k(1f, 48)), TOLERANS);
        assertEquals(1f, SesSonumu.kuruKazanc(2f), 0f, "kenetlenir");
        assertEquals(0f, SesSonumu.yankiKazanc(2f), 0f);
        assertEquals(0f, SesSonumu.kuruKazanc(Float.NaN), 0f);
    }

    @Test
    void karisimDosyaDuzeyiniAsmaz() {
        for (int i = 0; i <= 1000; i++) {
            float k = i / 1000f;
            float kuru = SesSonumu.kuruKazanc(k);
            float yanki = SesSonumu.yankiKazanc(k);
            assertTrue(kuru * kuru + yanki * yanki <= 1f + 1e-6f, "güç toplamı ≤ 1: k=" + k);
            assertTrue(kuru <= 1f && yanki <= SesSonumu.YANKI_TAVANI, "kazanç tavanı: k=" + k);
        }
    }
}
