package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import org.junit.jupiter.api.Test;

class AyEgrileriTest {
    private static final float EPS = 1e-5f;

    @Test
    void yaklasmaSirenSuresiyleAyni() {
        assertEquals(SirenTuru.sureTick(AfetTuru.AY_CARPMASI), Math.round(AyEgrileri.YAKLASMA_SN * 20),
                "istemci yaklaşmayı siren süresinden çizer (K0-19 rev. K-E67: 25 sn)");
    }

    /** K-E67: kısa yaklaşmada da evreler okunur kalır ve sırası korunur. */
    @Test
    void evrelerSiraliVeOkunur() {
        float son = AyEgrileri.YAKLASMA_SN - AyEgrileri.SON_YAKLASMA_SN;
        assertTrue(son >= 3f, "son yaklaşma en az 3 sn: " + son);
        float ucuncu = AyEgrileri.PARCALANMA_SN + 2 * AyEgrileri.CATLAK_ARALIGI_SN;
        assertTrue(AyEgrileri.PARCALANMA_SN >= 5f, "parçalanmadan önce süzülen ay görünür");
        assertTrue(ucuncu + AyEgrileri.CATLAK_BELIRME_SN <= AyEgrileri.SON_YAKLASMA_SN + 1f,
                "üçüncü çatlak son yaklaşmayla tamamlanır");
        assertTrue(AyEgrileri.CATLAK_BELIRME_SN < AyEgrileri.CATLAK_ARALIGI_SN, "çatlaklar tek tek belirir");
    }

    @Test
    void ayBirdenOnDortKataMonotonBuyurVeUfkaIner() {
        float yaklasma = AyEgrileri.YAKLASMA_SN;
        assertEquals(1f, AyEgrileri.olcek(0f), EPS, "vanilla boyda başlar");
        assertEquals(14f, AyEgrileri.olcek(yaklasma), EPS, "tasarım: 14x");
        assertEquals(1f + 13f * 0.125f, AyEgrileri.olcek(yaklasma / 2), EPS, "s = 1 + 13t³");
        assertEquals(35f, AyEgrileri.egim(0f), EPS);
        assertEquals(35f, AyEgrileri.egim(AyEgrileri.SON_YAKLASMA_SN), EPS, "son yaklaşmaya kadar 35°");
        assertEquals(AyEgrileri.SON_EGIM, AyEgrileri.egim(yaklasma), EPS);
        float olcek = 0f;
        float egim = 90f;
        float yay = -1f;
        float kiz = -1f;
        float[] catlak = {-1f, -1f, -1f};
        for (float s = 0f; s <= yaklasma; s += 0.05f) {
            assertTrue(AyEgrileri.olcek(s) >= olcek, "ölçek azalmamalı");
            assertTrue(AyEgrileri.egim(s) <= egim, "eğim artmamalı");
            assertTrue(AyEgrileri.parcaYayilmasi(s) >= yay, "parça yayı daralmamalı");
            assertTrue(AyEgrileri.kizarma(s) >= kiz, "kızarma azalmamalı");
            olcek = AyEgrileri.olcek(s);
            egim = AyEgrileri.egim(s);
            yay = AyEgrileri.parcaYayilmasi(s);
            kiz = AyEgrileri.kizarma(s);
            for (int i = 0; i < 3; i++) {
                float c = AyEgrileri.catlak(i + 1, s);
                assertTrue(c >= catlak[i], "çatlak " + (i + 1) + " sönmemeli");
                catlak[i] = c;
            }
        }
        assertEquals(0f, AyEgrileri.parcaYayilmasi(AyEgrileri.PARCALANMA_SN - 0.1f), EPS, "parçalanma 8. sn'de başlar");
        assertEquals(0f, AyEgrileri.catlak(1, AyEgrileri.PARCALANMA_SN), EPS);
        assertEquals(1f, AyEgrileri.catlak(3, yaklasma), EPS, "son aşama çarpmadan önce tam");
        assertEquals(0f, AyEgrileri.kizarma(AyEgrileri.SON_YAKLASMA_SN), EPS);
        assertEquals(1f, AyEgrileri.kizarma(yaklasma), EPS);
    }

