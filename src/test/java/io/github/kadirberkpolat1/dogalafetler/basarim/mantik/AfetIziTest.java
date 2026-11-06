package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class AfetIziTest {
    @Test
    void yeniAfetteSifirlanir() {
        AfetIzi iz = new AfetIzi();
        assertTrue(iz.afeteBagla(42));
        iz.suruklendi = true;
        iz.hasarAldi = true;
        assertFalse(iz.afeteBagla(42), "aynı afet: iz korunur");
        assertTrue(iz.suruklendi);
        assertTrue(iz.afeteBagla(43));
        assertFalse(iz.suruklendi);
        assertFalse(iz.hasarAldi);
        assertEquals(43, iz.no());
    }

    @Test
    void kosullar() {
        AfetIzi iz = new AfetIzi();
        iz.afeteBagla(1);
        assertEquals(EnumSet.of(SagCikmaKosulu.HASARSIZ), iz.kosullar(), "hasar almamış");
        iz.yanginSaniye = AfetIzi.YANGIN_SANIYESI - 1;
        assertFalse(iz.kosullar().contains(SagCikmaKosulu.YANGIN_ALANINDA));
        iz.yanginSaniye = AfetIzi.YANGIN_SANIYESI;
        iz.suruklendi = true;
        iz.cukuraDustu = true;
        iz.hasarAldi = true;
        assertEquals(EnumSet.of(SagCikmaKosulu.SURUKLENDI, SagCikmaKosulu.CUKURA_DUSTU,
                SagCikmaKosulu.YANGIN_ALANINDA), iz.kosullar());
    }

    @Test
    void kosulKimlikleri() {
        for (SagCikmaKosulu k : SagCikmaKosulu.values()) {
            assertEquals(k, SagCikmaKosulu.kimlikIle(k.kimlik().toUpperCase(java.util.Locale.ROOT)).orElseThrow());
        }
        assertTrue(SagCikmaKosulu.kimlikIle("yok").isEmpty());
    }
}
