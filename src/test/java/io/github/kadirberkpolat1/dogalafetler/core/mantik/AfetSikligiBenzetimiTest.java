package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik.DepremSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.Yonelim;
import io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik.OrmanKurallari;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.afet.tsunami.TsunamiSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;

/**
 * K-E67 kullanıcı istekleri 3 ve 1–2'nin benzetimle kanıtı: afetSikligi %100'de doğal afet başlangıçları arası ortalama
 * gerçek süre gün 1'de ~8 dk, gün 10'da ~5 dk, gün 30+'da ~3 dk; ilk afet 3. dakikadan önce değil, 8. dakikada garanti;
 * seviye dağılımı günle yükselir; eşzamanlı afet sayısı günle artar; çok afet gelirse seviye aşağı kayar. Afet süreleri
 * türlerin varsayılan seviye tablolarından (ortalama ön-uyarı + siren dahil) alınır; tablo satırları rapora yazılır. Güneş
 * patlaması ve ay çarpması gerçekleşmiş sayılır (kalıcı bölgesel tempo ölçülür).
 */
class AfetSikligiBenzetimiTest {
    private static final long GUN = AfetZamanlayicisi.GUN_TICK;
    private static final double DAKIKA = 1200.0;
    private static final int[] GUNLER = {1, 10, 30, 60};
    private static final int[] ARTCI = {0, 0, 1, 2, 3};

    /** Ön-uyarı + siren (ortalama) + AKTIF + yaklaşık bitiş evresi, tick. */
    static long sure(AfetTuru tur, int seviye) {
        long once = Math.round(OnUyariPenceresi.VARSAYILAN_ERKEN_BILDIRIM
                + OnUyariPenceresi.ortalama(OnUyariPenceresi.VARSAYILAN_EN_AZ, OnUyariPenceresi.VARSAYILAN_EN_COK, 0))
                + SirenTuru.STANDART_SURE_TICK;
        long aktif = switch (tur) {
            case KASIRGA -> KasirgaSeviyesi.icin(seviye).aktifSureSn() * 20L;
            case TSUNAMI -> {
                TsunamiSeviyesi s = TsunamiSeviyesi.al(seviye);
                double dalga = (s.aciklik() + s.karaGiris()) / s.hizBlokSn() * 20;
                // Doğuş gecikmesi ve ikinci dalga (S3+) ile suların çekilmesi için yaklaşık 20 sn bitiş.
                yield Math.round(dalga * (s.ikinciDalga() ? 2 : 1)) + 400;
            }
            // Ana sarsıntı + varsayılan artçılar (S1…S5: 0, 0, 1, 2, 3; aralık 20–60 sn, ortalama ~44 sn).
            case DEPREM -> DepremSeviyesi.varsayilan(seviye).sureTick() + ARTCI[seviye - 1] * 880L;
            case ASIT_YAGMURU -> AsitKurallari.Tablo.VARSAYILAN.seviye(seviye).sureTick();
            case SINKHOLE -> SinkholeSeviyesi.varsayilan(seviye).aktifTick();
            case ORMAN_YANGINI -> OrmanKurallari.Tablo.VARSAYILAN.seviye(seviye).sureTick();
            case GUNES_PATLAMASI, AY_CARPMASI -> throw new IllegalArgumentException("dönem olayı benzetime girmez");
        };
        return once + aktif;
    }

    /** Yerleşim: tek yuva (takvim temposu), tek oyuncu (gerçek alanlar), dağınık üç oyuncu. */
    private enum Yerlesim {
        TEK_YUVA,
        TEK_OYUNCU,
        DAGINIK
    }

