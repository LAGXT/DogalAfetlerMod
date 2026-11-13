package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class YonMesafeTest {
    private static final double E = 1e-9;

    @Test
    void pusulaAcisiKuzeydenSaatYonunde() {
        assertEquals(0, YonMesafe.pusulaAcisi(0, 0, 0, -10), E);
        assertEquals(90, YonMesafe.pusulaAcisi(0, 0, 10, 0), E);
        assertEquals(180, YonMesafe.pusulaAcisi(0, 0, 0, 10), E);
        assertEquals(270, YonMesafe.pusulaAcisi(0, 0, -10, 0), E);
        assertEquals(45, YonMesafe.pusulaAcisi(5, 5, 15, -5), E);
    }

    @Test
    void mesafeYataydir() {
        assertEquals(5, YonMesafe.mesafe(1, 1, 4, 5), E);
    }

    @Test
    void goreliAciOyuncununBakisinaGore() {
        // yaw 180 kuzeye bakıyor: kuzeydeki hedef tam önde, doğudaki sağda.
        assertEquals(0, YonMesafe.goreliAci(0, 180f), E);
        assertEquals(90, YonMesafe.goreliAci(90, 180f), E);
        // yaw 0 güneye bakıyor: kuzeydeki hedef arkada (180), doğudaki solda.
        assertEquals(180, YonMesafe.goreliAci(0, 0f), E);
        assertEquals(-90, YonMesafe.goreliAci(90, 0f), E);
        // yaw -90 doğuya bakıyor; Minecraft yaw'ı sınırsız birikebilir.
        assertEquals(0, YonMesafe.goreliAci(90, -90f), E);
        assertEquals(0, YonMesafe.goreliAci(90, 270f + 720f), E);
    }

    @Test
    void normalizeAraliktaTutar() {
        assertEquals(180, YonMesafe.normalize(-180), E);
        assertEquals(-170, YonMesafe.normalize(190), E);
        assertEquals(10, YonMesafe.normalize(730), E);
    }

    @Test
    void sekizYon() {
        assertEquals(0, YonMesafe.sekizYon(0));
        assertEquals(0, YonMesafe.sekizYon(350));
        assertEquals(1, YonMesafe.sekizYon(40));
        assertEquals(2, YonMesafe.sekizYon(90));
        assertEquals(4, YonMesafe.sekizYon(181));
        assertEquals(7, YonMesafe.sekizYon(320));
    }
}
