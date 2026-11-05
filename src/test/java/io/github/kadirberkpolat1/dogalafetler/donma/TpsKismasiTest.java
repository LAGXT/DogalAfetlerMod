package io.github.kadirberkpolat1.dogalafetler.donma;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TpsKismasiTest {
    private static long[] sabit(long milisaniye) {
        long[] t = new long[100];
        Arrays.fill(t, milisaniye * 1_000_000L);
        return t;
    }

    @Test
    void tpsTickSuresindenHesaplanir() {
        assertEquals(20.0, TpsKismasi.tps(sabit(50)), 1e-9);
        assertEquals(10.0, TpsKismasi.tps(sabit(100)), 1e-9);
        assertEquals(20.0, TpsKismasi.tps(sabit(25)), 1e-9, "20'de kesilir");
        assertEquals(20.0, TpsKismasi.tps(new long[100]), 1e-9, "ölçüm yokken 20");
    }

    @Test
    void butceTpsEsiklerindeKisilir() {
        assertEquals(256, TpsKismasi.butce(256, 20.0, 18.0, 15.0));
        assertEquals(256, TpsKismasi.butce(256, 18.0, 18.0, 15.0));
        assertEquals(128, TpsKismasi.butce(256, 17.9, 18.0, 15.0));
        assertEquals(128, TpsKismasi.butce(256, 15.0, 18.0, 15.0));
        assertEquals(64, TpsKismasi.butce(256, 14.9, 18.0, 15.0));
        assertEquals(1, TpsKismasi.butce(2, 10.0, 18.0, 15.0), "bütçe sıfıra inmez");
    }

    @Test
    void kismaKademesi() {
        assertEquals(1, TpsKismasi.bolen(19.0, 18.0, 15.0));
        assertEquals(2, TpsKismasi.bolen(16.0, 18.0, 15.0));
        assertEquals(4, TpsKismasi.bolen(12.0, 18.0, 15.0));
    }

    @Test
    void bosTickGirdileriOrtalamayaGirmez() {
        long[] t = new long[100];
        Arrays.fill(t, 50, 100, 100_000_000L);
        assertEquals(100.0, TpsKismasi.ortalamaMs(t), 1e-9);
        assertEquals(10.0, TpsKismasi.tps(t), 1e-9, "yazılmamış 0 girdileri TPS'i yüksek göstermemeli");
        assertEquals(0.0, TpsKismasi.ortalamaMs(new long[100]), 0.0);
    }
}
