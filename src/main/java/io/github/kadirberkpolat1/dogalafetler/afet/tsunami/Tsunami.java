package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Tsunaminin mod kurucusundaki tek kayıt satırı: config bölümleri (SERVER, CLIENT), sesler (+ {@code sounds.json} ve alt yazı
 * çevirileri), afet fabrikası, #2 Dalga Avcısı ipucu yayıncısı ve Sifon başarımı dinleyicisi.
 */
public final class Tsunami {
    private Tsunami() {
    }

    public static void kaydet(IEventBus modBus) {
        TsunamiAyarlari.kaydet();
        TsunamiIstemciAyarlari.kaydet();
        TsunamiSesleri.kaydet(modBus);
        DataGenerators.sesEkle(TsunamiSesleri::tanimla);
        DataGenerators.ceviriEkle(TsunamiSesleri::cevirileriYaz);
        AfetKaydi.kaydet(AfetTuru.TSUNAMI, TsunamiAfeti::new);
        BasarimIpucuEvent.yayinciKaydet(AfetTuru.TSUNAMI, Ipucu.SURUKLENDI);
        MinecraftForge.EVENT_BUS.addListener(TsunamiSifon::blokYerlestirildi);
    }
}
