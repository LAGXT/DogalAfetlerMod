package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class DonusumTablosuTest {
    private static DonusumTablosu<String> tablo() {
        DonusumTablosu<String> t = new DonusumTablosu<>();
        t.ekle("su", "buz", 20);
        t.ekle("buz", "sikisik_buz", 90);
        t.ekle("camur", "kuru_camur", 60);
        return t;
    }

    @Test
    void dondurSonraCozBaslangicDurumunaDoner() {
        DonusumTablosu<String> t = tablo();
        for (String kok : new String[] {"su", "camur", "tas"}) {
            assertEquals(kok, t.durum(t.durum(kok, 100), 0), kok);
        }
    }

    @Test
    void sonDurumYalnizSonSeviyeyeBaglidir() {
        DonusumTablosu<String> t = tablo();
        Random r = new Random(11);
        for (String kok : new String[] {"su", "camur", "tas"}) {
            for (int deneme = 0; deneme < 200; deneme++) {
                String mevcut = kok;
                int son = 0;
                int adim = 1 + r.nextInt(8);
                for (int i = 0; i < adim; i++) {
                    son = r.nextInt(101);
                    mevcut = t.durum(mevcut, son);
                }
                assertEquals(t.durum(kok, son), mevcut, kok + " son seviye " + son);
            }
        }
    }

    @Test
    void esiklerZincirBoyuncaUygulanir() {
        DonusumTablosu<String> t = tablo();
        assertEquals("su", t.durum("su", 19));
        assertEquals("buz", t.durum("su", 20));
        assertEquals("sikisik_buz", t.durum("su", 90));
        assertEquals("buz", t.durum("sikisik_buz", 50));
        assertEquals("su", t.durum("sikisik_buz", 0));
        assertEquals("su", t.kok("sikisik_buz"));
    }

    @Test
    void taninmayanBlokDokunulmaz() {
        DonusumTablosu<String> t = tablo();
        assertFalse(t.taniyor("tas"));
        assertTrue(t.taniyor("buz"));
        for (int seviye = 0; seviye <= 100; seviye += 10) {
            assertEquals("tas", t.durum("tas", seviye));
        }
    }

    @Test
    void ayniKaynakIkiKezReddedilir() {
        DonusumTablosu<String> t = tablo();
        assertThrows(IllegalArgumentException.class, () -> t.ekle("su", "kar", 30));
    }

    @Test
    void ayniHedefIkiKezReddedilir() {
        DonusumTablosu<String> t = tablo();
        assertThrows(IllegalArgumentException.class, () -> t.ekle("lav", "buz", 30));
    }

    @Test
    void donguVeKendineDonusumReddedilir() {
        DonusumTablosu<String> t = tablo();
        assertThrows(IllegalArgumentException.class, () -> t.ekle("sikisik_buz", "su", 95));
        assertThrows(IllegalArgumentException.class, () -> t.ekle("tas", "tas", 10));
    }

    @Test
    void zincirdeEsikArtmali() {
        DonusumTablosu<String> t = new DonusumTablosu<>();
        t.ekle("a", "b", 50);
        assertThrows(IllegalArgumentException.class, () -> t.ekle("b", "c", 40));
        assertThrows(IllegalArgumentException.class, () -> t.ekle("z", "a", 60));
    }

    @Test
    void donarkenGeriCozmezCozulurkenIleriDondurmez() {
        DonusumTablosu<String> t = tablo();
        assertEquals("buz", t.durum("buz", 0, 1), "donma yönünde doğal buz korunur");
        assertEquals("sikisik_buz", t.durum("su", 95, 1));
        assertEquals("su", t.durum("su", 50, -1), "çözülme yönünde yeni donma olmaz");
        assertEquals("buz", t.durum("sikisik_buz", 50, -1));
        assertEquals("su", t.durum("sikisik_buz", 0, -1));
        assertEquals("buz", t.durum("buz", 95, 0), "yön 0 dokunmaz");
        assertEquals("tas", t.durum("tas", 100, 1));
    }

    @Test
    void yonluYoldaSifiraCozmekKokuVerir() {
        DonusumTablosu<String> t = tablo();
        Random r = new Random(17);
        for (String kok : new String[] {"su", "camur", "tas"}) {
            for (int deneme = 0; deneme < 200; deneme++) {
                String mevcut = kok;
                int onceki = 0;
                for (int i = 0; i < 1 + r.nextInt(8); i++) {
                    int yeni = r.nextInt(101);
                    mevcut = t.durum(mevcut, yeni, Integer.signum(yeni - onceki));
                    onceki = yeni;
                }
                assertEquals(kok, t.durum(mevcut, 0, -1), kok);
            }
        }
    }

    @Test
    void cozulmeEsigiDonmaEsigindenYuksekHisterezis() {
        DonusumTablosu<String> t = new DonusumTablosu<>();
        t.ekle("cimen", "kiragili", 10, 50);
        assertEquals("kiragili", t.durum("cimen", 30, 1));
        assertEquals("kiragili", t.durum("kiragili", 55, -1), "çözülme eşiğinin üstünde kalır");
        assertEquals("cimen", t.durum("kiragili", 45, -1), "çözülmede 50'nin altında kalkar");
        assertEquals("cimen", t.durum("cimen", 45, -1));
        assertEquals("cimen", t.durum("kiragili", 0, -1));
    }

    @Test
    void cozulmeEsigiDonmaEsigindenKucukReddedilir() {
        DonusumTablosu<String> t = new DonusumTablosu<>();
        assertThrows(IllegalArgumentException.class, () -> t.ekle("a", "b", 50, 40));
        t.ekle("a", "b", 20, 60);
        assertThrows(IllegalArgumentException.class, () -> t.ekle("b", "c", 30, 50), "zincirde çözülme eşiği de artmalı");
    }
}
