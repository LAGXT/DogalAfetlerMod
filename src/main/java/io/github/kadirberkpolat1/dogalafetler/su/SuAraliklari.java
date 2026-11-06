package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.Arrays;

/**
 * Bir sütundaki sanal su dilimleri: her biri {@code [alt, ust)} dikey aralığı. Deniz, dalga, mağara ve çekilme suyu aynı
 * sütunda üst üste binebilir; {@link #birlestir()} sıralayıp kesişenleri tek aralığa indirir. Kare başına binlerce kez
 * çağrıldığı için tahsis yapmadan yeniden kullanılır (tek iş parçacığı).
 */
public final class SuAraliklari {
    private double[] alt = new double[4];
    private double[] ust = new double[4];
    private boolean[] sakin = new boolean[4];
    private int adet;

    public void temizle() {
        adet = 0;
    }

    public void ekle(double a, double u) {
        ekle(a, u, false);
    }

    /** {@code sakinMi}: yüzeyine dalgacık uygulanmayan su (mağara). Boş ya da ters aralık yok sayılır. */
    public void ekle(double a, double u, boolean sakinMi) {
        if (!(u > a)) {
            return;
        }
        if (adet == alt.length) {
            alt = Arrays.copyOf(alt, adet * 2);
            ust = Arrays.copyOf(ust, adet * 2);
            sakin = Arrays.copyOf(sakin, adet * 2);
        }
        alt[adet] = a;
        ust[adet] = u;
        sakin[adet] = sakinMi;
        adet++;
    }

    /** Alta göre sıralar, kesişen ya da değen aralıkları birleştirir; sakinlik üstü yüksek olandan gelir. */
    public void birlestir() {
        for (int i = 1; i < adet; i++) {
            double a = alt[i];
            double u = ust[i];
            boolean s = sakin[i];
            int j = i - 1;
            while (j >= 0 && alt[j] > a) {
                alt[j + 1] = alt[j];
                ust[j + 1] = ust[j];
                sakin[j + 1] = sakin[j];
                j--;
            }
            alt[j + 1] = a;
            ust[j + 1] = u;
            sakin[j + 1] = s;
        }
        int n = 0;
        for (int i = 0; i < adet; i++) {
            if (n > 0 && alt[i] <= ust[n - 1]) {
                if (ust[i] > ust[n - 1]) {
                    ust[n - 1] = ust[i];
                    sakin[n - 1] = sakin[i];
                }
            } else {
                alt[n] = alt[i];
                ust[n] = ust[i];
                sakin[n] = sakin[i];
                n++;
            }
        }
        adet = n;
    }

    public int adet() {
        return adet;
    }

    public double alt(int i) {
        return alt[i];
    }

    public double ust(int i) {
        return ust[i];
    }

    public boolean sakin(int i) {
        return sakin[i];
    }

    /** {@code y}'yi içeren aralığın üstü; {@code y} suda değilse NaN. */
    public double ustuBul(double y) {
        for (int i = 0; i < adet; i++) {
            if (alt[i] <= y && y < ust[i]) {
                return ust[i];
            }
        }
        return Double.NaN;
    }

    /** {@code [minY, maxY]} kutusunun suda kalan payı (0..1). */
    public double batiklikOrani(double minY, double maxY) {
        double boy = maxY - minY;
        if (boy <= 0) {
            return 0;
        }
        double toplam = 0;
        for (int i = 0; i < adet; i++) {
            double a = Math.max(alt[i], minY);
            double u = Math.min(ust[i], maxY);
            if (u > a) {
                toplam += u - a;
            }
        }
        return Math.min(1.0, toplam / boy);
    }

    /** {@code [minY, maxY]} ile kesişen en yüksek aralığın üstünün {@code minY}'den yüksekliği; kesişme yoksa 0. */
    public double ayaktanYukseklik(double minY, double maxY) {
        double en = 0;
        for (int i = 0; i < adet; i++) {
            if (alt[i] < maxY && ust[i] > minY) {
                en = Math.max(en, ust[i] - minY);
            }
        }
        return en;
    }
}
