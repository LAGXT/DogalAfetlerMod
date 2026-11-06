package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Meteor eşyaları, varlığı ve komut çıktıları (en_us, tr_tr). */
final class MeteorCevirileri {
    static final String CEKIRDEK_ACIKLAMASI = "tooltip.dogalafetler.kozmik_cekirdek";

    private MeteorCevirileri() {
    }

    static void yaz(CeviriYazici y) {
        y.ekle("item.dogalafetler.meteorit_parcasi", "Meteorite Fragment", "Meteorit Parçası");
        y.ekle("item.dogalafetler.kozmik_cekirdek", "Cosmic Core", "Kozmik Çekirdek");
        y.ekle("item.dogalafetler.meteorit_kulcesi", "Meteorite Ingot", "Meteorit Külçesi");
        y.ekle("item.dogalafetler.termostat_plakasi", "Thermostat Plate", "Termostat Plakası");
        y.ekle("entity.dogalafetler.meteor", "Meteor", "Meteor");
        y.ekle(CEKIRDEK_ACIKLAMASI, "Hums with cosmic energy", "Kozmik enerjiyle titreşiyor");
        y.ekle(MeteorKomutlari.DUSUR, "Meteor (%s) falling towards %s, %s", "Meteor (%s) %s, %s noktasına düşüyor");
        y.ekle(MeteorKomutlari.DUSUR_HEMEN, "Meteor (%s) struck %s, %s at once (target chunk is not entity-ticking)",
                "Meteor (%s) %s, %s noktasına hemen çarptı (hedef chunk varlık işlemiyor)");
        y.ekle(MeteorKomutlari.DUSUR_YOK, "No meteor: the target chunk is not loaded or the meteor cap (%s) is reached",
                "Meteor yok: hedef chunk yüklü değil ya da meteor tavanı (%s) dolu");
        y.ekle(MeteorKomutlari.YAGMUR, "Meteor shower #%s started: %s s, %s per player per minute",
                "Meteor yağmuru #%s başladı: %s sn, oyuncu başına dakikada %s");
        y.ekle(MeteorKomutlari.DURDUR, "Stopped %s command meteor shower(s); flying command meteors removed",
                "%s komut meteor yağmuru durdu; uçan komut meteorları kaldırıldı");
        y.ekle(MeteorKomutlari.BILGI, "Flying meteors: %s/%s, showers: %s, pending craters: %s (%s waiting for unloaded chunks)",
                "Uçan meteor: %s/%s, yağmur: %s, bekleyen krater: %s (%s yüklü olmayan chunk bekliyor)");
    }
}
