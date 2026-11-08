package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import org.junit.jupiter.api.Test;

/** Kasırganın saf mantığı (madde 10 seviye tablosu, tasarım notu §2, §5, §6). */
class KasirgaSafMantikTest {
    private static final double E = 1e-9;

    @Test
    void seviyeArttikcaBoyutHizSureCekimBlokVeHasarArtar() {
        for (int l = 2; l <= 5; l++) {
            KasirgaSeviyesi a = KasirgaSeviyesi.icin(l - 1);
            KasirgaSeviyesi b = KasirgaSeviyesi.icin(l);
            assertTrue(b.tabanCapi() > a.tabanCapi() && b.tepeCapi() > a.tepeCapi() && b.yukseklik() > a.yukseklik(), "boyut " + l);
            assertTrue(b.hiz() > a.hiz() && b.aktifSureSn() > a.aktifSureSn(), "hız/süre " + l);
            assertTrue(b.cekimYaricapi() > a.cekimYaricapi() && b.blokKaldirmaTick() > a.blokKaldirmaTick(), "çekim/blok " + l);
            assertTrue(b.sertlikSiniri() > a.sertlikSiniri() && b.cekirdekHasari() > a.cekirdekHasari(), "sertlik/hasar " + l);
            assertTrue(b.enkazYuksekligi() > a.enkazYuksekligi(), "etek " + l);
        }
    }

    @Test
    void tabloTasarimNotuylaAyni() {
        KasirgaSeviyesi s1 = KasirgaSeviyesi.icin(1);
        KasirgaSeviyesi s5 = KasirgaSeviyesi.icin(5);
        assertEquals(10, s1.tabanCapi(), E);
        assertEquals(30, s1.yukseklik(), E);
        assertEquals(15, s1.cekimYaricapi(), E);
        assertEquals(60, s1.aktifSureSn());
        assertEquals(48, s5.tabanCapi(), E);
        assertEquals(76.8, s5.tepeCapi(), 1e-9);
        assertEquals(144, s5.yukseklik(), 1e-9);
        assertEquals(72, s5.cekimYaricapi(), 1e-9);
        assertEquals(4.4, s5.hiz(), E);
        assertEquals(180 * 20L, s5.aktifSureTick());
        assertEquals(32, s5.blokKaldirmaTick());
        assertEquals(6, s5.sertlikSiniri(), E);
        assertEquals(6, s5.cekirdekHasari(), E);
        assertEquals(43.2, s5.etekYaricapi(), 1e-9);
        assertTrue(s1.altBoslukOrani() > 0, "S1 yere tam değmeyen halat");
        assertEquals(0, s5.altBoslukOrani(), E);
        assertFalse(s1.zeminKazar());
        assertTrue(s5.zeminKazar());
        assertThrows(IllegalArgumentException.class, () -> KasirgaSeviyesi.icin(0));
        assertThrows(IllegalArgumentException.class, () -> KasirgaSeviyesi.icin(6));
    }

