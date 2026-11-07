package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;

/**
 * Meteorun ödül sınıfı: ödül şansı buna göre, Kozmik Çekirdek yalnız {@link #BUYUK}'te. Görünür boyut, krater, yangın, sarsıntı
 * ve ses sürekli ölçektendir ({@link MeteorOlcegi}); sınıf ölçeğin bantından gelir.
 */
public enum MeteorBoyutu {
    KUCUK,
    BUYUK;

    public boolean buyuk() {
        return this == BUYUK;
    }

    /** Bu sınıfın tipik ölçeği (boyut seçilerek atılan meteor). */
    public double tipikOlcek() {
        return MeteorOlcegi.tipik(buyuk());
    }

    public static MeteorBoyutu olcekten(double olcek) {
        return MeteorOlcegi.buyuk(olcek) ? BUYUK : KUCUK;
    }

    static MeteorBoyutu kimlik(int i) {
        return i == 1 ? BUYUK : KUCUK;
    }
}
