package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import java.util.Arrays;

/**
 * Sabit kapasiteli float halka tamponu: sismograf son 60 saniyenin örneklerini (20 Hz, 1200 örnek) tutar. Ekleme O(1),
 * bellek sabit. {@link #sutunlar} grafiğin her piksel sütunu için en küçük ve en büyük örneği verir; tamponun henüz
 * dolmamış (eski) kısmına düşen sütunlar {@code NaN}'dır.
 */
public final class HalkaTampon {
    private final float[] veri;
    private int bas;
    private int boyut;

    public HalkaTampon(int kapasite) {
        if (kapasite <= 0) {
            throw new IllegalArgumentException("Kapasite pozitif olmalı: " + kapasite);
        }
        veri = new float[kapasite];
    }

    public int kapasite() {
        return veri.length;
    }

    public int boyut() {
        return boyut;
    }

    public void ekle(float deger) {
        int yer = (bas + boyut) % veri.length;
        veri[yer] = deger;
        if (boyut < veri.length) {
            boyut++;
        } else {
            bas = (bas + 1) % veri.length;
        }
    }

    /** {@code i}. örnek; 0 en eski. */
    public float get(int i) {
        if (i < 0 || i >= boyut) {
            throw new IndexOutOfBoundsException(i + " / " + boyut);
        }
        return veri[(bas + i) % veri.length];
    }

    /** En yeni örnek; boşsa 0. */
    public float son() {
        return boyut == 0 ? 0 : get(boyut - 1);
    }

    public void temizle() {
        bas = 0;
        boyut = 0;
    }

    /**
     * Tamponun tam kapasitesini (60 sn) {@code sutun} eşit dilime böler; her dilimin en küçük ve en büyük örneğini yazar.
     * Sağdaki sütun en yeni örnekleri içerir; veri olmayan dilimler {@code NaN}.
     */
    public void sutunlar(int sutun, float[] enKucuk, float[] enBuyuk) {
        if (sutun <= 0 || enKucuk.length < sutun || enBuyuk.length < sutun) {
            throw new IllegalArgumentException("Sütun dizileri yetersiz: " + sutun);
        }
        Arrays.fill(enKucuk, 0, sutun, Float.NaN);
        Arrays.fill(enBuyuk, 0, sutun, Float.NaN);
        int bos = veri.length - boyut;
        for (int s = 0; s < sutun; s++) {
            int a = (int) ((long) s * veri.length / sutun);
            int b = (int) ((long) (s + 1) * veri.length / sutun);
            float mn = Float.POSITIVE_INFINITY;
            float mx = Float.NEGATIVE_INFINITY;
            for (int k = Math.max(a, bos); k < Math.max(b, a + 1); k++) {
                if (k < bos || k >= veri.length) {
                    continue;
                }
                float v = get(k - bos);
                mn = Math.min(mn, v);
                mx = Math.max(mx, v);
            }
            if (mn <= mx) {
                enKucuk[s] = mn;
                enBuyuk[s] = mx;
            }
        }
    }
}
