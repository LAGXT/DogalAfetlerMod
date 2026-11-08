package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** S3-02, S3-05: hissedilen sıcaklık (ıslaklık, rüzgâr soğuğu, sol el). */
class HissedilenTest {
    private static final Hissedilen.Ayarlar A = Hissedilen.Ayarlar.VARSAYILAN;

    private static Hissedilen.Girdi girdi(double mekan, double w, boolean gok, int y, Yagis yagis, boolean kosu,
            SolEl el) {
        return new Hissedilen.Girdi(mekan, w, gok, y, yagis, kosu, el);
    }

    private static Hissedilen.Sonuc hesapla(Hissedilen.Girdi g) {
        return Hissedilen.hesapla(g, A);
    }

    @Test
    void kuruKapaliYerdeHissedilenMekandir() {
        Hissedilen.Sonuc s = hesapla(girdi(12, 0, false, 120, Yagis.YOK, true, SolEl.YOK));
        assertEquals(12, s.toplam(), 1e-12);
        assertTrue(s.duzeltmeler().isEmpty());
        assertEquals(0, s.ruzgarKmh(), 1e-12, "gökyüzü kapalıyken rüzgâr yok");
    }

    @Test
    void islaklikSicaktaDaSerinletir() {
        for (double mekan : new double[] {-5, 15, 35}) {
            Hissedilen.Sonuc s = hesapla(girdi(mekan, 0.6, false, 64, Yagis.YOK, false, SolEl.YOK));
            assertEquals(-A.islakSogutma() * 0.6, s.duzeltme(Katki.ISLAKLIK), 1e-12, mekan + " °C");
            assertEquals(mekan - 3, s.toplam(), 1e-12);
        }
    }

    @Test
    void ruzgarHiziYukseklikYagisVeKosmaylaArtar() {
        assertEquals(0, Hissedilen.ruzgarHizi(girdi(0, 0, true, 64, Yagis.YOK, false, SolEl.YOK), A), 1e-12);
        assertEquals(10, Hissedilen.ruzgarHizi(girdi(0, 0, true, 104, Yagis.YOK, false, SolEl.YOK), A), 1e-12);
        assertEquals(8, Hissedilen.ruzgarHizi(girdi(0, 0, true, 30, Yagis.YAGMUR, false, SolEl.YOK), A), 1e-12);
        assertEquals(20 + 12, Hissedilen.ruzgarHizi(girdi(0, 0, true, 64, Yagis.FIRTINA, true, SolEl.YOK), A),
                1e-12);
        assertEquals(0, Hissedilen.ruzgarHizi(girdi(0, 0, false, 200, Yagis.FIRTINA, true, SolEl.YOK), A), 1e-12);
    }

    @Test
    void ruzgarSoguguSinirlardaSifirIcerideNws() {
        assertEquals(0, Hissedilen.ruzgarSogugu(10.01, 40), 1e-12, "10 °C üstünde yok");
        assertEquals(0, Hissedilen.ruzgarSogugu(-20, 4.8), 1e-12, "4,8 km/h ve altında yok");
        // Sınırların içinde (V ≥ 9,8, T ≤ 5) yumuşatma tamdır: NWS değeri aynen.
        double nws = Hissedilen.wci(-10, 30) - (-10);
        assertEquals(nws, Hissedilen.ruzgarSogugu(-10, 30), 1e-12);
        assertEquals(-19.5, Hissedilen.wci(-10, 30), 0.1, "NWS: −10 °C, 30 km/h ≈ −19,5 °C (tabloda −20)");
    }

    @Test
    void ruzgarSoguguSurekliVeTekYonlu() {
        double onceki = 0;
        for (double v = 4.8; v <= 60; v += 0.05) {
            double d = Hissedilen.ruzgarSogugu(-5, v);
            assertTrue(d <= 0, "ısıtmaz: " + v);
            assertTrue(Math.abs(d - onceki) < 0.1, "sıçrama yok: " + v + " " + onceki + " → " + d);
            assertTrue(d <= onceki + 1e-9, "hızla artar: " + v);
            onceki = d;
        }
        onceki = 0;
        for (double t = 10; t >= -40; t -= 0.05) {
            double d = Hissedilen.ruzgarSogugu(t, 25);
            assertTrue(Math.abs(d - onceki) < 0.1, "sıcaklıkta sıçrama yok: " + t);
            onceki = d;
        }
    }

    @Test
    void dagdaFirtinadaKosarkenHissedilenCokDuser() {
        Hissedilen.Sonuc s = hesapla(girdi(-5, 0, true, 160, Yagis.FIRTINA, true, SolEl.YOK));
        assertEquals(24 + 20 + 12, s.ruzgarKmh(), 1e-12);
        assertTrue(s.duzeltme(Katki.RUZGAR) < -8, "rüzgâr: " + s.duzeltme(Katki.RUZGAR));
        Hissedilen.Sonuc ilik = hesapla(girdi(20, 0, true, 160, Yagis.FIRTINA, true, SolEl.YOK));
        assertEquals(0, ilik.duzeltme(Katki.RUZGAR), 1e-12, "ılıkta rüzgâr soğuğu yok");
    }

    @Test
    void solElBuzKovasiSerinletirLavKovasiIsitir() {
        Hissedilen.Sonuc buz = hesapla(girdi(30, 0, false, 64, Yagis.YOK, false, SolEl.BUZ_KOVASI));
        Hissedilen.Sonuc lav = hesapla(girdi(-10, 0, false, 64, Yagis.YOK, false, SolEl.LAV_KOVASI));
        assertEquals(18, buz.toplam(), 1e-12);
        assertEquals(A.buzKovasi(), buz.duzeltme(Katki.SOL_EL), 1e-12);
        assertEquals(SolEl.BUZ_KOVASI, buz.solEl());
        assertEquals(2, lav.toplam(), 1e-12);
        assertEquals(0, hesapla(girdi(30, 0, false, 64, Yagis.YOK, false, SolEl.YOK)).duzeltme(Katki.SOL_EL), 1e-12);
    }

    @Test
    void duzeltmelerToplamaEsitVeYalnizHissedilenKalemleri() {
        Hissedilen.Sonuc s = hesapla(girdi(-8, 0.4, true, 140, Yagis.YAGMUR, false, SolEl.LAV_KOVASI));
        double toplam = s.mekan();
        for (Katki k : s.duzeltmeler().keySet()) {
            assertTrue(k.hissedilen(), k.name());
            toplam += s.duzeltme(k);
        }
        assertEquals(s.toplam(), toplam, 1e-12);
        assertEquals(3, s.duzeltmeler().size());
    }

    @Test
    void buzKovasiEsikUstundeErir() {
        assertEquals(0, Hissedilen.erimeArtisi(25, A), 1e-12);
        assertEquals(0, Hissedilen.erimeArtisi(-5, A), 1e-12);
        assertEquals(2, Hissedilen.erimeArtisi(45, A), 1e-12);
        assertEquals(150, Hissedilen.ERIME_SINIRI / Hissedilen.erimeArtisi(45, A), 1e-9, "45 °C'de ~150 sn");
    }
}
