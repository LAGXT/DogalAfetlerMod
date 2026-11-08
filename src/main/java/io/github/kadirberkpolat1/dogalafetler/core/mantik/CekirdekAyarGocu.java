package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Çekirdek zamanlayıcı config anahtarlarının göç tablosu (K-E61, K-C2 rev. 2; kuralları {@link SurumluGocPlani}). Dosyanın
 * sürümü {@value #SURUM_YOLU}'ndadır.
 */
public final class CekirdekAyarGocu {
    public static final String SURUM_YOLU = "cekirdek.ayarSurumu";
    public static final int GUNCEL = 1;

    /** Sürüme göre sıralı. Sürüm 1: kullanıcı ilk afeti ilk oyun gününde görsün (AS-11). */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = adimlar();

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private CekirdekAyarGocu() {
    }

    private static List<SurumluGocPlani.Adim> adimlar() {
        List<SurumluGocPlani.Adim> liste = new ArrayList<>();
        liste.add(adim(1, "cekirdek.zamanlayici.baslangicBeklemeTick", 6_000L, 24_000L));
        liste.add(adim(1, "cekirdek.zamanlayici.genelBeklemeTick", 6_000L, 12_000L));
        for (AfetTuru tur : AfetTuru.values()) {
            if (!tur.sunucuGeneli()) {
                liste.add(adim(1, "cekirdek.afetler." + tur.kimlik() + ".beklemeTick", 24_000L, 48_000L));
            }
        }
        return List.copyOf(liste);
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
