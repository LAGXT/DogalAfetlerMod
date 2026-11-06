package io.github.kadirberkpolat1.dogalafetler.uyari;

/**
 * Konumlu ortam katmanının mesafe sönümü (saf mantık, ses.md §4 kural 5: vanilla gibi doğrusal). Vanilla'nın sabit 16 blok
 * menzili afet seslerine yetmediği için sönümü istemci hesaplar; ses örneği kaynağın konumunda durur (yön duyulur),
 * OpenAL'ın kendi sönümü kapalıdır.
 */
public final class OrtamSonumu {
    /** Menzil yerine verilince katman konumsuzdur (her yerde aynı düzey). */
    public static final double GLOBAL = 0.0;

    private OrtamSonumu() {
    }

    /** {@code 1 - mesafe/menzil}, [0, 1]; {@code menzil <= 0} (global) için 1. */
    public static float carpan(double mesafe, double menzil) {
        if (menzil <= GLOBAL) {
            return 1f;
        }
        return (float) Math.max(0.0, Math.min(1.0, 1.0 - mesafe / menzil));
    }
}
