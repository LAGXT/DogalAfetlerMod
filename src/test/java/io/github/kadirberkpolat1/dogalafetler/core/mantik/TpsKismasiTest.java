package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TpsKismasiTest {
    private static final long MS = 1_000_000L;

    @Test
    void yalnizDoluGirdilerOrtalamayaGirer() {
        long[] tickler = new long[100];
        for (int i = 0; i < 10; i++) {
            tickler[i] = 40 * MS;
        }
        assertEquals(40.0, TpsKismasi.ortalamaMs(tickler), 1e-9, "boş (0) girdiler ortalamayı düşürmemeli");
        assertEquals(20.0, TpsKismasi.tps(40.0), 1e-9, "40 ms/tick 20 TPS'e tavanlanır");
        assertEquals(0.0, TpsKismasi.ortalamaMs(new long[100]), 1e-12);
        assertEquals(20.0, TpsKismasi.tps(0.0), 1e-12, "ölçüm yokken TPS 20 sayılır");
    }

    @Test
    void tpsTekOrtalamadanHesaplanir() {
        long[] tickler = {60 * MS, 60 * MS, 0, 0};
        double ms = TpsKismasi.ortalamaMs(tickler);
        assertEquals(60.0, ms, 1e-9);
        assertEquals(1000.0 / 60.0, TpsKismasi.tps(ms), 1e-9);
    }

    @Test
    void esiklerKismaKademeleriniBelirler() {
        assertEquals(1.0, TpsKismasi.carpan(20, 18, 15));
        assertEquals(1.0, TpsKismasi.carpan(18, 18, 15), "eşiğin kendisi kısmaz");
        assertEquals(0.5, TpsKismasi.carpan(17.99, 18, 15));
        assertEquals(0.5, TpsKismasi.carpan(15, 18, 15));
        assertEquals(0.25, TpsKismasi.carpan(14.99, 18, 15));
        assertEquals(0.25, TpsKismasi.carpan(2, 18, 15));
        assertEquals(0.5, TpsKismasi.carpan(TpsKismasi.tps(1000.0 / 16.7), 18, 15), "16,7 TPS yarı bütçe");
        assertEquals(0.25, TpsKismasi.carpan(TpsKismasi.tps(80.0), 18, 15), "80 ms/tick çeyrek bütçe");
    }
}
