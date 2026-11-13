package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.listeAdimi;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Deprem config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class DepremAyarGocu {
    public static final String SURUM_YOLU = "deprem.ayarSurumu";
    public static final int GUNCEL = 1;

    /**
     * 1: S4–S5 daha büyük ve yıkıcı (yarık boyu/genişliği/derinliği, süre, kamera genliği, yıkım yarıçapı, yakın hasar,
     * artçı sayısı). Liste, eski varsayılana öğe öğe eşitse yeni varsayılana çekilir.
     */
    public static final List<SurumluGocPlani.ListeAdimi> LISTE_ADIMLARI = List.of(
            listeAdimi(1, "deprem.yarikUzunlugu", List.of("24", "48", "96", "200", "400"),
                    List.of("24", "48", "96", "160", "256")),
            listeAdimi(1, "deprem.yarikGenisligi", List.of("1", "2", "3", "5", "9"), List.of("1", "2", "3", "4", "6")),
            listeAdimi(1, "deprem.yarikDerinligi", List.of("4", "8", "14", "26", "44"),
                    List.of("4", "8", "14", "22", "32")),
            listeAdimi(1, "deprem.sarsintiSuresi", List.of("8", "15", "25", "45", "90"),
                    List.of("8", "15", "25", "40", "60")),
            listeAdimi(1, "deprem.kameraGenligi", List.of("0.3", "0.8", "1.5", "2.8", "4.6"),
                    List.of("0.3", "0.8", "1.5", "2.5", "4.0")),
            listeAdimi(1, "deprem.yikimYaricapi", List.of("16", "32", "48", "84", "160"),
                    List.of("16", "32", "48", "72", "100")),
            listeAdimi(1, "deprem.yakinHasar", List.of("0.0", "0.0", "0.0", "1.5", "4.0"),
                    List.of("0.0", "0.0", "0.0", "1.0", "2.0")),
            listeAdimi(1, "deprem.artciSayisi", List.of("0", "0", "1", "3", "5"), List.of("0", "0", "1", "2", "3")));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, List.of(), LISTE_ADIMLARI);

    private DepremAyarGocu() {
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
