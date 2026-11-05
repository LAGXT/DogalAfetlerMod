package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import org.junit.jupiter.api.Test;

class GeciciCarpanTest {
    @Test
    void sabitSureBoyuncaTepeDegerdeKalir() {
        GeciciCarpan carpan = new GeciciCarpan(AfetTuru.TSUNAMI, 3.5, 1000, 72_000, 48_000, SonumTipi.DOGRUSAL);
        assertEquals(3.5, carpan.deger(1000), 1e-12);
        assertEquals(3.5, carpan.deger(1000 + 72_000), 1e-12);
        assertEquals(1.0, carpan.deger(999), 1e-12, "başlamadan önce etkisiz");
    }

    @Test
    void dogrusalSonumBireIner() {
        GeciciCarpan carpan = new GeciciCarpan(AfetTuru.TSUNAMI, 3, 0, 100, 1000, SonumTipi.DOGRUSAL);
        assertEquals(2.0, carpan.deger(600), 1e-9, "yarı yolda tepe ile 1'in ortası");
        assertEquals(1.0, carpan.deger(1100), 1e-12);
        assertEquals(1.0, carpan.deger(5000), 1e-12);
        assertTrue(carpan.bitti(1100));
        assertFalse(carpan.bitti(1099));
    }

    @Test
    void ustelSonumTekduzeAzalirVeSondaBirOlur() {
        GeciciCarpan carpan = new GeciciCarpan(AfetTuru.TSUNAMI, 4, 0, 0, 10_000, SonumTipi.USTEL);
        double onceki = carpan.deger(0);
        assertEquals(4.0, onceki, 1e-12);
        for (long t = 100; t <= 10_000; t += 100) {
            double deger = carpan.deger(t);
            assertTrue(deger <= onceki + 1e-12, "üstel sönüm artıyor: " + t);
            assertTrue(deger >= 1.0 - 1e-12);
            onceki = deger;
        }
        assertEquals(1.0, carpan.deger(10_000), 1e-9);
        assertTrue(carpan.deger(2000) < 1 + (4 - 1) * 0.8, "üstel sönüm başta doğrusaldan hızlı iner");
    }

    @Test
    void birdenKucukCarpanDaBireCikar() {
        GeciciCarpan carpan = new GeciciCarpan(AfetTuru.DEPREM, 0.5, 0, 0, 100, SonumTipi.DOGRUSAL);
        assertEquals(0.75, carpan.deger(50), 1e-9);
        assertEquals(1.0, carpan.deger(100), 1e-12);
    }

    @Test
    void listeTureGoreCarparVeBitenleriTemizler() {
        CarpanListesi liste = new CarpanListesi();
        assertEquals(1.0, liste.carpan(AfetTuru.TSUNAMI, 0), 1e-12);
        liste.ekle(new GeciciCarpan(AfetTuru.TSUNAMI, 2, 0, 100, 0, SonumTipi.DOGRUSAL));
        liste.ekle(new GeciciCarpan(AfetTuru.TSUNAMI, 3, 0, 200, 0, SonumTipi.DOGRUSAL));
        liste.ekle(new GeciciCarpan(AfetTuru.KASIRGA, 5, 0, 50, 0, SonumTipi.DOGRUSAL));
        assertEquals(6.0, liste.carpan(AfetTuru.TSUNAMI, 10), 1e-12);
        assertEquals(5.0, liste.carpan(AfetTuru.KASIRGA, 10), 1e-12);
        assertEquals(1.0, liste.carpan(AfetTuru.DEPREM, 10), 1e-12);
        assertTrue(liste.temizle(150));
        assertEquals(1, liste.liste().size());
        assertEquals(3.0, liste.carpan(AfetTuru.TSUNAMI, 150), 1e-12);
        assertFalse(liste.temizle(150));
        liste.sifirla();
        assertTrue(liste.bosMu());
    }

    @Test
    void carpanZamanlaBireIner() {
        CarpanListesi liste = new CarpanListesi();
        liste.ekle(new GeciciCarpan(AfetTuru.TSUNAMI, 3.5, 0, 72_000, 48_000, SonumTipi.DOGRUSAL));
        double onceki = Double.MAX_VALUE;
        for (long t = 0; t <= 130_000; t += 1000) {
            double deger = liste.carpan(AfetTuru.TSUNAMI, t);
            assertTrue(deger <= onceki + 1e-12);
            onceki = deger;
        }
        assertEquals(1.0, liste.carpan(AfetTuru.TSUNAMI, 120_000), 1e-12);
    }

    @Test
    void gecersizCarpanReddedilir() {
        assertThrows(IllegalArgumentException.class,
                () -> new GeciciCarpan(AfetTuru.TSUNAMI, 0, 0, 1, 1, SonumTipi.DOGRUSAL));
        assertThrows(IllegalArgumentException.class,
                () -> new GeciciCarpan(AfetTuru.TSUNAMI, 2, 0, -1, 1, SonumTipi.DOGRUSAL));
        assertThrows(IllegalArgumentException.class,
                () -> new GeciciCarpan(AfetTuru.TSUNAMI, Double.NaN, 0, 1, 1, SonumTipi.DOGRUSAL));
    }
}
