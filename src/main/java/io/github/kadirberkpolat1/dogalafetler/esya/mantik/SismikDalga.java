package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;

/**
 * Sismograf grafiğinin dalgası (madde 13b, "son 60 saniyenin sismik dalga grafiği"). {@link #ornek} tick'e bağlı
 * deterministik bir taşıyıcıdır (üç uyumsuz sinüs + yumuşak gürültü, [−1, 1]); {@link #genlik} ona zarfı verir: sakin
 * zeminde mikro sismik kıpırtı, öncü sarsıntıda darbe zarfıyla büyüyen tepeler, ana sarsıntıda (AKTIF) geniş salınım.
 */
public final class SismikDalga {
    public static final double TABAN = 0.04;

    private SismikDalga() {
    }

    public static double ornek(long tick, long tohum) {
        double t = tick;
        double f1 = Tohum.birim(tohum) * Math.PI * 2;
        double f2 = Tohum.birim(tohum + 1) * Math.PI * 2;
        double a = 0.45 * Math.sin(t * 0.83 + f1) + 0.30 * Math.sin(t * 2.17 + f2) + 0.15 * Math.sin(t * 0.29 + f1 * 2);
        double g0 = Tohum.birim(tohum ^ tick) * 2 - 1;
        double g1 = Tohum.birim(tohum ^ (tick - 1)) * 2 - 1;
        double v = a + 0.10 * (g0 + g1);
        return Math.max(-1, Math.min(1, v));
    }

    /**
     * @param algilandi sismograf bir sismik afet görüyor mu
     * @param oncu      öncü sarsıntı şiddeti 0–1 ({@link OncuSarsinti#siddet})
     * @param zarf      geçerli darbenin zarfı 0–1
     */
    public static double genlik(boolean algilandi, AfetAsamasi asama, double oncu, double zarf) {
        if (!algilandi) {
            return TABAN;
        }
        double o = Math.max(0, Math.min(1, oncu));
        if (asama == AfetAsamasi.AKTIF) {
            return Math.min(1, 0.55 + 0.45 * o);
        }
        return Math.min(1, TABAN + o * (0.12 + 0.84 * Math.max(0, Math.min(1, zarf))));
    }
}