    /** Kullanıcı isteği (tur 3): gücüyle boyu doğru orantılı; S5 belirgin daha büyük ve yıkıcı. */
    @Test
    void butunBoyutlarGucleDogruOrantili() {
        KasirgaSeviyesi s1 = KasirgaSeviyesi.icin(1);
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            double g = s.guc() / s1.guc();
            String ad = "S" + l;
            assertEquals(s1.tabanCapi() * g, s.tabanCapi(), 1e-9, ad);
            assertEquals(s1.tepeCapi() * g, s.tepeCapi(), 1e-9, ad);
            assertEquals(s1.yukseklik() * g, s.yukseklik(), 1e-9, ad);
            assertEquals(s1.cekimYaricapi() * g, s.cekimYaricapi(), 1e-9, ad);
            assertEquals(s1.enkazYuksekligi() * g, s.enkazYuksekligi(), 1e-9, ad);
            assertEquals(s1.etekYaricapi() * g, s.etekYaricapi(), 1e-9, ad);
            assertEquals(s1.duvarBulutuYaricapi() * g, s.duvarBulutuYaricapi(), 1e-9, ad);
            assertEquals(KasirgaYikimi.yikimYaricapi(s1.cekimYaricapi()) * g, KasirgaYikimi.yikimYaricapi(s.cekimYaricapi()),
                    1e-9, ad);
            assertEquals(s1.kaldirmaTavani(36) * g, s.kaldirmaTavani(36), 1e-9, ad);
            assertEquals(s1.ucusYuksekligi(0.5) * g, s.ucusYuksekligi(0.5), 1e-9, ad);
        }
        KasirgaSeviyesi s4 = KasirgaSeviyesi.icin(4);
        KasirgaSeviyesi s5 = KasirgaSeviyesi.icin(5);
        assertTrue(s5.guc() / s4.guc() > 1.4, "S5 S4'ten belirgin güçlü");
        assertTrue(s5.blokKaldirmaTick() >= 1.5 * 20, "S5 eski 20 blok/tick'ten belirgin yıkıcı");
        assertEquals(KasirgaSeviyesi.S5_KALDIRMA_TAVANI, s5.kaldirmaTavani(KasirgaSeviyesi.S5_KALDIRMA_TAVANI), 1e-9);
        assertEquals(0.25 * s5.yukseklik(), s5.kaldirmaTavani(KasirgaSeviyesi.S5_KALDIRMA_TAVANI), 1e-9);
        assertEquals(7.5, s1.kaldirmaTavani(KasirgaSeviyesi.S5_KALDIRMA_TAVANI), 1e-9);
        KasirgaSeviyesi buyuk = KasirgaSeviyesi.icin(5, new Carpanlar(2, 1, 1, 1, 1, 1));
        assertEquals(72, buyuk.kaldirmaTavani(KasirgaSeviyesi.S5_KALDIRMA_TAVANI), 1e-9, "boyut çarpanı tavanı da ölçekler");
        assertEquals(0.2 * s5.yukseklik(), s5.ucusYuksekligi(0), 1e-9);
        assertEquals(0.55 * s5.yukseklik(), s5.ucusYuksekligi(1), 1e-9);
    }

    /** Boyutlar tek katsayıyla ölçeklendiği için huni biçimi her seviyede aynı: eski S5 gibi kama (orta genişlik ≥ %40). */
    @Test
    void huniBicimiHerSeviyedeAyniKama() {
        double oran = KasirgaSeviyesi.icin(1).ortaGenislik() / KasirgaSeviyesi.icin(1).yukseklik();
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            assertEquals(oran, s.ortaGenislik() / s.yukseklik(), 1e-9, "S" + l);
            assertTrue(s.kamaMi(), "S" + l);
        }
        assertEquals(67.0, KasirgaSeviyesi.icin(5).ortaGenislik(), 0.1);
    }

    @Test
    void carpanlarBoyutVeHiziAyriOlcekler() {
        KasirgaSeviyesi s = KasirgaSeviyesi.icin(3, new Carpanlar(2, 1, 1, 1, 1, 1));
        assertEquals(48, s.tabanCapi(), 1e-9);
        assertEquals(72, s.cekimYaricapi(), 1e-9);
        assertEquals(3.2, s.hiz(), E);
        assertEquals(KasirgaSeviyesi.icin(3).guc(), s.guc(), E, "boyut çarpanı gücü değiştirmez");
        KasirgaSeviyesi h = KasirgaSeviyesi.icin(3, new Carpanlar(1, 0.5, 2, 1, 0.5, 3));
        assertEquals(1.6, h.hiz(), E);
        assertEquals(240, h.aktifSureSn());
        assertEquals(4, h.blokKaldirmaTick());
        assertEquals(6, h.cekirdekHasari(), E);
        assertTrue(KasirgaSeviyesi.icin(2, Carpanlar.BIR) == KasirgaSeviyesi.icin(2), "çarpan 1 tabloyu döner");
        assertThrows(IllegalArgumentException.class, () -> new Carpanlar(0, 1, 1, 1, 1, 1));
    }

    @Test
    void huniYukariGenislerEtekKubbeBicimli() {
        assertEquals(3, HuniProfili.yaricap(3, 5, 0), E);
        assertEquals(5, HuniProfili.yaricap(3, 5, 1), E);
        assertEquals(5, HuniProfili.yaricap(3, 5, 7), E);
        double onceki = 0;
        for (int i = 0; i <= 10; i++) {
            double r = HuniProfili.yaricap(3, 5, i / 10.0);
            assertTrue(r >= onceki);
            onceki = r;
        }
        assertEquals(10, EtekProfili.yaricap(10, 4, 0), E);
        assertEquals(4, EtekProfili.yaricap(10, 4, 1), E);
        assertTrue(EtekProfili.yaricap(10, 4, 0.5) > 7 + 1e-6, "kubbe: orta yükseklik doğrusaldan geniş");
        assertEquals(EtekProfili.TABAN_ALFA, EtekProfili.alfa(0.3), E);
        assertEquals(0, EtekProfili.alfa(1), E);
    }

    @Test
    void rankineCekirdekSinirindaSurekliVeEnBuyuk() {
        RankineProfili r = new RankineProfili(2, 4);
        assertEquals(2, r.acisalHiz(1), E);
        assertEquals(8, r.tegetHiz(4), E);
        assertEquals(4, r.tegetHiz(8), E);
        assertTrue(r.tegetHiz(3.9) < r.tegetHiz(4) && r.tegetHiz(4.1) < r.tegetHiz(4));
        assertThrows(IllegalArgumentException.class, () -> new RankineProfili(1, 0));
    }

    @Test
    void evrelerSirayla() {
        long olgun = 1200;
        assertEquals(1600, KasirgaEvresi.toplamAktif(olgun));
        assertEquals(KasirgaEvresi.DOGUS, KasirgaEvresi.aktifte(0, olgun).evre());
        assertEquals(KasirgaEvresi.OLGUN, KasirgaEvresi.aktifte(160, olgun).evre());
        assertEquals(KasirgaEvresi.IP, KasirgaEvresi.aktifte(1360, olgun).evre());
        assertEquals(KasirgaEvresi.DAGILMA, KasirgaEvresi.aktifte(1560, olgun).evre());
        KasirgaEvresi.Durum son = KasirgaEvresi.aktifte(5000, olgun);
        assertEquals(KasirgaEvresi.DAGILMA, son.evre());
        assertEquals(1, son.ilerleme(), E);
        assertEquals(0.5, KasirgaEvresi.aktifte(80, olgun).ilerleme(), E);
        for (KasirgaEvresi e : new KasirgaEvresi[] {KasirgaEvresi.DOGUS, KasirgaEvresi.OLGUN, KasirgaEvresi.IP,
            KasirgaEvresi.DAGILMA}) {
            KasirgaEvresi.Durum d = KasirgaEvresi.aktifte(KasirgaEvresi.baslangic(e, olgun), olgun);
            assertEquals(e, d.evre());
            assertEquals(0, d.ilerleme(), E);
        }
        assertEquals(KasirgaEvresi.DAGILMA, KasirgaEvresi.sirayla(99));
        assertEquals(KasirgaEvresi.OLUSUM, KasirgaEvresi.sirayla(-3));
    }

    @Test
    void kaldirmaHizVeGucPaylari() {
        assertEquals(0, KasirgaEvresi.kaldirmaPayi(new KasirgaEvresi.Durum(KasirgaEvresi.OLUSUM, 0.9)), E);
        assertEquals(0, KasirgaEvresi.kaldirmaPayi(new KasirgaEvresi.Durum(KasirgaEvresi.DOGUS, 0.3)), E);
        assertEquals(0.5, KasirgaEvresi.kaldirmaPayi(new KasirgaEvresi.Durum(KasirgaEvresi.DOGUS, 0.7)), E);
        assertEquals(1, KasirgaEvresi.kaldirmaPayi(new KasirgaEvresi.Durum(KasirgaEvresi.OLGUN, 0.2)), E);
        assertEquals(0, KasirgaEvresi.kaldirmaPayi(new KasirgaEvresi.Durum(KasirgaEvresi.DAGILMA, 0.2)), E);
        assertEquals(0, KasirgaEvresi.hizPayi(KasirgaEvresi.OLUSUM), E);
        assertEquals(1, KasirgaEvresi.hizPayi(KasirgaEvresi.OLGUN), E);
        assertEquals(0, KasirgaEvresi.guc(new KasirgaEvresi.Durum(KasirgaEvresi.OLUSUM, 1)), E);
        assertEquals(1, KasirgaEvresi.guc(new KasirgaEvresi.Durum(KasirgaEvresi.OLGUN, 0.5)), E);
        assertEquals(0, KasirgaEvresi.guc(new KasirgaEvresi.Durum(KasirgaEvresi.DAGILMA, 1)), E);
    }

    @Test
    void kaliteButcesiVeLod() {
        KasirgaKaliteButcesi.Lod lod = KasirgaKaliteButcesi.Lod.VARSAYILAN;
        KasirgaKaliteButcesi y = KasirgaKaliteButcesi.icin(EfektKalitesi.YUKSEK);
        assertEquals(175, y.tabanSpriteSayisi(5, 30, lod));
        assertEquals(35, y.tabanSpriteSayisi(1, 30, lod));
        assertEquals(700, y.tabanSpriteSayisi(5, 60, lod));
        assertEquals(140, y.tabanSpriteSayisi(1, 60, lod));
        assertEquals(350, y.tabanSpriteSayisi(5, 100, lod));
        assertEquals(0, y.tabanSpriteSayisi(5, 300, lod));
        assertEquals(4, y.gorselEnkazSayisi(150, lod));
        assertEquals(0, y.gorselEnkazSayisi(170, lod));
        assertEquals(120, y.duvarSpriteSayisi(200, lod));
        assertEquals(60, y.duvarSpriteSayisi(300, lod));
        assertEquals(0, y.altGirdapSayisi(3, 10, lod));
        assertEquals(3, y.altGirdapSayisi(4, 10, lod));
        assertEquals(4, y.altGirdapSayisi(5, 10, lod));
        assertEquals(0, KasirgaKaliteButcesi.icin(EfektKalitesi.DUSUK).altGirdapSayisi(5, 10, lod));
        int onceki = 0;
        for (EfektKalitesi k : EfektKalitesi.values()) {
            KasirgaKaliteButcesi b = KasirgaKaliteButcesi.icin(k);
            assertTrue(b.huniDilim() * b.huniHalka() > onceki, "kalite arttıkça ağ sıklaşır: " + k);
            onceki = b.huniDilim() * b.huniHalka();
        }
        assertThrows(IllegalArgumentException.class, () -> new KasirgaKaliteButcesi.Lod(0, 100, 50, 200));
        assertThrows(IllegalArgumentException.class, () -> new KasirgaKaliteButcesi.Lod(120, 100, 150, 200));
    }

    @Test
    void dogusMesafesiSirenMenzilindeVeSapmaSinirli() {
        assertEquals(32.5, Yonelim.dogusMesafesi(9, 0), E);
        assertEquals(65, Yonelim.dogusMesafesi(9, 1), E);
        assertEquals(107, Yonelim.dogusMesafesi(51, 1), E);
        for (int i = 0; i <= 10; i++) {
            double d = Yonelim.dogusMesafesi(51, i / 10.0);
            // Siren alıcıları etki alanı (çekim yarıçapı) + 64 blok içindekiler (çekirdek §4).
            assertTrue(d <= 51 + 64, "hedef sireni duymalı");
            double yon = Yonelim.sapmaliYon(1, i / 10.0);
            assertTrue(Math.abs(yon - 1) <= Yonelim.SAPMA + E);
        }
        double[] ofset = Yonelim.dogusOfseti(9, 1, Math.PI / 2);
        assertEquals(0, ofset[0], 1e-9);
        assertEquals(65, ofset[1], 1e-9);
        assertEquals(Math.PI / 2, Yonelim.yon(0, 0, 0, 10), E);
    }

    @Test
    void donusYumusakVeKisaYoldan() {
        assertEquals(Yonelim.DONUS_HIZI, Yonelim.don(0, 1), E);
        assertEquals(0.01, Yonelim.don(0, 0.01), E);
        assertEquals(0.2, Yonelim.aciFarki(0.1, 2 * Math.PI - 0.1), 1e-9);
        // 3,1 → -3,1 en kısa yol π üzerinden artı yönde.
        assertEquals(3.1 + Yonelim.DONUS_HIZI, Yonelim.don(3.1, -3.1), 1e-9);
    }

    /** S3 (çekirdek 12, çekim yarıçapı 36, güç 2,4, tavan 18) yerde duran varlık için çekim. */
    private static double[] s3(double dx, double dz, double h, double seviyeGucu, double carpan) {
        return Cekim.ivme(dx, dz, h, 0, 0, 12, 36, seviyeGucu, carpan, 18, Cekim.YER_SURTUNMESI);
    }

    @Test
    void cekimIceBaskinSiklonikVeKaldirmaCekirdekte() {
        double[] disari = s3(36.5, 0, 0, 2.4, 1);
        assertEquals(0, disari[0] + disari[1] + disari[2], E);
        double[] dogu = s3(20, 0, 0, 2.4, 1);
        assertTrue(dogu[0] < 0, "içe (batıya) çeker");
        assertTrue(dogu[2] < 0, "siklonik: doğudaki varlık kuzeye (-z) sürüklenir");
        assertEquals(Cekim.TEGET_ORANI * -dogu[0], -dogu[2], 1e-12, "teğet içenin yarısı");
        assertTrue(Cekim.TEGET_ORANI <= 0.5);
        assertEquals(0, dogu[1], E);
        double[] cekirdek = s3(1, 0, 0, 2.4, 1);
        assertTrue(cekirdek[1] > 0.08, "S3 çekirdekte yerçekimini aşar");
        assertEquals(0, s3(1, 0, 18, 2.4, 1)[1], E, "tavanda kaldırma durur");
        assertTrue(s3(0, 0, 0, KasirgaSeviyesi.icin(1).guc(), 1)[1] < 0.08, "S1 (F1) varlıkları havaya kaldırmaz");
        assertTrue(s3(0, 0, 0, KasirgaSeviyesi.icin(2).guc(), 1)[1] > 0.08, "S2 eksende kaldırır");
        assertTrue(Math.abs(s3(20, 0, 0, 4.8, 1)[0]) > Math.abs(s3(20, 0, 0, 1, 1)[0]));
        assertEquals(0, s3(20, 0, 0, 4.8, 0)[0], E);
        double yerde = s3(20, 0, 0, 2.4, 1)[0];
        double havada = Cekim.ivme(20, 0, 5, 0, 0, 12, 36, 2.4, 1, 18, Cekim.HAVA_SURTUNMESI)[0];
        assertEquals((1 - Cekim.HAVA_SURTUNMESI) / (1 - Cekim.YER_SURTUNMESI), havada / yerde, 1e-9,
                "ivme sürtünmeyle ölçekli: yerde ve havada aynı sürüklenme hızı");
        double[] hiz = Cekim.kirp(new double[] {3, 2, 4});
        assertEquals(Cekim.EN_COK_YATAY, Math.hypot(hiz[0], hiz[2]), 1e-9);
        assertEquals(Cekim.EN_COK_DIKEY, hiz[1], E);
    }

    /** Hız ne olursa olsun çekirdekte ve çekim bölgesinde radyal ivme dışa bakmaz (merkezkaçla savurma yok). */
    @Test
    void hicbirYerdeDisaNetIvmeYok() {
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            for (double d = 0.25; d < s.cekimYaricapi(); d += 0.5) {
                for (double vt = -1.4; vt <= 1.4; vt += 0.35) {
                    for (double vr = -1.0; vr <= 1.0; vr += 0.5) {
                        for (double f : new double[] {Cekim.YER_SURTUNMESI, Cekim.HAVA_SURTUNMESI}) {
                            // Varlık doğuda (+x): radyal hız x, teğet hız -z yönünde (siklonik).
                            double[] a = Cekim.ivme(d, 0, 2, vr, -vt, s.tabanYaricapi(), s.cekimYaricapi(), s.guc(), 1,
                                    s.kaldirmaTavani(36), f);
                            assertTrue(a[0] <= 1e-12, "S" + l + " d " + d + " vt " + vt + " vr " + vr + ": dışa " + a[0]);
                        }
                    }
                }
            }
        }
    }
}
