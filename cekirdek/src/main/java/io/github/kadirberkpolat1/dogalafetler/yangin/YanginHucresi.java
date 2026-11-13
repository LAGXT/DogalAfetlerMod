package io.github.kadirberkpolat1.dogalafetler.yangin;

/** Yanan bir yakıt bloğu. Konumlar {@code BlockPos.asLong} biçimindedir; zamanlar dünya {@code getGameTime()}'ıdır. */
final class YanginHucresi {
    final long konum;
    final YanginKaynagi kaynak;
    final float yogunluk;
    final long baslangic;
    long bitis;
    long sonIslenme;
    boolean yayilabilir;
    /** Bu hücre için konulan {@link AfetAtesiBlogu}'nun konumu; yoksa {@code atesVar} false. */
    long atesKonumu;
    boolean atesVar;

    YanginHucresi(long konum, YanginKaynagi kaynak, float yogunluk, long baslangic, long bitis) {
        this.konum = konum;
        this.kaynak = kaynak;
        this.yogunluk = yogunluk;
        this.baslangic = baslangic;
        this.bitis = bitis;
        this.sonIslenme = baslangic;
        this.yayilabilir = true;
    }

    /** Yanma süresinin geçen kısmı, 0–1. */
    double yasOrani(long simdi) {
        long sure = Math.max(1, bitis - baslangic);
        return Math.min(1.0, Math.max(0.0, (simdi - baslangic) / (double) sure));
    }
}
