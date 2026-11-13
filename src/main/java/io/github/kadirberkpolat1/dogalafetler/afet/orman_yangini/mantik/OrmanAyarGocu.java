package io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.listeAdimi;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Orman yangını config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class OrmanAyarGocu {
    public static final String SURUM_YOLU = "orman_yangini.ayarSurumu";
    public static final int GUNCEL = 1;

    /** 1: S4–S5 daha büyük başlangıç alanı, daha uzun süre ve (S4) daha yüksek yoğunluk. */
    public static final List<SurumluGocPlani.ListeAdimi> LISTE_ADIMLARI = List.of(
            listeAdimi(1, "orman_yangini.baslangicYaricapi", List.of("3", "6", "10", "20", "40"),
                    List.of("3", "6", "10", "16", "24")),
            listeAdimi(1, "orman_yangini.yogunluk", List.of("0.0", "0.2", "0.4", "0.8", "1.0"),
                    List.of("0.0", "0.2", "0.4", "0.7", "1.0")),
            listeAdimi(1, "orman_yangini.sureSaniye", List.of("120", "180", "240", "420", "720"),
                    List.of("120", "180", "240", "360", "480")));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, List.of(), LISTE_ADIMLARI);

    private OrmanAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }

    /** {@link SurumluGocPlani#listePlani} bu tabloyla. */
    public static Map<String, List<String>> listePlani(int surum, Function<String, Object> oku) {
        return PLAN.listePlani(surum, oku);
    }
}
