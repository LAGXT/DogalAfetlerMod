package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import java.util.Locale;

/** Kalan sürenin göstergedeki biçimi: gerçek tick → saniye → {@code m:ss} (bir saati aşarsa {@code s:mm:ss}). */
public final class SureBicimi {
    private SureBicimi() {
    }

    /** Gerçek tick'i yukarı yuvarlanmış saniyeye çevirir; bilinmeyen (&lt; 0) {@code -1}. */
    public static long saniye(long gercekTick) {
        if (gercekTick < 0) {
            return -1;
        }
        return (gercekTick + 19) / 20;
    }

    /** {@code 0:45}, {@code 12:05}, {@code 1:02:03}; bilinmeyen süre {@code ?}. */
    public static String bicim(long saniye) {
        if (saniye < 0) {
            return "?";
        }
        long s = saniye % 60;
        long dk = saniye / 60 % 60;
        long sa = saniye / 3600;
        return sa > 0 ? String.format(Locale.ROOT, "%d:%02d:%02d", sa, dk, s) : String.format(Locale.ROOT, "%d:%02d", dk, s);
    }
}
