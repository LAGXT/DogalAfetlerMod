package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Etkin geçici çarpanlar. Aynı türdeki çarpanlar çarpılır (üst sınır {@value #TOPLAM_EN_COK}). */
public final class CarpanListesi {
    public static final double TOPLAM_EN_COK = 1000.0;

    private final List<GeciciCarpan> carpanlar = new ArrayList<>();

    public void ekle(GeciciCarpan carpan) {
        carpanlar.add(Objects.requireNonNull(carpan, "carpan"));
    }

    public double carpan(AfetTuru tur, long saat) {
        double sonuc = 1.0;
        for (GeciciCarpan carpan : carpanlar) {
            if (carpan.tur() == tur) {
                sonuc *= carpan.deger(saat);
            }
        }
        return Math.min(TOPLAM_EN_COK, sonuc);
    }

    /** Süresi dolanları çıkarır; bir şey çıktıysa {@code true}. */
    public boolean temizle(long saat) {
        return carpanlar.removeIf(carpan -> carpan.bitti(saat));
    }

    public List<GeciciCarpan> liste() {
        return List.copyOf(carpanlar);
    }

    public boolean bosMu() {
        return carpanlar.isEmpty();
    }

    public void sifirla() {
        carpanlar.clear();
    }

    /** Kayıttan yükler. */
    public void yukle(List<GeciciCarpan> liste) {
        carpanlar.clear();
        carpanlar.addAll(liste);
    }
}
