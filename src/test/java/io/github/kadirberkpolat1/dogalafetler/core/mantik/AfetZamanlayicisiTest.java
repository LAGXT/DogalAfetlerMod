package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class AfetZamanlayicisiTest {
    private static final List<AfetTuru> SIK_ALTI = List.of(AfetTuru.KASIRGA, AfetTuru.TSUNAMI, AfetTuru.DEPREM,
            AfetTuru.ASIT_YAGMURU, AfetTuru.SINKHOLE, AfetTuru.ORMAN_YANGINI);
    /** Bekleme yok, günde 240 olay oranı: 10^6 tick'te ~9 000 afet; sıra istatistiksel olarak sağlam. */
    private static final ZamanlayiciAyarlari YOGUN = ZamanlayiciAyarlari.VARSAYILAN.gunlukOranIle(240).genelBeklemeIle(0)
            .turBeklemeleriIle(0).baslangicBeklemeIle(0);

    private static AfetZamanlayicisi.Girdi girdi(boolean aktif, long saat, BeklemeDurumu bekleme,
            Set<AfetTuru> uygun, double carpan, boolean zorla) {
        return new AfetZamanlayicisi.Girdi(aktif, saat, 20, bekleme, uygun, tur -> carpan, 1.0, zorla);
    }

    private static AfetSimulasyonu yogun() {
        return new AfetSimulasyonu().zamanlayici(YOGUN).afetSuresi(20);
    }

    @Test
    void dagilimNadirlikSirasiniKorurVeHicbirAfetSifirKalmaz() {
        // Gün 1000: gün kapıları açık, ay var, güneş normal (dönem geçişi kapalı: tek seferlikler tekrar çıkabilir).
        AfetSimulasyonu.Sonuc sonuc = yogun().gun(1000).kos(1_000_000);
        int ay = sonuc.sayi(AfetTuru.AY_CARPMASI);
        int gunes = sonuc.sayi(AfetTuru.GUNES_PATLAMASI);
        double sikOrtalama = SIK_ALTI.stream().mapToInt(sonuc::sayi).average().orElseThrow();
        for (AfetTuru tur : AfetTuru.values()) {
            assertTrue(sonuc.sayi(tur) > 0, tur + " hiç çıkmadı");
        }
        for (AfetTuru tur : SIK_ALTI) {
            assertTrue(sonuc.sayi(tur) > ay, tur + " ay çarpmasından sık olmalı: " + sonuc.sayilar());
            assertEquals(sikOrtalama, sonuc.sayi(tur), sikOrtalama * 0.2,
                    "altı sık afet birbirine yakın olmalı: " + sonuc.sayilar());
        }
        assertTrue(ay > gunes, "ay çarpması güneş patlamasından sık olmalı: " + sonuc.sayilar());
        assertEquals(0, sonuc.ayniAnIhlali());
    }

    @Test
    void gunSifirdaGunesPatlamasiVeAyCarpmasiCikmaz() {
        AfetSimulasyonu.Sonuc sonuc = yogun().gunSabit(0).kos(1_000_000);
        assertEquals(0, sonuc.sayi(AfetTuru.GUNES_PATLAMASI), sonuc.sayilar().toString());
        assertEquals(0, sonuc.sayi(AfetTuru.AY_CARPMASI), sonuc.sayilar().toString());
        for (AfetTuru tur : SIK_ALTI) {
            assertTrue(sonuc.sayi(tur) > 0, tur + " gün 0'da da çıkmalı");
        }
    }

    @Test
    void ayYokkenAyCarpmasiKizilCucedeGunesPatlamasiCikmaz() {
        AfetSimulasyonu.Sonuc aysiz = yogun().gun(1000).ayVar(false).kos(1_000_000);
        assertEquals(0, aysiz.sayi(AfetTuru.AY_CARPMASI));
        assertTrue(aysiz.sayi(AfetTuru.GUNES_PATLAMASI) > 0);
        AfetSimulasyonu.Sonuc kizilCuce = yogun().gun(1000).evre(GunesEvresi.KIZIL_CUCE).kos(1_000_000);
        assertEquals(0, kizilCuce.sayi(AfetTuru.GUNES_PATLAMASI));
        assertTrue(kizilCuce.sayi(AfetTuru.AY_CARPMASI) > 0);
    }

    @Test
    void puanKriterindeGelismemisOyuncuyaTekSeferlikAfetGelmez() {
        TekSeferlikAyarlari puanli = TekSeferlikAyarlari.VARSAYILAN.kriterlerIle(GelisimKriteri.PUAN,
                GelisimKriteri.PUAN);
        AfetSimulasyonu.Sonuc dusuk = yogun().gun(1000).tekSeferlik(puanli).puan(10).kos(1_000_000);
        assertEquals(0, dusuk.sayi(AfetTuru.GUNES_PATLAMASI));
        assertEquals(0, dusuk.sayi(AfetTuru.AY_CARPMASI));
        AfetSimulasyonu.Sonuc yuksek = yogun().gunSabit(0).tekSeferlik(puanli).puan(100).kos(1_000_000);
        assertTrue(yuksek.sayi(AfetTuru.GUNES_PATLAMASI) > 0, "puan kriteri güne bakmaz");
        assertTrue(yuksek.sayi(AfetTuru.AY_CARPMASI) > 0);
    }

    @Test
    void tekSeferlikOlaylarGerceklesinceBirDahaSecilmez() {
        AfetSimulasyonu.Sonuc sonuc = yogun().gun(1000).donemGecisleri(true).kos(1_000_000);
        assertEquals(1, sonuc.sayi(AfetTuru.AY_CARPMASI), sonuc.sayilar().toString());
        assertEquals(1, sonuc.sayi(AfetTuru.GUNES_PATLAMASI), sonuc.sayilar().toString());
        for (AfetTuru tur : SIK_ALTI) {
            assertTrue(sonuc.sayi(tur) > 0, tur + " her dönemde devam etmeli");
        }
    }

    @Test
    void varsayilanAyarlarlaSiklikMakulVeHerAfetGorulur() {
        AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().afetSuresi(6000).donemGecisleri(true).kos(10_000_000);
        double gunluk = sonuc.gunlukOlay();
        assertTrue(gunluk > 0.25 && gunluk < 1.0, "günlük afet sayısı makul aralıkta değil: " + gunluk);
        for (AfetTuru tur : AfetTuru.values()) {
            assertTrue(sonuc.sayi(tur) > 0, tur + " 417 günde hiç çıkmadı: " + sonuc.sayilar());
        }
        assertEquals(1, sonuc.sayi(AfetTuru.AY_CARPMASI));
        assertEquals(1, sonuc.sayi(AfetTuru.GUNES_PATLAMASI));
        List<AfetSimulasyonu.Olay> olaylar = sonuc.olaylar();
        for (int i = 1; i < olaylar.size(); i++) {
            long ara = olaylar.get(i).baslangic() - olaylar.get(i - 1).bitis();
            assertTrue(ara >= ZamanlayiciAyarlari.VARSAYILAN.genelBeklemeTick(), "bekleme bozuldu: " + ara);
        }
    }

    @Test
    void ilkGunesPatlamasiOrtancasiAltmisIleYuzYirmiArasiVeAydanSonra() {
        int dunya = 400;
        long sinir = 400L * AfetZamanlayicisi.GUN_TICK;
        List<Double> gunes = new ArrayList<>();
        List<Double> ay = new ArrayList<>();
        int gunesli = 0;
        for (int i = 0; i < dunya; i++) {
            AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().tohum(1000 + i).adim(100).afetSuresi(8000)
                    .donemGecisleri(true).ilkOlaylardaDur(EnumSet.of(AfetTuru.GUNES_PATLAMASI, AfetTuru.AY_CARPMASI))
                    .kos(sinir);
            OptionalDouble g = sonuc.ilkGun(AfetTuru.GUNES_PATLAMASI);
            OptionalDouble a = sonuc.ilkGun(AfetTuru.AY_CARPMASI);
            gunes.add(g.orElse(Double.POSITIVE_INFINITY));
            ay.add(a.orElse(Double.POSITIVE_INFINITY));
            if (g.isPresent()) {
                gunesli++;
            }
        }
        Collections.sort(gunes);
        Collections.sort(ay);
        double gunesOrtanca = gunes.get(dunya / 2);
        double ayOrtanca = ay.get(dunya / 2);
        double oran = gunesli / (double) dunya;
        System.out.printf(Locale.ROOT, "ilk güneş patlaması ortanca gün %.1f, 400 günde görülme %.4f; ilk ay çarpması ortanca gün %.1f%n",
                gunesOrtanca, oran, ayOrtanca);
        assertTrue(gunesOrtanca >= 60 && gunesOrtanca <= 120, "ilk güneş patlaması ortancası: " + gunesOrtanca);
        assertTrue(oran >= 0.99, "400 günde en az bir güneş patlaması oranı: " + oran);
        assertTrue(ayOrtanca < gunesOrtanca, "ay çarpması ortancası " + ayOrtanca + " güneşten önce olmalı");
    }

    @Test
    void minimumBeklemeIcindeSecimYok() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN.gunlukOranIle(1_000_000).baslangicBeklemeIle(0);
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.KASIRGA, 1, 1000, ayarlar, 1.0);
        long bitis = 1000 + ayarlar.genelBeklemeTick();
        SplittableRandom rastgele = new SplittableRandom(1);
        Set<AfetTuru> hepsi = EnumSet.allOf(AfetTuru.class);
        for (long saat = 1000; saat < bitis; saat += 20) {
            assertTrue(AfetZamanlayicisi.sec(ayarlar, girdi(false, saat, bekleme, hepsi, 100, false), rastgele)
                    .isEmpty(), "genel bekleme içinde seçim yapıldı: saat " + saat);
        }
        Optional<AfetTuru> sonra = AfetZamanlayicisi.sec(ayarlar, girdi(false, bitis, bekleme, hepsi, 1, false),
                rastgele);
        assertTrue(sonra.isPresent(), "bekleme bitince seçim olmalı");
        assertTrue(sonra.get() != AfetTuru.KASIRGA, "kasırga kendi beklemesinde olmalı");
    }

    @Test
    void baslangicBeklemesindeSecimYok() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN.gunlukOranIle(1_000_000);
        SplittableRandom rastgele = new SplittableRandom(2);
        Set<AfetTuru> hepsi = EnumSet.allOf(AfetTuru.class);
        assertTrue(AfetZamanlayicisi.sec(ayarlar,
                girdi(false, ayarlar.baslangicBeklemeTick() - 1, new BeklemeDurumu(), hepsi, 1, false), rastgele)
                .isEmpty());
        assertTrue(AfetZamanlayicisi.sec(ayarlar,
                girdi(false, ayarlar.baslangicBeklemeTick(), new BeklemeDurumu(), hepsi, 1, false), rastgele)
                .isPresent());
    }

    @Test
    void siddetliAfettenSonraBeklemeUzar() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN;
        long[] kalanlar = new long[6];
        for (int seviye = 1; seviye <= 5; seviye++) {
            BeklemeDurumu bekleme = new BeklemeDurumu();
            bekleme.afetBitti(AfetTuru.DEPREM, seviye, 0, ayarlar, 1.0);
            kalanlar[seviye] = bekleme.genelKalan(0);
        }
        assertEquals(ayarlar.genelBeklemeTick(), kalanlar[1]);
        assertEquals(ayarlar.genelBeklemeTick(), kalanlar[2]);
        assertTrue(kalanlar[3] > kalanlar[2]);
        assertTrue(kalanlar[4] > kalanlar[3]);
        assertTrue(kalanlar[5] > kalanlar[4]);
        assertEquals(Math.round(ayarlar.genelBeklemeTick() * (1 + 3 * ayarlar.siddetBeklemeCarpani())), kalanlar[5]);
    }

    @Test
    void carpanSonumundeBeklemeVeTekAfetKuraliBozulmaz() {
        CarpanListesi carpanlar = new CarpanListesi();
        carpanlar.ekle(new GeciciCarpan(AfetTuru.TSUNAMI, GeciciCarpan.EN_COK, 0, 200_000, 200_000, SonumTipi.USTEL));
        AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().carpanlar(carpanlar).afetSuresi(4000).kos(400_000);
        assertEquals(0, sonuc.ayniAnIhlali());
        assertTrue(sonuc.sayi(AfetTuru.TSUNAMI) > 0);
        List<AfetSimulasyonu.Olay> olaylar = sonuc.olaylar();
        for (int i = 1; i < olaylar.size(); i++) {
            AfetSimulasyonu.Olay onceki = olaylar.get(i - 1);
            AfetSimulasyonu.Olay olay = olaylar.get(i);
            assertTrue(olay.baslangic() >= onceki.bitis(), "iki afet üst üste bindi");
            assertTrue(olay.baslangic() - onceki.bitis() >= ZamanlayiciAyarlari.VARSAYILAN.genelBeklemeTick(),
                    "çarpan genel beklemeyi bozdu");
            if (olay.tur() == onceki.tur()) {
                assertTrue(olay.baslangic() - onceki.bitis() >= ZamanlayiciAyarlari.VARSAYILAN.turBekleme(olay.tur()),
                        "çarpan afet başına beklemeyi bozdu");
            }
        }
    }

    @Test
    void carpanTurunPayiniArttirir() {
        CarpanListesi carpanlar = new CarpanListesi();
        carpanlar.ekle(new GeciciCarpan(AfetTuru.TSUNAMI, 4, 0, 10_000_000, 1, SonumTipi.DOGRUSAL));
        AfetSimulasyonu.Sonuc normal = yogun().gun(1000).kos(1_000_000);
        AfetSimulasyonu.Sonuc artmis = yogun().gun(1000).carpanlar(carpanlar).kos(1_000_000);
        double payNormal = normal.sayi(AfetTuru.TSUNAMI) / (double) normal.toplam();
        double payArtmis = artmis.sayi(AfetTuru.TSUNAMI) / (double) artmis.toplam();
        assertTrue(payArtmis > payNormal * 2.5, "çarpan 4 tsunami payını belirgin artırmalı: " + payNormal + " -> "
                + payArtmis);
    }

    @Test
    void ayniAndaIkiAfetSecilmez() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN.gunlukOranIle(1_000_000).baslangicBeklemeIle(0);
        SplittableRandom rastgele = new SplittableRandom(3);
        Set<AfetTuru> hepsi = EnumSet.allOf(AfetTuru.class);
        for (int i = 0; i < 1000; i++) {
            assertTrue(AfetZamanlayicisi.sec(ayarlar, girdi(true, 100_000, new BeklemeDurumu(), hepsi, 100, false),
                    rastgele).isEmpty());
            assertTrue(AfetZamanlayicisi.sec(ayarlar, girdi(true, 100_000, new BeklemeDurumu(), hepsi, 100, true),
                    rastgele).isEmpty());
        }
        AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().gun(1000)
                .zamanlayici(ayarlar.genelBeklemeIle(0).turBeklemeleriIle(0)).afetSuresi(2000).kos(2_000_000);
        assertEquals(0, sonuc.ayniAnIhlali());
        assertTrue(sonuc.toplam() > 100);
    }

    @Test
    void secimTuruAdaySayisiniSikligaKatmaz() {
        // Gerileme bekçisi: SecimTuru (zamanlayıcı + hedef sıralaması) 1 ve 10 hedef adayıyla, her yer uygun, 10^6 tick.
        // Aday sayısı yalnız hedef kimliğini değiştirir; fark tohum gürültüsüdür. N14 ertelemesinin oyuncu sayısına bağlı
        // etkisi burada ölçülmez.
        AfetSimulasyonu.Sonuc bir = yogun().gun(1000).oyuncuSayisi(1).kos(1_000_000);
        AfetSimulasyonu.Sonuc on = yogun().gun(1000).oyuncuSayisi(10).kos(1_000_000);
        double beklenen = bir.gunlukOlay();
        assertTrue(bir.toplam() > 5000, "örnek çok küçük: " + bir.toplam());
        assertEquals(beklenen, on.gunlukOlay(), beklenen * 0.10,
                "1 ve 10 oyuncuda günlük afet: " + bir.gunlukOlay() + " / " + on.gunlukOlay());
        System.out.printf(Locale.ROOT, "1 oyuncu %d afet (%.2f/gün), 10 oyuncu %d afet (%.2f/gün)%n", bir.toplam(),
                bir.gunlukOlay(), on.toplam(), on.gunlukOlay());
    }

    @Test
    void zorlaBeklemeyiYokSayarAmaUygunlukFiltresiniKorur() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN;
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.DEPREM, 5, 0, ayarlar, 1.0);
        SplittableRandom rastgele = new SplittableRandom(4);
        Set<AfetTuru> kasirgasiz = EnumSet.complementOf(EnumSet.of(AfetTuru.KASIRGA));
        for (int i = 0; i < 2000; i++) {
            Optional<AfetTuru> secim = AfetZamanlayicisi.sec(ayarlar, girdi(false, 0, bekleme, kasirgasiz, 1, true),
                    rastgele);
            assertTrue(secim.isPresent());
            assertTrue(secim.get() != AfetTuru.KASIRGA, "uygun olmayan tür seçildi");
        }
        assertEquals(Optional.of(AfetTuru.DEPREM), AfetZamanlayicisi.sec(ayarlar,
                girdi(false, 0, bekleme, EnumSet.of(AfetTuru.DEPREM), 1, true), rastgele));
        assertTrue(AfetZamanlayicisi.sec(ayarlar, girdi(false, 0, bekleme, EnumSet.noneOf(AfetTuru.class), 1, true),
                rastgele).isEmpty());
    }

    @Test
    void afetSikligiSifirsaSecimYok() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN.baslangicBeklemeIle(0);
        AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().zamanlayici(ayarlar).gun(1000).siklik(0).kos(5_000_000);
        assertEquals(0, sonuc.toplam());
    }

    @Test
    void beklemeOlcekleSikligaBoler() {
        assertEquals(1_200, AfetZamanlayicisi.beklemeOlcekle(12_000, 10));
        assertEquals(24_000, AfetZamanlayicisi.beklemeOlcekle(12_000, 0.5));
        assertEquals(12_000, AfetZamanlayicisi.beklemeOlcekle(12_000, 1));
        assertEquals(12_000, AfetZamanlayicisi.beklemeOlcekle(12_000, 0), "sıklık 0'da süre değişmez");
    }

    @Test
    void onKatSiklikBaslangicBeklemesiniOndaBireIndirir() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN.gunlukOranIle(1_000_000);
        SplittableRandom rastgele = new SplittableRandom(5);
        Set<AfetTuru> hepsi = EnumSet.allOf(AfetTuru.class);
        long bitis = ayarlar.baslangicBeklemeTick() / 10;
        assertTrue(AfetZamanlayicisi.sec(ayarlar, new AfetZamanlayicisi.Girdi(false, bitis - 1, 20, new BeklemeDurumu(),
                hepsi, tur -> 1, 10, false), rastgele).isEmpty());
        assertTrue(AfetZamanlayicisi.sec(ayarlar, new AfetZamanlayicisi.Girdi(false, bitis, 20, new BeklemeDurumu(),
                hepsi, tur -> 1, 10, false), rastgele).isPresent());
    }

    /**
     * Kullanıcı şikayeti (2026-11-08): {@code afetSikligi} yalnız oranı çarpıyordu; başlangıç, genel ve tür beklemeleri sabit
     * kaldığı için %1000 gerçekte ~3 katıydı. 11.4 "200 = iki katı": varsayılan beklemelerle gerçekleşen afet sayısı sıklıkla
     * orantılı olmalı (afet süresi kısa tutuldu; afetin kendisi sıklıkla kısalmaz).
     */
    @Test
    void gerceklesenAfetSayisiSikliklaOrantili() {
        double normal = new AfetSimulasyonu().afetSuresi(20).gun(1000).kos(20_000_000).gunlukOlay();
        double iki = new AfetSimulasyonu().afetSuresi(20).gun(1000).siklik(2).kos(20_000_000).gunlukOlay();
        double on = new AfetSimulasyonu().afetSuresi(20).gun(1000).siklik(10).kos(20_000_000).gunlukOlay();
        System.out.printf(Locale.ROOT, "günlük afet: %%100 %.3f, %%200 %.3f, %%1000 %.3f%n", normal, iki, on);
        assertEquals(2.0, iki / normal, 0.3, "%200 iki katı olmalı: " + normal + " / " + iki);
        assertEquals(10.0, on / normal, 1.5, "%1000 on katı olmalı: " + normal + " / " + on);
    }

    @Test
    void siklikCarpaniOraniOlcekler() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN;
        double normal = AfetZamanlayicisi.turOrani(ayarlar, AfetTuru.KASIRGA, 1, 1);
        assertEquals(normal * 2, AfetZamanlayicisi.turOrani(ayarlar, AfetTuru.KASIRGA, 1, 2), 1e-12);
        assertEquals(normal * 3, AfetZamanlayicisi.turOrani(ayarlar, AfetTuru.KASIRGA, 3, 1), 1e-12);
        double toplam = 0;
        for (AfetTuru tur : AfetTuru.values()) {
            toplam += AfetZamanlayicisi.turOrani(ayarlar, tur, 1, 1);
        }
        assertEquals(ayarlar.gunlukAfetOrani(), toplam, 1e-9, "tüm türler uygunken toplam oran günlük orana eşit");
    }
}
