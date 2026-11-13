package io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.listeAdimi;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Sinkhole config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class SinkholeAyarGocu {
    public static final String SURUM_YOLU = "sinkhole.ayarSurumu";
    public static final int GUNCEL = 1;

    /** 1: S4–S5 daha büyük çukur (çap, derinlik, çatlama/çöküş süresi, basamak, düşen blok hasarı). */
    public static final List<SurumluGocPlani.ListeAdimi> LISTE_ADIMLARI = List.of(
            listeAdimi(1, "sinkhole.cap", List.of("6", "10", "16", "28", "48"), List.of("6", "10", "16", "24", "34")),
            listeAdimi(1, "sinkhole.derinlik", List.of("6", "10", "16", "28", "46"),
                    List.of("6", "10", "16", "24", "32")),
            listeAdimi(1, "sinkhole.catlamaSuresi", List.of("4", "4", "5", "6", "8"), List.of("4", "4", "5", "6", "7")),
            listeAdimi(1, "sinkhole.cokusSuresi", List.of("3", "4", "6", "9", "13"), List.of("3", "4", "6", "8", "10")),
            listeAdimi(1, "sinkhole.basamak", List.of("1", "1", "2", "3", "4"), List.of("1", "1", "2", "2", "3")),
            listeAdimi(1, "sinkhole.dusenBlokHasari", List.of("0.0", "0.0", "4.0", "7.0", "12.0"),
                    List.of("0.0", "0.0", "4.0", "6.0", "8.0")));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, List.of(), LISTE_ADIMLARI);

    private SinkholeAyarGocu() {
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
