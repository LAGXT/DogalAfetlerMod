package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Madde 15 risk/denge: buz üstünde hareketsiz balık tutan oyuncunun ortamına giderek güçlenen soğuk kaynak. */
class HareketsizlikSogumasiTest {
    private final HareketsizlikSogumasi.Ayarlar a = new HareketsizlikSogumasi.Ayarlar(8.0, 20);

    @Test
    void hareketliykenSogutmaz() {
        assertEquals(0.0, HareketsizlikSogumasi.guc(0, a));
    }

    @Test
    void giderekGuclenirVeTavandaKalir() {
        double g5 = HareketsizlikSogumasi.guc(5, a);
        double g10 = HareketsizlikSogumasi.guc(10, a);
        assertTrue(g5 < 0 && g10 < g5, "negatif ve giderek güçlenen");
        assertEquals(-8.0, HareketsizlikSogumasi.guc(20, a), 1e-9);
        assertEquals(-8.0, HareketsizlikSogumasi.guc(600, a), 1e-9);
        assertEquals(-4.0, g10, 1e-9);
    }

    @Test
    void hareketsizlikEsigi() {
        assertTrue(HareketsizlikSogumasi.hareketsizMi(0.0));
        assertTrue(HareketsizlikSogumasi.hareketsizMi(0.2 * 0.2));
        assertFalse(HareketsizlikSogumasi.hareketsizMi(0.5 * 0.5), "saniyede yarım blok yürümek hareket");
    }

    @Test
    void sayacHareketteSifirlanir() {
        assertEquals(4, HareketsizlikSogumasi.sonrakiSayac(3, true));
        assertEquals(0, HareketsizlikSogumasi.sonrakiSayac(30, false));
    }

    @Test
    void kapaliAyarSogutmaz() {
        assertEquals(0.0, HareketsizlikSogumasi.guc(100, new HareketsizlikSogumasi.Ayarlar(0.0, 20)));
    }
}
