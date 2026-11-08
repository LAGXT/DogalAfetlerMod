package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import org.junit.jupiter.api.Test;

class MeteorYagmuruEgrisiTest {
    private static final double EPS = 1e-9;
    /** Güneş sireni 25 sn (K0-19 rev. K-E67). */
    private static final long SIREN = SirenTuru.UZUN_SURE_TICK;
    /** Varsayılan öncü payı (5 sn) ve sunucu geneli afetin en uzun ön-uyarısı (erken bildirim 100 + pencere 60 tick). */
    private static final long ONCU = 100;
    private static final long ON_UYARI = 160;
    private static final long GUN = GunesPatlamasiEgrileri.GUN_TICK;

    @Test
    void oncuYagmurOnUyaridaBaslarSirendeKesintisizArtar() {
        assertEquals(0.0, MeteorYagmuruEgrisi.onUyari(ONCU + 1, ON_UYARI, SIREN, ONCU, 6, 24), EPS, "pencereden önce yok");
        assertEquals(6.0, MeteorYagmuruEgrisi.onUyari(ONCU, ON_UYARI, SIREN, ONCU, 6, 24), EPS, "pencerenin başında 6/dk");
        assertEquals(9.0, MeteorYagmuruEgrisi.onUyari(0, ON_UYARI, SIREN, ONCU, 6, 24), EPS, "100 / 600 tick'te");
        assertEquals(MeteorYagmuruEgrisi.onUyari(0, ON_UYARI, SIREN, ONCU, 6, 24),
                MeteorYagmuruEgrisi.siren(0, ON_UYARI, SIREN, ONCU, 6, 24), EPS, "siren başında sıçrama yok");
        assertEquals(24.0, MeteorYagmuruEgrisi.siren(SIREN, ON_UYARI, SIREN, ONCU, 6, 24), EPS);
        assertEquals(24.0, MeteorYagmuruEgrisi.siren(5 * SIREN, ON_UYARI, SIREN, ONCU, 6, 24), EPS);
        assertEquals(6.0, MeteorYagmuruEgrisi.onUyari(60, 60, SIREN, ONCU, 6, 24), EPS, "kısa ön-uyarıda baştan");
        assertEquals(30 * 20, ONCU + SIREN, "öncü yağmur AKTIF'ten önceki son 30 sn (K-E67)");
        double onceki = -1;
        for (long kalan = ON_UYARI; kalan >= 0; kalan -= 10) {
            double y = MeteorYagmuruEgrisi.onUyari(kalan, ON_UYARI, SIREN, ONCU, 6, 24);
            assertTrue(y >= onceki, "azalmamalı: kalan " + kalan);
            onceki = y;
        }
        for (long gecen = 0; gecen <= SIREN; gecen += 10) {
            double y = MeteorYagmuruEgrisi.siren(gecen, ON_UYARI, SIREN, ONCU, 6, 24);
            assertTrue(y >= onceki, "azalmamalı: siren " + gecen);
            onceki = y;
        }
    }

    @Test
    void oncuPenceresiOncesiYagmurYokSifirdaYalnizSiren() {
        assertEquals(0.0, MeteorYagmuruEgrisi.onUyari(61, ON_UYARI, SIREN, 60, 6, 24), EPS);
        assertEquals(6.0, MeteorYagmuruEgrisi.onUyari(60, ON_UYARI, SIREN, 60, 6, 24), EPS);
        assertEquals(0.0, MeteorYagmuruEgrisi.onUyari(0, ON_UYARI, SIREN, 0, 6, 24), EPS);
        assertEquals(6.0, MeteorYagmuruEgrisi.siren(0, ON_UYARI, SIREN, 0, 6, 24), EPS);
        assertEquals(15.0, MeteorYagmuruEgrisi.siren(SIREN / 2, ON_UYARI, SIREN, 0, 6, 24), EPS);
    }

    @Test
    void onUyariSonundaOncuArtisSurer() {
        assertEquals(9.0, MeteorYagmuruEgrisi.onUyari(0, ON_UYARI, SIREN, ONCU, 6, 24), EPS, "ön-uyarı ucunda 9/dk");
        assertEquals(9.0, MeteorYagmuruEgrisi.siren(0, ON_UYARI, SIREN, ONCU, 6, 24), EPS, "sirende kaldığı yerden");
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
