package io.github.kadirberkpolat1.dogalafetler.uyari;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import org.junit.jupiter.api.Test;

class SirenZarfiTest {
    private static final float TOLERANS = 1e-4f;
    /** Güneş sireninin tam süresi (25 sn, K0-19 rev. K-E67). */
    private static final int UZUN = SirenTuru.UZUN_SURE_TICK;

    private static SirenZarfi ilerlet(SirenZarfi zarf, int tick) {
        for (int i = 0; i < tick; i++) {
            zarf.ilerle();
        }
        return zarf;
    }

    private static SirenZarfi tam(SirenProfili profil, int sure) {
        return new SirenZarfi(profil, sure, sure);
    }

    @Test
    void standartSirenSessizBaslarAcilistaTamDuzeyeCikar() {
        SirenZarfi zarf = tam(SirenProfili.STANDART, 200);
        assertEquals(0f, zarf.duzey(), TOLERANS);
        float onceki = zarf.duzey();
        for (int i = 0; i < SirenProfili.STANDART.acilisTick(); i++) {
            zarf.ilerle();
            assertTrue(zarf.duzey() > onceki, "açılışta düzey her tick artmalı");
            onceki = zarf.duzey();
        }
        assertEquals(1f, zarf.duzey(), TOLERANS);
        ilerlet(zarf, 100);
        assertEquals(1f, zarf.duzey(), TOLERANS);
        assertFalse(zarf.sonduruluyor());
    }

    /** B3: sürenin yetkilisi sunucunun durdurma paketidir; istemci süresi yalnız güvenlik tavanı (süre + 3 sn). */
    @Test
    void sureDolsaDaDurdurmaPaketiGelmedikceGuvenlikTavaninaKadarCalar() {
        int sure = 200;
        SirenZarfi zarf = ilerlet(tam(SirenProfili.STANDART, sure), sure);
        assertFalse(zarf.sonduruluyor(), "süre dolunca kendiliğinden sönmez; sunucunun durdurmasını bekler");
        assertEquals(0, zarf.kalanTick());
        ilerlet(zarf, SirenZarfi.GUVENLIK_PAYI_TICK - 1);
        assertFalse(zarf.sonduruluyor());
        assertFalse(zarf.tavanaUlasti());
        zarf.ilerle();
        assertTrue(zarf.sonduruluyor(), "güvenlik tavanında kendi fade-out'u başlar");
        assertTrue(zarf.tavanaUlasti());
        ilerlet(zarf, SirenProfili.STANDART.sondurmeTick());
        assertTrue(zarf.bitti());
        assertEquals(sure + SirenZarfi.GUVENLIK_PAYI_TICK + SirenProfili.STANDART.sondurmeTick(), zarf.gecenTick());
    }

    @Test
    void sondurmeUsteldirOAnkiDuzeydenBaslarVeSondaAltmisDesibelAsagidadir() {
        SirenZarfi zarf = ilerlet(tam(SirenProfili.STANDART, 200), 200);
        int sondurme = 40;
        assertTrue(zarf.sondur(sondurme));
        float onceki = zarf.duzey();
        assertEquals(1f, onceki, TOLERANS, "fade-out o anki düzeyden başlar, sıçramaz");
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
    }

    @Test
    void erkenSondurmeOAnkiDuzeydenBaslarVeGunesProfiliDortSaniyedir() {
        SirenZarfi zarf = ilerlet(tam(SirenProfili.GUNES, UZUN), 300);
        float sondurmeOncesi = zarf.duzey();
        zarf.sondur(SirenProfili.GUNES.sondurmeTick());
        assertEquals(sondurmeOncesi, zarf.duzey(), TOLERANS);
        ilerlet(zarf, SirenProfili.GUNES.sondurmeTick() - 1);
        assertFalse(zarf.bitti());
        zarf.ilerle();
        assertTrue(zarf.bitti());
        assertEquals(80, SirenProfili.GUNES.sondurmeTick());
        assertEquals(40, SirenProfili.STANDART.sondurmeTick());
    }

    @Test
    void acilistaSondurulenSirenYukselmedenSoner() {
        SirenZarfi zarf = ilerlet(tam(SirenProfili.STANDART, 200), 5);
        zarf.sondur(40);
        float onceki = zarf.duzey();
        while (!zarf.bitti()) {
            zarf.ilerle();
            assertTrue(zarf.duzey() < onceki);
            onceki = zarf.duzey();
        }
    }

    @Test
    void ikinciSondurmeYalnizDahaErkenBitiyorsaKisaltir() {
        SirenZarfi zarf = ilerlet(tam(SirenProfili.STANDART, 200), 50);
        assertTrue(zarf.sondur(40), "ilk söndürme uygulanır");
        ilerlet(zarf, 10);
        assertFalse(zarf.sondur(40), "daha geç bitecek ikinci söndürme yok sayılır");
        ilerlet(zarf, 30);
        assertTrue(zarf.bitti(), "ikinci sondur() fade-out'u uzatmaz");

        SirenZarfi kesilen = ilerlet(tam(SirenProfili.STANDART, 200), 50);
        kesilen.sondur(40);
        ilerlet(kesilen, 5);
        assertTrue(kesilen.sondur(SirenZarfi.KESME_TICK), "daha erken bitiren kesme uygulanır");
        ilerlet(kesilen, SirenZarfi.KESME_TICK);
        assertTrue(kesilen.bitti(), "yeni siren eskisini kısa fade ile keser");
        assertThrows(IllegalArgumentException.class, () -> kesilen.sondur(0));
    }

