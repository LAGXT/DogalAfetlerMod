package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Kara delik yutması koreografisinin kontrol noktaları (PL-4). */
class KaraDelikYutmaEgrileriTest {
    private static final float PARLAMA = PulsarZinciri.YUTMA_PARLAMA_SN;
    private static final float SON = PulsarZinciri.YUTMA_SN;

    @Test
    void sabitlerZincirle() {
        assertEquals(90f, SON, 0f);
        assertEquals(55f, PARLAMA, 0f);
        assertEquals(60f, KaraDelikYutmaEgrileri.YUTUM_SN, 0f);
        float aci = KaraDelikYutmaEgrileri.KARADELIK_ACI;
        assertTrue(aci >= 35f && aci <= 45f, "pulsar ile kara delik aynı kadrajda: " + aci);
    }

    @Test
    void karaDelikHizlanarakYaklasirSabitKalirKuculurVeSoner() {
        assertEquals(1.5f, KaraDelikYutmaEgrileri.karaDelikCapi(0f), 1e-5f);
        assertEquals(20f, KaraDelikYutmaEgrileri.karaDelikCapi(20f), 1e-3f);
        assertEquals(20f, KaraDelikYutmaEgrileri.karaDelikCapi(40f), 0f);
        assertEquals(20f, KaraDelikYutmaEgrileri.karaDelikCapi(59.9f), 0f);
        float onceki = 0f;
        float oncekiArtis = 0f;
        for (int i = 0; i <= 20; i++) {
            float c = KaraDelikYutmaEgrileri.karaDelikCapi(i);
            assertTrue(c >= onceki, "yaklaşırken büyür");
            if (i > 0) {
                assertTrue(c - onceki >= oncekiArtis - 1e-4f, "yaklaşma hızlanır: " + i);
                oncekiArtis = c - onceki;
            }
            onceki = c;
        }
        assertTrue(KaraDelikYutmaEgrileri.karaDelikCapi(10f) < 4f, "ilk yarıda hâlâ uzak");
        onceki = Float.MAX_VALUE;
        for (float s = 60f; s <= 90f; s += 1f) {
            float c = KaraDelikYutmaEgrileri.karaDelikCapi(s);
            assertTrue(c <= onceki, "uzaklaşırken küçülür");
            onceki = c;
        }
        assertTrue(KaraDelikYutmaEgrileri.karaDelikCapi(90f) < 2f);
        assertEquals(0f, KaraDelikYutmaEgrileri.karaDelikAlfa(0f), 1e-6f);
        assertEquals(1f, KaraDelikYutmaEgrileri.karaDelikAlfa(30f), 1e-6f);
        assertEquals(1f, KaraDelikYutmaEgrileri.karaDelikAlfa(70f), 1e-6f);
        assertEquals(0f, KaraDelikYutmaEgrileri.karaDelikAlfa(90f), 1e-6f, "90 sn'de sönmüş");
    }

    @Test
    void pulsarCekilirSpagettilesirTitresimZirveYapar() {
        assertEquals(0f, KaraDelikYutmaEgrileri.cekim(10f), 0f);
        assertEquals(KaraDelikYutmaEgrileri.KARADELIK_ACI, KaraDelikYutmaEgrileri.ayrim(0f), 0f);
        assertEquals(0f, KaraDelikYutmaEgrileri.spagetti(20f), 1e-6f);
        assertEquals(1f, KaraDelikYutmaEgrileri.spagetti(PARLAMA), 1e-6f);
        assertEquals(1f, KaraDelikYutmaEgrileri.titresimCarpani(5f), 1e-6f);
        assertEquals(2.5f, KaraDelikYutmaEgrileri.titresimCarpani(PARLAMA), 1e-5f);
        float onceki = -1f;
        for (float s = 0f; s <= 60f; s += 0.5f) {
            float c = KaraDelikYutmaEgrileri.cekim(s);
            assertTrue(c >= onceki && c <= 1f, "çekim monoton: " + s);
            onceki = c;
        }
        assertEquals(1f, KaraDelikYutmaEgrileri.cekim(60f), 1e-6f);
        assertEquals(0f, KaraDelikYutmaEgrileri.ayrim(60f), 1e-5f);
        assertTrue(KaraDelikYutmaEgrileri.ayrim(PARLAMA) < KaraDelikYutmaEgrileri.EN_BUYUK_CAP / 2f,
                "parlamada pulsar kara deliğin diskinde");
    }

