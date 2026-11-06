package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;

/** Kasırganın mod kurucusundan çağrılan tek kayıt noktası (K-C33). */
public final class KasirgaSistemi {
    private KasirgaSistemi() {
    }

    public static void kaydet(IEventBus modBus) {
        KasirgaAyarlari.kaydet();
        KasirgaKayitlari.kaydet(modBus);
        if (AfetKaydi.kayitliMi(AfetTuru.KASIRGA)) {
            // -PoyunTestiAfeti=kasirga geliştirme özelliği açıkça istendi (başarım/radar otomasyonları): o öncelikli.
            DogalAfetler.LOGGER.warn("Kasırga oyun testi afeti olarak kayıtlı; gerçek kasırga kaydı atlandı");
        } else {
            AfetKaydi.kaydet(AfetTuru.KASIRGA, Kasirga::new);
        }
        KomutKaydi.ekle(KasirgaKomutlari.komut());
        DataGenerators.ceviriEkle(KasirgaCevirileri::yaz);
        DataGenerators.sesEkle(KasirgaKayitlari::sesleriTanimla);
        DataGenerators.saglayiciEkle(KasirgaVerileri::ekle);
    }
}
