package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Yiyecek tablosu: madde 14'ün dokuz yiyeceği ve sıcaklık-3'ün on iki yiyeceği; adları, yönleri, kademeleri, süreleri ve
 * kapları birebir.
 */
class YiyecekTablosuTest {
    private record Beklenen(String ad, Yon yon, Buyukluk buyukluk, int saniye, YiyecekTablosu.Kap kap) {
    }

    private static final List<Beklenen> MADDE_14 = List.of(
            new Beklenen("sicak_salep", Yon.ISINMA, Buyukluk.ORTA, 90, YiyecekTablosu.Kap.SISE),
            new Beklenen("sicak_cikolata", Yon.ISINMA, Buyukluk.ORTA, 90, YiyecekTablosu.Kap.SISE),
            new Beklenen("sicak_tavuk_corbasi", Yon.ISINMA, Buyukluk.BUYUK, 150, YiyecekTablosu.Kap.KASE),
            new Beklenen("atesli_guvec", Yon.ISINMA, Buyukluk.COK_BUYUK, 180, YiyecekTablosu.Kap.KASE),
            new Beklenen("isitici_cay", Yon.ISINMA, Buyukluk.KUCUK, 180, YiyecekTablosu.Kap.SISE),
            new Beklenen("ayran", Yon.SERINLIK, Buyukluk.ORTA, 90, YiyecekTablosu.Kap.SISE),
            new Beklenen("buzlu_karpuz", Yon.SERINLIK, Buyukluk.KUCUK, 60, YiyecekTablosu.Kap.YOK),
            new Beklenen("meyve_serbeti", Yon.SERINLIK, Buyukluk.BUYUK, 120, YiyecekTablosu.Kap.SISE),
            new Beklenen("kutup_baligi_corbasi", Yon.ISINMA, Buyukluk.BUYUK, 150, YiyecekTablosu.Kap.KASE));

    /** S3-06: altı serinletici, altı ısıtıcı; madde 14'ün ardına eklenir (kimlikler ve sıra sabit). */
    private static final List<Beklenen> SICAKLIK_3 = List.of(
            new Beklenen("maras_dondurmasi", Yon.SERINLIK, Buyukluk.BUYUK, 120, YiyecekTablosu.Kap.KASE),
            new Beklenen("soguk_elma_suyu", Yon.SERINLIK, Buyukluk.KUCUK, 90, YiyecekTablosu.Kap.SISE),
            new Beklenen("kar_helvasi", Yon.SERINLIK, Buyukluk.ORTA, 60, YiyecekTablosu.Kap.KASE),
            new Beklenen("soguk_komposto", Yon.SERINLIK, Buyukluk.ORTA, 120, YiyecekTablosu.Kap.SISE),
            new Beklenen("parlak_sorbe", Yon.SERINLIK, Buyukluk.COK_BUYUK, 150, YiyecekTablosu.Kap.KASE),
            new Beklenen("buzlu_kakao", Yon.SERINLIK, Buyukluk.ORTA, 90, YiyecekTablosu.Kap.SISE),
            new Beklenen("tarhana_corbasi", Yon.ISINMA, Buyukluk.BUYUK, 150, YiyecekTablosu.Kap.KASE),
            new Beklenen("kuzu_haslama", Yon.ISINMA, Buyukluk.ORTA, 150, YiyecekTablosu.Kap.KASE),
            new Beklenen("kabak_corbasi", Yon.ISINMA, Buyukluk.ORTA, 120, YiyecekTablosu.Kap.KASE),
            new Beklenen("sucuklu_yumurta", Yon.ISINMA, Buyukluk.ORTA, 90, YiyecekTablosu.Kap.KASE),
            new Beklenen("firinda_elma", Yon.ISINMA, Buyukluk.KUCUK, 90, YiyecekTablosu.Kap.YOK),
            new Beklenen("elma_cayi", Yon.ISINMA, Buyukluk.KUCUK, 120, YiyecekTablosu.Kap.SISE));

