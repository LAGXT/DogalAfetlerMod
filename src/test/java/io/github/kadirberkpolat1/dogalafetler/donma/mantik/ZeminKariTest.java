package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** Kar örtüsü iki yönlü dönüşümü: çimen (tek kat) ve doğal zemin donarken gelir, çözülürken kalkar, 0'da başlangıca döner. */
class ZeminKariTest {
    private final DonmaEsikleri e = DonmaEsikleri.VARSAYILAN.titresimsiz();

    private int yuru(boolean yapi, boolean tekKat, int mevcut, int eski, int yeni) {
        return ZeminKari.katman(e, yapi, tekKat, mevcut, yeni, Integer.signum(yeni - eski));
    }

    @Test
    void cimenDonarkenTekKatKarAlirCozulurkenKalkar() {
        assertEquals(0, yuru(false, true, 0, 0, 14), "ilk kar eşiğinden önce çimen çıplak");
        assertEquals(1, yuru(false, true, 0, 0, 15), "15'te karlı çimen: bir kat kar");
        assertEquals(1, yuru(false, true, 1, 15, 100), "çimende kar 100'de de tek kat");
        assertEquals(1, yuru(false, true, 1, 100, 55), "son kar 55'in altına kadar kalır");
        assertEquals(0, yuru(false, true, 1, 55, 54), "54'te kar kalkar, çimen karlı olmaz");
    }

    @Test
    void dogalZeminBesKataCikarVeSiraylaIner() {
        assertEquals(5, yuru(false, false, 0, 0, 100));
        assertEquals(3, yuru(false, false, 5, 100, 75));
        assertEquals(0, yuru(false, false, 3, 75, 40));
    }

    @Test
    void yonunTersineDegismez() {
        assertEquals(1, ZeminKari.katman(e, false, true, 1, 0, 1), "donarken azalmaz");
        assertEquals(0, ZeminKari.katman(e, false, true, 0, 100, -1), "çözülürken artmaz");
        assertEquals(2, ZeminKari.katman(e, false, false, 2, 100, 0), "yön yoksa dokunulmaz");
    }

    @Test
    void herYoldanSifirdaBaslangicaDoner() {
        Random r = new Random(13);
        for (int tur = 0; tur < 4; tur++) {
            boolean yapi = tur >= 2;
            boolean tekKat = tur % 2 == 0;
            for (int deneme = 0; deneme < 300; deneme++) {
                int seviye = 0;
                int katman = 0;
                for (int adim = 0; adim < 1 + r.nextInt(12); adim++) {
                    int yeni = r.nextInt(101);
                    katman = yuru(yapi, tekKat, katman, seviye, yeni);
                    assertTrue(katman >= 0 && katman <= (tekKat && !yapi ? 1 : 5), "katman sınır dışı: " + katman);
                    seviye = yeni;
                }
                assertEquals(0, yuru(yapi, tekKat, katman, seviye, 0), "0'da kar kalmamalı (yapı " + yapi + ")");
            }
        }
    }
}
