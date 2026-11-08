package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class SecimTuruTest {
    private static final ZamanlayiciAyarlari A = ZamanlayiciAyarlari.VARSAYILAN;
    private static final long SAAT = 1_000_000;

    private static AfetZamanlayicisi.Girdi zorla(BeklemeDurumu bekleme, Set<AfetTuru> uygun) {
        return new AfetZamanlayicisi.Girdi(false, SAAT, 20, bekleme, uygun, tur -> 1.0, 1.0, true);
    }

    private static List<HedefSecici.Aday<String>> adaylar(String... adlar) {
        return java.util.Arrays.stream(adlar).map(ad -> new HedefSecici.Aday<>(ad, false)).toList();
    }

    @Test
    void hedefAdayiYokkenYalnizSunucuGeneliAfetSecilir() {
        SplittableRandom rastgele = new SplittableRandom(1);
        int sunucuGeneli = 0;
        for (int i = 0; i < 2000; i++) {
            SecimTuru.Sonuc<String> sonuc = SecimTuru.isle(A, zorla(new BeklemeDurumu(), EnumSet.allOf(AfetTuru.class)),
                    List.of(), null, (tur, hedef) -> SecimTuru.Deneme.BASLADI, rastgele);
            assertTrue(sonuc.basladi());
            assertTrue(sonuc.tur().sunucuGeneli(), "hedef yokken bölgesel afet seçildi: " + sonuc.tur());
            assertNull(sonuc.hedef());
            sunucuGeneli++;
        }
        assertEquals(2000, sunucuGeneli);
        SecimTuru.Sonuc<String> bolgesel = SecimTuru.isle(A,
                zorla(new BeklemeDurumu(), EnumSet.of(AfetTuru.DEPREM, AfetTuru.KASIRGA)), List.of(), null,
                (tur, hedef) -> SecimTuru.Deneme.BASLADI, rastgele);
        assertTrue(!bolgesel.basladi() && bolgesel.ertelenenler().isEmpty(), "hedefsiz bölgesel afet başlamamalı");
    }

    @Test
    void yerUygunDegilseHedeflerDenenirSonraTurErtelenirVeSiradakiSecilir() {
        SplittableRandom rastgele = new SplittableRandom(2);
        boolean ertelemeGoruldu = false;
        for (int i = 0; i < 200; i++) {
            BeklemeDurumu bekleme = new BeklemeDurumu();
            Map<AfetTuru, Integer> denemeler = new EnumMap<>(AfetTuru.class);
            SecimTuru.Sonuc<String> sonuc = SecimTuru.isle(A, zorla(bekleme, EnumSet.of(AfetTuru.DEPREM, AfetTuru.KASIRGA)),
                    adaylar("a", "b", "c"), null, (tur, hedef) -> {
                        denemeler.merge(tur, 1, Integer::sum);
                        return tur != AfetTuru.DEPREM ? SecimTuru.Deneme.BASLADI : SecimTuru.Deneme.YER_YOK;
                    }, rastgele);
            assertEquals(AfetTuru.KASIRGA, sonuc.tur(), "uygun yeri olan tür başlamalı");
            if (sonuc.ertelenenler().contains(AfetTuru.DEPREM)) {
                ertelemeGoruldu = true;
                assertEquals(3, denemeler.get(AfetTuru.DEPREM), "üç adayın hepsi denenmeli (N14)");
                assertTrue(bekleme.turBeklemede(AfetTuru.DEPREM, SAAT + A.ertelemeTick() - 1));
                assertTrue(!bekleme.turBeklemede(AfetTuru.DEPREM, SAAT + A.ertelemeTick()));
            }
        }
        assertTrue(ertelemeGoruldu, "deprem hiç önce seçilmedi");
    }

    @Test
    void hicbirTurBaslayamazsaHepsiErtelenir() {
        BeklemeDurumu bekleme = new BeklemeDurumu();
        SecimTuru.Sonuc<String> sonuc = SecimTuru.isle(A, zorla(bekleme, EnumSet.of(AfetTuru.DEPREM, AfetTuru.KASIRGA)),
                adaylar("a"), null, (tur, hedef) -> SecimTuru.Deneme.YER_YOK, new SplittableRandom(3));
        assertTrue(!sonuc.basladi());
        assertEquals(EnumSet.of(AfetTuru.DEPREM, AfetTuru.KASIRGA), sonuc.ertelenenler());
    }

    @Test
    void sonHedefArtArdaSecilmez() {
        SplittableRandom rastgele = new SplittableRandom(4);
        for (int i = 0; i < 500; i++) {
            SecimTuru.Sonuc<String> sonuc = SecimTuru.isle(A, zorla(new BeklemeDurumu(), EnumSet.of(AfetTuru.DEPREM)),
                    adaylar("a", "b"), "a", (tur, hedef) -> SecimTuru.Deneme.BASLADI, rastgele);
            assertEquals("b", sonuc.hedef());
        }
    }

    @Test
    void afetAktifkenHicbirSeyDenenmez() {
        AfetZamanlayicisi.Girdi girdi = new AfetZamanlayicisi.Girdi(true, SAAT, 20, new BeklemeDurumu(),
                EnumSet.allOf(AfetTuru.class), tur -> 1.0, 1.0, true);
        int[] cagri = new int[1];
        SecimTuru.Sonuc<String> sonuc = SecimTuru.isle(A, girdi, adaylar("a"), null, (tur, hedef) -> {
            cagri[0]++;
            return SecimTuru.Deneme.BASLADI;
        }, new SplittableRandom(5));
        assertTrue(!sonuc.basladi());
        assertEquals(0, cagri[0]);
    }

    @Test
    void cakisanTurErtelenmezSiradakiDenenir() {
        SplittableRandom rastgele = new SplittableRandom(6);
        boolean cakismaGoruldu = false;
        for (int i = 0; i < 200; i++) {
            BeklemeDurumu bekleme = new BeklemeDurumu();
            boolean[] depremDenendi = new boolean[1];
            SecimTuru.Sonuc<String> sonuc = SecimTuru.isle(A, zorla(bekleme, EnumSet.of(AfetTuru.DEPREM, AfetTuru.KASIRGA)),
                    adaylar("a", "b"), null, (tur, hedef) -> {
                        if (tur == AfetTuru.DEPREM) {
                            depremDenendi[0] = true;
                            return SecimTuru.Deneme.CAKISTI;
                        }
                        return SecimTuru.Deneme.BASLADI;
                    }, rastgele);
            assertEquals(AfetTuru.KASIRGA, sonuc.tur(), "çakışmayan tür başlamalı");
            assertTrue(sonuc.ertelenenler().isEmpty(), "çakışan tür ertelenmemeli");
            assertTrue(!bekleme.turBeklemede(AfetTuru.DEPREM, SAAT), "çakışan tür beklemeye girmemeli");
            cakismaGoruldu |= depremDenendi[0];
        }
        assertTrue(cakismaGoruldu, "deprem hiç denenmedi");
        BeklemeDurumu bekleme = new BeklemeDurumu();
        SecimTuru.Sonuc<String> hepsi = SecimTuru.isle(A, zorla(bekleme, EnumSet.of(AfetTuru.DEPREM, AfetTuru.KASIRGA)),
                adaylar("a"), null, (tur, hedef) -> SecimTuru.Deneme.CAKISTI, rastgele);
        assertTrue(!hepsi.basladi() && hepsi.ertelenenler().isEmpty(), "hepsi çakışınca tur boş biter, erteleme yok");
    }
}
