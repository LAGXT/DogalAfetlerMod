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
        assertEquals(6, s1.tabanCapi(), E);
        assertEquals(50, s1.yukseklik(), E);
        assertEquals(60, s1.aktifSureSn());
        assertEquals(34, s5.tabanCapi(), E);
        assertEquals(55, s5.tepeCapi(), E);
        assertEquals(110, s5.yukseklik(), E);
        assertEquals(4.4, s5.hiz(), E);
        assertEquals(180 * 20L, s5.aktifSureTick());
        assertEquals(20, s5.blokKaldirmaTick());
        assertEquals(30.6, s5.etekYaricapi(), 1e-6);
        assertTrue(s1.altBoslukOrani() > 0, "S1 yere tam değmeyen halat");
        assertEquals(0, s5.altBoslukOrani(), E);
        assertFalse(s1.zeminKazar());
        assertTrue(s5.zeminKazar());
        assertThrows(IllegalArgumentException.class, () -> KasirgaSeviyesi.icin(0));
        assertThrows(IllegalArgumentException.class, () -> KasirgaSeviyesi.icin(6));
    }

    @Test
    void kamaYalnizS4VeS5() {
        assertFalse(KasirgaSeviyesi.icin(3).kamaMi());
        assertTrue(KasirgaSeviyesi.icin(4).kamaMi());
        assertTrue(KasirgaSeviyesi.icin(5).kamaMi());
        assertEquals(38.5, KasirgaSeviyesi.icin(4).ortaGenislik(), 0.1);
        assertEquals(47.9, KasirgaSeviyesi.icin(5).ortaGenislik(), 0.1);
    }

    @Test
    void carpanlarBoyutVeHiziAyriOlcekler() {
        KasirgaSeviyesi s = KasirgaSeviyesi.icin(3, new Carpanlar(2, 1, 1, 1, 1, 1));
        assertEquals(32, s.tabanCapi(), E);
        assertEquals(48, s.cekimYaricapi(), E);
        assertEquals(3.2, s.hiz(), E);
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
        assertEquals(700, y.tabanSpriteSayisi(5, 30, lod));
        assertEquals(140, y.tabanSpriteSayisi(1, 30, lod));
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
        assertThrows(IllegalArgumentException.class, () -> new KasirgaKaliteButcesi.Lod(100, 50, 200));
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

    @Test
    void cekimIceSiklonikVeKaldirmaCekirdekte() {
        double[] disari = Cekim.ivme(10, 0, 0, 3, 9, 3, 1, 25);
        assertEquals(0, disari[0] + disari[1] + disari[2], E);
        double[] dogu = Cekim.ivme(5, 0, 0, 3, 9, 3, 1, 25);
        assertTrue(dogu[0] < 0, "içe (batıya) çeker");
        assertTrue(dogu[2] < 0, "siklonik: doğudaki varlık kuzeye (-z) sürüklenir");
        assertEquals(0, dogu[1], E);
        double[] cekirdek = Cekim.ivme(1, 0, 0, 3, 9, 3, 1, 25);
        assertTrue(cekirdek[1] > 0.08, "S3 çekirdekte yerçekimini aşar");
        assertEquals(0, Cekim.ivme(1, 0, 25, 3, 9, 3, 1, 25)[1], E);
        assertTrue(Cekim.ivme(0, 0, 0, 3, 9, 1, 1, 25)[1] < 0.08, "S1 (F1) varlıkları havaya kaldırmaz");
        assertTrue(Math.abs(Cekim.ivme(5, 0, 0, 3, 9, 5, 1, 25)[0]) > Math.abs(Cekim.ivme(5, 0, 0, 3, 9, 1, 1, 25)[0]));
        assertEquals(0, Cekim.ivme(5, 0, 0, 3, 9, 5, 0, 25)[0], E);
        double[] hiz = Cekim.kirp(new double[] {3, 2, 4});
        assertEquals(Cekim.EN_COK_YATAY, Math.hypot(hiz[0], hiz[2]), 1e-9);
        assertEquals(Cekim.EN_COK_DIKEY, hiz[1], E);
    }
}
