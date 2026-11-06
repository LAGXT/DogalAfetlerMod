package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;

/**
 * Radar ve sismografın afeti ne zaman gördüğü (madde 4, 13a, 13b, N7; çekirdek §3a radar/sismograf kuralı).
 * <ul>
 * <li>ON_UYARI'da normal radar ve sismograf ancak normal pencere başlayınca ({@code Afet.normalOnUyariBasladi()}) görür;
 * Hassas Sismograf erken bölümün {@code hassasErkenOran} kadarını da görür (1 = ON_UYARI'nın başından).</li>
 * <li>Sunucu geneli afet (güneş patlaması, ay çarpması) ON_UYARI'da radarda yoktur; SIREN ve AKTIF'te "gökyüzü
 * anomalisi" olarak görünür, menzil ve boyut aranmaz.</li>
 * <li>Bölgesel afet aynı boyutta ve merkezi menzil içindeyse görünür. BITIS'te afet gösterilmez.</li>
 * <li>Sismograf yalnız deprem ve sinkhole'u algılar.</li>
 * </ul>
 */
public final class Gorunurluk {
    private Gorunurluk() {
    }

    public static RadarGorunumu radar(AfetAsamasi asama, boolean normalOnUyariBasladi, boolean global,
            boolean ayniBoyut, double mesafe, double menzil) {
        if (asama == AfetAsamasi.BITIS || asama == AfetAsamasi.ON_UYARI && (global || !normalOnUyariBasladi)) {
            return RadarGorunumu.GIZLI;
        }
        if (global) {
            return RadarGorunumu.ANOMALI;
        }
        return ayniBoyut && mesafe <= menzil ? RadarGorunumu.AFET : RadarGorunumu.GIZLI;
    }

    /** Sismografın algıladığı türler: deprem ve sinkhole (madde 13b). */
    public static boolean sismikTur(AfetTuru tur) {
        return tur == AfetTuru.DEPREM || tur == AfetTuru.SINKHOLE;
    }

    /**
     * @param asamaGecen       geçerli aşamanın başından beri geçen ölçekli tick
     * @param erkenBildirim    ON_UYARI'nın yalnız Hassas Sismograf'ın gördüğü erken bölümü (ölçekli tick)
     * @param hassasErkenOran  Hassas'ın erken bölümden gördüğü pay (0 = normalle aynı, 1 = başından)
     */
    public static boolean sismograf(AfetTuru tur, AfetAsamasi asama, long asamaGecen, long erkenBildirim,
            boolean hassas, double hassasErkenOran, boolean ayniBoyut, double mesafe, double menzil) {
        if (!sismikTur(tur) || asama == AfetAsamasi.BITIS || !ayniBoyut || mesafe > menzil) {
            return false;
        }
        if (asama != AfetAsamasi.ON_UYARI) {
            return true;
        }
        double oran = hassas ? Math.max(0, Math.min(1, hassasErkenOran)) : 0;
        return asamaGecen >= erkenBildirim * (1 - oran) - 1e-9;
    }
}
