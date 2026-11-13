package io.github.kadirberkpolat1.dogalafetler.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SicaklikSistemiModuTest {
    @Test
    void otomatikColdSweatYukluyseKapanir() {
        assertTrue(SicaklikSistemiModu.OTOMATIK.etkinMi(false));
        assertFalse(SicaklikSistemiModu.OTOMATIK.etkinMi(true));
    }

    @Test
    void acikVeKapaliColdSweattenBagimsiz() {
        assertTrue(SicaklikSistemiModu.ACIK.etkinMi(true));
        assertTrue(SicaklikSistemiModu.ACIK.etkinMi(false));
        assertFalse(SicaklikSistemiModu.KAPALI.etkinMi(true));
        assertFalse(SicaklikSistemiModu.KAPALI.etkinMi(false));
    }

    @Test
    void gameruleKapaliysaHicCalismaz() {
        for (SicaklikSistemiModu mod : SicaklikSistemiModu.values()) {
            assertFalse(mod.calisirMi(false, false), mod.name());
            assertFalse(mod.calisirMi(false, true), mod.name());
        }
        assertTrue(SicaklikSistemiModu.OTOMATIK.calisirMi(true, false));
    }
}
