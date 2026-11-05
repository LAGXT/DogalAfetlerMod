package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;

/** Çekirdeğin dil dosyası girdileri (runData ile {@code en_us} ve {@code tr_tr} üretilir). */
public final class CekirdekCevirileri {
    public static final String MOD_ADI = "mod.dogalafetler.ad";

    private CekirdekCevirileri() {
    }

    public static void yaz(CeviriYazici yazici) {
        yazici.ekle(MOD_ADI, "Natural Disasters", "Doğal Afetler");
        for (AfetTuru tur : AfetTuru.values()) {
            yazici.ekle(tur.ceviriAnahtari(), tur.ingilizceAd(), tur.turkceAd());
        }
    }
}
