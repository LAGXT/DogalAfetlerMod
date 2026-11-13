package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Esikler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import org.junit.jupiter.api.Test;

class CorbaTakibiTest {
    @Test
    void donarkenSicakYiyecekVeNotreDonusBirKezSayilir() {
        CorbaTakibi t = new CorbaTakibi();
        t.yedi(100, true, true, 1800);
        assertFalse(t.kontrol(120, false), "henüz nötr değil");
        assertTrue(t.kontrol(900, true), "etki sürerken nötre döndü");
        assertFalse(t.kontrol(920, true), "bir kez");
    }

    @Test
    void donmuyorkenYenenSayilmaz() {
        CorbaTakibi t = new CorbaTakibi();
        t.yedi(100, false, true, 1800);
        assertFalse(t.kontrol(120, true), "N16: donma eşiğinin altındayken yemek gerekir");
    }

    @Test
    void donmaDurumuYalnizDonmaHasariKademesi() {
        for (Kademe k : Kademe.values()) {
            assertEquals(k == Kademe.DONUYOR, CorbaTakibi.donmaDurumunda(k), k.name());
        }
        Esikler e = Esikler.VARSAYILAN;
        assertFalse(CorbaTakibi.donmaDurumunda(e.kademe(-31)), "−31 (35,45 °C) üşüme: yavaşlama, donma hasarı değil");
        assertFalse(CorbaTakibi.donmaDurumunda(e.kademe(-60)), "−60 (34,0 °C) donma: buz kenarı, hasar kademesi değil");
        assertTrue(CorbaTakibi.donmaDurumunda(e.kademe(-76)), "−76 (33,2 °C) donma hasarı kademesi");
    }

    @Test
    void soguktaKizarmisPatatesAlinmazDonarkenAlinir() {
        Esikler e = Esikler.VARSAYILAN;
        CorbaTakibi t = new CorbaTakibi();
        // −31'de vanilla pişmiş yiyecek (küçük, 30 sn ısınma): nötre hemen dönse de sayılmaz (inceleme A1).
        t.yedi(100, CorbaTakibi.donmaDurumunda(e.kademe(-31)), false, 600);
        assertFalse(t.teyitBekliyor(101), "donmuyorken aday bile olmaz");
        assertFalse(t.kontrol(120, true));
        t.yedi(200, CorbaTakibi.donmaDurumunda(e.kademe(-80)), true, 3600);
        assertFalse(t.kontrol(220, false));
        assertTrue(t.kontrol(3000, true), "donarken sıcak yiyecek, etkisi sürerken nötr");
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
