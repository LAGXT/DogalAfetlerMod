package io.github.kadirberkpolat1.dogalafetler.afet.tsunami.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Tsunami config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class TsunamiAyarGocu {
    public static final String SURUM_YOLU = "tsunami.ayarSurumu";
    public static final int GUNCEL = 1;

    /** Sürüme göre sıralı. 1: K-E67 (dalga AKTIF'ten 2 sn sonra doğar, çekilme vuruştan önce, 8 sn'de biter). */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "tsunami.dalgaGecikmeSn", 2.0, 10.0),
            adim(1, "tsunami.cekilmeSn", 8.0, 18.0));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private TsunamiAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
