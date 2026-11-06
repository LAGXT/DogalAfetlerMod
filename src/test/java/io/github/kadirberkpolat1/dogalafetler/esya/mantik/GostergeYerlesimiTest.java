package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.esya.mantik.GostergeYerlesimi.Kose;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.GostergeYerlesimi.Konum;
import org.junit.jupiter.api.Test;

class GostergeYerlesimiTest {
    @Test
    void genisEkrandaSagAltKoseyeOturur() {
        // 1920x1080, GUI ölçeği 2: 960x540; sağ boşluk hotbar sütunundan uzak.
        Konum k = GostergeYerlesimi.konum(960, 540, 110, 54, Kose.SAG_ALT, 0, 0);
        assertEquals(new Konum(960 - 110 - 4, 540 - 54 - 4), k);
    }

    @Test
    void darEkrandaActionBarSatirininUstuneKalkar() {
        // 1280x720, ölçek 3: 426x240; panel yan el/saldırı göstergesi sütununa giriyor: action bar (sismograf) üstte kalır.
        Konum k = GostergeYerlesimi.konum(426, 240, 110, 54, Kose.SAG_ALT, 0, 0);
        assertEquals(426 - 110 - 4, k.x());
        assertEquals(240 - GostergeYerlesimi.ALT_YAZI_UST - 54, k.y());
        assertTrue(k.y() + 54 <= 240 - 68 - 4, "action bar satırıyla örtüşür");
        assertTrue(k.x() >= 426 / 2 + GostergeYerlesimi.HOTBAR_YARI, "orta durum sütunu (sıcaklık barı) ile örtüşmez");
    }

    @Test
    void ortaSutunlaOrtusurseDurumSatirlarininUstuneKalkar() {
        Konum k = GostergeYerlesimi.konum(300, 240, 140, 60, Kose.SAG_ALT, 0, 0);
        assertEquals(240 - GostergeYerlesimi.DURUM_UST - 60, k.y());
        Konum sol = GostergeYerlesimi.konum(300, 240, 140, 60, Kose.SOL_ALT, 0, 0);
        assertEquals(4, sol.x());
        assertEquals(240 - GostergeYerlesimi.DURUM_UST - 60, sol.y());
    }

    @Test
    void ustKoselerVeOfsetlerEkranIcindeKalir() {
        assertEquals(new Konum(4, 4), GostergeYerlesimi.konum(400, 200, 100, 50, Kose.SOL_UST, 0, 0));
        assertEquals(new Konum(400 - 100 - 4 - 10, 4 + 20), GostergeYerlesimi.konum(400, 200, 100, 50, Kose.SAG_UST, 10, 20));
        assertEquals(new Konum(0, 0), GostergeYerlesimi.konum(400, 200, 100, 50, Kose.SOL_UST, -50, -50));
        Konum buyuk = GostergeYerlesimi.konum(100, 60, 120, 80, Kose.SAG_ALT, 0, 0);
        assertEquals(new Konum(0, 0), buyuk);
    }
}
