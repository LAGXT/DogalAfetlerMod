package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.afet.ortak.mantik.BlokRolu;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import org.junit.jupiter.api.Test;

class TsunamiSeviyesiTest {
    /** Madde 10.4: yükseklik, hız, karaya giriş, sürükleme ve yıkım gücü seviyeyle kesin artar. */
    @Test
    void herOlcuSeviyeyleArtar() {
        for (int s = 2; s <= 5; s++) {
            TsunamiSeviyesi a = TsunamiSeviyesi.al(s - 1);
            TsunamiSeviyesi b = TsunamiSeviyesi.al(s);
            assertTrue(b.yukseklik() > a.yukseklik() && b.hizBlokSn() > a.hizBlokSn() && b.karaGiris() > a.karaGiris()
                    && b.genislik() > a.genislik() && b.cekilme() > a.cekilme() && b.ivme() > a.ivme()
                    && b.hasar() > a.hasar() && b.sertlik() > a.sertlik() && b.yikimButcesi() > a.yikimButcesi(), "S" + s);
        }
        assertEquals(1, TsunamiSeviyesi.al(0).seviye());
        assertEquals(5, TsunamiSeviyesi.al(9).seviye());
    }

    /** Tasarım notu §2 "Seviye 1 ile 5 farkı": S1 4 blok / 30 blok içeri, S5 20 blok / 220 blok, 40 blok çekilme, ikinci dalga. */
    @Test
    void s1VeS5TasarimNotuylaAyni() {
        TsunamiSeviyesi s1 = TsunamiSeviyesi.al(1);
        TsunamiSeviyesi s5 = TsunamiSeviyesi.al(5);
        assertEquals(4, s1.yukseklik());
        assertEquals(30, s1.karaGiris());
        assertFalse(s1.ikinciDalga() || s1.tasYikar());
        assertEquals(20, s5.yukseklik());
        assertEquals(220, s5.karaGiris());
        assertEquals(40, s5.cekilme());
        assertTrue(s5.ikinciDalga() && s5.tasYikar());
        assertEquals(48, s5.yikimButcesi());
        assertEquals(0.4, s5.hizTick(), 1e-12);
    }

    /**
     * Rol karar tablosu (seviye × rol): dekor S1'den, kap S3'ten sertliğe bakılmadan yıkılır; DIGER hiçbir seviyede rolle
     * yıkılmaz (sınıf tablosuna kalır). Kap eşiği sınır dışı seviyede de tablonun ucundan okunur.
     */
    @Test
    void rolKarariSeviyeTablosu() {
        boolean[][] beklenen = {
            // DEKOR, KAP, DIGER
            {true, false, false},
            {true, false, false},
            {true, true, false},
            {true, true, false},
            {true, true, false},
        };
        for (int s = 1; s <= 5; s++) {
            TsunamiSeviyesi sv = TsunamiSeviyesi.al(s);
            for (BlokRolu rol : BlokRolu.values()) {
                assertEquals(beklenen[s - 1][rol.ordinal()], sv.rolYikar(rol), "S" + s + " " + rol);
            }
            assertEquals(s >= 3, sv.kapYikar(), "S" + s);
        }
        assertFalse(TsunamiSeviyesi.al(0).rolYikar(BlokRolu.KAP));
        assertTrue(TsunamiSeviyesi.al(9).rolYikar(BlokRolu.KAP));
    }

    /** Sığlaşma: açıkta pay {@code ACIK_PAY}, kıyıda tam yükseklik, aradaki yolda monoton artar. */
    @Test
    void siglasmaKiyiyaYaklastikcaArtar() {
        assertEquals(TsunamiSeviyesi.ACIK_PAY, TsunamiSeviyesi.siglasma(1), 1e-12);
        assertEquals(1, TsunamiSeviyesi.siglasma(0), 1e-12);
        assertEquals(1, TsunamiSeviyesi.siglasma(-3), 1e-12);
        double onceki = 0;
        for (double k = 1; k >= 0; k -= 0.05) {
            double h = TsunamiSeviyesi.siglasma(k);
            assertTrue(h >= onceki, "kalan " + k);
            onceki = h;
        }
    }

    /** K-E67: açıklık dalgayı doğuştan 8 sn'de kıyıya getirir (32–64 blok); ilk dalga açıkta alçak doğar. */
    @Test
    void dalgaKiyiyaSekizSaniyedeVarir() {
        for (int s = 1; s <= 5; s++) {
            TsunamiSeviyesi sv = TsunamiSeviyesi.al(s);
            double sure = sv.aciklik() / sv.hizTick() / 20;
            assertTrue(sv.aciklik() >= 32 && sv.aciklik() <= 64, "S" + s + " " + sv.aciklik());
            assertTrue(sure <= TsunamiSeviyesi.KIYIYA_SN + 1e-9, "S" + s + " " + sure);
            assertTrue(sv.aciklik() > sv.cekilme(), "dalga çekilen şeridin dışında doğar: S" + s);
        }
    }

    /** Geometri: yön denizin tersine, boyuna/yanal ile x/z birbirinin tersi; dalga kıyıdan açıklık kadar açıkta doğar. */
    @Test
    void geometriDenizdenKarayaVeTersiTutarli() {
        double[] yon = TsunamiGeometrisi.yonHesapla(-300, 10, 100);
        assertTrue(yon[0] > 0.99, "deniz batıda: dalga doğuya gider");
        assertEquals(1, TsunamiGeometrisi.yonHesapla(0.2, -0.3, 100)[0], 1e-12);
        TsunamiGeometrisi g = new TsunamiGeometrisi(100, -40, 3, 4, 62, 900);
        assertEquals(0.6, g.yonX(), 1e-12);
        for (double b = -50; b <= 50; b += 12.5) {
            for (double y = -30; y <= 30; y += 7.5) {
                assertEquals(b, g.boyuna(g.x(b, y), g.z(b, y)), 1e-9);
                assertEquals(y, g.yanal(g.x(b, y), g.z(b, y)), 1e-9);
            }
        }
        TsunamiSeviyesi sv = TsunamiSeviyesi.al(3);
        Dalga d = new Dalga(g.x(-sv.aciklik(), 0), g.z(-sv.aciklik(), 0), g.yonX(), g.yonZ(), 0, sv.hizTick(), sv.yukseklik(),
                sv.genislik(), sv.uzunluk(), 62, sv.aciklik() + sv.karaGiris(), sv.ivme(), sv.hasar());
        double kiyiya = sv.aciklik() / sv.hizTick();
        assertEquals(0, d.cepheyeUzaklik(g.kiyiX(), g.kiyiZ(), kiyiya), 1e-9);
        assertTrue(Double.isNaN(d.yuzey(g.x(5, 0), g.z(5, 0), kiyiya)), "cephe önünde su var");
        assertTrue(d.yuzey(g.x(-2, 0), g.z(-2, 0), kiyiya) > 62, "cephe arkasında su yok");
    }
}
