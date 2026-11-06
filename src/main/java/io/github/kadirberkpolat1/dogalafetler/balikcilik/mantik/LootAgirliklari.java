package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import java.util.Locale;

/**
 * Buz deliği balıkçılığının kategori ağırlıkları (madde 15: yaygın, orta, nadir balık; düşük çöp; çok nadir hazine; meteor
 * ödülü yok). Her kategorinin içeriği kendi loot tablosundadır ({@code dogalafetler:gameplay/buz_deligi/<ad>}). Şans
 * (Luck of the Sea + şans efekti) vanilla "quality" gibi çalışır: seviye başına çöp azalır, hazine artar.
 */
public record LootAgirliklari(int yaygin, int orta, int nadir, int cop, int hazine, int sansCopAzalmasi,
        int sansHazineArtisi) {
    /** Varsayılan: balık %92 (yaygın 56, orta 28, nadir 8), çöp %7, hazine %1 (vanilla: balık 85, çöp 10, hazine 5). */
    public static final LootAgirliklari VARSAYILAN = new LootAgirliklari(56, 28, 8, 7, 1, 2, 1);

    /** Kategori; {@link #ad()} loot tablosu dosya adıdır. */
    public enum Kategori {
        YAYGIN,
        ORTA,
        NADIR,
        COP,
        HAZINE;

        public String ad() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public LootAgirliklari {
        if (yaygin < 0 || orta < 0 || nadir < 0 || cop < 0 || hazine < 0 || yaygin + orta + nadir + cop + hazine <= 0) {
            throw new IllegalArgumentException("loot ağırlıkları negatif olamaz ve toplamı pozitif olmalı");
        }
    }

    /** Şansla düzeltilmiş ağırlık (eksiye düşmez). */
    public int agirlik(Kategori k, float sans) {
        int s = Math.round(sans);
        int w = switch (k) {
            case YAYGIN -> yaygin;
            case ORTA -> orta;
            case NADIR -> nadir;
            case COP -> cop - s * sansCopAzalmasi;
            case HAZINE -> hazine + s * sansHazineArtisi;
        };
        return Math.max(0, w);
    }

    private int toplam(float sans) {
        int t = 0;
        for (Kategori k : Kategori.values()) {
            t += agirlik(k, sans);
        }
        return t;
    }

    public double oran(Kategori k, float sans) {
        int t = toplam(sans);
        return t == 0 ? 0 : agirlik(k, sans) / (double) t;
    }

    /** {@code u} ∈ [0, 1) zarıyla kategori. Toplam ağırlık şansla 0'a düşerse yaygın balık. */
    public Kategori sec(double u, float sans) {
        int t = toplam(sans);
        if (t <= 0) {
            return Kategori.YAYGIN;
        }
        double hedef = Math.max(0.0, Math.min(u, Math.nextDown(1.0))) * t;
        double birikim = 0;
        Kategori son = Kategori.YAYGIN;
        for (Kategori k : Kategori.values()) {
            int w = agirlik(k, sans);
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
