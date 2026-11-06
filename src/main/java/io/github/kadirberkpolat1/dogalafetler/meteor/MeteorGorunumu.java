package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;

/**
 * Meteorun görünümü (tasarım notları): {@link #GUNES} lav/magma gövde, beyaz-sarı plazma, turuncu iz; {@link #AY} gri-beyaz
 * ay kayası (bazalt + kalsit), mavi-beyaz plazma izi. Krater astarı ve kenar enkazı da görünüme göre seçilir.
 */
public enum MeteorGorunumu {
    GUNES,
    AY;

    /** Ay çarpması kaynağı ay görünümünü, diğerleri (güneş patlaması, komut) güneş görünümünü alır. */
    public static MeteorGorunumu kaynaktan(YanginKaynagi kaynak) {
        return kaynak != null && kaynak.tur() == AfetTuru.AY_CARPMASI ? AY : GUNES;
    }

    static MeteorGorunumu kimlik(int i) {
        return i == 1 ? AY : GUNES;
    }
}
