package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

/**
 * Artçı sarsıntısının istemciye taşınması (AF-D7): artçı afet yuvası dışındadır ve yeni paket yoktur. Sunucu artçının kamera
 * genliğini (derece; ana sarsıntı genliği × artçı kesri) kısa gürleme katmanının hacim eğrisinin tepesine
 * ({@value #HACIM_TABANI} + genlik, en çok 1), depremin etki yarıçapını katmanın menziline (yarıçap + {@value #MENZIL_PAYI})
 * yazar; istemci aynı katmandan geri okur. Böylece büyük depremin artçısı hem daha gür hem daha serttir. Zarf
 * {@link SarsintiZarfi#artci}, uzaklık {@link SarsintiZarfi#mesafeCarpani} (ana sarsıntıyla aynı: yarıçapta %15'e iner,
 * yarıçap + {@value #SONUM_PAYI} ötesinde 0). Saf Java'dır.
 */
public final class ArtciSarsintisi {
    public static final double HACIM_TABANI = 0.3;
    public static final float MENZIL_PAYI = 32;
    public static final double SONUM_PAYI = 64;

    private ArtciSarsintisi() {
    }

    /** Artçının yakındaki kamera genliği (derece). */
    public static double genlik(double anaGenlik, double kesir) {
        return Math.max(0, anaGenlik) * Math.max(0, kesir);
    }

    /** Gürleme eğrisinin tepe hacmi; genlik {@code 1 − HACIM_TABANI} dereceye kadar geri okunabilir, üstü kırpılır. */
    public static float hacim(double genlik) {
        return (float) Math.min(1, HACIM_TABANI + Math.max(0, genlik));
    }

    /** {@link #hacim}'in tersi: eğrinin tepe hacminden genlik (derece). */
    public static double genlikOku(double tepeHacim) {
        return Math.max(0, tepeHacim - HACIM_TABANI);
    }

    /** Katman menzili: gürleme etki yarıçapının {@value #MENZIL_PAYI} blok ötesine kadar duyulur. */
    public static float menzil(double yaricap) {
        return (float) Math.max(0, yaricap) + MENZIL_PAYI;
    }

    /** {@link #menzil}'in tersi: depremin etki yarıçapı. */
    public static double yaricap(double menzil) {
        return Math.max(0, menzil - MENZIL_PAYI);
    }

    /**
     * Oyuncunun anlık artçı genliği (derece).
     *
     * @param gecen katman başından beri geçen tick
     * @param sure artçının süresi (eğrinin son noktası)
     * @param mesafe oyuncunun merkeze yatay uzaklığı
     */
    public static double oyuncuGenligi(double tepeHacim, double gecen, double sure, double mesafe, double menzil) {
        double r = yaricap(menzil);
        return genlikOku(tepeHacim) * SarsintiZarfi.artci(gecen, sure) * SarsintiZarfi.mesafeCarpani(mesafe, r, r + SONUM_PAYI);
    }
}
