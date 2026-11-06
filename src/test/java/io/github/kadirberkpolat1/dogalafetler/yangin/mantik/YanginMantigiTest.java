package io.github.kadirberkpolat1.dogalafetler.yangin.mantik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.BitisNedeni;
import org.junit.jupiter.api.Test;

class YanginMantigiTest {
    @Test
    void butceKismaCarpaniylaDuser() {
        assertEquals(64, YanginMantigi.butce(64, 1.0));
        assertEquals(32, YanginMantigi.butce(64, 0.5));
        assertEquals(16, YanginMantigi.butce(64, 0.25));
        assertEquals(1, YanginMantigi.butce(2, 0.25), "kısma bütçeyi sıfırlamaz");
        assertEquals(0, YanginMantigi.butce(0, 1.0));
    }

    @Test
    void tavanAsilincaEnEskilerSoner() {
        assertEquals(0, YanginMantigi.tasanSayisi(1500, 1500));
        assertEquals(7, YanginMantigi.tasanSayisi(1507, 1500));
        assertEquals(0, YanginMantigi.tasanSayisi(3, 1500));
    }

    @Test
    void yanmazBlokAslaTutusmaz() {
        assertEquals(0, YanginMantigi.tutusmaOlasiligi(0, 10, 2.5, 1.6, 1));
        assertEquals(0, YanginMantigi.tutusmaOlasiligi(60, 0, 1, 1, 1), "hız çarpanı 0 yayılmayı durdurur");
    }

    @Test
    void olasilikVanillaYayilmaHizindanGelir() {
        double cimen = YanginMantigi.tutusmaOlasiligi(60, 1, 1, 1, 1);
        double yaprak = YanginMantigi.tutusmaOlasiligi(30, 1, 1, 1, 1);
        double odun = YanginMantigi.tutusmaOlasiligi(5, 1, 1, 1, 1);
        assertEquals(0.6, cimen, 1e-9);
        assertTrue(cimen > yaprak && yaprak > odun);
        assertEquals(1.0, YanginMantigi.tutusmaOlasiligi(60, 5, 2, 1.6, 1), "olasılık 1'i aşmaz");
    }

    @Test
    void ruzgarEgimVeNemKatsayilari() {
        double ruzgarYonunde = YanginMantigi.ruzgarKatsayisi(0.6, 0, 1, 0);
        double karsi = YanginMantigi.ruzgarKatsayisi(0.6, 0, -1, 0);
        double yan = YanginMantigi.ruzgarKatsayisi(0.6, 0, 0, 1);
        assertEquals(1.6, ruzgarYonunde, 1e-9);
        assertEquals(0.4, karsi, 1e-9);
        assertEquals(1.0, yan, 1e-9);
        assertEquals(1.0, YanginMantigi.ruzgarKatsayisi(0, 0, 1, 0), "rüzgârsızken etkisiz");
        assertEquals(1.0, YanginMantigi.ruzgarKatsayisi(0.6, 0, 0, 0), "dikey yayılmada etkisiz");
        assertTrue(YanginMantigi.egimKatsayisi(1) > YanginMantigi.egimKatsayisi(0));
        assertTrue(YanginMantigi.egimKatsayisi(0) > YanginMantigi.egimKatsayisi(-1));
        assertEquals(1.0, YanginMantigi.nemKatsayisi(0), 1e-9);
        assertEquals(0.3, YanginMantigi.nemKatsayisi(1), 1e-9);
        assertEquals(0.3, YanginMantigi.nemKatsayisi(5), 1e-9);
    }

    @Test
    void yaprakOduncanCabukYanar() {
        int yaprak = YanginMantigi.yanmaSuresiTick(60);
        int odun = YanginMantigi.yanmaSuresiTick(5);
        int cimen = YanginMantigi.yanmaSuresiTick(100);
        assertTrue(cimen < yaprak && yaprak < odun, cimen + " < " + yaprak + " < " + odun);
        assertEquals(180, yaprak);
        assertEquals(444, odun);
    }

    @Test
    void yagmurdaSonmeYaslaArtar() {
        assertEquals(0.25, YanginMantigi.yagmurSonmeOlasiligi(0.25, 0, false), 1e-9);
        assertEquals(0.55, YanginMantigi.yagmurSonmeOlasiligi(0.25, 1, false), 1e-9);
        assertEquals(0.55, YanginMantigi.yagmurSonmeOlasiligi(0.25, 3, false), 1e-9);
        assertEquals(1.0, YanginMantigi.yagmurSonmeOlasiligi(0.9, 1, false), 1e-9);
        assertEquals(0.0, YanginMantigi.yagmurSonmeOlasiligi(0, 0, false), 1e-9);
    }

