package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OdulIstegiTest {
    @Test
    void turlerAdetiDogruYereKoyar() {
        assertEquals(new OdulIstegi(3, 0), OdulIstegi.coz("parca", 3));
        assertEquals(new OdulIstegi(0, 2), OdulIstegi.coz("cekirdek", 2));
        assertEquals(new OdulIstegi(5, 5), OdulIstegi.coz("hepsi", 5));
        assertEquals(new OdulIstegi(1, 1), OdulIstegi.VARSAYILAN);
    }

    @Test
    void turBuyukKucukHarfDuyarsiz() {
        assertEquals(new OdulIstegi(0, 1), OdulIstegi.coz("CEKIRDEK", 1));
        assertTrue(OdulIstegi.gecerliTur("Parca"));
        assertFalse(OdulIstegi.gecerliTur("yildiz"));
        assertFalse(OdulIstegi.gecerliTur(null));
    }

    @Test
    void gecersizTurVeAdetReddedilir() {
        assertThrows(IllegalArgumentException.class, () -> OdulIstegi.coz("yildiz", 1));
        assertThrows(IllegalArgumentException.class, () -> OdulIstegi.coz(null, 1));
        assertThrows(IllegalArgumentException.class, () -> OdulIstegi.coz("parca", 0));
        assertThrows(IllegalArgumentException.class, () -> OdulIstegi.coz("parca", OdulIstegi.EN_COK_ADET + 1));
        assertEquals(new OdulIstegi(OdulIstegi.EN_COK_ADET, 0), OdulIstegi.coz("parca", OdulIstegi.EN_COK_ADET));
    }
}
