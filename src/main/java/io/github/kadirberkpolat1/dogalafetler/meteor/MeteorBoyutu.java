package io.github.kadirberkpolat1.dogalafetler.meteor;

/** Meteor boyutu: krater, yangın, sarsıntı ve ödül şansı buna göre; Kozmik Çekirdek yalnız {@link #BUYUK}'te. */
public enum MeteorBoyutu {
    KUCUK,
    BUYUK;

    public boolean buyuk() {
        return this == BUYUK;
    }

    static MeteorBoyutu kimlik(int i) {
        return i == 1 ? BUYUK : KUCUK;
    }
}
