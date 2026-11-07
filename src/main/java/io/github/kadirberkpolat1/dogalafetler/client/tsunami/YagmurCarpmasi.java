package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

/**
 * Yağmur damlasının bir tick'teki çarpma kararı (saf; dünya sorguları {@link TsunamiYagmuru}'da). Damla {@code once}'den
 * {@code y}'ye iner; {@code ust} yolundaki hücrelerde bulunan en üst blok yüzeyi (katı bloğun damlanın noktasındaki görünen
 * tepesi ya da üst yüzü çizilen gerçek sıvının yüzeyi; yoksa {@link #YAN}, üst yüzü çizilmeyen gerçek suya girdiyse
 * {@link #GIZLI}), {@code su} sütunun bu karede çizilen sanal su yüzeyi (yoksa ya da analitik yüzey suyu yok diyorsa NaN).
 */
final class YagmurCarpmasi {
    /** Çarpmadı, düşmeye devam eder. */
    static final double YOK = Double.NaN;
    /** Bloğa yandan ya da sanal suya alttan girdi: halkasız söner. Blok yüzeyi yokken {@code ust} için de bu değer verilir. */
    static final double YAN = Double.NEGATIVE_INFINITY;
    /**
     * {@code ust} için: damla üst yüzü çizilmeyen gerçek suya girdi (çekilmenin gizlediği katmanın altındaki su; üstünde aynı sıvı
     * olduğu için yüzü yok). Orada görünen yüzey yalnız sanal sudur: damla bu tick onu geçtiyse ona iner, yoksa halkasız söner.
     */
    static final double GIZLI = Double.POSITIVE_INFINITY;

    private YagmurCarpmasi() {
    }

    /**
     * Çarptığı yükseklik (halka orada), {@link #YOK} ya da {@link #YAN}. Blok ve sanal su birlikteyse yüksek olan. Bir önceki
     * konumu zaten sanal suyun altında olan damla (yüzey üstüne yükseldi ya da yandan girdi) yüzeyden geçmez, halkasız söner;
     * üst yüzü çizilmeyen gerçek suya giren damla için {@link #GIZLI}.
     */
    static double karar(double once, double y, double ust, double su) {
        boolean suda = !Double.isNaN(su) && y <= su;
        if (suda && once <= su) {
            return YAN;
        }
        if (ust == GIZLI) {
            return suda ? su : YAN;
        }
        double suya = suda ? su : YOK;
        if (ust == YAN || ust < y) {
            return suya;
        }
        if (ust > once) {
            return !Double.isNaN(suya) && suya >= ust ? suya : YAN;
        }
        return Double.isNaN(suya) ? ust : Math.max(ust, suya);
    }

    /**
     * Halka {@code yuzey} yüksekliğindeki üst yüze yatay çizilir; üst yüz yalnız göz ondan yüksekteyse görünür. Göz hizasının
     * üstündeki yüzeyde (kum tepesi, çatı, korkuluk) halka blokların kenarında gökyüzünün önünde asılı durur.
     */
    static boolean ustYuzGorunur(double yuzey, double gozY) {
        return yuzey < gozY;
    }

    /**
     * Sanal su halkasının bir ucu, o ucun sütununda çizilen su üstü ({@code uc}) halkanın {@code yuzey}'inden en çok
     * {@code pay} alçaksa desteklidir (daha yüksek su ucu örter). Çizilen su yoksa (NaN: kara ya da gerçek su) ya da su daha
     * alçaksa uç boşta kalır: dalga cephesinde ve kıyıda halka suyun kenarından havaya taşardı.
     */
    static boolean sanalUcDestekli(double uc, double yuzey, double pay) {
        return uc >= yuzey - pay;
    }
}
