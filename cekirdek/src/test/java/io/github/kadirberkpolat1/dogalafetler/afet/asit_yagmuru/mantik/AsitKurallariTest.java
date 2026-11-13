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
    void seviyeArttikcaAlanSureVeAsinmaArtar() {
        Seviye onceki = null;
        for (int s = 1; s <= 5; s++) {
            Seviye v = Tablo.VARSAYILAN.seviye(s);
            if (onceki != null) {
                assertTrue(v.yaricap() > onceki.yaricap(), "yarıçap S" + s);
                assertTrue(v.sureTick() > onceki.sureTick(), "süre S" + s);
                assertTrue(v.asindirmaSaniye() > onceki.asindirmaSaniye(), "aşınma S" + s);
            }
            onceki = v;
        }
        assertEquals(new Seviye(32, 1200, 2.0, false), Tablo.VARSAYILAN.seviye(1));
        assertEquals(new Seviye(192, 6000, 26.0, true), Tablo.VARSAYILAN.seviye(5));
        assertEquals(Tablo.VARSAYILAN.seviye(1), Tablo.VARSAYILAN.seviye(0), "alt sınır");
        assertEquals(Tablo.VARSAYILAN.seviye(5), Tablo.VARSAYILAN.seviye(9), "üst sınır");
    }

    @Test
    void seviyeBesteBileOldurmez() {
        float can = 20f;
        for (int i = 0; i < 10_000; i++) {
            can -= AsitKurallari.hasarMiktari(can, 1f, 4f);
        }
        assertEquals(4f, can, 1e-6, "can 4'te durur");
        assertEquals(0f, AsitKurallari.hasarMiktari(4f, 1f, 4f));
        assertEquals(0.5f, AsitKurallari.hasarMiktari(4.5f, 1f, 4f));
        assertEquals(0f, AsitKurallari.hasarMiktari(3f, 1f, 4f), "zaten altındaysa hasar yok");
    }

    /** Kullanıcı isteği (18): açıkta saniyede bir hasar; 4 tick'lik denetimlerle 20 tick'te tam bir kez. */
    @Test
    void maruziyetSaniyedeBirHasarVerir() {
        int sayac = 0;
        int hasar = 0;
        for (int tick = 4; tick <= 20 * 60; tick += 4) {
            AsitKurallari.Maruziyet m = AsitKurallari.maruziyet(sayac, 4);
            sayac = m.sayac();
            if (m.hasar()) {
                hasar++;
                assertEquals(0, tick % 20, "hasar yalnız saniye sınırında: " + tick);
            }
        }
        assertEquals(60, hasar, "60 sn açıkta 60 hasar");
        float can = 20f;
        for (int i = 0; i < 20; i++) {
            can -= AsitKurallari.hasarMiktari(can, 1f, 0f);
        }
        assertEquals(0f, can, 1e-6, "asgari can 0'da 1 HP/sn ile 20 can 20 sn'de biter");
    }

    @Test
    void maruziyetHizdaYaDaGecikmedePatlamaz() {
        AsitKurallari.Maruziyet m = AsitKurallari.maruziyet(0, 400);
        assertTrue(m.hasar());
        assertEquals(AsitKurallari.SANIYE_TICK, m.sayac(), "artık birikim bir saniyeyle sınırlı");
        assertTrue(AsitKurallari.maruziyet(m.sayac(), 0).hasar(), "sınırlı birikim sonraki denetimde bir hasar daha");
        assertFalse(AsitKurallari.maruziyet(0, 19).hasar());
        assertEquals(0, AsitKurallari.maruziyet(-5, 0).sayac(), "negatif sayaç sıfırlanır");
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
            birikim = AsitKurallari.birikimEkle(birikim, 7.0, 1, 1.0, 1.0, 1);
            while (birikim >= 1) {
                birikim -= 1;
                donusen++;
            }
        }
        assertEquals(7 * 60, donusen, 1, "S3: dakikada 420 blok");
        assertEquals(2.0, AsitKurallari.birikimEkle(1.9, 16, 1, 1, 1, 1000), "birikim tavanı 2");
        assertEquals(0.0, AsitKurallari.birikimEkle(0, 16, 1, 1, 0.5, 0), "duraklatılmış saat ilerletmez");
        assertEquals(0.4, AsitKurallari.birikimEkle(0, 16, 1, 1, 0.5, 1), 1e-9, "TPS kısması yarıya indirir");
    }

    /** AF2-05: her oyuncunun çevresi aynı hızla aşınır; tavan oyuncu başına 2, oyuncusuz dünyada tek oyuncu sayılır. */
    @Test
    void asinmaHiziOyuncuSayisiylaOlceklenir() {
        assertEquals(1.6, AsitKurallari.birikimEkle(0, 16, 2, 1, 1, 1), 1e-9, "iki oyuncu: tick başına 2 × 0,8");
        assertEquals(6.0, AsitKurallari.birikimEkle(5.9, 16, 3, 1, 1, 1000), "üç oyuncuda tavan 6");
        assertEquals(0.8, AsitKurallari.birikimEkle(0, 16, 0, 1, 1, 1), 1e-9, "oyuncu 0 → tek oyuncu hızı");
    }

    @Test
    void ornekSayisiUcIleOnAltiArasindaVeKismaylaAzalir() {
        assertEquals(3, AsitKurallari.ornekSayisi(2, 1));
        assertEquals(16, AsitKurallari.ornekSayisi(16, 1));
        assertEquals(8, AsitKurallari.ornekSayisi(16, 0.5));
        assertEquals(1, AsitKurallari.ornekSayisi(2, 0.25));
    }

    /** AF2-05: örnekleme oyuncu başına, toplamı {@code blokButcesi} × TPS kısması ile sınırlı; oyuncusuz dünyada 0. */
    @Test
    void ornekButcesiOyuncuBasinaVeTavanli() {
        assertEquals(0, AsitKurallari.ornekButcesi(16, 1, 0, 32), "oyuncu yoksa örnek yok");
        assertEquals(16, AsitKurallari.ornekButcesi(16, 1, 1, 32));
        assertEquals(32, AsitKurallari.ornekButcesi(16, 1, 10, 32), "on oyuncu: bütçe tavanı");
        assertEquals(9, AsitKurallari.ornekButcesi(2, 1, 3, 32), "S1 üç oyuncu: 3 × 3");
        assertEquals(8, AsitKurallari.ornekButcesi(16, 0.25, 10, 32), "TPS kısması bütçeyi çeyreğe indirir");
        assertEquals(1, AsitKurallari.ornekButcesi(16, 0, 10, 32), "en az 1");
        assertEquals(0, AsitKurallari.ornekButcesi(16, 1, 4, 0), "bütçe 0: aşınma kapalı");
    }

    /** AF2-02: vanilla yağmuru kesilince, gök gürültüsü başlayınca ya da süresi azalınca yeniden açılır. */
    @Test
    void vanillaYagmuruAsitBoyuncaTutulur() {
        assertFalse(AsitKurallari.yagmurYenilenmeli(true, false, 1000, 0), "yağıyor, süre bol");
        assertTrue(AsitKurallari.yagmurYenilenmeli(false, false, 1000, 0), "vanilla yağmuru kesti");
        assertTrue(AsitKurallari.yagmurYenilenmeli(true, true, 1000, 0), "gök gürültüsü yok");
        assertTrue(AsitKurallari.yagmurYenilenmeli(true, false, 1000, 6000), "/weather clear sayacı kurdu");
        assertTrue(AsitKurallari.yagmurYenilenmeli(true, false, AsitKurallari.YAGMUR_YENILEME_TICK - 1, 0), "süre bitiyor");
        assertFalse(AsitKurallari.yagmurYenilenmeli(true, false, AsitKurallari.YAGMUR_YENILEME_TICK, 0));
        assertFalse(AsitKurallari.yagmurKesildi(true, false, 0), "süre tazelemesi kesilme sayılmaz");
        assertTrue(AsitKurallari.yagmurKesildi(true, false, 6000), "/weather clear kesmedir");
        assertTrue(AsitKurallari.yagmurKesildi(false, false, 0));
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
    void gorselYogunlukSeviyeyleArtar() {
        assertEquals(0.25, AsitKurallari.gorselCarpan(1), 1e-9, "S1 seyrek çise (AOi-2)");
        assertEquals(0.55, AsitKurallari.gorselCarpan(2), 1e-9, "S2+ doğrusal dizi korunur");
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
