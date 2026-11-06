package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;

/** Güneş patlamasının mod kurucusundaki kaydı (çekirdek §2, K-C33). */
public final class GunesPatlamasiSistemi {
    private GunesPatlamasiSistemi() {
    }

    /**
     * {@code OyunTestiAfeti.ozelliktenKaydet()}'ten sonra çağrılmalı: çekirdeğin yaşam döngüsü taklidi
     * ({@code -PoyunTestiAfeti=...,gunes_patlamasi}, yalnız senaryo) türü almışsa gerçek afet kaydedilmez (iki kayıt hatadır;
     * gökyüzü G15 ile aynı yol). Config bölümü her durumda kaydedilir.
     */
    public static void kaydet() {
        GunesPatlamasiAyarlari.kaydet();
        if (AfetKaydi.kayitliMi(AfetTuru.GUNES_PATLAMASI)) {
            DogalAfetler.LOGGER.warn("Güneş patlaması türü zaten kayıtlı (oyun testi taklidi); gerçek afet kaydedilmedi");
            return;
        }
        AfetKaydi.kaydet(AfetTuru.GUNES_PATLAMASI, GunesPatlamasi::new);
    }
}
