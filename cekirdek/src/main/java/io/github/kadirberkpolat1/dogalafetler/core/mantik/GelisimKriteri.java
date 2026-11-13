package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * Güneş patlamasının ve ay çarpmasının "oyuncular yeterince gelişti" kapısı (bölüm D ve H, madde 3): oyun günü, gelişim
 * puanı ({@link GelisimOlcutu}), ikisi birden ya da herhangi biri. Saf Java'dır.
 */
public enum GelisimKriteri {
    /** Oyun günü en az gün eşiği. */
    GUN,
    /** Gelişim puanı en az puan eşiği. */
    PUAN,
    /** İkisi birden. */
    IKISI_DE,
    /** Herhangi biri. */
    HERHANGI;

    public boolean saglandi(double gun, double minGun, double puan, double puanEsigi) {
        boolean gunTamam = gun >= minGun;
        boolean puanTamam = puan >= puanEsigi;
        return switch (this) {
            case GUN -> gunTamam;
            case PUAN -> puanTamam;
            case IKISI_DE -> gunTamam && puanTamam;
            case HERHANGI -> gunTamam || puanTamam;
        };
    }
}
