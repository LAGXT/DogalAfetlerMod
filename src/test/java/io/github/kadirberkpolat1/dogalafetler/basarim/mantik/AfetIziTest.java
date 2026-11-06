package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AfetIziTest {
    private static final Set<Ipucu> OLAYSIZ = EnumSet.noneOf(Ipucu.class);
    private static final Set<Ipucu> HEPSI_OLAYLI = EnumSet.allOf(Ipucu.class);

    @Test
    void yeniAfetteSifirlanir() {
        AfetIzi iz = new AfetIzi();
        assertTrue(iz.afeteBagla(42));
        iz.isaretle(Ipucu.SURUKLENDI, true, 0);
        iz.isaretle(Ipucu.HASAR_ALDI, false, 0);
        iz.isaretle(Ipucu.YANGIN_ALANINDA, false, 7);
        assertFalse(iz.afeteBagla(42), "aynı afet: iz korunur");
        assertEquals(Ipucu.SURUKLENDI.bit(), iz.olayBitleri);
        assertTrue(iz.afeteBagla(43));
        assertEquals(0, iz.olayBitleri);
        assertEquals(0, iz.tahminBitleri);
        assertEquals(0, iz.yanginTahmin);
        assertEquals(AfetIzi.SANIYE_YOK, iz.yanginTahminSon);
        assertEquals(43, iz.no());
    }

    @Test
    void olayGelmeyenIpucundaYedekTahminSayilir() {
        AfetIzi iz = new AfetIzi();
        iz.afeteBagla(1);
        assertEquals(EnumSet.of(SagCikmaKosulu.HASARSIZ), iz.kosullar(OLAYSIZ), "hiçbir ipucu yok: yalnız hasarsız");
        iz.isaretle(Ipucu.SURUKLENDI, false, 0);
        iz.isaretle(Ipucu.CUKURA_DUSTU, false, 0);
        iz.isaretle(Ipucu.HASAR_ALDI, false, 0);
        for (int s = 0; s < AfetIzi.YANGIN_SANIYESI; s++) {
            iz.isaretle(Ipucu.YANGIN_ALANINDA, false, s);
        }
        assertEquals(EnumSet.of(SagCikmaKosulu.SURUKLENDI, SagCikmaKosulu.CUKURA_DUSTU, SagCikmaKosulu.YANGIN_ALANINDA),
                iz.kosullar(OLAYSIZ));
    }

    @Test
    void olayGelenIpucundaTekKaynakOlaydir() {
        AfetIzi iz = new AfetIzi();
        iz.afeteBagla(1);
        // Yedek: ağaçtan atladı (yanlış pozitif), zombi vurdu (yanlış negatif), yangına yakın 30 sn.
        iz.isaretle(Ipucu.CUKURA_DUSTU, false, 0);
        iz.isaretle(Ipucu.HASAR_ALDI, false, 0);
        for (int s = 0; s < AfetIzi.YANGIN_SANIYESI; s++) {
            iz.isaretle(Ipucu.YANGIN_ALANINDA, false, s);
        }
        assertEquals(EnumSet.of(SagCikmaKosulu.HASARSIZ), iz.kosullar(HEPSI_OLAYLI),
                "afet olayı kaynakken yedek tahmin sayılmaz; asit olayı yoksa zombi hasarı Çatı Altı'nı bozmaz");
        iz.isaretle(Ipucu.CUKURA_DUSTU, true, 0);
        assertTrue(iz.kosullar(HEPSI_OLAYLI).contains(SagCikmaKosulu.CUKURA_DUSTU));
        iz.isaretle(Ipucu.HASAR_ALDI, true, 0);
        assertFalse(iz.kosullar(HEPSI_OLAYLI).contains(SagCikmaKosulu.HASARSIZ), "asit olayıyla gelen hasar bozar");
    }

    @Test
    void yanginSaniyeleriKaynakBasinaSaniyedeBirKezVeToplanmaz() {
        AfetIzi iz = new AfetIzi();
        iz.afeteBagla(1);
        assertTrue(iz.isaretle(Ipucu.YANGIN_ALANINDA, true, 10));
        assertFalse(iz.isaretle(Ipucu.YANGIN_ALANINDA, true, 10), "aynı saniye ikinci kez sayılmaz");
        assertTrue(iz.isaretle(Ipucu.YANGIN_ALANINDA, false, 10), "yedek kendi sayacında");
        assertEquals(1, iz.yanginOlay);
        assertEquals(1, iz.yanginTahmin);
        for (int s = 11; s < 11 + AfetIzi.YANGIN_SANIYESI - 2; s++) {
            iz.isaretle(Ipucu.YANGIN_ALANINDA, true, s);
            iz.isaretle(Ipucu.YANGIN_ALANINDA, false, s);
        }
        Set<Ipucu> yanginOlayli = EnumSet.of(Ipucu.YANGIN_ALANINDA);
        assertFalse(iz.kosullar(yanginOlayli).contains(SagCikmaKosulu.YANGIN_ALANINDA),
                "29 olay saniyesi + 29 tahmin saniyesi toplanmaz (çift sayım yok)");
        iz.isaretle(Ipucu.YANGIN_ALANINDA, true, 500);
        assertTrue(iz.kosullar(yanginOlayli).contains(SagCikmaKosulu.YANGIN_ALANINDA), "30. olay saniyesi");
    }

    @Test
    void kosulVeIpucuKimlikleri() {
        for (SagCikmaKosulu k : SagCikmaKosulu.values()) {
            assertEquals(k, SagCikmaKosulu.kimlikIle(k.kimlik().toUpperCase(Locale.ROOT)).orElseThrow());
        }
        assertTrue(SagCikmaKosulu.kimlikIle("yok").isEmpty());
        int hepsi = 0;
        for (Ipucu ipucu : Ipucu.values()) {
            assertEquals(0, hepsi & ipucu.bit(), "bitler çakışmaz");
            hepsi |= ipucu.bit();
        }
        assertEquals(EnumSet.allOf(Ipucu.class), Ipucu.bitlerden(hepsi));
        assertEquals(EnumSet.of(Ipucu.CUKURA_DUSTU), Ipucu.bitlerden(Ipucu.CUKURA_DUSTU.bit()));
    }
}
