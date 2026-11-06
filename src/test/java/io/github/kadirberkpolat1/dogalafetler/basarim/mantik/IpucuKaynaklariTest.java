package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class IpucuKaynaklariTest {
    @Test
    void olayliKayitVeGelenOlaylarinBirlesimi() {
        IpucuKaynaklari k = new IpucuKaynaklari();
        assertEquals(EnumSet.noneOf(Ipucu.class), k.olayli(AfetTuru.SINKHOLE, 0), "kayıt ve olay yok: hepsi yedekte");
        assertEquals(EnumSet.of(Ipucu.CUKURA_DUSTU), k.olayli(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU.bit()),
                "bu afette olayı gelen ipucu");
        assertTrue(k.kaydet(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI));
        assertFalse(k.kaydet(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI), "ikinci kayıt yeni değil");
        assertTrue(k.yayinliyor(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI));
        assertFalse(k.yayinliyor(AfetTuru.KASIRGA, Ipucu.HASAR_ALDI), "tür başına");
        assertEquals(EnumSet.of(Ipucu.HASAR_ALDI), k.olayli(AfetTuru.ASIT_YAGMURU, 0),
                "kayıtlı yayıncıda olay hiç gelmese de yedek kapalı");
        assertEquals(EnumSet.of(Ipucu.HASAR_ALDI, Ipucu.SURUKLENDI),
                k.olayli(AfetTuru.ASIT_YAGMURU, Ipucu.SURUKLENDI.bit()));
        k.kaldir(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI);
        assertFalse(k.yayinliyor(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI));
        k.kaldir(AfetTuru.TSUNAMI, Ipucu.SURUKLENDI);
        assertFalse(k.yayinliyor(AfetTuru.TSUNAMI, Ipucu.SURUKLENDI), "kayıtsızı kaldırmak zararsız");
    }
}
