package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import java.util.Locale;

/**
 * Buz deliği balıkçılığının kategori ağırlıkları (madde 15: yaygın, orta, nadir balık; düşük çöp; çok nadir hazine; meteor
 * ödülü yok). Her kategorinin içeriği kendi loot tablosundadır ({@code dogalafetler:gameplay/buz_deligi/<ad>}). Şans
 * (Luck of the Sea + şans efekti) vanilla "quality" gibi çalışır: seviye başına çöp azalır, hazine artar.
 *
 * <p>{@link Kategori#ESYA} (soğutma parçası, tuz kristali, buzlu inci) her olta ile çıkar; {@link Kategori#OZEL} (özel kutup
 * balıkları) yalnız Kutup Oltası'yla atıldığında havuza girer ({@code ozelAcik}); başka olta ile ağırlığı yok sayılır.
 */
public record LootAgirliklari(int yaygin, int orta, int nadir, int cop, int hazine, int esya, int ozel,
        int sansCopAzalmasi, int sansHazineArtisi) {
    /**
     * Varsayılan: Kutup Oltası'sız toplam 118; balık %78 (yaygın 56, orta 28, nadir 8), eşya %15 (18), çöp %6 (7), hazine
     * %1 (vanilla: balık 85, çöp 10, hazine 5). Kutup Oltası'yla özel balık ağırlığı 14 eklenir (≈ %11).
     */
    public static final LootAgirliklari VARSAYILAN = new LootAgirliklari(56, 28, 8, 7, 1, 18, 14, 2, 1);

    /** Kategori; {@link #ad()} loot tablosu dosya adıdır. */
    public enum Kategori {
        YAYGIN,
        ORTA,
        NADIR,
        COP,
        HAZINE,
        ESYA,
        OZEL;

        public String ad() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public LootAgirliklari {
        if (yaygin < 0 || orta < 0 || nadir < 0 || cop < 0 || hazine < 0 || esya < 0 || ozel < 0
                || yaygin + orta + nadir + cop + hazine + esya <= 0) {
            throw new IllegalArgumentException("loot ağırlıkları negatif olamaz ve toplamı pozitif olmalı");
        }
    }

    /** Şansla düzeltilmiş ağırlık (eksiye düşmez). {@link Kategori#OZEL} için yalnız yapılandırılan değer. */
    public int agirlik(Kategori k, float sans) {
        int s = Math.round(sans);
        int w = switch (k) {
            case YAYGIN -> yaygin;
            case ORTA -> orta;
            case NADIR -> nadir;
            case COP -> cop - s * sansCopAzalmasi;
            case HAZINE -> hazine + s * sansHazineArtisi;
            case ESYA -> esya;
            case OZEL -> ozel;
        };
        return Math.max(0, w);
    }

    private int etkin(Kategori k, float sans, boolean ozelAcik) {
        return k == Kategori.OZEL && !ozelAcik ? 0 : agirlik(k, sans);
    }

    private int toplam(float sans, boolean ozelAcik) {
        int t = 0;
        for (Kategori k : Kategori.values()) {
            t += etkin(k, sans, ozelAcik);
        }
        return t;
    }

    /** Kutup Oltası'sız oran. */
    public double oran(Kategori k, float sans) {
        return oran(k, sans, false);
    }

    public double oran(Kategori k, float sans, boolean ozelAcik) {
        int t = toplam(sans, ozelAcik);
        return t == 0 ? 0 : etkin(k, sans, ozelAcik) / (double) t;
    }

    /** Kutup Oltası'sız seçim. */
    public Kategori sec(double u, float sans) {
        return sec(u, sans, false);
    }

    /** {@code u} ∈ [0, 1) zarıyla kategori. Toplam ağırlık şansla 0'a düşerse yaygın balık. */
    public Kategori sec(double u, float sans, boolean ozelAcik) {
        int t = toplam(sans, ozelAcik);
        if (t <= 0) {
            return Kategori.YAYGIN;
        }
        double hedef = Math.max(0.0, Math.min(u, Math.nextDown(1.0))) * t;
        double birikim = 0;
        Kategori son = Kategori.YAYGIN;
        for (Kategori k : Kategori.values()) {
            int w = etkin(k, sans, ozelAcik);
            if (w == 0) {
                continue;
            }
            birikim += w;
            son = k;
            if (hedef < birikim) {
                return k;
            }
        }
        return son;
    }
}
