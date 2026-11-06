package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TazelemePlaniTest {
    private static long a(int cx, int cz) {
        return DenizAlani.anahtar(cx, cz);
    }

    /** {@code (0,0)} merkezli 9x9 chunk. */
    private static long[] kare9() {
        long[] s = new long[81];
        int i = 0;
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                s[i++] = a(x, z);
            }
        }
        return s;
    }

    private static Set<Long> kume(long[] dizi) {
        Set<Long> s = new HashSet<>();
        for (long k : dizi) {
            s.add(k);
        }
        return s;
    }

    @Test
    void yakinHalkaHerPeriyottaTamamenSecilir() {
        TazelemePlani plan = new TazelemePlani(2, 20, 4);
        long[] odak = {a(0, 0)};
        for (long t = 0; t <= 100; t += 20) {
            Set<Long> secilen = kume(plan.sec(t, odak, kare9()));
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    assertTrue(secilen.contains(a(x, z)), "t=" + t + " yakın (" + x + "," + z + ") seçilmedi");
                }
            }
            assertEquals(25 + 4, secilen.size(), "yakın halka + uzak bütçe");
        }
    }

    @Test
    void periyotDolmadanBosDoner() {
        TazelemePlani plan = new TazelemePlani(2, 20, 4);
        long[] odak = {a(0, 0)};
        assertTrue(plan.sec(100, odak, kare9()).length > 0);
        assertEquals(0, plan.sec(105, odak, kare9()).length);
        assertEquals(0, plan.sec(119, odak, kare9()).length);
        assertTrue(plan.sec(120, odak, kare9()).length > 0);
        assertTrue(plan.sec(50, odak, kare9()).length > 0, "zaman geri giderse (yeni dünya) yeniden başlar");
    }

    @Test
    void uzakChunklarButceyleDonusumluHepsiKapsanir() {
        TazelemePlani plan = new TazelemePlani(2, 20, 4);
        long[] odak = {a(0, 0)};
        Set<Long> uzak = new HashSet<>();
        int periyot = 0;
        while (uzak.size() < 81 - 25 && periyot < 100) {
            for (long k : plan.sec(periyot * 20L, odak, kare9())) {
                int cx = (int) (k >> 32);
                int cz = (int) k;
                if (Math.abs(cx) > 2 || Math.abs(cz) > 2) {
                    uzak.add(k);
                }
            }
            periyot++;
        }
        assertEquals(56, uzak.size());
        assertEquals(14, periyot, "56 uzak chunk / 4 bütçe = 14 periyot");
    }

    @Test
    void istenmeyenChunkSecilmez() {
        TazelemePlani plan = new TazelemePlani(2, 20, 4);
        long[] istenen = {a(0, 0), a(1, 0), a(9, 9)};
        Set<Long> secilen = kume(plan.sec(0, new long[] {a(0, 0)}, istenen));
        assertEquals(Set.of(a(0, 0), a(1, 0), a(9, 9)), secilen);
        assertEquals(0, plan.sec(20, new long[] {a(0, 0)}, new long[0]).length);
    }

    @Test
    void birdenCokOdakYakinHalkalariBirlestirir() {
        TazelemePlani plan = new TazelemePlani(1, 20, 0);
        long[] odak = {a(-3, 0), a(3, 0)};
        Set<Long> secilen = kume(plan.sec(0, odak, kare9()));
        assertEquals(18, secilen.size());
        assertTrue(secilen.contains(a(-4, -1)));
        assertTrue(secilen.contains(a(4, 1)));
    }
}