    @Test
    void parlamaZarfiVePulsarYok() {
        assertEquals(0f, KaraDelikYutmaEgrileri.parlama(54.9f), 0f);
        assertEquals(0.5f, KaraDelikYutmaEgrileri.parlama(PARLAMA + 0.05f), 1e-3f, "0,1 sn'de tam");
        assertEquals(1f, KaraDelikYutmaEgrileri.parlama(PARLAMA + 0.1f), 1e-4f);
        assertEquals(1f, KaraDelikYutmaEgrileri.parlama(PARLAMA + 1.4f), 0f, "1,5 sn tepe");
        float onceki = 1f;
        for (float x = 1.5f; x < 5f; x += 0.25f) {
            float p = KaraDelikYutmaEgrileri.parlama(PARLAMA + x);
            assertTrue(p <= onceki && p >= 0f, "sönüm: " + x);
            onceki = p;
        }
        assertTrue(KaraDelikYutmaEgrileri.parlama(PARLAMA + 2.4f) < 0.5f);
        assertEquals(0f, KaraDelikYutmaEgrileri.parlama(60f), 0f, "60 sn'de bitmiş");
        assertEquals(1f, KaraDelikYutmaEgrileri.pulsarAlfa(50f), 0f);
        assertEquals(0f, KaraDelikYutmaEgrileri.pulsarAlfa(60f), 0f, "pulsar 60. sn'de yok");
        assertEquals(0f, KaraDelikYutmaEgrileri.pulsarAlfa(PARLAMA + 1.5f), 1e-6f, "parlamanın tepesinde yutuldu");
    }

    @Test
    void merceklenmeUzayVeGok() {
        assertEquals(0f, KaraDelikYutmaEgrileri.merceklenme(0f), 1e-6f);
        assertEquals(1f, KaraDelikYutmaEgrileri.merceklenme(30f), 1e-5f);
        assertTrue(KaraDelikYutmaEgrileri.merceklenme(10f) < KaraDelikYutmaEgrileri.merceklenme(18f));
        assertTrue(KaraDelikYutmaEgrileri.fotonHalkasi(30f) > KaraDelikYutmaEgrileri.fotonHalkasi(2f));
        assertEquals(1f, KaraDelikYutmaEgrileri.fotonHalkasi(PARLAMA + 1f), 1e-6f, "parlamada foton halkası tavan");
        for (float s = 60f; s <= 90f; s += 0.5f) {
            assertEquals(1f, KaraDelikYutmaEgrileri.uzayTabani(s), 0f, "60–90 sn uzay %100");
            assertEquals(1f, KaraDelikYutmaEgrileri.karanlik(s), 0f, "gök simsiyah");
            assertEquals(0f, KaraDelikYutmaEgrileri.kalinti(s), 0f, "kalıntı nebulası yok");
        }
        assertEquals(0f, KaraDelikYutmaEgrileri.uzayTabani(10f), 0f);
        assertEquals(1f, KaraDelikYutmaEgrileri.kalinti(10f), 0f);
        assertEquals(1f, KaraDelikYutmaEgrileri.morluk(20f), 0f);
        assertTrue(KaraDelikYutmaEgrileri.diskParlakligi(50f) > KaraDelikYutmaEgrileri.diskParlakligi(10f),
                "pulsar maddesiyle disk parlar");
        assertEquals(0f, KaraDelikYutmaEgrileri.diskParlakligi(90f), 1e-6f);
    }
}
