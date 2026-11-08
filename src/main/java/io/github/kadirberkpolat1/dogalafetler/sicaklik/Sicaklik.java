package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.uyum.ColdSweatKoprusu;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;

/** Sıcaklık alt sisteminin mod kurucusundan çağrılan tek kayıt noktası (K-C33: koşulsuz, açık {@code kaydet}). */
public final class Sicaklik {
    private Sicaklik() {
    }

    public static void kaydet(IEventBus modBus) {
        SicaklikAyarlari.kaydet();
        SicaklikIstemciAyarlari.kaydet();
        modBus.addListener(SicaklikAyarlari::configDegisti);
        SicaklikKayitlari.kaydet(modBus);
        SicaklikParcaciklari.kaydet(modBus);
        SicaklikDurumPaketi.kaydet();
        SicaklikDokumPaketi.kaydet();
        SicaklikDokumIstegi.kaydet();
        SicaklikKomutlari.kaydet();
        DataGenerators.ceviriEkle(SicaklikCevirileri::yaz);
        DataGenerators.ceviriEkle(SicaklikMenuCevirileri::yaz);
        DataGenerators.saglayiciEkle(SicaklikVerileri::ekle);
        if (SicaklikKapisi.coldSweatYuklu()) {
            ColdSweatKoprusu.kur();
        }
    }
}
