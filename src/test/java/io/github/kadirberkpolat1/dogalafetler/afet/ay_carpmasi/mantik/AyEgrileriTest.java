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
                "istemci yaklaşmayı siren süresinden çizer (K0-19: 60 sn)");
    }

    @Test
    void ayBirdenOnDortKataMonotonBuyurVeUfkaIner() {
        assertEquals(1f, AyEgrileri.olcek(0f), EPS, "vanilla boyda başlar");
        assertEquals(14f, AyEgrileri.olcek(60f), EPS, "tasarım: 14x");
        assertEquals(1f + 13f * 0.125f, AyEgrileri.olcek(30f), EPS, "s = 1 + 13t³");
        assertEquals(35f, AyEgrileri.egim(0f), EPS);
        assertEquals(35f, AyEgrileri.egim(50f), EPS, "son yaklaşmaya kadar 35°");
        assertEquals(AyEgrileri.SON_EGIM, AyEgrileri.egim(60f), EPS);
        float olcek = 0f;
        float egim = 90f;
        float yay = -1f;
        float kiz = -1f;
        float[] catlak = {-1f, -1f, -1f};
        for (float s = 0f; s <= 60f; s += 0.05f) {
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
        assertEquals(0f, AyEgrileri.parcaYayilmasi(19.9f), EPS, "parçalanma 20. sn'de başlar");
        assertEquals(0f, AyEgrileri.catlak(1, 20f), EPS);
        assertEquals(1f, AyEgrileri.catlak(3, 60f), EPS, "son aşama çarpmadan önce tam");
        assertEquals(0f, AyEgrileri.kizarma(50f), EPS);
        assertEquals(1f, AyEgrileri.kizarma(60f), EPS);
    }

    @Test
    void parlamaYarimSaniyeYuzdeYetmisSonraSoner() {
        assertEquals(0f, AyEgrileri.parlama(-0.1f), EPS, "çarpmadan önce parlama yok");
        assertEquals(0.7f, AyEgrileri.parlama(0f), EPS);
        assertEquals(0.7f, AyEgrileri.parlama(0.5f), EPS, "0,5 sn %70");
        assertEquals(0f, AyEgrileri.parlama(0.75f), EPS, "0,25 sn'de söner");
        assertTrue(AyEgrileri.ufukIsigi(0f) > AyEgrileri.ufukIsigi(10f), "ufuk ışığı söner");
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
    void mantarYirmiBesDereceyeBuyurBirGundeSoner() {
        assertEquals(0f, AyEgrileri.mantarYuksekligi(2f), EPS, "2 sn sonra yükselir");
        assertEquals(25f, AyEgrileri.mantarYuksekligi(47f), EPS, "45 sn'de 25°");
        assertEquals(0f, AyEgrileri.mantarAlfa(1f), EPS);
        assertEquals(1f, AyEgrileri.mantarAlfa(60f), EPS);
        assertEquals(0.35f, AyEgrileri.mantarAlfa(300f), EPS, "kalıntı");
        assertEquals(0f, AyEgrileri.mantarAlfa(AyEgrileri.MANTAR_SONUM_SN), EPS, "bir günde söner");
        float onceki = 2f;
        for (float s = 90f; s < AyEgrileri.MANTAR_SONUM_SN; s += 1f) {
            float a = AyEgrileri.mantarAlfa(s);
            assertTrue(a <= onceki + EPS, "90 sn'den sonra mantar söner");
            onceki = a;
        }
    }

    @Test
    void halkaUcGundeAltidaAltidanSifiraIner() {
        assertEquals(0f, AyEgrileri.halkaAlfa(10f), EPS, "çarpmadan hemen sonra halka yok");
        assertEquals(0.6f * (1f - 80f / 3600f), AyEgrileri.halkaAlfa(80f), EPS);
        assertEquals(0f, AyEgrileri.halkaAlfa(3600f), EPS, "3 gün sonra yalnız yıldız, galaksi, nebula");
        assertTrue(AyEgrileri.carpmaIzleriSuruyor(2000f, true), "halka açıkken 2. günde de gök çizimi sürer");
        assertFalse(AyEgrileri.carpmaIzleriSuruyor(2000f, false), "halka kapalıyken mantar söndükten sonra iz yok");
        assertFalse(AyEgrileri.carpmaIzleriSuruyor(3600f, true));
    }

    @Test
    void kaliteTablosu() {
        assertEquals(15, AyEgrileri.parcaSayisi(0));
        assertEquals(40, AyEgrileri.parcaSayisi(2));
        assertEquals(70, AyEgrileri.parcaSayisi(3));
        assertEquals(30, AyEgrileri.mantarSayisi(0));
        assertEquals(180, AyEgrileri.mantarSayisi(3));
    }

    @Test
    void ayDurumuOlaySaatinden() {
        assertEquals(AyDurumu.VAR, AyDurumu.den(true, AyDurumu.OLAY_YOK));
        assertEquals(AyDurumu.YAKLASIYOR, AyDurumu.den(true, 0L));
        assertEquals(AyDurumu.YOK, AyDurumu.den(false, 1234L));
        assertEquals(AyDurumu.YOK, AyDurumu.den(false, AyDurumu.OLAY_YOK), "elle kaldırılan ay");
    }
}
