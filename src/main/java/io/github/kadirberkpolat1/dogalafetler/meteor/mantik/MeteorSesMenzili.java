package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Meteor seslerinin menzili, kazancı ve perdesi (istemci çalar; tasarım notu "Meteor" → "Ses"). Kazanç mesafeyle karesel
 * azalır ({@code (1 − d/menzil)²}) ve menzilde sıfırdır: ses menzil sınırında tam düzeyde başlamaz, vanilla doğrusal
 * azalma kullanılmaz. Uçuş sesinin perdesi Doppler oranıdır: {@code c / (c − v)}, {@code v} meteorun dinleyiciye yaklaşma
 * hızı. Aynı türden aynı anda en çok {@value #AYNI_TUR_TAVANI} ses çalar; fazlası en uzaktakinden düşer (ses.md §4 kural 3).
 */
public final class MeteorSesMenzili {
    /** Uçuş sesi meteor bu kadar yakınken başlar (blok). */
    public static final double UCUS_MENZILI = 48;
    /** Çalan uçuş sesi ancak bu mesafeyi aşınca durur: sınırda aç-kapa olmasın. */
    public static final double UCUS_BIRAKMA = 52;
    public static final double CARPMA_MENZILI_BUYUK = 128;
    public static final double CARPMA_MENZILI_KUCUK = 96;
    /** Küçük çarpmanın düzeyi büyüğe göre (aynı mesafede). */
    public static final double KUCUK_CARPMA_DUZEYI = 0.7;
    public static final int AYNI_TUR_TAVANI = 6;
    /** Oyundaki ses hızı (blok/tick): 3 blok/tick'lik meteorda perde 0,8 (uzaklaşan) ile 1,33 (yaklaşan) arası kayar. */
    public static final double SES_HIZI = 12;

    private MeteorSesMenzili() {
    }

    /** Menzil içinde {@code (1 − d/menzil)²}, menzilde ve ötesinde 0. */
    public static double kazanc(double mesafe, double menzil) {
        if (menzil <= 0 || mesafe >= menzil) {
            return 0;
        }
        double f = 1 - Math.max(0, mesafe) / menzil;
        return f * f;
    }

    /** Uçuş sesi duyulur mu: çalmıyorsa menzil, çalıyorsa bırakma mesafesi. */
    public static boolean ucusDuyulur(double mesafe, boolean caliyor) {
        return mesafe <= (caliyor ? UCUS_BIRAKMA : UCUS_MENZILI);
    }

    /** Uçuş kazancı: bırakma payında da (menzil ile bırakma arası) sıfır. */
    public static double ucusKazanci(double mesafe) {
        return kazanc(mesafe, UCUS_MENZILI);
    }

    /** Doppler perdesi, Minecraft'ın perde sınırına (0,5–2) kırpılır. {@code yaklasmaHizi} blok/tick, uzaklaşırken eksi. */
    public static double dopplerPerdesi(double yaklasmaHizi) {
        double v = Math.max(-0.9 * SES_HIZI, Math.min(0.9 * SES_HIZI, yaklasmaHizi));
        return Math.max(0.5, Math.min(2.0, SES_HIZI / (SES_HIZI - v)));
    }

    public static double carpmaMenzili(boolean buyuk) {
        return buyuk ? CARPMA_MENZILI_BUYUK : CARPMA_MENZILI_KUCUK;
    }

    /** Çarpma patlamasının dinleyicideki kazancı (0–1): büyük daha uzaktan ve aynı mesafede daha güçlü duyulur. */
    public static double carpmaKazanci(double mesafe, boolean buyuk) {
        return (buyuk ? 1.0 : KUCUK_CARPMA_DUZEYI) * kazanc(mesafe, carpmaMenzili(buyuk));
    }

    /** Büyük meteorun patlaması daha alçak; ay parçaları %10 tiz (tasarım notu). */
    public static double carpmaPerdesi(boolean buyuk, boolean ay) {
        return (buyuk ? 0.85 : 1.0) * (ay ? 1.1 : 1.0);
    }

    /**
     * Tavan doluyken yeni ses için yer: dolu değilse {@code -1} (yer var); yeni ses çalanların en uzağından yakınsa o
     * örneğin indeksi (o düşürülür); değilse {@code -2} (yeni ses çalmaz).
     */
    public static int yer(double[] calanMesafeler, double yeni, int tavan) {
        if (calanMesafeler.length < tavan) {
            return -1;
        }
        int enUzak = -2;
        double uzak = yeni;
        for (int i = 0; i < calanMesafeler.length; i++) {
            if (calanMesafeler[i] > uzak) {
                uzak = calanMesafeler[i];
                enUzak = i;
            }
        }
        return enUzak;
    }

    /**
     * Uçuş seslerinin seçimi: duyulanlardan ({@link #ucusDuyulur}) en yakın {@code tavan} tanesi. {@code caliyor[i]}: i.
     * meteorun sesi şu an çalıyor mu; sonuç hangilerinin çalması gerektiği.
     */
    public static boolean[] ucusSecimi(double[] mesafeler, boolean[] caliyor, int tavan) {
        Integer[] sira = new Integer[mesafeler.length];
        for (int i = 0; i < sira.length; i++) {
            sira[i] = i;
        }
        Arrays.sort(sira, Comparator.comparingDouble(i -> mesafeler[i]));
        boolean[] secildi = new boolean[mesafeler.length];
        int adet = 0;
        for (int i : sira) {
            if (adet >= tavan) {
                break;
            }
            if (ucusDuyulur(mesafeler[i], caliyor[i])) {
                secildi[i] = true;
                adet++;
            }
        }
        return secildi;
    }
}
