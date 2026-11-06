package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

/**
 * Su sütununun yukarıdan aşağı donma planı (madde 9: yüzeyden başlar, kalınlık ilerlemeyle artar). Girdi sütunun
 * en üstten itibaren taranan kısmıdır. Kurallar:
 * <ul>
 * <li>Üstteki kesintisiz su/buz parçası en az bir su içermeli; parça tarama içinde bitiyorsa en altı su olmalı. Böylece
 * altında su olmayan buz (oyuncunun buz zemini, buz sütunu) su sayılmaz ve hiç eritilmez.</li>
 * <li>Parça en az 2 blok derinse en alt bloğu hiç donmaz (kalınlık en çok uzunluk−1): buz altı balıkçılık için su kalır
 * (madde 15) ve yukarıdaki kural donmuş sütunda da tutar.</li>
 * <li>1 blok derin su (altı tarama içinde katı) tamamen donar: kıyı rafları ve sığ bataklık da yürünebilir buz olur
 * (madde 9). Donmuş 1 derin sütunda su kalmadığı için çözülürken yalnız buzu bizim koyduğumuz biliniyorsa
 * ({@code bizim}) su sayılır.</li>
 * <li>Buz seviyesi: 0 yok, 1 üst blok ince buz ({@link Tur#INCE}), {@code k + 1} üstte {@code k} blok buz. İnce buz
 * donmanın ilk, çözülmenin son evresidir.</li>
 * </ul>
 * Sonuç yalnız kök sütun ve seviyenin fonksiyonudur: herhangi bir ara durumdan yeniden planlamak aynı sonucu verir.
 */
public final class SuSutunu {
    public enum Tur {
        SU, BUZ, INCE, DIGER
    }

    private SuSutunu() {
    }

    /** Yönsüz plan, {@code kalinlik} blok buz; sütundaki buz bizimdir (saf testler). */
    public static Tur[] planla(Tur[] sutun, int kalinlik) {
        int n = parca(sutun, true);
        return n < 0 ? sutun.clone() : doldur(sutun, n, kalinlikSeviyesi(kalinlik));
    }

    /** Yönlü plan, {@code kalinlik} blok buz (ince evre yok); sütundaki buz bizimdir. */
    public static Tur[] planla(Tur[] sutun, int kalinlik, int yon) {
        return planlaSeviye(sutun, kalinlikSeviyesi(kalinlik), yon, true);
    }

    /** {@link #planlaSeviye(Tur[], int, int, boolean)}, buzun bizim olduğu bilinmeden. */
    public static Tur[] planlaSeviye(Tur[] sutun, int seviye, int yon) {
        return planlaSeviye(sutun, seviye, yon, false);
    }

    /**
     * Yönlü plan, buz seviyesiyle: donarken ({@code yon > 0}) buz yalnız ilerler (su → ince → kalın), çözülürken
     * ({@code yon < 0}) yalnız geriler, {@code yon == 0} dokunmaz. Donma başlarken doğal buz eritilmez.
     *
     * @param bizim sütunun buzunu donma koydu (chunk verisindeki buz biti); tamamen donmuş 1 derin sütun ancak böyle çözülür
     */
    public static Tur[] planlaSeviye(Tur[] sutun, int seviye, int yon, boolean bizim) {
        int n = parca(sutun, bizim);
        if (n < 0 || yon == 0) {
            return sutun.clone();
        }
        int mevcut = mevcutSeviye(sutun);
        int hedef = sinirla(seviye, n);
        return doldur(sutun, n, yon > 0 ? Math.max(hedef, mevcut) : Math.min(hedef, mevcut));
    }

    /** Sütunun şimdiki buz seviyesi. */
    public static int mevcutSeviye(Tur[] sutun) {
        if (sutun.length > 0 && sutun[0] == Tur.INCE) {
            return 1;
        }
        int k = 0;
        while (k < sutun.length && sutun[k] == Tur.BUZ) {
            k++;
        }
        return kalinlikSeviyesi(k);
    }

    private static int kalinlikSeviyesi(int kalinlik) {
        return kalinlik <= 0 ? 0 : kalinlik + 1;
    }

    /** Parçada en çok buz: 1 derin sütun tamamen, daha derini en alt bloğu dışında. */
    private static int enCokBuz(int n) {
        return n == 1 ? 1 : n - 1;
    }

    /** Seviyeyi parça uzunluğuna sığdırır. */
    private static int sinirla(int seviye, int n) {
        if (seviye <= 1) {
            return Math.max(0, seviye);
        }
        return Math.min(seviye - 1, enCokBuz(n)) + 1;
    }

    /** Üstteki su/buz parçasının uzunluğu; parça kurallara uymuyorsa -1. */
    private static int parca(Tur[] sutun, boolean bizim) {
        int n = 0;
        boolean suVar = false;
        while (n < sutun.length && sutun[n] != Tur.DIGER) {
            if (sutun[n] == Tur.SU) {
                suVar = true;
            }
            n++;
        }
        if (n == 1) {
            // Tek blok: altı tarama içinde katı olmalı; su ya da bizim donmuş suyumuz.
            return n < sutun.length && (suVar || bizim) ? 1 : -1;
        }
        if (n < 2 || !suVar) {
            return -1;
        }
        if (n < sutun.length && sutun[n - 1] != Tur.SU) {
            return -1;
        }
        return n;
    }

    private static Tur[] doldur(Tur[] sutun, int n, int seviye) {
        Tur[] sonuc = sutun.clone();
        int s = sinirla(seviye, n);
        int buz = s <= 1 ? 0 : s - 1;
        for (int d = 0; d < n; d++) {
            sonuc[d] = d < buz ? Tur.BUZ : Tur.SU;
        }
        if (s == 1) {
            sonuc[0] = Tur.INCE;
        }
        return sonuc;
    }
}
