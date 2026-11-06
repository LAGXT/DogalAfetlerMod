package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Madde 15: ısırma 30–60 sn, Lure seviye başına kısaltır, taban altına inmez. */
class IsirmaSuresiTest {
    private final IsirmaSuresi.Ayarlar a = new IsirmaSuresi.Ayarlar(30 * 20, 60 * 20, 5 * 20, 10 * 20);

    @Test
    void luresizAralik30ila60Sn() {
        assertEquals(600, IsirmaSuresi.toplamTick(0.0, 0, a));
        assertEquals(1200, IsirmaSuresi.toplamTick(0.999_999, 0, a));
        for (int i = 0; i < 1000; i++) {
            int t = IsirmaSuresi.toplamTick(i / 1000.0, 0, a);
            assertTrue(t >= 600 && t <= 1200, "aralık dışı " + t);
        }
    }

    @Test
    void lureHerSeviyedeKisaltir() {
        int l0 = IsirmaSuresi.toplamTick(0.5, 0, a);
        int l1 = IsirmaSuresi.toplamTick(0.5, 1, a);
        int l3 = IsirmaSuresi.toplamTick(0.5, 3, a);
        assertEquals(l0 - 100, l1);
        assertEquals(l0 - 300, l3);
    }

    @Test
    void tabanAltinaInmez() {
        assertEquals(200, IsirmaSuresi.toplamTick(0.0, 10, a));
    }

    @Test
    void vanilladanUzun() {
        // Vanilla bekleme 100–600 tick (+20–80 yaklaşma); bizimki Lure'suz en az 600.
        assertTrue(IsirmaSuresi.toplamTick(0.0, 0, a) >= 600);
    }

    @Test
    void beklemeYaklasmaPayiniDusurur() {
        assertEquals(600 - IsirmaSuresi.YAKLASMA_TICK, IsirmaSuresi.beklemeTick(600));
        assertEquals(1, IsirmaSuresi.beklemeTick(10), "en az bir tick bekler");
    }
}
