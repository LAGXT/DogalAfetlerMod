package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Yalıtım toplamı, termostat kancası (madde 13c, N4), eşikler ve süreli yiyecek etkisi (madde 14 kancası). */
class YalitimVeEsiklerTest {
    private static final Yalitim.Ayarlar Y = Yalitim.Ayarlar.VARSAYILAN;
    private static final Esikler E = Esikler.VARSAYILAN;

    @Test
    void toplamYalitimDortParcaninToplamidir() {
        assertEquals(0, Yalitim.toplam(new Yalitim.Girdi(0, 0, 0, 0), Y), 1e-12);
        double tek = Yalitim.toplam(new Yalitim.Girdi(1, 0, 0, 0), Y);
        assertEquals(4 * tek, Yalitim.toplam(new Yalitim.Girdi(4, 0, 0, 0), Y), 1e-12);
        double tuy = Yalitim.toplam(new Yalitim.Girdi(0, 0, 12, 0), Y);
        assertEquals(12 * Y.tuyBasina(), tuy, 1e-12);
        double yun = Yalitim.toplam(new Yalitim.Girdi(2, 2, 0, 0), Y) - Yalitim.toplam(new Yalitim.Girdi(2, 0, 0, 0), Y);
        assertEquals(2 * Y.yunBasina(), yun, 1e-12);
        assertEquals(Yalitim.toplam(new Yalitim.Girdi(4, 0, 12, 0), Y),
                Yalitim.kuruToplam(new Yalitim.Girdi(4, 3, 12, 0), Y), 1e-12, "kuru toplam yünü saymaz");
        assertThrows(IllegalArgumentException.class, () -> new Yalitim.Girdi(5, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new Yalitim.Girdi(2, 3, 0, 0));
    }

    @Test
    void yunSogumaCarpaniAsiriSoguktaYalnizYavaslatir() {
        for (int n = 0; n <= 4; n++) {
            assertEquals(1 - 0.25 * n, Yalitim.yunSogumaCarpani(n, false, Y), 1e-12);
            double asiri = Yalitim.yunSogumaCarpani(n, true, Y);
            assertTrue(asiri > 0, "aşırı soğukta sıfır olmamalı");
            assertTrue(asiri >= 1 - 0.25 * n);
        }
    }

    @Test
    void termostatNotreCekerUclardaAzalirAmaHicKaybolmaz() {
        double ilimli = Yalitim.termostatCekimi(-40, -30, 4, Y);
        double uc = Yalitim.termostatCekimi(-40, -200, 4, Y);
        assertTrue(ilimli > 0, "soğukta ısıtır");
        assertTrue(uc > 0, "uçta da biraz ısıtır");
        assertTrue(uc < ilimli / 2, ilimli + " / " + uc);
        assertTrue(Yalitim.termostatCekimi(40, 30, 4, Y) < 0, "sıcakta soğutur");
        assertEquals(0, Yalitim.termostatCekimi(0, -30, 4, Y), 1e-12);
        assertEquals(0, Yalitim.termostatCekimi(-40, -30, 0, Y), 1e-12);
        // Bir parçanın çekimi dört parçanın dörtte biri; çekim nötrü geçmez.
        assertEquals(ilimli / 4, Yalitim.termostatCekimi(-40, -30, 1, Y), 1e-12);
        assertEquals(0.01, Yalitim.termostatCekimi(-0.01, -30, 4, Y), 1e-12);
        // Hiçbir koşulda tam koruma yok: dört parça uç soğukta soğumayı yenemez.
        IsiAlisverisi.Girdi g = new IsiAlisverisi.Girdi(-40, -45, Etkinlik.YURUME, Islaklik.KURU,
                new Yalitim.Girdi(4, 0, 0, 4));
        assertTrue(IsiAlisverisi.egilim(g, IsiAlisverisi.Ayarlar.VARSAYILAN, Y) < 0);
    }

    @Test
    void kademelerSiraliVeOkunakli() {
        assertEquals(Kademe.NOTR, E.kademe(0));
        assertEquals(Kademe.NOTR, E.kademe(-29.9));
        assertEquals(Kademe.SOGUK, E.kademe(-30));
        assertEquals(Kademe.COK_SOGUK, E.kademe(-55));
        assertEquals(Kademe.DONUYOR, E.kademe(-75));
        assertEquals(Kademe.SICAK, E.kademe(30));
        assertEquals(Kademe.COK_SICAK, E.kademe(55));
        assertEquals(Kademe.YANIYOR, E.kademe(75));
        assertEquals(Kademe.YANIYOR, E.kademe(100));
    }

    @Test
    void yavaslamaVeHasarKademeli() {
        assertEquals(0, E.yavaslama(-29), 1e-12);
        assertTrue(E.yavaslama(-40) > 0);
        assertTrue(E.yavaslama(-90) > E.yavaslama(-60));
        assertTrue(E.yavaslama(-100) <= 0.5 + 1e-12);
        assertEquals(0, E.hasarAraligi(-74.9));
        assertEquals(0, E.hasarAraligi(74.9));
        int donmaBasi = E.hasarAraligi(-75);
        int donmaSonu = E.hasarAraligi(-100);
        assertTrue(donmaBasi > donmaSonu && donmaSonu > 0, donmaBasi + " / " + donmaSonu);
        assertTrue(E.hasarAraligi(75) > E.hasarAraligi(100));
        assertTrue(donmaSonu >= 20, "ani değil: en sık saniyede bir");
    }

    @Test
    void sureliEtkiToplamiVerirVeGiderekAzalir() {
        SureliEtki etki = new SureliEtki(30, 1800);
        double toplam = 0;
        double ilk = etki.adim(20);
        toplam += ilk;
        double son = ilk;
        while (!etki.bitti()) {
            double d = etki.adim(20);
            assertTrue(d <= son + 1e-12, "giderek azalır");
            son = d;
            toplam += d;
        }
        assertEquals(30, toplam, 1e-9);
        assertEquals(0, etki.adim(20), 1e-12);
        SureliEtki serin = new SureliEtki(-20, 1200);
        double s = 0;
        while (!serin.bitti()) {
            s += serin.adim(37);
        }
        assertEquals(-20, s, 1e-9);
    }

    @Test
    void sureliEtkiKayittanAyniDevamEder() {
        SureliEtki a = new SureliEtki(40, 3000);
        a.adim(400);
        SureliEtki b = new SureliEtki(a.toplam(), a.sure(), a.gecen());
        assertEquals(a.adim(100), b.adim(100), 1e-12);
    }
}
