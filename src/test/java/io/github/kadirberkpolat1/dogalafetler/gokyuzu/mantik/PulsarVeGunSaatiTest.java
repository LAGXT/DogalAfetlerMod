package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.OrtamSicakligi;
import org.junit.jupiter.api.Test;

/** Gökyüzü-2 saf mantığı: pulsar seçimi, pulsar eğrileri ve ısısı, güneş patlamasında gün saati sabitleme. */
class PulsarVeGunSaatiTest {
    @Test
    void pulsarOlasiligiVarsayilanaYakinVeBelirlenimci() {
        int pulsar = 0;
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            long tohum = PulsarSecimi.tohum(20261105L, 24_000L * i + 7);
            boolean secim = PulsarSecimi.pulsarMi(PulsarSecimi.VARSAYILAN_OLASILIK, tohum);
            assertEquals(secim, PulsarSecimi.pulsarMi(PulsarSecimi.VARSAYILAN_OLASILIK, tohum), "aynı tohum aynı sonuç");
            if (secim) {
                pulsar++;
            }
        }
        double oran = pulsar / (double) n;
        assertTrue(Math.abs(oran - 0.15) < 0.01, "oran ~0,15: " + oran);
        assertTrue(PulsarSecimi.tohum(1L, 2L) != PulsarSecimi.tohum(2L, 2L), "başka dünya başka tohum");
    }

    @Test
    void pulsarOlasiligiUclari() {
        for (long t = 0; t < 500; t++) {
            assertFalse(PulsarSecimi.pulsarMi(0.0, t));
            assertTrue(PulsarSecimi.pulsarMi(1.0, t));
            assertFalse(PulsarSecimi.pulsarMi(Double.NaN, t));
            assertTrue(PulsarSecimi.pulsarMi(5.0, t), "aralık dışı kenetlenir");
        }
    }

    @Test
    void pulsarIsiniDonerNabizYalnizHizalanincaParlar() {
        assertEquals(0f, PulsarEgrileri.isinAcisi(0), 1e-5f);
        assertEquals(PulsarEgrileri.isinAcisi(1.0), PulsarEgrileri.isinAcisi(1.0 + PulsarEgrileri.DONUS_SN), 1e-4f);
        assertEquals(1f, PulsarEgrileri.nabiz(1f), 1e-6f);
        assertTrue(PulsarEgrileri.nabiz(0.8f) < 0.1f, "ışın bakıştan biraz sapınca nabız söner");
        assertEquals(0f, PulsarEgrileri.nabiz(-1f), 1e-6f);
        assertEquals(0f, PulsarEgrileri.yogunluk(0f), 1e-6f);
        assertEquals(1f, PulsarEgrileri.yogunluk(1f), 1e-6f);
    }

    @Test
    void pulsarDunyayiGunesPatlamasindanCokIsitir() {
        OrtamSicakligi.Ayarlar a = OrtamSicakligi.Ayarlar.VARSAYILAN;
        double tam = OrtamSicakligi.PULSAR_ARTIS_CARPANI * a.gunesPatlamasiArtisi();
        assertTrue(tam > a.gunesPatlamasiArtisi() && tam > 40, "pulsar ısısı güneş patlaması tepesini aşar: " + tam);
        assertTrue(GunesEvresi.values()[GunesEvresi.values().length - 1] == GunesEvresi.PULSAR,
                "PULSAR en sonda (paket sıra numarası değişmez)");
        assertEquals(GunesEvresi.PULSAR, GunesEvresi.kimlikIle("pulsar"));
    }

    @Test
    void gunSaatiIlkOgleyeIleriGecerVeTutulur() {
        assertEquals(6_000L, GunSaatiSabitleme.ilkHedef(1_000L));
        assertEquals(6_000L, GunSaatiSabitleme.ilkHedef(6_000L));
        assertEquals(30_000L, GunSaatiSabitleme.ilkHedef(18_000L), "gece: geri sarılmaz, ertesi öğle");
        assertEquals(78_000L, GunSaatiSabitleme.ilkHedef(72_001L));
        assertEquals(6_000L, GunSaatiSabitleme.tut(6_000L, 6_001L, true), "vanilla'nın +1'i geri alınır");
        assertEquals(6_000L, GunSaatiSabitleme.tut(6_000L, 6_000L, false));
        assertEquals(2_500L, GunSaatiSabitleme.tut(6_000L, 2_500L, true), "/time set yeni sabit olur");
        assertEquals(6_001L, GunSaatiSabitleme.tut(6_000L, 6_001L, false), "döngü kapalıyken +1 dış değişikliktir");
        long saat = GunSaatiSabitleme.ilkHedef(12_345L);
        for (int tick = 0; tick < 5 * 24_000; tick++) {
            saat = GunSaatiSabitleme.tut(saat, saat + 1, true);
        }
        assertEquals(30_000L, saat, "5 gün boyunca öğlede kalır");
    }
}
