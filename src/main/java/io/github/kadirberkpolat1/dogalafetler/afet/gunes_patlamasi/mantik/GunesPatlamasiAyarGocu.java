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
    public static final int GUNCEL = 2;

    /**
     * Sürüme göre sıralı. 1: K-E67 (siren 25 sn; öncü yağmur AKTIF'ten önceki son 30 sn). 2: K-E72 (AKTIF 5 günden 1 güne;
     * kavrulma başlangıcı 3 günden 0,4 güne, yoksa yeni sürenin dışında kalıp hiç çalışmazdı).
     */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "gunes_patlamasi.meteorYagmuruOncuSaniye", 5, 90),
            adim(2, "gunes_patlamasi.aktifSureGun", 1.0, 5.0),
            adim(2, "gunes_patlamasi.kavrulmaBaslangicGun", 0.4, 3.0));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private GunesPatlamasiAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