    /**
     * Tek oyuncu çevresinde etki alanı (oyuncu 0, 0'da): türlerin {@code merkezSec} uzaklıkları ve {@code etkiAlani}
     * yarıçapları seviye tablolarından. Kasırga {@code Yonelim.dogusOfseti} ve çekim yarıçapı; deprem 6 + 2·seviye blok
     * uzakta, {@code max(yıkım yarıçapı, uzunluk/2 + 8)}; sinkhole 12–40 blok, yarıçap + 16; orman yangını 40–90 blok
     * ({@code orman.aramaEnAz/EnCok} varsayılanı), başlangıç yarıçapı + {@code ALAN_PAYI}; tsunami oyuncunun kıyısında,
     * {@code max((karaGiris + aciklik)/2, genislik/2) + 16}; asit yağmuru global.
     */
    static EtkiAlani tekOyuncuAlani(AfetTuru tur, int seviye, RandomGenerator r) {
        double aci = r.nextDouble() * Math.PI * 2;
        return switch (tur) {
            case KASIRGA -> {
                double cekim = KasirgaSeviyesi.icin(seviye).cekimYaricapi();
                double[] o = Yonelim.dogusOfseti(cekim, r.nextDouble(), aci);
                yield EtkiAlani.daire(o[0], 0, o[1], cekim);
            }
            case DEPREM -> {
                DepremSeviyesi s = DepremSeviyesi.varsayilan(seviye);
                double d = 6 + 2 * seviye;
                yield EtkiAlani.daire(Math.cos(aci) * d, 0, Math.sin(aci) * d,
                        Math.max(s.yikimYaricapi(), s.uzunluk() / 2.0 + 8));
            }
            case SINKHOLE -> {
                double d = 12 + r.nextDouble() * 28;
                yield EtkiAlani.daire(Math.cos(aci) * d, 0, Math.sin(aci) * d,
                        SinkholeSeviyesi.varsayilan(seviye).yaricap() + 16);
            }
            case ORMAN_YANGINI -> {
                double d = 40 + r.nextDouble() * 50;
                yield EtkiAlani.daire(Math.cos(aci) * d, 0, Math.sin(aci) * d,
                        OrmanKurallari.Tablo.VARSAYILAN.seviye(seviye).yaricap() + OrmanKurallari.ALAN_PAYI);
            }
            case TSUNAMI -> {
                TsunamiSeviyesi s = TsunamiSeviyesi.al(seviye);
                yield EtkiAlani.daire(0, 0, 0, Math.max((s.karaGiris() + s.aciklik()) / 2, s.genislik() / 2) + 16);
            }
            case ASIT_YAGMURU, GUNES_PATLAMASI, AY_CARPMASI -> EtkiAlani.herYer();
        };
    }

    private static AfetSimulasyonu kalici(double gun, Yerlesim yerlesim, long tohum) {
        AfetSimulasyonu sim = new AfetSimulasyonu().gunSabit(gun).ayVar(false).evre(GunesEvresi.GUNESSIZ).sureler(
                AfetSikligiBenzetimiTest::sure).tohum(tohum);
        return switch (yerlesim) {
            case TEK_YUVA -> sim.hepsiCakisir();
            case TEK_OYUNCU -> sim.alanlar(AfetSikligiBenzetimiTest::tekOyuncuAlani);
            case DAGINIK -> sim.oyuncuSayisi(3).alanlar(AfetSikligiBenzetimiTest::tekOyuncuAlani);
        };
    }

    /** Başlangıçlar arası ortalama (dk). */
    private static double aralik(AfetSimulasyonu.Sonuc sonuc) {
        List<AfetSimulasyonu.Olay> o = sonuc.olaylar();
        return (o.get(o.size() - 1).baslangic() - o.get(0).baslangic()) / (double) (o.size() - 1) / DAKIKA;
    }

    private static double[] seviyeDagilimi(AfetSimulasyonu.Sonuc sonuc) {
        double[] pay = new double[5];
        int adet = 0;
        for (AfetSimulasyonu.Olay olay : sonuc.olaylar()) {
            if (olay.seviye() > 0) {
                pay[olay.seviye() - 1]++;
                adet++;
            }
        }
        for (int i = 0; i < 5; i++) {
            pay[i] /= adet;
        }
        return pay;
    }

