package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DenizTakibiTest {
    @Test
    void yukselisVeTamamlanma() {
        assertFalse(DenizTakibi.yukseliyorMu(0, 0), "yükselme yok");
        assertFalse(DenizTakibi.yukseliyorMu(4, 0), "bekleme süresi: henüz yükselmedi");
        assertTrue(DenizTakibi.yukseliyorMu(4, 1.5f));
        assertFalse(DenizTakibi.yukseliyorMu(4, 4));
        assertFalse(DenizTakibi.tamamlandiMi(0, 0));
        assertFalse(DenizTakibi.tamamlandiMi(4, 3.9f));
        assertTrue(DenizTakibi.tamamlandiMi(4, 4));
        assertTrue(DenizTakibi.tamamlandiMi(4, 3.9995f), "float payı");
    }

    @Test
    void yukselisteOlenHakKazanmaz() {
        assertTrue(DenizTakibi.hakKazandiMi(DenizTakibi.OLUM_YOK, 1234));
        assertFalse(DenizTakibi.hakKazandiMi(1234, 1234));
        assertTrue(DenizTakibi.hakKazandiMi(1000, 1234), "önceki döngüdeki ölüm sayılmaz");
    }
}