    /** K-E83: ay olay başladığı anda vanilla ayın bulunduğu yükseklikten iner; özgün 35° yolu aynen korunur. */
    @Test
    void ayBaslangicYuksekligindenInerOzgunYolKorunur() {
        for (float s = 0f; s <= AyEgrileri.YAKLASMA_SN; s += 0.25f) {
            assertEquals(AyEgrileri.egim(s), AyEgrileri.egim(s, AyEgrileri.YAKLASMA_EGIMI), EPS, "35° başlangıç özgün yol");
        }
        assertEquals(4f, AyEgrileri.sonEgim(35f), EPS);
        assertEquals(4f, AyEgrileri.sonEgim(20f), EPS, "alçak ay 4°'nin altına inmez");
        assertEquals(29f, AyEgrileri.sonEgim(60f), EPS, "yüksek ay en çok 31° iner");
        assertEquals(9f, AyEgrileri.patlamaEgimi(35f), EPS, "özgün patlama yüksekliği");
        assertEquals(63f, AyEgrileri.patlamaEgimi(89f), EPS, "tepeden gelen ay tepeye yakın patlar");
        float tepe = AyEgrileri.EN_COK_BASLANGIC_EGIMI;
        assertEquals(tepe, AyEgrileri.egim(0f, tepe), EPS);
        assertEquals(tepe, AyEgrileri.egim(AyEgrileri.SON_YAKLASMA_SN, tepe), EPS, "son yaklaşmaya kadar yerinde");
        assertEquals(AyEgrileri.sonEgim(tepe), AyEgrileri.egim(AyEgrileri.YAKLASMA_SN, tepe), EPS);
        float onceki = 90f;
        for (float s = 0f; s <= AyEgrileri.YAKLASMA_SN; s += 0.05f) {
            float e = AyEgrileri.egim(s, tepe);
            assertTrue(e <= onceki, "eğim artmamalı (tepe başlangıcı)");
            onceki = e;
        }
    }

    @Test
    void parlamaYarimSaniyeYuzdeYetmisSonraSoner() {
        assertEquals(0f, AyEgrileri.parlama(-0.1f), EPS, "çarpmadan önce parlama yok");
        assertEquals(0.7f, AyEgrileri.parlama(0f), EPS);
        assertEquals(0.7f, AyEgrileri.parlama(0.5f), EPS, "0,5 sn %70");
        assertEquals(0f, AyEgrileri.parlama(0.75f), EPS, "0,25 sn'de söner");
        assertTrue(AyEgrileri.dunyaParlamasi(0.3f) > 0.9f, "dünya parlaması ilk yarım saniyede tam");
        assertTrue(AyEgrileri.dunyaParlamasi(2f) < AyEgrileri.dunyaParlamasi(1f), "dünya parlaması söner");
        assertEquals(0f, AyEgrileri.dunyaParlamasi(4.5f), EPS, "birkaç saniyede biter");
    }

    @Test
    void sarsintiUcUnSaniyedenOnBeseKadar() {
        assertEquals(0f, AyEgrileri.sarsinti(2.9f), EPS);
        assertEquals(0f, AyEgrileri.sarsinti(15.1f), EPS);
        assertEquals(1f, AyEgrileri.sarsinti(6f), EPS, "pencere ortasında tam genlik");
        assertEquals(12f, AyEgrileri.SARSINTI_SON_SN - AyEgrileri.SARSINTI_BAS_SN, EPS, "tasarım: 12 sn");
        for (float s = 3f; s <= 15f; s += 0.1f) {
            float a = AyEgrileri.sarsinti(s);
            assertTrue(a >= 0f && a <= 1f, "genlik 0–1");
        }
    }

