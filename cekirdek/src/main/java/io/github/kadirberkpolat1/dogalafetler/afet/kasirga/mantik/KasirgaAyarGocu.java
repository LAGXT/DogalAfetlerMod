package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Kasırga config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class KasirgaAyarGocu {
    public static final String SURUM_YOLU = "kasirga.ayarSurumu";
    public static final int GUNCEL = 2;

    /**
     * Sürüme göre sıralı. 1: afet-fizik-3 (kaldırma tavanı S5 huni yüksekliğinin dörtte biri, AF3-05). 2: S5 büyüdü (güç 4,8 → 6,
     * huni 144 → 180 blok): tavan 36 → 45, 25 ve 36'dan gelen dosya zincirle 45'e ulaşır.
     */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "kasirga.kaldirmaTavani", 36.0, 25.0),
            adim(2, "kasirga.kaldirmaTavani", KasirgaSeviyesi.S5_KALDIRMA_TAVANI, 36.0));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private KasirgaAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
