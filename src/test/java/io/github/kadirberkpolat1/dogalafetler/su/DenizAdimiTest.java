package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DenizAdimiTest {
    @Test
    void yaklasHedefiAsmazVeIkiYoneCalisir() {
        double hiz = 4.0 / 6000;
        float m = 0f;
        for (int i = 0; i < 3000; i++) {
            m = DenizAdimi.yaklas(m, 4f, hiz, 1);
        }
        assertEquals(2f, m, 1e-3, "yarı sürede yarı artış");
        assertEquals(4f, DenizAdimi.yaklas(m, 4f, hiz, 100_000), 0f, "hedef aşılmaz");
        assertEquals(3.5f, DenizAdimi.yaklas(4f, 0f, 0.5, 1), 1e-6, "geri dönüş");
        assertEquals(0f, DenizAdimi.yaklas(0.2f, 0f, 0.5, 1), 0f);
        assertEquals(1.5f, DenizAdimi.yaklas(1.5f, 4f, hiz, 0), 0f, "adım 0 (hız 0 ya da durmuş saat)");
    }

    @Test
    void hedefUstSiniraKirpilir() {
        assertEquals(6f, DenizAdimi.hedef(9f, 6), 0f);
        assertEquals(4f, DenizAdimi.hedef(4f, 6), 0f);
        assertEquals(0f, DenizAdimi.hedef(-1f, 6), 0f);
        assertEquals(8f, DenizAdimi.hedef(9f, 99), 0f, "üst sınır en çok " + DenizAlani.EN_COK_ARTIS);
    }

    @Test
    void gercekDakikaOlcekliTickeCevrilir() {
        assertEquals(12_000L, DenizAdimi.dakikaTick(10));
        assertEquals(6_000L, DenizAdimi.dakikaTick(5));
    }

    @Test
    void buharlasmaYukselmeyiKademeliSifiraIndirirVeGeriGetirmez() {
        float taban = 4f;
        assertEquals(4f, DenizAdimi.buharlasmaSonrasi(4f, taban, 0f), 0f);
        assertEquals(2.5f, DenizAdimi.buharlasmaSonrasi(4f, taban, 1.5f), 1e-6);
        assertEquals(0f, DenizAdimi.buharlasmaSonrasi(2.5f, taban, 5f), 0f);
        assertEquals(0f, DenizAdimi.buharlasmaSonrasi(0f, taban, 1f), 0f, "buharlaşma azalınca yükselme geri gelmez");
        assertTrue(DenizAdimi.buharlasmaSonrasi(3f, taban, 0.5f) <= 3f, "monoton");
    }
}
