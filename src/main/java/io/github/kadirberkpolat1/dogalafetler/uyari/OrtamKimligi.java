package io.github.kadirberkpolat1.dogalafetler.uyari;

import java.util.regex.Pattern;

/**
 * Ortam katmanı kimliği {@code <alt sistem>/<katman>} (saf mantık): küçük ASCII, alt sistem en çok
 * {@value #EN_UZUN_ALT_SISTEM}, katman en çok {@value #EN_UZUN_KATMAN} karakter; toplam {@value #EN_UZUN} karakter ağa
 * giden paketin sınırıdır. Geçersiz kimlik {@code OrtamSesi.baslat}'ta reddedilir, paket kodlanırken değil.
 */
public final class OrtamKimligi {
    public static final int EN_UZUN_ALT_SISTEM = 15;
    public static final int EN_UZUN_KATMAN = 48;
    public static final int EN_UZUN = EN_UZUN_ALT_SISTEM + 1 + EN_UZUN_KATMAN;

    private static final Pattern BICIM = Pattern.compile(
            "[a-z0-9_]{1," + EN_UZUN_ALT_SISTEM + "}/[a-z0-9_./-]{1," + EN_UZUN_KATMAN + "}");

    private OrtamKimligi() {
    }

    public static boolean gecerli(String kimlik) {
        return kimlik != null && BICIM.matcher(kimlik).matches();
    }

    public static String dogrula(String kimlik) {
        if (!gecerli(kimlik)) {
            throw new IllegalArgumentException("Ortam katmanı kimliği <alt sistem ≤ " + EN_UZUN_ALT_SISTEM + ">/<katman ≤ "
                    + EN_UZUN_KATMAN + "> (küçük ASCII) olmalı: " + kimlik);
        }
        return kimlik;
    }
}
