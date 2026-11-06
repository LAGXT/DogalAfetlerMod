package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Config'teki bir ısı/soğuk kaynağı satırı: {@code blok=güç:yarıçap} (ör. {@code minecraft:lava=40:6}). Güç kaynağın
 * üstündeki °C katkısıdır (negatif = soğuk), yarıçap blok. Başka modların blokları aynı biçimle eklenir.
 */
public record KaynakTanimi(String blok, double guc, double yaricap) {
    public static final double EN_BUYUK_YARICAP = 8;

    public static KaynakTanimi coz(String satir) {
        String s = satir == null ? "" : satir.trim();
        int esit = s.indexOf('=');
        int iki = s.lastIndexOf(':');
        if (esit <= 0 || iki <= esit) {
            throw new IllegalArgumentException("Kaynak biçimi blok=güç:yarıçap olmalı: " + satir);
        }
        String blok = s.substring(0, esit).trim();
        try {
            double guc = Double.parseDouble(s.substring(esit + 1, iki).trim());
            double yaricap = Double.parseDouble(s.substring(iki + 1).trim());
            if (!(yaricap > 0 && yaricap <= EN_BUYUK_YARICAP) || !Double.isFinite(guc)) {
                throw new IllegalArgumentException("Yarıçap (0, " + EN_BUYUK_YARICAP + "] olmalı: " + satir);
            }
            return new KaynakTanimi(blok, guc, yaricap);
        } catch (NumberFormatException hata) {
            throw new IllegalArgumentException("Kaynak sayıları okunamadı: " + satir, hata);
        }
    }

    public static boolean gecerliMi(Object satir) {
        if (!(satir instanceof String s)) {
            return false;
        }
        try {
            coz(s);
            return true;
        } catch (IllegalArgumentException hata) {
            return false;
        }
    }
}
