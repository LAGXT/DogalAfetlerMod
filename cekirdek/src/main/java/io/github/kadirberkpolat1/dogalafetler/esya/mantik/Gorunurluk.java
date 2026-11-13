package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;

/**
 * Radar ve sismografın afeti ne zaman gördüğü (madde 4, 13a, 13b, N7; çekirdek §3a radar/sismograf kuralı).
 * <ul>
 * <li>ON_UYARI'da normal radar ve sismograf ancak normal pencere başlayınca ({@code Afet.normalOnUyariBasladi()}) görür;
 * Hassas Sismograf ON_UYARI'nın başından, yani çekirdeğin erken bölümünü ({@code AfetBaglami.erkenBildirimSuresi}) de görür.
 * Pencere tanımı tek kaynaktır: çekirdek.</li>
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
     * @param normalOnUyariBasladi çekirdeğin {@code Afet.normalOnUyariBasladi()} değeri (ON_UYARI dışında true)
     */
    public static boolean sismograf(AfetTuru tur, AfetAsamasi asama, boolean normalOnUyariBasladi, boolean hassas,
            boolean ayniBoyut, double mesafe, double menzil) {
        if (!sismikTur(tur) || asama == AfetAsamasi.BITIS || !ayniBoyut || mesafe > menzil) {
            return false;
        }
        return asama != AfetAsamasi.ON_UYARI || hassas || normalOnUyariBasladi;
    }
}
