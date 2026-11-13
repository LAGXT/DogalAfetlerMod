package io.github.kadirberkpolat1.dogalafetler.kural;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;

/**
 * Tamsayı gamerule'ların geçerli aralıkları. Vanilla {@code IntegerValue} sınır denetlemez; bu yüzden okuyan
 * taraf değeri buradan geçirir. Saf Java'dır.
 */
public final class KuralSinirlari {
    public static final int SIKLIK_VARSAYILAN = 100;
    public static final int SIKLIK_EN_AZ = 0;
    /** {@code afetSikligi} üst sınırı (%1000 = on kat); daha yüksek değer afetleri art arda dizer. */
    public static final int SIKLIK_EN_COK = 1000;

    private KuralSinirlari() {
    }

    /** {@code afetSikligi} yüzdesi [0, 1000] aralığına sıkıştırılır. */
    public static int siklikYuzdesi(int ham) {
        return Math.max(SIKLIK_EN_AZ, Math.min(SIKLIK_EN_COK, ham));
    }

    /** Config olasılıklarını çarpan oran (100 → 1.0, 50 → 0.5). */
    public static double siklikCarpani(int ham) {
        return siklikYuzdesi(ham) / 100.0;
    }

    /** {@code afetMaksSiddet} madde 11'e göre [1, 5] aralığına sıkıştırılır. */
    public static int maksSiddet(int ham) {
        return Math.max(AfetTuru.EN_DUSUK_SEVIYE, Math.min(AfetTuru.EN_YUKSEK_SEVIYE, ham));
    }
}
