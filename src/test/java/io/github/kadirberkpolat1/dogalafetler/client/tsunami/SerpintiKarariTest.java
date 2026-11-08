package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.client.tsunami.SerpintiKarari.Sonuc;
import org.junit.jupiter.api.Test;

class SerpintiKarariTest {
    private static final double YOK = Double.NaN;

    private static Sonuc karar(double y, boolean yakinKapsar, double yakin, double uzak, int tepe, boolean kati) {
        return SerpintiKarari.karar(y, SerpintiKarari.yuzey(yakinKapsar, yakin, uzak), tepe, kati);
    }

    @Test
    void yakinAgdaCizilenSuyunUstundeDogar() {
        assertEquals(Sonuc.DOGAR, karar(70.1, true, 70.0, YOK, 64, false));
        assertEquals(Sonuc.DOGAR, karar(70.2, true, 70.0, 74.0, 64, false), "yakın ağın alanında uzak LOD sorulmaz");
    }

    @Test
    void yakinAgDisindaUzakLodunCizdigiKaroyaOturur() {
        assertEquals(Sonuc.DOGAR, karar(68.1, false, YOK, 68.0, 63, false));
        assertEquals(68.0, SerpintiKarari.yuzey(false, YOK, 68.0));
        assertEquals(70.0, SerpintiKarari.yuzey(true, 70.0, 68.0));
    }

    /** Denetim P1-1: yakın ağ boşken (yavaş makinede dalga yeni geldi) uzak LOD da kameranın yakın karesini çizmez: üretme. */
    @Test
    void yakinAgBoskenUretmez() {
        assertEquals(Sonuc.CIZIMSIZ, karar(70.1, false, YOK, YOK, 64, false));
        assertTrue(Double.isNaN(SerpintiKarari.yuzey(false, YOK, YOK)));
    }

    @Test
    void agSuyuCizmediyseDogmaz() {
        assertEquals(Sonuc.CIZIMSIZ, karar(70.1, true, YOK, 70.0, 64, false), "yakın ağ burada su çizmiyor: uzak LOD'a düşmez");
        assertTrue(Double.isNaN(SerpintiKarari.yuzey(true, YOK, 70.0)));
    }

    @Test
    void katiBlokHucresindeDogmaz() {
        assertEquals(Sonuc.BLOK_ICINDE, karar(66.1, false, YOK, 66.0, 63, true), "tepe aralığında arazinin içi");
    }

    @Test
    void hareketEngelleyenTepeninAltindaDogmaz() {
        assertEquals(Sonuc.CATI_ALTI, karar(66.1, true, 66.0, YOK, 70, false), "evin çatısı altında");
        assertEquals(Sonuc.CATI_ALTI, karar(66.1, false, YOK, 66.0, 67, false), "ağaç yaprakları altında");
        assertEquals(Sonuc.DOGAR, karar(66.1, false, YOK, 66.0, 66, false), "tepe su üstünün hücresinde");
    }

    @Test
    void gorunenSuyunCokUstundeDogmaz() {
        assertEquals(Sonuc.HAVADA, karar(71.0, true, 70.0, YOK, 64, false));
        assertEquals(Sonuc.HAVADA, karar(64.0 + SerpintiKarari.HAVA_PAYI + 0.01, false, YOK, 64.0, 60, false));
        assertEquals(Sonuc.DOGAR, karar(64.0 + SerpintiKarari.HAVA_PAYI, false, YOK, 64.0, 60, false), "pay sınırı");
    }

    @Test
    void suYokluguBloktanVeCatidanOnceGelir() {
        assertEquals(Sonuc.CIZIMSIZ, karar(66.1, true, YOK, YOK, 70, true));
        assertEquals(Sonuc.BLOK_ICINDE, karar(66.1, true, 66.0, YOK, 70, true));
    }
}
