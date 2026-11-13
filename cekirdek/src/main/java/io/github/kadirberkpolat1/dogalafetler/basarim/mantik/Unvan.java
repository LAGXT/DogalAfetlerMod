package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import java.util.Locale;
import java.util.Optional;

/**
 * Meydan okuma başarımlarının verdiği kozmetik ünvanlar (madde 12, N16). Oyuncu verisinde kimliğiyle saklanır; sohbet ve
 * tab listesindeki adın önünde {@code [<ad>]} olarak görünür. Ad istemcinin dilinde çevrilir.
 */
public enum Unvan {
    KUTUP_SAKINI("kutup_sakini", "Polar Dweller", "Kutup Sakini"),
    KOZMIK_KASIF("kozmik_kasif", "Cosmic Explorer", "Kozmik Kâşif"),
    FELAKET_USTASI("felaket_ustasi", "Master of Disaster", "Felaket Ustası"),
    HER_SEYI_ATLATAN("her_seyi_atlatan", "Survivor of All", "Her Şeyi Atlatan"),
    ASTRONOM("astronom", "Astronomer", "Astronom");

    private final String kimlik;
    private final String ingilizceAd;
    private final String turkceAd;

    Unvan(String kimlik, String ingilizceAd, String turkceAd) {
        this.kimlik = kimlik;
        this.ingilizceAd = ingilizceAd;
        this.turkceAd = turkceAd;
    }

    public String kimlik() {
        return kimlik;
    }

    public String ingilizceAd() {
        return ingilizceAd;
    }

    public String turkceAd() {
        return turkceAd;
    }

    public String ceviriAnahtari() {
        return "unvan.dogalafetler." + kimlik;
    }

    /** Kimliği büyük-küçük harf fark etmeksizin çözer ({@code Locale.ROOT}). */
    public static Optional<Unvan> kimlikIle(String girdi) {
        if (girdi == null) {
            return Optional.empty();
        }
        String aranan = girdi.trim().toLowerCase(Locale.ROOT);
        for (Unvan u : values()) {
            if (u.kimlik.equals(aranan)) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }
}
