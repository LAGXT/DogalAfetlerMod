package io.github.kadirberkpolat1.dogalafetler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AfetTuruTest {
    @Test
    void kimliklerN18IleAyni() {
        List<String> kimlikler = Arrays.stream(AfetTuru.values()).map(AfetTuru::kimlik).toList();
        assertEquals(List.of("kasirga", "tsunami", "deprem", "gunes_patlamasi", "asit_yagmuru", "sinkhole",
                "orman_yangini", "ay_carpmasi"), kimlikler);
    }

    @Test
    void yalnizGunesPatlamasiVeAyCarpmasiSeviyesiz() {
        for (AfetTuru tur : AfetTuru.values()) {
            boolean seviyesiz = tur == AfetTuru.GUNES_PATLAMASI || tur == AfetTuru.AY_CARPMASI;
            assertEquals(!seviyesiz, tur.seviyeli(), tur.kimlik());
        }
    }

    @Test
    void kimlikTurkceYereldeBuyukHarfleCozulur() {
        Locale onceki = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(Optional.of(AfetTuru.KASIRGA), AfetTuru.kimlikIle("KASIRGA"));
            assertEquals(Optional.of(AfetTuru.ASIT_YAGMURU), AfetTuru.kimlikIle(" ASIT_YAGMURU "));
        } finally {
            Locale.setDefault(onceki);
        }
    }

    @Test
    void bilinmeyenKimlikBosDoner() {
        assertTrue(AfetTuru.kimlikIle("meteor").isEmpty());
        assertTrue(AfetTuru.kimlikIle(null).isEmpty());
        assertFalse(AfetTuru.kimlikIle("tsunami").isEmpty());
    }
}