    @Test
    void standartPerdeSpinUpIleYarimdanTamaCikarVeAsla05AltinaInmez() {
        SirenZarfi zarf = tam(SirenProfili.STANDART, 200);
        assertEquals(0.5f, zarf.perde(), TOLERANS);
        ilerlet(zarf, SirenProfili.STANDART.acilisTick());
        assertEquals(1f, zarf.perde(), TOLERANS);
        zarf.sondur(40);
        ilerlet(zarf, 40);
        assertTrue(zarf.perde() >= 0.5f, "OpenAL perdesi 0,5'in altına kırpılır");
        assertTrue(zarf.perde() < 1f, "kapanışta perde iner (wind-down)");
    }

    @Test
    void gunesSireniSureBoyuncaUcDesibelYukselir() {
        int sure = UZUN;
        SirenZarfi zarf = tam(SirenProfili.GUNES, sure);
        assertEquals(1f, zarf.perde(), TOLERANS);
        ilerlet(zarf, sure / 2);
        assertEquals(desibel(-1.5f), zarf.duzey(), 1e-3f);
        ilerlet(zarf, sure / 2);
        assertEquals(1f, zarf.duzey(), 1e-3f);
    }

    /**
     * Sonradan giren oyuncu (madde 3, S-06 B2): süre kalan süredir; açılış kısalmaz (kısa bir belirme), güneş sireninin
     * yükselişi ise geçen sürenin yerinden sürer (geç gelen baştaki kısık düzeyi duymaz).
     */
    @Test
    void kalanToplamdanKisaysaSureKalandirYukselisKaldigiYerdenSurer() {
        SirenZarfi zarf = new SirenZarfi(SirenProfili.GUNES, UZUN, 300);
        assertEquals(300, zarf.kalanTick());
        assertEquals(UZUN, zarf.toplamTick());
        assertEquals(0f, zarf.duzey(), TOLERANS, "geç gelen de sessizden başlar");
        ilerlet(zarf, SirenProfili.GUNES.acilisTick());
        int kalanYukselis = 300 - SirenProfili.GUNES.acilisTick();
        assertEquals(desibel(-3f * kalanYukselis / UZUN), zarf.duzey(), 1e-3f,
                "yükseliş " + (UZUN - 300) + ". tick'ten sürer");
        ilerlet(zarf, 300 - SirenProfili.GUNES.acilisTick());
        assertEquals(0, zarf.kalanTick());
        ilerlet(zarf, SirenZarfi.GUVENLIK_PAYI_TICK);
        assertTrue(zarf.sonduruluyor(), "güvenlik tavanı kalan süreye göre");
    }

    @Test
    void paketleGelenSurelerKirpilir() {
        assertEquals(1, SirenZarfi.toplamiKirp(0));
        assertEquals(UZUN, SirenZarfi.toplamiKirp(UZUN));
        assertEquals(SirenZarfi.EN_UZUN_TICK, SirenZarfi.toplamiKirp(Integer.MAX_VALUE));
        assertEquals(1, SirenZarfi.kalaniKirp(-5, 200));
        assertEquals(120, SirenZarfi.kalaniKirp(120, 200), "kalan < toplam korunur");
        assertEquals(200, SirenZarfi.kalaniKirp(500, 200), "kalan toplamı aşamaz");
        SirenZarfi zarf = new SirenZarfi(SirenProfili.STANDART, 200, 500);
        assertEquals(200, zarf.kalanTick(), "kurucu da kırpar");
    }

    /**
     * İstemci, ses motorunun kestiği sireni (boyut değişimi, /stopsound) aynı zarfla yeni bir ses örneğinde sürdürür;
     * okumak zarfı ilerletmediği için açılış yeniden başlamaz, düzey sıçramaz (K0-27).
     */
    @Test
    void zarfKorunarakYenidenBaslatmaAcilisiTekrarlamaz() {
        SirenZarfi zarf = ilerlet(tam(SirenProfili.GUNES, UZUN), 30);
        float kesilmeOncesi = zarf.duzey();
        assertTrue(kesilmeOncesi > 0.3f, "açılışın ortası");
        assertEquals(kesilmeOncesi, zarf.duzey(), TOLERANS, "okuma durumu değiştirmez");
        assertEquals(30, zarf.gecenTick(), "okuma zarfı ilerletmez");

        float onceki = kesilmeOncesi;
        float enBuyukAdim = 0f;
        for (int i = 0; i < 300; i++) {
            zarf.ilerle();
            enBuyukAdim = Math.max(enBuyukAdim, Math.abs(zarf.duzey() - onceki));
            onceki = zarf.duzey();
        }
        assertTrue(enBuyukAdim < 0.05f, "düzey sürekli, açılış tekrarı yok (en büyük adım " + enBuyukAdim + ")");
    }

    private static float desibel(float db) {
        return (float) Math.pow(10.0, db / 20.0);
    }
}
