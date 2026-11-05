package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SuAraliklariTest {
    @Test
    void ortusenAraliklarBirlesir() {
        SuAraliklari a = new SuAraliklari();
        a.ekle(63, 67);
        a.ekle(65, 71);
        a.ekle(10, 12);
        a.ekle(5, 5);
        a.birlestir();
        assertEquals(2, a.adet());
        assertEquals(10, a.alt(0));
        assertEquals(12, a.ust(0));
        assertEquals(63, a.alt(1));
        assertEquals(71, a.ust(1));
    }

    @Test
    void ustuBulIcindekiAraliginTepesiniVerir() {
        SuAraliklari a = new SuAraliklari();
        a.ekle(63, 67);
        a.birlestir();
        assertEquals(67, a.ustuBul(64.5));
        assertTrue(Double.isNaN(a.ustuBul(67.0)));
        assertTrue(Double.isNaN(a.ustuBul(62.9)));
    }

    @Test
    void batiklikOraniKutununSudaKalanPayidir() {
        SuAraliklari a = new SuAraliklari();
        a.ekle(64, 65.8);
        a.birlestir();
        assertEquals(1.0, a.batiklikOrani(64, 65.8), 1e-9);
        assertEquals(0.5, a.batiklikOrani(64.9, 66.7), 1e-9);
        assertEquals(0.0, a.batiklikOrani(66, 67.8), 1e-9);
    }
}
