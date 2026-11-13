package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.IpucuKaynaklari.OlayKarari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.EnumSet;
import org.junit.jupiter.api.Test;

class IpucuKaynaklariTest {
    @Test
    void kayitliYayincidaOlayKabulVeYedekKapali() {
        IpucuKaynaklari k = new IpucuKaynaklari();
        assertEquals(EnumSet.noneOf(Ipucu.class), k.olayli(AfetTuru.SINKHOLE), "kayıt yok: hepsi yedekte");
        assertTrue(k.kaydet(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI));
        assertFalse(k.kaydet(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI), "ikinci kayıt yeni değil");
        assertTrue(k.yayinliyor(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI));
        assertFalse(k.yayinliyor(AfetTuru.KASIRGA, Ipucu.HASAR_ALDI), "tür başına");
        assertEquals(EnumSet.of(Ipucu.HASAR_ALDI), k.olayli(AfetTuru.ASIT_YAGMURU),
                "kayıtlı yayıncıda olay hiç gelmese de yedek kapalı");
        for (int i = 0; i < 3; i++) {
            assertEquals(OlayKarari.KABUL, k.olayGeldi(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI), "kayıtlıdan her olay");
        }
        assertTrue(k.kaldir(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI), "kayıtlıydı");
        assertFalse(k.yayinliyor(AfetTuru.ASIT_YAGMURU, Ipucu.HASAR_ALDI));
        assertFalse(k.kaldir(AfetTuru.TSUNAMI, Ipucu.SURUKLENDI), "kayıtsızdı");
        assertFalse(k.yayinliyor(AfetTuru.TSUNAMI, Ipucu.SURUKLENDI), "kayıtsızı kaldırmak zararsız");
    }

    @Test
    void kaldirVeGeriKoyOncekiKaydiKorur() {
        IpucuKaynaklari k = new IpucuKaynaklari();
        k.kaydet(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU);
        k.kaydet(AfetTuru.SINKHOLE, Ipucu.HASAR_ALDI);
        boolean vardi = k.kaldir(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU);
        assertTrue(vardi);
        assertEquals(EnumSet.of(Ipucu.HASAR_ALDI), k.olayli(AfetTuru.SINKHOLE), "yalnız o ipucu kalkar");
        assertEquals(OlayKarari.UYAR_YOK_SAY, k.olayGeldi(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU), "kaldırılınca kayıtsız");
        if (vardi) {
            k.kaydet(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU);
        }
        assertEquals(EnumSet.of(Ipucu.CUKURA_DUSTU, Ipucu.HASAR_ALDI), k.olayli(AfetTuru.SINKHOLE), "geri kondu");
        assertFalse(k.kaldir(AfetTuru.ORMAN_YANGINI, Ipucu.YANGIN_ALANINDA), "kayıtsızı kaldırmak false");
        assertEquals(EnumSet.noneOf(Ipucu.class), k.olayli(AfetTuru.ORMAN_YANGINI), "kayıtsız geri konmaz");
    }

    @Test
    void kayitsizKaynaktanOlayYokSayilirIlkindeUyarilir() {
        IpucuKaynaklari k = new IpucuKaynaklari();
        assertEquals(OlayKarari.UYAR_YOK_SAY, k.olayGeldi(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU), "ilk olay uyarır");
        assertEquals(OlayKarari.YOK_SAY, k.olayGeldi(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU), "tekrar uyarmaz");
        assertEquals(OlayKarari.UYAR_YOK_SAY, k.olayGeldi(AfetTuru.SINKHOLE, Ipucu.HASAR_ALDI), "ipucu başına");
        assertEquals(OlayKarari.UYAR_YOK_SAY, k.olayGeldi(AfetTuru.TSUNAMI, Ipucu.CUKURA_DUSTU), "tür başına");
        assertEquals(EnumSet.noneOf(Ipucu.class), k.olayli(AfetTuru.SINKHOLE),
                "yok sayılan olay kaynağı değiştirmez: yedek açık");
        k.kaydet(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU);
        assertEquals(OlayKarari.KABUL, k.olayGeldi(AfetTuru.SINKHOLE, Ipucu.CUKURA_DUSTU), "kayıttan sonra kabul");
    }
}
