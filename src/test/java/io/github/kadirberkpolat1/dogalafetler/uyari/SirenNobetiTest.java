package io.github.kadirberkpolat1.dogalafetler.uyari;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SirenNobetiTest {
    @Test
    void motorCalarkenYenidenCalmaz() {
        SirenNobeti nobet = new SirenNobeti();
        for (int i = 0; i < 100; i++) {
            assertFalse(nobet.yenidenCalinmali(false, false, true));
        }
    }

    @Test
    void motorKestiyseHemenYenidenCalar() {
        SirenNobeti nobet = new SirenNobeti();
        assertFalse(nobet.yenidenCalinmali(false, false, true));
        assertTrue(nobet.yenidenCalinmali(false, false, false), "boyut değişimi ya da /stopsound sonrası ilk tick");
    }

    @Test
    void kendiDurdurdugumuzVeBitenSireniYenidenCalmaz() {
        SirenNobeti nobet = new SirenNobeti();
        assertFalse(nobet.yenidenCalinmali(true, false, false), "ses örneğini zarf bitince biz durdurduk");
        assertFalse(nobet.yenidenCalinmali(false, true, false), "zarf bitti");
    }

    @Test
    void calamayanMotordaHerTickDenemez() {
        SirenNobeti nobet = new SirenNobeti();
        assertTrue(nobet.yenidenCalinmali(false, false, false));
        for (int i = 1; i < SirenNobeti.DENEME_ARALIGI_TICK; i++) {
            assertFalse(nobet.yenidenCalinmali(false, false, false), "deneme aralığı dolmadan (tick " + i + ")");
        }
        assertTrue(nobet.yenidenCalinmali(false, false, false));
    }
}
