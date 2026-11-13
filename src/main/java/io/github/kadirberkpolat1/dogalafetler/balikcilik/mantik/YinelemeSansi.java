package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

/**
 * Yineleme büyüsünün şansı (saf mantık): I %2, II %6, III %10; olta ile tutulan her yiğın bu şansla ikiye katlanır (ikinci,
 * aynı yiğın). III'ün üstü (komutla verilmiş) III sayılır.
 */
public final class YinelemeSansi {
    public static final int EN_COK_SEVIYE = 3;
    private static final double[] SANS = {0.02, 0.06, 0.10};

    private YinelemeSansi() {
    }

    /** Seviyenin kopyalama şansı (0–1); seviye 0 ya da eksi ise 0. */
    public static double sans(int seviye) {
        return seviye <= 0 ? 0.0 : SANS[Math.min(seviye, EN_COK_SEVIYE) - 1];
    }

    /** {@code zar} ∈ [0, 1) şansın altındaysa yiğın kopyalanır. */
    public static boolean kopyalanir(int seviye, double zar) {
        return zar < sans(seviye);
    }
}
