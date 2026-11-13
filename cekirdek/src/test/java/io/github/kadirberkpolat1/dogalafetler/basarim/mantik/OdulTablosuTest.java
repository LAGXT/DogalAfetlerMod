package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OdulTablosuTest {
    @Test
    void meydanOkumaEnBuyukXp() {
        assertTrue(BasarimTuru.GOREV.varsayilanXp() < BasarimTuru.HEDEF.varsayilanXp());
        assertTrue(BasarimTuru.HEDEF.varsayilanXp() < BasarimTuru.MEYDAN_OKUMA.varsayilanXp());
        assertTrue(BasarimTuru.MEYDAN_OKUMA.varsayilanXp() <= 150, "madde 12: ödüller oyunu bozmayacak kadar küçük");
    }

    @Test
    void ayarFarkiVarsayilanaGore() {
        assertEquals(0, OdulTablosu.xpFarki(BasarimTuru.HEDEF, BasarimTuru.HEDEF.varsayilanXp()));
        assertEquals(10, OdulTablosu.xpFarki(BasarimTuru.GOREV, BasarimTuru.GOREV.varsayilanXp() + 10));
        assertEquals(-BasarimTuru.MEYDAN_OKUMA.varsayilanXp(), OdulTablosu.xpFarki(BasarimTuru.MEYDAN_OKUMA, 0),
                "0 XP ayarı JSON'un verdiğini geri alır");
    }

    @Test
    void hedefYiyecekSayisi() {
        assertEquals(1, OdulTablosu.HEDEF_YIYECEK_EN_AZ);
        assertEquals(2, OdulTablosu.HEDEF_YIYECEK_EN_COK);
    }
}
