package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void ustYuzuCizilmeyenGercekSuyaDamlaHalkasizSonerYaDaSanalSuyaIner() {
        double gizli = YagmurCarpmasi.GIZLI;
        assertEquals(YagmurCarpmasi.YAN, YagmurCarpmasi.karar(60.3, 59.15, gizli, Double.NaN), "çekilmenin gizlediği katmanın altı, sanal su yok");
        assertEquals(YagmurCarpmasi.YAN, YagmurCarpmasi.karar(60.3, 59.15, gizli, 58.0), "sanal yüzey daha aşağıda: görünmez suda asılı kalmasın");
        assertEquals(60.6, YagmurCarpmasi.karar(61.2, 60.05, gizli, 60.6), "alçalmış sanal yüzeyi bu tick geçti");
        assertEquals(YagmurCarpmasi.YAN, YagmurCarpmasi.karar(60.3, 59.15, gizli, 60.6), "sanal yüzeyin zaten altındaydı");
    }

    @Test
    void gozHizasininUstundekiYuzeydeHalkaGorunmez() {
        assertTrue(YagmurCarpmasi.ustYuzGorunur(64.0, 64.62), "gözün altındaki kum");
        assertFalse(YagmurCarpmasi.ustYuzGorunur(65.0, 64.62), "gözün 0,38 üstündeki kum tepesi (tur 4 karesi)");
        assertFalse(YagmurCarpmasi.ustYuzGorunur(64.62, 64.62), "göz hizası: yüz kenardan, görünmez");
    }

    @Test
    void sanalHalkaUcuCizilenSuyunUstundeOlmali() {
        assertTrue(YagmurCarpmasi.sanalUcDestekli(66.0, 66.0, 0.125), "açık deniz: uç aynı yüzeyde");
        assertTrue(YagmurCarpmasi.sanalUcDestekli(65.9, 66.0, 0.125), "pay içinde alçak su");
        assertTrue(YagmurCarpmasi.sanalUcDestekli(68.5, 66.0, 0.125), "yükselen dalga yüzü ucu örter");
        assertFalse(YagmurCarpmasi.sanalUcDestekli(64.0, 66.0, 0.125), "dalga cephesinden havaya taşar");
        assertFalse(YagmurCarpmasi.sanalUcDestekli(Double.NaN, 66.0, 0.125), "uçta çizilen su yok (kıyı, gerçek su)");
    }
}