    private static int enCokEszamanli(AfetSimulasyonu.Sonuc sonuc) {
        List<long[]> kenarlar = new ArrayList<>();
        for (AfetSimulasyonu.Olay olay : sonuc.olaylar()) {
            kenarlar.add(new long[] {olay.baslangic(), 1});
            kenarlar.add(new long[] {olay.bitis(), -1});
        }
        kenarlar.sort((a, b) -> a[0] != b[0] ? Long.compare(a[0], b[0]) : Long.compare(a[1], b[1]));
        int simdi = 0;
        int enCok = 0;
        for (long[] k : kenarlar) {
            simdi += (int) k[1];
            enCok = Math.max(enCok, simdi);
        }
        return enCok;
    }

    /**
     * Takvim temposu (tek yuva) hedefe yakın: gün 1 ~8, gün 10 ~5, gün 30+ ~3 dk. Gün 15'ten sonra eşzamanlı afetler
     * başlangıçları sıklaştırır: tek oyuncuda alanları çakışmayan tür çiftleri (kasırga, sinkhole, orman yangını, kısmen
     * deprem) yan yana sürer, dağınık oyuncularda eşzamanlı sınıra ulaşılır.
     */
    @Test
    void aralikGunleKisalirEszamanliAfetlerSayiyiArtirir() {
        double[] takvim = new double[GUNLER.length];
        double[] tekAralik = new double[GUNLER.length];
        int[] tekEnCok = new int[GUNLER.length];
        System.out.println("| Gün | Takvim (tek yuva) aralık (dk) | Tek oyuncu aralık (dk) | Tek oyuncu en çok eşzamanlı | "
                + "Dağınık 3 oyuncu aralık (dk) | Dağınık en çok eşzamanlı | S1 | S2 | S3 | S4 | S5 |");
        System.out.println("|---|---|---|---|---|---|---|---|---|---|---|");
        for (int i = 0; i < GUNLER.length; i++) {
            int sinir = ZamanlayiciAyarlari.VARSAYILAN.eszamanliSinir(GUNLER[i]);
            AfetSimulasyonu.Sonuc yuva = kalici(GUNLER[i], Yerlesim.TEK_YUVA, 11 + i).kos(60 * GUN);
            AfetSimulasyonu.Sonuc tek = kalici(GUNLER[i], Yerlesim.TEK_OYUNCU, 11 + i).kos(60 * GUN);
            AfetSimulasyonu.Sonuc cok = kalici(GUNLER[i], Yerlesim.DAGINIK, 11 + i).kos(60 * GUN);
            for (AfetSimulasyonu.Sonuc sonuc : List.of(yuva, tek, cok)) {
                assertEquals(0, sonuc.ayniAnIhlali());
                assertTrue(enCokEszamanli(sonuc) <= sinir, "eşzamanlı sınır aşıldı: gün " + GUNLER[i]);
            }
            takvim[i] = aralik(yuva);
            tekAralik[i] = aralik(tek);
            tekEnCok[i] = enCokEszamanli(tek);
            double[] pay = seviyeDagilimi(tek);
            System.out.printf(Locale.ROOT, "| %d | %.2f | %.2f | %d | %.2f | %d | %.1f | %.1f | %.1f | %.1f | %.1f |%n",
                    GUNLER[i], takvim[i], tekAralik[i], tekEnCok[i], aralik(cok), enCokEszamanli(cok), pay[0] * 100,
                    pay[1] * 100, pay[2] * 100, pay[3] * 100, pay[4] * 100);
            assertEquals(1, enCokEszamanli(yuva));
            assertTrue(tekAralik[i] <= takvim[i] + 0.1, "eşzamanlı afet sayıyı azaltmamalı: gün " + GUNLER[i]);
            assertTrue(aralik(cok) <= tekAralik[i] + 0.1, "dağınık oyuncuda en az tek oyuncu kadar: gün " + GUNLER[i]);
            assertEquals(sinir, enCokEszamanli(cok), "dağınık oyuncularda eşzamanlı sınıra ulaşılmalı: gün " + GUNLER[i]);
        }
        assertEquals(8.0, takvim[0], 1.3, "gün 1 ~8 dk");
        assertEquals(5.0, takvim[1], 0.9, "gün 10 ~5 dk");
        assertEquals(3.0, takvim[2], 0.6, "gün 30 ~3 dk");
        assertEquals(3.0, takvim[3], 0.6, "gün 60 ~3 dk");
        assertTrue(takvim[0] > takvim[1] && takvim[1] > takvim[2], "aralık günle kısalmalı");
        assertTrue(takvim[3] <= takvim[2] * 1.1, "yüksek seviye beklemesi günle artan sıklığı tersine çevirmemeli");
        assertTrue(tekEnCok[3] >= 2, "tek oyuncuda da gün 60'ta eşzamanlı doğal afet olmalı: " + tekEnCok[3]);
        assertTrue(tekAralik[3] < takvim[3], "tek oyuncuda eşzamanlılık başlangıçları sıklaştırmalı");
    }

