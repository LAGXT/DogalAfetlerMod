package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Objects;

/**
 * Bir afetin olasılığını geçici olarak değiştiren çarpan (madde 4, bölüm H): {@code sabitSure} boyunca {@code tepe}, ardından
 * {@code sonumSuresi} boyunca 1'e döner. Zamanlar ölçekli afet saatidir. Çarpan yalnız oranı değiştirir; bekleme ve tek afet
 * kuralı zamanlayıcıda ayrıca uygulanır.
 */
public record GeciciCarpan(AfetTuru tur, double tepe, long baslangic, long sabitSure, long sonumSuresi, SonumTipi tip) {
    /** Tek bir çarpanın üst sınırı (yüz kat). */
    public static final double EN_COK = 100.0;
    private static final double USTEL_K = 5.0;

    public GeciciCarpan {
        Objects.requireNonNull(tur, "tur");
        Objects.requireNonNull(tip, "tip");
        if (!(tepe > 0) || tepe > EN_COK) {
            throw new IllegalArgumentException("Çarpan (0, " + EN_COK + "] aralığında olmalı: " + tepe);
        }
        if (sabitSure < 0 || sonumSuresi < 0) {
            throw new IllegalArgumentException("Süreler negatif olamaz: " + sabitSure + ", " + sonumSuresi);
        }
    }

    public double deger(long saat) {
        long gecen = saat - baslangic;
        if (gecen < 0) {
            return 1.0;
        }
        if (gecen <= sabitSure) {
            return tepe;
        }
        long sonum = gecen - sabitSure;
        if (sonum >= sonumSuresi) {
            return 1.0;
        }
        double f = (double) sonum / sonumSuresi;
        return switch (tip) {
            case DOGRUSAL -> tepe + (1.0 - tepe) * f;
            case USTEL -> 1.0 + (tepe - 1.0) * (Math.exp(-USTEL_K * f) - Math.exp(-USTEL_K)) / (1.0 - Math.exp(-USTEL_K));
        };
    }

    public boolean bitti(long saat) {
        return saat - baslangic >= sabitSure + sonumSuresi;
    }
}
