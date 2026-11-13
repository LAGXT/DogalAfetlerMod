package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari.Kategori;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Madde 15: yaygın > orta > nadir, çöp düşük, hazine çok nadir; şans hazineyi artırır, çöpü azaltır; meteor yok. */
class LootAgirliklariTest {
    private final LootAgirliklari a = LootAgirliklari.VARSAYILAN;

    @Test
    void varsayilanSiraVeOranlar() {
        assertTrue(a.agirlik(Kategori.YAYGIN, 0) > a.agirlik(Kategori.ORTA, 0));
        assertTrue(a.agirlik(Kategori.ORTA, 0) > a.agirlik(Kategori.NADIR, 0));
        assertTrue(a.oran(Kategori.COP, 0) < 0.10, "çöp vanilla'dan (%10) düşük");
        assertTrue(a.oran(Kategori.HAZINE, 0) <= 0.02, "hazine çok nadir");
        double balik = a.oran(Kategori.YAYGIN, 0) + a.oran(Kategori.ORTA, 0) + a.oran(Kategori.NADIR, 0);
        assertTrue(balik >= 0.75, "ödül iyi: balık en az %75 (kalanı eşya, çöp, hazine)");
    }

    @Test
    void secimAgirliklaraUyar() {
        Random r = new Random(15);
        Map<Kategori, Integer> sayi = new EnumMap<>(Kategori.class);
        int n = 200_000;
        for (int i = 0; i < n; i++) {
            sayi.merge(a.sec(r.nextDouble(), 0), 1, Integer::sum);
        }
        for (Kategori k : Kategori.values()) {
            double beklenen = a.oran(k, 0);
            double gercek = sayi.getOrDefault(k, 0) / (double) n;
            assertEquals(beklenen, gercek, 0.005, k.name());
        }
    }

    @Test
    void uclarDogruKategoriyiVerir() {
        assertEquals(Kategori.YAYGIN, a.sec(0.0, 0));
        assertEquals(Kategori.ESYA, a.sec(0.999_999, 0), "özel kategori olmadan son dilim eşya");
        assertEquals(Kategori.OZEL, a.sec(0.999_999, 0, true), "Kutup Oltası'yla son dilim özel balık");
    }

    @Test
    void sansHazineyiArtirirCopuAzaltir() {
        assertTrue(a.oran(Kategori.HAZINE, 3) > a.oran(Kategori.HAZINE, 0));
        assertTrue(a.oran(Kategori.COP, 3) < a.oran(Kategori.COP, 0));
        assertEquals(0, a.agirlik(Kategori.COP, 100), "ağırlık eksiye düşmez");
        assertTrue(a.agirlik(Kategori.COP, -100) > a.agirlik(Kategori.COP, 0), "kötü şans çöpü artırır");
    }

    @Test
    void varsayilanOranlarTamDegerler() {
        // Kutup Oltası'sız toplam 118 (özel kategori havuzda değil).
        assertEquals(56 / 118.0, a.oran(Kategori.YAYGIN, 0), 1e-12);
        assertEquals(28 / 118.0, a.oran(Kategori.ORTA, 0), 1e-12);
        assertEquals(8 / 118.0, a.oran(Kategori.NADIR, 0), 1e-12);
        assertEquals(7 / 118.0, a.oran(Kategori.COP, 0), 1e-12);
        assertEquals(1 / 118.0, a.oran(Kategori.HAZINE, 0), 1e-12);
        assertEquals(18 / 118.0, a.oran(Kategori.ESYA, 0), 1e-12);
        assertEquals(0.0, a.oran(Kategori.OZEL, 0), 1e-12);
        // Kutup Oltası'yla özel ağırlık 14 eklenir (toplam 132).
        assertEquals(14 / 132.0, a.oran(Kategori.OZEL, 0, true), 1e-12);
        assertEquals(56 / 132.0, a.oran(Kategori.YAYGIN, 0, true), 1e-12);
        // Şans 3: çöp 7 − 6 = 1, hazine 1 + 3 = 4, toplam 115.
        assertEquals(1 / 115.0, a.oran(Kategori.COP, 3), 1e-12);
        assertEquals(4 / 115.0, a.oran(Kategori.HAZINE, 3), 1e-12);
    }

    @Test
    void hepsiSifirsaReddedilir() {
        assertThrows(IllegalArgumentException.class, () -> new LootAgirliklari(0, 0, 0, 0, 0, 0, 0, 2, 1));
        assertThrows(IllegalArgumentException.class, () -> new LootAgirliklari(-1, 1, 1, 1, 1, 1, 1, 2, 1));
        // Özel ağırlık tek başına toplamı pozitif yapmaz (yalnız Kutup Oltası'yla havuza girer).
        assertThrows(IllegalArgumentException.class, () -> new LootAgirliklari(0, 0, 0, 0, 0, 0, 5, 2, 1));
    }

    @Test
    void kategoriAdlariDosyaAdiDir() {
        assertEquals("yaygin", Kategori.YAYGIN.ad());
        assertEquals("hazine", Kategori.HAZINE.ad());
    }
}
