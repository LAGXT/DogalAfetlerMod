package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OyunGunuTest {
    @Test
    void gunOyunZamanindanVeOfsettenHesaplanir() {
        assertEquals(0, OyunGunu.gun(0, 0));
        assertEquals(0, OyunGunu.gun(23_999, 0));
        assertEquals(1, OyunGunu.gun(24_000, 0));
        assertEquals(42, OyunGunu.gun(48_000, 40));
        assertEquals(0, OyunGunu.gun(24_000, -5), "gün negatif olmaz");
    }

    @Test
    void ofsetIstenenGunuVerirVeGunYineIlerler() {
        long zaman = 3 * 24_000L + 500;
        long ofset = OyunGunu.ofset(42, zaman);
        assertEquals(42, OyunGunu.gun(zaman, ofset));
        assertEquals(43, OyunGunu.gun(zaman + 24_000, ofset));
        assertEquals(0, OyunGunu.gun(zaman, OyunGunu.ofset(0, zaman)));
    }
}
