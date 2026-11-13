package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/**
 * Taban enkaz eteği (tasarım notu §2 katman 1): huninin tabanını saran dışa şişkin toz kubbesi.
 * {@code r(h) = rUst + (rZemin - rUst)·(1 - h^2.2)}: alt yarısı neredeyse etek genişliğinde kalır, üst kenarda huniye
 * kıvrılır (zemine yapışık içbükey etek arazinin arkasında kalıyordu, spike SK-10). Alfa tabanda {@link #TABAN_ALFA},
 * yüksekliğin %55'inden sonra yumuşakça söner.
 */
public final class EtekProfili {
    public static final double US = 2.2;
    public static final double TABAN_ALFA = 0.75;

    private EtekProfili() {
    }

    public static double yaricap(double rZemin, double rUst, double h) {
        return rUst + (rZemin - rUst) * (1 - Math.pow(kirp(h), US));
    }

    public static double alfa(double h) {
        double t = Math.max(0, (kirp(h) - 0.55) / 0.45);
        return TABAN_ALFA * (1 - t * t * (3 - 2 * t));
    }

    private static double kirp(double h) {
        return Math.max(0, Math.min(1, h));
    }
}
