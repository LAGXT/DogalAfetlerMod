package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari.Donusum;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari.Seviye;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari.Sinif;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari.Tablo;
import org.junit.jupiter.api.Test;

class AsitKurallariTest {
    @Test
    void seviyeArttikcaAlanSureAsinmaVeHasarArtar() {
        Seviye onceki = null;
        for (int s = 1; s <= 5; s++) {
            Seviye v = Tablo.VARSAYILAN.seviye(s);
            if (onceki != null) {
                assertTrue(v.yaricap() > onceki.yaricap(), "yarıçap S" + s);
                assertTrue(v.sureTick() > onceki.sureTick(), "süre S" + s);
                assertTrue(v.asindirmaSaniye() > onceki.asindirmaSaniye(), "aşınma S" + s);
                assertTrue(v.hasar() / v.hasarAraligi() >= onceki.hasar() / onceki.hasarAraligi(), "hasar hızı S" + s);
            }
            onceki = v;
        }
        assertEquals(new Seviye(32, 1200, 2.0, 0.5f, 80, false), Tablo.VARSAYILAN.seviye(1));
        assertEquals(new Seviye(128, 4800, 16.0, 1.0f, 40, true), Tablo.VARSAYILAN.seviye(5));
        assertEquals(Tablo.VARSAYILAN.seviye(1), Tablo.VARSAYILAN.seviye(0), "alt sınır");
        assertEquals(Tablo.VARSAYILAN.seviye(5), Tablo.VARSAYILAN.seviye(9), "üst sınır");
    }

    @Test
    void seviyeBesteBileOldurmez() {
        Seviye s5 = Tablo.VARSAYILAN.seviye(5);
        float can = 20f;
        for (int i = 0; i < 10_000; i++) {
            can -= AsitKurallari.hasarMiktari(can, s5.hasar(), 4f);
        }
        assertEquals(4f, can, 1e-6, "can 4'te durur");
        assertEquals(0f, AsitKurallari.hasarMiktari(4f, 1f, 4f));
        assertEquals(0.5f, AsitKurallari.hasarMiktari(4.5f, 1f, 4f));
        assertEquals(0f, AsitKurallari.hasarMiktari(3f, 1f, 4f), "zaten altındaysa hasar yok");
    }

    @Test
    void yogunlukGiristeYukselirSondaSeyrelir() {
        long sure = 1200;
        assertEquals(0, AsitKurallari.yogunluk(0, sure));
        assertEquals(0.5, AsitKurallari.yogunluk(100, sure), 1e-9);
        assertEquals(1, AsitKurallari.yogunluk(600, sure), 1e-9);
        assertEquals(0.5, AsitKurallari.yogunluk(sure - 150, sure), 1e-9);
        assertEquals(0, AsitKurallari.yogunluk(sure, sure));
        assertEquals(0, AsitKurallari.yogunluk(-5, sure));
    }

    @Test
    void dayanikliEtiketTablonunOnundedir() {
        for (Sinif s : Sinif.values()) {
            assertEquals(Donusum.YOK, AsitKurallari.donusum(s, true, true), s.name());
        }
        assertEquals(Donusum.YOK_OLUR, AsitKurallari.donusum(Sinif.YAPRAK, false, false));
        assertEquals(Donusum.YOK_OLUR, AsitKurallari.donusum(Sinif.AHSAP, false, false));
        assertEquals(Donusum.OLU_CALI, AsitKurallari.donusum(Sinif.OT, false, false));
        assertEquals(Donusum.TOPRAK, AsitKurallari.donusum(Sinif.CIMEN_BLOGU, false, false));
        assertEquals(Donusum.KABA_TOPRAK, AsitKurallari.donusum(Sinif.TOPRAK, false, false));
        assertEquals(Donusum.CAKIL, AsitKurallari.donusum(Sinif.KALSIT, false, false));
        assertEquals(Donusum.OKSITLEN, AsitKurallari.donusum(Sinif.BAKIR, false, false));
        assertEquals(Donusum.YOK, AsitKurallari.donusum(Sinif.DIGER, true, false), "tanınmayan blok bozulmaz");
    }

