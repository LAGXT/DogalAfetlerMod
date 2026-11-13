package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import java.util.random.RandomGenerator;

/**
 * Meteor ödül zarı (madde 13c, K-E74): Meteorit Parçası küçük meteorda {@code parca}, büyükte {@code buyukParca} olasılığıyla;
 * büyükte parça çıktıysa her biri {@code buyukEkParca} olasılığıyla en çok {@value #EN_COK_EK} ek parça daha düşer (K-E74: büyük
 * meteorun eşyası hem daha sık hem daha bol). Kozmik Çekirdek yalnız büyük meteorda {@code cekirdek} olasılığıyla, tek tanedir.
 * Atışlar bağımsızdır; zar her zaman dört sayı tüketir (tohumlu tekrar).
 */
public final class OdulZari {
    /** Büyük meteorda ilk parçaya eklenebilecek en çok ek Meteorit Parçası. */
    public static final int EN_COK_EK = 2;

    public record Sanslar(double parca, double buyukParca, double cekirdek, double buyukEkParca) {
        /**
         * Büyükte beklenen eşya 0,053 → 0,158 (≈ ×3): parça %5 → %10, ek parça ortalaması 0,5 (çıkan parça başına), çekirdek
         * %0,3 → %0,8. Küçük meteor %3 aynı.
         */
        public static final Sanslar VARSAYILAN = new Sanslar(0.03, 0.10, 0.008, 0.25);

        public Sanslar {
            for (double s : new double[] {parca, buyukParca, cekirdek, buyukEkParca}) {
                if (!(s >= 0 && s <= 1)) {
                    throw new IllegalArgumentException("şans 0–1 olmalı: " + s);
                }
            }
        }
    }

    /**
     * Zar sonucu: {@code parca} ilk Meteorit Parçası, {@code ekParca} ona eklenen parça sayısı (parça yoksa 0), {@code cekirdek}
     * Kozmik Çekirdek.
     */
    public record Sonuc(boolean parca, boolean cekirdek, int ekParca) {
        public static final Sonuc YOK = new Sonuc(false, false, 0);

        public Sonuc {
            if (ekParca < 0 || ekParca > EN_COK_EK || (!parca && ekParca != 0)) {
                throw new IllegalArgumentException("ek parça 0–" + EN_COK_EK + " ve yalnız parça varken: " + ekParca);
            }
        }

        public Sonuc(boolean parca, boolean cekirdek) {
            this(parca, cekirdek, 0);
        }

        public boolean bos() {
            return !parca && !cekirdek;
        }

        /** Düşecek Meteorit Parçası sayısı. */
        public int parcaAdedi() {
            return parca ? 1 + ekParca : 0;
        }

        /** Düşecek toplam eşya (Meteorit Parçası + Kozmik Çekirdek). */
        public int adet() {
            return parcaAdedi() + (cekirdek ? 1 : 0);
        }
    }

    private OdulZari() {
    }

    public static Sonuc at(boolean buyuk, Sanslar s, RandomGenerator r) {
        double parcaZari = r.nextDouble();
        double cekirdekZari = r.nextDouble();
        double ek1 = r.nextDouble();
        double ek2 = r.nextDouble();
        boolean parca = parcaZari < (buyuk ? s.buyukParca() : s.parca());
        boolean cekirdek = buyuk && cekirdekZari < s.cekirdek();
        int ek = parca && buyuk ? (ek1 < s.buyukEkParca() ? 1 : 0) + (ek2 < s.buyukEkParca() ? 1 : 0) : 0;
        return new Sonuc(parca, cekirdek, ek);
    }
}
