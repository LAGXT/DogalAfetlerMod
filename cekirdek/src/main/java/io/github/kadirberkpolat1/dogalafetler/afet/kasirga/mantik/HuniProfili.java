package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

/** Huni yarıçap profili (tasarım notu §2 katman 2): {@code r(h) = rTaban + (rTepe - rTaban)·h^0.6}, {@code h ∈ [0, 1]}. */
public final class HuniProfili {
    public static final double US = 0.6;

    private HuniProfili() {
    }

    /** {@code h} huni boyunca normalleştirilmiş yükseklik; [0, 1] dışı kırpılır. */
    public static double yaricap(double rTaban, double rTepe, double h) {
        return rTaban + (rTepe - rTaban) * Math.pow(Math.max(0, Math.min(1, h)), US);
    }
}
