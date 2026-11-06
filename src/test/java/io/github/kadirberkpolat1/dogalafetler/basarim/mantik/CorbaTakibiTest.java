package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CorbaTakibiTest {
    @Test
    void soguktaSicakYiyecekVeNotreDonusBirKezSayilir() {
        CorbaTakibi t = new CorbaTakibi();
        t.yedi(100, true, true, 1800);
        assertFalse(t.kontrol(120, false), "henüz nötr değil");
        assertTrue(t.kontrol(900, true), "etki sürerken nötre döndü");
        assertFalse(t.kontrol(920, true), "bir kez");
    }

    @Test
    void sogukDegilkenYenenSayilmaz() {
        CorbaTakibi t = new CorbaTakibi();
        t.yedi(100, false, true, 1800);
        assertFalse(t.kontrol(120, true), "N16: donma eşiğinin altındayken yemek gerekir");
    }

    @Test
    void etkiBitinceSayilmaz() {
        CorbaTakibi t = new CorbaTakibi();
        t.yedi(100, true, true, 600);
        assertFalse(t.kontrol(100 + 601, true), "etki bittikten sonra nötr sayılmaz");
        assertFalse(t.kontrol(800, true));
    }

    @Test
    void taninmayanYiyecekIsinmaGozlenirseSayilir() {
        CorbaTakibi t = new CorbaTakibi();
        t.yedi(100, true, false, 0);
        t.isinmaGozlendi(120, 1500);
        assertTrue(t.kontrol(140, true));
    }

    @Test
    void gecGozlenenIsinmaSayilmaz() {
        CorbaTakibi t = new CorbaTakibi();
        t.yedi(100, true, false, 0);
        t.isinmaGozlendi(100 + CorbaTakibi.ADAY_PENCERESI + 1, 1500);
        assertFalse(t.kontrol(200, true), "yemekle ilgisiz ısınma sayılmaz");
    }

    @Test
    void taninanYiyecekteSureVerilmezseVarsayilanPencere() {
        CorbaTakibi t = new CorbaTakibi();
        t.yedi(0, true, true, 0);
        assertTrue(t.kontrol(CorbaTakibi.VARSAYILAN_SURE, true));
    }
}
