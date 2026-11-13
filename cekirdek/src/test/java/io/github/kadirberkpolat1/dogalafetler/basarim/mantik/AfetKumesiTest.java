package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.List;
import org.junit.jupiter.api.Test;

class AfetKumesiTest {
    @Test
    void dokuzTurunHepsiGerekir() {
        AfetKumesi kume = new AfetKumesi();
        AfetTuru[] turler = AfetTuru.values();
        for (int i = 0; i < turler.length - 1; i++) {
            assertTrue(kume.ekle(turler[i]));
            assertFalse(kume.tamamMi(), "eksik: " + (turler.length - 1 - i));
        }
        assertFalse(kume.ekle(turler[0]), "aynı tür ikinci kez eklenmez");
        assertTrue(kume.ekle(turler[turler.length - 1]));
        assertTrue(kume.tamamMi());
        assertEquals(9, kume.boyut());
    }

    @Test
    void kimlikListesindenGeriKurulurBilinmeyenAtlanir() {
        AfetKumesi kume = AfetKumesi.kimliklerden(List.of("KASIRGA", "deprem", "volkan", "deprem"));
        assertEquals(List.of("deprem", "kasirga"), kume.kimlikler(), "sıralı, tekil, Locale.ROOT");
        assertTrue(kume.iceriyor(AfetTuru.KASIRGA));
        assertFalse(kume.iceriyor(AfetTuru.TSUNAMI));
        assertEquals(kume.kimlikler(), AfetKumesi.kimliklerden(kume.kimlikler()).kimlikler());
    }
}
