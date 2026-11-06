package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;

/** Tsunaminin mod kurucusundaki tek kayıt satırı: config bölümü, afet fabrikası ve #2 Dalga Avcısı ipucu yayıncısı. */
public final class Tsunami {
    private Tsunami() {
    }

    public static void kaydet() {
        TsunamiAyarlari.kaydet();
        AfetKaydi.kaydet(AfetTuru.TSUNAMI, TsunamiAfeti::new);
        BasarimIpucuEvent.yayinciKaydet(AfetTuru.TSUNAMI, Ipucu.SURUKLENDI);
    }
}
