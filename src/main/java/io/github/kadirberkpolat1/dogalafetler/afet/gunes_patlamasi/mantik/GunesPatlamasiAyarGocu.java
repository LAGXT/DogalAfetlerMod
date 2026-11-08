package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Güneş patlaması config anahtarlarının göç tablosu (K-E61; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır. Meteor yoğunluklarının göçü meteor tablosundadır ({@code MeteorAyarGocu}).
 */
public final class GunesPatlamasiAyarGocu {
    public static final String SURUM_YOLU = "gunes_patlamasi.ayarSurumu";
    public static final int GUNCEL = 1;

    /** Sürüme göre sıralı. 1: K-E67 (siren 25 sn; öncü yağmur AKTIF'ten önceki son 30 sn). */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "gunes_patlamasi.meteorYagmuruOncuSaniye", 5, 90));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private GunesPatlamasiAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
