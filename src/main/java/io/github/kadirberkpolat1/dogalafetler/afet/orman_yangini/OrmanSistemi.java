package io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini;

import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;

/** Orman yangını afetinin tek kayıt noktası; {@code DogalAfetler} kurucusundan koşulsuz çağrılır (K-C33). */
public final class OrmanSistemi {
    private OrmanSistemi() {
    }

    public static void kaydet() {
        OrmanAyarlari.kaydet();
        AfetKaydi.kaydet(AfetTuru.ORMAN_YANGINI, OrmanYangini::new);
        BasarimIpucuEvent.yayinciKaydet(AfetTuru.ORMAN_YANGINI, Ipucu.YANGIN_ALANINDA);
    }
}
