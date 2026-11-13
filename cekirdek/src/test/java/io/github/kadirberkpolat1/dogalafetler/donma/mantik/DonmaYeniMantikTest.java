package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur.BUZ;
import static io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur.DIGER;
import static io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur.INCE;
import static io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur.SU;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu.Tur;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Faz 2'de eklenen saf mantık: ilerleme hızları, buharlaşma, yetişme bütçesi, eşik aralığı, ince buz evresi. */
class DonmaYeniMantikTest {
    private static final int GUN = 24_000;

    @Test
    void donmaAyarliGundeYuzeVarirVeGecmez() {
        float g = 0f;
        for (int t = 0; t < 6 * GUN / 20; t++) {
            g = DonmaIlerlemesi.donmaAdimi(g, 20, 6.0);
        }
        assertEquals(100f, g, 0.01f);
        assertEquals(100f, DonmaIlerlemesi.donmaAdimi(100f, 20, 6.0));
        assertEquals(50f, DonmaIlerlemesi.donmaAdimi(0f, 3 * GUN, 6.0), 0.01f, "hiz 1'de 3 günde yarı");
    }

    @Test
    void cozulmeTabanaIner10GundeVeAltinaInmez() {
        float g = 100f;
        for (int gun = 0; gun < 10; gun++) {
            g = DonmaIlerlemesi.cozulmeAdimi(g, GUN, 10.0, 40.0);
        }
        assertEquals(40f, g, 0.01f);
        assertEquals(40f, DonmaIlerlemesi.cozulmeAdimi(40f, GUN, 10.0, 40.0));
        assertEquals(30f, DonmaIlerlemesi.cozulmeAdimi(30f, GUN, 10.0, 40.0), "tabanın altındaki ilerleme yükselmez");
        assertEquals(94f, DonmaIlerlemesi.cozulmeAdimi(100f, GUN, 10.0, 40.0), 0.01f);
    }

    @Test
    void bagimsizCozulmeSifiraIner() {
        assertEquals(90f, DonmaIlerlemesi.bagimsizCozulmeAdimi(100f, GUN, 10.0), 0.01f);
        assertEquals(0f, DonmaIlerlemesi.bagimsizCozulmeAdimi(1f, GUN, 10.0));
    }

    @Test
    void sifirAdimDegistirmezGecersizSureReddedilir() {
        assertEquals(42f, DonmaIlerlemesi.donmaAdimi(42f, 0, 6.0));
        assertThrows(IllegalArgumentException.class, () -> DonmaIlerlemesi.donmaAdimi(0f, 1, 0.0));
    }

    @Test
    void buharlasmaDerinligiSureBoyuncaEnCogaVarir() {
        float d = 0f;
        for (int gun = 0; gun < 5; gun++) {
            d = BuharlasmaHedefi.derinlikAdimi(d, GUN, 5.0, 6.0);
        }
        assertEquals(6f, d, 0.001f);
        assertEquals(6f, BuharlasmaHedefi.derinlikAdimi(6f, GUN, 5.0, 6.0));
    }

    @Test
    void buharlasmaSanalPayiOnceTuketirSigGolTamamenKurur() {
        assertEquals(0, BuharlasmaHedefi.etkinDerinlik(2.5f, 3.0), "sanal deniz yükselmesi önce iner (N10)");
        assertEquals(3, BuharlasmaHedefi.etkinDerinlik(6f, 3.0));
        assertEquals(6, BuharlasmaHedefi.etkinDerinlik(6f, 0.0));
        assertEquals(1, BuharlasmaHedefi.etkinDerinlik(0.02f, 0.0), "buhar çıkar çıkmaz ilk blok kurur");
        assertEquals(0, BuharlasmaHedefi.etkinDerinlik(0.005f, 0.0), "eşik altında su dokunulmaz");
        assertEquals(1, BuharlasmaHedefi.etkinDerinlik(3.5f, 3.0), "pay tüketildiği an ilk blok kurur");
        assertEquals(2, BuharlasmaHedefi.kaldirilacak(6, 2), "2 derin göl tamamen kurur");
        assertEquals(6, BuharlasmaHedefi.kaldirilacak(6, 40), "okyanus yalnız derinlik kadar alçalır");
        assertEquals(0, BuharlasmaHedefi.kaldirilacak(0, 40));
        for (int d = 0; d <= 64; d++) {
            for (int su = 0; su <= 20; su++) {
                int k = BuharlasmaHedefi.kaldirilacak(d, su);
                assertTrue(k >= 0 && k <= d && k <= su, "sınır: d=" + d + " su=" + su);
            }
        }
    }

