package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

/**
 * Yağmur damlasının bir tick'teki çarpma kararı (saf; dünya sorguları {@link TsunamiYagmuru}'da). Damla {@code once}'den
 * {@code y}'ye iner; {@code ust} yolundaki hücrelerde bulunan en üst blok yüzeyi (çarpışma şeklinin damlanın noktasındaki tepesi
 * ya da görünen gerçek sıvının yüzeyi; yoksa {@link #YAN}), {@code su} sütunun sanal su yüzeyi (yoksa NaN).
 */
final class YagmurCarpmasi {
    /** Çarpmadı, düşmeye devam eder. */
    static final double YOK = Double.NaN;
    /** Bloğa yandan girdi: halkasız söner. Blok yüzeyi yokken {@code ust} için de bu değer verilir. */
    static final double YAN = Double.NEGATIVE_INFINITY;

    private YagmurCarpmasi() {
    }

    /** Çarptığı yükseklik (halka orada), {@link #YOK} ya da {@link #YAN}. Blok ve sanal su birlikteyse yüksek olan. */
    static double karar(double once, double y, double ust, double su) {
        double suya = !Double.isNaN(su) && y <= su ? su : YOK;
        if (ust == YAN || ust < y) {
            return suya;
        }
        if (ust > once) {
            return !Double.isNaN(suya) && suya >= ust ? suya : YAN;
        }
        return Double.isNaN(suya) ? ust : Math.max(ust, suya);
    }
}
