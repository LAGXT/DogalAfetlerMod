package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani.adim;

import io.github.kadirberkpolat1.dogalafetler.config.SurumluGocPlani;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Meteor ve yağmur config anahtarlarının göç tablosu (MT2-31; kuralları {@link SurumluGocPlani}). Dosyanın sürümü
 * {@value #SURUM_YOLU}'ndadır.
 */
public final class MeteorAyarGocu {
    public static final String SURUM_YOLU = "meteor.ayarSurumu";
    public static final int GUNCEL = 3;

    /**
     * Sürüme göre sıralı. Ara değerler (96, 240, 24, 90, 30) yalnız {@code is/meteor-2} yapılarında varsayılandı. 2: büyük meteor
     * krateri (yarıçap 5,5→9, derinlik 4→6,5) — kullanıcı büyük meteor etrafı kırmıyor dedi. 3: büyük meteor eşyası (K-E74,
     * büyük meteorlardan item çıkma şansı daha çok olsun): Meteorit Parçası %5→%10, Kozmik Çekirdek %0,3→%0,8 (yeni ek parça
     * anahtarı buyukEkParcaSansi göç istemez).
     */
    public static final List<SurumluGocPlani.Adim> ADIMLAR = List.of(
            adim(1, "meteor.varlikTavani", 448, 48, 96, 240),
            adim(1, "meteor.girisYuksekligi", 200, 140),
            adim(1, "meteor.yagmurBuyukOrani", 0.10, 0.05),
            adim(1, "gunes_patlamasi.buyukMeteorOrani", 0.10, 0.05),
            adim(1, "gunes_patlamasi.meteorYagmuruDakikaBasina", 180.0, 24.0, 90.0),
            adim(1, "gunes_patlamasi.meteorYagmuruSaniye", 600, 180),
            adim(1, "ay_carpmasi.meteorDakikaBasina", 180.0, 7.5, 30.0, 90.0),
            adim(1, "ay_carpmasi.meteorSuresiSn", 600, 90),
            adim(1, "ay_carpmasi.meteorBuyukOrani", 0.10, 0.04),
            adim(2, "meteor.buyukKraterYaricapi", 9.0, 5.5),
            adim(2, "meteor.buyukKraterDerinligi", 6.5, 4.0),
            adim(3, "meteor.buyukParcaSansi", 0.10, 0.05),
            adim(3, "meteor.cekirdekSansi", 0.008, 0.003));

    public static final SurumluGocPlani PLAN = new SurumluGocPlani(SURUM_YOLU, GUNCEL, ADIMLAR);

    private MeteorAyarGocu() {
    }

    /** {@link SurumluGocPlani#plan} bu tabloyla. */
    public static Map<String, Number> plan(int surum, Function<String, Object> oku) {
        return PLAN.plan(surum, oku);
    }
}
