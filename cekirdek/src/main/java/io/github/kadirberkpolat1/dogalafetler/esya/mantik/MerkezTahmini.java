package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

/**
 * Sismografın gösterdiği merkez: gerçek merkezden afet başına sabit bir yönde ve uzaklıkta (en çok {@code hataYaricapi})
 * sapan nokta (madde 13b: Hassas Sismograf merkezi "daha doğru" gösterir, yani yarıçapı küçüktür).
 */
public final class MerkezTahmini {
    private MerkezTahmini() {
    }

    /** {x, z}; yarıçap içinde alanca düzgün dağılım. */
    public static double[] tahmin(double x, double z, double hataYaricapi, long tohum) {
        double aci = Tohum.birim(tohum) * Math.PI * 2;
        double r = Math.max(0, hataYaricapi) * Math.sqrt(Tohum.birim(tohum ^ 0x5DEECE66DL));
        return new double[] {x + Math.cos(aci) * r, z + Math.sin(aci) * r};
    }
}
