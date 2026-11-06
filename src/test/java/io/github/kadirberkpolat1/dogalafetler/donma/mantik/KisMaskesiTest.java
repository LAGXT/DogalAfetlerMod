package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class KisMaskesiTest {
    private static KisMaskesi varsayilan(long seed) {
        return new KisMaskesi(seed, KisMaskesi.VARSAYILAN_ORAN, KisMaskesi.VARSAYILAN_DALGA_BOYU,
                KisMaskesi.VARSAYILAN_BANT);
    }

    @Test
    void ayniSeedIkiAyriOrnekteAyniSonucuVerir() {
        KisMaskesi a = varsayilan(20261105L);
        KisMaskesi b = varsayilan(20261105L);
        Random r = new Random(7);
        for (int i = 0; i < 1000; i++) {
            int cx = r.nextInt(200_000) - 100_000;
            int cz = r.nextInt(200_000) - 100_000;
            assertEquals(a.agirlik(cx, cz), b.agirlik(cx, cz), 0.0, "chunk " + cx + "," + cz);
            assertEquals(a.kisMi(cx, cz), b.kisMi(cx, cz));
        }
    }

    @Test
    void farkliSeedFarkliMaskeVerir() {
        KisMaskesi a = varsayilan(1L);
        KisMaskesi b = varsayilan(2L);
        int farkli = 0;
        for (int i = 0; i < 1000; i++) {
            int cx = i * 37 - 18_000;
            int cz = i * 53 - 26_000;
            if (a.kisMi(cx, cz) != b.kisMi(cx, cz)) {
                farkli++;
            }
        }
        assertTrue(farkli > 150, "iki seed neredeyse aynı maskeyi verdi: " + farkli);
    }

    @Test
    void kisOraniBuyukOrneklemdeYuzde25ArtiEksi5() {
        long kis = 0;
        long toplam = 0;
        for (long seed : new long[] {1L, 2L, 3L, 20261105L}) {
            KisMaskesi m = varsayilan(seed);
            long seedKis = 0;
            for (int i = 0; i < 300; i++) {
                for (int j = 0; j < 300; j++) {
                    if (m.kisMi(i * 8 - 1200, j * 8 - 1200)) {
                        seedKis++;
                    }
                }
            }
            double seedOrani = seedKis / 90_000.0;
            assertEquals(0.25, seedOrani, 0.10, "seed " + seed + " oranı");
            kis += seedKis;
            toplam += 90_000;
        }
        assertEquals(0.25, kis / (double) toplam, 0.05);
    }

    @Test
    void oranAyarlanabilir() {
        for (double oran : new double[] {0.10, 0.50}) {
            KisMaskesi m = new KisMaskesi(5L, oran, KisMaskesi.VARSAYILAN_DALGA_BOYU, KisMaskesi.VARSAYILAN_BANT);
            long kis = 0;
            for (int i = 0; i < 300; i++) {
                for (int j = 0; j < 300; j++) {
                    if (m.kisMi(i * 8, j * 8)) {
                        kis++;
                    }
                }
            }
            assertEquals(oran, kis / 90_000.0, 0.06, "oran " + oran);
        }
    }

    @Test
    void agirlikSifirIleBirArasindaVeKisMiYariEsigiyleUyumlu() {
        KisMaskesi m = varsayilan(42L);
        for (int i = 0; i < 5000; i++) {
            int cx = i * 7 - 17_000;
            int cz = i * 11 - 27_000;
            double w = m.agirlik(cx, cz);
            assertTrue(w >= 0.0 && w <= 1.0, "ağırlık aralık dışı: " + w);
            assertEquals(w >= 0.5, m.kisMi(cx, cz));
        }
    }

    @Test
    void sinirYumusakGecisliKeskinSicramaYok() {
        KisMaskesi m = varsayilan(20261105L);
        double enBuyukSicrama = 0;
        long araChunk = 0;
        long araKosu = 0;
        for (int z = 0; z < 60; z++) {
            int cz = z * 40;
            boolean kosuda = false;
            double onceki = m.agirlik(-1000, cz);
            for (int cx = -999; cx < 1000; cx++) {
                double w = m.agirlik(cx, cz);
                enBuyukSicrama = Math.max(enBuyukSicrama, Math.abs(w - onceki));
                boolean ara = w > 0.02 && w < 0.98;
                if (ara) {
                    araChunk++;
                    if (!kosuda) {
                        araKosu++;
                    }
                }
                kosuda = ara;
                onceki = w;
            }
        }
        assertTrue(enBuyukSicrama < 0.9, "komşu chunk'lar arasında keskin sıçrama: " + enBuyukSicrama);
        assertTrue(araKosu > 0, "hiç geçiş kuşağı yok");
        double ortalamaGenislik = araChunk / (double) araKosu;
        assertTrue(ortalamaGenislik >= 3.0, "geçiş kuşağı çok dar: " + ortalamaGenislik + " chunk");
    }
}
