package io.github.kadirberkpolat1.dogalafetler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SirenOnceligiTest {
    /** AS-9: ay çarpması &gt; güneş patlaması &gt; diğerleri; diğerleri eşit (eşitlikte önce başlayan çalar). */
    @Test
    void ayGunesDigerleri() {
        assertTrue(SirenTuru.oncelik(AfetTuru.AY_CARPMASI) > SirenTuru.oncelik(AfetTuru.GUNES_PATLAMASI));
        assertTrue(SirenTuru.oncelik(AfetTuru.GUNES_PATLAMASI) > SirenTuru.oncelik(AfetTuru.KASIRGA));
        for (AfetTuru tur : AfetTuru.values()) {
            if (!tur.sunucuGeneli()) {
                assertEquals(SirenTuru.oncelik(AfetTuru.KASIRGA), SirenTuru.oncelik(tur), tur.kimlik());
            }
        }
    }
}