    @Test
    void tasYalnizUcuncuSeviyedenItibarenAsinir() {
        assertFalse(Tablo.VARSAYILAN.seviye(2).tasAsinir());
        assertTrue(Tablo.VARSAYILAN.seviye(3).tasAsinir());
        assertEquals(Donusum.YOK, AsitKurallari.donusum(Sinif.TAS_TUGLA, false, false));
        assertEquals(Donusum.CATLAK_TAS_TUGLA, AsitKurallari.donusum(Sinif.TAS_TUGLA, true, false));
        assertEquals(Donusum.YOSUNLU_KIRIKTAS, AsitKurallari.donusum(Sinif.KIRIKTAS, true, false));
    }

    @Test
    void asinmaHiziSaniyedeBelirtilenBlokuGecmez() {
        double birikim = 0;
        int donusen = 0;
        for (int tick = 0; tick < 20 * 60; tick++) {
            birikim = AsitKurallari.birikimEkle(birikim, 7.0, 1.0, 1.0, 1);
            while (birikim >= 1) {
                birikim -= 1;
                donusen++;
            }
        }
        assertEquals(7 * 60, donusen, 1, "S3: dakikada 420 blok");
        assertEquals(2.0, AsitKurallari.birikimEkle(1.9, 16, 1, 1, 1000), "birikim tavanı 2");
        assertEquals(0.0, AsitKurallari.birikimEkle(0, 16, 1, 0.5, 0), "duraklatılmış saat ilerletmez");
        assertEquals(0.4, AsitKurallari.birikimEkle(0, 16, 1, 0.5, 1), 1e-9, "TPS kısması yarıya indirir");
    }

    @Test
    void ornekSayisiUcIleOnAltiArasindaVeKismaylaAzalir() {
        assertEquals(3, AsitKurallari.ornekSayisi(2, 1));
        assertEquals(16, AsitKurallari.ornekSayisi(16, 1));
        assertEquals(8, AsitKurallari.ornekSayisi(16, 0.5));
        assertEquals(1, AsitKurallari.ornekSayisi(2, 0.25));
    }

    @Test
    void catiAltiAciktaDegildir() {
        assertTrue(AsitKurallari.acikta(65, 66.62), "açık arazi: harita ayak hizasında");
        assertFalse(AsitKurallari.acikta(71, 66.62), "çatı 70'te");
        assertTrue(AsitKurallari.acikta(66, 66.62), "göz bloğu hizası");
        assertFalse(AsitKurallari.acikta(67, 66.62), "başın hemen üstünde blok");
    }

    @Test
    void catlakAsamasiSifirdanDokuzaIlerler() {
        assertEquals(0, AsitKurallari.catlakAsamasi(0, 80));
        assertEquals(5, AsitKurallari.catlakAsamasi(40, 80));
        assertEquals(9, AsitKurallari.catlakAsamasi(79, 80));
        assertEquals(9, AsitKurallari.catlakAsamasi(500, 80));
    }

    @Test
    void alanVeSisAgirligiKenardaYumusakSoner() {
        assertEquals(1, AsitKurallari.alanAgirligi(0, 64), 1e-9);
        assertEquals(1, AsitKurallari.alanAgirligi(48, 64), 1e-9);
        assertEquals(0.5, AsitKurallari.alanAgirligi(56, 64), 1e-9);
        assertEquals(0, AsitKurallari.alanAgirligi(64, 64), 1e-9);
        assertEquals(1, AsitKurallari.sisAgirligi(64, 64), 1e-9);
        assertEquals(0.5, AsitKurallari.sisAgirligi(80, 64), 1e-9);
        assertEquals(0, AsitKurallari.sisAgirligi(96, 64), 1e-9);
    }

    @Test
    void gorselYogunlukSeviyeyleArtar() {
        assertEquals(0.4, AsitKurallari.gorselCarpan(1), 1e-9, "S1 seyrek çise");
        assertEquals(1.0, AsitKurallari.gorselCarpan(5), 1e-9, "S5 tam sağanak");
        assertEquals(1.0, AsitKurallari.gorselCarpan(8), 1e-9);
        for (int s = 2; s <= 5; s++) {
            assertTrue(AsitKurallari.gorselCarpan(s) > AsitKurallari.gorselCarpan(s - 1));
        }
    }

    @Test
    void ignedenOnceYapraklilarDahaYavas() {
        assertEquals(1.0, AsitKurallari.kabulOlasiligi(Sinif.IGNE_YAPRAK));
        assertTrue(AsitKurallari.kabulOlasiligi(Sinif.YAPRAK) < 1.0);
    }
}
