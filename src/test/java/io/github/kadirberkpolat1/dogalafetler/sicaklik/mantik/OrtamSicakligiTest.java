package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import org.junit.jupiter.api.Test;

/** Madde 7a, 9, bölüm D ve G: ortam sıcaklığının girdileri. */
class OrtamSicakligiTest {
    private static final OrtamSicakligi.Ayarlar A = OrtamSicakligi.Ayarlar.VARSAYILAN;
    private static final long OGLE = 6000;
    private static final long GECE_YARISI = 18000;

    /** Ova, deniz seviyesinde, açık havada, öğlen. */
    private static OrtamSicakligi.Girdi ova() {
        return OrtamSicakligi.Girdi.yuzey(0.8, false, OGLE, 64);
    }

    private static double t(OrtamSicakligi.Girdi g) {
        return OrtamSicakligi.hesapla(g, A).toplam();
    }

    @Test
    void ovaOglenKonforluGeceSerin() {
        double ogle = t(ova());
        double gece = t(ova().gunSaati(GECE_YARISI));
        assertTrue(ogle > 16 && ogle < 28, "ova öğlen " + ogle);
        assertTrue(gece < ogle - 8, "gece daha soğuk " + gece);
    }

    @Test
    void coldeGunduzCokSicakGeceCokSoguk() {
        OrtamSicakligi.Girdi col = OrtamSicakligi.Girdi.yuzey(2.0, true, OGLE, 64);
        double ogle = t(col);
        double gece = t(col.gunSaati(GECE_YARISI));
        assertTrue(ogle > 42, "çöl öğlen " + ogle);
        assertTrue(gece < 10, "çöl gece " + gece);
        double ovaFark = t(ova()) - t(ova().gunSaati(GECE_YARISI));
        assertTrue(ogle - gece > 2.5 * ovaFark);
    }

    @Test
    void yukseklikSogutur() {
        assertTrue(t(ova().y(200)) < t(ova()) - 6);
        assertEquals(t(ova()), t(ova().y(50).yuzeyY(50)), 1e-9, "deniz altındaki yüzey cezasız");
    }

    @Test
    void derinMagaraDengeli() {
        OrtamSicakligi.Girdi magaraCol = OrtamSicakligi.Girdi.yuzey(2.0, true, OGLE, 64).y(10).gokyuzu(false);
        OrtamSicakligi.Girdi magaraKar = OrtamSicakligi.Girdi.yuzey(0.0, false, GECE_YARISI, 64).y(10).gokyuzu(false);
        assertEquals(A.yeraltiSicakligi(), t(magaraCol), 1.0);
        assertEquals(A.yeraltiSicakligi(), t(magaraKar), 1.0);
        OrtamSicakligi.Girdi ev = ova().gunSaati(GECE_YARISI).gokyuzu(false);
        double disari = t(ova().gunSaati(GECE_YARISI));
        assertTrue(t(ev) > disari, "kapalı alan gece dışarıdan dengeli");
    }

    @Test
    void yagisSuVeGunesAlti() {
        assertTrue(t(ova().yagis(Yagis.YAGMUR)) < t(ova()));
        assertTrue(t(ova().yagis(Yagis.FIRTINA)) < t(ova().yagis(Yagis.YAGMUR)));
        assertTrue(t(ova().yagis(Yagis.KAR)) < t(ova()));
        assertTrue(t(ova().suda(true)) < t(ova()));
        // Gölgede (gökyüzü kapalı ama yüzeyde) öğlen güneş altindakinden serin.
        OrtamSicakligi.Sonuc gunes = OrtamSicakligi.hesapla(ova(), A);
        assertTrue(gunes.katki(Katki.GUNES) > 0);
        assertEquals(0, OrtamSicakligi.hesapla(ova().gunSaati(GECE_YARISI), A).katki(Katki.GUNES), 1e-12);
    }

    @Test
    void kaynaklarYakindaIsitirSinirli() {
        assertEquals(40 * 0.25, OrtamSicakligi.kaynakKatkisi(40, 2.5, 5), 1e-9);
        assertEquals(0, OrtamSicakligi.kaynakKatkisi(40, 5, 5), 1e-12);
        assertEquals(0, OrtamSicakligi.kaynakKatkisi(40, 7, 5), 1e-12);
        double atesli = t(ova().gunSaati(GECE_YARISI).kaynaklar(25));
        assertEquals(t(ova().gunSaati(GECE_YARISI)) + 25, atesli, 1e-9);
        assertEquals(t(ova()) + A.kaynakUstSiniri(), t(ova().kaynaklar(500)), 1e-9);
        assertEquals(t(ova()) + A.kaynakAltSiniri(), t(ova().kaynaklar(-500)), 1e-9);
    }