    @Test
    void tabloMadde14VeSicaklik3IleBirebirAyni() {
        List<Beklenen> hepsi = new ArrayList<>(MADDE_14);
        hepsi.addAll(SICAKLIK_3);
        assertEquals(hepsi.size(), YiyecekTablosu.KAYITLAR.size(), "dokuz + on iki yiyecek");
        for (int i = 0; i < hepsi.size(); i++) {
            Beklenen b = hepsi.get(i);
            YiyecekTablosu.Kayit k = YiyecekTablosu.KAYITLAR.get(i);
            assertEquals(b.ad(), k.ad());
            assertEquals("dogalafetler:" + b.ad(), k.tanim().tur(), "azalan verim türü eşya kimliğidir");
            assertEquals(b.yon(), k.tanim().yon(), b.ad());
            assertEquals(b.buyukluk(), k.tanim().buyukluk(), b.ad());
            assertEquals(b.saniye(), k.tanim().sureSaniye(), b.ad());
            assertEquals(b.saniye() * 20, k.tanim().sureTick(), b.ad());
            assertEquals(b.kap(), k.kap(), b.ad());
            assertEquals(b.ad().equals("atesli_guvec"), k.tanim().sicaktaGuclenir(), "yan etki yalnız Ateşli Güveç'te");
            assertTrue(k.besin() > 0 && k.doygunluk() > 0, b.ad());
        }
    }

    @Test
    void yeniYiyeceklerSogukSicakDengeli() {
        long serinlik = SICAKLIK_3.stream().filter(b -> b.yon() == Yon.SERINLIK).count();
        assertTrue(SICAKLIK_3.size() >= 10, "en az on yeni yiyecek");
        assertEquals(SICAKLIK_3.size() - serinlik, serinlik, "serinleten ve ısıtan sayısı eşit");
        assertTrue(SICAKLIK_3.stream().anyMatch(b -> b.yon() == Yon.SERINLIK && b.buyukluk() == Buyukluk.COK_BUYUK),
                "Ateşli Güveç'in serin karşılığı var");
    }

    @Test
    void sureler60Ile180SaniyeArasinda() {
        for (YiyecekTablosu.Kayit k : YiyecekTablosu.KAYITLAR) {
            assertTrue(k.tanim().sureSaniye() >= 60 && k.tanim().sureSaniye() <= 180, k.ad());
        }
    }

    @Test
    void tavukCorbasiYuksekDoyurur() {
        YiyecekTablosu.Kayit corba = YiyecekTablosu.al("sicak_tavuk_corbasi");
        for (YiyecekTablosu.Kayit k : YiyecekTablosu.KAYITLAR) {
            assertTrue(corba.besin() >= k.besin(), "en doyurucu: " + k.ad());
        }
        assertTrue(corba.besin() * corba.doygunluk() * 2 >= 16, "doygunluk yüksek");
    }

    @Test
    void adlarBenzersizVeKucukAscii() {
        Set<String> adlar = new HashSet<>();
        for (YiyecekTablosu.Kayit k : YiyecekTablosu.KAYITLAR) {
            assertTrue(adlar.add(k.ad()), "tekrar: " + k.ad());
            assertTrue(k.ad().matches("[a-z_]+"), k.ad());
        }
        assertThrows(IllegalArgumentException.class, () -> YiyecekTablosu.al("yok"));
    }

    @Test
    void kademeSeviyesiEfektAmplifierIcinSirali() {
        assertEquals(0, Buyukluk.COK_KUCUK.seviye());
        assertEquals(0, Buyukluk.KUCUK.seviye());
        assertEquals(1, Buyukluk.ORTA.seviye());
        assertEquals(2, Buyukluk.BUYUK.seviye());
        assertEquals(3, Buyukluk.COK_BUYUK.seviye());
        assertEquals("cok_buyuk", Buyukluk.COK_BUYUK.anahtar());
        assertEquals(1, Yon.ISINMA.isaret());
        assertEquals(-1, Yon.SERINLIK.isaret());
    }

    @Test
    void tanimDogrulanir() {
        assertThrows(IllegalArgumentException.class, () -> new YiyecekTanimi(" ", Yon.ISINMA, Buyukluk.ORTA, 90, false));
        assertThrows(IllegalArgumentException.class, () -> new YiyecekTanimi("x:y", Yon.ISINMA, Buyukluk.ORTA, 0, false));
        assertThrows(NullPointerException.class, () -> new YiyecekTanimi("x:y", null, Buyukluk.ORTA, 90, false));
        YiyecekTanimi balik = YiyecekTanimi.pismisBalik("dogalafetler:buz_alabaligi_pismis", 90);
        assertEquals(Yon.ISINMA, balik.yon());
        assertEquals(Buyukluk.ORTA, balik.buyukluk());
        assertFalse(balik.sicaktaGuclenir());
        YiyecekTanimi cig = YiyecekTanimi.cigBalik("dogalafetler:buz_alabaligi", 60);
        assertEquals(Yon.SERINLIK, cig.yon());
        assertEquals(Buyukluk.KUCUK, cig.buyukluk());
        assertEquals(1200, cig.sureTick());
    }
}
