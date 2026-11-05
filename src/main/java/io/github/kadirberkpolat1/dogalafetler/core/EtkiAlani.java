package io.github.kadirberkpolat1.dogalafetler.core;

/**
 * Afetin etki alanı: merkez ve yatay yarıçap (blok). Etkilenebilecek oyuncular (siren, paket) ve "sağ çıkan" takibi bundan
 * hesaplanır. Mesafe yataydır (XZ): mağaradaki ya da çatıdaki oyuncu da afetin altındadır. Sunucu geneli afetlerde
 * (güneş patlaması, ay çarpması) {@link #herYer()} her yeri içerir. Saf Java'dır.
 */
public record EtkiAlani(double x, double y, double z, double yaricap, boolean global) {
    private static final EtkiAlani GLOBAL = new EtkiAlani(0, 0, 0, Double.POSITIVE_INFINITY, true);

    public EtkiAlani {
        if (!global && !(yaricap >= 0)) {
            throw new IllegalArgumentException("Etki alanı yarıçapı negatif ya da sayı değil: " + yaricap);
        }
    }

    public static EtkiAlani daire(double x, double y, double z, double yaricap) {
        return new EtkiAlani(x, y, z, yaricap, false);
    }

    /** Sunucu geneli afetin her yeri kapsayan alanı. */
    public static EtkiAlani herYer() {
        return GLOBAL;
    }

    /** Nokta alanın ({@code ek} blok genişletilmiş) içinde mi; global alan her noktayı içerir. */
    public boolean iceriyor(double px, double py, double pz, double ek) {
        if (global) {
            return true;
        }
        double dx = px - x;
        double dz = pz - z;
        double r = yaricap + Math.max(0, ek);
        return dx * dx + dz * dz <= r * r;
    }
}
