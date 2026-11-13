package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OyunTestiAfetSecimiTest {
    @Test
    void gercekAfetiKayitliTurAtlanir() {
        Set<AfetTuru> kayitli = EnumSet.of(AfetTuru.DEPREM);
        OyunTestiAfetSecimi.Secim s = OyunTestiAfetSecimi.sec("deprem,sinkhole", kayitli::contains);
        assertEquals(List.of(AfetTuru.SINKHOLE), s.kaydedilecek());
        assertEquals(List.of(AfetTuru.DEPREM), s.atlanan(), "gerçek kayıt önce; test afeti kayıtlı türü atlar (K-E22)");
        assertEquals(List.of(), s.ezilecek(), "! olmadan gerçek kayda dokunulmaz");
    }

    @Test
    void bilinmeyenAdlarVeTekrarlarSayilmaz() {
        OyunTestiAfetSecimi.Secim s = OyunTestiAfetSecimi.sec(" Deprem , yok, deprem,,gunes_patlamasi", t -> false);
        assertEquals(List.of(AfetTuru.DEPREM, AfetTuru.GUNES_PATLAMASI), s.kaydedilecek());
        assertEquals(List.of(), s.atlanan());
    }

    @Test
    void unlemliTurGercekKaydinYerineGecer() {
        Set<AfetTuru> kayitli = EnumSet.of(AfetTuru.DEPREM, AfetTuru.GUNES_PATLAMASI);
        OyunTestiAfetSecimi.Secim s = OyunTestiAfetSecimi.sec("!deprem, ! gunes_patlamasi,!sinkhole,kasirga", kayitli::contains);
        assertEquals(List.of(AfetTuru.DEPREM, AfetTuru.GUNES_PATLAMASI), s.ezilecek(), "kayıtlı + ! → ezilir (K-E23)");
        assertEquals(List.of(AfetTuru.SINKHOLE, AfetTuru.KASIRGA), s.kaydedilecek(), "kayıtsızsa ! fark etmez");
        assertEquals(List.of(), s.atlanan());
        OyunTestiAfetSecimi.Secim iki = OyunTestiAfetSecimi.sec("deprem,!deprem", kayitli::contains);
        assertEquals(List.of(AfetTuru.DEPREM), iki.ezilecek(), "aynı tür ! ile bir kez de istenirse ezilir");
        assertEquals(List.of(), iki.atlanan());
    }

    @Test
    void ozellikYoksaHicbirSeyYok() {
        for (String deger : new String[] {null, "", "   "}) {
            OyunTestiAfetSecimi.Secim s = OyunTestiAfetSecimi.sec(deger, t -> false);
            assertEquals(List.of(), s.kaydedilecek());
            assertEquals(List.of(), s.atlanan());
            assertEquals(List.of(), s.ezilecek());
        }
    }
}
