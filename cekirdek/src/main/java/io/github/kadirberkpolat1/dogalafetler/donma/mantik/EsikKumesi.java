package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

/**
 * Sütunun etkin seviyesi {@code (a, b]} aralığında değişirken herhangi bir adımın (kar, buz, ince buz, tablo satırı)
 * eşiği aşılıyor mu (spike B7): aşılmıyorsa sütunun blokları hiç okunmaz. Küme eşik fonksiyonlarının kendisinden kurulur
 * (her seviyede bir önceki seviyeye göre farkı olan fonksiyon var mı), böylece fonksiyonlarla her zaman tutarlıdır.
 * Seviye titreşimle 0–100'ün dışına çıkabilir; {@value #ALT}..{@value #UST} aralığı kapsanır, dışı uçlara kırpılır.
 */
public final class EsikKumesi {
    static final int ALT = -32;
    static final int UST = 140;

    private final int[] donmaSayaci = new int[UST - ALT + 2];
    private final int[] cozulmeSayaci = new int[UST - ALT + 2];

    private EsikKumesi() {
    }

    /**
     * @param tabloDonma yüzey tablosu satırlarının donma eşikleri (seviye ≥ eşik olunca gelir)
     * @param tabloCozulme aynı satırların çözülme eşikleri (seviye eşiğin altına inince kalkar)
     */
    public static EsikKumesi kur(DonmaEsikleri e, int[] tabloDonma, int[] tabloCozulme) {
        EsikKumesi k = new EsikKumesi();
        boolean[] donma = new boolean[UST - ALT + 1];
        boolean[] cozulme = new boolean[UST - ALT + 1];
        for (int l = ALT + 1; l <= UST; l++) {
            boolean d = e.karKatmani(l - 1, false) != e.karKatmani(l, false)
                    || e.yapiKarKatmani(l - 1, 1) != e.yapiKarKatmani(l, 1);
            boolean c = e.karKatmaniCozulme(l - 1, false) != e.karKatmaniCozulme(l, false)
                    || e.yapiKarKatmani(l - 1, -1) != e.yapiKarKatmani(l, -1);
            for (int u = 0; u <= e.kiyiUzaklikSiniri() && !(d && c); u++) {
                d |= e.buzSeviyesi(l - 1, u) != e.buzSeviyesi(l, u);
                c |= e.buzSeviyesiCozulme(l - 1, u) != e.buzSeviyesiCozulme(l, u);
            }
            donma[l - ALT] = d;
            cozulme[l - ALT] = c;
        }
        isaretle(donma, tabloDonma);
        isaretle(cozulme, tabloCozulme);
        for (int i = 0; i < donma.length; i++) {
            k.donmaSayaci[i + 1] = k.donmaSayaci[i] + (donma[i] ? 1 : 0);
            k.cozulmeSayaci[i + 1] = k.cozulmeSayaci[i] + (cozulme[i] ? 1 : 0);
        }
        return k;
    }

    private static void isaretle(boolean[] dizi, int[] esikler) {
        for (int t : esikler) {
            dizi[kirp(t) - ALT] = true;
        }
    }

    private static int kirp(int l) {
        return Math.max(ALT, Math.min(UST, l));
    }

    /** {@code (alt, ust]} aralığında bir eşik var mı ({@code cozulme}: çözülme eşikleri). */
    public boolean degisimVar(int alt, int ust, boolean cozulme) {
        int a = Math.min(alt, ust);
        int b = Math.max(alt, ust);
        if (a == b) {
            return false;
        }
        int[] s = cozulme ? cozulmeSayaci : donmaSayaci;
        return s[kirp(b) - ALT + 1] - s[kirp(a) - ALT + 1] > 0;
    }
}
