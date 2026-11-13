package io.github.kadirberkpolat1.dogalafetler.esya.termostat;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;

/**
 * Termostatlı zırh eki (sıcaklık §A3): ad tam {@code termostatli}; yalıtım girdisi bu adı termostatlı parça sayısına
 * sayar, tooltip satırı ve {@code dogalafetler:zirh_eki} tarifi kayıtlı eki tanır.
 */
public final class Termostat {
    public static final ZirhEkleri.Ek TERMOSTATLI = new ZirhEkleri.Ek("termostatli", "tooltip.dogalafetler.termostatli");
    public static final String PLAKA_ACIKLAMASI = "tooltip.dogalafetler.termostat_plakasi";

    private Termostat() {
    }

    /** Mod kurucusunda bir kez. */
    public static void kaydet() {
        ZirhEkleri.kaydet(TERMOSTATLI);
        DataGenerators.ceviriEkle(Termostat::cevirileriYaz);
    }

    static void cevirileriYaz(CeviriYazici y) {
        y.ekle(TERMOSTATLI.ceviriAnahtari(), "Thermostatic", "Termostatlı");
        y.ekle(PLAKA_ACIKLAMASI, "Craft with an armor piece: pulls body temperature toward neutral and warms you in the cold",
                "Bir zırh parçasıyla birleştir: vücut sıcaklığını nötre çeker, soğukta ortamı ısıtır");
    }
}