    @Test
    void korunakliAtesYagmurdaDahaYavasSoner() {
        double acik = YanginMantigi.yagmurSonmeOlasiligi(0.25, 0.5, false);
        double korunakli = YanginMantigi.yagmurSonmeOlasiligi(0.25, 0.5, true);
        assertEquals(0.40, acik, 1e-9);
        assertEquals(0.12, korunakli, 1e-9);
        assertTrue(korunakli > 0, "yaprak altındaki ateş de sonunda söner");
        assertEquals(0.0, YanginMantigi.yagmurSonmeOlasiligi(0, 0, true), 1e-9);
    }

    @Test
    void blokHasariKapaliykenYakitDegismez() {
        assertEquals(YanginMantigi.YanmaSonucu.HAVA, YanginMantigi.yanmaSonucu(true));
        assertEquals(YanginMantigi.YanmaSonucu.DEGISMEZ, YanginMantigi.yanmaSonucu(false));
    }

    @Test
    void oncelikYakinOnceUzakSonundaIslenir() {
        assertTrue(YanginMantigi.oncelik(4 * 4, 0) < YanginMantigi.oncelik(100 * 100, 0));
        assertTrue(YanginMantigi.oncelik(100 * 100, 2000) < YanginMantigi.oncelik(10 * 10, 0),
                "uzun bekleyen uzak hücre öne geçer");
    }

    @Test
    void seciciEnKucukleriSiraylaVerir() {
        double[] puan = {5, 1, 9, 1, 3, 7};
        assertArrayEquals(new int[] {1, 3, 4}, OncelikSecici.enIyiler(puan, puan.length, 3));
        assertArrayEquals(new int[] {1, 3, 4, 0, 5, 2}, OncelikSecici.enIyiler(puan, puan.length, 10));
        assertArrayEquals(new int[] {1}, OncelikSecici.enIyiler(puan, 2, 1), "yalnız ilk n girdi");
        assertEquals(0, OncelikSecici.enIyiler(puan, puan.length, 0).length);
    }

    @Test
    void kivilcimVeRuzgar() {
        assertEquals(1, YanginMantigi.kivilcimYaricapi(0));
        assertEquals(2, YanginMantigi.kivilcimYaricapi(0.5));
        assertEquals(3, YanginMantigi.kivilcimYaricapi(1));
        assertArrayEquals(YanginMantigi.gununRuzgari(7), YanginMantigi.gununRuzgari(7));
        double[] r = YanginMantigi.gununRuzgari(3);
        assertEquals(0.6, Math.hypot(r[0], r[1]), 1e-9);
    }

    @Test
    void yogunlukSeviyeTablosunuVerir() {
        assertEquals(0.5, YanginMantigi.yayilmaCarpani(0), 1e-9);
        assertEquals(1.8, YanginMantigi.yayilmaCarpani(1), 1e-9);
        assertEquals(1.8, YanginMantigi.yayilmaCarpani(3), 1e-9, "yoğunluk 0–1'e sıkıştırılır");
        assertEquals(0, YanginMantigi.sicramaMesafesi(0));
        assertEquals(2, YanginMantigi.sicramaMesafesi(0.2));
        assertEquals(5, YanginMantigi.sicramaMesafesi(0.4));
        assertEquals(8, YanginMantigi.sicramaMesafesi(0.7));
        assertEquals(12, YanginMantigi.sicramaMesafesi(1));
        assertEquals(0.3, YanginMantigi.alanOrani(0), 1e-9, "S1 başlangıç alanı da tutuşur");
        assertEquals(1.0, YanginMantigi.alanOrani(1), 1e-9);
    }

    @Test
    void sicramaRuzgarAltinaDuser() {
        assertArrayEquals(new int[] {5, 0}, YanginMantigi.sicramaKaymasi(0.6, 0, 0, 5));
        assertArrayEquals(new int[] {0, 8}, YanginMantigi.sicramaKaymasi(0, 0.6, 0, 8));
        int[] sapan = YanginMantigi.sicramaKaymasi(0.6, 0, 20, 12);
        assertEquals(11, sapan[0]);
        assertEquals(4, sapan[1]);
        assertArrayEquals(new int[] {3, 0}, YanginMantigi.sicramaKaymasi(0, 0, 0, 3), "rüzgârsızken doğrultu yalnız sapma");
    }

