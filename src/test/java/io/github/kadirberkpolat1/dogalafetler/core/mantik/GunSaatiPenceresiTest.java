package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Gökyüzü-2 istek 26: güneş yalnız sabah, ay yalnız gece patlar; pencere sınırları ve seçim kuralı (K-E67). */
class GunSaatiPenceresiTest {
    private static final GunSaatiPenceresi SABAH = GunSaatiPenceresi.SABAH;
    private static final GunSaatiPenceresi GECE = GunSaatiPenceresi.GECE;

    @Test
    void pencereSinirlari() {
        assertTrue(SABAH.icinde(0));
        assertTrue(SABAH.icinde(5_999));
        assertFalse(SABAH.icinde(6_000));
        assertFalse(SABAH.icinde(23_999));
        assertFalse(GECE.icinde(12_999));
        assertTrue(GECE.icinde(13_000));
        assertTrue(GECE.icinde(22_999));
        assertFalse(GECE.icinde(23_000));
        assertTrue(SABAH.icinde(24_000 * 7 + 100), "gün saati 24000'e göre kalanla");
        assertTrue(GECE.icinde(-6_000), "negatif gün saati de kalanla (18000)");
    }

    @Test
    void turPencereleri() {
        assertEquals(Optional.of(SABAH), GunSaatiPenceresi.dogalPatlama(AfetTuru.GUNES_PATLAMASI));
        assertEquals(Optional.of(GECE), GunSaatiPenceresi.dogalPatlama(AfetTuru.AY_CARPMASI));
        for (AfetTuru tur : AfetTuru.values()) {
            if (tur != AfetTuru.GUNES_PATLAMASI && tur != AfetTuru.AY_CARPMASI) {
                assertTrue(GunSaatiPenceresi.dogalPatlama(tur).isEmpty(), "pencere kuralı yok: " + tur);
            }
        }
        assertThrows(IllegalArgumentException.class, () -> new GunSaatiPenceresi(6_000, 6_000));
        assertThrows(IllegalArgumentException.class, () -> new GunSaatiPenceresi(-1, 6_000));
        assertThrows(IllegalArgumentException.class, () -> new GunSaatiPenceresi(0, 24_001));
    }

    /** K-E67: pencere seçimde uygulanır; seçim anı ve seçimden {@code pay} sonraki görünme anı pencerede olmalı. */
    @Test
    void secimAniVeGorunmeAniPencerede() {
        long pay = 800;
        assertTrue(SABAH.secilebilir(0, pay));
        assertTrue(SABAH.secilebilir(5_199, pay), "son an: görünme 5999'da");
        assertFalse(SABAH.secilebilir(5_200, pay), "pencerenin son 40 sn'si");
        assertFalse(SABAH.secilebilir(23_500, pay), "şafaktan önce seçilmez");
        assertFalse(SABAH.secilebilir(18_000, pay));
        assertTrue(GECE.secilebilir(13_000, pay));
        assertFalse(GECE.secilebilir(22_201, pay));
        assertFalse(GECE.secilebilir(6_000, pay));
        assertFalse(SABAH.secilebilir(0, 6_000), "pay pencereden uzunsa hiç seçilmez");
        for (long g = -24_000; g < 48_000; g += 37) {
            for (GunSaatiPenceresi p : new GunSaatiPenceresi[] {SABAH, GECE}) {
                if (p.secilebilir(g, pay)) {
                    for (long t = 0; t <= pay; t += 50) {
                        assertTrue(p.icinde(g + t), "seçimden " + t + " tick sonra pencere dışı: " + g);
                    }
                }
            }
        }
        assertEquals(5_200 / 24_000.0, SABAH.secimPayi(pay), 1e-12);
        assertEquals(9_200 / 24_000.0, GECE.secimPayi(pay), 1e-12);
    }

    /** Varsayılan ön-uyarı ve siren ile görünme payı 800 tick (ön-uyarı 100 + 60, siren 500, emniyet 140). */
    @Test
    void varsayilanGorunmePayi() {
        for (AfetTuru tur : new AfetTuru[] {AfetTuru.GUNES_PATLAMASI, AfetTuru.AY_CARPMASI}) {
            assertEquals(800, GunSaatiPenceresi.gorunmePayi(tur, OnUyariPenceresi.VARSAYILAN_ERKEN_BILDIRIM,
                    OnUyariPenceresi.VARSAYILAN_EN_AZ, OnUyariPenceresi.VARSAYILAN_EN_COK), tur.kimlik());
        }
    }

    /** Doğal seçim çarpanı: pencere dışı 0, içinde seçim payının tersi (günlük ortalama korunur); kural yoksa ya da döngü kapalıysa 1. */
    @Test
    void dogalSecimCarpaniGunlukOrtalamayiKorur() {
        long pay = 800;
        assertEquals(0.0, GunSaatiPenceresi.dogalSecimCarpani(AfetTuru.GUNES_PATLAMASI, 18_000, pay, true));
        assertEquals(0.0, GunSaatiPenceresi.dogalSecimCarpani(AfetTuru.AY_CARPMASI, 6_000, pay, true));
        assertEquals(1.0, GunSaatiPenceresi.dogalSecimCarpani(AfetTuru.GUNES_PATLAMASI, 18_000, pay, false),
                "gün döngüsü kapalı: pencere yok");
        assertEquals(1.0, GunSaatiPenceresi.dogalSecimCarpani(AfetTuru.DEPREM, 18_000, pay, true));
        for (AfetTuru tur : new AfetTuru[] {AfetTuru.GUNES_PATLAMASI, AfetTuru.AY_CARPMASI}) {
            double toplam = 0;
            for (long g = 0; g < GunSaatiPenceresi.GUN_TICK; g++) {
                toplam += GunSaatiPenceresi.dogalSecimCarpani(tur, g, pay, true);
            }
            assertEquals(GunSaatiPenceresi.GUN_TICK, toplam, 1e-6, tur.kimlik() + ": gün boyu ortalama çarpan 1");
        }
    }
}
