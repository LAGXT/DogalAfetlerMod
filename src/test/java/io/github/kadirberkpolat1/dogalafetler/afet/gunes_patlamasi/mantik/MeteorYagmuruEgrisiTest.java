package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MeteorYagmuruEgrisiTest {
    private static final double EPS = 1e-9;
    private static final long SIREN = 1200;
    private static final long GUN = GunesPatlamasiEgrileri.GUN_TICK;

    @Test
    void oncuYagmurOnUyaridaBaslarSirendeKesintisizArtar() {
        long onUyari = 1200;
        assertEquals(6.0, MeteorYagmuruEgrisi.onUyari(1200, onUyari, SIREN, 1800, 6, 24), EPS, "kısa ön-uyarıda baştan");
        assertEquals(15.0, MeteorYagmuruEgrisi.onUyari(0, onUyari, SIREN, 1800, 6, 24), EPS);
        assertEquals(MeteorYagmuruEgrisi.onUyari(0, onUyari, SIREN, 1800, 6, 24),
                MeteorYagmuruEgrisi.siren(0, onUyari, SIREN, 1800, 6, 24), EPS, "siren başında sıçrama yok");
        assertEquals(24.0, MeteorYagmuruEgrisi.siren(SIREN, onUyari, SIREN, 1800, 6, 24), EPS);
        assertEquals(24.0, MeteorYagmuruEgrisi.siren(5 * SIREN, onUyari, SIREN, 1800, 6, 24), EPS);
        double onceki = -1;
        for (long kalan = onUyari; kalan >= 0; kalan -= 50) {
            double y = MeteorYagmuruEgrisi.onUyari(kalan, onUyari, SIREN, 1800, 6, 24);
            assertTrue(y >= onceki, "azalmamalı: kalan " + kalan);
            onceki = y;
        }
    }

    @Test
    void oncuPenceresiOncesiYagmurYokSifirdaYalnizSiren() {
        assertEquals(0.0, MeteorYagmuruEgrisi.onUyari(700, 1200, SIREN, 600, 6, 24), EPS);
        assertEquals(6.0, MeteorYagmuruEgrisi.onUyari(600, 1200, SIREN, 600, 6, 24), EPS);
        assertEquals(0.0, MeteorYagmuruEgrisi.onUyari(0, 1200, SIREN, 0, 6, 24), EPS);
        assertEquals(6.0, MeteorYagmuruEgrisi.siren(0, 1200, SIREN, 0, 6, 24), EPS);
        assertEquals(15.0, MeteorYagmuruEgrisi.siren(600, 1200, SIREN, 0, 6, 24), EPS);
    }

    @Test
    void aktifteHerGununBasindaYagmurArasindaArkaPlan() {
        long sure = 5 * GUN;
        assertTrue(MeteorYagmuruEgrisi.yagmurda(0, 3600));
        assertTrue(MeteorYagmuruEgrisi.yagmurda(3599, 3600));
        assertFalse(MeteorYagmuruEgrisi.yagmurda(3600, 3600));
        assertTrue(MeteorYagmuruEgrisi.yagmurda(2 * GUN + 10, 3600));
        assertFalse(MeteorYagmuruEgrisi.yagmurda(0, 0), "0 süre: yağmur yok");
        assertEquals(24.0, MeteorYagmuruEgrisi.aktif(0, sure, 1.5, 5, 3600, 24), EPS);
        assertEquals(24.0, MeteorYagmuruEgrisi.aktif(3 * GUN + 100, sure, 1.5, 5, 3600, 24), EPS);
        assertEquals(GunesPatlamasiEgrileri.dakikaBasinaMeteor(5000, sure, 1.5, 5),
                MeteorYagmuruEgrisi.aktif(5000, sure, 1.5, 5, 3600, 24), EPS);
        assertEquals(5.0, MeteorYagmuruEgrisi.aktif(sure, sure, 1.5, 5, 3600, 2), EPS, "arka plan büyükse o");
    }
}
