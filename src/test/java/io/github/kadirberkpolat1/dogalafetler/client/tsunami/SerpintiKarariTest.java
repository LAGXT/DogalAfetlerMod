package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.client.tsunami.SerpintiKarari.Sonuc;
import org.junit.jupiter.api.Test;

class SerpintiKarariTest {
    private static final double YOK = Double.NaN;

    @Test
    void kapsananNoktadaCizilenSuyunUstundeDogar() {
        assertEquals(Sonuc.DOGAR, SerpintiKarari.karar(70.1, 70.0, true, YOK, 64, false));
        assertEquals(Sonuc.DOGAR, SerpintiKarari.karar(70.2, 70.0, true, 74.0, 64, false), "analitik seviye kapsanan noktada sorulmaz");
    }

    @Test
    void kapsanmayanNoktadaSanalSuSeviyesininUstundeDogar() {
        assertEquals(Sonuc.DOGAR, SerpintiKarari.karar(68.1, YOK, false, 68.0, 63, false));
        assertEquals(68.0, SerpintiKarari.yuzey(false, YOK, 68.0));
        assertEquals(70.0, SerpintiKarari.yuzey(true, 70.0, 68.0));
    }

    @Test
    void agSuyuCizmediyseDogmaz() {
        assertEquals(Sonuc.CIZIMSIZ, SerpintiKarari.karar(70.1, YOK, true, 70.0, 64, false),
                "ağ dalgayı henüz çizmedi: analitik seviyeye düşmez");
        assertTrue(Double.isNaN(SerpintiKarari.yuzey(true, YOK, 70.0)));
    }

    @Test
    void sanalSuYoksaDogmaz() {
        assertEquals(Sonuc.SUSUZ, SerpintiKarari.karar(70.1, YOK, false, YOK, 64, false));
    }

    @Test
    void katiBlokHucresindeDogmaz() {
        assertEquals(Sonuc.BLOK_ICINDE, SerpintiKarari.karar(66.1, YOK, false, 66.0, 63, true), "tepe aralığında arazinin içi");
    }

    @Test
    void hareketEngelleyenTepeninAltindaDogmaz() {
        assertEquals(Sonuc.CATI_ALTI, SerpintiKarari.karar(66.1, 66.0, true, YOK, 70, false), "evin çatısı altında");
        assertEquals(Sonuc.CATI_ALTI, SerpintiKarari.karar(66.1, YOK, false, 66.0, 67, false), "ağaç yaprakları altında");
        assertEquals(Sonuc.DOGAR, SerpintiKarari.karar(66.1, YOK, false, 66.0, 66, false), "tepe su üstünün hücresinde");
    }

    @Test
    void gorunenSuyunCokUstundeDogmaz() {
        assertEquals(Sonuc.HAVADA, SerpintiKarari.karar(71.0, 70.0, true, YOK, 64, false));
        assertEquals(Sonuc.HAVADA, SerpintiKarari.karar(64.0 + SerpintiKarari.HAVA_PAYI + 0.01, YOK, false, 64.0, 60, false));
        assertEquals(Sonuc.DOGAR, SerpintiKarari.karar(64.0 + SerpintiKarari.HAVA_PAYI, YOK, false, 64.0, 60, false), "pay sınırı");
    }

    @Test
    void suYokluguBloktanVeCatidanOnceGelir() {
        assertEquals(Sonuc.CIZIMSIZ, SerpintiKarari.karar(66.1, YOK, true, YOK, 70, true));
        assertEquals(Sonuc.BLOK_ICINDE, SerpintiKarari.karar(66.1, 66.0, true, YOK, 70, true));
    }
}
