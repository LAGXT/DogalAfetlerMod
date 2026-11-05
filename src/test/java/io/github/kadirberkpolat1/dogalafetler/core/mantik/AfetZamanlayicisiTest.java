package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class AfetZamanlayicisiTest {
    private static final List<AfetTuru> SIK_ALTI = List.of(AfetTuru.KASIRGA, AfetTuru.TSUNAMI, AfetTuru.DEPREM,
            AfetTuru.ASIT_YAGMURU, AfetTuru.SINKHOLE, AfetTuru.ORMAN_YANGINI);

    private static AfetZamanlayicisi.Girdi girdi(boolean aktif, long saat, BeklemeDurumu bekleme,
            Set<AfetTuru> uygun, double carpan, boolean zorla) {
        return new AfetZamanlayicisi.Girdi(aktif, saat, AfetSimulasyonu.ADIM, bekleme, uygun, tur -> carpan, 1.0,
                zorla);
    }

    @Test
    void dagilimNadirlikSirasiniKorurVeHicbirAfetSifirKalmaz() {
        // Yoğun ayar: bekleme yok, günde 240 olay oranı. 10^6 tick'te ~7 700 afet; sıra istatistiksel olarak sağlam.
        ZamanlayiciAyarlari yogun = ZamanlayiciAyarlari.VARSAYILAN.gunlukOranIle(240).genelBeklemeIle(0)
                .turBeklemeleriIle(0).baslangicBeklemeIle(0);
        AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().zamanlayici(yogun).afetSuresi(20).kos(1_000_000);

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
    void varsayilanAyarlarlaSiklikMakulVeHerAfetGorulur() {
        AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().afetSuresi(6000).kos(10_000_000);
        double gunluk = sonuc.gunlukOlay();
        assertTrue(gunluk > 0.25 && gunluk < 1.0, "günlük afet sayısı makul aralıkta değil: " + gunluk);
        for (AfetTuru tur : AfetTuru.values()) {
            assertTrue(sonuc.sayi(tur) > 0, tur + " 417 günde hiç çıkmadı: " + sonuc.sayilar());
        }
        // İlk afetten sonraki her afet en az genel bekleme kadar sonra başlar.
        List<AfetSimulasyonu.Olay> olaylar = sonuc.olaylar();
        for (int i = 1; i < olaylar.size(); i++) {
            long ara = olaylar.get(i).baslangic() - olaylar.get(i - 1).bitis();
            assertTrue(ara >= ZamanlayiciAyarlari.VARSAYILAN.genelBeklemeTick(), "bekleme bozuldu: " + ara);
        }
    }

    @Test
    void minimumBeklemeIcindeSecimYok() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN.gunlukOranIle(1_000_000).baslangicBeklemeIle(0);
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.KASIRGA, 1, 1000, ayarlar);
        long bitis = 1000 + ayarlar.genelBeklemeTick();
        SplittableRandom rastgele = new SplittableRandom(1);
        Set<AfetTuru> hepsi = EnumSet.allOf(AfetTuru.class);
        for (long saat = 1000; saat < bitis; saat += AfetSimulasyonu.ADIM) {
            assertTrue(AfetZamanlayicisi.sec(ayarlar, girdi(false, saat, bekleme, hepsi, 1000, false), rastgele)
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
            bekleme.afetBitti(AfetTuru.DEPREM, seviye, 0, ayarlar);
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
        ZamanlayiciAyarlari yogun = ZamanlayiciAyarlari.VARSAYILAN.gunlukOranIle(240).genelBeklemeIle(0)
                .turBeklemeleriIle(0).baslangicBeklemeIle(0);
        AfetSimulasyonu.Sonuc normal = new AfetSimulasyonu().zamanlayici(yogun).afetSuresi(20).kos(1_000_000);
        AfetSimulasyonu.Sonuc artmis = new AfetSimulasyonu().zamanlayici(yogun).carpanlar(carpanlar).afetSuresi(20)
                .kos(1_000_000);
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
            assertTrue(AfetZamanlayicisi.sec(ayarlar, girdi(true, 100_000, new BeklemeDurumu(), hepsi, 1000, false),
                    rastgele).isEmpty());
            assertTrue(AfetZamanlayicisi.sec(ayarlar, girdi(true, 100_000, new BeklemeDurumu(), hepsi, 1000, true),
                    rastgele).isEmpty());
        }
        AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().zamanlayici(ayarlar.genelBeklemeIle(0).turBeklemeleriIle(0))
                .afetSuresi(2000).kos(2_000_000);
        assertEquals(0, sonuc.ayniAnIhlali());
    }

    @Test
    void oyuncuSayisindanBagimsizSiklik() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN.baslangicBeklemeIle(0);
        AfetSimulasyonu.Sonuc bir = new AfetSimulasyonu().zamanlayici(ayarlar).oyuncuSayisi(1).tohum(11).kos(100_000_000);
        AfetSimulasyonu.Sonuc on = new AfetSimulasyonu().zamanlayici(ayarlar).oyuncuSayisi(10).tohum(12).kos(100_000_000);
        double beklenen = bir.gunlukOlay();
        assertTrue(bir.toplam() > 1000, "örnek çok küçük: " + bir.toplam());
        assertEquals(beklenen, on.gunlukOlay(), beklenen * 0.10,
                "1 ve 10 oyuncuda günlük afet: " + bir.gunlukOlay() + " / " + on.gunlukOlay());
    }

    @Test
    void zorlaBeklemeyiYokSayarAmaUygunlukFiltresiniKorur() {
        ZamanlayiciAyarlari ayarlar = ZamanlayiciAyarlari.VARSAYILAN;
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.DEPREM, 5, 0, ayarlar);
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
        AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().zamanlayici(ayarlar).siklik(0).kos(5_000_000);
        assertEquals(0, sonuc.toplam());
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

    @Test
    void tekSeferlikOlaylarGerceklesinceBirDahaSecilmez() {
        ZamanlayiciAyarlari yogun = ZamanlayiciAyarlari.VARSAYILAN.gunlukOranIle(240).genelBeklemeIle(0)
                .turBeklemeleriIle(0).baslangicBeklemeIle(0);
        TekSeferlikAyarlari tek = TekSeferlikAyarlari.VARSAYILAN;
        Set<AfetTuru> uygun = EnumSet.noneOf(AfetTuru.class);
        for (AfetTuru tur : AfetTuru.values()) {
            // Ay yok ve kızıl cüce dönemine ulaşılmış; gün ve gelişim eşikleri aşılmış.
            if (TekSeferlikKurallar.uygunMu(tur, false, io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi.KIZIL_CUCE,
                    500, 100, tek)) {
                uygun.add(tur);
            }
        }
        AfetSimulasyonu.Sonuc sonuc = new AfetSimulasyonu().zamanlayici(yogun).uygunTurler(uygun).afetSuresi(20)
                .kos(1_000_000);
        assertEquals(0, sonuc.sayi(AfetTuru.AY_CARPMASI));
        assertEquals(0, sonuc.sayi(AfetTuru.GUNES_PATLAMASI));
        for (AfetTuru tur : SIK_ALTI) {
            assertTrue(sonuc.sayi(tur) > 0, tur + " her dönemde devam etmeli");
        }
    }
}
