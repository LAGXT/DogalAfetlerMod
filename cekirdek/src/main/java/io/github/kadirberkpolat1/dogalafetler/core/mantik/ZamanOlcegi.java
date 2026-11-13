package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * Afet ve dönem zamanlayıcılarının tek hız çarpanı ({@code /dogalafet hiz}, N18). Her sunucu tick'inde {@link #ilerle()}
 * kaç ölçekli tick ilerleneceğini verir; kesirli çarpanlar birikerek tam sayıya çevrilir (0,5 → iki tick'te bir).
 */
public final class ZamanOlcegi {
    public static final double EN_AZ = 0.1;
    public static final double EN_COK = 1000.0;

    private double carpan = 1.0;
    private double birikim;

    public double carpan() {
        return carpan;
    }

    public void ayarla(double yeni) {
        if (Double.isNaN(yeni)) {
            throw new IllegalArgumentException("Hız çarpanı sayı değil");
        }
        carpan = Math.max(EN_AZ, Math.min(EN_COK, yeni));
    }

    /** Bu tick'te ilerleyen ölçekli tick sayısı. */
    public int ilerle() {
        birikim += carpan;
        int adim = (int) Math.floor(birikim + 1e-9);
        birikim -= adim;
        return adim;
    }

    /** Ölçekli bir sürenin gerçek tick karşılığı (yukarı yuvarlanır); bilinmeyen süre (&lt; 0) {@code -1}. */
    public long gercekTick(long olcekliTick) {
        if (olcekliTick < 0) {
            return -1;
        }
        return olcekliTick == 0 ? 0 : (long) Math.ceil(olcekliTick / carpan);
    }

    public void sifirla() {
        carpan = 1.0;
        birikim = 0;
    }
}
