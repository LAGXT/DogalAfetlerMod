package io.github.kadirberkpolat1.dogalafetler.rehber;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Rehber cümlelerinin dil anahtarları (datagen; {@code en_us} ve {@code tr_tr}); metinler {@link RehberMesaji}'dadır. */
public final class RehberCevirileri {
    private RehberCevirileri() {
    }

    public static void yaz(CeviriYazici y) {
        for (RehberMesaji m : RehberMesaji.values()) {
            y.ekle(m.ceviriAnahtari(), m.ingilizce(), m.turkce());
        }
    }
}
