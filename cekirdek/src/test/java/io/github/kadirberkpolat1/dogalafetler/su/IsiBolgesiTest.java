package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Isı kaynağı bölgesi (K-E80): çekirdeği tam kuru, kenara doğru yumuşakça sele karışır, dışında etkisizdir. */
class IsiBolgesiTest {
    /** (10, 10) bloğu, yarıçap 6: merkez (10,5; 10,5), çekirdek 3 blok. */
    private static final IsiBolgesi B = new IsiBolgesi(10, 64, 10, 6f);

    @Test
    void cekirdekTamKuruDisiEtkisiz() {
        assertEquals(1.0, B.oran(10.5, 10.5));
        assertEquals(1.0, B.oran(10.5 + 3.0, 10.5), 1e-9, "çekirdeğin kenarı");
        assertEquals(0.0, B.oran(10.5 + 6.0, 10.5), 1e-9, "yarıçapın kenarı");
        assertEquals(0.0, B.oran(100, 100));
    }

    @Test
    void kenaraDogruMonotonAzalirVeYonlerdeSimetriktir() {
        double onceki = 1.0;
        for (double d = 3.0; d <= 6.0; d += 0.1) {
            double o = B.oran(10.5 + d, 10.5);
            assertTrue(o <= onceki + 1e-12 && o >= 0, "d=" + d + " oran=" + o);
            assertEquals(o, B.oran(10.5, 10.5 - d), 1e-9, "x ve z eşdeğer");
            onceki = o;
        }
        assertEquals(0.5, B.oran(10.5 + 4.5, 10.5), 1e-9, "kuşağın ortası smoothstep ile yarı");
    }

    @Test
    void yaricapsizBolgeEtkisiz() {
        assertEquals(0.0, new IsiBolgesi(0, 0, 0, 0f).oran(0.5, 0.5));
    }
}
