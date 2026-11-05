package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import java.util.Locale;

/** Güneşin evresi (madde 16). Ay durumundan bağımsızdır. */
public enum GunesEvresi {
    NORMAL("normal"),
    BUYUYEN("buyuyen"),
    SUPERNOVA("supernova"),
    GUNESSIZ("gunessiz"),
    KIZIL_CUCE("kizilcuce");

    /** Komutta ve NBT'de kullanılan ASCII ad. */
    public final String komutAdi;

    GunesEvresi(String komutAdi) {
        this.komutAdi = komutAdi;
    }

    public static GunesEvresi komuttan(String ad) {
        String kucuk = ad.toLowerCase(Locale.ROOT);
        for (GunesEvresi e : values()) {
            if (e.komutAdi.equals(kucuk)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Bilinmeyen güneş evresi: " + ad);
    }
}
