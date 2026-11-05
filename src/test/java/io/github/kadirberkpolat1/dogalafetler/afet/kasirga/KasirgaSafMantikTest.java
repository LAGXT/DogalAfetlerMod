package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import org.junit.jupiter.api.Test;

class KasirgaSafMantikTest {
    private static final double EPS = 1e-9;

    // --- Seviye tablosu (tasarım notu §6) ---

    @Test
    void seviyeTablosuTasarimNotundakiDegerleriVerir() {
        KasirgaSeviyesi s1 = KasirgaSeviyesi.icin(1);
        assertEquals(6, s1.tabanCapi(), EPS);
        assertEquals(50, s1.yukseklik(), EPS);
        assertEquals(2.0, s1.hiz(), EPS);
        assertEquals(60, s1.aktifSureSn());
        assertEquals(9, s1.cekimYaricapi(), EPS);
        assertEquals(2, s1.blokKaldirmaTick());
        assertEquals(0.6, s1.sertlikSiniri(), EPS);

        KasirgaSeviyesi s5 = KasirgaSeviyesi.icin(5);
        assertEquals(34, s5.tabanCapi(), EPS);
        assertEquals(110, s5.yukseklik(), EPS);
        assertEquals(4.4, s5.hiz(), EPS);
        assertEquals(180, s5.aktifSureSn());
        assertEquals(51, s5.cekimYaricapi(), EPS);
        assertEquals(20, s5.blokKaldirmaTick());
        assertEquals(5.0, s5.sertlikSiniri(), EPS);
        assertEquals(4.0, s5.cekirdekHasari(), EPS);
    }

    @Test
    void seviyeArttikcaHerBoyutBuyur() {
        for (int s = 2; s <= 5; s++) {
            KasirgaSeviyesi once = KasirgaSeviyesi.icin(s - 1);
            KasirgaSeviyesi simdi = KasirgaSeviyesi.icin(s);
            assertTrue(simdi.tabanCapi() > once.tabanCapi(), "taban çapı " + s);
            assertTrue(simdi.yukseklik() > once.yukseklik(), "yükseklik " + s);
            assertTrue(simdi.hiz() > once.hiz(), "hız " + s);
            assertTrue(simdi.tepeYaricapi() > once.tepeYaricapi(), "tepe yarıçapı " + s);
            assertTrue(simdi.enkazYuksekligi() > once.enkazYuksekligi(), "enkaz yüksekliği " + s);
            assertTrue(simdi.blokKaldirmaTick() > once.blokKaldirmaTick(), "blok/tick " + s);
        }
    }

    @Test
    void besinciSeviyeKamaBirinciSeviyeIncedir() {
        // Kama: taban yarıçapı tepe yarıçapının yarısından büyük (tasarım notu §2 katman 2).
        assertTrue(KasirgaSeviyesi.icin(5).kamaMi());
        assertTrue(KasirgaSeviyesi.icin(4).kamaMi());
        assertFalse(KasirgaSeviyesi.icin(1).kamaMi());
        // S1 yere tam değmez; S3 ve üstü değer.
        assertTrue(KasirgaSeviyesi.icin(1).altBoslukOrani() > 0);
        assertEquals(0, KasirgaSeviyesi.icin(3).altBoslukOrani(), EPS);
        // Enkaz halkası taban çapının 1,8 katı: yarıçapı tabanYarıçapı × 1,8.
        assertEquals(17 * 1.8, KasirgaSeviyesi.icin(5).enkazYaricapi(), EPS);
        assertEquals(6, KasirgaSeviyesi.icin(1).enkazYuksekligi(), EPS);
        assertEquals(14, KasirgaSeviyesi.icin(5).enkazYuksekligi(), EPS);
    }

    @Test
    void gecersizSeviyeReddedilir() {
        assertThrows(IllegalArgumentException.class, () -> KasirgaSeviyesi.icin(0));
        assertThrows(IllegalArgumentException.class, () -> KasirgaSeviyesi.icin(6));
    }

