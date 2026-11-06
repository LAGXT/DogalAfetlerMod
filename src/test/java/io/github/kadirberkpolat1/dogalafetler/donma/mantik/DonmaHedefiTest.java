package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DonmaHedefiTest {
    @Test
    void donmaDonemindeHedefGlobalIlerlemedir() {
        assertEquals(30, DonmaHedefi.sutunHedefi(30, false, 1.0, 10.0));
        assertEquals(30, DonmaHedefi.sutunHedefi(30, false, 0.0, 0.0));
    }

    @Test
    void cozulmedeKisAgirligiHedefiYuzeCeker() {
        assertEquals(100, DonmaHedefi.sutunHedefi(40, true, 1.0, 0.0));
        assertEquals(40, DonmaHedefi.sutunHedefi(40, true, 0.0, 0.0));
        assertEquals(70, DonmaHedefi.sutunHedefi(40, true, 0.5, 0.0));
    }

    @Test
    void ilikBolgeOnceIner() {
        assertEquals(30, DonmaHedefi.sutunHedefi(40, true, 0.0, 10.0));
        assertEquals(100, DonmaHedefi.sutunHedefi(40, true, 1.0, 10.0), "kış bölgesinde ofset yok");
        assertTrue(DonmaHedefi.sutunHedefi(60, true, 0.0, 10.0) < DonmaHedefi.sutunHedefi(60, true, 0.0, 2.0));
    }

    @Test
    void hedefSinirlarIcindeKalir() {
        assertEquals(100, DonmaHedefi.sutunHedefi(250, false, 0.0, 0.0));
        assertEquals(0, DonmaHedefi.sutunHedefi(-5, false, 0.0, 0.0));
        assertEquals(100, DonmaHedefi.sutunHedefi(40, true, 3.0, 0.0));
        assertEquals(0, DonmaHedefi.sutunHedefi(5, true, 0.0, 10.0));
    }

    @Test
    void seviyeHedefeAdimAdimGider() {
        assertEquals(65, DonmaHedefi.siradakiSeviye(60, 100, 5));
        assertEquals(100, DonmaHedefi.siradakiSeviye(98, 100, 5));
        assertEquals(55, DonmaHedefi.siradakiSeviye(60, 40, 5));
        assertEquals(40, DonmaHedefi.siradakiSeviye(42, 40, 5));
        assertEquals(60, DonmaHedefi.siradakiSeviye(60, 60, 5));
    }

    @Test
    void yonluDegerDonarkenAzalmazCozulurkenArtmaz() {
        assertEquals(3, DonmaHedefi.yonlu(1, 3, 1));
        assertEquals(3, DonmaHedefi.yonlu(3, 1, 1));
        assertEquals(1, DonmaHedefi.yonlu(3, 1, -1));
        assertEquals(1, DonmaHedefi.yonlu(1, 3, -1));
        assertEquals(2, DonmaHedefi.yonlu(2, 5, 0));
    }
}
