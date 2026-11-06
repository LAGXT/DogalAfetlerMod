package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi;

import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import net.minecraftforge.eventbus.api.IEventBus;

/** Güneş patlamasının mod kurucusundaki kaydı (çekirdek §2, K-C33). */
public final class GunesPatlamasiSistemi {
    private GunesPatlamasiSistemi() {
    }

    /**
     * Config bölümü, sesler ve afet kaydı. {@code OyunTestiAfeti.ozelliktenKaydet()}'ten önce çağrılır (K-E22): çekirdeğin
     * yaşam döngüsü taklidi ({@code -PoyunTestiAfeti=...,gunes_patlamasi}) bu türü artık uyarıyla atlar ve onunla birlikte
     * {@code OyunTestiSureci} de kaydedilmez; güneş zinciri her zaman gerçek afetten başlar.
     */
    public static void kaydet(IEventBus modBus) {
        GunesPatlamasiAyarlari.kaydet();
        GunesPatlamasiSesleri.kaydet(modBus);
        AfetKaydi.kaydet(AfetTuru.GUNES_PATLAMASI, GunesPatlamasi::new);
    }
}
