package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;
import org.junit.jupiter.api.Test;

class OdulTavaniTest {
    private static final OdulZari.Sonuc IKISI = new OdulZari.Sonuc(true, true);
    private static final OdulZari.Sonuc PARCA = new OdulZari.Sonuc(true, false);
    /** Üç parça (1 + 2 ek). */
    private static final OdulZari.Sonuc UC_PARCA = new OdulZari.Sonuc(true, false, 2);
    /** Üç parça ve bir çekirdek: dört eşya. */
    private static final OdulZari.Sonuc BOL = new OdulZari.Sonuc(true, true, 2);

    @Test
    void kucukVeOrtaBantOdulVermez() {
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(PARCA, false, 0, 8));
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(IKISI, false, 0, 8));
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(BOL, false, 0, 8));
        assertEquals(PARCA, OdulTavani.uygula(PARCA, true, 0, 8));
        assertEquals(BOL, OdulTavani.uygula(BOL, true, 0, 8));
    }

    @Test
    void tavanaUlasincaKesilirIkiEsyaSigmazsaCekirdekKalir() {
        assertEquals(IKISI, OdulTavani.uygula(IKISI, true, 6, 8));
        assertEquals(new OdulZari.Sonuc(false, true), OdulTavani.uygula(IKISI, true, 7, 8));
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(PARCA, true, 8, 8));
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(PARCA, true, 0, 0), "tavan 0: ödül yok");
    }

    /** Ek parçalar tavana sığmazsa önce onlar atılır; tavan hiç aşılmaz, çekirdek ilk sığar. */
    @Test
    void ekParcalarTavanaGoreKirpilir() {
        assertEquals(new OdulZari.Sonuc(true, false, 1), OdulTavani.uygula(UC_PARCA, true, 6, 8));
        assertEquals(PARCA, OdulTavani.uygula(UC_PARCA, true, 7, 8));
        assertEquals(new OdulZari.Sonuc(true, true, 0), OdulTavani.uygula(BOL, true, 6, 8));
        assertEquals(new OdulZari.Sonuc(false, true), OdulTavani.uygula(BOL, true, 7, 8));
        assertEquals(new OdulZari.Sonuc(true, true, 1), OdulTavani.uygula(BOL, true, 5, 8));
        assertEquals(BOL, OdulTavani.uygula(BOL, true, 4, 8));
        assertEquals(OdulZari.Sonuc.YOK, OdulTavani.uygula(BOL, true, 8, 8));
    }

    /**
     * Varsayılan şanslarla 10 dk 180/dk yağmur (1800 meteor, %10 büyük): sınırsız ~77 eşya (küçük/orta ~49 + büyük ~28;
     * K-E74 öncesi ~58), sınırla en çok 8 (tavan aynı).
     */
    @Test
    void onDakikalikYagmurdaOncesiVeSonrasi() {
        Random r = new Random(20261107);
        int once = 0;
        int sonra = 0;
        for (int i = 0; i < 1800; i++) {
            boolean buyuk = MeteorOlcegi.buyuk(MeteorOlcegi.ornekle(r, 0.10, 0.30));
            OdulZari.Sonuc zar = OdulZari.at(buyuk, OdulZari.Sanslar.VARSAYILAN, r);
            once += zar.adet();
            sonra += OdulTavani.uygula(zar, buyuk, sonra, 8).adet();
        }
        assertEquals(true, once >= 40 && once <= 120, "sınırsız ödül " + once);
        assertEquals(true, sonra <= 8 && sonra < once, "sınırlı ödül " + sonra + " / " + once);
    }
}
