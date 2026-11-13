package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class KisAlaniTest {
    private static final long SEED = 20261105L;

    /** Chunk başına bağımsız rastgele soğuk/ılık: en kötü durum, her chunk kenarı olası bir biyom sınırı. */
    private static KisAlani.ChunkOrnegi rastgeleBiyom(long tuz, double sogukOrani) {
        return new KisAlani.ChunkOrnegi() {
            private long h(int cx, int cz) {
                return KisMaskesi.karistir(tuz ^ (cx * 0x9E3779B97F4A7C15L) ^ KisMaskesi.karistir(cz * 0xC2B2AE3D27D4EB4FL));
            }

            @Override
            public boolean soguk(int cx, int cz) {
                return (h(cx, cz) >>> 11) * 0x1.0p-53 < sogukOrani;
            }

            @Override
            public double sicaklik(int cx, int cz) {
                return ((h(cx, cz) >>> 20) & 0xFF) / 255.0 * 2.0;
            }
        };
    }

    private static KisAlani alan(KisAlani.ChunkOrnegi ornek) {
        return new KisAlani(new KisMaskesi(SEED, KisMaskesi.VARSAYILAN_ORAN, KisMaskesi.VARSAYILAN_DALGA_BOYU,
                KisMaskesi.VARSAYILAN_BANT), ornek, KisAlani.Ayar.VARSAYILAN);
    }

    private static double w(KisAlani a, int x, int z, int y) {
        return a.chunk(x >> 4, z >> 4).agirlik(x, z, y);
    }

    @Test
    void komsuSutunlarArasiAgirlikFarkiEnCok005() {
        KisAlani a = alan(rastgeleBiyom(7L, 0.5));
        KisMaskesi m = new KisMaskesi(SEED, KisMaskesi.VARSAYILAN_ORAN, KisMaskesi.VARSAYILAN_DALGA_BOYU,
                KisMaskesi.VARSAYILAN_BANT);
        KisAlani.Ayar ayar = KisAlani.Ayar.VARSAYILAN;
        Random r = new Random(11);
        double enBuyuk = 0;
        int maskeSiniri = 0;
        for (int deneme = 0; deneme < 10_000; deneme++) {
            int x = r.nextInt(400_000) - 200_000;
            int z = r.nextInt(400_000) - 200_000;
            if (deneme % 2 == 0) {
                // Maske sınırındaki chunk'ı ara (ağırlık 0 ile 1 arası).
                for (int i = 0; i < 400; i++) {
                    double wm = m.agirlik(x >> 4, z >> 4);
                    if (wm > 0.02 && wm < 0.98) {
                        maskeSiniri++;
                        break;
                    }
                    x = r.nextInt(400_000) - 200_000;
                    z = r.nextInt(400_000) - 200_000;
                }
            }
            int y1 = ayar.yAlt() - 10 + r.nextInt(ayar.yUst() - ayar.yAlt() + 20);
            int y2 = y1 + r.nextInt(3) - 1;
            boolean xYonu = r.nextBoolean();
            int x2 = xYonu ? x + 1 : x;
            int z2 = xYonu ? z : z + 1;
            double fark = Math.abs(w(a, x, z, y1) - w(a, x2, z2, y2));
            enBuyuk = Math.max(enBuyuk, fark);
        }
        assertTrue(maskeSiniri > 2000, "maske sınırı örneklenemedi: " + maskeSiniri);
        assertTrue(enBuyuk <= 0.05, "komşu sütunlar arasında ağırlık sıçraması: " + enBuyuk);
    }

    @Test
    void chunkSinirindaSurekli() {
        KisAlani a = alan(rastgeleBiyom(3L, 0.5));
        for (int cx = -50; cx < 50; cx++) {
            int x = cx * 16 + 15;
            double sol = w(a, x, 37, 60);
            double sag = w(a, x + 1, 37, 60);
            assertTrue(Math.abs(sol - sag) <= 0.05, "chunk sınırında sıçrama x=" + x + ": " + sol + " / " + sag);
        }
    }

    @Test
    void yukseklikAgirligiUstSinirdaTam() {
        KisAlani.Ayar ayar = KisAlani.Ayar.VARSAYILAN;
        assertEquals(0.0, KisAlani.yumusakBasamak(ayar.yAlt(), ayar.yUst(), ayar.yAlt()), 0.0);
        assertEquals(1.0, KisAlani.yumusakBasamak(ayar.yAlt(), ayar.yUst(), ayar.yUst()), 0.0);
        assertEquals(1.0, KisAlani.yumusakBasamak(ayar.yAlt(), ayar.yUst(), 200), 0.0);
        assertEquals(120, ayar.yUst(), "şartname: Y=120 üstü kalıcı kış");
        assertTrue(1.5 / (ayar.yUst() - ayar.yAlt()) <= 0.05, "1 bloklu yükseklik farkı 0,05'ten büyük sıçratır");
    }

    @Test
    void tamamenSogukCevredeAgirlikBirIlikCevredeSifir() {
        KisAlani.ChunkOrnegi soguk = new KisAlani.ChunkOrnegi() {
            @Override
            public boolean soguk(int cx, int cz) {
                return true;
            }

            @Override
            public double sicaklik(int cx, int cz) {
                return -0.5;
            }
        };
        KisAlani.ChunkOrnegi ilik = new KisAlani.ChunkOrnegi() {
            @Override
            public boolean soguk(int cx, int cz) {
                return false;
            }

            @Override
            public double sicaklik(int cx, int cz) {
                return 2.0;
            }
        };
        KisAlani s = alan(soguk);
        KisAlani i = alan(ilik);
        KisMaskesi m = new KisMaskesi(SEED, KisMaskesi.VARSAYILAN_ORAN, KisMaskesi.VARSAYILAN_DALGA_BOYU,
                KisMaskesi.VARSAYILAN_BANT);
        int bulunan = 0;
        for (int cx = -2000; cx < 2000 && bulunan < 50; cx += 7) {
            if (m.agirlikNoktada(cx * 16, 48) == 0.0 && m.agirlikNoktada(cx * 16 + 16, 48) == 0.0
                    && m.agirlikNoktada(cx * 16, 64) == 0.0 && m.agirlikNoktada(cx * 16 + 16, 64) == 0.0) {
                int x = cx * 16 + 5;
                assertEquals(1.0, w(s, x, 3 * 16 + 5, 64), 1e-9);
                assertEquals(0.0, w(i, x, 3 * 16 + 5, 64), 1e-9);
                assertEquals(1.0, w(i, x, 3 * 16 + 5, 130), 1e-9, "Y>120 ılık bölgede de kış");
                assertEquals(KisAlani.Ayar.VARSAYILAN.ofsetEnCok(), i.chunk(cx, 3).ofset(x, 3 * 16 + 5), 1e-9);
                assertEquals(0.0, s.chunk(cx, 3).ofset(x, 3 * 16 + 5), 1e-9);
                bulunan++;
            }
        }
        assertTrue(bulunan > 10, "maskesiz bölge bulunamadı");
    }

    @Test
    void ayniGirdiAyniSonuc() {
        KisAlani a = alan(rastgeleBiyom(5L, 0.3));
        KisAlani b = alan(rastgeleBiyom(5L, 0.3));
        Random r = new Random(2);
        for (int i = 0; i < 1000; i++) {
            int x = r.nextInt(100_000) - 50_000;
            int z = r.nextInt(100_000) - 50_000;
            assertEquals(w(a, x, z, 100), w(b, x, z, 100), 0.0);
        }
    }
}
