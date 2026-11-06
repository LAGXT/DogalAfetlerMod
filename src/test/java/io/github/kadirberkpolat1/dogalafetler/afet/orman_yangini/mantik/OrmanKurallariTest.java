package io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik.OrmanKurallari.Kare;
import io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik.OrmanKurallari.Seviye;
import io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik.OrmanKurallari.Tablo;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrmanKurallariTest {
    @Test
    void seviyeArttikcaBaslangicAlaniYayilmaVeSureArtar() {
        Seviye onceki = null;
        for (int s = 1; s <= 5; s++) {
            Seviye v = Tablo.VARSAYILAN.seviye(s);
            if (onceki != null) {
                assertTrue(v.yaricap() > onceki.yaricap(), "başlangıç alanı S" + s);
                assertTrue(v.yogunluk() > onceki.yogunluk(), "yayılma S" + s);
                assertTrue(v.sureTick() > onceki.sureTick(), "süre S" + s);
            }
            onceki = v;
        }
        assertEquals(new Seviye(3, 0.0, 2400), Tablo.VARSAYILAN.seviye(1));
        assertEquals(new Seviye(24, 1.0, 9600), Tablo.VARSAYILAN.seviye(5));
        assertEquals(Tablo.VARSAYILAN.seviye(5), Tablo.VARSAYILAN.seviye(7));
    }

    @Test
    void yogunlukConfigteTasarsaKirpilir() {
        Tablo t = new Tablo(List.of(3, 6, 10, 16, 24), List.of(-1.0, 0.2, 0.4, 0.7, 3.0), List.of(1, 2, 3, 4, 5));
        assertEquals(0.0, t.seviye(1).yogunluk());
        assertEquals(1.0, t.seviye(5).yogunluk());
    }

    @Test
    void etkiAlaniYanginlaBuyurKuculmezTavanliDir() {
        double a = OrmanKurallari.alanYaricapi(0, 0, 10);
        assertEquals(10 + OrmanKurallari.ALAN_PAYI, a);
        double b = OrmanKurallari.alanYaricapi(a, 60, 10);
        assertEquals(60 + OrmanKurallari.ALAN_PAYI, b);
        assertEquals(b, OrmanKurallari.alanYaricapi(b, 5, 10), "yangın sönse de alan küçülmez");
        assertEquals(OrmanKurallari.EN_BUYUK_ALAN, OrmanKurallari.alanYaricapi(b, 10_000, 10));
    }

    @Test
    void sicakKarelerKalabaliktanAzaSiralanirVeTavanlanir() {
        List<Kare> kareler = List.of(new Kare(16, 16, 3), new Kare(48, 16, 40), new Kare(-16, 80, 9));
        assertArrayEquals(new int[] {48, 16, 40, -16, 80, 9}, OrmanKurallari.sicakKareler(kareler, 2));
        assertEquals(9, OrmanKurallari.sicakKareler(kareler, 64).length);
        assertEquals(0, OrmanKurallari.sicakKareler(List.of(), 8).length);
    }

    @Test
    void enYakinKareKenarinaUzaklik() {
        int[] kareler = {16, 16, 5, 112, 16, 9};
        assertEquals(0, OrmanKurallari.enYakin(kareler, 20, 10), 1e-9, "karenin içinde");
        assertEquals(10, OrmanKurallari.enYakin(kareler, 42, 16), 1e-9, "kenardan 10 blok");
        assertEquals(Double.POSITIVE_INFINITY, OrmanKurallari.enYakin(new int[0], 0, 0));
    }

    @Test
    void yakinlikAgirligiMenzildeSifir() {
        assertEquals(1, OrmanKurallari.yakinlikAgirligi(0, 48), 1e-9);
        assertEquals(0.5, OrmanKurallari.yakinlikAgirligi(24, 48), 1e-9);
        assertEquals(0, OrmanKurallari.yakinlikAgirligi(48, 48), 1e-9);
        assertEquals(0, OrmanKurallari.yakinlikAgirligi(Double.POSITIVE_INFINITY, 48), 1e-9);
    }

    @Test
    void kareUzakligiKaredekiEnUzakHucreyiKapsar() {
        double d = OrmanKurallari.kareUzakligi(0, 0);
        assertTrue(d >= Math.hypot(16, 16) - 1e-9);
    }

    @Test
    void erkenBitisEsikteVeKapatilabilir() {
        assertFalse(OrmanKurallari.sondu(14, 15));
        assertTrue(OrmanKurallari.sondu(15, 15));
        assertFalse(OrmanKurallari.sondu(1_000, 0), "0 erken bitişi kapatır");
    }
}
