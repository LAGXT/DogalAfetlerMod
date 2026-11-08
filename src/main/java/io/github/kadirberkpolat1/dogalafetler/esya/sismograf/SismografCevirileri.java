package io.github.kadirberkpolat1.dogalafetler.esya.sismograf;

import io.github.kadirberkpolat1.dogalafetler.esya.EsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Sismografın dil metinleri (datagen; madde 13b, N6: action bar yalnız taşıyan oyuncuda). */
public final class SismografCevirileri {
    public static final String ON = "esya.dogalafetler.sismograf.";

    private SismografCevirileri() {
    }

    public static void yaz(CeviriYazici y) {
        y.ekle("item.dogalafetler.sismograf", "Seismograph", "Sismograf");
        y.ekle("item.dogalafetler.sismograf.ipucu",
                "In hotbar or offhand: detects foreshocks of earthquakes and sinkholes. Use to open the 60 s graph.",
                "Hotbar'da ya da yan elde: deprem ve sinkhole öncü sarsıntılarını algılar. Sağ tık: 60 sn'lik grafik.");
        y.ekle("item.dogalafetler.hassas_sismograf", "Precision Seismograph", "Hassas Sismograf");
        y.ekle("item.dogalafetler.hassas_sismograf.ipucu", "Detects earlier, locates the epicentre precisely and reads the level.",
                "Daha erken algılar, merkezi daha doğru gösterir ve seviyeyi belirtir.");
        y.ekle("item.dogalafetler.hassas_sismograf.tarif",
                "Needs a Seismograph and a Cosmic Core first (found very rarely in meteor craters).",
                "Önce Sismograf ve Kozmik Çekirdek gerekir (meteor kraterlerinde çok nadir çıkar).");
        y.ekle(ON + "oncu", "Foreshock detected", "Öncü sarsıntı algılandı");
        y.ekle(ON + "ana", "Main shock", "Ana sarsıntı");
        y.ekle(ON + "yuzde", "%s%%", "%%%s");
        y.ekle(ON + "konum", "%s %s m", "%s %s m");
        y.ekle(ON + "seviye", "Level: %s", "Seviye: %s");
        y.ekle(ON + "sakin", "No seismic activity", "Sismik etkinlik yok");
        y.ekle(ON + "once", "60 s ago", "60 sn önce");
        y.ekle(ON + "simdi", "now", "şimdi");
        y.ekle(EsyaKayitlari.ONCU_SARSINTI_ALTYAZI, "Ground trembles", "Zemin titriyor");
    }
}
