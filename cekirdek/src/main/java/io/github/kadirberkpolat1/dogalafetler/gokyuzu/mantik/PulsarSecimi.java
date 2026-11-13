package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import java.util.SplittableRandom;

/**
 * Süpernovadan sonra doğan yeni yıldızın seçimi (gökyüzü-2): kızıl cüce ya da düşük olasılıkla pulsar. Seçim dünya tohumu ve
 * zincir döngüsünden (süpernova başlangıcı) belirlenir: aynı dünyada aynı döngü yeniden başlatmada ya da ikinci sorguda aynı
 * sonucu verir, başka dünyada ya da başka döngüde bağımsızdır. Minecraft sınıfı kullanmaz.
 */
public final class PulsarSecimi {
    /** Config varsayılanı ({@code gokyuzu.pulsarOlasiligi}). */
    public static final double VARSAYILAN_OLASILIK = 0.15;

    private PulsarSecimi() {
    }

    /** Dünya tohumu ile döngü kimliğinden seçim tohumu (ikisi de 64 bit karma). */
    public static long tohum(long dunyaTohumu, long dongu) {
        return dunyaTohumu * 0x9E3779B97F4A7C15L ^ Long.rotateLeft(dongu, 29) ^ 0x5DEECE66DL;
    }

    /**
     * Bu tohumla pulsar mı doğar: tohumdan türetilen düzgün [0, 1) sayı {@code olasilik}'tan küçükse. 0 hiç, 1 her zaman;
     * aralık dışı değerler kenetlenir.
     */
    public static boolean pulsarMi(double olasilik, long tohum) {
        double p = Double.isNaN(olasilik) ? 0.0 : Math.max(0.0, Math.min(1.0, olasilik));
        return new SplittableRandom(tohum).nextDouble() < p;
    }
}
