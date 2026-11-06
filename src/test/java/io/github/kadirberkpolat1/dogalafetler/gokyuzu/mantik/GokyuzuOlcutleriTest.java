package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import org.junit.jupiter.api.Test;

class GokyuzuOlcutleriTest {
    @Test
    void geceVanillaGunSaatiAraligi() {
        assertFalse(GokyuzuOlcutleri.geceMi(12_999));
        assertTrue(GokyuzuOlcutleri.geceMi(13_000), "gece başı");
        assertTrue(GokyuzuOlcutleri.geceMi(18_000));
        assertTrue(GokyuzuOlcutleri.geceMi(22_999));
        assertFalse(GokyuzuOlcutleri.geceMi(23_000), "şafak gece sayılmaz");
        assertFalse(GokyuzuOlcutleri.geceMi(6_000));
        assertTrue(GokyuzuOlcutleri.geceMi(24_000L * 7 + 18_000), "çok günlü saat");
        assertTrue(GokyuzuOlcutleri.geceMi(-6_000), "negatif saat (18000)");
    }

    @Test
    void yildizliGeceAyYokGeceVeGokyuzuGorulurken() {
        assertTrue(GokyuzuOlcutleri.yildizliGece(false, 18_000, true));
        assertFalse(GokyuzuOlcutleri.yildizliGece(true, 18_000, true), "ay varken sayılmaz");
        assertFalse(GokyuzuOlcutleri.yildizliGece(false, 18_000, false), "çatı altında sayılmaz");
        assertFalse(GokyuzuOlcutleri.yildizliGece(false, 6_000, true),
                "gündüz sayılmaz (güneşsiz dönemde gündüz yıldız görünse de)");
        assertFalse(GokyuzuOlcutleri.yildizliGece(false, 23_500, true), "şafak sayılmaz");
    }

    @Test
    void supernovaTanigiYalnizSupernovadaGokyuzuGorulurken() {
        assertTrue(GokyuzuOlcutleri.supernovaTanigi(GunesEvresi.SUPERNOVA, true));
        assertFalse(GokyuzuOlcutleri.supernovaTanigi(GunesEvresi.SUPERNOVA, false));
        for (GunesEvresi evre : GunesEvresi.values()) {
            if (evre != GunesEvresi.SUPERNOVA) {
                assertFalse(GokyuzuOlcutleri.supernovaTanigi(evre, true), evre.kimlik());
            }
        }
    }
}
