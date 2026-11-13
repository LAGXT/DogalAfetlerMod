package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/**
 * Termometre menüsünün ve tuşunun dil anahtarları (datagen, S3-08). Mekan kalemlerinin adları {@link SicaklikCevirileri}'nin
 * {@code sicaklik.dogalafetler.katki.*} anahtarlarından gelir.
 */
public final class SicaklikMenuCevirileri {
    public static final String ON = "gui.dogalafetler.termometre.";
    public static final String TUS = "key.dogalafetler.termometre";
    public static final String KATEGORI = "key.categories.dogalafetler";
    public static final String KAPALI = ON + "kapali";

    private SicaklikMenuCevirileri() {
    }

    public static void yaz(CeviriYazici y) {
        y.ekle(TUS, "Thermometer", "Termometre");
        y.ekle(KATEGORI, "Natural Disasters", "Doğal Afetler");
        y.ekle(KAPALI, "The temperature system is off", "Sıcaklık sistemi kapalı");

        y.ekle(ON + "baslik", "Thermometer", "Termometre");
        y.ekle(ON + "vucut", "BODY", "VÜCUT");
        y.ekle(ON + "hissedilen", "FEELS LIKE", "HİSSEDİLEN");
        y.ekle(ON + "mekan", "Ambient %s", "Mekan %s");
        y.ekle(ON + "hissedilen_deger", "Feels like %s", "Hissedilen %s");
        y.ekle(ON + "ruzgar", "Wind %s km/h", "Rüzgâr %s km/h");
        y.ekle(ON + "egilim", "Body temperature changing %s °C/min", "Vücut sıcaklığı %s °C/dk değişiyor");
        y.ekle(ON + "olcek", "Body temperature scale: %1$s – %2$s °C", "Vücut sıcaklığı ölçeği: %1$s – %2$s °C");
        y.ekle(ON + "bekleniyor", "Waiting for data…", "Veri bekleniyor…");

        y.ekle(ON + "durum.normal", "Normal", "Normal");
        y.ekle(ON + "durum.usume", "Chilled", "Üşüme");
        y.ekle(ON + "durum.donma", "Freezing", "Donma");
        y.ekle(ON + "durum.bunalma", "Overheated", "Bunalma");
        y.ekle(ON + "durum.yanma", "Heatstroke", "Yanma");
        y.ekle(ON + "durum.islak", "Wet", "Islak");

        y.ekle(ON + "aciklama.islak",
                "Water and rain soak you. While wet you feel colder and lose heat faster; out of the rain you dry slowly,"
                        + " near heat much faster.",
                "Su ve yağmur seni ıslatır. Islakken daha soğuk hisseder, daha hızlı üşürsün; yağmurdan çıkınca yavaş yavaş,"
                        + " ısı başında çok daha hızlı kurursun.");
        y.ekle(ON + "aciklama.usume",
                "Your body is cooling down: you move a little slower and your breath steams. Warm up before it turns"
                        + " into freezing.",
                "Vücudun soğuyor: biraz yavaşlarsın, ağzından soğuk hava buharı çıkar. Donmaya dönmeden ısın.");
        y.ekle(ON + "aciklama.donma",
                "Hypothermia: you slow down heavily and frost creeps over the screen; at the extreme you take freezing"
                        + " damage.",
                "Donma: belirgin biçimde yavaşlarsın, ekranın kenarını buz kaplar; en uçta donma hasarı alırsın.");
        y.ekle(ON + "aciklama.bunalma",
                "Your body is overheating: you sweat and get hungry faster. Cool down in shade, in water or with cold"
                        + " food.",
                "Vücudun ısınıyor: terlersin ve daha çabuk acıkırsın. Gölgede, suda ya da soğuk yiyecekle serinle.");
        y.ekle(ON + "aciklama.yanma",
                "Heatstroke: heat haze blurs your view and hunger drains fast; at the extreme you take overheating"
                        + " damage.",
                "Yanma: sıcak hava görüşü bulanıklaştırır, açlık hızla düşer; en uçta aşırı ısınma hasarı alırsın.");
        y.ekle(ON + "etkin", "Active", "Etkin");
        y.ekle(ON + "pasif", "Inactive", "Etkin değil");
        y.ekle(ON + "esik.alt", "Body at or below %s °C", "Vücut %s °C ve altı");
        y.ekle(ON + "esik.ust", "Body at or above %s °C", "Vücut %s °C ve üstü");
        y.ekle(ON + "yuzde", "%s%%", "%%%s");
        y.ekle(ON + "islaklik_durum", "Wetness %1$s · %2$s", "Islaklık %1$s · %2$s");
        y.ekle(ON + "kuruma_kalan", "dry in %s", "%s içinde kurur");
        y.ekle(ON + "islaniyor", "Getting wet", "Islanıyor");
        y.ekle(ON + "kuru", "Dry", "Kuru");

        y.ekle(ON + "mekan_katkilari", "AMBIENT FACTORS", "MEKAN KATKILARI");
        y.ekle(ON + "hissedilen_duzeltmeleri", "FEELS-LIKE ADJUSTMENTS", "HİSSEDİLEN DÜZELTMELERİ");
        y.ekle(ON + "duzeltme.islaklik", "Wetness (%s)", "Islaklık (%s)");
        y.ekle(ON + "duzeltme.ruzgar", "Wind (%s km/h)", "Rüzgâr (%s km/h)");
        y.ekle(ON + "duzeltme.sol_el", "Off hand (%s)", "Sol el (%s)");
        y.ekle(ON + "duzeltme_yok", "No adjustments", "Düzeltme yok");
        y.ekle(ON + "yalitim", "Insulation", "Yalıtım");
        y.ekle(ON + "kuruma", "Drying", "Kuruma");
        y.ekle(ON + "sure.dk", "%1$s min %2$s s", "%1$s dk %2$s sn");
        y.ekle(ON + "sure.sn", "%s s", "%s sn");
    }
}
