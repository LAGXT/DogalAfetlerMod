package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;
import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.listeAdimi;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Asit yağmuru config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class AsitAyarGocu {
    public static final String SURUM_YOLU = "asit_yagmuru.ayarSurumu";
    public static final int GUNCEL = 1;
    /** Eşzamanlı aşınma tavanı varsayılanı: S5 hızı (26 blok/sn) × ortalama çatlak süresi (4 sn) ≈ 104. */
    public static final int ESZAMANLI = 112;

    /** 1: S4–S5 daha geniş aşınma yarıçapı, daha uzun süre ve daha hızlı aşınma. */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "asit_yagmuru.eszamanliAsindirma", ESZAMANLI, 64));

    public static final List<SurumluGocPlani.ListeAdimi> LISTE_ADIMLARI = List.of(
            listeAdimi(1, "asit_yagmuru.yaricap", List.of("32", "48", "64", "120", "192"),
                    List.of("32", "48", "64", "96", "128")),
            listeAdimi(1, "asit_yagmuru.sureSaniye", List.of("60", "90", "120", "210", "300"),
                    List.of("60", "90", "120", "180", "240")),
            listeAdimi(1, "asit_yagmuru.asindirmaBlokSaniye", List.of("2.0", "4.0", "7.0", "14.0", "26.0"),
                    List.of("2.0", "4.0", "7.0", "11.0", "16.0")));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR, LISTE_ADIMLARI);

    private AsitAyarGocu() {
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
