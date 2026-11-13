package io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.listeAdimi;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Sel config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü {@value #SURUM_YOLU}'ndadır.
 */
public final class SelAyarGocu {
    public static final String SURUM_YOLU = "sel.ayarSurumu";
    public static final int GUNCEL = 1;

    /** 1: K-E81 (sel daha çok ve daha hızlı yükselir: S1 5 blok / 20 sn, S5 12 blok / 40 sn). */
    public static final List<SurumluGocPlani.ListeAdimi> LISTE_ADIMLARI = List.of(
            listeAdimi(1, "sel.artisBlok", List.of("5.0", "6.75", "8.5", "10.25", "12.0"),
                    List.of("2.0", "3.0", "4.0", "5.5", "7.5")),
            listeAdimi(1, "sel.yukselmeSaniye", List.of("20", "25", "30", "35", "40"),
                    List.of("45", "60", "75", "90", "105")));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, List.of(), LISTE_ADIMLARI);

    private SelAyarGocu() {
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
