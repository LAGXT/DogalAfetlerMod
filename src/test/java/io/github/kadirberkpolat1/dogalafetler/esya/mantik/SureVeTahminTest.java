package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SureVeTahminTest {
    @Test
    void sureGercekTickSaniyeyeYukariYuvarlanir() {
        assertEquals(-1, SureBicimi.saniye(-1));
        assertEquals(0, SureBicimi.saniye(0));
        assertEquals(1, SureBicimi.saniye(1));
        assertEquals(1, SureBicimi.saniye(20));
        assertEquals(2, SureBicimi.saniye(21));
    }

    @Test
    void sureBicimi() {
        assertEquals("?", SureBicimi.bicim(-1));
        assertEquals("0:00", SureBicimi.bicim(0));
        assertEquals("0:45", SureBicimi.bicim(45));
        assertEquals("12:05", SureBicimi.bicim(725));
        assertEquals("1:02:03", SureBicimi.bicim(3723));
    }

    @Test
    void siddetTahminiHataPayiIcindeVeHassasDahaDar() {
        for (int seviye = 1; seviye <= 5; seviye++) {
            for (long tohum = 0; tohum < 500; tohum++) {
                for (double i = 0; i <= 1.0001; i += 0.25) {
                    int gercek = SiddetTahmini.gercekYuzde(seviye);
                    int normal = SiddetTahmini.yuzde(seviye, i, false, tohum);
                    int hassas = SiddetTahmini.yuzde(seviye, i, true, tohum);
                    assertTrue(normal >= 1 && normal <= 100 && hassas >= 1 && hassas <= 100);
                    assertTrue(Math.abs(normal - gercek) <= SiddetTahmini.hataPayi(false, i) + 0.5, "normal " + normal);
                    assertTrue(Math.abs(hassas - gercek) <= SiddetTahmini.hataPayi(true, i) + 0.5, "hassas " + hassas);
                }
            }
        }
        assertTrue(SiddetTahmini.hataPayi(true, 0) * 3 < SiddetTahmini.hataPayi(false, 0));
        assertTrue(SiddetTahmini.hataPayi(false, 1) < SiddetTahmini.hataPayi(false, 0) / 3 + 1e-9);
        assertEquals(20, SiddetTahmini.gercekYuzde(1));
        assertEquals(100, SiddetTahmini.gercekYuzde(5));
    }

    @Test
    void siddetTahminiDeterministikVeAfettenAfeteFarkli() {
        assertEquals(SiddetTahmini.yuzde(3, 0.2, false, 42), SiddetTahmini.yuzde(3, 0.2, false, 42));
        long farkli = java.util.stream.LongStream.range(0, 50).map(t -> SiddetTahmini.yuzde(3, 0, false, t)).distinct()
                .count();
        assertTrue(farkli > 10, "tahminler hep aynı: " + farkli);
    }

    @Test
    void merkezTahminiYaricapIcinde() {
        for (long tohum = 0; tohum < 1000; tohum++) {
            double[] t = MerkezTahmini.tahmin(100, -50, 40, tohum);
            assertTrue(Math.hypot(t[0] - 100, t[1] + 50) <= 40 + 1e-9);
        }
        assertArrayEquals(new double[] {7, 9}, MerkezTahmini.tahmin(7, 9, 0, 3), 1e-12);
        assertArrayEquals(MerkezTahmini.tahmin(0, 0, 40, 5), MerkezTahmini.tahmin(0, 0, 40, 5), 0);
    }

    @Test
    void oncuSarsintiEgrisiArtanVeSinirli() {
        double onceki = -1;
        for (double i = 0; i <= 1.0001; i += 0.05) {
            double s = OncuSarsinti.siddet(i, 3);
            assertTrue(s > onceki && s <= 1);
            onceki = s;
        }
        assertEquals(1.0, OncuSarsinti.siddet(1, 5), 1e-9);
        assertTrue(OncuSarsinti.siddet(1, 1) < OncuSarsinti.siddet(1, 5));
        assertEquals(OncuSarsinti.EN_UZUN_ARALIK, OncuSarsinti.darbeAraligi(0));
        assertEquals(OncuSarsinti.EN_KISA_ARALIK, OncuSarsinti.darbeAraligi(1));
        assertEquals(0, OncuSarsinti.zarf(0), 1e-12);
        assertEquals(1, OncuSarsinti.zarf(OncuSarsinti.DARBE_SURESI / 2), 1e-12);
        assertEquals(0, OncuSarsinti.zarf(OncuSarsinti.DARBE_SURESI), 1e-12);
        assertTrue(OncuSarsinti.sesSeviyesi(1) <= 0.3f + 1e-6 && OncuSarsinti.sesSeviyesi(0) > 0);
        assertTrue(OncuSarsinti.titremeAcisi(1, 1, 1) <= 0.6 + 1e-9);
        assertEquals(0, OncuSarsinti.titremeAcisi(1, 1, 0), 1e-12);
    }
}
