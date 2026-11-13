package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

/**
 * Radar göstergesinin ekrandaki yeri (GUI pikseli). Gösterge seçilen köşeye oturur; alt köşelerde vanilla HUD'un, sıcaklık
 * barının (XP barının üstü, sıcaklık §A, S-10) ve action bar'ın (sismograf metni) üstüne binmez: panel yatayda hotbar ve yan el
 * yuvasıyla örtüşürse hotbar'ın, orta durum sütunuyla (kalp, zırh, açlık, hava, sıcaklık barı, XP) örtüşürse onların, ortalanmış
 * action bar satırıyla örtüşürse o satırın üstüne kalkar. Forge action bar'ı durum sütununun hemen üstüne çizer
 * ({@code h - max(max(leftHeight, rightHeight) + 9, 68)}); bu yüksekliklerle genişlik {@link Alt} ile verilir. Sağ üst köşede
 * vanilla durum efekti ikonlarının ({@link #efektAlti}) altına iner. Sonuç her zaman ekranın içindedir. Panelin yüksekliği
 * de buradadır ({@link #panelYuksekligi}): madde 13a "küçük, ekranı kaplamayan".
 */
public final class GostergeYerlesimi {
    public enum Kose {
        SAG_ALT, SAG_UST, SOL_UST, SOL_ALT
    }

    public record Konum(int x, int y) {
    }

    /**
     * Alt kenardaki dolu bölgeler (ekranın altından, GUI pikseli).
     *
     * @param durumUst    orta durum sütununun üstü
     * @param altYaziUst  action bar satırının üstü
     * @param altYaziYari action bar satırının yarı genişliği (ortalanmış)
     */
    public record Alt(int durumUst, int altYaziUst, int altYaziYari) {
    }

    /** Ekran kenarından boşluk. */
    public static final int PAY = 4;
    /** Hotbar'ın yarı genişliği (182/2). */
    public static final int HOTBAR_YARI = 91;
    /** Hotbar'ın iki yanında yan el yuvası ve saldırı göstergesi için ek pay. */
    public static final int HOTBAR_YAN = 30;
    /** Hotbar ve yan el yuvasının üstü ve 4 px pay (ekranın altından). */
    public static final int HOTBAR_UST = 26;
    /** Vanilla action bar satırının üstü ({@code h - 68}) ve 4 px pay; Forge yüksekliği bilinmezken kullanılır. */
    public static final int ALT_YAZI_UST = 72;
    /** Orta durum sütununun üstü: iki kalp satırı + zırh/hava + sıcaklık barı + XP (ekranın altından). */
    public static final int DURUM_UST = 86;
    /** Panel: kenar iç boşluğu, radar kapının yarıçapı ve çapı (normal ve Gelişmiş aynı). */
    public static final int IC = 4;
    public static final int KAP_YARICAP = 16;
    public static final int KAP = 2 * KAP_YARICAP + 1;
    /** Gelişmiş radarın alt bölümü: ayıraç ve satır adımı (8 px yazı + 1 px gölge). */
    public static final int AYIRAC = 2;
    public static final int ALT_SATIR = 9;
    /** Gelişmiş radarın en çok alt satırı: şiddet + hareket, "Son afetler", 5 geçmiş. */
    public static final int EN_COK_ALT_SATIR = 7;
    /** Panel yüksekliğinin ekran yüksekliğine oranı üst sınırı (GUI ölçeği 2'de; R-18). */
    public static final double EN_COK_EKRAN_ORANI = 0.30;
    /** Forge yükseklikleri bilinmezken varsayılan alt bölgeler. */
    public static final Alt VARSAYILAN = new Alt(DURUM_UST, ALT_YAZI_UST, HOTBAR_YARI + HOTBAR_YAN);

    private GostergeYerlesimi() {
    }

    /** Panelin yüksekliği: üstte kap ve yanındaki üç satır, altında {@code altSatir} kısa satır (yalnız Gelişmiş). */
    public static int panelYuksekligi(int altSatir) {
        int alt = Math.max(0, altSatir);
        return IC + KAP + IC + (alt == 0 ? 0 : AYIRAC + alt * ALT_SATIR + 2);
    }

    /**
     * Vanilla durum efekti ikonlarının alt kenarı (ekranın üstünden): yararlılar y 1'de, zararlılar y 27'de, ikonlar 24 px
     * ({@code Gui.renderEffects}). İkon yoksa 0.
     */
    public static int efektAlti(boolean yararli, boolean zararli) {
        return zararli ? 27 + 24 : yararli ? 1 + 24 : 0;
    }

    public static Konum konum(int w, int h, int pw, int ph, Kose kose, int ofsetX, int ofsetY) {
        return konum(w, h, pw, ph, kose, ofsetX, ofsetY, VARSAYILAN, 0);
    }

    /**
     * Forge {@code ForgeGui.leftHeight/rightHeight}'tan: durum sütunu {@code max(left, right)}. Forge 1.20.1 action bar'ı
     * {@code h - max(max(left, right) + 9, 68)}'e taşır ve metni 4 px yukarıdan çizer; üstüne aksan (Ö, Ç) için 2 px ve 2 px pay.
     */
    public static Alt forgeAlt(int solYukseklik, int sagYukseklik, int altYaziGenisligi) {
        int durum = Math.max(solYukseklik, sagYukseklik);
        return new Alt(durum + 2, Math.max(durum + 9, 68) + 8, Math.max(HOTBAR_YARI + HOTBAR_YAN, altYaziGenisligi / 2 + 4));
    }

    /** @param efektAlti sağ üstteki efekt ikonlarının alt kenarı ({@link #efektAlti}); yalnız {@code SAG_UST}'te kullanılır */
    public static Konum konum(int w, int h, int pw, int ph, Kose kose, int ofsetX, int ofsetY, Alt alt, int efektAlti) {
        boolean sag = kose == Kose.SAG_ALT || kose == Kose.SAG_UST;
        boolean altta = kose == Kose.SAG_ALT || kose == Kose.SOL_ALT;
        int x = sag ? w - pw - PAY - ofsetX : PAY + ofsetX;
        int y = altta ? h - ph - PAY - ofsetY : PAY + ofsetY;
        if (altta) {
            int orta = w / 2;
            if (ortusur(x, pw, orta - HOTBAR_YARI - HOTBAR_YAN, orta + HOTBAR_YARI + HOTBAR_YAN)) {
                y = Math.min(y, h - HOTBAR_UST - ph);
            }
            if (ortusur(x, pw, orta - HOTBAR_YARI, orta + HOTBAR_YARI)) {
                y = Math.min(y, h - alt.durumUst() - ph);
            }
            if (ortusur(x, pw, orta - alt.altYaziYari(), orta + alt.altYaziYari())) {
                y = Math.min(y, h - alt.altYaziUst() - ph);
            }
        } else if (kose == Kose.SAG_UST && efektAlti > 0) {
            y = Math.max(y, efektAlti + 2);
        }
        x = Math.max(0, Math.min(x, w - pw));
        y = Math.max(0, Math.min(y, h - ph));
        return new Konum(x, y);
    }

    private static boolean ortusur(int x, int pw, int sol, int sag) {
        return x < sag && x + pw > sol;
    }
}
