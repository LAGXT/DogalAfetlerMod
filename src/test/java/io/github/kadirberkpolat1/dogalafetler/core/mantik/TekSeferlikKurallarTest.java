package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import org.junit.jupiter.api.Test;

class TekSeferlikKurallarTest {
    private static final TekSeferlikAyarlari A = TekSeferlikAyarlari.VARSAYILAN;

    @Test
    void varsayilanKriterGunVeAyGunestenErken() {
        assertEquals(GelisimKriteri.GUN, A.gunesPatlamasiKriteri());
        assertEquals(GelisimKriteri.GUN, A.ayCarpmasiKriteri());
        assertEquals(40, A.gunesPatlamasiMinGun());
        assertEquals(25, A.ayCarpmasiMinGun());
        assertFalse(A.gunesPatlamasiTekrarlanabilir());
    }

    @Test
    void kriterlerGunVePuaniBirlestirir() {
        assertTrue(GelisimKriteri.GUN.saglandi(40, 40, 0, 35));
        assertFalse(GelisimKriteri.GUN.saglandi(39.9, 40, 1000, 35));
        assertTrue(GelisimKriteri.PUAN.saglandi(0, 40, 35, 35));
        assertFalse(GelisimKriteri.PUAN.saglandi(1000, 40, 34.9, 35));
        assertTrue(GelisimKriteri.IKISI_DE.saglandi(40, 40, 35, 35));
        assertFalse(GelisimKriteri.IKISI_DE.saglandi(40, 40, 34, 35));
        assertFalse(GelisimKriteri.IKISI_DE.saglandi(39, 40, 35, 35));
        assertTrue(GelisimKriteri.HERHANGI.saglandi(40, 40, 0, 35));
        assertTrue(GelisimKriteri.HERHANGI.saglandi(0, 40, 35, 35));
        assertFalse(GelisimKriteri.HERHANGI.saglandi(39, 40, 34, 35));
    }

    @Test
    void ayYoksaAyCarpmasiSecilmez() {
        assertTrue(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, true, GunesEvresi.NORMAL, 500, 0, A));
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, false, GunesEvresi.NORMAL, 500, 0, A));
    }

    @Test
    void gunKriterindePuanaBakilmaz() {
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, GunesEvresi.NORMAL,
                A.gunesPatlamasiMinGun() - 1, 1000, A));
        assertTrue(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, GunesEvresi.NORMAL,
                A.gunesPatlamasiMinGun(), 0, A));
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, true, GunesEvresi.NORMAL,
                A.ayCarpmasiMinGun() - 1, 1000, A));
        assertTrue(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, true, GunesEvresi.NORMAL, A.ayCarpmasiMinGun(),
                0, A));
    }

    @Test
    void puanKriterindeOyuncuGelisimiBeklenir() {
        TekSeferlikAyarlari puan = A.kriterlerIle(GelisimKriteri.PUAN, GelisimKriteri.PUAN);
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, GunesEvresi.NORMAL, 1000,
                A.gunesPatlamasiPuanEsigi() - 0.5, puan));
        assertTrue(TekSeferlikKurallar.uygunMu(AfetTuru.GUNES_PATLAMASI, true, GunesEvresi.NORMAL, 0,
                A.gunesPatlamasiPuanEsigi(), puan));
        assertFalse(TekSeferlikKurallar.uygunMu(AfetTuru.AY_CARPMASI, true, GunesEvresi.NORMAL, 1000,
                A.ayCarpmasiPuanEsigi() - 0.5, puan));
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
