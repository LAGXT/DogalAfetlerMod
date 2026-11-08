package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import org.junit.jupiter.api.Test;

/**
 * S3-10: bunalmadaki kırmızı kenar izi donmadaki buz kenarı kadar belirgin (aynı derinlikte en az onun opaklığında, her
 * nabız fazında), yanmada bunalmanın her anından yoğun.
 */
class EkranSiddetiTest {
    /** Varsayılan eşikler (S3-04). */
    private static final SicaklikDurumPaketi D = SicaklikDurumPaketi.KAPALI;
    /** Nabız periyodunun en uzunu 1200/70 tick; tüm fazlar bu aralıkta taranır. */
    private static final float PERIYOT = 1200f / 70f;

    @Test
    void bunalmaKirmiziIziDonmaBuzKenariKadarBelirgin() {
        float genislik = D.cokSicak() - D.sicak();
        assertEquals(genislik, D.cokSoguk() - D.donma(), 1e-4, "varsayılan eşiklerde iki durum eş genişlikte");
        for (float derinlik = 0f; derinlik < genislik; derinlik += 0.5f) {
            float buz = EkranSiddeti.buzOpaklik(D, D.cokSoguk() - derinlik, 0f);
            for (float zaman = 0f; zaman < PERIYOT; zaman += 0.25f) {
                float kirmizi = EkranSiddeti.kirmiziOpaklik(D, D.sicak() + derinlik, zaman);
                assertTrue(kirmizi >= buz - 1e-4f, "derinlik " + derinlik + " zaman " + zaman + ": kırmızı " + kirmizi
                        + " buz kenarının " + buz + " altında");
            }
        }
    }

    @Test
    void yanmadaKirmiziIzBunalmaninHerAnindanYogun() {
        float bunalmaEnCok = 0f;
        for (float v = D.sicak(); v < D.cokSicak(); v += 0.5f) {
            for (float zaman = 0f; zaman < PERIYOT; zaman += 0.25f) {
                bunalmaEnCok = Math.max(bunalmaEnCok, EkranSiddeti.kirmiziOpaklik(D, v, zaman));
            }
        }
        for (float v = D.cokSicak(); v <= 100f; v += 0.5f) {
            for (float zaman = 0f; zaman < PERIYOT; zaman += 0.25f) {
                float yanma = EkranSiddeti.kirmiziOpaklik(D, v, zaman);
                assertTrue(yanma > bunalmaEnCok, "vücut " + v + " zaman " + zaman + ": yanma " + yanma
                        + " bunalmanın en yüksek " + bunalmaEnCok + " değerini aşmalı");
            }
        }
    }

    @Test
    void izlerYalnizKendiDurumlarinda() {
        assertEquals(0f, EkranSiddeti.kirmiziOpaklik(D, D.sicak() - 0.01f, 0f), "bunalmanın altında kırmızı iz yok");
        assertEquals(0f, EkranSiddeti.buzOpaklik(D, D.cokSoguk() + 0.01f, 0f), "donmanın üstünde (üşüme) buz yok");
        assertEquals(0f, EkranSiddeti.buzOpaklik(D, 0f, 0f));
        assertEquals(0f, EkranSiddeti.kirmiziOpaklik(D, 0f, 0f));
    }
}
