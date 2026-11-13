package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** S3-03: ıslanma ve kuruma. */
class IslaklikTest {
    private static final Islaklik.Ayarlar A = Islaklik.Ayarlar.VARSAYILAN;

    /** Sabit koşulda 1 Hz adımla {@code saniye} sonraki ıslaklık. */
    private static double benzet(double w, Islaklik.Kaynak k, double mekan, double kaynaklar, int saniye) {
        for (int i = 0; i < saniye; i++) {
            w = Islaklik.adim(w, k, false, mekan, kaynaklar, 1.0, A);
        }
        return w;
    }

    /** Kuru yerde tamamen kurumaya kaç saniye gerektiği. */
    private static int kurumaSuresi(double mekan, double kaynaklar) {
        double w = 1;
        int s = 0;
        while (w > 0 && s < 100_000) {
            w = Islaklik.adim(w, Islaklik.Kaynak.KURU, false, mekan, kaynaklar, 1.0, A);
            s++;
        }
        return s;
    }

    @Test
    void sudaDortSaniyedeDolar() {
        assertEquals(0.5, benzet(0, Islaklik.Kaynak.SU, 10, 0, 2), 1e-9);
        assertEquals(1.0, benzet(0, Islaklik.Kaynak.SU, 10, 0, 4), 1e-9);
        assertEquals(1.0, benzet(0, Islaklik.Kaynak.SU, 10, 0, 60), 1e-9, "1'i aşmaz");
    }

    @Test
    void yagmurdaYavasFirtinadaDahaHizliIslanir() {
        double yagmur = benzet(0, Islaklik.Kaynak.YAGMUR, 10, 0, 30);
        double firtina = benzet(0, Islaklik.Kaynak.FIRTINA, 10, 0, 30);
        assertTrue(yagmur > 0.3 && yagmur < 0.5, "30 sn yağmur: " + yagmur);
        assertTrue(firtina > yagmur);
        assertTrue(benzet(0, Islaklik.Kaynak.YAGMUR, 10, 0, 80) < 1, "~83 sn'den önce dolmaz");
        assertEquals(1.0, benzet(0, Islaklik.Kaynak.YAGMUR, 10, 0, 90), 1e-9);
    }

    @Test
    void islanirkenKurumaYok() {
        double w = Islaklik.adim(0.5, Islaklik.Kaynak.YAGMUR, false, 40, 30, 1.0, A);
        assertTrue(w > 0.5, "sıcakta ve ateş başında da yağmur ıslatır: " + w);
    }

    @Test
    void sicaktaHizliSoguktaYavasKurur() {
        int on = kurumaSuresi(10, 0);
        int sifir = kurumaSuresi(0, 0);
        int otuz = kurumaSuresi(30, 0);
        assertEquals(150, on, 1);
        assertEquals(300, sifir, 1);
        assertEquals(75, otuz, 1);
        assertTrue(kurumaSuresi(-20, 0) > sifir, "dondurucuda daha da yavaş");
        assertTrue(kurumaSuresi(-20, 0) < 100_000, "ama kurur (taban oran)");
    }

    @Test
    void atesYanindaHizliKurur() {
        int atessiz = kurumaSuresi(10, 0);
        int atesli = kurumaSuresi(10, 20);
        assertTrue(atesli * 2 < atessiz, atesli + " / " + atessiz);
        assertEquals(Islaklik.kurumaHizi(10, 0, A), Islaklik.kurumaHizi(10, -15, A), 1e-12,
                "soğuk kaynak kurumayı yavaşlatmaz");
    }

    @Test
    void yanarkenAnindaKurur() {
        assertEquals(0, Islaklik.adim(1, Islaklik.Kaynak.KURU, true, 0, 0, 1.0, A), 1e-12);
        assertEquals(0, Islaklik.adim(0.7, Islaklik.Kaynak.YAGMUR, true, 0, 0, 1.0, A), 1e-12);
    }

    @Test
    void gorunurlukVeKalanSure() {
        assertTrue(Islaklik.gorunur(0.05));
        assertFalse(Islaklik.gorunur(0.049));
        double hiz = Islaklik.kurumaHizi(10, 0, A);
        assertEquals(75, Islaklik.kalanSaniye(0.5, hiz), 1e-6);
        assertEquals(-1, Islaklik.kalanSaniye(0.5, 0), 1e-12, "ıslanırken kurumaz");
        assertEquals(0, Islaklik.kalanSaniye(0, hiz), 1e-12);
    }
}
