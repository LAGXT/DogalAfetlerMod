package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * "Çok gelirse güçsüz" kuralı (K-E67, kullanıcı isteği): son bir oyun gününde (24 000 ölçekli tick) başlayan doğal afet
 * sayısı o günün normalinden (afetSikligi %100'deki hedef aralık, {@link #hedefAralik}) fazlaysa ya da {@code afetSikligi}
 * %100'ün üstündeyse yük ({@link #yuk}) 1'i aşar; yük {@value #ESIK}'i aşınca seçilen seviye aşağı kayar: seviye L'nin
 * ağırlığı {@code z^(−(L−1)/2)} ile çarpılır, {@code z = min(}{@value #EN_COK_ZAYIFLATMA}{@code , yük / }{@value #ESIK}{@code )}.
 * Hiçbir seviye sıfırlanmaz (ender de olsa yüksek seviye gelebilir); gün ilerledikçe normal sayı da arttığı için günün
 * oluğan gücü korunur. Deterministiktir, saf Java.
 */
public final class AfetYogunlugu {
    /** Yük bunu aşmadança seviye dağılımı değişmez (doğal dalgalanma payı). */
    public static final double ESIK = 1.5;
    public static final double EN_COK_ZAYIFLATMA = 8.0;
    /** Hedef ortalama aralık (tick): gün ≤ 1'de 8 dk, gün 10'da 5 dk, gün ≥ 30'da 3 dk; arası doğrusal. */
    public static final long ARALIK_GUN_1 = 9_600L;
    public static final long ARALIK_GUN_10 = 6_000L;
    public static final long ARALIK_GUN_30 = 3_600L;

    private AfetYogunlugu() {
    }

    /** {@code afetSikligi} %100'de o gün doğal afet başlangıçları arasının hedef ortalaması (gerçek tick, K-E67). */
    public static long hedefAralik(double gun) {
        if (!(gun > 1)) {
            return ARALIK_GUN_1;
        }
        if (gun <= 10) {
            return Math.round(ARALIK_GUN_1 + (ARALIK_GUN_10 - ARALIK_GUN_1) * (gun - 1) / 9.0);
        }
        if (gun <= 30) {
            return Math.round(ARALIK_GUN_10 + (ARALIK_GUN_30 - ARALIK_GUN_10) * (gun - 10) / 20.0);
        }
        return ARALIK_GUN_30;
    }

    /** O gün bir oyun gününde normal sayılan doğal afet sayısı. */
    public static double normalGunluk(double gun) {
        return AfetZamanlayicisi.GUN_TICK / (double) hedefAralik(gun);
    }

    /** Yük: son gündeki doğal afet sayısının normale oranı ile {@code afetSikligi} çarpanının büyüğü. */
    public static double yuk(int sonGunSayisi, double gun, double siklik) {
        double oran = Math.max(0, sonGunSayisi) / normalGunluk(gun);
        double s = Double.isNaN(siklik) ? 0 : siklik;
        return Math.max(oran, s);
    }

    /** Yüke göre zayıflatma katsayısı {@code z} (1 = etkisiz). */
    public static double zayiflatma(double yuk) {
        if (!(yuk > ESIK)) {
            return 1.0;
        }
        return Math.min(EN_COK_ZAYIFLATMA, yuk / ESIK);
    }

    /** Seviyenin (1–5) ağırlık çarpanı. */
    public static double seviyeCarpani(int seviye, double yuk) {
        return Math.pow(zayiflatma(yuk), -(seviye - 1) / 2.0);
    }
}
