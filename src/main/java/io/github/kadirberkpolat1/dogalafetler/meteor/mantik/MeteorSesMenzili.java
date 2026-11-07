package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Meteor seslerinin menzili, kazancı ve perdesi (istemci çalar; tasarım notu "Meteor" → "Ses"). Kazanç mesafeyle karesel
 * azalır ({@code (1 − d/menzil)²}) ve menzilde sıfırdır: ses menzil sınırında tam düzeyde başlamaz, vanilla doğrusal
 * azalma kullanılmaz. Uçuş sesinin perdesi Doppler oranıdır: {@code c / (c − v)}, {@code v} meteorun dinleyiciye yaklaşma
 * hızı. Aynı türden aynı anda en çok {@value #AYNI_TUR_TAVANI} ses çalar; fazlası en uzaktakinden düşer (ses.md §4 kural 3).
 * Çarpmanın menzili, düzeyi ve perdesi sürekli ölçekten ({@link MeteorOlcegi#ara}): tipik küçük 96 blok / %70, tipik büyük
 * 128 blok / %100, arası ve uçları doğrusal.
 */
public final class MeteorSesMenzili {
    /** Uçuş sesi meteor bu kadar yakınken başlar (blok). */
    public static final double UCUS_MENZILI = 48;
    /** Çalan uçuş sesi ancak bu mesafeyi aşınca durur: sınırda aç-kapa olmasın. */
    public static final double UCUS_BIRAKMA = 52;
    public static final double CARPMA_MENZILI_BUYUK = 128;
    public static final double CARPMA_MENZILI_KUCUK = 96;
    /** Tipik küçük çarpmanın düzeyi tipik büyüğe göre (aynı mesafede). */
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

    /** Doppler perdesi: hız ±0,9 ses hızında kırpılır, sonuç 0,526–2 (oyunda hız ≤ 3,8 blok/tick: 0,76–1,46). {@code yaklasmaHizi} blok/tick, uzaklaşırken eksi. */
    public static double dopplerPerdesi(double yaklasmaHizi) {
        double v = Math.max(-0.9 * SES_HIZI, Math.min(0.9 * SES_HIZI, yaklasmaHizi));
        return Math.max(0.5, Math.min(2.0, SES_HIZI / (SES_HIZI - v)));
    }

    /** Çarpma menzili (blok): ölçek 0,5'te ~91, 2,5'te ~139. */
    public static double carpmaMenzili(double olcek) {
        return MeteorOlcegi.ara(olcek, CARPMA_MENZILI_KUCUK, CARPMA_MENZILI_BUYUK, 64, 160);
    }

    /** Çarpma patlamasının dinleyicideki kazancı (0–1): büyük daha uzaktan ve aynı mesafede daha güçlü duyulur. */
    public static double carpmaKazanci(double mesafe, double olcek) {
        return MeteorOlcegi.ara(olcek, KUCUK_CARPMA_DUZEYI, 1.0, 0.5, 1.0) * kazanc(mesafe, carpmaMenzili(olcek));
    }

    /** Büyük meteorun patlaması daha alçak (tipik büyük 0,85); ay parçaları %10 tiz (tasarım notu). */
    public static double carpmaPerdesi(double olcek, boolean ay) {
        return MeteorOlcegi.ara(olcek, 1.0, 0.85, 0.75, 1.05) * (ay ? 1.1 : 1.0);
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
