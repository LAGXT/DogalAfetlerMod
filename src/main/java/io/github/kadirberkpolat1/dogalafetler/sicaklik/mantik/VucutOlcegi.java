package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import java.util.Locale;

/**
 * Vücut biriminin (−100…+100, 0 nötr) santigrat karşılığı (S3-01). İç değer ve kayıtlar birimde kalır; oyuncuya görünen her
 * vücut değeri (HUD, menü, komutlar) yalnız bu sınıftan geçer: −100 → 32 °C, 0 → 37 °C, +100 → 42 °C.
 */
public final class VucutOlcegi {
    public static final double NORMAL_C = 37.0;
    public static final double DERECE_PER_BIRIM = 0.05;
    public static final double EN_DUSUK_C = santigrat(-IsiAlisverisi.OLCEK);
    public static final double EN_YUKSEK_C = santigrat(IsiAlisverisi.OLCEK);

    private VucutOlcegi() {
    }

    public static double santigrat(double birim) {
        return NORMAL_C + DERECE_PER_BIRIM * birim;
    }

    public static double birim(double santigrat) {
        return (santigrat - NORMAL_C) / DERECE_PER_BIRIM;
    }

    /** Bir ondalıklı metin ({@code "36.4"}). */
    public static String metin(double santigrat) {
        return String.format(Locale.ROOT, "%.1f", santigrat);
    }
}
