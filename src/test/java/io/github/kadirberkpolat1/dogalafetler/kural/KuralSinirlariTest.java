package io.github.kadirberkpolat1.dogalafetler.kural;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class KuralSinirlariTest {
    @Test
    void maksSiddetBirIleBesArasindaKalir() {
        assertEquals(1, KuralSinirlari.maksSiddet(-3));
        assertEquals(1, KuralSinirlari.maksSiddet(0));
        assertEquals(3, KuralSinirlari.maksSiddet(3));
        assertEquals(5, KuralSinirlari.maksSiddet(5));
        assertEquals(5, KuralSinirlari.maksSiddet(99));
    }

    @Test
    void siklikYuzdesiSifirIleBinArasindaKalir() {
        assertEquals(0, KuralSinirlari.siklikYuzdesi(-50));
        assertEquals(100, KuralSinirlari.siklikYuzdesi(100));
        assertEquals(1000, KuralSinirlari.siklikYuzdesi(5000));
    }

    @Test
    void siklikCarpaniYuzdeyiOranaCevirir() {
        assertEquals(1.0, KuralSinirlari.siklikCarpani(100));
        assertEquals(0.5, KuralSinirlari.siklikCarpani(50));
        assertEquals(2.0, KuralSinirlari.siklikCarpani(200));
        assertEquals(0.0, KuralSinirlari.siklikCarpani(-1));
    }
}
