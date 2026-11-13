package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import org.junit.jupiter.api.Test;

class GunesPatlamasiEgrileriTest {
    private static final float EPS = 1e-6f;
    private static final long SIREN = 1_200L;
    private static final long BES_GUN = GunesPatlamasiEgrileri.aktifSure(5.0);

    @Test
    void sirendeGunesBirden1Virgul6KataHizlanarakBuyur() {
        assertEquals(0f, GunesPatlamasiEgrileri.sirenIlerlemesi(0, SIREN), EPS);
        assertEquals(GunesPatlamasiEgrileri.SIREN_SONU, GunesPatlamasiEgrileri.sirenIlerlemesi(SIREN, SIREN), EPS);
        assertEquals(1.6f, 1f + 2.2f * GunesPatlamasiEgrileri.SIREN_SONU, 1e-5f, "tasarım: siren sonunda 1,6 kat");
        float onceki = -1f;
        float oncekiAdim = -1f;
        for (long t = 0; t <= SIREN; t += 20) {
            float p = GunesPatlamasiEgrileri.sirenIlerlemesi(t, SIREN);
            assertTrue(p >= onceki, "monoton");
            if (onceki >= 0) {
                assertTrue(p - onceki >= oncekiAdim - EPS, "sona doğru hızlanır (s = 1 + 0,6·t²)");
                oncekiAdim = p - onceki;
            }
            onceki = p;
        }
    }

    @Test
    void aktifteBesGundeDogrusalSirenSonundanBire() {
        assertEquals(BES_GUN, 120_000L);
        assertEquals(GunesPatlamasiEgrileri.SIREN_SONU, GunesPatlamasiEgrileri.aktifIlerlemesi(0, BES_GUN), EPS,
                "siren bitişinden sıçramadan sürer");
        assertEquals(1f, GunesPatlamasiEgrileri.aktifIlerlemesi(BES_GUN, BES_GUN), EPS);
        assertEquals(1f, GunesPatlamasiEgrileri.aktifIlerlemesi(BES_GUN * 2, BES_GUN), EPS, "tavan 1");
        float yarim = GunesPatlamasiEgrileri.aktifIlerlemesi(BES_GUN / 2, BES_GUN);
        assertEquals((GunesPatlamasiEgrileri.SIREN_SONU + 1f) / 2f, yarim, 1e-5f, "doğrusal");
        assertEquals(3.2f, 1f + 2.2f * GunesPatlamasiEgrileri.aktifIlerlemesi(BES_GUN, BES_GUN), 1e-5f, "son: 3,2 kat");
    }

    @Test
    void meteorYogunluguKirkSaniyedenOnIkiSaniyeye() {
        assertEquals(1.5, GunesPatlamasiEgrileri.dakikaBasinaMeteor(0, BES_GUN, 1.5, 5.0), 1e-9, "1/40 sn");
        assertEquals(5.0, GunesPatlamasiEgrileri.dakikaBasinaMeteor(BES_GUN, BES_GUN, 1.5, 5.0), 1e-9, "1/12 sn");
        assertEquals(3.25, GunesPatlamasiEgrileri.dakikaBasinaMeteor(BES_GUN / 2, BES_GUN, 1.5, 5.0), 1e-9);
    }

    @Test
    void kavrulmaUcuncuGundenSonraArtar() {
        long ucuncuGun = 3 * GunesPatlamasiEgrileri.GUN_TICK;
        assertEquals(0, GunesPatlamasiEgrileri.kavrulmaOlasiligi(ucuncuGun - 1, BES_GUN, 3, 0.5), 1e-12);
        assertEquals(0, GunesPatlamasiEgrileri.kavrulmaOlasiligi(ucuncuGun, BES_GUN, 3, 0.5), 1e-12);
        assertEquals(0.25, GunesPatlamasiEgrileri.kavrulmaOlasiligi(4 * GunesPatlamasiEgrileri.GUN_TICK, BES_GUN, 3, 0.5), 1e-9);
        assertEquals(0.5, GunesPatlamasiEgrileri.kavrulmaOlasiligi(BES_GUN, BES_GUN, 3, 0.5), 1e-9);
        assertEquals(0, GunesPatlamasiEgrileri.kavrulmaOlasiligi(BES_GUN, BES_GUN, 6, 0.5), 1e-12, "süreden sonra başlar: hiç");
    }

    @Test
    void gunVeAltAsamaSayilari() {
        assertEquals(5, GunesPatlamasiEgrileri.gunSayisi(BES_GUN));
        assertEquals(2, GunesPatlamasiEgrileri.gunSayisi(GunesPatlamasiEgrileri.aktifSure(1.5)));
        assertEquals(0, GunesPatlamasiEgrileri.gun(0));
        assertEquals(0, GunesPatlamasiEgrileri.gun(23_999));
        assertEquals(4, GunesPatlamasiEgrileri.gun(BES_GUN - 1));
    }

    @Test
    void yalnizNormalYaDaTamamlanmisKizilCuceBaslar() {
        assertTrue(GunesPatlamasiEgrileri.baslayabilir(GunesEvresi.NORMAL, 0f));
        assertTrue(GunesPatlamasiEgrileri.baslayabilir(GunesEvresi.KIZIL_CUCE, 1f));
        assertFalse(GunesPatlamasiEgrileri.baslayabilir(GunesEvresi.KIZIL_CUCE, 0.5f), "doğuş sürerken zincirin");
        assertFalse(GunesPatlamasiEgrileri.baslayabilir(GunesEvresi.BUYUYEN, 0.5f));
        assertFalse(GunesPatlamasiEgrileri.baslayabilir(GunesEvresi.SUPERNOVA, 0f));
        assertFalse(GunesPatlamasiEgrileri.baslayabilir(GunesEvresi.GUNESSIZ, 1f));
    }
}
