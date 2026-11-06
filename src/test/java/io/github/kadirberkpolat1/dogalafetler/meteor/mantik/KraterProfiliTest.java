package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class KraterProfiliTest {
    @Test
    void derinlikMerkezdeEnCokKenardaBirDisaridaSifir() {
        assertEquals(4, KraterProfili.derinlik(0, 5.5, 4));
        assertEquals(1, KraterProfili.derinlik(5.4, 5.5, 4));
        assertEquals(0, KraterProfili.derinlik(5.6, 5.5, 4));
        assertEquals(2, KraterProfili.derinlik(0, 2.5, 2));
        int onceki = Integer.MAX_VALUE;
        for (double r = 0; r <= 5.5; r += 0.25) {
            int d = KraterProfili.derinlik(r, 5.5, 4);
            assertTrue(d <= onceki, "merkezden uzaklaştıkça derinlik artmamalı");
            onceki = d;
        }
    }

    @Test
    void buyukKraterKucuktenGenisVeDerin() {
        KraterProfili.Plan kucuk = KraterProfili.planla(2.5, 2, 1L, 2, true);
        KraterProfili.Plan buyuk = KraterProfili.planla(5.5, 4, 1L, 2, true);
        assertTrue(buyuk.kazi().size() > 3 * kucuk.kazi().size(), kucuk.kazi().size() + " / " + buyuk.kazi().size());
        int enDerinK = kucuk.kazi().stream().mapToInt(KraterProfili.Hucre::dy).min().orElse(0);
        int enDerinB = buyuk.kazi().stream().mapToInt(KraterProfili.Hucre::dy).min().orElse(0);
        assertEquals(-1, enDerinK, "küçük krater merkezde 2 blok (yüzey + 1)");
        assertEquals(-3, enDerinB, "büyük krater merkezde 4 blok");
    }

    @Test
    void planTekrarsizSiraliVeYaricapIcinde() {
        KraterProfili.Plan p = KraterProfili.planla(5.5, 4, 42L, 2, true);
        Set<KraterProfili.Hucre> gorulen = new HashSet<>();
        int oncekiDy = Integer.MAX_VALUE;
        for (KraterProfili.Hucre h : p.kazi()) {
            assertTrue(gorulen.add(h), "tekrar: " + h);
            assertTrue(Math.hypot(h.dx(), h.dz()) <= 5.5 + 1e-9, "yarıçap dışı: " + h);
            assertTrue(h.dy() <= 2, "üst temizlik sınırı: " + h);
            assertTrue(h.dy() <= oncekiDy, "yukarıdan aşağı sıralı olmalı");
            oncekiDy = h.dy();
        }
        for (KraterProfili.AstarHucresi a : p.astar()) {
            int k = KraterProfili.derinlik(Math.hypot(a.dx(), a.dz()), 5.5, 4);
            assertEquals(-k, a.dy(), "astar kazılan sütunun hemen altında");
            assertFalse(gorulen.contains(new KraterProfili.Hucre(a.dx(), a.dy(), a.dz())));
        }
        for (KraterProfili.AstarHucresi a : p.kenar()) {
            double r = Math.hypot(a.dx(), a.dz());
            assertTrue(r > 5.5 && r <= 5.5 + KraterProfili.KENAR_GENISLIGI, "kenar halkası: " + r);
            assertEquals(1, a.dy());
        }
        assertTrue(p.astar().stream().anyMatch(a -> a.malzeme() == KraterProfili.Malzeme.MAGMA), "merkezde sıcak astar");
        assertTrue(!p.kenar().isEmpty(), "kenarda enkaz");
    }

    @Test
    void ayniTohumAyniPlan() {
        assertEquals(KraterProfili.planla(5.5, 4, 9L, 2, true), KraterProfili.planla(5.5, 4, 9L, 2, true));
    }

    @Test
    void sertBloklarKorunur() {
        KraterProfili.Plan p = KraterProfili.planla(5.5, 4, 3L, 2, true);
        // Merkezden geçen x = 0 duvarı ve dy = -1 katmanı "sert".
        List<KraterProfili.Hucre> kazilacak = KraterProfili.korunanlarHaric(p.kazi(), h -> h.dx() == 0 || h.dy() == -1);
        assertTrue(kazilacak.size() < p.kazi().size());
        for (KraterProfili.Hucre h : kazilacak) {
            assertTrue(h.dx() != 0 && h.dy() != -1, "sert hücre kazılmamalı: " + h);
        }
        assertEquals(p.kazi().stream().filter(h -> h.dx() != 0 && h.dy() != -1).count(), kazilacak.size());
    }

    @Test
    void blokHasariKapaliysaKraterYok() {
        KraterProfili.Plan p = KraterProfili.planla(5.5, 4, 3L, 2, false);
        assertTrue(p.kazi().isEmpty() && p.astar().isEmpty() && p.kenar().isEmpty());
        assertEquals(KraterProfili.Plan.BOS, p);
    }
}
