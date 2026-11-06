package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ZamanOlcegiTest {
    private static long topla(ZamanOlcegi olcek, int tick) {
        long toplam = 0;
        for (int i = 0; i < tick; i++) {
            toplam += olcek.ilerle();
        }
        return toplam;
    }

    @Test
    void normalHizdaHerTickBirAdim() {
        ZamanOlcegi olcek = new ZamanOlcegi();
        assertEquals(1.0, olcek.carpan());
        assertEquals(1000, topla(olcek, 1000));
    }

    @Test
    void tamsayiVeKesirliCarpanlarBirikir() {
        ZamanOlcegi olcek = new ZamanOlcegi();
        olcek.ayarla(10);
        assertEquals(10, olcek.ilerle());
        olcek.ayarla(2.5);
        assertEquals(10, topla(olcek, 4));
        olcek.ayarla(0.5);
        assertEquals(5, topla(olcek, 10));
    }

    @Test
    void gercekTickOlcekliSureyiCevirir() {
        ZamanOlcegi olcek = new ZamanOlcegi();
        assertEquals(200, olcek.gercekTick(200));
        olcek.ayarla(10);
        assertEquals(20, olcek.gercekTick(200));
        assertEquals(1, olcek.gercekTick(3));
        assertEquals(0, olcek.gercekTick(0));
        assertEquals(-1, olcek.gercekTick(-1), "bilinmeyen süre (-1) korunur");
    }

    @Test
    void sinirlarIcindeTutulurVeSifirlanir() {
        ZamanOlcegi olcek = new ZamanOlcegi();
        olcek.ayarla(1e9);
        assertEquals(ZamanOlcegi.EN_COK, olcek.carpan());
        olcek.ayarla(0);
        assertEquals(ZamanOlcegi.EN_AZ, olcek.carpan());
        assertThrows(IllegalArgumentException.class, () -> olcek.ayarla(Double.NaN));
        olcek.sifirla();
        assertEquals(1.0, olcek.carpan());
    }
}
