package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;

class OdulTavaniTest {
    private static final OdulZari.Sonuc IKISI = new OdulZari.Sonuc(true, true);
    private static final OdulZari.Sonuc PARCA = new OdulZari.Sonuc(true, false);

    @Test
    void kucukVeOrtaBantOdulVermez() {
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(PARCA, false, 0, 8));
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(IKISI, false, 0, 8));
        assertEquals(PARCA, OdulTavani.uygula(PARCA, true, 0, 8));
    }

    @Test
    void tavanaUlasincaKesilirIkiEsyaSigmazsaCekirdekKalir() {
        assertEquals(IKISI, OdulTavani.uygula(IKISI, true, 6, 8));
        assertEquals(new OdulZari.Sonuc(false, true), OdulTavani.uygula(IKISI, true, 7, 8));
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(PARCA, true, 8, 8));
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(PARCA, true, 0, 0), "tavan 0: ödül yok");
    }

    /** Varsayılan şanslarla 10 dk 180/dk yağmur (1800 meteor, %10 büyük): sınırsız ~58 eşya, sınırla en çok 8. */
    @Test
    void onDakikalikYagmurdaOncesiVeSonrasi() {
        Random r = new Random(20261107);
        int once = 0;
        int sonra = 0;
        for (int i = 0; i < 1800; i++) {
            boolean buyuk = MeteorOlcegi.buyuk(MeteorOlcegi.ornekle(r, 0.10, 0.30));
            OdulZari.Sonuc zar = OdulZari.at(buyuk, OdulZari.Sanslar.VARSAYILAN, r);
            once += OdulTavani.adet(zar);
            sonra += OdulTavani.adet(OdulTavani.uygula(zar, buyuk, sonra, 8));
        }
        assertEquals(true, once >= 40 && once <= 78, "sınırsız ödül " + once);
        assertEquals(true, sonra <= 8 && sonra < once, "sınırlı ödül " + sonra + " / " + once);
    }
}
