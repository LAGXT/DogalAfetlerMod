package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Bir süre boyunca giderek azalan hızla vücuda eklenen sıcaklık (madde 14: "ısınma etkisi 60–180 sn sürsün, giderek
 * azalsın"). Hız doğrusal azalır: {@code r(t) = 2·toplam/T · (1 − t/T)}; sürenin sonunda verilen toplam tam
 * {@code toplam}'dır. Zaman tick'tir.
 */
public final class SureliEtki {
    private final double toplam;
    private final int sure;
    private int gecen;

    public SureliEtki(double toplam, int sureTick) {
        this(toplam, sureTick, 0);
    }

    public SureliEtki(double toplam, int sureTick, int gecenTick) {
        if (sureTick <= 0) {
            throw new IllegalArgumentException("süre pozitif olmalı: " + sureTick);
        }
        this.toplam = toplam;
        this.sure = sureTick;
        this.gecen = Math.max(0, Math.min(sureTick, gecenTick));
    }

    /** {@code tick} kadar ilerler, bu aralıkta verilen sıcaklığı döndürür. */
    public double adim(int tick) {
        if (tick <= 0 || bitti()) {
            return 0;
        }
        int once = gecen;
        gecen = Math.min(sure, gecen + tick);
        return birikim(gecen) - birikim(once);
    }

    private double birikim(int t) {
        double f = (double) t / sure;
        return toplam * (2 * f - f * f);
    }

    public boolean bitti() {
        return gecen >= sure;
    }

    public double toplam() {
        return toplam;
    }

    public int sure() {
        return sure;
    }

    public int gecen() {
        return gecen;
    }

    /** Kalan (henüz verilmemiş) sıcaklık. */
    public double kalan() {
        return toplam - birikim(gecen);
    }
}
