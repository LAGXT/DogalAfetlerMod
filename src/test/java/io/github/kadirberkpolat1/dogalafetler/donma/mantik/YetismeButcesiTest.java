package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/** D2-2: kıyı BFS'i bütçeye sütundan önce girer; süre denetimi her işten önce. */
class YetismeButcesiTest {
    private final YetismeButcesi b = new YetismeButcesi(256, 8, 4_000_000L);

    @Test
    void bfsBekleyenSutununRezerviBfsDahil() {
        assertEquals(b.rezerv(5) + YetismeButcesi.BFS_MALIYETI, b.sutunRezervi(5, true));
        assertEquals(b.rezerv(5), b.sutunRezervi(5, false));
        int kalan = b.rezerv(5) + YetismeButcesi.BFS_MALIYETI - 1;
        assertFalse(b.sutunSigar(kalan, b.sutunRezervi(5, true)), "BFS'i karşılamayan birimle sütun başlamaz");
        assertTrue(b.sutunSigar(kalan, b.sutunRezervi(5, false)), "BFS bittiyse aynı birim yeter");
    }

    /**
     * Zamanlayıcının döngüsü: sütun ancak rezervi (BFS dahil) sığarsa işlenir, BFS ilk su sütununda tetiklenir. 2000 tick,
     * üç kısma değerinde birim hiç eksiye düşmez ve tick'in blok sayısı sınırı aşmaz.
     */
    @Test
    void bfsDahilBirimVeBlokSiniriHicAsilmaz() {
        int enCok = 5;
        Random r = new Random(7);
        for (double kisma : new double[] {1.0, 0.5, 0.2}) {
            int birim = b.birim(kisma);
            boolean bfsYapildi = false;
            boolean suluChunk = true;
            int sutun = 0;
            for (int tick = 0; tick < 2000; tick++) {
                int kalan = birim;
                int blok = 0;
                while (b.sutunSigar(kalan, Math.min(birim, b.sutunRezervi(enCok, !bfsYapildi)))) {
                    int degisen = r.nextInt(enCok + 1);
                    kalan -= b.sutunMaliyeti(degisen);
                    blok += degisen;
                    if (!bfsYapildi && suluChunk && r.nextInt(8) == 0) {
                        kalan -= YetismeButcesi.BFS_MALIYETI;
                        bfsYapildi = true;
                    }
                    assertTrue(kalan >= 0, "birim eksiye düştü: kısma " + kisma + " tick " + tick);
                    if (++sutun == 256) {
                        sutun = 0;
                        bfsYapildi = false;
                        suluChunk = r.nextBoolean();
                    }
                }
                assertTrue(blok <= b.blokSiniri(kisma), "blok sınırı aşıldı: " + blok);
            }
        }
    }

    @Test
    void sureDenetimiVeAsimDusumu() {
        long sinir = 1_000_000L + b.sureSiniriNs(0);
        assertTrue(b.sureVar(sinir - 1, sinir));
        assertFalse(b.sureVar(sinir, sinir), "sınırda yeni iş başlamaz");
        assertEquals(4_000_000L, b.sureSiniriNs(0));
        assertEquals(3_000_000L, b.sureSiniriNs(1_000_000L));
        assertEquals(1_000_000L, b.sureSiniriNs(70_000_000L), "ısınma aşımından sonra tavanın dörtte biri kalır");
    }
}
