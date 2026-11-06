package io.github.kadirberkpolat1.dogalafetler.afet.sinkhole;

import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;

/**
 * Sinkhole'un mod kurucusundan çağrılan tek kayıt noktası (K-C33): config, afet fabrikası ve "Delikten Dönüş" ipucunun
 * yayıncı kaydı (başarım §A.3 #4, A-31). Sesleri {@code DepremSistemi} kaydeder.
 */
public final class SinkholeSistemi {
    private SinkholeSistemi() {
    }

    public static void kaydet() {
        SinkholeAyarlari.kaydet();
        AfetKaydi.kaydet(AfetTuru.SINKHOLE, Sinkhole::new);
        BasarimIpucuEvent.yayinciKaydet(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU);
    }
}
