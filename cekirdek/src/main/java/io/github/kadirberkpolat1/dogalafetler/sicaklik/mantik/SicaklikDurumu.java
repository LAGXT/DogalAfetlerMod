package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import java.util.Locale;

/**
 * Oyuncuya görünen dört durum ve normal (S3-04). Üşüme donmadan, bunalma yanmadan daha ılım vucut sıcaklığında başlar;
 * ikisi de hasarsızdır. {@link Kademe#durum()} eşlemeyi verir.
 */
public enum SicaklikDurumu {
    NORMAL,
    USUME,
    DONMA,
    BUNALMA,
    YANMA;

    /** Dil anahtarının son parçası ({@code sicaklik.dogalafetler.durum.<anahtar>}). */
    public String anahtar() {
        return name().toLowerCase(Locale.ROOT);
    }

    public boolean soguk() {
        return this == USUME || this == DONMA;
    }

    public boolean sicak() {
        return this == BUNALMA || this == YANMA;
    }
}