    @Test
    void yogunlukOlasiligiCarpar() {
        double s1 = YanginMantigi.tutusmaOlasiligi(30, YanginMantigi.yayilmaCarpani(0), 1, 1, 1);
        double s5 = YanginMantigi.tutusmaOlasiligi(30, YanginMantigi.yayilmaCarpani(1), 1, 1, 1);
        assertEquals(0.15, s1, 1e-9);
        assertEquals(0.54, s5, 1e-9);
    }

    @Test
    void yagmurYalnizGokyuzuneYakinAtesiEtkiler() {
        assertEquals(YanginMantigi.YagisDurumu.YOK, YanginMantigi.yagisDurumu(false, true, true, 0), "yağmur yoksa etkisiz");
        assertEquals(YanginMantigi.YagisDurumu.ACIK, YanginMantigi.yagisDurumu(true, true, true, 0));
        assertEquals(YanginMantigi.YagisDurumu.KORUNAKLI, YanginMantigi.yagisDurumu(true, false, true, 0),
                "yalnız yaprak örtüsü altında");
        assertEquals(YanginMantigi.YagisDurumu.KORUNAKLI, YanginMantigi.yagisDurumu(true, false, true,
                YanginMantigi.KORUNAKLI_ORTU), "8 blok içinde çatı");
        assertEquals(YanginMantigi.YagisDurumu.YOK, YanginMantigi.yagisDurumu(true, false, true,
                YanginMantigi.KORUNAKLI_ORTU + 1), "yeraltı yağmurdan etkilenmez");
        assertEquals(YanginMantigi.YagisDurumu.YOK, YanginMantigi.yagisDurumu(true, false, false, 2),
                "yağışsız ya da karlı biyom");
    }

    @Test
    void alanOkumaButcesiKismayaUyar() {
        assertEquals(4096, YanginMantigi.alanOkumaButcesi(64));
        assertEquals(1024, YanginMantigi.alanOkumaButcesi(16));
        assertEquals(64, YanginMantigi.alanOkumaButcesi(1));
    }

    @Test
    void kureTarayiciHerKonumuBirKezVerirVeButceyeUyar() {
        int r = 24;
        int beklenen = 0;
        for (int x = -r; x <= r; x++) {
            for (int y = -r; y <= r; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + y * y + z * z <= r * r) {
                        beklenen++;
                    }
                }
            }
        }
        java.util.Set<Long> gorulen = new java.util.HashSet<>();
        KureTarayici tarayici = new KureTarayici(r);
        int cagri = 0;
        while (!tarayici.bitti()) {
            int harcanan = tarayici.ilerle(4096, (dx, dy, dz) -> {
                assertTrue(gorulen.add(((long) dx + 64) << 16 | ((long) dy + 64) << 8 | (dz + 64)), "tekrar yok");
                return 1;
            });
            assertTrue(harcanan <= 4096, "bütçe aşılmaz: " + harcanan);
            cagri++;
        }
        assertEquals(beklenen, gorulen.size(), "kürenin bütün konumları");
        assertEquals((beklenen + 4095) / 4096, cagri, "r=24 tek tick'te değil, bütçeyle birkaç tick'e yayılır");
        assertEquals(0, tarayici.ilerle(4096, (dx, dy, dz) -> 1), "bitmiş tarayıcı okuma harcamaz");
    }

    @Test
    void kureTarayiciCokOkumaliKonumdaDurur() {
        KureTarayici tarayici = new KureTarayici(3);
        assertEquals(7, tarayici.ilerle(5, (dx, dy, dz) -> 7), "bir konum 7 okuma harcadı, bütçe 5: tek konumda durur");
        assertTrue(!tarayici.bitti());
    }

    @Test
    void afetBitisNedeniYanginKipiniBelirler() {
        assertTrue(YanginMantigi.temizBitis(BitisNedeni.KOMUT), "stop ve sifirla ateşi söndürür");
        assertTrue(YanginMantigi.temizBitis(BitisNedeni.IPTAL), "ön-uyarıda iptal edilen afetin ateşi kalmaz");
        assertFalse(YanginMantigi.temizBitis(BitisNedeni.DOGAL), "doğal bitişte ateş yanıp biter");
        assertFalse(YanginMantigi.temizBitis(BitisNedeni.KURAL), "gamerule kapanınca ateş yanıp biter");
        assertFalse(YanginMantigi.temizBitis(BitisNedeni.HATA), "hata bitişinde ateş yanıp biter");
    }
}
