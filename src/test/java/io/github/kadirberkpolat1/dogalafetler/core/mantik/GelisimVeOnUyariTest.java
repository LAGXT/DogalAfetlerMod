package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class GelisimVeOnUyariTest {
    @Test
    void gelisimPuaniXpVeZirhToplamidir() {
        assertEquals(35.0, GelisimOlcutu.puan(15, 20), 1e-12);
        assertEquals(0.0, GelisimOlcutu.puan(-3, -1), 1e-12);
    }

    @Test
    void ortalamaVeEnGelismisToplanir() {
        double[] puanlar = {10, 20, 60};
        assertEquals(30.0, GelisimOlcutu.ORTALAMA.topla(puanlar), 1e-12);
        assertEquals(60.0, GelisimOlcutu.EN_GELISMIS.topla(puanlar), 1e-12);
        assertEquals(0.0, GelisimOlcutu.ORTALAMA.topla(new double[0]), 1e-12);
        assertEquals(0.0, GelisimOlcutu.EN_GELISMIS.topla(new double[0]), 1e-12);
    }

    @Test
    void onUyariSuresiPencereIcindeKalir() {
        SplittableRandom rastgele = new SplittableRandom(8);
        long enAz = Long.MAX_VALUE;
        long enCok = Long.MIN_VALUE;
        for (int i = 0; i < 20_000; i++) {
            long sure = OnUyariPenceresi.sure(900, 1800, rastgele);
            assertTrue(sure >= 900 && sure <= 1800, "pencere dışı: " + sure);
            enAz = Math.min(enAz, sure);
            enCok = Math.max(enCok, sure);
        }
        assertTrue(enAz < 950 && enCok > 1750, "pencerenin iki ucu da kullanılmalı");
        assertEquals(1200, OnUyariPenceresi.sure(1200, 1200, rastgele));
        long ters = OnUyariPenceresi.sure(1800, 900, rastgele);
        assertTrue(ters >= 900 && ters <= 1800, "ters verilen sınırlar düzeltilir");
    }
}
