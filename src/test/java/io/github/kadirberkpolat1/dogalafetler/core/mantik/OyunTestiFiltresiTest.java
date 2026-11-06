package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OyunTestiFiltresiTest {
    private static OyunTestiFiltresi filtre(String desen) {
        return OyunTestiFiltresi.coz(desen).orElseThrow();
    }

    @Test
    void sinifBatchYaDaTestAdindanBirineUyanKalir() {
        assertTrue(filtre("Meteor").uyar("MeteorOyunTestleri", "defaultBatch", "kraterAcilir"), "sınıf adı");
        assertTrue(filtre("donma_alan").uyar("DonmaOyunTestleri", "donma_alan_2", "golDonar"), "batch adı");
        assertTrue(filtre("hicBir").uyar("CekirdekOyunTestleri", "cekirdek_yerlesim", "hicBirBatchSatirKirmaz"),
                "test adı");
        assertTrue(filtre("^su_(deniz|dalga)$").uyar("SuOyunTestleri", "su_dalga", "dalgaIlerler"), "çapalı regex");
    }

    @Test
    void hicBirAdaUymayanDuser() {
        OyunTestiFiltresi f = filtre("Meteor");
        assertFalse(f.uyar("YanginOyunTestleri", "yangin_genel", "atesYayilir"));
        assertFalse(f.uyar("RadarOyunTestleri", "meteor_dusur", "meteorDuser"), "eşleme büyük/küçük harfe duyarlı");
        assertFalse(filtre("^Meteor$").uyar("MeteorOyunTestleri", "meteor", "meteor"), "find çapayı korur");
    }

    @Test
    void ozellikYoksaYaDaBossaFiltreYok() {
        assertTrue(OyunTestiFiltresi.coz(null).isEmpty());
        assertTrue(OyunTestiFiltresi.coz("").isEmpty());
        assertTrue(OyunTestiFiltresi.coz("  ").isEmpty());
        assertEquals("Meteor|Yangin", filtre("Meteor|Yangin").desen());
    }

    @Test
    void gecersizRegexAcikHataVerir() {
        IllegalArgumentException hata = assertThrows(IllegalArgumentException.class, () -> OyunTestiFiltresi.coz("Meteor("));
        assertTrue(hata.getMessage().contains(OyunTestiFiltresi.OZELLIK), hata.getMessage());
        assertTrue(hata.getMessage().contains("'Meteor('"), hata.getMessage());
    }
}
