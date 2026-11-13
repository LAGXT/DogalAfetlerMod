package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Gökyüzü config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class GokyuzuAyarGocu {
    public static final String SURUM_YOLU = "gokyuzu.ayarSurumu";
    public static final int GUNCEL = 2;

    /**
     * Sürüme göre sıralı. 1: pulsar-2 (ısınma 1 gün, yangın dakikada 24). 2: K-E72 (güneş patlaması zinciri kısalır: süpernova
     * 1,5 günden 1 güne, kızıl cüce kapısı 7 günden 3 güne).
     */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "gokyuzu.pulsarIsinmaGun", 1.0, 2.0),
            adim(1, "gokyuzu.pulsarYanginDakikaBasina", 24.0, 20.0),
            adim(2, "gokyuzu.supernovaSuresiGun", 1.0, 1.5),
            adim(2, "gokyuzu.kizilCuceGecikmeGun", 3.0, 7.0));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private GokyuzuAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
