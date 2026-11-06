package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import java.util.Locale;

/** Ortam sıcaklığının kalemleri ({@code /dogalafet sicaklik bilgi} dökümü aynı sırayla yazar). */
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
    KAYNAKLAR;

    /** Dil anahtarının son parçası (küçük ASCII). */
    public String anahtar() {
        return name().toLowerCase(Locale.ROOT);
    }
}
