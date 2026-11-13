package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Donma config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü {@value #SURUM_YOLU}'ndadır.
 */
public final class DonmaAyarGocu {
    public static final String SURUM_YOLU = "donma.ayarSurumu";
    public static final int GUNCEL = 1;

    /** Güneş patlaması zincirinin donma aşaması (K-E72; eski 6 gün). */
    public static final double YENI_DONMA_SURESI = 2.0;

    /** Sürüme göre sıralı. 1: K-E72 (donma %0'dan %100'e 6 günden 2 güne). */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "donma.donmaSuresi", YENI_DONMA_SURESI, 6.0));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private DonmaAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
