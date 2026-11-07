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
    void ilkGun10DakikaSonrakiGunler3DakikaArasindaArkaPlan() {
        long sure = 5 * GUN;
        long ilk = 12_000;
        long gunluk = 3_600;
        assertTrue(MeteorYagmuruEgrisi.yagmurda(0, ilk, gunluk));
        assertTrue(MeteorYagmuruEgrisi.yagmurda(11_999, ilk, gunluk), "ilk gün 10 dk");
        assertFalse(MeteorYagmuruEgrisi.yagmurda(12_000, ilk, gunluk));
        assertTrue(MeteorYagmuruEgrisi.yagmurda(GUN + 3_599, ilk, gunluk));
        assertFalse(MeteorYagmuruEgrisi.yagmurda(GUN + 3_600, ilk, gunluk), "sonraki günler 3 dk");
        assertTrue(MeteorYagmuruEgrisi.yagmurda(2 * GUN + 10, ilk, gunluk));
        assertFalse(MeteorYagmuruEgrisi.yagmurda(GUN, ilk, 0), "0 süre: yağmur yok");
        assertEquals(90.0, MeteorYagmuruEgrisi.aktif(0, sure, 1.5, 5, ilk, gunluk, 90), EPS);
        assertEquals(90.0, MeteorYagmuruEgrisi.aktif(9_000, sure, 1.5, 5, ilk, gunluk, 90), EPS);
        assertEquals(90.0, MeteorYagmuruEgrisi.aktif(3 * GUN + 100, sure, 1.5, 5, ilk, gunluk, 90), EPS);
        assertEquals(GunesPatlamasiEgrileri.dakikaBasinaMeteor(15_000, sure, 1.5, 5),
                MeteorYagmuruEgrisi.aktif(15_000, sure, 1.5, 5, ilk, gunluk, 90), EPS);
        assertEquals(5.0, MeteorYagmuruEgrisi.aktif(sure, sure, 1.5, 5, ilk, gunluk, 2), EPS, "arka plan büyükse o");
    }
}
