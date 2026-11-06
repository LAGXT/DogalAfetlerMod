package io.github.kadirberkpolat1.dogalafetler.uyari;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SirenNobetiTest {
    @Test
    void motorCalarkenYenidenCalmaz() {
        SirenNobeti nobet = new SirenNobeti();
        for (int i = 0; i < 100; i++) {
            assertFalse(nobet.yenidenCalinmali(false, false, true, false));
        }
    }

    @Test
    void motorKestiyseHemenYenidenCalar() {
        SirenNobeti nobet = new SirenNobeti();
        assertFalse(nobet.yenidenCalinmali(false, false, true, false));
        assertTrue(nobet.yenidenCalinmali(false, false, false, false), "boyut değişimi ya da /stopsound sonrası ilk tick");
    }

    @Test
    void kendiDurdurdugumuzVeBitenSireniYenidenCalmaz() {
        SirenNobeti nobet = new SirenNobeti();
        assertFalse(nobet.yenidenCalinmali(true, false, false, false), "ses örneğini zarf bitince biz durdurduk");
        assertFalse(nobet.yenidenCalinmali(false, true, false, false), "zarf bitti");
    }

    @Test
    void calamayanMotordaHerTickDenemez() {
        SirenNobeti nobet = new SirenNobeti();
        assertTrue(nobet.yenidenCalinmali(false, false, false, false));
        for (int i = 1; i < SirenNobeti.DENEME_ARALIGI_TICK; i++) {
            assertFalse(nobet.yenidenCalinmali(false, false, false, false), "deneme aralığı dolmadan (tick " + i + ")");
        }
        assertTrue(nobet.yenidenCalinmali(false, false, false, false));
    }

    /**
     * V4: Ana Ses 0 iken vanilla sesi hiç başlatmaz, örnek motorda görünmez; bu "motor kesti" sayılmaz, nöbet denemez
     * (log kirlenmez). Ana Ses açılınca ilk tick'te yeniden çalar.
     */
    @Test
    void anaSesKapaliykenDenemezAcilincaHemenCalar() {
        SirenNobeti nobet = new SirenNobeti();
        for (int i = 0; i < 100; i++) {
            assertFalse(nobet.yenidenCalinmali(false, false, false, true));
        }
        assertTrue(nobet.yenidenCalinmali(false, false, false, false), "Ana Ses açıldı");
    }

    /**
     * V2: boyut değişimi ({@code SoundManager.stop()}) zarfı sıfırlamaz. Kesildikten sonra yeni örneğin çaldığı her tick'te
     * kalan süre = kesilmeden önceki kalan − geçen tick ve düzey kesilmeden öncekiyle aynıdır (açılış tekrarlanmaz); süre
     * dolunca zarf kendiliğinden sönmez. {@code SirenSesi.devam()}'ın aynı zarfı paylaşması istemci log'unda
     * ({@code zarf korundu t=…}) ve kayıtta ölçülür; bu test zarf tarafını sınar.
     */
    @Test
    void boyutDegisimindeZarfAyniDuzeyVeKalanSureyleSurer() {
        SirenZarfi zarf = new SirenZarfi(SirenProfili.STANDART, 200, 200);
        SirenNobeti nobet = new SirenNobeti();
        for (int i = 0; i < 80; i++) {
            zarf.ilerle();
            assertFalse(nobet.yenidenCalinmali(false, zarf.bitti(), true, false));
        }
        float kesilmeOncesi = zarf.duzey();
        int kalanOncesi = zarf.kalanTick();
        assertEquals(1f, kesilmeOncesi, 1e-6f);
        assertTrue(nobet.yenidenCalinmali(false, zarf.bitti(), false, false), "motor kesti: aynı zarfla yeniden çal");
        for (int gecen = 1; gecen <= kalanOncesi; gecen++) {
            zarf.ilerle();
            assertEquals(kalanOncesi - gecen, zarf.kalanTick(), "kalan = önceki kalan − geçen (tick " + gecen + ")");
            assertEquals(kesilmeOncesi, zarf.duzey(), 1e-6f, "düzey kesilmeden öncekiyle aynı (tick " + gecen + ")");
            assertFalse(nobet.yenidenCalinmali(false, zarf.bitti(), true, false));
        }
        assertEquals(0, zarf.kalanTick());
        assertFalse(zarf.sonduruluyor(), "sürenin sonunu sunucunun durdurması belirler (U-01)");
    }
}
