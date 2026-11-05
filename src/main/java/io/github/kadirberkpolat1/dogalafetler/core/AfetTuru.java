package io.github.kadirberkpolat1.dogalafetler.core;

import java.util.Locale;
import java.util.Optional;

/**
 * Şartnamedeki sekiz afet (N18'deki komut adlarıyla). Yeni afet eklenmez (şartname girişi).
 * Saf Java'dır; Minecraft sınıflarına dokunmaz.
 */
public enum AfetTuru {
    KASIRGA("kasirga", true, "Tornado", "Kasırga"),
    TSUNAMI("tsunami", true, "Tsunami", "Tsunami"),
    DEPREM("deprem", true, "Earthquake", "Deprem"),
    GUNES_PATLAMASI("gunes_patlamasi", false, "Solar Flare", "Güneş Patlaması"),
    ASIT_YAGMURU("asit_yagmuru", true, "Acid Rain", "Asit Yağmuru"),
    SINKHOLE("sinkhole", true, "Sinkhole", "Sinkhole"),
    ORMAN_YANGINI("orman_yangini", true, "Wildfire", "Orman Yangını"),
    AY_CARPMASI("ay_carpmasi", false, "Moon Impact", "Ay Çarpması");

    /** Madde 10: seviyeli afetlerin en hafif seviyesi (Hafif). */
    public static final int EN_DUSUK_SEVIYE = 1;
    /** Madde 10: seviyeli afetlerin en ağır seviyesi (Felaket). */
    public static final int EN_YUKSEK_SEVIYE = 5;

    private final String kimlik;
    private final boolean seviyeli;
    private final String ingilizceAd;
    private final String turkceAd;

    AfetTuru(String kimlik, boolean seviyeli, String ingilizceAd, String turkceAd) {
        this.kimlik = kimlik;
        this.seviyeli = seviyeli;
        this.ingilizceAd = ingilizceAd;
        this.turkceAd = turkceAd;
    }

    /** Komutlarda ve kayıtlarda kullanılan küçük harfli ad (ör. {@code asit_yagmuru}). */
    public String kimlik() {
        return kimlik;
    }

    /** Güneş patlaması ve ay çarpması seviyesizdir (madde 10). */
    public boolean seviyeli() {
        return seviyeli;
    }

    public String ingilizceAd() {
        return ingilizceAd;
    }

    public String turkceAd() {
        return turkceAd;
    }

    /** Dil dosyasındaki ad anahtarı: {@code afet.dogalafetler.<kimlik>}. */
    public String ceviriAnahtari() {
        return "afet.dogalafetler." + kimlik;
    }

    /**
     * Kimliği büyük-küçük harf fark etmeksizin çözer. Varsayılan yerel Türkçe olsa da {@code Locale.ROOT} ile
     * küçültülür; aksi halde "KASIRGA" "kasırga" olur ve eşleşmez.
     */
    public static Optional<AfetTuru> kimlikIle(String girdi) {
        if (girdi == null) {
            return Optional.empty();
        }
        String aranan = girdi.trim().toLowerCase(Locale.ROOT);
        for (AfetTuru tur : values()) {
            if (tur.kimlik.equals(aranan)) {
                return Optional.of(tur);
            }
        }
        return Optional.empty();
    }
}
