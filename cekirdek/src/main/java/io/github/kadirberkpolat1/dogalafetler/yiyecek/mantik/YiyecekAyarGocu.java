package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.listeAdimi;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Yiyecek config anahtarlarının göç tablosu (K-E61, S3-09; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır. Bölüm ve anahtar adları {@code YiyecekAyarlari} spec'i de buradan alır.
 */
public final class YiyecekAyarGocu {
    public static final String BOLUM = "yiyecek";
    public static final String ISINMA_LISTESI = "vanillaYiyecekler";
    public static final String SURUM_YOLU = BOLUM + ".ayarSurumu";
    public static final int GUNCEL = 1;

    /** Sürüme göre sıralı. 1: sıcaklık-3 (vanilla ısınma listesi kademeli ve genişletilmiş). */
    public static final List<SurumluGocPlani.ListeAdimi> LISTE_ADIMLARI = List.of(
            listeAdimi(1, BOLUM + "." + ISINMA_LISTESI, VanillaSicaklik.VARSAYILAN_ISINMA, VanillaSicaklik.ESKI_ISINMA));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, List.of(), LISTE_ADIMLARI);

    private YiyecekAyarGocu() {
    }

    /** {@link SurumluGocPlani#listePlani} bu tabloyla. */
    public static Map<String, List<String>> listePlani(int surum, Function<String, Object> oku) {
        return PLAN.listePlani(surum, oku);
    }
}
