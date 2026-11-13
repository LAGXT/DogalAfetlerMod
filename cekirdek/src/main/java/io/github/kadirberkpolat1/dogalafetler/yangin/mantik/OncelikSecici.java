package io.github.kadirberkpolat1.dogalafetler.yangin.mantik;

import java.util.Arrays;

/** Yayılma kuyruğunun seçimi: puanı en küçük {@code k} girdinin sırası, O(n log k) (en büyük-yığın). */
public final class OncelikSecici {
    private OncelikSecici() {
    }

    /** {@code puan[0..n)} içinden en küçük {@code k} puanın indeksleri, küçükten büyüğe; eşitlikte önce eklenen. */
    public static int[] enIyiler(double[] puan, int n, int k) {
        int adet = Math.max(0, Math.min(k, n));
        if (adet == 0) {
            return new int[0];
        }
        int[] yigin = new int[adet];
        int boy = 0;
        for (int i = 0; i < n; i++) {
            if (boy < adet) {
                yigin[boy] = i;
                yukari(yigin, boy++, puan);
            } else if (kotu(yigin[0], i, puan)) {
                yigin[0] = i;
                asagi(yigin, boy, puan);
            }
        }
        Integer[] sirali = new Integer[adet];
        for (int i = 0; i < adet; i++) {
            sirali[i] = yigin[i];
        }
        Arrays.sort(sirali, (a, b) -> kotu(a, b, puan) ? 1 : kotu(b, a, puan) ? -1 : 0);
        int[] sonuc = new int[adet];
        for (int i = 0; i < adet; i++) {
            sonuc[i] = sirali[i];
        }
        return sonuc;
    }

    /** {@code a}, {@code b}'den sonra mı gelmeli (puanı büyük ya da eşit puanda sonra eklenmiş). */
    private static boolean kotu(int a, int b, double[] puan) {
        return puan[a] > puan[b] || (puan[a] == puan[b] && a > b);
    }

    private static void yukari(int[] y, int i, double[] puan) {
        while (i > 0) {
            int ust = (i - 1) / 2;
            if (!kotu(y[i], y[ust], puan)) {
                return;
            }
            int t = y[i];
            y[i] = y[ust];
            y[ust] = t;
            i = ust;
        }
    }

    private static void asagi(int[] y, int boy, double[] puan) {
        int i = 0;
        while (true) {
            int sol = 2 * i + 1;
            int sag = sol + 1;
            int en = i;
            if (sol < boy && kotu(y[sol], y[en], puan)) {
                en = sol;
            }
            if (sag < boy && kotu(y[sag], y[en], puan)) {
                en = sag;
            }
            if (en == i) {
                return;
            }
            int t = y[i];
            y[i] = y[en];
            y[en] = t;
            i = en;
        }
    }
}
