package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;

/** Kasırganın mod kurucusundan çağrılan tek kayıt noktası (K-C33); {@code OyunTestiAfeti.ozelliktenKaydet}'ten önce (K-E22). */
public final class KasirgaSistemi {
    private KasirgaSistemi() {
    }

    public static void kaydet(IEventBus modBus) {
        KasirgaAyarlari.kaydet();
        KasirgaKayitlari.kaydet(modBus);
        AfetKaydi.kaydet(AfetTuru.KASIRGA, Kasirga::new);
        KomutKaydi.ekle(KasirgaKomutlari.komut());
        DataGenerators.ceviriEkle(KasirgaCevirileri::yaz);
        DataGenerators.sesEkle(KasirgaKayitlari::sesleriTanimla);
        DataGenerators.saglayiciEkle(KasirgaVerileri::ekle);
    }
}
