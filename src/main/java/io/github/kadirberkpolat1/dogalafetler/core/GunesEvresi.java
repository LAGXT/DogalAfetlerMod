package io.github.kadirberkpolat1.dogalafetler.core;

import java.util.Locale;

/**
 * Güneşin dünya dönemi evresi (madde 16, bölüm D). Ay durumundan bağımsızdır. Saf Java'dır.
 */
public enum GunesEvresi {
    NORMAL("normal"),
    /** Güneş patlamasının 5 günlük süreci: güneş büyüyor. */
    BUYUYEN("buyuyen"),
    /** Süpernova (1-2 gün). */
    SUPERNOVA("supernova"),
    /** Güneşsiz, yıldızlı/nebulalı dönem; dünya donar. */
    GUNESSIZ("gunessiz"),
    /** Kızıl cüce dönemi; buzul kısmen çekilir. */
    KIZIL_CUCE("kizil_cuce"),
    /**
     * Kızıl cüce yerine düşük olasılıkla doğan pulsar (gökyüzü-2): dünya donmanın tersine kavrulur. Paket sıra numarasıyla
     * yazıldığı için en sonda kalmalı.
     */
    PULSAR("pulsar"),
    /**
     * Pulsarın uzak bir kara delikçe yutulması (pulsar-2): 90 sn'lik gök koreografisi, ardından pulsar yolundaki
     * {@link #GUNESSIZ}. Paket sıra numarasıyla yazdığı için en sonda kalmalı.
     */
    KARADELIK_YUTMA("karadelik_yutma");

    private final String kimlik;

    GunesEvresi(String kimlik) {
        this.kimlik = kimlik;
    }

    public String kimlik() {
        return kimlik;
    }

    /** Dil dosyasındaki ad anahtarı: {@code gunes_evresi.dogalafetler.<kimlik>}. */
    public String ceviriAnahtari() {
        return "gunes_evresi.dogalafetler." + kimlik;
    }

    /** Kayıtlardaki kimliği çözer; tanınmayan değer {@link #NORMAL} olur (bozuk kayıt dünyayı durdurmaz). */
    public static GunesEvresi kimlikIle(String girdi) {
        if (girdi != null) {
            String aranan = girdi.trim().toLowerCase(Locale.ROOT);
            for (GunesEvresi evre : values()) {
                if (evre.kimlik.equals(aranan)) {
                    return evre;
                }
            }
        }
        return NORMAL;
    }
}
