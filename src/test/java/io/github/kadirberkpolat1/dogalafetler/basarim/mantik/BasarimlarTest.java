package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Madde 12 listesi birebir: kök + 26 başarım + N29 "Yeni Şafak" (27), türler ve gizliler şartnamedeki gibi; K-E74'ün 18 yeni
 * başarımı (28–45) sona eklenir.
 */
class BasarimlarTest {
    private static final Map<Integer, BasarimTuru> TURLER = Map.ofEntries(
            Map.entry(0, BasarimTuru.GOREV), Map.entry(1, BasarimTuru.GOREV), Map.entry(2, BasarimTuru.GOREV),
            Map.entry(3, BasarimTuru.GOREV), Map.entry(4, BasarimTuru.GOREV), Map.entry(5, BasarimTuru.GOREV),
            Map.entry(6, BasarimTuru.GOREV), Map.entry(7, BasarimTuru.GOREV), Map.entry(8, BasarimTuru.HEDEF),
            Map.entry(9, BasarimTuru.HEDEF), Map.entry(10, BasarimTuru.HEDEF), Map.entry(11, BasarimTuru.MEYDAN_OKUMA),
            Map.entry(12, BasarimTuru.HEDEF), Map.entry(13, BasarimTuru.GOREV), Map.entry(14, BasarimTuru.GOREV),
            Map.entry(15, BasarimTuru.GOREV), Map.entry(16, BasarimTuru.GOREV), Map.entry(17, BasarimTuru.GOREV),
            Map.entry(18, BasarimTuru.HEDEF), Map.entry(19, BasarimTuru.GOREV), Map.entry(20, BasarimTuru.GOREV),
            Map.entry(21, BasarimTuru.HEDEF), Map.entry(22, BasarimTuru.MEYDAN_OKUMA), Map.entry(23, BasarimTuru.HEDEF),
            Map.entry(24, BasarimTuru.MEYDAN_OKUMA), Map.entry(25, BasarimTuru.MEYDAN_OKUMA),
            Map.entry(26, BasarimTuru.HEDEF), Map.entry(27, BasarimTuru.GOREV), Map.entry(28, BasarimTuru.GOREV),
            Map.entry(29, BasarimTuru.MEYDAN_OKUMA), Map.entry(30, BasarimTuru.HEDEF), Map.entry(31, BasarimTuru.GOREV),
            Map.entry(32, BasarimTuru.HEDEF), Map.entry(33, BasarimTuru.HEDEF), Map.entry(34, BasarimTuru.HEDEF),
            Map.entry(35, BasarimTuru.GOREV), Map.entry(36, BasarimTuru.GOREV), Map.entry(37, BasarimTuru.GOREV),
            Map.entry(38, BasarimTuru.GOREV), Map.entry(39, BasarimTuru.GOREV), Map.entry(40, BasarimTuru.HEDEF),
            Map.entry(41, BasarimTuru.GOREV), Map.entry(42, BasarimTuru.GOREV), Map.entry(43, BasarimTuru.GOREV),
            Map.entry(44, BasarimTuru.GOREV), Map.entry(45, BasarimTuru.GOREV));

    @Test
    void kokVeKirkBesBasarim() {
        assertEquals(46, Basarimlar.values().length);
        Set<Integer> numaralar = new HashSet<>();
        Set<String> yollar = new HashSet<>();
        for (Basarimlar b : Basarimlar.values()) {
            assertTrue(numaralar.add(b.no()), "tekrar eden numara " + b.no());
            assertTrue(yollar.add(b.yol()), "tekrar eden yol " + b.yol());
            assertTrue(b.yol().matches("[a-z_]+"), b.yol());
            assertEquals(TURLER.get(b.no()), b.tur(), b.yol());
        }
        assertEquals(46, numaralar.size());
        assertEquals(Basarimlar.KOK, Basarimlar.numarayla(0).orElseThrow());
        assertEquals("root", Basarimlar.KOK.yol());
        assertEquals("Felaket Başlıyor", Basarimlar.KOK.turkceAd());
    }

