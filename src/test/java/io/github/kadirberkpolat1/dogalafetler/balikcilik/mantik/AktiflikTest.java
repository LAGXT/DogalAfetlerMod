package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Madde 15: eşik, su üstü buz ve kızıl cüce dönemi. */
class AktiflikTest {
    @Test
    void esigiGecenDonmadaSuUstuBuzdaAktif() {
        assertTrue(Aktiflik.aktifMi(30, 30, false, 0, true), "eşikte aktif");
        assertTrue(Aktiflik.aktifMi(80, 30, false, 0, true));
        assertFalse(Aktiflik.aktifMi(29.9, 30, false, 0, true), "eşiğin altında kapalı");
    }

    @Test
    void suUstuBuzOlmadanHicAktifDegil() {
        assertFalse(Aktiflik.aktifMi(100, 30, false, 1, false));
        assertFalse(Aktiflik.aktifMi(100, 30, true, 1, false), "kızıl cücede de buz şart");
    }

    @Test
    void kizilCuceDonemindeDusukIlerlemedeKisBolgesindeVeDonmusYuzeydeSurer() {
        assertTrue(Aktiflik.aktifMi(10, 30, true, 1, true), "kalıcı kış bölgesi");
        assertTrue(Aktiflik.aktifMi(10, 30, true, 0, true), "hâlâ donmuş su yüzeyi");
        assertFalse(Aktiflik.aktifMi(10, 30, false, 1, true), "süpernovadan önce kış ağırlığı tek başına açmaz");
    }

    @Test
    void kisBolgesiEsigiYarim() {
        assertTrue(Aktiflik.kisBolgesiMi(0.5));
        assertFalse(Aktiflik.kisBolgesiMi(0.49));
    }
}
