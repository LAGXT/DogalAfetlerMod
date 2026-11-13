package io.github.kadirberkpolat1.dogalafetler.uyari;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrtamKimligiTest {
    @Test
    void gecerliKimlikAltSistemVeKatmandir() {
        assertTrue(OrtamKimligi.gecerli("kasirga/ugultu"));
        assertTrue(OrtamKimligi.gecerli("su/deniz.yakin-1"));
        assertFalse(OrtamKimligi.gecerli(null));
        assertFalse(OrtamKimligi.gecerli("ugultu"), "alt sistem yok");
        assertFalse(OrtamKimligi.gecerli("Kasirga/ugultu"), "büyük harf");
        assertFalse(OrtamKimligi.gecerli("kasırga/ugultu"), "ASCII dışı");
        assertFalse(OrtamKimligi.gecerli("kasirga/"), "boş katman");
    }

    /** Paket kimliği {@value OrtamKimligi#EN_UZUN} karakterle yazar; daha uzunu çağrı yerinde reddedilmeli (V5). */
    @Test
    void toplamUzunlukPaketSiniriniAsamaz() {
        String altSistem = "a".repeat(OrtamKimligi.EN_UZUN_ALT_SISTEM);
        String katman = "b".repeat(OrtamKimligi.EN_UZUN_KATMAN);
        String enUzun = altSistem + "/" + katman;
        assertEquals(OrtamKimligi.EN_UZUN, enUzun.length());
        assertTrue(OrtamKimligi.gecerli(enUzun));
        assertFalse(OrtamKimligi.gecerli(altSistem + "a/" + katman), "alt sistem 16 karakter");
        assertFalse(OrtamKimligi.gecerli(altSistem + "/" + katman + "b"), "katman 49 karakter");
        assertEquals(enUzun, OrtamKimligi.dogrula(enUzun));
        assertThrows(IllegalArgumentException.class, () -> OrtamKimligi.dogrula("a".repeat(40) + "/b"));
    }
}
