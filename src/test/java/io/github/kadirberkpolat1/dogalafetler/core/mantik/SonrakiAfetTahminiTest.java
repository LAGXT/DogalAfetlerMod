package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.EnumSet;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class SonrakiAfetTahminiTest {
    private static final ZamanlayiciAyarlari A = ZamanlayiciAyarlari.VARSAYILAN;
    private static final ZamanlayiciAyarlari BEKLEMESIZ = A.baslangicBeklemeIle(0);
    private static final double GUN = AfetZamanlayicisi.GUN_TICK;

    private static SonrakiAfetTahmini tahmin(ZamanlayiciAyarlari ayarlar, long saat, BeklemeDurumu bekleme,
            Set<AfetTuru> turler, double siklik) {
        return SonrakiAfetTahmini.hesapla(ayarlar, saat, bekleme, turler, tur -> 1, siklik).orElseThrow();
    }

    @Test
    void uygunTurYoksaYaDaSiklikSifirsaBos() {
        assertTrue(SonrakiAfetTahmini.hesapla(A, 0, new BeklemeDurumu(), EnumSet.noneOf(AfetTuru.class), tur -> 1, 1)
                .isEmpty());
        assertTrue(SonrakiAfetTahmini.hesapla(A, 0, new BeklemeDurumu(), EnumSet.allOf(AfetTuru.class), tur -> 1, 0)
                .isEmpty());
        assertTrue(SonrakiAfetTahmini.hesapla(A, 0, new BeklemeDurumu(), EnumSet.of(AfetTuru.DEPREM), tur -> 0, 1)
                .isEmpty());
    }

    @Test
    void beklemesizTekTurUstelDagilim() {
        double oran = AfetZamanlayicisi.turOrani(BEKLEMESIZ, AfetTuru.DEPREM, 1, 1);
        SonrakiAfetTahmini t = tahmin(BEKLEMESIZ, 0, new BeklemeDurumu(), EnumSet.of(AfetTuru.DEPREM), 1);
        assertEquals(0, t.enErken());
        assertEquals(GUN / oran, t.ortalama(), 1);
        assertEquals(Math.log(10) * GUN / oran, t.enGec(), 1);
    }

    @Test
    void baslangicVeGenelBeklemeSikligaBolunur() {
        Set<AfetTuru> hepsi = EnumSet.allOf(AfetTuru.class);
        assertEquals(A.baslangicBeklemeTick(), tahmin(A, 0, new BeklemeDurumu(), hepsi, 1).enErken());
        assertEquals(A.baslangicBeklemeTick() / 10, tahmin(A, 0, new BeklemeDurumu(), hepsi, 10).enErken());
        assertEquals(A.baslangicBeklemeTick() - 1_000, tahmin(A, 1_000, new BeklemeDurumu(), hepsi, 1).enErken());
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.DEPREM, 1, 100_000, A, 10);
        assertEquals(A.genelBeklemeTick() / 10, tahmin(A, 100_000, bekleme, hepsi, 10).enErken());
        SonrakiAfetTahmini normal = tahmin(A, 0, new BeklemeDurumu(), hepsi, 1);
        SonrakiAfetTahmini on = tahmin(A, 0, new BeklemeDurumu(), hepsi, 10);
        assertEquals(normal.ortalama() / 10.0, on.ortalama(), 1, "%1000 bekleme ve oranı birlikte on kat kısaltmalı");
        assertTrue(on.enErken() <= on.ortalama() && on.ortalama() <= on.enGec());
    }

    /** İki tür: A hemen, B {@code T} sonra katılır. Ortalama = (1 − e^(−aT))/a + e^(−aT)/(a + b). */
    @Test
    void turBeklemesiParcaParcaOranVerir() {
        long t = 30_000;
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.ertele(AfetTuru.KASIRGA, 0, t);
        double a = AfetZamanlayicisi.turOrani(BEKLEMESIZ, AfetTuru.DEPREM, 1, 1) / GUN;
        double b = AfetZamanlayicisi.turOrani(BEKLEMESIZ, AfetTuru.KASIRGA, 1, 1) / GUN;
        SonrakiAfetTahmini tahmin = tahmin(BEKLEMESIZ, 0, bekleme, EnumSet.of(AfetTuru.DEPREM, AfetTuru.KASIRGA), 1);
        double beklenen = (1 - Math.exp(-a * t)) / a + Math.exp(-a * t) / (a + b);
        assertEquals(beklenen, tahmin.ortalama(), 1);
        assertEquals(0, tahmin.enErken());
    }

    /** AS-12: garanti anı en geç sınırdır; ortalama kısalır. Garanti beklemeden önceyse seçim ilk çıkışta olur. */
    @Test
    void ilkAfetGarantisiEnGecSinirdir() {
        Set<AfetTuru> hepsi = EnumSet.allOf(AfetTuru.class);
        SonrakiAfetTahmini serbest = SonrakiAfetTahmini.hesapla(A, 0, new BeklemeDurumu(), hepsi, tur -> 1, 1).orElseThrow();
        SonrakiAfetTahmini garantili = SonrakiAfetTahmini.hesapla(A, 0, new BeklemeDurumu(), hepsi, tur -> 1, 1, 6_000)
                .orElseThrow();
        assertTrue(serbest.enGec() > 6_000, "garantisiz %90 sınırı garantiden uzun: " + serbest);
        assertEquals(6_000, garantili.enGec());
        assertTrue(garantili.ortalama() < serbest.ortalama() && garantili.ortalama() > garantili.enErken(), garantili.toString());
        SonrakiAfetTahmini erken = SonrakiAfetTahmini.hesapla(A, 0, new BeklemeDurumu(), hepsi, tur -> 1, 1, 100)
                .orElseThrow();
        assertEquals(A.baslangicBeklemeTick(), erken.enGec());
        assertEquals(A.baslangicBeklemeTick(), erken.ortalama());
    }

    /** Tahmin zamanlayıcının kendisiyle tutarlı: aynı kurallarla sınanan seçimlerin ortalaması tahmine yakın. */
    @Test
    void tahminZamanlayiciylaTutarli() {
        Set<AfetTuru> hepsi = EnumSet.allOf(AfetTuru.class);
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.DEPREM, 3, 0, A, 10);
        long baslangic = 500;
        SonrakiAfetTahmini tahmin = tahmin(A, baslangic, bekleme, hepsi, 10);
        assertEquals(Math.max(A.baslangicBeklemeTick() / 10 - baslangic, bekleme.genelKalan(baslangic)), tahmin.enErken(),
                "başlangıç ya da genel beklemenin geç biteni belirler");
        SplittableRandom rastgele = new SplittableRandom(7);
        double toplam = 0;
        int deneme = 4_000;
        for (int i = 0; i < deneme; i++) {
            long saat = baslangic;
            while (AfetZamanlayicisi.sec(A, new AfetZamanlayicisi.Girdi(false, saat + 1, 1, bekleme, hepsi, tur -> 1, 10,
                    false), rastgele).isEmpty()) {
                saat++;
            }
            toplam += saat + 1 - baslangic;
        }
        double ortalama = toplam / deneme;
        assertEquals(tahmin.ortalama(), ortalama, tahmin.ortalama() * 0.05,
                "tahmin " + tahmin.ortalama() + ", sınanan " + ortalama);
    }

    /** K-E67: gün çarpanı bölgesel oranları çarpar, dönem olaylarını (sunucu geneli) yalnız dördüncü köküyle. */
    @Test
    void gunCarpaniBolgeselOranlariCarparDonemOlaylarinaAzEtkiler() {
        SonrakiAfetTahmini normal = SonrakiAfetTahmini.hesapla(BEKLEMESIZ, 0, new BeklemeDurumu(),
                EnumSet.of(AfetTuru.DEPREM), tur -> 1, 1, 1.0, Long.MAX_VALUE).orElseThrow();
        SonrakiAfetTahmini dortKat = SonrakiAfetTahmini.hesapla(BEKLEMESIZ, 0, new BeklemeDurumu(),
                EnumSet.of(AfetTuru.DEPREM), tur -> 1, 1, 4.0, Long.MAX_VALUE).orElseThrow();
        assertEquals(normal.ortalama() / 4.0, dortKat.ortalama(), 1);
        SonrakiAfetTahmini ay = SonrakiAfetTahmini.hesapla(BEKLEMESIZ, 0, new BeklemeDurumu(),
                EnumSet.of(AfetTuru.AY_CARPMASI), tur -> 1, 1, 1.0, Long.MAX_VALUE).orElseThrow();
        SonrakiAfetTahmini ayDortKat = SonrakiAfetTahmini.hesapla(BEKLEMESIZ, 0, new BeklemeDurumu(),
                EnumSet.of(AfetTuru.AY_CARPMASI), tur -> 1, 1, 4.0, Long.MAX_VALUE).orElseThrow();
        assertEquals(ay.ortalama() / Math.pow(4.0, AfetZamanlayicisi.DONEM_GUN_USSU), ayDortKat.ortalama(), 1);
    }
}