    @Test
    void patlamaBuyurSonerYediSaniyedeKaybolur() {
        assertTrue(AyEgrileri.patlamaGorunur(0f) && !AyEgrileri.patlamaGorunur(-0.1f));
        assertTrue(AyEgrileri.atesTopuAlfa(0.3f) > 0.9f, "0 sn: ateş topu büyüyor");
        assertTrue(AyEgrileri.patlamaBuyume(0.3f) < AyEgrileri.patlamaBuyume(2f), "genişler");
        float iki = AyEgrileri.atesTopuAlfa(2f);
        assertTrue(iki > 0.2f && iki < 0.9f, "2 sn: sönüyor (" + iki + ")");
        for (float s = AyEgrileri.PATLAMA_SN; s < 20000f; s += 37f) {
            assertFalse(AyEgrileri.patlamaGorunur(s), "ömür dolunca çizilmez");
            assertEquals(0f, AyEgrileri.atesTopuAlfa(s) + AyEgrileri.kabukAlfa(s) + AyEgrileri.enkazAlfa(s)
                    + AyEgrileri.dumanAlfa(s) + AyEgrileri.dunyaParlamasi(s), EPS, "8 sn ve sonrası: yok");
        }
        assertTrue(AyEgrileri.PATLAMA_SN < 8f, "8. sn karesinde patlama yok");
        float onceki = 2f;
        for (float s = 1f; s < AyEgrileri.PATLAMA_SN; s += 0.05f) {
            float a = AyEgrileri.atesTopuAlfa(s);
            assertTrue(a <= onceki + EPS, "1 sn'den sonra ateş topu monoton söner");
            onceki = a;
        }
        assertFalse(AyEgrileri.carpmaIzleriSuruyor(10f, false), "halka kapalıyken patlamadan sonra iz yok");
    }

    @Test
    void halkaUcGundeAltidaAltidanSifiraIner() {
        assertEquals(0f, AyEgrileri.halkaAlfa(10f), EPS, "çarpmadan hemen sonra halka yok");
        assertEquals(0.6f * (1f - 80f / 3600f), AyEgrileri.halkaAlfa(80f), EPS);
        assertEquals(0f, AyEgrileri.halkaAlfa(3600f), EPS, "3 gün sonra yalnız yıldız, galaksi, nebula");
        assertTrue(AyEgrileri.carpmaIzleriSuruyor(2000f, true), "halka açıkken 2. günde de gök çizimi sürer");
        assertFalse(AyEgrileri.carpmaIzleriSuruyor(2000f, false), "halka kapalıyken patlama söndükten sonra iz yok");
        assertFalse(AyEgrileri.carpmaIzleriSuruyor(3600f, true));
    }

    @Test
    void kaliteTablosu() {
        assertEquals(15, AyEgrileri.parcaSayisi(0));
        assertEquals(40, AyEgrileri.parcaSayisi(2));
        assertEquals(70, AyEgrileri.parcaSayisi(3));
        assertEquals(12, AyEgrileri.enkazSayisi(0));
        assertEquals(64, AyEgrileri.enkazSayisi(3));
        assertTrue(AyEgrileri.kureDilimi(0) < AyEgrileri.kureDilimi(3) && AyEgrileri.dumanSayisi(0) < AyEgrileri.dumanSayisi(3));
    }

    @Test
    void ayDurumuOlaySaatinden() {
        assertEquals(AyDurumu.VAR, AyDurumu.den(true, AyDurumu.OLAY_YOK));
        assertEquals(AyDurumu.YAKLASIYOR, AyDurumu.den(true, 0L));
        assertEquals(AyDurumu.YOK, AyDurumu.den(false, 1234L));
        assertEquals(AyDurumu.YOK, AyDurumu.den(false, AyDurumu.OLAY_YOK), "elle kaldırılan ay");
    }
}
