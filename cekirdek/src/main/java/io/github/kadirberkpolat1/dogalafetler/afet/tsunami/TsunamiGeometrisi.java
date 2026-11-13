package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

/**
 * Tsunaminin yeri: kıyı noktası {@code (kiyiX, kiyiZ)}, dalganın karaya doğru ilerleme yönü {@code (yonX, yonZ)} (birim), dalga
 * tabanı (gerçek su yüzeyinin bir altı, mutlak Y) ve aramada bulunan deniz sütunu sayısı. Dalga kıyıdan {@code aciklik} blok
 * açıkta doğar, kıyıdan {@code karaGiris} blok içeride durur. Saf Java.
 */
public record TsunamiGeometrisi(double kiyiX, double kiyiZ, double yonX, double yonZ, double taban, int denizSutunu) {
    public TsunamiGeometrisi {
        double boy = Math.hypot(yonX, yonZ);
        if (!(boy > 0)) {
            throw new IllegalArgumentException("tsunami yönü sıfır");
        }
        yonX /= boy;
        yonZ /= boy;
    }

    /**
     * Deniz örneklerinin merkeze göre toplam ofsetinden dalga yönü: denizden karaya, yani toplamın tersi. Deniz her yandaysa
     * (toplam neredeyse sıfır: ada, tekne) {@code +x}.
     */
    public static double[] yonHesapla(double toplamDx, double toplamDz, int ornek) {
        double ox = ornek > 0 ? toplamDx / ornek : 0;
        double oz = ornek > 0 ? toplamDz / ornek : 0;
        double boy = Math.hypot(ox, oz);
        return boy < 1.0 ? new double[] {1, 0} : new double[] {-ox / boy, -oz / boy};
    }

    /** Noktanın kıyı çizgisine göre dalga yönündeki uzaklığı (artı: karada). */
    public double boyuna(double x, double z) {
        return (x - kiyiX) * yonX + (z - kiyiZ) * yonZ;
    }

    /** Noktanın dalga ekseninden yanal uzaklığı. */
    public double yanal(double x, double z) {
        return -(x - kiyiX) * yonZ + (z - kiyiZ) * yonX;
    }

    /** Kıyıdan {@code boyuna} blok ileri, eksenden {@code yanal} blok yandaki dünya X'i. */
    public double x(double boyuna, double yanal) {
        return kiyiX + yonX * boyuna - yonZ * yanal;
    }

    public double z(double boyuna, double yanal) {
        return kiyiZ + yonZ * boyuna + yonX * yanal;
    }
}
