package io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelTetikleyicileri.Anlik;
import io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik.SelTetikleyicileri.Gecici;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.AfetZamanlayicisi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.CarpanListesi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.GeciciCarpan;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.SonumTipi;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.ZamanlayiciAyarlari;
import org.junit.jupiter.api.Test;

/** Selin doğal seçim tetikleyicileri (K-E80): yağmur, fırtına, tsunami ve ay çarpması olasılığı artırır. */
class SelTetikleyicileriTest {
    private static final Anlik A = Anlik.VARSAYILAN;

    @Test
    void havaVeTsunamiCarpilirHavadanEnGucluSayilir() {
        assertEquals(1.0, SelTetikleyicileri.anlikCarpan(false, false, false, A));
        assertEquals(A.yagmur(), SelTetikleyicileri.anlikCarpan(true, false, false, A));
        assertEquals(A.firtina(), SelTetikleyicileri.anlikCarpan(true, true, false, A), "fırtına yağmurun üstüne eklenmez");
        assertEquals(A.firtina(), SelTetikleyicileri.anlikCarpan(false, true, false, A));
        assertEquals(A.tsunamiSururken(), SelTetikleyicileri.anlikCarpan(false, false, true, A));
        assertEquals(A.yagmur() * A.tsunamiSururken(), SelTetikleyicileri.anlikCarpan(true, false, true, A), 1e-12);
    }

    @Test
    void birdenKucukCarpanAzaltmazSecimiSifirlamaz() {
        Anlik zayif = new Anlik(0.1, 0.0, -3.0);
        assertEquals(1.0, SelTetikleyicileri.anlikCarpan(true, true, true, zayif));
    }

    @Test
    void geciciCarpanSinirlanirVeEtkisizOlaniAyiklar() {
        assertTrue(new Gecici(6.0, 4800, 12000).etkili());
        assertFalse(new Gecici(1.0, 4800, 12000).etkili(), "tepe 1 etkisiz");
        assertFalse(new Gecici(6.0, 0, 0).etkili(), "süresiz etkisiz");
        assertEquals(SelTetikleyicileri.CARPAN_EN_COK, new Gecici(1e6, 1, 1).tepe());
        assertEquals(1.0, new Gecici(-2, 1, 1).tepe());
    }

    @Test
    void cekirdekCarpaniylaSeciliOranTepedeYukselirSonumdeNormaleIner() {
        ZamanlayiciAyarlari ayar = ZamanlayiciAyarlari.VARSAYILAN;
        CarpanListesi liste = new CarpanListesi();
        Gecici g = new Gecici(10.0, 18_000, 36_000);
        liste.ekle(new GeciciCarpan(AfetTuru.SEL, g.tepe(), 0, g.sabitTick(), g.sonumTick(), SonumTipi.DOGRUSAL));
        double normal = AfetZamanlayicisi.turOrani(ayar, AfetTuru.SEL, 1.0, 1.0);
        double tepe = AfetZamanlayicisi.turOrani(ayar, AfetTuru.SEL, liste.carpan(AfetTuru.SEL, 100), 1.0);
        double sonum = AfetZamanlayicisi.turOrani(ayar, AfetTuru.SEL, liste.carpan(AfetTuru.SEL, 36_000), 1.0);
        double bitti = AfetZamanlayicisi.turOrani(ayar, AfetTuru.SEL, liste.carpan(AfetTuru.SEL, 90_000), 1.0);
        assertTrue(normal > 0 && normal < AfetZamanlayicisi.turOrani(ayar, AfetTuru.KASIRGA, 1.0, 1.0),
                "sel sık afetlerden nadir başlar");
        assertEquals(10 * normal, tepe, 1e-12);
        assertTrue(sonum > normal && sonum < tepe, "sönümde azalır");
        assertEquals(normal, bitti, 1e-12, "süre dolunca normale döner");
        assertEquals(1.0, liste.carpan(AfetTuru.TSUNAMI, 100), "diğer türler etkilenmez");
    }
}
