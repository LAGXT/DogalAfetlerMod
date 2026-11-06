package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

/**
 * Çarpma sarsıntısı: şiddet mesafeyle karesel azalır ({@code temel · (1 − d/menzil)²}), menzilde biter. Süre şiddetle uzar.
 */
public final class Sarsinti {
    public static final int EN_KISA_SURE = 8;
    private static final int EK_SURE = 32;

    private Sarsinti() {
    }

    public static double siddet(double mesafe, double menzil, double temel) {
        if (menzil <= 0 || mesafe >= menzil) {
            return 0;
        }
        double f = 1 - Math.max(0, mesafe) / menzil;
        return temel * f * f;
    }

    public static int sureTick(double siddet) {
        return EN_KISA_SURE + (int) Math.round(EK_SURE * Math.max(0, Math.min(1, siddet)));
    }
}
