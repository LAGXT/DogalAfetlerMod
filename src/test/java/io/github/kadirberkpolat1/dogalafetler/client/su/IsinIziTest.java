package io.github.kadirberkpolat1.dogalafetler.client.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IsinIziTest {
    private static List<int[]> hucreler(IsinIzi iz) {
        List<int[]> l = new ArrayList<>();
        while (iz.ilerle()) {
            l.add(new int[] {iz.x(), iz.y(), iz.z()});
        }
        return l;
    }

    @Test
    void dikIsinHerHucreyeErimeKadarGirer() {
        IsinIzi iz = new IsinIzi();
        iz.baslat(0.5, 0.2, 0.5, 0, 1, 0, 3);
        assertTrue(iz.ilerle());
        assertEquals(1, iz.y());
        assertEquals(0.8, iz.t(), 1e-9);
        assertTrue(iz.ilerle());
        assertTrue(iz.ilerle());
        assertEquals(3, iz.y());
        assertEquals(2.8, iz.t(), 1e-9);
        assertFalse(iz.ilerle(), "4. hücrenin girişi 3,8 blokta, erimin ötesinde");
    }

    @Test
    void caprazKoseAtlamadanKomsuHucrelerdenGecer() {
        IsinIzi iz = new IsinIzi();
        iz.baslat(0.3, 13.2, 0.6, 0.7, 0.35, 0.62, 24);
        List<int[]> l = hucreler(iz);
        assertTrue(l.size() >= 24, "24 blokta en az 24 hücre: " + l.size());
        int[] onceki = {0, 13, 0};
        for (int[] h : l) {
            int fark = Math.abs(h[0] - onceki[0]) + Math.abs(h[1] - onceki[1]) + Math.abs(h[2] - onceki[2]);
            assertEquals(1, fark, "her adım tek eksende tek hücre");
            onceki = h;
        }
    }

    @Test
    void eksiYondeIlkSinirHucreninKendiKenari() {
        IsinIzi iz = new IsinIzi();
        iz.baslat(0.25, 5.5, 0.5, -1, 0, 0, 10);
        assertTrue(iz.ilerle());
        assertEquals(-1, iz.x());
        assertEquals(5, iz.y());
        assertEquals(0.25, iz.t(), 1e-9);
        assertTrue(iz.ilerle());
        assertEquals(-2, iz.x());
        assertEquals(1.25, iz.t(), 1e-9);
    }
}
