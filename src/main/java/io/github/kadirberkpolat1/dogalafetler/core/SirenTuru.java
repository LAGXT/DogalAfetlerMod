package io.github.kadirberkpolat1.dogalafetler.core;

/**
 * Siren türü ve süresi (madde 5, K0-19 rev. K-E67). Sireni çalan {@code uyari} paketidir; çekirdek yalnız "hangi tür, kaç
 * tick" bildirir. Güneş patlamasının kendi ürkütücü sireni vardır; ay çarpması standart sireni 25 sn çalar. Saf Java'dır.
 */
public enum SirenTuru {
    STANDART("standart"),
    GUNES("gunes");

    /** Madde 5: güneş patlaması dışındaki afetlerde 10 sn. */
    public static final int STANDART_SURE_TICK = 200;
    /**
     * Bölüm D ve H: siren başladıktan 25 sn sonra meteorlar / çarpma (K0-19 rev. K-E67: kullanıcı isteğiyle afetin
     * seçilmesinden görünmesine ~30 sn; eski 60 sn).
     */
    public static final int UZUN_SURE_TICK = 500;

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

    /**
     * AS-9: aynı anda birden çok afet siren aşamasındayken oyuncunun duyacağı sirenin önceliği (büyük olan çalar, ötekiler
     * susar): ay çarpması (25 sn) &gt; güneş patlaması &gt; diğerleri (10 sn).
     */
    public static int oncelik(AfetTuru tur) {
        return switch (tur) {
            case AY_CARPMASI -> 3;
            case GUNES_PATLAMASI -> 2;
            default -> 1;
        };
    }

    /** Sirenin ölçekli (afet saati) tick cinsinden süresi. */
    public static int sureTick(AfetTuru tur) {
        return tur.sunucuGeneli() ? UZUN_SURE_TICK : STANDART_SURE_TICK;
    }
}
