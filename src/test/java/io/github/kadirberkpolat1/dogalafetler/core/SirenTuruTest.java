package io.github.kadirberkpolat1.dogalafetler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SirenTuruTest {
    /** K0-19 rev. K-E67: uzun siren 25 sn (eski 60 sn). */
    @Test
    void standartSirenOnSaniyeUzunSirenYirmiBesSaniye() {
        for (AfetTuru tur : AfetTuru.values()) {
            boolean uzun = tur == AfetTuru.GUNES_PATLAMASI || tur == AfetTuru.AY_CARPMASI;
            assertEquals(uzun ? 500 : 200, SirenTuru.sureTick(tur), tur.kimlik());
        }
    }

    @Test
    void yalnizGunesPatlamasininOzelSireniVar() {
        for (AfetTuru tur : AfetTuru.values()) {
            assertEquals(tur == AfetTuru.GUNES_PATLAMASI ? SirenTuru.GUNES : SirenTuru.STANDART, SirenTuru.afetIcin(tur),
                    tur.kimlik());
        }
    }
}
