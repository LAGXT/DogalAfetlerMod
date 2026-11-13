package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Balıkçılık config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class BalikcilikAyarGocu {
    public static final String SURUM_YOLU = "balikcilik.ayarSurumu";
    public static final int GUNCEL = 2;

    /**
     * Sürüme göre sıralı. 1: ısırma süresi 30–60 sn yerine 5–30 sn (nadirliğe göre dilimlenir); Lure seviyesi başına
     * kısaltma 5 → 3 sn, taban 10 → 5 sn. 2: bu değerler Kutup Oltası'nın değeri oldu; normal olta (buz deliği ve açık deniz)
     * 2 kat yavaş: 10–60 sn, Lure 6 sn, taban 10 sn (Kutup Oltası {@code kutupHizCarpani} 0,5 ile eski 5–30 sn'yi korur).
     */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "balikcilik.isirmaEnAzSn", 5, 30),
            adim(1, "balikcilik.isirmaEnCokSn", 30, 60),
            adim(1, "balikcilik.lureKisaltmaSn", 3, 5),
            adim(1, "balikcilik.isirmaTabanSn", 5, 10),
            adim(2, "balikcilik.isirmaEnAzSn", 10, 5),
            adim(2, "balikcilik.isirmaEnCokSn", 60, 30),
            adim(2, "balikcilik.lureKisaltmaSn", 6, 3),
            adim(2, "balikcilik.isirmaTabanSn", 10, 5));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private BalikcilikAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