    @Test
    void butceKismaylaCarpilir() {
        YetismeButcesi b = new YetismeButcesi(256, 8, 4_000_000L);
        assertEquals(256, b.blokSiniri(1.0));
        assertEquals(128, b.blokSiniri(0.5));
        assertEquals(64, b.blokSiniri(0.25));
        assertEquals(256 * 8, b.birim(1.0));
        assertEquals(1, new YetismeButcesi(2, 8, 4_000_000L).blokSiniri(0.25), "en az bir blok");
    }

    @Test
    void sureTavaniAsimKadarDuserAmaCeyrekAltinaInmez() {
        YetismeButcesi b = new YetismeButcesi(256, 8, 4_000_000L);
        assertEquals(4_000_000L, b.sureSiniriNs(0));
        assertEquals(3_000_000L, b.sureSiniriNs(1_000_000L));
        assertEquals(1_000_000L, b.sureSiniriNs(50_000_000L));
    }

    /** Rezerv kuralı: sütun başına en çok {@code enCok} değişimle tick'in blok sayısı sınırı hiç aşmaz. */
    @Test
    void rezervleTickBasinaBlokSiniriAsilmaz() {
        Random r = new Random(7);
        for (double kisma : new double[] {1.0, 0.5, 0.25}) {
            YetismeButcesi b = new YetismeButcesi(256, 8, 4_000_000L);
            int enCok = 12;
            int rezerv = b.rezerv(enCok);
            for (int tick = 0; tick < 2000; tick++) {
                int kalan = b.birim(kisma);
                int blok = 0;
                while (b.sutunSigar(kalan, rezerv)) {
                    int degisen = r.nextInt(enCok + 1);
                    blok += degisen;
                    kalan -= b.sutunMaliyeti(degisen);
                }
                assertTrue(blok <= b.blokSiniri(kisma), "tick'te " + blok + " blok > " + b.blokSiniri(kisma));
            }
        }
    }

    @Test
    void esikKumesiDegisimOlmayanAraligiAtlar() {
        DonmaEsikleri e = DonmaEsikleri.VARSAYILAN;
        EsikKumesi k = EsikKumesi.kur(e, new int[] {10}, new int[] {50});
        assertTrue(k.degisimVar(9, 10, false), "kırağı 10'da gelir");
        assertFalse(k.degisimVar(0, 9, false), "0..9 arası hiçbir şey yok");
        assertTrue(k.degisimVar(49, 50, true), "kırağı 50'nin altında kalkar");
        assertFalse(k.degisimVar(100, 101, false), "100 üstü titreşimde yeni kar katmanı yok");
        assertTrue(k.degisimVar(-8, 108, false));
    }

    /** Atlama güvenli: bir aralıkta değişim yoksa eşik fonksiyonlarının hiçbiri aralığın iki ucunda farklı değildir. */
    @Test
    void esikKumesiEsikFonksiyonlariylaTutarli() {
        DonmaEsikleri e = DonmaEsikleri.VARSAYILAN;
        EsikKumesi k = EsikKumesi.kur(e, new int[0], new int[0]);
        for (int a = -10; a <= 110; a++) {
            for (int b = a + 1; b <= Math.min(110, a + 12); b++) {
                for (int c = 0; c <= 16; c++) {
                    boolean donmaFarki = e.karKatmani(a, false) != e.karKatmani(b, false)
                            || e.yapiKarKatmani(a, 1) != e.yapiKarKatmani(b, 1)
                            || e.buzSeviyesi(a, c) != e.buzSeviyesi(b, c);
                    boolean cozulmeFarki = e.karKatmaniCozulme(a, false) != e.karKatmaniCozulme(b, false)
                            || e.yapiKarKatmani(a, -1) != e.yapiKarKatmani(b, -1)
                            || e.buzSeviyesiCozulme(a, c) != e.buzSeviyesiCozulme(b, c);
                    if (donmaFarki) {
                        assertTrue(k.degisimVar(a, b, false), "donma " + a + ".." + b + " c=" + c);
                    }
                    if (cozulmeFarki) {
                        assertTrue(k.degisimVar(a, b, true), "çözülme " + a + ".." + b + " c=" + c);
                    }
                }
            }
        }
    }

    @Test
    void inceBuzIlkVeSonEvre() {
        DonmaEsikleri e = DonmaEsikleri.VARSAYILAN;
        assertEquals(0, e.buzSeviyesi(19, 0));
        assertEquals(1, e.buzSeviyesi(20, 0), "kıyıda 20'de ince buz");
        assertEquals(2, e.buzSeviyesi(26, 0), "pay bitince bir blok buz");
        assertEquals(5, e.buzSeviyesi(100, 0), "4 blok buz = seviye 5");
        assertEquals(1, e.buzSeviyesiCozulme(45, 0), "çözülürken son evre ince buz");
        assertEquals(0, e.buzSeviyesiCozulme(44, 0));
        for (int c = 0; c <= 15; c++) {
            int onceki = 0;
            for (int l = -8; l <= 108; l++) {
                int v = e.buzSeviyesi(l, c);
                assertTrue(v >= onceki, "buz seviyesi tekdüze değil l=" + l + " c=" + c);
                assertEquals(v == 0 ? 0 : Math.max(1, v - 1), e.buzKalinligi(l, c), "kalınlıkla tutarlı");
                onceki = v;
            }
        }
    }