    @Test
    void seviyeDagilimiGunleYukselir() {
        double[][] pay = new double[GUNLER.length][];
        for (int i = 0; i < GUNLER.length; i++) {
            pay[i] = seviyeDagilimi(kalici(GUNLER[i], Yerlesim.TEK_OYUNCU, 31 + i).kos(120 * GUN));
        }
        assertTrue(pay[0][0] + pay[0][1] >= 0.99 && pay[0][2] == 0, "gün 1: yalnız S1–S2");
        assertTrue(pay[1][3] > 0.02 && pay[1][4] == 0, "gün 10: S4 gelir, S5 henüz yok");
        assertTrue(pay[2][4] > 0.03, "gün 30: S5 gelir");
        // Taban olasılık %27 (SiddetSeciciTest); art arda 4-5 kuralı gerçekleşeni ~%21'e indirir.
        assertTrue(pay[3][3] + pay[3][4] >= 0.15, "gün 60: S4+S5 ≥ %15");
        for (int i = 1; i < GUNLER.length; i++) {
            assertTrue(pay[i][0] < pay[i - 1][0], "S1 payı günle azalmalı");
        }
    }

    @Test
    void ilkAfetUcDakikadanOnceDegilSekizDakikadaGaranti() {
        List<Double> ilkler = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
            AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().gun(0).sureler(AfetSikligiBenzetimiTest::sure)
                    .hepsiCakisir().tohum(500 + i).kos(GUN / 2);
            assertTrue(sonuc.toplam() > 0, "yarım günde afet yok");
            long ilk = sonuc.olaylar().get(0).baslangic();
            assertTrue(ilk >= ZamanlayiciAyarlari.VARSAYILAN.baslangicBeklemeTick(), "3. dakikadan önce: " + ilk);
            assertTrue(ilk <= ZamanlayiciAyarlari.VARSAYILAN.ilkAfetGarantiTick(), "8. dakikada garanti: " + ilk);
            ilkler.add(ilk / DAKIKA);
        }
        Collections.sort(ilkler);
        double ortanca = ilkler.get(ilkler.size() / 2);
        System.out.printf(Locale.ROOT, "ilk afet (seçim) ortanca %.2f dk, en erken %.2f, en geç %.2f%n", ortanca,
                ilkler.get(0), ilkler.get(ilkler.size() - 1));
        assertTrue(ortanca > 3.0 && ortanca < 7.5, "ilk afet ortancası: " + ortanca);
    }

    /** "Çok gelirse güçsüz": afetSikligi %400'de aynı günde seviye dağılımı aşağı kayar (deterministik zayıflatma). */
    @Test
    void cokAfetGelinceSeviyeAsagiKayar() {
        double[] normal = seviyeDagilimi(kalici(60, Yerlesim.TEK_OYUNCU, 71).kos(60 * GUN));
        double[] yogun = seviyeDagilimi(kalici(60, Yerlesim.TEK_OYUNCU, 71).siklik(4).kos(60 * GUN));
        System.out.printf(Locale.ROOT, "gün 60 S4+S5: %%100 %.1f%%, %%400 %.1f%%%n", (normal[3] + normal[4]) * 100,
                (yogun[3] + yogun[4]) * 100);
        assertTrue(yogun[3] + yogun[4] < (normal[3] + normal[4]) * 0.6, "yüksek seviyeler azalmalı");
        assertTrue(yogun[4] > 0, "zayıflatma S5'i sıfırlamaz");
        assertTrue(yogun[0] > normal[0], "S1 payı artmalı");
    }
}
