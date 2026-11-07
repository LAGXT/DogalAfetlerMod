package io.github.kadirberkpolat1.dogalafetler.basarim;

import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Başarım alt sisteminin mod kurucusundan çağrılan tek kayıt noktası (K-C33: koşulsuz, açık {@code kaydet}). */
public final class Basarim {
    private Basarim() {
    }

    public static void kaydet(IEventBus modBus) {
        BasarimAyarlari.kaydet();
        modBus.addListener(Basarim::ortakKurulum);
        BasarimKomutlari.kaydet();
        DataGenerators.ceviriEkle(BasarimCevirileri::yaz);
        DataGenerators.saglayiciEkle(BasarimVerileri::ekle);
    }

    private static void ortakKurulum(FMLCommonSetupEvent olay) {
        olay.enqueueWork(BasarimOlcutleri::kaydet);
    }
}
