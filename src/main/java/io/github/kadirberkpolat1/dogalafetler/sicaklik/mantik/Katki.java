package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import java.util.Locale;

/**
 * Ortam sıcaklığının kalemleri ({@code /dogalafet sicaklik bilgi} dökümü aynı sırayla yazar). {@code BIYOM}…{@code KAYNAKLAR}
 * mekanın, {@code ISLAKLIK}…{@code SOL_EL} ({@link #hissedilen()}) kişisel hissedilen düzeltmeleridir (S3-02);
 * {@code YUN_BLOKLARI}…{@code SOGUTUCU_ESYA} çevre ve ekipmandan gelen ek mekan kalemleridir ({@code EkIsi},
 * {@code IsiKaynagi}; menüde mekan katkıları arasında görünür). Yalnız sona eklenir: sıra döküm paketinde dizin olarak
 * gider.
 */
public enum Katki {
    BIYOM,
    GUN_GECE,
    YUKSEKLIK,
    YAGIS,
    GUNES,
    YERALTI,
    KAPALI_ALAN,
    GUNES_EVRESI,
    DONMA,
    KIZIL_CUCE,
    KIS_BOLGESI,
    BOYUT,
    SU,
    KAYNAKLAR,
    ISLAKLIK,
    RUZGAR,
    SOL_EL,
    YUN_BLOKLARI,
    LAV_ICINDE,
    YAKIN_OYUNCULAR,
    YUN,
    TERMOSTAT,
    ISITICI_ESYA,
    SOGUTUCU_ESYA;

    /** Hissedilen düzeltmesi mi (mekan kalemi değil). */
    public boolean hissedilen() {
        return this == ISLAKLIK || this == RUZGAR || this == SOL_EL;
    }

    /** Dil anahtarının son parçası (küçük ASCII). */
    public String anahtar() {
        return name().toLowerCase(Locale.ROOT);
    }
}
