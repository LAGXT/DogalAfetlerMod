package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Schwarzschild ince-merceği: lens denklemi, Einstein halkası, Doppler uçları, diskin arka yarısının iki yayı. */
class KaraDelikMercekTest {

    @Test
    void ikiGoruntuLensDenkleminiSaglar() {
        float[] thetaE = {0.5f, 2.45f, 3.46f, 14.5f};
        float[] beta = {0f, 0.01f, 0.3f, 1f, 2.6f, 7f, 20f, 80f};
        for (float e : thetaE) {
            for (float b : beta) {
                float arti = KaraDelikMercek.goruntuArti(b, e);
                float eksi = -KaraDelikMercek.goruntuEksi(b, e);
                assertEquals(b, arti - e * e / arti, 2e-4f * Math.max(1f, e), "θ+ β=" + b + " θE=" + e);
                if (b <= 8f * e) {
                    // u > 8'de θ− ≈ θE²/β iki yakın sayının farkıdır; float çözünürlüğü denklemi sınamaya yetmez.
                    assertEquals(b, eksi - e * e / eksi, 2e-3f * Math.max(1f, e), "θ− β=" + b + " θE=" + e);
                    assertEquals(e * e, arti * -eksi, 1e-3f * e * e, "θ+·|θ−| = θE²");
                }
                assertTrue(arti >= e - 1e-5f, "ana görüntü halkanın dışında");
                assertTrue(-eksi <= e + 1e-5f, "ikinci görüntü halkanın içinde");
            }
        }
    }

    @Test
    void tabloKesinFormulleAyni() {
        for (float u = 0f; u < 20f; u += 0.0137f) {
            double kesin = (u + Math.sqrt(u * u + 4.0)) / 2.0;
            assertEquals(kesin, KaraDelikMercek.artiOrani(u), 3e-6 * Math.max(1.0, kesin), "u=" + u);
        }
    }

    @Test
    void betaSifirdaEinsteinHalkasi() {
        float e = KaraDelikMercek.einsteinYaricapi(6f);
        assertEquals((float) Math.sqrt(12.0), e, 1e-5f);
        assertEquals(e, KaraDelikMercek.goruntuArti(0f, e), 1e-5f);
        assertEquals(e, KaraDelikMercek.goruntuEksi(0f, e), 1e-5f);
        assertEquals(e, KaraDelikMercek.goruntuArti(1e-4f, e), 1e-4f);
        assertTrue(KaraDelikMercek.buyutme(1e-3f, true) > 100f, "halkada büyütme çok büyük");
        assertEquals(0f, KaraDelikMercek.einsteinYaricapi(-3f), 0f, "öndeki kaynak merceklenmez");
        assertEquals(5f, KaraDelikMercek.goruntuArti(5f, 0f), 0f);
    }

    @Test
    void buyutmeFarkiBirVeUzaktaBireIner() {
        for (float u = 0.05f; u < 10f; u += 0.25f) {
            assertEquals(1f, KaraDelikMercek.buyutme(u, true) - KaraDelikMercek.buyutme(u, false), 1e-5f);
        }
        assertEquals(1f, KaraDelikMercek.buyutme(50f, true), 1e-3f);
        assertEquals(0f, KaraDelikMercek.buyutme(50f, false), 1e-3f);
        float theta = KaraDelikMercek.goruntuArti(0.2f, 3f);
        assertTrue(KaraDelikMercek.tegetselUzama(theta, 0.2f) > 10f, "halkaya yakın yıldız teğet uzar");
        assertTrue(KaraDelikMercek.radyalUzama(theta, 3f) < 0.6f, "radyal sıkışır");
    }