    @Test
    void kaynakTanimiCozulur() {
        KaynakTanimi k = KaynakTanimi.coz("minecraft:lava=40:6");
        assertEquals("minecraft:lava", k.blok());
        assertEquals(40, k.guc(), 1e-12);
        assertEquals(6, k.yaricap(), 1e-12);
        KaynakTanimi buz = KaynakTanimi.coz(" minecraft:blue_ice = -10 : 4 ");
        assertEquals(-10, buz.guc(), 1e-12);
        assertThrows(IllegalArgumentException.class, () -> KaynakTanimi.coz("minecraft:lava"));
        assertThrows(IllegalArgumentException.class, () -> KaynakTanimi.coz("minecraft:lava=x:4"));
        assertThrows(IllegalArgumentException.class, () -> KaynakTanimi.coz("minecraft:lava=4:0"));
    }

    @Test
    void netherSicakEndSoguk() {
        assertTrue(t(ova().boyut(Boyut.NETHER)) > 45);
        assertTrue(t(ova().boyut(Boyut.END)) < 0);
        // Dünya dönemi Nether'i etkilemez.
        assertEquals(t(ova().boyut(Boyut.NETHER)),
                t(ova().boyut(Boyut.NETHER).evre(GunesEvresi.GUNESSIZ, 0).donma(100)), 1e-9);
    }

    @Test
    void gunesPatlamasiIsitirGolgedeDahaAz() {
        double normal = t(ova());
        double tepe = t(ova().evre(GunesEvresi.BUYUYEN, 1));
        double golge = t(ova().evre(GunesEvresi.BUYUYEN, 1).gokyuzu(false));
        double derin = t(ova().evre(GunesEvresi.BUYUYEN, 1).gokyuzu(false).y(5));
        assertTrue(tepe > normal + 30, "güneş patlaması tepe " + tepe);
        assertTrue(t(ova().evre(GunesEvresi.BUYUYEN, 0.5)) < tepe);
        assertTrue(golge < tepe - 10, "gölge " + golge);
        assertTrue(derin < golge, "derin sığınak " + derin);
    }

    @Test
    void supernovaSonrasiDonmaIlerledikceSogur() {
        double onceki = Double.POSITIVE_INFINITY;
        for (int g = 0; g <= 100; g += 25) {
            double d = t(ova().evre(GunesEvresi.GUNESSIZ, 0.5).donma(g));
            assertTrue(d < onceki, "G=" + g + ": " + d);
            onceki = d;
        }
        assertTrue(onceki <= IsiAlisverisi.Ayarlar.VARSAYILAN.asiriSogukEsigi(), "G=100 dondurucu: " + onceki);
        // Güneşsiz dönemde öğlen ile gece arasında güneş farkı yok.
        assertEquals(t(ova().evre(GunesEvresi.GUNESSIZ, 0).donma(50)),
                t(ova().evre(GunesEvresi.GUNESSIZ, 0).donma(50).gunSaati(GECE_YARISI)), 1e-9);
    }

    @Test
    void kizilCuceKismenToparlarAmaEskidenSogukKisBolgesiCokSoguk() {
        double eski = t(ova());
        double donmus = t(ova().evre(GunesEvresi.GUNESSIZ, 1).donma(100));
        double kizil = t(ova().evre(GunesEvresi.KIZIL_CUCE, 1).donma(40));
        double kis = t(ova().evre(GunesEvresi.KIZIL_CUCE, 1).donma(40).kisAgirligi(1));
        assertTrue(kizil > donmus + 15, "toparlanır " + kizil);
        assertTrue(kizil < eski - 15, "belirgin şekilde soğuk " + kizil);
        assertTrue(kis < kizil - 15, "kış bölgesi " + kis);
        assertTrue(kis <= IsiAlisverisi.Ayarlar.VARSAYILAN.asiriSogukEsigi(), "kış bölgesi dondurucu " + kis);
    }

    @Test
    void katkilarToplamaEsit() {
        OrtamSicakligi.Sonuc s = OrtamSicakligi.hesapla(ova().gunSaati(15000).y(150).yagis(Yagis.KAR).kaynaklar(5)
                .evre(GunesEvresi.KIZIL_CUCE, 0.3).donma(60).kisAgirligi(0.5), A);
        double toplam = 0;
        for (Katki k : Katki.values()) {
            toplam += s.katki(k);
        }
        assertEquals(s.toplam(), toplam, 1e-9);
    }
}
