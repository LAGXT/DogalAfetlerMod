package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MeteorSesMenziliTest {
    private static final double EPS = 1e-9;

    @Test
    void ucusYalnizMenzildeDuyulurKazancMesafeyleKareselAzalir() {
        assertEquals(1.0, MeteorSesMenzili.ucusKazanci(0), EPS);
        assertEquals(0.25, MeteorSesMenzili.ucusKazanci(24), EPS);
        assertEquals(0.0, MeteorSesMenzili.ucusKazanci(48), EPS);
        assertEquals(0.0, MeteorSesMenzili.ucusKazanci(200), EPS);
        double onceki = 2;
        for (double d = 0; d <= 48; d += 1) {
            double k = MeteorSesMenzili.ucusKazanci(d);
            assertTrue(k <= onceki, "kazanç mesafeyle artmamalı: " + d);
            onceki = k;
        }
        assertTrue(MeteorSesMenzili.ucusDuyulur(47.9, false));
        assertFalse(MeteorSesMenzili.ucusDuyulur(48.5, false), "menzil dışında başlamaz");
        assertTrue(MeteorSesMenzili.ucusDuyulur(50, true), "çalan ses bırakma payında sürer (aç-kapa yok)");
        assertFalse(MeteorSesMenzili.ucusDuyulur(52.5, true));
    }

    @Test
    void dopplerYaklasirkenTizUzaklasirkenPesSinirda() {
        assertEquals(1.0, MeteorSesMenzili.dopplerPerdesi(0), EPS);
        assertEquals(12.0 / 9.0, MeteorSesMenzili.dopplerPerdesi(3), EPS);
        assertEquals(0.8, MeteorSesMenzili.dopplerPerdesi(-3), EPS);
        assertEquals(2.0, MeteorSesMenzili.dopplerPerdesi(100), EPS);
        assertEquals(12.0 / 22.8, MeteorSesMenzili.dopplerPerdesi(-100), EPS, "hız ±0,9 c'de kırpılır");
    }

    @Test
    void carpmaOlcekleBuyurTipikKucuk96TipikBuyuk128Blok() {
        double k = MeteorOlcegi.KUCUK_TIPIK;
        double b = MeteorOlcegi.BUYUK_TIPIK;
        assertEquals(1.0, MeteorSesMenzili.carpmaKazanci(0, b), EPS);
        assertEquals(MeteorSesMenzili.KUCUK_CARPMA_DUZEYI, MeteorSesMenzili.carpmaKazanci(0, k), EPS);
        assertEquals(96.0, MeteorSesMenzili.carpmaMenzili(k), EPS);
        assertEquals(128.0, MeteorSesMenzili.carpmaMenzili(b), EPS);
        assertEquals(0.0, MeteorSesMenzili.carpmaKazanci(96, k), EPS);
        assertTrue(MeteorSesMenzili.carpmaKazanci(96, b) > 0);
        assertEquals(0.0, MeteorSesMenzili.carpmaKazanci(128, b), EPS);
        double orta = MeteorSesMenzili.carpmaMenzili(1.25);
        assertTrue(orta > 96 && orta < 128, "orta ölçek aradadır: " + orta);
        assertTrue(MeteorSesMenzili.carpmaMenzili(MeteorOlcegi.UST) > 128 && MeteorSesMenzili.carpmaMenzili(0.5) < 96);
        for (double d = 0; d < 90; d += 8) {
            double onceki = -1;
            for (double o = MeteorOlcegi.KUCUK_ALT; o <= MeteorOlcegi.UST; o += 0.1) {
                double g = MeteorSesMenzili.carpmaKazanci(d, o);
                assertTrue(g >= onceki, "ölçek büyüdükçe kazanç azalmaz: mesafe " + d + " ölçek " + o);
                onceki = g;
            }
        }
        assertEquals(0.85 * 1.1, MeteorSesMenzili.carpmaPerdesi(b, true), EPS);
        assertEquals(1.0, MeteorSesMenzili.carpmaPerdesi(k, false), EPS);
    }

    @Test
    void tavanDoluykenEnUzakDuserDahaUzakYeniCalmaz() {
        assertEquals(-1, MeteorSesMenzili.yer(new double[] {10, 20}, 90, 6));
        double[] dolu = {10, 80, 30, 40, 50, 60};
        assertEquals(1, MeteorSesMenzili.yer(dolu, 20, 6));
        assertEquals(-2, MeteorSesMenzili.yer(dolu, 85, 6));
    }

    @Test
    void ucusSecimiEnYakinAltiDuyulandan() {
        double[] m = {5, 100, 20, 49, 30, 10, 40, 45, 1};
        boolean[] caliyor = new boolean[m.length];
        caliyor[3] = true;
        boolean[] secim = MeteorSesMenzili.ucusSecimi(m, caliyor, 6);
        assertArrayEquals(new boolean[] {true, false, true, false, true, true, true, false, true}, secim);
        boolean[] az = MeteorSesMenzili.ucusSecimi(new double[] {49, 60, 3}, new boolean[] {true, false, false}, 6);
        assertArrayEquals(new boolean[] {true, false, true}, az, "çalan 49 blokta sürer, 60 blok çalmaz");
    }
}
