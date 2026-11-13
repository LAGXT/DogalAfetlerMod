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
    void uzunActionBarSatiriPaneliYukariItar() {
        // Kanıt koşuları (468x243 GUI): leftHeight 74; action bar altın metninin pikselleri y 154..164 (aksanlı Ö dahil),
        // Hassas metni ~274 px geniş. Panel (133x94) bu satırın tamamen üstünde kalmalı.
        GostergeYerlesimi.Alt alt = GostergeYerlesimi.forgeAlt(74, 59, 274);
        assertEquals(76, alt.durumUst());
        assertEquals(91, alt.altYaziUst());
        Konum k = GostergeYerlesimi.konum(468, 243, 133, 94, Kose.SAG_ALT, 0, 0, alt, 0);
        assertEquals(468 - 133 - 4, k.x());
        assertTrue(k.y() + 94 < 154, "panel action bar satırına (y 154) iniyor: " + k);
        // Durum sütunu kısayken Forge vanilla yüksekliğini (68) korur.
        assertEquals(68 + 8, GostergeYerlesimi.forgeAlt(39, 39, 0).altYaziUst());
        // Kısa metin (yarı genişlik 121'e sığar) ve geniş ekran: panel köşede kalır.
        Konum genis = GostergeYerlesimi.konum(960, 540, 133, 94, Kose.SAG_ALT, 0, 0,
                GostergeYerlesimi.forgeAlt(81, 59, 150), 0);
        assertEquals(new Konum(960 - 133 - 4, 540 - 94 - 4), genis);
    }

    @Test
    void ustKoselerVeOfsetlerEkranIcindeKalir() {
        assertEquals(new Konum(4, 4), GostergeYerlesimi.konum(400, 200, 100, 50, Kose.SOL_UST, 0, 0));
        assertEquals(new Konum(400 - 100 - 4 - 10, 4 + 20), GostergeYerlesimi.konum(400, 200, 100, 50, Kose.SAG_UST, 10, 20));
        assertEquals(new Konum(0, 0), GostergeYerlesimi.konum(400, 200, 100, 50, Kose.SOL_UST, -50, -50));
        Konum buyuk = GostergeYerlesimi.konum(100, 60, 120, 80, Kose.SAG_ALT, 0, 0);
        assertEquals(new Konum(0, 0), buyuk);
    }

    @Test
    void gelismisPanelGuiOlcegi2deEkranYuksekligininYuzdeOtuzunuGecmez() {
        // R-18: GUI ölçeği 2'de 1280x720 (360), kanıt penceresi 1870x969 (484), 1920x1080 (540).
        int enCok = GostergeYerlesimi.panelYuksekligi(GostergeYerlesimi.EN_COK_ALT_SATIR);
        assertEquals(108, enCok);
        for (int ekran : new int[] {360, 484, 540}) {
            assertTrue(enCok <= GostergeYerlesimi.EN_COK_EKRAN_ORANI * ekran, "panel " + enCok + " / ekran " + ekran);
        }
        // Normal radar: yalnız kap ve üç satır (41 px); önceki Gelişmiş panel 124 px idi.
        assertEquals(41, GostergeYerlesimi.panelYuksekligi(0));
        assertTrue(GostergeYerlesimi.panelYuksekligi(6) < enCok);
    }

    @Test
    void sagUstteEfektIkonlarininAltinaIner() {
        assertEquals(0, GostergeYerlesimi.efektAlti(false, false));
        assertEquals(25, GostergeYerlesimi.efektAlti(true, false));
        assertEquals(51, GostergeYerlesimi.efektAlti(true, true));
        assertEquals(51, GostergeYerlesimi.efektAlti(false, true));
        GostergeYerlesimi.Alt alt = GostergeYerlesimi.VARSAYILAN;
        assertEquals(new Konum(960 - 120 - 4, 4), GostergeYerlesimi.konum(960, 540, 120, 41, Kose.SAG_UST, 0, 0, alt, 0));
        assertEquals(new Konum(960 - 120 - 4, 27), GostergeYerlesimi.konum(960, 540, 120, 41, Kose.SAG_UST, 0, 0, alt, 25));
        assertEquals(53, GostergeYerlesimi.konum(960, 540, 120, 41, Kose.SAG_UST, 0, 0, alt, 51).y());
        // Sol üstte ve altta efekt ikonu yoktur.
        assertEquals(4, GostergeYerlesimi.konum(960, 540, 120, 41, Kose.SOL_UST, 0, 0, alt, 51).y());
        assertEquals(540 - 41 - 4, GostergeYerlesimi.konum(960, 540, 120, 41, Kose.SAG_ALT, 0, 0, alt, 51).y());
    }
}
