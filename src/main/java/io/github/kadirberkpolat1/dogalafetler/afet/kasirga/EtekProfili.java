package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

/**
 * Taban enkaz eteği (tasarım notu §2 katman 1): zeminde etek yarıçapından başlar, üst kenarda huniyle birleşir. Yarıçap
 * {@code r(h) = rUst + (rZemin - rUst)·(1 - h)^1.7}, {@code h ∈ [0, 1]}: zemine doğru açılan içbükey etek. Alfa tabanda
 * {@link #TABAN_ALFA}, yüksekliğin %35'inden sonra yumuşakça söner, üst kenarda 0.
 */
public final class EtekProfili {
    public static final double US = 1.7;
    public static final double TABAN_ALFA = 0.75;

    private EtekProfili() {
    }

    /** {@code h} etek boyunca normalleştirilmiş yükseklik; [0, 1] dışı kırpılır. */
    public static double yaricap(double rZemin, double rUst, double h) {
        return rUst + (rZemin - rUst) * Math.pow(1 - kirp(h), US);
    }

    public static double alfa(double h) {
        double t = Math.max(0, (kirp(h) - 0.35) / 0.65);
        return TABAN_ALFA * (1 - t * t * (3 - 2 * t));
    }

    private static double kirp(double h) {
        return Math.max(0, Math.min(1, h));
    }
}
