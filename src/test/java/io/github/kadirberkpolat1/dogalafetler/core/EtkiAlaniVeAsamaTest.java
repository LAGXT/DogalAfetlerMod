package io.github.kadirberkpolat1.dogalafetler.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class EtkiAlaniVeAsamaTest {
    @Test
    void etkiAlaniYatayMesafeyleOlculur() {
        EtkiAlani alan = EtkiAlani.daire(100, 64, 100, 30);
        assertTrue(alan.iceriyor(100, -40, 129, 0), "derinlik fark etmez");
        assertTrue(alan.iceriyor(130, 64, 100, 0));
        assertFalse(alan.iceriyor(131, 64, 100, 0));
        assertTrue(alan.iceriyor(131, 64, 100, 64), "ek mesafeyle içeride");
        assertFalse(alan.iceriyor(100 + 30 + 65, 64, 100, 64));
    }

    @Test
    void globalAlanHerYeriIcerir() {
        EtkiAlani alan = EtkiAlani.herYer();
        assertTrue(alan.global());
        assertTrue(alan.iceriyor(1e7, 0, -1e7, 0));
    }

    @Test
    void asamalarSiraylaIlerler() {
        assertEquals(Optional.of(AfetAsamasi.SIREN), AfetAsamasi.ON_UYARI.sonraki());
        assertEquals(Optional.of(AfetAsamasi.AKTIF), AfetAsamasi.SIREN.sonraki());
        assertEquals(Optional.of(AfetAsamasi.BITIS), AfetAsamasi.AKTIF.sonraki());
        assertEquals(Optional.empty(), AfetAsamasi.BITIS.sonraki());
        for (AfetAsamasi asama : AfetAsamasi.values()) {
            assertEquals(Optional.of(asama), AfetAsamasi.kimlikIle(asama.kimlik()));
        }
    }

    @Test
    void gunesEvresiKimlikleriCozulur() {
        for (GunesEvresi evre : GunesEvresi.values()) {
            assertEquals(evre, GunesEvresi.kimlikIle(evre.kimlik()));
        }
        assertEquals(GunesEvresi.NORMAL, GunesEvresi.kimlikIle("bilinmeyen"));
    }

    @Test
    void sunucuGeneliAfetlerGunesVeAy() {
        for (AfetTuru tur : AfetTuru.values()) {
            assertEquals(tur == AfetTuru.GUNES_PATLAMASI || tur == AfetTuru.AY_CARPMASI, tur.sunucuGeneli(), tur.kimlik());
        }
    }
}
