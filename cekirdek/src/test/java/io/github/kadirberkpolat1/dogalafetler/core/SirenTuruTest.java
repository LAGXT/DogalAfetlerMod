package io.github.kadirberkpolat1.dogalafetler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SirenTuruTest {
    /** K0-19 rev. K-E67: güneş patlaması sireni 25 sn; ay çarpması yaklaşması kullanıcı isteğiyle 2 dakika. */
    @Test
    void standartSirenOnSaniyeGunesYirmiBesSaniyeAyIkiDakika() {
        for (AfetTuru tur : AfetTuru.values()) {
            int beklenen = tur == AfetTuru.AY_CARPMASI ? 2400 : tur == AfetTuru.GUNES_PATLAMASI ? 500 : 200;
            assertEquals(beklenen, SirenTuru.sureTick(tur), tur.kimlik());
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
