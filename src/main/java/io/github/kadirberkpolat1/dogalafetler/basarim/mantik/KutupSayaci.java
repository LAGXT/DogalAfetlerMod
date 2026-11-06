package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

/**
 * #11 "Kutup Sığınağı": donma %100'e ulaştıktan sonra hayatta geçirilen oyun zamanı. Saat, oyun gününün tabanıdır
 * (K-C42: {@code getGameTime() + uykuBirikimi}); uyku atlaması da sayılır, {@code /time set} saymaz. Yalnız oyuncu çevrim
 * içiyken ve koşul aktifken birikir; ölüm sıfırlar, giriş son gözlemi unutur (çevrim dışı geçen süre sayılmaz, A-08).
 */
public final class KutupSayaci {
    public static final long GUN_TICK = 24_000L;
    /** Tek gözlemde eklenebilecek en çok süre: bir günlük uyku atlaması + saniyelik adım payı. */
    public static final long EN_COK_ADIM = GUN_TICK + 200L;
    public static final long YOK = Long.MIN_VALUE;

    private long birikim;
    private long son;

    public KutupSayaci(long birikim, long son) {
        this.birikim = Math.max(0, birikim);
        this.son = son;
    }

    /** Bir gözlem: {@code aktif} ise son gözlemden bu yana geçen süre (sınırlı) birikir. */
    public long guncelle(long saat, boolean aktif) {
        if (aktif && son != YOK) {
            long fark = saat - son;
            if (fark > 0) {
                birikim += Math.min(fark, EN_COK_ADIM);
            }
        }
        son = saat;
        return birikim;
    }

    public void olum() {
        birikim = 0;
    }

    public void girisYapti() {
        son = YOK;
    }

    public long birikim() {
        return birikim;
    }

    public long son() {
        return son;
    }

    public double gun() {
        return (double) birikim / GUN_TICK;
    }
}
