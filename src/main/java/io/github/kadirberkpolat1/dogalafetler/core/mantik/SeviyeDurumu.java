package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Bir oyun günündeki seviye olasılıkları ve olasılığı 0 olan seviyelerin nedeni ({@code /dogalafet gun} ve {@code durum},
 * K-E67). Doğal seçimin kullandığı değerlerin aynısıdır: gelişim kriteri, {@code afetMaksSiddet}, art arda 4-5 kuralı ve
 * yoğunluk zayıflatması dahil. Diziler sıfır tabanlıdır: indeks 0 = Seviye 1.
 *
 * @param olasilik  S1–S5 olasılıkları (toplam 1)
 * @param nedenler  her seviye için kapalılık nedeni; açık seviyede boş
 * @param maks      {@code afetMaksSiddet}
 * @param yuk       afet yoğunluğu ({@link AfetYogunlugu#yuk})
 */
public record SeviyeDurumu(double[] olasilik, List<Optional<SiddetSecici.KapaliNeden>> nedenler, int maks, double yuk) {
    public SeviyeDurumu {
        olasilik = olasilik.clone();
        nedenler = List.copyOf(nedenler);
    }

    public static SeviyeDurumu hesapla(SiddetAyarlari ayarlar, double gercekGun, double etkinGun, int maks,
            boolean oncekiYuksek, double yuk) {
        double[] olasilik = SiddetSecici.olasiliklar(ayarlar, etkinGun, maks, oncekiYuksek, yuk);
        List<Optional<SiddetSecici.KapaliNeden>> nedenler = new ArrayList<>();
        for (int seviye = AfetTuru.EN_DUSUK_SEVIYE; seviye <= AfetTuru.EN_YUKSEK_SEVIYE; seviye++) {
            nedenler.add(olasilik[seviye - 1] > 0 ? Optional.empty()
                    : SiddetSecici.kapaliNedeni(ayarlar, gercekGun, etkinGun, seviye, maks, oncekiYuksek));
        }
        return new SeviyeDurumu(olasilik, nedenler, maks, yuk);
    }

    @Override
    public double[] olasilik() {
        return olasilik.clone();
    }

    /** Seviyenin (1–5) olasılığı. */
    public double olasilik(int seviye) {
        return olasilik[seviye - 1];
    }

    /** Seviyenin (1–5) kapalılık nedeni. */
    public Optional<SiddetSecici.KapaliNeden> neden(int seviye) {
        return nedenler.get(seviye - 1);
    }

    /** Yoğunluk zayıflatması etkin mi. */
    public boolean zayiflatildi() {
        return AfetYogunlugu.zayiflatma(yuk) > 1.0;
    }
}
