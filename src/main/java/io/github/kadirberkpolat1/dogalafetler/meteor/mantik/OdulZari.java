package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import java.util.random.RandomGenerator;

/**
 * Meteor ödül zarı (madde 13c): Meteorit Parçası küçük meteorda {@code parca}, büyükte {@code buyukParca} olasılığıyla;
 * Kozmik Çekirdek yalnız büyük meteorda {@code cekirdek} olasılığıyla. İki atış bağımsızdır; zar her zaman iki sayı tüketir
 * (tohumlu tekrar).
 */
public final class OdulZari {
    public record Sanslar(double parca, double buyukParca, double cekirdek) {
        public static final Sanslar VARSAYILAN = new Sanslar(0.03, 0.05, 0.003);

        public Sanslar {
            for (double s : new double[] {parca, buyukParca, cekirdek}) {
                if (!(s >= 0 && s <= 1)) {
                    throw new IllegalArgumentException("şans 0–1 olmalı: " + s);
                }
            }
        }
    }

    public record Sonuc(boolean parca, boolean cekirdek) {
        public static final Sonuc YOK = new Sonuc(false, false);

        public boolean bos() {
            return !parca && !cekirdek;
        }
    }

    private OdulZari() {
    }

    public static Sonuc at(boolean buyuk, Sanslar s, RandomGenerator r) {
        double parcaZari = r.nextDouble();
        double cekirdekZari = r.nextDouble();
        boolean parca = parcaZari < (buyuk ? s.buyukParca() : s.parca());
        boolean cekirdek = buyuk && cekirdekZari < s.cekirdek();
        return new Sonuc(parca, cekirdek);
    }
}
