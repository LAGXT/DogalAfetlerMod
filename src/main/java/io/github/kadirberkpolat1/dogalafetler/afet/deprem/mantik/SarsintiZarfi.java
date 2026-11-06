package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

/**
 * Ekran sarsıntısının zarfı ve dalga biçimi (tasarım notu deprem §2 "Ekran sarsıntısı", sinkhole §2 "Kamera"). İstemci her
 * oyuncu için ayrı hesaplar (madde 3); sunucu yalnız aşama, merkez, seviye ve geçen süreyi gönderir. Saf Java'dır.
 */
public final class SarsintiZarfi {
    /** Ana sarsıntıdan önceki öncü titreme (1,5 sn, %20). */
    public static final int ONCU_TICK = 30;
    /** Sondaki sönüm (3 sn). */
    public static final int SONUM_TICK = 60;
    /** Sinkhole çekirdek çöküşünün tek sert sarsması (0,6 sn). */
    public static final int DARBE_TICK = 12;
    private static final double EN_AZ_MESAFE_CARPANI = 0.15;

    private SarsintiZarfi() {
    }

    /** {@code clamp(1 − d/R, 0.15, 1)}: uzaktaki oyuncu hafif titrer, yakındaki sallanır; menzil dışında 0. */
    public static double mesafeCarpani(double mesafe, double yaricap, double menzil) {
        if (mesafe > menzil || yaricap <= 0) {
            return 0;
        }
        return Math.max(EN_AZ_MESAFE_CARPANI, Math.min(1, 1 - mesafe / yaricap));
    }

    /** Deprem zarfı: 1,5 sn %20 öncü, 0,5 sn'de tam güce çıkış, sonda 3 sn doğrusal sönüm. */
    public static double deprem(double gecen, double sure) {
        if (gecen < 0 || gecen >= sure || sure <= 0) {
            return 0;
        }
        double z;
        if (gecen < ONCU_TICK) {
            z = 0.2;
        } else if (gecen < ONCU_TICK + 10) {
            z = 0.2 + 0.8 * (gecen - ONCU_TICK) / 10.0;
        } else {
            z = 1;
        }
        double kalan = sure - gecen;
        return kalan < SONUM_TICK ? z * kalan / SONUM_TICK : z;
    }

    /** Sert sarsma: {@code (1 − t/12)²}. */
    public static double darbe(double gecen) {
        if (gecen < 0 || gecen >= DARBE_TICK) {
            return 0;
        }
        double f = 1 - gecen / DARBE_TICK;
        return f * f;
    }

    /** Hızlı titreme (8–14 Hz, birbirinden bağımsız iki kanal; {@code kanal} 0 yaw, 1 pitch), −1…1. {@code t} saniye. */
    public static double titreme(double t, int kanal) {
        double faz = kanal * 1.7;
        return 0.5 * Math.sin(2 * Math.PI * 9.3 * t + faz) + 0.3 * Math.sin(2 * Math.PI * 12.7 * t + 1.3 + faz)
                + 0.2 * Math.sin(2 * Math.PI * 8.1 * t + 0.7 + 2 * faz);
    }

    /** Yavaş yuvarlanma (1,2–2 Hz, roll), −1…1. */
    public static double yuvarlanma(double t) {
        return 0.6 * Math.sin(2 * Math.PI * 1.3 * t) + 0.4 * Math.sin(2 * Math.PI * 1.9 * t + 0.9);
    }
}
