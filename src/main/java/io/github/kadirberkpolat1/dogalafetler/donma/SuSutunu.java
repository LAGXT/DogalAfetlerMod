package io.github.kadirberkpolat1.dogalafetler.donma;

/**
 * Su sütununun yukarıdan aşağı donma planı (madde 9: yüzeyden başlar, kalınlık ilerlemeyle artar). Girdi sütunun
 * en üstten itibaren taranan kısmıdır. Kurallar:
 * <ul>
 * <li>Üstteki kesintisiz su/buz parçası en az bir su içermeli; parça tarama içinde bitiyorsa en altı su olmalı. Böylece
 * altında su olmayan buz (oyuncunun buz zemini, buz sütunu) su sayılmaz ve hiç eritilmez.</li>
 * <li>Parçanın en alt bloğu hiç donmaz (kalınlık en çok uzunluk−1). Bu, yukarıdaki kuralın donmuş sütunda da tutmasını
 * sağlar ve buz altı balıkçılık için su bırakır (madde 15).</li>
 * </ul>
 * Sonuç yalnız kök sütun ve kalınlığın fonksiyonudur: herhangi bir ara durumdan yeniden planlamak aynı sonucu verir.
 */
public final class SuSutunu {
    public enum Tur {
        SU, BUZ, DIGER
    }

    private SuSutunu() {
    }

    public static Tur[] planla(Tur[] sutun, int kalinlik) {
        int n = parca(sutun);
        return n < 0 ? sutun.clone() : doldur(sutun, n, kalinlik);
    }

    /**
     * Yönlü plan: donarken ({@code yon > 0}) buz yalnız kalınlaşır, çözülürken ({@code yon < 0}) yalnız incelir,
     * {@code yon == 0} dokunmaz. Donma başlarken doğal buz (donmuş nehir, okyanus) eritilmez.
     */
    public static Tur[] planla(Tur[] sutun, int kalinlik, int yon) {
        int n = parca(sutun);
        if (n < 0 || yon == 0) {
            return sutun.clone();
        }
        int mevcut = 0;
        while (mevcut < n && sutun[mevcut] == Tur.BUZ) {
            mevcut++;
        }
        int hedef = Math.max(0, Math.min(kalinlik, n - 1));
        return doldur(sutun, n, yon > 0 ? Math.max(hedef, mevcut) : Math.min(hedef, mevcut));
    }

    /** Üstteki su/buz parçasının uzunluğu; parça kurallara uymuyorsa -1. */
    private static int parca(Tur[] sutun) {
        int n = 0;
        boolean suVar = false;
        while (n < sutun.length && sutun[n] != Tur.DIGER) {
            if (sutun[n] == Tur.SU) {
                suVar = true;
            }
            n++;
        }
        if (n < 2 || !suVar) {
            return -1;
        }
        if (n < sutun.length && sutun[n - 1] != Tur.SU) {
            return -1;
        }
        return n;
    }

    private static Tur[] doldur(Tur[] sutun, int n, int kalinlik) {
        Tur[] sonuc = sutun.clone();
        int buz = Math.max(0, Math.min(kalinlik, n - 1));
        for (int d = 0; d < n; d++) {
            sonuc[d] = d < buz ? Tur.BUZ : Tur.SU;
        }
        return sonuc;
    }
}