    // --- Huni yarıçap profili r(h) = rTaban + (rTepe - rTaban)·h^0.6 ---

    @Test
    void huniProfiliUclardaTabanVeTepeyiVerir() {
        assertEquals(3, HuniProfili.yaricap(3, 10, 0), EPS);
        assertEquals(10, HuniProfili.yaricap(3, 10, 1), EPS);
    }

    @Test
    void huniProfiliOrtadaUsluEgriyiIzler() {
        assertEquals(3 + 7 * Math.pow(0.5, 0.6), HuniProfili.yaricap(3, 10, 0.5), EPS);
        // h^0.6 içbükey: ortadaki yarıçap doğrusal ortalamanın üstünde.
        assertTrue(HuniProfili.yaricap(3, 10, 0.5) > 6.5);
    }

    @Test
    void huniProfiliMonotonVeAraligaKirpilir() {
        double onceki = HuniProfili.yaricap(17, 30, 0);
        for (int i = 1; i <= 100; i++) {
            double r = HuniProfili.yaricap(17, 30, i / 100.0);
            assertTrue(r >= onceki);
            onceki = r;
        }
        assertEquals(17, HuniProfili.yaricap(17, 30, -0.5), EPS);
        assertEquals(30, HuniProfili.yaricap(17, 30, 1.5), EPS);
    }

    // --- Rankine girdabı: çekirdekte katı cisim, dışında potansiyel girdap ---

    @Test
    void rankineCekirdekteAcisalHizSabittir() {
        RankineProfili p = new RankineProfili(2.0, 5.0);
        assertEquals(2.0, p.acisalHiz(0), EPS);
        assertEquals(2.0, p.acisalHiz(2.5), EPS);
        assertEquals(2.0, p.acisalHiz(5.0), EPS);
    }

    @Test
    void rankineDisindaAcisalHizUzaklikKaresiyleDuser() {
        RankineProfili p = new RankineProfili(2.0, 5.0);
        assertEquals(2.0 / 4, p.acisalHiz(10), EPS);
        assertEquals(2.0 / 9, p.acisalHiz(15), EPS);
    }

    @Test
    void rankineTegetHiziCekirdekSinirindaEnBuyukVeSureklidir() {
        RankineProfili p = new RankineProfili(2.0, 5.0);
        assertEquals(10.0, p.tegetHiz(5.0), EPS);
        assertEquals(5.0, p.tegetHiz(2.5), EPS);
        assertEquals(5.0, p.tegetHiz(10), EPS);
        assertEquals(p.tegetHiz(5.0 - 1e-7), p.tegetHiz(5.0 + 1e-7), 1e-5);
        for (double r = 0.5; r < 40; r += 0.5) {
            assertTrue(p.tegetHiz(r) <= p.tegetHiz(5.0) + EPS);
        }
    }

    @Test
    void rankineGecersizParametreyiReddeder() {
        assertThrows(IllegalArgumentException.class, () -> new RankineProfili(1, 0));
        assertThrows(IllegalArgumentException.class, () -> new RankineProfili(-1, 3));
    }

    // --- Kalite bütçesi ve LOD (tasarım notu §5) ---

    @Test
    void kaliteButcesiTasarimTablosunuVerir() {
        KasirgaKaliteButcesi yuksek = KasirgaKaliteButcesi.icin(EfektKalitesi.YUKSEK);
        assertEquals(24, yuksek.huniDilim());
        assertEquals(32, yuksek.huniHalka());
        assertEquals(700, yuksek.tabanSprite());
        assertEquals(4, yuksek.gorselEnkazBlokBasina());
        assertEquals(120, yuksek.duvarSprite());

        KasirgaKaliteButcesi ultra = KasirgaKaliteButcesi.icin(EfektKalitesi.ULTRA);
        assertEquals(1400, ultra.tabanSprite());
        assertEquals(32, ultra.huniDilim());
        assertEquals(48, ultra.huniHalka());
        assertEquals(6, ultra.gorselEnkazBlokBasina());

        KasirgaKaliteButcesi dusuk = KasirgaKaliteButcesi.icin(EfektKalitesi.DUSUK);
        assertEquals(150, dusuk.tabanSprite());
        assertEquals(12, dusuk.huniDilim());
        assertEquals(16, dusuk.huniHalka());
        assertEquals(1, dusuk.gorselEnkazBlokBasina());
        assertEquals(40, dusuk.duvarSprite());
    }

