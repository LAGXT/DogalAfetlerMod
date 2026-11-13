package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** K-E86: uzaktaki kasırga siluete dönüşmez, alfası düşer ama kaybolmaz. */
class UzaklikSolmasiTest {
    @Test
    void sisBitisineYakinTamOpakKalir() {
        assertEquals(1f, UzaklikSolmasi.carpan(0f, 80f));
        assertEquals(1f, UzaklikSolmasi.carpan(80f * UzaklikSolmasi.BASLANGIC_CARPANI, 80f));
    }

    @Test
    void uzaklastikcaMonotonAzalirVeTabandaDurur() {
        float onceki = 1f;
        for (float d = 72f; d <= 400f; d += 8f) {
            float c = UzaklikSolmasi.carpan(d, 80f);
            assertTrue(c <= onceki + 1e-6f, "azalmalı: " + d);
            assertTrue(c >= UzaklikSolmasi.EN_AZ_ORAN - 1e-6f, "tabanın altına inmemeli: " + d);
            onceki = c;
        }
        assertEquals(UzaklikSolmasi.EN_AZ_ORAN, UzaklikSolmasi.carpan(80f * UzaklikSolmasi.BITIS_CARPANI, 80f), 1e-6f);
        assertEquals(UzaklikSolmasi.EN_AZ_ORAN, UzaklikSolmasi.carpan(10_000f, 80f), 1e-6f);
    }

    @Test
    void sisYoksaSolmaDaYoktur() {
        assertEquals(1f, UzaklikSolmasi.carpan(5_000f, 0f));
        assertEquals(Float.POSITIVE_INFINITY, UzaklikSolmasi.baslangic(1f));
    }
}