    @Test
    void ebeveynlerOnceTanimliKokTek() {
        Set<Basarimlar> gorulen = new HashSet<>();
        for (Basarimlar b : Basarimlar.values()) {
            if (b == Basarimlar.KOK) {
                assertNull(b.ebeveyn());
            } else {
                assertNotNull(b.ebeveyn(), b.yol());
                assertTrue(gorulen.contains(b.ebeveyn()), b.yol() + " ebeveyninden önce tanımlı");
            }
            gorulen.add(b);
        }
    }

    @Test
    void yalnizYirmiBesYirmiAltiVeAstronomGizli() {
        for (Basarimlar b : Basarimlar.values()) {
            assertEquals(b.no() == 25 || b.no() == 26 || b.no() == 29, b.gizli(), b.yol());
        }
        assertTrue(Basarimlar.ASTRONOM.gizli());
        assertEquals(Unvan.ASTRONOM, Basarimlar.ASTRONOM.unvan());
    }

    @Test
    void ozelAnahtarlarBasarimYoluylaCozulur() {
        int ozel = 0;
        for (Basarimlar b : Basarimlar.values()) {
            if (b.ozelMi()) {
                ozel++;
                assertEquals(b, Basarimlar.ozelAnahtardan(b.yol()).orElseThrow(), b.yol());
            } else {
                assertTrue(Basarimlar.ozelAnahtardan(b.yol()).isEmpty(), b.yol());
            }
        }
        assertEquals(13, ozel);
        assertTrue(Basarimlar.ozelAnahtardan("yok_boyle_bir_anahtar").isEmpty());
        assertTrue(Basarimlar.ozelAnahtardan(null).isEmpty());
        for (String anahtar : new String[] {"devasa", "astronom", "cayir_cayir", "sifon", "ince_buz_kirici",
                "yineleme_ikiz", "defter_ilk_acilis", "kolye_serin"}) {
            assertTrue(Basarimlar.ozelAnahtardan(anahtar).isPresent(), anahtar);
        }
    }

    @Test
    void meydanOkumalarUnvanVerir() {
        Set<Unvan> unvanlar = new HashSet<>();
        for (Basarimlar b : Basarimlar.values()) {
            if (b.tur() == BasarimTuru.MEYDAN_OKUMA) {
                assertNotNull(b.unvan(), b.yol());
                assertTrue(unvanlar.add(b.unvan()), "her meydan okumanın kendi ünvanı");
            } else {
                assertNull(b.unvan(), b.yol());
            }
        }
        assertEquals(Unvan.values().length, unvanlar.size());
        assertEquals(Unvan.FELAKET_USTASI, Basarimlar.FELAKET_USTASI.unvan());
    }

    @Test
    void metinlerVeIkonlarDolu() {
        for (Basarimlar b : Basarimlar.values()) {
            for (String metin : new String[] {b.ingilizceAd(), b.turkceAd(), b.ingilizceAciklama(), b.turkceAciklama()}) {
                assertFalse(metin.isBlank(), b.yol());
            }
            assertTrue(b.ikon().matches("[a-z_]+:[a-z_/]+"), b.ikon());
            if (b.ikon().startsWith("dogalafetler:")) {
                assertNotNull(b.ikonYedegi(), "mod eşyası ikonunun vanilla yedeği olmalı: " + b.yol());
                assertTrue(b.ikonYedegi().startsWith("minecraft:"), b.ikonYedegi());
            }
            assertEquals("advancements.dogalafetler." + b.yol() + ".title", b.adAnahtari());
            assertEquals("advancements.dogalafetler." + b.yol() + ".description", b.aciklamaAnahtari());
        }
    }

    @Test
    void unvanKimlikleri() {
        for (Unvan u : Unvan.values()) {
            assertEquals(u, Unvan.kimlikIle(u.kimlik().toUpperCase(java.util.Locale.ROOT)).orElseThrow());
            assertEquals("unvan.dogalafetler." + u.kimlik(), u.ceviriAnahtari());
        }
        assertTrue(Unvan.kimlikIle("yok").isEmpty());
    }
}
