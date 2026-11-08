package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/** Sol elde taşınan ve hissedileni değiştiren eşya (S3-05; ana elde etkisi yok). Sıra pakette bayt olarak gider. */
public enum SolEl {
    YOK,
    BUZ_KOVASI,
    LAV_KOVASI;

    private static final SolEl[] DEGERLER = values();

    public static SolEl sirayla(int sira) {
        return DEGERLER[Math.max(0, Math.min(DEGERLER.length - 1, sira))];
    }
}
