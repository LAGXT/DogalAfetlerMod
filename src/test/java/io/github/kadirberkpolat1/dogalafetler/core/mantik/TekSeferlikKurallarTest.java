package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import org.junit.jupiter.api.Test;

class TekSeferlikKurallarTest {
    private static final TekSeferlikAyarlari A = TekSeferlikAyarlari.VARSAYILAN;

    @Test
    void ayYoksaAyCarpmasiSecilmez() {
        assertTrue(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, true, GunesEvresi.NORMAL, 500, 100, A));
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, false, GunesEvresi.NORMAL, 500, 100, A));
    }

    @Test
    void ayCarpmasiGunVeGelisimEsiginiBekler() {
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, true, GunesEvresi.NORMAL,
                A.ayCarpmasiEnErkenGun() - 1, 100, A));
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, true, GunesEvresi.NORMAL, 500,
                A.ayCarpmasiPuanEsigi() - 1, A));
        assertTrue(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, true, GunesEvresi.NORMAL,
                A.ayCarpmasiEnErkenGun(), A.ayCarpmasiPuanEsigi(), A));
    }

    @Test
    void gunesPatlamasiYalnizNormalGunesteSecilir() {
        assertTrue(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, GunesEvresi.NORMAL, 500, 100, A));
        for (GunesEvresi evre : new GunesEvresi[] {GunesEvresi.BUYUYEN, GunesEvresi.SUPERNOVA, GunesEvresi.GUNESSIZ,
                GunesEvresi.KIZIL_CUCE}) {
            assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, evre, 500, 100, A), evre.name());
        }
    }

    @Test
    void kizilCuceDonemindeTekrarlanabilirAyariylaSecilir() {
        TekSeferlikAyarlari tekrar = A.gunesTekrarIle(true);
        assertTrue(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, GunesEvresi.KIZIL_CUCE, 500, 100,
                tekrar));
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, GunesEvresi.GUNESSIZ, 500, 100,
                tekrar), "güneşsiz dönemde tekrar ayarı açık olsa da seçilmez");
    }

    @Test
    void gunesPatlamasiOyuncuGelisimineBakar() {
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, GunesEvresi.NORMAL,
                A.gunesPatlamasiEnErkenGun() - 1, 100, A));
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, GunesEvresi.NORMAL, 500,
                A.gunesPatlamasiPuanEsigi() - 0.5, A));
    }

    @Test
    void digerAfetlerHerDonemdeDevamEder() {
        for (AfetTuru tur : AfetTuru.values()) {
            if (tur.sunucuGeneli()) {
                continue;
            }
            for (GunesEvresi evre : GunesEvresi.values()) {
                assertTrue(TekSeferlikKurallar.uygunMu(tur, false, evre, 0, 0, A), tur + " " + evre);
            }
        }
    }
}
