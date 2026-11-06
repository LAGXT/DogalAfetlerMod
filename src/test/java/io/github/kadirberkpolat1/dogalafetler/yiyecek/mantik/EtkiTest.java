package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Etki büyüklükleri (madde 14: küçük %5–10, orta %15–25, büyük %30–40), anında/süreli bölünme, yan etki. */
class EtkiTest {
    private static final EtkiAyarlari A = EtkiAyarlari.VARSAYILAN;

    @Test
    void varsayilanKademelerMadde14Araliklarinda() {
        assertTrue(A.miktar(Buyukluk.KUCUK) >= 5 && A.miktar(Buyukluk.KUCUK) <= 10);
        assertTrue(A.miktar(Buyukluk.ORTA) >= 15 && A.miktar(Buyukluk.ORTA) <= 25);
        assertTrue(A.miktar(Buyukluk.BUYUK) >= 30 && A.miktar(Buyukluk.BUYUK) <= 40);
        assertTrue(A.miktar(Buyukluk.COK_BUYUK) > 40, "çok büyük büyüğün üstünde");
        assertTrue(A.miktar(Buyukluk.COK_KUCUK) > 0 && A.miktar(Buyukluk.COK_KUCUK) < 5, "vanilla: çok küçük");
        assertEquals(60, A.azalanVerimPenceresiSaniye());
        assertEquals(0.5, A.azalanVerimOrani(), 1e-12);
    }

    @Test
    void anlikVeSureliToplamKademeyiVerir() {
        for (YiyecekTablosu.Kayit k : YiyecekTablosu.KAYITLAR) {
            Etki e = Etki.hesapla(k.tanim(), A, 1.0, false);
            double beklenen = A.miktar(k.tanim().buyukluk()) * k.tanim().yon().isaret();
            assertEquals(beklenen, e.toplam(), 1e-9, k.ad());
            assertEquals(beklenen * A.anlikOran(), e.anlik(), 1e-9, k.ad());
            assertEquals(k.tanim().sureTick(), e.sureTick(), k.ad());
            assertTrue(Math.signum(e.anlik()) == Math.signum(e.sureli()), "aynı yön: " + k.ad());
        }
    }

    @Test
    void salepOrtaIsinmaSerinlikNegatif() {
        Etki salep = Etki.hesapla(YiyecekTablosu.al("sicak_salep").tanim(), A, 1.0, false);
        assertEquals(5.0, salep.anlik(), 1e-9);
        assertEquals(15.0, salep.sureli(), 1e-9);
        assertEquals(1800, salep.sureTick());
        Etki ayran = Etki.hesapla(YiyecekTablosu.al("ayran").tanim(), A, 1.0, false);
        assertEquals(-5.0, ayran.anlik(), 1e-9);
        assertEquals(-15.0, ayran.sureli(), 1e-9);
    }

    @Test
    void azalanVerimIkisiniDeYariyaIndirir() {
        YiyecekTanimi salep = YiyecekTablosu.al("sicak_salep").tanim();
        Etki tam = Etki.hesapla(salep, A, 1.0, false);
        Etki yari = Etki.hesapla(salep, A, 0.5, false);
        assertEquals(tam.anlik() / 2, yari.anlik(), 1e-9);
        assertEquals(tam.sureli() / 2, yari.sureli(), 1e-9);
        assertEquals(tam.sureTick(), yari.sureTick(), "süre kısalmaz, miktar azalır");
    }

    @Test
    void guvecYanEtkisiYalnizSicaktaVeYalnizGuvecte() {
        YiyecekTanimi guvec = YiyecekTablosu.al("atesli_guvec").tanim();
        YiyecekTanimi corba = YiyecekTablosu.al("sicak_tavuk_corbasi").tanim();
        Etki soguk = Etki.hesapla(guvec, A, 1.0, false);
        Etki sicak = Etki.hesapla(guvec, A, 1.0, true);
        assertEquals(soguk.toplam() * A.yanEtkiCarpani(), sicak.toplam(), 1e-9);
        assertEquals(Etki.hesapla(corba, A, 1.0, false).toplam(), Etki.hesapla(corba, A, 1.0, true).toplam(), 1e-9,
                "çorbada yan etki yok");
    }

    @Test
    void sicaklikEsigiVucutYaDaOrtamdan() {
        assertFalse(Etki.sicakMi(0, 0, A));
        assertFalse(Etki.sicakMi(19.9, 5, A));
        assertTrue(Etki.sicakMi(20, -40, A), "vücut sıcak");
        assertTrue(Etki.sicakMi(-10, 30, A), "ortam sıcak");
    }

    @Test
    void ayarlarDogrulanir() {
        assertThrows(IllegalArgumentException.class,
                () -> new EtkiAyarlari(3, 25, 20, 35, 45, 0.25, 60, 0.5, 1.5, 20), "sırasız kademeler");
        assertThrows(IllegalArgumentException.class,
                () -> new EtkiAyarlari(3, 8, 20, 35, 45, 1.5, 60, 0.5, 1.5, 20), "anında oranı > 1");
        assertThrows(IllegalArgumentException.class,
                () -> new EtkiAyarlari(3, 8, 20, 35, 45, 0.25, 60, 1.5, 1.5, 20), "azalan oran > 1");
        assertThrows(IllegalArgumentException.class,
                () -> new EtkiAyarlari(3, 8, 20, 35, 45, 0.25, 60, 0.5, 0.5, 20), "yan etki zayıflatamaz");
        assertThrows(IllegalArgumentException.class,
                () -> new EtkiAyarlari(3, 8, 20, 35, 145, 0.25, 60, 0.5, 1.5, 20), "ölçek dışı");
    }
}
