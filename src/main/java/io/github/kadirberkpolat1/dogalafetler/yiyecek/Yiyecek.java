package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;

/** Sıcaklık yiyeceklerinin mod kurucusundan çağrılan tek kayıt noktası (K-C33: koşulsuz, açık {@code kaydet}). */
public final class Yiyecek {
    private Yiyecek() {
    }

    public static void kaydet(IEventBus modBus) {
        YiyecekAyarlari.kaydet();
        modBus.addListener(YiyecekAyarlari::configDegisti);
        YiyecekKayitlari.kaydet(modBus);
        YiyecekKomutlari.kaydet();
        DataGenerators.ceviriEkle(YiyecekCevirileri::yaz);
        DataGenerators.saglayiciEkle(YiyecekVerileri::ekle);
    }
}
