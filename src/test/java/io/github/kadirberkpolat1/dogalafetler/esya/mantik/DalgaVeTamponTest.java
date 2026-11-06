package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import org.junit.jupiter.api.Test;

class DalgaVeTamponTest {
    @Test
    void halkaTamponEnEskiyiDusurur() {
        HalkaTampon t = new HalkaTampon(3);
        assertEquals(0, t.boyut());
        assertEquals(0f, t.son());
        t.ekle(1);
        t.ekle(2);
        assertEquals(2, t.boyut());
        assertEquals(1f, t.get(0));
        t.ekle(3);
        t.ekle(4);
        assertEquals(3, t.boyut());
        assertEquals(2f, t.get(0));
        assertEquals(4f, t.get(2));
        assertEquals(4f, t.son());
        assertThrows(IndexOutOfBoundsException.class, () -> t.get(3));
        t.temizle();
        assertEquals(0, t.boyut());
    }

    @Test
    void altmisSaniyelikTamponSabitBellekte() {
        HalkaTampon t = new HalkaTampon(1200);
        for (int i = 0; i < 5000; i++) {
            t.ekle(i);
        }
        assertEquals(1200, t.boyut());
        assertEquals(3800f, t.get(0));
        assertEquals(4999f, t.son());
    }

    @Test
    void sutunlarYeniVeriSagdaBosKisimNaN() {
        HalkaTampon t = new HalkaTampon(8);
        t.ekle(-1);
        t.ekle(3);
        t.ekle(0.5f);
        float[] mn = new float[4];
        float[] mx = new float[4];
        t.sutunlar(4, mn, mx);
        // 8 örnek / 4 sütun = sütun başına 2; ilk 5 yer boş: sütun 0, 1 boş; sütun 2 = [boş, -1]; sütun 3 = [3, 0.5].
        assertTrue(Float.isNaN(mn[0]) && Float.isNaN(mx[1]));
        assertEquals(-1f, mn[2]);
        assertEquals(-1f, mx[2]);
        assertEquals(0.5f, mn[3]);
        assertEquals(3f, mx[3]);
        float[] genis = new float[16];
        float[] genis2 = new float[16];
        t.sutunlar(16, genis, genis2);
        assertEquals(0.5f, genis[15]);
        assertFalse(Float.isNaN(genis[10]));
    }

    @Test
    void dalgaSinirliVeDeterministik() {
        for (long tick = 0; tick < 5000; tick++) {
            double v = SismikDalga.ornek(tick, 77);
            assertTrue(v >= -1 && v <= 1);
            assertEquals(v, SismikDalga.ornek(tick, 77));
        }
    }

    @Test
    void genlikSakindeKucukOncudeDarbeyleAnaSarsintidaBuyuk() {
        assertEquals(SismikDalga.TABAN, SismikDalga.genlik(false, AfetAsamasi.AKTIF, 1, 1), 1e-12);
        double sakinDarbe = SismikDalga.genlik(true, AfetAsamasi.ON_UYARI, 0.5, 0);
        double tepeDarbe = SismikDalga.genlik(true, AfetAsamasi.ON_UYARI, 0.5, 1);
        double ana = SismikDalga.genlik(true, AfetAsamasi.AKTIF, 0.5, 0);
        assertTrue(SismikDalga.TABAN < sakinDarbe && sakinDarbe < tepeDarbe && tepeDarbe <= ana);
        assertTrue(SismikDalga.genlik(true, AfetAsamasi.SIREN, 1, 1) <= 1);
    }

    @Test
    void hareketIziYonVeHiz() {
        HareketIzi iz = new HareketIzi(5);
        assertTrue(iz.yon().isEmpty());
        iz.ekle(1, 0, 0, 0);
        iz.ekle(1, 20, 0.5, 0);
        assertTrue(iz.yon().isEmpty(), "bir bloktan az: durağan");
        iz.ekle(1, 40, 6, 0);
        assertEquals(90, iz.yon().getAsDouble(), 1e-9);
        assertEquals(3, iz.hiz(), 1e-9);
        for (int i = 3; i < 10; i++) {
            iz.ekle(1, i * 20L, 6, -4.0 * i);
        }
        assertEquals(0, iz.yon().getAsDouble(), 1e-9);
        assertEquals(4, iz.hiz(), 1e-9);
        iz.ekle(2, 400, 100, 100);
        assertTrue(iz.yon().isEmpty(), "yeni afette iz sıfırlanır");
    }
}
