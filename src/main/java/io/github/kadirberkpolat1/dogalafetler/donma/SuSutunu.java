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
        Tur[] sonuc = sutun.clone();
        int n = 0;
        boolean suVar = false;
        while (n < sutun.length && sutun[n] != Tur.DIGER) {
            if (sutun[n] == Tur.SU) {
                suVar = true;
            }
            n++;
        }
        if (n < 2 || !suVar) {
            return sonuc;
        }
        if (n < sutun.length && sutun[n - 1] != Tur.SU) {
            return sonuc;
        }
        int buz = Math.max(0, Math.min(kalinlik, n - 1));
        for (int d = 0; d < n; d++) {
            sonuc[d] = d < buz ? Tur.BUZ : Tur.SU;
        }
        return sonuc;
    }
}
