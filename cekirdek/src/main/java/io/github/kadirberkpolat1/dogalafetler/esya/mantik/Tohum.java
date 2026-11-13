package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

/** Deterministik sözde rastgele sayı (SplitMix64): aynı afet aynı tahmin hatasını verir, istemci ve sunucu aynı dalgayı. */
public final class Tohum {
    private Tohum() {
    }

    public static long karistir(long x) {
        long z = x + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** [0, 1) aralığında düzgün dağılımlı değer. */
    public static double birim(long tohum) {
        return (karistir(tohum) >>> 11) * 0x1.0p-53;
    }
}
