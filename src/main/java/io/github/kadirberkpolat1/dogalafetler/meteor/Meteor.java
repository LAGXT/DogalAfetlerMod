package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.termostat.Termostat;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;

/** Meteor, meteor eşyaları ve termostatın mod kurucusundan çağrılan tek kayıt noktası (K-C33). */
public final class Meteor {
    private Meteor() {
    }

    public static void kaydet(IEventBus modBus) {
        MeteorAyarlari.kaydet();
        MeteorKayitlari.kaydet(modBus);
        MeteorSesleri.kaydet(modBus);
        MeteorEsyaKayitlari.kaydet(modBus);
        Termostat.kaydet();
        MeteorZirhi.kaydet();
        AgKanali.kaydet(k -> k.sunucudanIstemciye("meteor/sarsinti", MeteorSarsintiPaketi.class, MeteorSarsintiPaketi::yaz,
                MeteorSarsintiPaketi::oku, MeteorSarsintiPaketi::isle));
        KomutKaydi.ekle(MeteorKomutlari.komut());
        KomutKaydi.ekle(MeteorKomutlari.odulKomutu());
        DataGenerators.ceviriEkle(MeteorCevirileri::yaz);
        DataGenerators.sesEkle(MeteorSesleri::tanimla);
        DataGenerators.ceviriEkle(MeteorSesleri::cevirileriYaz);
        DataGenerators.saglayiciEkle(MeteorVerileri::ekle);
    }
}
