package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.PunchySicaklikMantigi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import org.junit.jupiter.api.Test;

class PunchySicaklikMantigiTest {
    @Test
    void dortDurumunTamamindaBenzersizKlipVeMomentVar() {
        assertNull(PunchySicaklikMantigi.klipAdi(SicaklikDurumu.NORMAL));
        assertNull(PunchySicaklikMantigi.momentAdi(SicaklikDurumu.NORMAL));

        for (SicaklikDurumu d : new SicaklikDurumu[] {
            SicaklikDurumu.BUNALMA, SicaklikDurumu.YANMA, SicaklikDurumu.USUME, SicaklikDurumu.DONMA
        }) {
            assertNotNull(PunchySicaklikMantigi.klipAdi(d));
            assertNotNull(PunchySicaklikMantigi.momentAdi(d));
            assertTrue(PunchySicaklikMantigi.klipAdi(d).startsWith("vz_sicaklik_"));
            assertTrue(PunchySicaklikMantigi.momentAdi(d).startsWith("dogalafetler:sicaklik_"));
        }
    }

    @Test
    void kolKaplamalariVeMikroSalinimSiddetSirasiDogru() {
        assertEquals(0f, PunchySicaklikMantigi.yanikYogunlugu(SicaklikDurumu.NORMAL));
        assertTrue(PunchySicaklikMantigi.yanikYogunlugu(SicaklikDurumu.YANMA)
                > PunchySicaklikMantigi.yanikYogunlugu(SicaklikDurumu.BUNALMA));
        assertTrue(PunchySicaklikMantigi.kiragiYogunlugu(SicaklikDurumu.DONMA)
                > PunchySicaklikMantigi.kiragiYogunlugu(SicaklikDurumu.USUME));

        assertEquals(0f, PunchySicaklikMantigi.kolEgimDerece(SicaklikDurumu.NORMAL, 0.23, false));
        assertEquals(
                -PunchySicaklikMantigi.kolYalpaDerece(SicaklikDurumu.DONMA, 0.10, false),
                PunchySicaklikMantigi.kolYalpaDerece(SicaklikDurumu.DONMA, 0.10, true),
                1e-5f);
    }

    @Test
    void tepkiMesgulkenAslaCalismazBosaCikincaVePeriyottaTetiklenir() {
        // İlk geçiş: oyuncu meşgulse tetiklenmez, boşa çıkınca hemen tetiklenir
        assertFalse(PunchySicaklikMantigi.tepkiTetiklenmeli(
                SicaklikDurumu.NORMAL, SicaklikDurumu.BUNALMA, 200L, -1L, true));
        assertTrue(PunchySicaklikMantigi.tepkiTetiklenmeli(
                SicaklikDurumu.NORMAL, SicaklikDurumu.BUNALMA, 200L, -1L, false));

        // Aynı durumda periyottan önce tekrar etmez, periyot dolunca tekrar eder
        assertFalse(PunchySicaklikMantigi.tepkiTetiklenmeli(
                SicaklikDurumu.BUNALMA, SicaklikDurumu.BUNALMA, 500L, 200L, false));
        assertTrue(PunchySicaklikMantigi.tepkiTetiklenmeli(
                SicaklikDurumu.BUNALMA, SicaklikDurumu.BUNALMA, 200L + PunchySicaklikMantigi.ORTA_TEKRAR_TICK, 200L, false));

        // Ağırlaşma geçişi (BUNALMA -> YANMA): spam eşiği geçildiyse periyodu beklemeden tetiklenir
        assertFalse(PunchySicaklikMantigi.tepkiTetiklenmeli(
                SicaklikDurumu.BUNALMA, SicaklikDurumu.YANMA, 250L, 200L, false));
        assertTrue(PunchySicaklikMantigi.tepkiTetiklenmeli(
                SicaklikDurumu.BUNALMA, SicaklikDurumu.YANMA, 200L + PunchySicaklikMantigi.GECIS_BEKLEME_TICK, 200L, false));
    }
}
