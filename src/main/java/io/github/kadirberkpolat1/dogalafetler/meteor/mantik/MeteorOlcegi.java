package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import java.util.Random;

/**
 * Meteorun sürekli ölçeği (kullanıcı isteği 27, {@code .vibe/kararlar/meteor-2.md} MT2-13): üç bant içinde düzgün dağılım, küçük
 * [{@value #KUCUK_ALT}, {@value #ORTA_ALT}), orta [{@value #ORTA_ALT}, {@value #BUYUK_ALT}), büyük [{@value #BUYUK_ALT},
 * {@value #UST}]. Büyüklüğe bağlı her değer (krater, yangın, sarsıntı, ses, çizim) iki config referansından {@link #ara} ile
 * doğrusal türetilir: küçük referans ölçek {@value #KUCUK_TIPIK}, büyük {@value #BUYUK_TIPIK}. Ödül sınıfı (Kozmik Çekirdek
 * yalnız büyükte) büyük banttır.
 */
public final class MeteorOlcegi {
    public static final double KUCUK_ALT = 0.5;
    public static final double ORTA_ALT = 0.9;
    public static final double BUYUK_ALT = 1.6;
    public static final double UST = 2.5;
    /** Eski iki boyutun karşılığı: küçük ve büyük config değerleri bu ölçeklerde aynen geçerlidir. */
    public static final double KUCUK_TIPIK = 0.7;
    public static final double BUYUK_TIPIK = 2.0;

    private MeteorOlcegi() {
    }

    /**
     * Bant olasılıklarıyla ölçek: büyük {@code buyukOran}, orta {@code ortaOran}, küçük kalan (eksi değil); bant içinde
     * düzgün. Oranlar 0–1'e kırpılır, toplamları 1'i aşarsa orta kısılır.
     */
    public static double ornekle(Random r, double buyukOran, double ortaOran) {
        double buyuk = Math.max(0, Math.min(1, buyukOran));
        double orta = Math.max(0, Math.min(1 - buyuk, ortaOran));
        double z = r.nextDouble();
        double u = r.nextDouble();
        if (z < buyuk) {
            return BUYUK_ALT + u * (UST - BUYUK_ALT);
        }
        if (z < buyuk + orta) {
            return ORTA_ALT + u * (BUYUK_ALT - ORTA_ALT);
        }
        return KUCUK_ALT + u * (ORTA_ALT - KUCUK_ALT);
    }

    /** Ödül sınıfı büyük mü (büyük bant). */
    public static boolean buyuk(double olcek) {
        return olcek >= BUYUK_ALT;
    }

    /** Bant: 0 küçük, 1 orta, 2 büyük. */
    public static int bant(double olcek) {
        return olcek >= BUYUK_ALT ? 2 : olcek >= ORTA_ALT ? 1 : 0;
    }

    /** Eski boyutun tipik ölçeği (açık boyutlu komut ve test çağrıları). */
    public static double tipik(boolean buyuk) {
        return buyuk ? BUYUK_TIPIK : KUCUK_TIPIK;
    }

    /** {@code (KUCUK_TIPIK, kucuk)} ile {@code (BUYUK_TIPIK, buyuk)} arası doğrusal, uçlarda kırpmadan uzatılır. */
    public static double ara(double olcek, double kucuk, double buyuk) {
        return kucuk + (buyuk - kucuk) * (olcek - KUCUK_TIPIK) / (BUYUK_TIPIK - KUCUK_TIPIK);
    }

    /** {@link #ara}, sonra {@code [enAz, enCok]}'a kırpılır. */
    public static double ara(double olcek, double kucuk, double buyuk, double enAz, double enCok) {
        return Math.max(enAz, Math.min(enCok, ara(olcek, kucuk, buyuk)));
    }
}
