package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class OyunGunuTest {
    /** Vanilla uyku: gece ≥ 12 542'de yatılır, bir sonraki günün başında (24 000'in katı) uyanılır. */
    private static long uyanis(long gunSaati) {
        long sonraki = gunSaati + OyunGunu.GUN_TICK;
        return sonraki - sonraki % OyunGunu.GUN_TICK;
    }

    @Test
    void gunOyunZamanindanVeOfsettenHesaplanir() {
        assertEquals(0, OyunGunu.gun(0, 0, 0));
        assertEquals(0, OyunGunu.gun(23_999, 0, 0));
        assertEquals(1, OyunGunu.gun(24_000, 0, 0));
        assertEquals(42, OyunGunu.gun(48_000, 0, 40));
        assertEquals(0, OyunGunu.gun(24_000, 0, -5), "gün negatif olmaz");
    }

    @Test
    void ofsetIstenenGunuVerirVeGunYineIlerler() {
        long zaman = 3 * 24_000L + 500;
        long uyku = 30_000;
        long ofset = OyunGunu.ofset(42, zaman, uyku);
        assertEquals(42, OyunGunu.gun(zaman, uyku, ofset));
        assertEquals(43, OyunGunu.gun(zaman + 24_000, uyku, ofset));
        assertEquals(43, OyunGunu.gun(zaman, uyku + 24_000, ofset), "ofsetten sonraki uyku da günü ilerletir");
        assertEquals(0, OyunGunu.gun(zaman, uyku, OyunGunu.ofset(0, zaman, uyku)));
    }

    @Test
    void uykununAtladigiTickGuneEklenir() {
        long yatis = 12_542;
        long atlanan = OyunGunu.uykuAtlamasi(uyanis(yatis), yatis);
        assertEquals(11_458, atlanan);
        assertEquals(0.477, atlanan / (double) OyunGunu.GUN_TICK, 0.001, "bir uyku ~0,48 gün");
        assertEquals(0, OyunGunu.gun(yatis, 0, 0));
        assertEquals(1, OyunGunu.gun(yatis, atlanan, 0), "uyanınca yeni gün");
        assertEquals(0, OyunGunu.uykuAtlamasi(1_000, 5_000), "geri giden saat birikmez");
        assertEquals(0, OyunGunu.uykuAtlamasi(5_000, 5_000));
    }

    @Test
    void ardisikOnUykuOnGunSayar() {
        long oyunZamani = 0;
        long gunSaati = 0;
        long uyku = 0;
        for (int gece = 0; gece < 10; gece++) {
            oyunZamani += 12_542;
            gunSaati += 12_542;
            long yeni = uyanis(gunSaati);
            uyku += OyunGunu.uykuAtlamasi(yeni, gunSaati);
            gunSaati = yeni;
        }
        assertEquals(114_580, uyku);
        assertEquals(10, OyunGunu.gun(oyunZamani, uyku, 0), "her gece uyunan 10 gün = 10 oyun günü");
        assertEquals(5, OyunGunu.gun(oyunZamani, 0, 0), "uyku sayılmasaydı yarısı");
    }
}