    @Test
    void suSutunuInceBuzPlani() {
        Tur[] su = {SU, SU, SU, DIGER};
        assertArrayEquals(new Tur[] {INCE, SU, SU, DIGER}, SuSutunu.planlaSeviye(su, 1, 1));
        assertArrayEquals(new Tur[] {BUZ, SU, SU, DIGER}, SuSutunu.planlaSeviye(new Tur[] {INCE, SU, SU, DIGER}, 2, 1));
        assertArrayEquals(new Tur[] {INCE, SU, SU, DIGER},
                SuSutunu.planlaSeviye(new Tur[] {BUZ, BUZ, SU, DIGER}, 1, -1), "çözülürken buz → ince buz");
        assertArrayEquals(su, SuSutunu.planlaSeviye(new Tur[] {INCE, SU, SU, DIGER}, 0, -1));
        assertArrayEquals(new Tur[] {BUZ, SU, SU, DIGER}, SuSutunu.planlaSeviye(new Tur[] {BUZ, SU, SU, DIGER}, 1, 1),
                "donarken geri ince buza dönmez");
        assertArrayEquals(new Tur[] {INCE, SU, SU, DIGER}, SuSutunu.planlaSeviye(new Tur[] {INCE, SU, SU, DIGER}, 2, -1),
                "çözülürken kalınlaşmaz");
        assertArrayEquals(new Tur[] {BUZ, BUZ, SU, DIGER}, SuSutunu.planlaSeviye(su, 9, 1), "en alt su kalır");
        Tur[] oyuncu = {INCE, DIGER};
        assertArrayEquals(oyuncu, SuSutunu.planlaSeviye(oyuncu, 0, -1), "altı su olmayan buz su sayılmaz");
        assertEquals(1, SuSutunu.mevcutSeviye(new Tur[] {INCE, SU}));
        assertEquals(3, SuSutunu.mevcutSeviye(new Tur[] {BUZ, BUZ, SU}));
        assertEquals(0, SuSutunu.mevcutSeviye(new Tur[] {SU, SU}));
    }

    /**
     * Yön dönüşünde sütun öbür yönün fonksiyonundadır: donma 60'ta (c=0) 3 blok buz, çözülme fonksiyonu (55, 60]'ta sabit,
     * ama 55'e inince sütun 2 bloğa incelmeli. Bu yüzden eşik aralığı atlaması yalnız aynı yöndeki ardışık adımlarda serbest.
     */
    @Test
    void yonDonusundeAtlamaGuvenliDegil() {
        DonmaEsikleri e = DonmaEsikleri.VARSAYILAN;
        assertEquals(e.buzSeviyesiCozulme(55, 0), e.buzSeviyesiCozulme(60, 0), "çözülme fonksiyonu aralıkta sabit");
        assertTrue(e.buzSeviyesi(60, 0) > e.buzSeviyesiCozulme(55, 0), "donmuş sütun yine de incelmeli");
    }

    @Test
    void atlamaYalnizAyniYondeSerbest() {
        assertTrue(DonmaHedefi.atlamaSerbest(1, 20, false, 25, false, false), "donma sürüyor");
        assertTrue(DonmaHedefi.atlamaSerbest(-1, 60, true, 55, true, false), "çözülme sürüyor");
        assertFalse(DonmaHedefi.atlamaSerbest(1, 60, false, 55, false, false), "donmadan çözülmeye dönüş");
        assertFalse(DonmaHedefi.atlamaSerbest(-1, 40, false, 45, false, false), "çözülmeden donmaya dönüş");
        assertFalse(DonmaHedefi.atlamaSerbest(1, 20, false, 25, false, true), "yarım adımdan devam");
        assertFalse(DonmaHedefi.atlamaSerbest(-1, 100, false, 100, true, false), "dönem değişimi");
        assertTrue(DonmaHedefi.atlamaSerbest(0, 0, false, 5, false, false), "hiç işlenmemiş chunk donmaya başlıyor");
        assertFalse(DonmaHedefi.atlamaSerbest(0, 30, false, 35, false, false), "yön bilinmiyor (dönem değişimi sonrası)");
        assertFalse(DonmaHedefi.atlamaSerbest(0, 0, true, 5, true, false), "çözülme döneminde bilinmeyen yön");
        assertEquals(0, DonmaHedefi.adimYonu(100, false, 100, true), "dönem değişen adımın yönü bilinmez");
        assertEquals(-1, DonmaHedefi.adimYonu(60, true, 55, true));
        assertEquals(1, DonmaHedefi.adimYonu(55, false, 60, false));
    }
}
