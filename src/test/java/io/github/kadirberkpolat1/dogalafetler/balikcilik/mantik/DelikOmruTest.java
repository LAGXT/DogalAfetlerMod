package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Madde 15: delik 4–5 dk sonra üç donma aşamasıyla eski buza döner; chunk başına sınır. */
class DelikOmruTest {
    private final DelikOmru.Ayarlar a = new DelikOmru.Ayarlar(240 * 20, 300 * 20, 15 * 20);

    @Test
    void omurAraliginIcindeVeUclariKapsar() {
        assertEquals(4800, DelikOmru.omurTick(0.0, a));
        assertEquals(6000, DelikOmru.omurTick(0.999_999, a));
        for (int i = 0; i < 1000; i++) {
            int t = DelikOmru.omurTick(i / 1000.0, a);
            assertTrue(t >= 4800 && t <= 6000, "aralık dışı " + t);
        }
    }

    @Test
    void asamalarinToplamiOmreEsit() {
        for (double u : new double[] {0, 0.3, 0.999}) {
            int omur = DelikOmru.omurTick(u, a);
            int toplam = DelikOmru.ilkGecikme(omur, a);
            for (int asama = 0; asama < DelikOmru.SON_ASAMA; asama++) {
                toplam += DelikOmru.asamaGecikmesi(a);
            }
            assertEquals(omur, toplam, "delik tam ömründe donmalı");
        }
        assertEquals(4800 - 3 * 300, DelikOmru.ilkGecikme(4800, a));
    }

    @Test
    void asamaSuresiKisaOmurdeOmrunDortteBirineKisilir() {
        DelikOmru.Ayarlar kisa = new DelikOmru.Ayarlar(400, 800, 300);
        assertEquals(100, DelikOmru.asamaGecikmesi(kisa));
        assertEquals(100, DelikOmru.ilkGecikme(400, kisa));
        assertEquals(500, DelikOmru.ilkGecikme(800, kisa), "uzun ömürde de aşamalar en kısa ömürden");
    }

    @Test
    void sonAsamadanSonraBuzaDoner() {
        assertFalse(DelikOmru.buzaDonerMi(0));
        assertFalse(DelikOmru.buzaDonerMi(2));
        assertTrue(DelikOmru.buzaDonerMi(DelikOmru.SON_ASAMA));
    }

    @Test
    void sifirDonmadaSuyaAksiHaldeBuzaDoner() {
        assertTrue(DelikOmru.suyaDonerMi(0.0), "sıfırlanmış dünyada su");
        assertFalse(DelikOmru.suyaDonerMi(0.01), "donma sürerken eski buz");
        assertFalse(DelikOmru.suyaDonerMi(40.0), "kızıl cüce tabanında eski buz (bilinen sınır, B-20)");
    }

    @Test
    void gecersizAyarReddedilir() {
        assertThrows(IllegalArgumentException.class, () -> new DelikOmru.Ayarlar(600, 400, 20));
        assertThrows(IllegalArgumentException.class, () -> new DelikOmru.Ayarlar(0, 400, 20));
    }

    @Test
    void chunkSiniri() {
        assertTrue(DelikOmru.acilabilirMi(0, 4));
        assertTrue(DelikOmru.acilabilirMi(3, 4));
        assertFalse(DelikOmru.acilabilirMi(4, 4), "sınırda yeni delik açılmaz");
        assertFalse(DelikOmru.acilabilirMi(0, 0), "sınır 0: hiç açılmaz");
    }
}
