package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OdulZamaniVeSarsintiTest {
    @Test
    void sicakYirmiSaniyeOmurOnDakika() {
        assertEquals(20, OdulZamani.SICAK_SANIYE);
        assertEquals(600, OdulZamani.OMUR_SANIYE);
        assertEquals(12_000, OdulZamani.omurTick(OdulZamani.OMUR_SANIYE), "10 dk = 12000 tick");
        long bitis = OdulZamani.sicakBitis(1000, OdulZamani.SICAK_SANIYE);
        assertEquals(1400, bitis);
        assertTrue(OdulZamani.sicakMi(1000, bitis));
        assertTrue(OdulZamani.sicakMi(1399, bitis));
        assertFalse(OdulZamani.sicakMi(1400, bitis), "20 sn sonunda soğur");
        assertFalse(OdulZamani.sicakMi(5000, OdulZamani.SOGUK), "etiketsiz parça soğuk");
    }

    @Test
    void sarsintiMesafeyleAzalirMenzildeBiter() {
        assertEquals(0.8, Sarsinti.siddet(0, 96, 0.8), 1e-12);
        assertEquals(0.0, Sarsinti.siddet(96, 96, 0.8), 1e-12);
        assertEquals(0.0, Sarsinti.siddet(200, 96, 0.8), 1e-12);
        assertEquals(0.2, Sarsinti.siddet(48, 96, 0.8), 1e-12, "yarı menzilde dörtte bir");
        double onceki = 1;
        for (double d = 0; d < 96; d += 4) {
            double s = Sarsinti.siddet(d, 96, 0.8);
            assertTrue(s < onceki && s > 0);
            onceki = s;
        }
        assertTrue(Sarsinti.sureTick(0.8) > Sarsinti.sureTick(0.1), "güçlü sarsıntı uzun sürer");
        assertEquals(Sarsinti.EN_KISA_SURE, Sarsinti.sureTick(0));
    }
}
