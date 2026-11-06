package io.github.kadirberkpolat1.dogalafetler.core;

/**
 * Siren türü ve süresi (madde 5, K0-19). Sireni çalan {@code uyari} paketidir; çekirdek yalnız "hangi tür, kaç tick"
 * bildirir. Güneş patlamasının kendi ürkütücü sireni vardır; ay çarpması standart sireni 60 sn çalar. Saf Java'dır.
 */
public enum SirenTuru {
    STANDART("standart"),
    GUNES("gunes");

    /** Madde 5: güneş patlaması dışındaki afetlerde 10 sn. */
    public static final int STANDART_SURE_TICK = 200;
    /** Bölüm D ve H: siren başladıktan 60 sn sonra meteorlar / çarpma (K0-19). */
    public static final int UZUN_SURE_TICK = 1200;

    private final String kimlik;

    SirenTuru(String kimlik) {
        this.kimlik = kimlik;
    }

    public String kimlik() {
        return kimlik;
    }

    public static SirenTuru afetIcin(AfetTuru tur) {
        return tur == AfetTuru.GUNES_PATLAMASI ? GUNES : STANDART;
    }

    /** Sirenin ölçekli (afet saati) tick cinsinden süresi. */
    public static int sureTick(AfetTuru tur) {
        return tur.sunucuGeneli() ? UZUN_SURE_TICK : STANDART_SURE_TICK;
    }
}
