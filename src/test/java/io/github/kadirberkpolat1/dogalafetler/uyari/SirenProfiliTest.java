package io.github.kadirberkpolat1.dogalafetler.uyari;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import org.junit.jupiter.api.Test;

class SirenProfiliTest {
    @Test
    void herSirenTurununProfiliVeDosyasiVar() {
        assertEquals(SirenProfili.STANDART, SirenProfili.icin(SirenTuru.STANDART));
        assertEquals(SirenProfili.GUNES, SirenProfili.icin(SirenTuru.GUNES));
        assertEquals("siren_standart", SirenProfili.STANDART.sesAdi());
        assertEquals("siren_gunes", SirenProfili.GUNES.sesAdi());
        assertTrue(SirenProfili.STANDART.akisla() == false && SirenProfili.GUNES.akisla(),
                "10 sn'den uzun döngü akışla (ses.md §5)");
    }

    /**
     * Ay çarpması 25 sn = standart 10 sn dosyasının 2,5 turu, yukarı 3 (K0-19 rev. K-E67); güneş 25 sn = 20 sn dosyanın
     * 1,25 turu, yukarı 2.
     */
    @Test
    void donguSayisiSureyiDosyaBoyunaBoler() {
        SirenProfili ay = SirenProfili.icin(SirenTuru.afetIcin(AfetTuru.AY_CARPMASI));
        assertEquals(SirenProfili.STANDART, ay);
        assertEquals(3, ay.donguSayisi(SirenTuru.sureTick(AfetTuru.AY_CARPMASI)));
        assertEquals(1, SirenProfili.STANDART.donguSayisi(SirenTuru.sureTick(AfetTuru.DEPREM)));
        assertEquals(2, SirenProfili.GUNES.donguSayisi(SirenTuru.sureTick(AfetTuru.GUNES_PATLAMASI)));
        assertTrue(SirenProfili.GUNES.acilisTick() + SirenProfili.GUNES.sondurmeTick()
                < SirenTuru.sureTick(AfetTuru.GUNES_PATLAMASI) / 2, "açılış ve söndürme kısa sirenin yarısından kısa");
        assertEquals(1, SirenProfili.STANDART.donguSayisi(1), "kısa kalan süre de bir tur");
        assertEquals(2, SirenProfili.STANDART.donguSayisi(201), "yarım tur yukarı yuvarlanır");
    }

    @Test
    void sirenDuzeyiZarfCarpiSirenSesYuzdesidir() {
        assertEquals(0.28f, SesDuzeyi.siren(1f, 28), 1e-6f, "varsayılan %28");
        assertEquals(0.14f, SesDuzeyi.siren(0.5f, 28), 1e-6f);
        assertEquals(0f, SesDuzeyi.siren(1f, 0), 0f, "0 = kapalı");
        assertEquals(0.75f, SesDuzeyi.siren(0.75f, 100), 1e-6f);
        assertEquals(1f, SesDuzeyi.siren(2f, 100), 0f, "üst sınır 1");
        assertEquals(1f, SesDuzeyi.siren(1f, 150), 0f, "bozuk config de 1'i aşmaz");
        assertEquals(0f, SesDuzeyi.siren(-1f, 28), 0f);
    }

    /** S-02 / 1 Hz geç gelen istek: aynı afet zaten çalıyorsa yok sayılır, çalmıyorsa kalan süreyle başlar. */
    @Test
    void tekrarlananIstekAyniAfetCalarkenYokSayilir() {
        assertEquals(SirenKarari.BASLAT, SirenKarari.istek(false, false, 0L, 1234L));
        assertEquals(SirenKarari.YOK_SAY, SirenKarari.istek(true, false, 1234L, 1234L));
        assertEquals(SirenKarari.KES_VE_BASLAT, SirenKarari.istek(true, false, 1000L, 1234L));
        assertEquals(SirenKarari.BASLAT, SirenKarari.istek(false, false, 1234L, 1234L), "bitmiş siren yeniden başlar");
        assertEquals(SirenKarari.BASLAT, SirenKarari.istek(true, true, 1234L, 1234L),
                "sönen siren aynı saatte başlamış yeni afetin sirenini engellemez");
        assertEquals(SirenKarari.BASLAT, SirenKarari.istek(true, true, 1000L, 1234L), "söneni kesmeye gerek yok");
    }

    @Test
    void durdurmaYalnizCalanAfetinNumarasiylaUygulanir() {
        assertTrue(SirenKarari.durdurUygulanir(true, 1234L, 1234L));
        assertFalse(SirenKarari.durdurUygulanir(true, 1234L, 999L), "eski afetin geç kalan durdurması yeni sireni söndürmez");
        assertFalse(SirenKarari.durdurUygulanir(false, 1234L, 1234L));
    }
}
