package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Madde 15: eşik, su üstü buz ve kızıl cüce dönemi. */
class AktiflikTest {
    @Test
    void esigiGecenDonmadaSuUstuBuzdaAktif() {
        assertTrue(Aktiflik.aktifMi(30, 30, false, true), "eşikte aktif");
        assertTrue(Aktiflik.aktifMi(80, 30, false, true));
        assertFalse(Aktiflik.aktifMi(29.9, 30, false, true), "eşiğin altında kapalı");
    }

    @Test
    void suUstuBuzOlmadanHicAktifDegil() {
        assertFalse(Aktiflik.aktifMi(100, 30, false, false));
        assertFalse(Aktiflik.aktifMi(100, 30, true, false), "kızıl cücede de buz şart");
    }

    @Test
    void kizilCuceDonemindeEsikAltindaDonmusYuzeydeSurer() {
        assertTrue(Aktiflik.aktifMi(10, 30, true, true), "kızıl cücede donmuş yüzey eşiksiz");
        assertTrue(Aktiflik.aktifMi(0, 30, true, true), "global ilerleme 0'a düşse de");
        assertFalse(Aktiflik.aktifMi(10, 30, false, true), "süpernovadan önce doğal buz açılmaz");
    }

    @Test
    void esikSifirsaHerBuzdaAktif() {
        assertTrue(Aktiflik.aktifMi(0, 0, false, true));
        assertFalse(Aktiflik.aktifMi(0, 0, false, false));
    }
}
