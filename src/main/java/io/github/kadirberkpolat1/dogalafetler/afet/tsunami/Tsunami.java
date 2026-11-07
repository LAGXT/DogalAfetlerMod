package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Tsunaminin mod kurucusundaki tek kayıt satırı: config bölümü, sesler (+ {@code sounds.json} ve alt yazı çevirileri), afet
 * fabrikası ve #2 Dalga Avcısı ipucu yayıncısı.
 */
public final class Tsunami {
    private Tsunami() {
    }

    public static void kaydet(IEventBus modBus) {
        TsunamiAyarlari.kaydet();
        TsunamiSesleri.kaydet(modBus);
        DataGenerators.sesEkle(TsunamiSesleri::tanimla);
        DataGenerators.ceviriEkle(TsunamiSesleri::cevirileriYaz);
        AfetKaydi.kaydet(AfetTuru.TSUNAMI, TsunamiAfeti::new);
        BasarimIpucuEvent.yayinciKaydet(AfetTuru.TSUNAMI, Ipucu.SURUKLENDI);
    }
}