    @Test
    void dopplerUclari() {
        assertEquals(1f, KaraDelikMercek.doppler(1f), 1e-6f);
        assertEquals(0.3f, KaraDelikMercek.doppler(-1f), 1e-6f);
        assertEquals(0.65f, KaraDelikMercek.doppler(0f), 1e-6f);
        assertEquals(1f, KaraDelikMercek.doppler(3f), 1e-6f, "kenetlenir");
        float onceki = 0f;
        for (float c = -1f; c <= 1f; c += 0.05f) {
            float d = KaraDelikMercek.doppler(c);
            assertTrue(d >= onceki, "yaklaştıkça parlar");
            onceki = d;
        }
        assertEquals(1f, KaraDelikMercek.yaklasmaCos(180f, 0f), 1e-6f, "sol yan yaklaşır");
        assertEquals(-1f, KaraDelikMercek.yaklasmaCos(0f, 0f), 1e-6f, "sağ yan uzaklaşır");
        assertEquals(0f, KaraDelikMercek.yaklasmaCos(90f, 0f), 1e-6f);
    }

    @Test
    void boyutlarSirali() {
        assertEquals(Math.sqrt(27.0) / 2.0, KaraDelikMercek.GOLGE_RS, 0.01);
        assertTrue(KaraDelikMercek.FOTON_RS > KaraDelikMercek.GOLGE_RS && KaraDelikMercek.FOTON_RS <= 2.7f);
        assertTrue(KaraDelikMercek.DISK_IC_RS > KaraDelikMercek.FOTON_RS);
        assertTrue(KaraDelikMercek.DISK_DIS_RS * KaraDelikMercek.YILDIZ_EINSTEIN
                < KaraDelikMercek.DISK_DIS_RS * KaraDelikMercek.YILDIZ_ALANI);
    }

    @Test
    void arkaYariGolgeninUstundeVeAltindaIkiYay() {
        float[] c = new float[4];
        for (float egim : new float[] {3f, 5f, 11f}) {
            for (float r = 4f; r <= KaraDelikMercek.DISK_DIS_RS; r += 1f) {
                for (float fi = 60f; fi <= 120f; fi += 10f) {
                    assertTrue(KaraDelikMercek.diskGoruntusu(r, fi, egim, false, c));
                    assertTrue(c[1] > 0f && Math.hypot(c[0], c[1]) > KaraDelikMercek.GOLGE_RS,
                            "ana yay gölgenin dışında, üstte: r=" + r + " φ=" + fi + " i=" + egim + " y=" + c[1]);
                    if (fi == 90f) {
                        assertTrue(c[1] > KaraDelikMercek.GOLGE_RS, "tam arkası gölgenin üstünde: r=" + r);
                    }
                    assertTrue(c[2] > 0f);
                    float ust = c[3];
                    assertTrue(KaraDelikMercek.diskGoruntusu(r, fi, egim, true, c));
                    assertTrue(c[1] < 0f, "ikinci yay alt yanda: r=" + r + " φ=" + fi + " i=" + egim);
                    assertTrue(c[3] < ust, "alt yay sönük");
                    if (r >= 6f && fi >= 80f && fi <= 100f) {
                        assertTrue(Math.hypot(c[0], c[1]) > KaraDelikMercek.GOLGE_RS, "ikinci yay gölgenin dışında: r="
                                + r + " φ=" + fi + " i=" + egim);
                    }
                    if (r >= 6f && fi == 90f) {
                        assertTrue(c[1] < -KaraDelikMercek.GOLGE_RS, "ikinci yay gölgenin altında: r=" + r);
                    }
                }
            }
        }
    }

    @Test
    void onYariMerceklenmezVeGolgeninOnundenGecer() {
        float[] c = new float[4];
        assertTrue(KaraDelikMercek.diskGoruntusu(6f, 270f, 5f, false, c));
        assertEquals(0f, c[0], 1e-5f);
        assertTrue(c[1] < 0f && c[1] > -KaraDelikMercek.GOLGE_RS, "ön şerit gölgeyi merkezin hemen altında keser");
        assertTrue(c[2] < 0f);
        assertEquals(1f, c[3], 0f);
        assertFalse(KaraDelikMercek.diskGoruntusu(6f, 270f, 5f, true, c), "ön yarının ikinci görüntüsü yok");
        assertTrue(KaraDelikMercek.diskGoruntusu(8f, 0f, 5f, false, c));
        assertEquals(8f, c[0], 1e-5f, "yan kenar yerinde (yaylar diskle birleşir)");
        assertEquals(0f, c[1], 1e-5f);
    }
}
