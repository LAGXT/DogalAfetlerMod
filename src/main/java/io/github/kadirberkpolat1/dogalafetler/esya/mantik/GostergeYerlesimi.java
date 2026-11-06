package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

/**
 * Radar göstergesinin ekrandaki yeri (GUI pikseli). Gösterge seçilen köşeye oturur; alt köşelerde vanilla HUD'un ve
 * sıcaklık barının (XP barının üstü, sıcaklık §A, S-10) üstüne binmez: hotbar'ın yan sütunlarıyla (yan el yuvası, saldırı
 * göstergesi) örtüşürse action bar'ın ve tutulan eşya adının satırının (sismograf metni oraya yazılır), orta durum sütunuyla
 * (kalp, zırh, açlık, hava, sıcaklık barı, XP) örtüşürse onların üstüne kalkar. Sonuç her zaman ekranın içindedir.
 */
public final class GostergeYerlesimi {
    public enum Kose {
        SAG_ALT, SAG_UST, SOL_UST, SOL_ALT
    }

    public record Konum(int x, int y) {
    }

    /** Ekran kenarından boşluk. */
    public static final int PAY = 4;
    /** Hotbar'ın yarı genişliği (182/2). */
    public static final int HOTBAR_YARI = 91;
    /** Hotbar'ın iki yanında yan el yuvası ve saldırı göstergesi için ek pay. */
    public static final int HOTBAR_YAN = 30;
    /** Action bar satırının üstü (vanilla {@code h - 68}) ve 4 px pay, ekranın altından; tutulan eşya adı da bunun altındadır. */
    public static final int ALT_YAZI_UST = 72;
    /** Orta durum sütununun üstü: iki kalp satırı + zırh/hava + sıcaklık barı + XP (ekranın altından). */
    public static final int DURUM_UST = 86;

    private GostergeYerlesimi() {
    }

    public static Konum konum(int w, int h, int pw, int ph, Kose kose, int ofsetX, int ofsetY) {
        boolean sag = kose == Kose.SAG_ALT || kose == Kose.SAG_UST;
        boolean alt = kose == Kose.SAG_ALT || kose == Kose.SOL_ALT;
        int x = sag ? w - pw - PAY - ofsetX : PAY + ofsetX;
        int y = alt ? h - ph - PAY - ofsetY : PAY + ofsetY;
        if (alt) {
            int orta = w / 2;
            if (ortusur(x, pw, orta - HOTBAR_YARI, orta + HOTBAR_YARI)) {
                y = Math.min(y, h - DURUM_UST - ph);
            } else if (ortusur(x, pw, orta - HOTBAR_YARI - HOTBAR_YAN, orta + HOTBAR_YARI + HOTBAR_YAN)) {
                y = Math.min(y, h - ALT_YAZI_UST - ph);
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
