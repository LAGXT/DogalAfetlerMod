package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class YagmurCarpmasiTest {
    private static final double YUZEYSIZ = YagmurCarpmasi.YAN;

    @Test
    void damlaBlogunUstundenCarparsaHalkaYuzeyde() {
        assertEquals(65.0, YagmurCarpmasi.karar(65.6, 64.45, 65.0, Double.NaN));
        // Basamak (yarım blok): yüzey hücrenin ortasında.
        assertEquals(64.5, YagmurCarpmasi.karar(65.2, 64.05, 64.5, Double.NaN));
    }

    @Test
    void blokDamlaninAltindaysaHenuzCarpmaz() {
        assertTrue(Double.isNaN(YagmurCarpmasi.karar(66.4, 65.25, 64.5, Double.NaN)));
        assertTrue(Double.isNaN(YagmurCarpmasi.karar(66.4, 65.25, YUZEYSIZ, Double.NaN)), "yüzeysiz hücre (gizli su, direk yanı)");
    }

    @Test
    void yandanCarpanDamlaHalkasizSoner() {
        assertEquals(YagmurCarpmasi.YAN, YagmurCarpmasi.karar(64.3, 63.15, 65.0, Double.NaN));
    }

    @Test
    void sanalSuIleBloktanYuksekOlanKazanir() {
        assertEquals(66.0, YagmurCarpmasi.karar(66.6, 65.45, 64.0, 66.0), "batık zeminin üstünde su yüzeyi");
        assertEquals(65.0, YagmurCarpmasi.karar(65.6, 64.45, 65.0, 63.0), "suyun üstündeki çatı");
        assertEquals(66.0, YagmurCarpmasi.karar(66.6, 65.45, YUZEYSIZ, 66.0), "yalnız sanal su");
    }

    @Test
    void sanalSuyunAltindakiDamlaHalkasizSoner() {
        assertEquals(YagmurCarpmasi.YAN, YagmurCarpmasi.karar(64.3, 63.15, 65.0, 67.0), "bloka yandan, suyun altında");
        assertEquals(YagmurCarpmasi.YAN, YagmurCarpmasi.karar(66.0, 64.85, YUZEYSIZ, 67.0), "yüzey damlanın üstüne yükseldi");
        assertEquals(YagmurCarpmasi.YAN, YagmurCarpmasi.karar(64.3, 63.15, 65.0, 64.0), "yüzeyden geçti ama bloka yandan");
        assertEquals(67.0, YagmurCarpmasi.karar(67.5, 66.35, 65.0, 67.0), "yüzeye üstten");
    }
}
