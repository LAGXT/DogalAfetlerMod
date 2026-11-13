package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** K-E83: yaklaşan ay, olay başladığı anda vanilla ayın bulunduğu yerden gelir (gölgelendirici eğimi dahil). */
class AyYonuTest {
    private static final double EPS = 1e-9;

    @Test
    void vanillaAyGeceYarisiTepedeAksamDogudaDoguBatiyaGider() {
        double[] gece = AyYonu.vanillaAy(0.5, 0);
        assertEquals(0.0, gece[0], EPS, "gece yarısı tam tepede");
        assertEquals(1.0, gece[1], EPS);
        assertEquals(0.0, gece[2], EPS);
        double[] aksam = AyYonu.vanillaAy(0.25, 0);
        assertEquals(1.0, aksam[0], EPS, "akşamda doğudan doğar");
        assertEquals(0.0, aksam[1], EPS);
        double[] sabah = AyYonu.vanillaAy(0.75, 0);
        assertEquals(-1.0, sabah[0], EPS, "sabaha doğru batıda");
        assertEquals(0.0, sabah[1], 1e-12);
        for (double z = 0; z < 1.0; z += 0.037) {
            double[] d = AyYonu.vanillaAy(z, -25);
            assertEquals(1.0, Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]), 1e-9, "birim yön, zaman " + z);
        }
    }

    /** Gölgelendirici paketi güneş yolunu yattırır (varsayılan -25°): gece yarısı ay tepede değil, 65°'de ve güneyde. */
    @Test
    void gokEgimiAyiTepedenKaydirir() {
        double[] d = AyYonu.vanillaAy(0.5, -25);
        assertEquals(Math.toDegrees(Math.asin(d[1])), 65.0, 1e-9);
        assertTrue(d[2] > 0.4, "güneye (+Z) kayar: " + d[2]);
        assertEquals(0.0, d[0], 1e-9);
        AyYonu.Baslangic b = AyYonu.baslangic(0.5, -25);
        assertEquals(65.0, b.egimDerece(), 1e-4);
        assertEquals(90.0, b.azimutDerece(), 1e-4, "+Z (güney) 90°");
    }

    @Test
    void baslangicVanillaAyinAzimutuVeYuksekligidir() {
        AyYonu.Baslangic dogu = AyYonu.baslangic(125.0 / 360.0, 0);
        assertEquals(35.0, dogu.egimDerece(), 1e-4);
        assertEquals(0.0, dogu.azimutDerece(), 1e-4, "doğudaki ay doğudan gelir");
        AyYonu.Baslangic bati = AyYonu.baslangic(1.0 - 125.0 / 360.0, 0);
        assertEquals(35.0, bati.egimDerece(), 1e-4);
        assertEquals(180.0, Math.abs(bati.azimutDerece()), 1e-4, "batıdaki ay batıdan gelir (eskiden hep doğudan)");
    }

    @Test
    void tepedekiAyYuksekteKalirAzimutuGidisindenAlinir() {
        AyYonu.Baslangic b = AyYonu.baslangic(0.5, 0);
        assertEquals(AyEgrileri.EN_COK_BASLANGIC_EGIMI, b.egimDerece(), 1e-4, "dimdik tepe 89°'ye kırpılır");
        assertEquals(180.0, Math.abs(b.azimutDerece()), 1e-3, "ay batıya doğru gider");
        assertTrue(Float.isFinite(b.azimutDerece()));
    }

    @Test
    void ufkunAltindakiAyUfkaYakinDogar() {
        AyYonu.Baslangic gunduz = AyYonu.baslangic(0.0, 0);
        assertEquals(AyEgrileri.EN_AZ_BASLANGIC_EGIMI, gunduz.egimDerece(), 1e-4, "öğlen ay ufkun altında");
        assertTrue(Float.isFinite(gunduz.azimutDerece()));
        for (double z = 0; z < 1.0; z += 0.01) {
            AyYonu.Baslangic b = AyYonu.baslangic(z, -25);
            assertTrue(b.egimDerece() >= AyEgrileri.EN_AZ_BASLANGIC_EGIMI && b.egimDerece() <= AyEgrileri.EN_COK_BASLANGIC_EGIMI,
                    "sınırlar içinde, zaman " + z);
            assertTrue(Float.isFinite(b.azimutDerece()), "azimut sonlu, zaman " + z);
        }
    }
}
