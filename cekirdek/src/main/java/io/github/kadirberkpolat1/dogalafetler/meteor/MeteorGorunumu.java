package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;

/**
 * Meteorun görünümü (tasarım notları): {@link #GUNES} lav/magma gövde, beyaz-sarı plazma, turuncu iz; {@link #AY} gri-beyaz
 * ay kayası (bazalt + kalsit), mavi-beyaz plazma izi; {@link #PULSAR} güneşinkiyle aynı gövde, beyaz çekirdekli kızıl
 * plazma ve iz, kor kıvılcımları (pulsar dönemi yağmuru; krater ve ses güneşinki). Krater astarı ve kenar enkazı da
 * görünüme göre seçilir. Pakette ve kayıtta sıra numarasıyla yazılır: yeni görünüm sona eklenir.
 */
public enum MeteorGorunumu {
    GUNES,
    AY,
    PULSAR;

    private static final MeteorGorunumu[] DEGERLER = values();

    /** Ay çarpması kaynağı ay görünümünü, diğerleri (güneş patlaması, komut) güneş görünümünü alır. */
    public static MeteorGorunumu kaynaktan(YanginKaynagi kaynak) {
        return kaynak != null && kaynak.tur() == AfetTuru.AY_CARPMASI ? AY : GUNES;
    }

    static MeteorGorunumu kimlik(int i) {
        return i >= 0 && i < DEGERLER.length ? DEGERLER[i] : GUNES;
    }
}
