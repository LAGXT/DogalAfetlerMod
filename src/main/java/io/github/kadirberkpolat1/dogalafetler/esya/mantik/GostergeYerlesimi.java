package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

/**
 * Radar göstergesinin ekrandaki yeri (GUI pikseli). Gösterge seçilen köşeye oturur; alt köşelerde vanilla HUD'un, sıcaklık
 * barının (XP barının üstü, sıcaklık §A, S-10) ve action bar'ın (sismograf metni) üstüne binmez: panel yatayda hotbar ve yan el
 * yuvasıyla örtüşürse hotbar'ın, orta durum sütunuyla (kalp, zırh, açlık, hava, sıcaklık barı, XP) örtüşürse onların, ortalanmış
 * action bar satırıyla örtüşürse o satırın üstüne kalkar. Forge action bar'ı durum sütununun hemen üstüne çizer
 * ({@code h - max(leftHeight, rightHeight) - 4}); bu yüksekliklerle genişlik {@link Alt} ile verilir. Sonuç her zaman ekranın
 * içindedir.
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
    /** Forge yükseklikleri bilinmezken varsayılan alt bölgeler. */
    public static final Alt VARSAYILAN = new Alt(DURUM_UST, ALT_YAZI_UST, HOTBAR_YARI + HOTBAR_YAN);

    private GostergeYerlesimi() {
    }

    public static Konum konum(int w, int h, int pw, int ph, Kose kose, int ofsetX, int ofsetY) {
        return konum(w, h, pw, ph, kose, ofsetX, ofsetY, VARSAYILAN);
    }

    /**
     * Forge {@code ForgeGui.leftHeight/rightHeight}'tan: durum sütunu {@code max(left, right)}; action bar onun 4 px üstünde
     * ortalanır ve 9 px yüksekliğindedir.
     */
    public static Alt forgeAlt(int solYukseklik, int sagYukseklik, int altYaziGenisligi) {
        int durum = Math.max(solYukseklik, sagYukseklik);
        return new Alt(durum + 2, durum + 11, Math.max(HOTBAR_YARI + HOTBAR_YAN, altYaziGenisligi / 2 + 4));
    }

    public static Konum konum(int w, int h, int pw, int ph, Kose kose, int ofsetX, int ofsetY, Alt alt) {
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
        }
        x = Math.max(0, Math.min(x, w - pw));
        y = Math.max(0, Math.min(y, h - ph));
        return new Konum(x, y);
    }

    private static boolean ortusur(int x, int pw, int sol, int sag) {
        return x < sag && x + pw > sol;
    }
}
