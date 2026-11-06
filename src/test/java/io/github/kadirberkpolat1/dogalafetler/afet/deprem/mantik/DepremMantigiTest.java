package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DepremMantigiTest {
    @Test
    void ayniTohumAyniPlanFarkliTohumFarkliPlan() {
        DepremSeviyesi s = DepremSeviyesi.varsayilan(3);
        assertEquals(FayHatti.olustur(10, -20, 0.7, s, 42L), FayHatti.olustur(10, -20, 0.7, s, 42L));
        assertFalse(FayHatti.olustur(10, -20, 0.7, s, 42L).equals(FayHatti.olustur(10, -20, 0.7, s, 43L)));
    }

    @Test
    void yarikMerkezdenUclaraAcilirUzunlukVeDerinlikSeviyeyleBuyur() {
        int oncekiSutun = 0;
        int oncekiDerinlik = 0;
        for (int sev = 1; sev <= 5; sev++) {
            DepremSeviyesi s = DepremSeviyesi.varsayilan(sev);
            FayHatti.Plan p = FayHatti.olustur(0, 0, 1.1, s, 7L * sev);
            double onceki = -1;
            int enDerin = 0;
            Set<Long> konumlar = new HashSet<>();
            for (FayHatti.Hucre h : p.yarik()) {
                assertTrue(h.yol() >= onceki, "yol sırası");
                onceki = h.yol();
                assertTrue(h.yol() <= s.uzunluk() / 2.0 + 1e-9, "yarım boy aşıldı");
                assertTrue(h.derinlik() >= 1 && h.derinlik() <= s.derinlik() + 1, "derinlik sınırı");
                assertTrue(konumlar.add(FayHatti.anahtar(h.x(), h.z())), "aynı sütun iki kez");
                enDerin = Math.max(enDerin, h.derinlik());
            }
            assertEquals(0.0, p.yarik().get(0).yol(), 1e-9, "ilk açılan merkez");
            assertTrue(p.yarik().size() > oncekiSutun, "S" + sev + " sütun sayısı artmalı");
            assertTrue(enDerin > oncekiDerinlik, "S" + sev + " derinlik artmalı");
            oncekiSutun = p.yarik().size();
            oncekiDerinlik = enDerin;
            assertEquals(sev >= 2, !p.yan().isEmpty(), "yan çatlak S2+");
            assertEquals(sev >= 4, !p.basamak().isEmpty(), "fay basamağı S4+");
        }
    }

    @Test
    void profilMerkezdeTamUclardaSifir() {
        assertEquals(1.0, FayHatti.profil(0.5), 1e-12);
        assertEquals(0.0, FayHatti.profil(0), 1e-12);
        assertEquals(0.0, FayHatti.profil(1), 1e-6);
        assertTrue(FayHatti.profil(0.25) > Math.sin(Math.PI * 0.25), "üs 0,7 kenarı dolgunlaştırır");
    }

    @Test
    void sarsintiZarfiOncuAnaSonum() {
        int sure = DepremSeviyesi.varsayilan(1).sureTick();
        assertEquals(0.2, SarsintiZarfi.deprem(0, sure), 1e-12, "öncü %20");
        assertEquals(1.0, SarsintiZarfi.deprem(SarsintiZarfi.ONCU_TICK + 10, sure), 1e-12);
        assertEquals(0.5, SarsintiZarfi.deprem(sure - SarsintiZarfi.SONUM_TICK / 2.0, sure), 1e-12, "sönümün yarısı");
        assertEquals(0.0, SarsintiZarfi.deprem(sure, sure), 1e-12);
        assertEquals(0.0, SarsintiZarfi.deprem(-1, sure), 1e-12);
        assertEquals(1.0, SarsintiZarfi.darbe(0), 1e-12);
        assertEquals(0.0, SarsintiZarfi.darbe(SarsintiZarfi.DARBE_TICK), 1e-12);
        for (double t = 0; t < 3; t += 0.013) {
            assertTrue(Math.abs(SarsintiZarfi.titreme(t, 0)) <= 1 && Math.abs(SarsintiZarfi.titreme(t, 1)) <= 1);
            assertTrue(Math.abs(SarsintiZarfi.yuvarlanma(t)) <= 1);
        }
    }

    @Test
    void uzaktakiHafifYakindakiTamMenzilDisiSifir() {
        assertEquals(1.0, SarsintiZarfi.mesafeCarpani(0, 100, 164), 1e-12);
        assertEquals(0.5, SarsintiZarfi.mesafeCarpani(50, 100, 164), 1e-12);
        assertEquals(0.15, SarsintiZarfi.mesafeCarpani(120, 100, 164), 1e-12, "alan dışında hafif titreme");
        assertEquals(0.0, SarsintiZarfi.mesafeCarpani(170, 100, 164), 1e-12);
    }

    @Test
    void artciPlaniAralikSureVeZayiflama() {
        int ana = 1200;
        java.util.List<ArtciPlani.Artci> plan = ArtciPlani.olustur(ana, 3, 400, 1200, 0.15, 11L);
        assertEquals(plan, ArtciPlani.olustur(ana, 3, 400, 1200, 0.15, 11L), "aynı tohum aynı plan");
        assertEquals(3, plan.size());
        long onceki = ana;
        double kesir = 0.15;
        for (ArtciPlani.Artci a : plan) {
            long ara = a.baslangic() - onceki;
            assertTrue(ara >= 400 && ara <= 1200, "aralık " + ara);
            assertTrue(a.sure() >= ArtciPlani.EN_KISA_SURE && a.sure() <= ArtciPlani.EN_UZUN_SURE, "süre " + a.sure());
            assertEquals(kesir, a.kesir(), 1e-12, "ilk %15, sonra 0,8 kat");
            onceki = a.bitis();
            kesir *= 0.8;
        }
        ArtciPlani.Artci ikinci = plan.get(1);
        assertEquals(2, ArtciPlani.suren(plan, ikinci.baslangic()));
        assertEquals(2, ArtciPlani.suren(plan, ikinci.bitis() - 1));
        assertEquals(0, ArtciPlani.suren(plan, ikinci.bitis()), "artçılar arasında süren yok");
        assertEquals(0, ArtciPlani.suren(plan, ana - 1), "ana sarsıntıda artçı yok");
        assertEquals(plan.get(2).bitis(), ArtciPlani.son(plan, ana));
    }

    @Test
    void artcisizPlanAnaSarsintidaBiterZarfSinirli() {
        assertTrue(ArtciPlani.olustur(300, 0, 400, 1200, 0.15, 1L).isEmpty());
        assertEquals(300, ArtciPlani.son(java.util.List.of(), 300));
        java.util.List<ArtciPlani.Artci> ters = ArtciPlani.olustur(0, 2, 50, 10, 0.2, 3L);
        assertTrue(ters.get(0).baslangic() >= 10 && ters.get(0).baslangic() <= 50, "ters verilen aralık düzeltilir");
        assertEquals(0.0, SarsintiZarfi.artci(-1, 80), 1e-12);
        assertEquals(0.0, SarsintiZarfi.artci(80, 80), 1e-12);
        assertEquals(1.0, SarsintiZarfi.artci(40, 80), 1e-12, "ortada tam");
        assertEquals(0.5, SarsintiZarfi.artci(5, 80), 1e-12, "girişin yarısı");
        assertEquals(0.5, SarsintiZarfi.artci(65, 80), 1e-12, "sönümün yarısı");
    }
}
