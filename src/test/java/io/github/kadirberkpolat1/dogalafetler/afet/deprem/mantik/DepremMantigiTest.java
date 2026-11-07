package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
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

    @Test
    void aktifArtcilariBeklemezKaziPayiyleBiter() {
        int sure = DepremSeviyesi.varsayilan(3).sureTick();
        assertFalse(ArtciPlani.aktifBitti(sure - 1, sure, true), "ana sarsıntı sürerken");
        assertTrue(ArtciPlani.aktifBitti(sure, sure, true), "ana sarsıntı bitti, kazı tamam: artçı beklenmez");
        assertFalse(ArtciPlani.aktifBitti(sure + ArtciPlani.KAZI_PAYI - 1, sure, false), "kazı sürüyor");
        assertTrue(ArtciPlani.aktifBitti(sure + ArtciPlani.KAZI_PAYI, sure, false), "kazı payı doldu");
    }

    @Test
    void devirArtcilariKuyrugaKaydirirSiraVeSureyiKorur() {
        int ana = 500;
        List<ArtciPlani.Artci> plan = ArtciPlani.olustur(ana, 3, 400, 1200, 0.15, 5L);
        List<ArtciPlani.Artci> kuyruk = ArtciPlani.devret(plan, ana);
        assertEquals(3, kuyruk.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(plan.get(i).baslangic() - ana, kuyruk.get(i).baslangic(), "zaman devirden ölçülür");
            assertEquals(plan.get(i).sure(), kuyruk.get(i).sure());
            assertEquals(plan.get(i).kesir(), kuyruk.get(i).kesir(), 1e-12);
        }
        long gec = plan.get(0).baslangic() + 30;
        List<ArtciPlani.Artci> gecikmeli = ArtciPlani.devret(plan, gec);
        assertEquals(0, gecikmeli.get(0).baslangic(), "başlangıcı geçmiş artçı devirde başlar");
        assertEquals(plan.get(0).sure(), gecikmeli.get(0).sure());
        for (int i = 1; i < 3; i++) {
            assertTrue(gecikmeli.get(i).baslangic() >= gecikmeli.get(i - 1).bitis(), "artçılar çakışmaz");
        }
        assertTrue(ArtciPlani.devret(List.of(), ana).isEmpty());
    }

    @Test
    void kuyrukSinirlariSirayiIlerletirVeBiter() {
        List<ArtciPlani.Artci> plan = List.of(new ArtciPlani.Artci(100, 160, 0.15), new ArtciPlani.Artci(500, 580, 0.12));
        assertEquals(100, ArtciPlani.sonrakiSinir(plan, 0), "sıradaki artçının başı");
        assertEquals(160, ArtciPlani.sonrakiSinir(plan, 100), "süren artçının sonu");
        assertEquals(500, ArtciPlani.sonrakiSinir(plan, 160));
        assertEquals(580, ArtciPlani.sonrakiSinir(plan, 520));
        assertEquals(580, ArtciPlani.sonrakiSinir(plan, 580), "artçı kalmadı: son");
        assertFalse(ArtciPlani.bitti(plan, 579));
        assertTrue(ArtciPlani.bitti(plan, 580));
        assertTrue(ArtciPlani.bitti(List.of(), 0), "boş kuyruk hemen biter");
    }

    @Test
    void icYikimSeviyeEsikleri() {
        IcYikimSeviyesi s1 = IcYikimSeviyesi.seviye(1);
        IcYikimSeviyesi s2 = IcYikimSeviyesi.seviye(2);
        IcYikimSeviyesi s3 = IcYikimSeviyesi.seviye(3);
        IcYikimSeviyesi s4 = IcYikimSeviyesi.seviye(4);
        IcYikimSeviyesi s5 = IcYikimSeviyesi.seviye(5);
        assertTrue(s1.bos() && !s1.dekor() && !s1.yaprak() && !s1.tavan(), "S1 içeriye dokunmaz");
        assertTrue(!s2.bos() && s2.dekor() && !s2.yaprak() && !s2.tavan(), "S2 yalnız dekor");
        assertTrue(s3.dekor() && s3.yaprak() && !s3.tavan(), "S3 + yaprak");
        assertTrue(s4.dekor() && s4.yaprak() && s4.tavan(), "S4 + zayıf tavan");
        IcYikimSeviyesi onceki = s1;
        for (int sev = 2; sev <= 5; sev++) {
            IcYikimSeviyesi s = IcYikimSeviyesi.seviye(sev);
            assertTrue(s.olcek() > onceki.olcek() && s.dekorOlasiligi() > onceki.dekorOlasiligi(), "S" + sev + " büyümeli");
            assertTrue(s.yaprakOlasiligi() >= onceki.yaprakOlasiligi() && s.tavanOlasiligi() >= onceki.tavanOlasiligi());
            assertTrue(s.dekorOlasiligi() <= 1 && s.yaprakOlasiligi() <= 1 && s.tavanOlasiligi() <= 1);
            onceki = s;
        }
        assertTrue(s5.tavanOlasiligi() > s4.tavanOlasiligi() && s5.yaprakOlasiligi() > s4.yaprakOlasiligi(), "S5 daha çok");
        assertEquals(1, IcYikimSeviyesi.seviye(0).seviye());
        assertEquals(5, IcYikimSeviyesi.seviye(9).seviye());
    }

    @Test
    void icYikimButcesiSeviyeOlcegiVeTpsKademesiyle() {
        int ayar = 8;
        assertEquals(0, IcYikimSeviyesi.seviye(1).beklenenSutun(ayar, 1), 1e-12);
        assertEquals(4, IcYikimSeviyesi.seviye(2).beklenenSutun(ayar, 1), 1e-12);
        assertEquals(6, IcYikimSeviyesi.seviye(3).beklenenSutun(ayar, 1), 1e-12);
        IcYikimSeviyesi s4 = IcYikimSeviyesi.seviye(4);
        assertEquals(8, s4.beklenenSutun(ayar, 1), 1e-12, "config S4 ölçeğindedir");
        assertEquals(14, IcYikimSeviyesi.seviye(5).beklenenSutun(ayar, 1), 1e-12);
        assertEquals(2, s4.beklenenSutun(ayar, 0.25), 1e-12, "TPS çeyrek kademesi");
        assertEquals(32, s4.beklenenSutun(ayar, 4), 1e-12, "dört adımlık tick (hız)");
        assertEquals(0, s4.beklenenSutun(-3, 1), 1e-12);
        assertEquals(0, IcYikimSeviyesi.seviye(3).tavanSiniri(ayar), "S3'te tavan yok");
        assertEquals(4, s4.tavanSiniri(ayar));
        assertEquals(7, IcYikimSeviyesi.seviye(5).tavanSiniri(ayar));
    }

    @Test
    void zayifTavanSertlikEsigi() {
        IcYikimSeviyesi s4 = IcYikimSeviyesi.seviye(4);
        IcYikimSeviyesi s5 = IcYikimSeviyesi.seviye(5);
        assertTrue(s4.zayifSertlik(0.8f), "yün (0,8) S4'te zayıf");
        assertFalse(s4.zayifSertlik(1.0f), "balkabağı (1,0) S4'te zayıf değil");
        assertTrue(s5.zayifSertlik(1.0f));
        assertFalse(s5.zayifSertlik(1.25f), "pişmiş kil (1,25) zayıf değil");
        assertFalse(IcYikimSeviyesi.seviye(3).zayifSertlik(0.1f), "S3'te tavan yok");
        assertFalse(s5.zayifSertlik(-1f), "kırılmaz blok");
        assertEquals(0.1, IcYikimSeviyesi.YAPRAK_ESYA_ORANI, 1e-12);
    }

    @Test
    void artciSarsintisiKatmandanGeriOkunurUzakliklaIner() {
        double s3 = ArtciSarsintisi.genlik(DepremSeviyesi.varsayilan(3).kameraGenligi(), 0.15);
        double s5 = ArtciSarsintisi.genlik(DepremSeviyesi.varsayilan(5).kameraGenligi(), 0.15);
        assertEquals(0.225, s3, 1e-12);
        assertEquals(0.6, s5, 1e-12);
        assertEquals(s3, ArtciSarsintisi.genlikOku(ArtciSarsintisi.hacim(s3)), 1e-6, "eğri tepesinden geri okunur");
        assertEquals(s5, ArtciSarsintisi.genlikOku(ArtciSarsintisi.hacim(s5)), 1e-6);
        assertTrue(ArtciSarsintisi.hacim(s5) > ArtciSarsintisi.hacim(s3), "büyük depremin artçısı daha gür");
        assertEquals(1f, ArtciSarsintisi.hacim(2), 1e-6f, "hacim kırpılır");
        assertEquals(0.0, ArtciSarsintisi.genlikOku(0.1), 1e-12);
        assertEquals(56, ArtciSarsintisi.yaricap(ArtciSarsintisi.menzil(56)), 1e-6);
        float tepe = ArtciSarsintisi.hacim(s5);
        double menzil = ArtciSarsintisi.menzil(56);
        assertEquals(s5, ArtciSarsintisi.oyuncuGenligi(tepe, 40, 80, 0, menzil), 1e-6, "merkezde, zarf ortasında tam");
        assertEquals(0.15 * s5, ArtciSarsintisi.oyuncuGenligi(tepe, 40, 80, 56, menzil), 1e-6, "yarıçapta %15");
        assertEquals(0.0, ArtciSarsintisi.oyuncuGenligi(tepe, 40, 80, 56 + 65, menzil), 1e-12, "sönüm payı ötesi");
        assertEquals(0.0, ArtciSarsintisi.oyuncuGenligi(tepe, 80, 80, 0, menzil), 1e-12, "artçı bitti");
    }
}
