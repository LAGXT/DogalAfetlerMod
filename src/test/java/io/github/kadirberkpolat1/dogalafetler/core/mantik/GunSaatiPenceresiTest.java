package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Gökyüzü-2 istek 26: güneş yalnız sabah, ay yalnız gece patlar; pencere sınırları ve erteleme hesabı. */
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

    @Test
    void ertelemeAktifiPencereyeDusurur() {
        long siren = 1_200;
        assertEquals(4_800, SABAH.bekleme(18_000, siren), "gece: ertesi gün doğumunda AKTIF");
        assertEquals(0, SABAH.bekleme(23_000, siren), "şafaktan az önce: siren bitince pencere");
        assertEquals(18_000, SABAH.bekleme(4_800, siren), "siren pencereyi kaçırıyor: ertesi gün");
        assertEquals(0, SABAH.bekleme(4_799, siren), "son an: AKTIF 5999'da");
        assertEquals(5_800, GECE.bekleme(6_000, siren), "gündüz: AKTIF 13000'de");
        assertEquals(0, GECE.bekleme(11_800, siren));
        assertEquals(14_000, GECE.bekleme(23_000 - 1_200, siren), "gece sonu kaçtı: ertesi akşam 13000'de AKTIF");
        for (long g = 0; g < 48_000; g += 137) {
            for (GunSaatiPenceresi p : new GunSaatiPenceresi[] {SABAH, GECE}) {
                long w = p.bekleme(g, siren);
                assertTrue(w >= 0 && w < GunSaatiPenceresi.GUN_TICK, "bekleme bir günden kısa");
                assertTrue(p.icinde(g + w + siren), "beklemeden sonra AKTIF pencerede: " + g);
                assertTrue(w == 0 || !p.icinde(g + w - 1 + siren), "en erken an: " + g);
            }
        }
    }

    @Test
    void dogalBaslangicBeklerKomutVeDonguKapaliBeklemez() {
        Optional<GunSaatiPenceresi> gunes = GunSaatiPenceresi.dogalPatlama(AfetTuru.GUNES_PATLAMASI);
        assertTrue(GunSaatiPenceresi.bekle(gunes, false, true, 18_000, 1_200, 0), "doğal, gece: bekler");
        assertFalse(GunSaatiPenceresi.bekle(gunes, false, true, 1_000, 1_200, 0), "doğal, sabah: başlar");
        assertFalse(GunSaatiPenceresi.bekle(gunes, true, true, 18_000, 1_200, 0), "komut pencereye bakmaz");
        assertFalse(GunSaatiPenceresi.bekle(gunes, false, false, 18_000, 1_200, 0), "gün döngüsü kapalı: beklenmez");
        assertFalse(GunSaatiPenceresi.bekle(Optional.empty(), false, true, 18_000, 1_200, 0), "pencere kuralı olmayan tür");
        Optional<GunSaatiPenceresi> ay = GunSaatiPenceresi.dogalPatlama(AfetTuru.AY_CARPMASI);
        assertTrue(GunSaatiPenceresi.bekle(ay, false, true, 6_000, 1_200, 0), "doğal ay, gündüz: bekler");
        assertFalse(GunSaatiPenceresi.bekle(ay, false, true, 15_000, 2, 0), "doğal ay, gece (hiz 1000 sireni 2 tick)");
    }

    @Test
    void beklemeBirGundenUzunSurmez() {
        Optional<GunSaatiPenceresi> ay = GunSaatiPenceresi.dogalPatlama(AfetTuru.AY_CARPMASI);
        long sinir = GunSaatiPenceresi.EN_COK_BEKLEME;
        assertEquals(24_000L, sinir, "üst sınır bir MC günü");
        assertTrue(GunSaatiPenceresi.bekle(ay, false, true, 6_000, 1_200, sinir - 1), "sınırdan bir tick önce bekler");
        assertFalse(GunSaatiPenceresi.bekle(ay, false, true, 6_000, 1_200, sinir), "saat kilitli: sınırda bekleme biter");
        assertFalse(GunSaatiPenceresi.bekle(ay, false, true, 6_000, 1_200, sinir + 5_000));
        // Saati kilitli (6000'de duran) dünyayı tick tick sür: tam sınırda bitmeli.
        long beklenen = 0;
        while (GunSaatiPenceresi.bekle(ay, false, true, 6_000, 1_200, beklenen)) {
            beklenen++;
        }
        assertEquals(sinir, beklenen, "kilitli saatte bekleme tam 24000 tick");
        for (long g = 0; g < 24_000; g += 97) {
            for (GunSaatiPenceresi p : new GunSaatiPenceresi[] {SABAH, GECE}) {
                assertTrue(p.bekleme(g, 1_200) < sinir, "normal döngüde pencere sınırdan önce gelir: " + g);
            }
        }
    }
}
