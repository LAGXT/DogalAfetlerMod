package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Sıcaklık sisteminin dil anahtarları (datagen). */
public final class SicaklikCevirileri {
    private static final String K = "commands.dogalafetler.sicaklik.";

    private SicaklikCevirileri() {
    }

    public static void yaz(CeviriYazici y) {
        y.ekle("tooltip.dogalafetler.tuy", "Feather Upgrade: %s", "Tüy Upgrade'i: %s");
        y.ekle("tooltip.dogalafetler.yunlu", "Woolen", "Yünlü");

        y.ekle("death.attack.dogalafetler.donma", "%1$s froze to death", "%1$s donarak öldü");
        y.ekle("death.attack.dogalafetler.donma.player", "%1$s froze to death while fighting %2$s",
                "%1$s, %2$s ile savaşırken donarak öldü");
        y.ekle("death.attack.dogalafetler.asiri_isinma", "%1$s died of heatstroke", "%1$s sıcak çarpmasından öldü");
        y.ekle("death.attack.dogalafetler.asiri_isinma.player", "%1$s died of heatstroke while fighting %2$s",
                "%1$s, %2$s ile savaşırken sıcak çarpmasından öldü");

        y.ekle(K + "set", "Set %1$s's body temperature to %2$s (%3$s)",
                "%1$s oyuncusunun vücut sıcaklığı %2$s (%3$s) yapıldı");
        y.ekle(K + "kapali", "The temperature system is currently off: %s", "Sıcaklık sistemi şu an kapalı: %s");
        y.ekle(K + "acik", "on", "açık");
        y.ekle(K + "kapali_kisa", "off", "kapalı");
        y.ekle(K + "bilgi.baslik", "Temperature of %s:", "%s sıcaklık bilgisi:");
        y.ekle(K + "bilgi.kapi", "System: %1$s (%2$s)", "Sistem: %1$s (%2$s)");
        y.ekle(K + "bilgi.vucut", "Body: %1$s (%2$s), trend %3$s/s, activity %4$s, wetness %5$s",
                "Vücut: %1$s (%2$s), eğilim %3$s/sn, etkinlik %4$s, ıslaklık %5$s");
        y.ekle(K + "bilgi.ortam", "Ambient: %1$s °C (body units %2$s) — %3$s",
                "Ortam: %1$s °C (vücut birimi %2$s) — %3$s");
        y.ekle(K + "bilgi.yalitim",
                "Insulation: total %1$s (armor %2$s, woolen %3$s, feathers %4$s, thermostat %5$s); cooling multiplier %6$s",
                "Yalıtım: toplam %1$s (zırh %2$s, yünlü %3$s, tüy %4$s, termostatlı %5$s); soğuma çarpanı %6$s");
        y.ekle(K + "bilgi.etkiler", "Food effects: %1$s (remaining %2$s)", "Yiyecek etkileri: %1$s (kalan %2$s)");

        y.ekle("sicaklik.dogalafetler.kademe.donuyor", "freezing", "donuyor");
        y.ekle("sicaklik.dogalafetler.kademe.cok_soguk", "very cold", "çok soğuk");
        y.ekle("sicaklik.dogalafetler.kademe.soguk", "cold", "soğuk");
        y.ekle("sicaklik.dogalafetler.kademe.notr", "neutral", "nötr");
        y.ekle("sicaklik.dogalafetler.kademe.sicak", "hot", "sıcak");
        y.ekle("sicaklik.dogalafetler.kademe.cok_sicak", "very hot", "çok sıcak");
        y.ekle("sicaklik.dogalafetler.kademe.yaniyor", "overheating", "yanıyor");

        y.ekle("sicaklik.dogalafetler.katki.biyom", "biome", "biyom");
        y.ekle("sicaklik.dogalafetler.katki.gun_gece", "day/night", "gün/gece");
        y.ekle("sicaklik.dogalafetler.katki.yukseklik", "altitude", "yükseklik");
        y.ekle("sicaklik.dogalafetler.katki.yagis", "precipitation", "yağış");
        y.ekle("sicaklik.dogalafetler.katki.gunes", "sunlight", "güneş");
        y.ekle("sicaklik.dogalafetler.katki.yeralti", "underground", "yeraltı");
        y.ekle("sicaklik.dogalafetler.katki.kapali_alan", "indoors", "kapalı alan");
        y.ekle("sicaklik.dogalafetler.katki.gunes_evresi", "sun phase", "güneş evresi");
        y.ekle("sicaklik.dogalafetler.katki.donma", "world freeze", "dünya donması");
        y.ekle("sicaklik.dogalafetler.katki.kizil_cuce", "red dwarf", "kızıl cüce");
        y.ekle("sicaklik.dogalafetler.katki.kis_bolgesi", "winter zone", "kış bölgesi");
        y.ekle("sicaklik.dogalafetler.katki.boyut", "dimension", "boyut");
        y.ekle("sicaklik.dogalafetler.katki.su", "water", "su");
        y.ekle("sicaklik.dogalafetler.katki.kaynaklar", "heat sources", "ısı kaynakları");
    }
}