    @Test
    void tabanSpriteSayisiSeviyeyeVeMesafeyeGoreOlceklenir() {
        KasirgaKaliteButcesi yuksek = KasirgaKaliteButcesi.icin(EfektKalitesi.YUKSEK);
        assertEquals(700, yuksek.tabanSpriteSayisi(5, 30));
        assertEquals(140, yuksek.tabanSpriteSayisi(1, 30));
        // 96 bloğun ötesinde yarıya iner, 256'nın ötesinde hiç çizilmez.
        assertEquals(350, yuksek.tabanSpriteSayisi(5, 120));
        assertEquals(0, yuksek.tabanSpriteSayisi(5, 300));
        assertEquals(1400, KasirgaKaliteButcesi.icin(EfektKalitesi.ULTRA).tabanSpriteSayisi(5, 47));
    }

    @Test
    void gorselEnkaz160BlokOtesindeDurur() {
        KasirgaKaliteButcesi ultra = KasirgaKaliteButcesi.icin(EfektKalitesi.ULTRA);
        assertEquals(6, ultra.gorselEnkazSayisi(100));
        assertEquals(0, ultra.gorselEnkazSayisi(161));
    }

    @Test
    void altGirdapSayisiSeviyeVeKaliteninKucugudur() {
        assertEquals(0, KasirgaKaliteButcesi.icin(EfektKalitesi.ULTRA).altGirdapSayisi(3));
        assertEquals(2, KasirgaKaliteButcesi.icin(EfektKalitesi.YUKSEK).altGirdapSayisi(4));
        assertEquals(3, KasirgaKaliteButcesi.icin(EfektKalitesi.YUKSEK).altGirdapSayisi(5));
        assertEquals(4, KasirgaKaliteButcesi.icin(EfektKalitesi.ULTRA).altGirdapSayisi(5));
        assertEquals(0, KasirgaKaliteButcesi.icin(EfektKalitesi.DUSUK).altGirdapSayisi(5));
    }

    // --- Evreler: doğuş → olgun → ip → dağılma ---

    @Test
    void evreSirasiVeIlerlemesi() {
        int aktif = 200;
        assertEquals(KasirgaEvresi.DOGUS, KasirgaEvresi.hesapla(0, aktif).evre());
        assertEquals(0.5, KasirgaEvresi.hesapla(KasirgaEvresi.DOGUS_TICK / 2, aktif).ilerleme(), EPS);
        assertEquals(KasirgaEvresi.OLGUN, KasirgaEvresi.hesapla(KasirgaEvresi.DOGUS_TICK, aktif).evre());
        long ipBasi = KasirgaEvresi.DOGUS_TICK + aktif;
        assertEquals(KasirgaEvresi.IP, KasirgaEvresi.hesapla(ipBasi, aktif).evre());
        long dagilmaBasi = ipBasi + KasirgaEvresi.IP_TICK;
        assertEquals(KasirgaEvresi.DAGILMA, KasirgaEvresi.hesapla(dagilmaBasi, aktif).evre());
        assertFalse(KasirgaEvresi.hesapla(dagilmaBasi, aktif).bitti());
        assertTrue(KasirgaEvresi.hesapla(dagilmaBasi + KasirgaEvresi.DAGILMA_TICK, aktif).bitti());
    }

    @Test
    void sinirsizAktifSuredeOlgunEvredeKalir() {
        assertEquals(KasirgaEvresi.OLGUN,
                KasirgaEvresi.hesapla(10_000_000L, KasirgaEvresi.SINIRSIZ).evre());
    }
}
