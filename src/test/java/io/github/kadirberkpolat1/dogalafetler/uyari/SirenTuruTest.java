package io.github.kadirberkpolat1.dogalafetler.uyari;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import org.junit.jupiter.api.Test;

class SirenTuruTest {
    @Test
    void yalnizGunesPatlamasiOzelSirenKullanir() {
        for (AfetTuru afet : AfetTuru.values()) {
            SirenTuru beklenen = afet == AfetTuru.GUNES_PATLAMASI ? SirenTuru.GUNES : SirenTuru.STANDART;
            assertEquals(beklenen, SirenTuru.afetIcin(afet), afet.kimlik());
        }
    }

    @Test
    void sirenSureleriMadde5VeK019() {
        for (AfetTuru afet : AfetTuru.values()) {
            int beklenen = switch (afet) {
                case GUNES_PATLAMASI, AY_CARPMASI -> 60 * 20;
                default -> 10 * 20;
            };
            assertEquals(beklenen, SirenTuru.sureTick(afet), afet.kimlik());
        }
    }

    @Test
    void kimlikKomuttakiAdlaCozulur() {
        assertEquals(SirenTuru.STANDART, SirenTuru.kimlikIle("STANDART").orElseThrow());
        assertEquals(SirenTuru.GUNES, SirenTuru.kimlikIle("gunes").orElseThrow());
        assertTrue(SirenTuru.kimlikIle("güneş").isEmpty());
        assertEquals("siren_standart", SirenTuru.STANDART.sesAdi());
        assertEquals("siren_gunes", SirenTuru.GUNES.sesAdi());
    }
}
