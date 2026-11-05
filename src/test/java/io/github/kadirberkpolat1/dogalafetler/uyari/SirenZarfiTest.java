package io.github.kadirberkpolat1.dogalafetler.uyari;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SirenZarfiTest {
    private static final float TOLERANS = 1e-4f;

    private static SirenZarfi ilerlet(SirenZarfi zarf, int tick) {
        for (int i = 0; i < tick; i++) {
            zarf.ilerle();
        }
        return zarf;
    }

    @Test
    void standartSirenSessizBaslarAcilistaTamDuzeyeCikar() {
        SirenZarfi zarf = new SirenZarfi(SirenTuru.STANDART, 200);
        assertEquals(0f, zarf.duzey(), TOLERANS);
        float onceki = zarf.duzey();
        for (int i = 0; i < SirenTuru.STANDART.acilisTick(); i++) {
            zarf.ilerle();
            assertTrue(zarf.duzey() > onceki, "açılışta düzey her tick artmalı");
            onceki = zarf.duzey();
        }
        assertEquals(1f, zarf.duzey(), TOLERANS);
        ilerlet(zarf, 100);
        assertEquals(1f, zarf.duzey(), TOLERANS);
        assertFalse(zarf.sonduruluyor());
    }

    @Test
    void sureDoluncaUstelSonupKendiligindenBiter() {
        int sure = 200;
        SirenZarfi zarf = ilerlet(new SirenZarfi(SirenTuru.STANDART, sure), sure - 1);
        assertFalse(zarf.sonduruluyor());
        zarf.ilerle();
        assertTrue(zarf.sonduruluyor(), "süre dolunca fade-out başlar");

        int sondurme = SirenTuru.STANDART.sondurmeTick();
        float onceki = zarf.duzey();
        Float oran = null;
        for (int i = 1; i <= sondurme; i++) {
            assertFalse(zarf.bitti(), "fade-out bitmeden durmamalı (tick " + i + ")");
            zarf.ilerle();
            float simdiki = zarf.duzey();
            assertTrue(simdiki < onceki, "fade-out sırasında düzey her tick azalmalı");
            if (oran != null) {
                assertEquals(oran, simdiki / onceki, 1e-3f, "üstel sönüşte ardışık oran sabit");
            }
            oran = simdiki / onceki;
            onceki = simdiki;
        }
        assertTrue(zarf.bitti());
        assertTrue(zarf.duzey() <= 0.001f + TOLERANS, "sonda -60 dB");
        assertEquals(sure + sondurme, zarf.gecenTick());
    }

    @Test
    void erkenSondurmeOAnkiDuzeydenBaslarVeGunesteDortSaniyedir() {
        SirenZarfi zarf = ilerlet(new SirenZarfi(SirenTuru.GUNES, 1200), 300);
        float sondurmeOncesi = zarf.duzey();
        zarf.sondur();
        assertEquals(sondurmeOncesi, zarf.duzey(), TOLERANS);
        ilerlet(zarf, SirenTuru.GUNES.sondurmeTick() - 1);
        assertFalse(zarf.bitti());
        zarf.ilerle();
        assertTrue(zarf.bitti());
        assertEquals(80, SirenTuru.GUNES.sondurmeTick());
        assertEquals(40, SirenTuru.STANDART.sondurmeTick());
    }

    @Test
    void acilistaSondurulenSirenYukselmedenSoner() {
        SirenZarfi zarf = ilerlet(new SirenZarfi(SirenTuru.STANDART, 200), 5);
        zarf.sondur();
        float onceki = zarf.duzey();
        while (!zarf.bitti()) {
            zarf.ilerle();
            assertTrue(zarf.duzey() < onceki);
            onceki = zarf.duzey();
        }
    }

    @Test
    void ikinciSondurmeYalnizDahaErkenBitiyorsaKisaltir() {
        SirenZarfi zarf = ilerlet(new SirenZarfi(SirenTuru.STANDART, 200), 50);
        assertTrue(zarf.sondur(), "ilk söndürme uygulanır");
        ilerlet(zarf, 10);
        assertFalse(zarf.sondur(), "daha geç bitecek ikinci söndürme yok sayılır");
        ilerlet(zarf, SirenTuru.STANDART.sondurmeTick() - 10);
        assertTrue(zarf.bitti(), "ikinci sondur() fade-out'u uzatmaz");

        SirenZarfi kesilen = ilerlet(new SirenZarfi(SirenTuru.STANDART, 200), 50);
        kesilen.sondur();
        ilerlet(kesilen, 5);
        assertTrue(kesilen.sondur(SirenZarfi.KESME_TICK), "daha erken bitiren kesme uygulanır");
        ilerlet(kesilen, SirenZarfi.KESME_TICK);
        assertTrue(kesilen.bitti(), "yeni siren eskisini kısa fade ile keser");
    }

    @Test
    void standartPerdeSpinUpIleYarimdanTamaCikarVeAsla05AltinaInmez() {
        SirenZarfi zarf = new SirenZarfi(SirenTuru.STANDART, 200);
        assertEquals(0.5f, zarf.perde(), TOLERANS);
        ilerlet(zarf, SirenTuru.STANDART.acilisTick());
        assertEquals(1f, zarf.perde(), TOLERANS);
        ilerlet(zarf, 200 + SirenTuru.STANDART.sondurmeTick());
        assertTrue(zarf.perde() >= 0.5f, "OpenAL perdesi 0,5'in altına kırpılır");
        assertTrue(zarf.perde() < 1f, "kapanışta perde iner (wind-down)");
    }

    @Test
    void gunesSireniSureBoyuncaUcDesibelYukselir() {
        int sure = 1200;
        SirenZarfi zarf = new SirenZarfi(SirenTuru.GUNES, sure);
        assertEquals(1f, zarf.perde(), TOLERANS);
        ilerlet(zarf, sure / 2);
        assertEquals(desibel(-1.5f), zarf.duzey(), 1e-3f);
        ilerlet(zarf, sure / 2 - 1);
        assertEquals(desibel(-3f / sure), zarf.duzey(), 1e-3f);
    }

    private static float desibel(float db) {
        return (float) Math.pow(10.0, db / 20.0);
    }
}
