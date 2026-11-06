package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru;

import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;

/** Asit yağmurunun tek kayıt noktası; {@code DogalAfetler} kurucusundan koşulsuz çağrılır (K-C33). */
public final class AsitSistemi {
    private AsitSistemi() {
    }

    public static void kaydet(IEventBus modBus) {
        AsitAyarlari.kaydet();
        AsitKayitlari.kaydet(modBus);
        AfetKaydi.kaydet(AfetTuru.ASIT_YAGMURU, AsitYagmuru::new);
        BasarimIpucuEvent.yayinciKaydet(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI);
        DataGenerators.ceviriEkle(AsitSistemi::cevirileriYaz);
        DataGenerators.saglayiciEkle(AsitVerileri::ekle);
    }

    static void cevirileriYaz(CeviriYazici y) {
        y.ekle("death.attack.dogalafetler.asit", "%1$s was dissolved by acid rain", "%1$s asit yağmurunda eridi");
    }
}
