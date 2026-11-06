package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import org.junit.jupiter.api.Test;

class GorunurlukTest {
    @Test
    void radarOnUyaridaYalnizNormalPencereBasladiysaGorur() {
        assertEquals(RadarGorunumu.GIZLI, Gorunurluk.radar(AfetAsamasi.ON_UYARI, false, false, true, 10, 300));
        assertEquals(RadarGorunumu.AFET, Gorunurluk.radar(AfetAsamasi.ON_UYARI, true, false, true, 10, 300));
        assertEquals(RadarGorunumu.AFET, Gorunurluk.radar(AfetAsamasi.SIREN, true, false, true, 10, 300));
        assertEquals(RadarGorunumu.AFET, Gorunurluk.radar(AfetAsamasi.AKTIF, true, false, true, 10, 300));
        assertEquals(RadarGorunumu.GIZLI, Gorunurluk.radar(AfetAsamasi.BITIS, true, false, true, 10, 300));
    }

    @Test
    void menzilVeBoyutSuzgeci() {
        assertEquals(RadarGorunumu.AFET, Gorunurluk.radar(AfetAsamasi.SIREN, true, false, true, 300, 300));
        assertEquals(RadarGorunumu.GIZLI, Gorunurluk.radar(AfetAsamasi.SIREN, true, false, true, 300.5, 300));
        assertEquals(RadarGorunumu.AFET, Gorunurluk.radar(AfetAsamasi.SIREN, true, false, true, 700, 800));
        assertEquals(RadarGorunumu.GIZLI, Gorunurluk.radar(AfetAsamasi.SIREN, true, false, false, 10, 800));
    }

    @Test
    void globalAfetOnUyaridaYokSirendenSonraAnomali() {
        assertEquals(RadarGorunumu.GIZLI, Gorunurluk.radar(AfetAsamasi.ON_UYARI, true, true, true, 0, 300));
        assertEquals(RadarGorunumu.ANOMALI, Gorunurluk.radar(AfetAsamasi.SIREN, true, true, false, 1e9, 300));
        assertEquals(RadarGorunumu.ANOMALI, Gorunurluk.radar(AfetAsamasi.AKTIF, true, true, true, 0, 300));
    }

    @Test
    void sismografYalnizDepremVeSinkhole() {
        for (AfetTuru tur : AfetTuru.values()) {
            boolean beklenen = tur == AfetTuru.DEPREM || tur == AfetTuru.SINKHOLE;
            assertEquals(beklenen, Gorunurluk.sismograf(tur, AfetAsamasi.SIREN, 0, 600, false, 1, true, 0, 300), tur.kimlik());
        }
    }

    @Test
    void hassasSismografErkenBaslar() {
        assertFalse(Gorunurluk.sismograf(AfetTuru.DEPREM, AfetAsamasi.ON_UYARI, 0, 600, false, 1, true, 0, 300));
        assertFalse(Gorunurluk.sismograf(AfetTuru.DEPREM, AfetAsamasi.ON_UYARI, 599, 600, false, 1, true, 0, 300));
        assertTrue(Gorunurluk.sismograf(AfetTuru.DEPREM, AfetAsamasi.ON_UYARI, 600, 600, false, 1, true, 0, 300));
        assertTrue(Gorunurluk.sismograf(AfetTuru.DEPREM, AfetAsamasi.ON_UYARI, 0, 600, true, 1, true, 0, 300));
        // Oran 0,5: erken bölümün ikinci yarısından.
        assertFalse(Gorunurluk.sismograf(AfetTuru.SINKHOLE, AfetAsamasi.ON_UYARI, 299, 600, true, 0.5, true, 0, 300));
        assertTrue(Gorunurluk.sismograf(AfetTuru.SINKHOLE, AfetAsamasi.ON_UYARI, 300, 600, true, 0.5, true, 0, 300));
        assertFalse(Gorunurluk.sismograf(AfetTuru.DEPREM, AfetAsamasi.BITIS, 0, 600, true, 1, true, 0, 300));
        assertFalse(Gorunurluk.sismograf(AfetTuru.DEPREM, AfetAsamasi.SIREN, 0, 600, true, 1, true, 801, 800));
    }
}
