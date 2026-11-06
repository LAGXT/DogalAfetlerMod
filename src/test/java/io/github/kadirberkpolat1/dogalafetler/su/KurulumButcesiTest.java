package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KurulumButcesiTest {
    private static final long MS = 1_000_000L;

    @Test
    void ilkTickTamButce() {
        KurulumButcesi b = new KurulumButcesi();
        b.tickBasla(1, 2 * MS);
        assertTrue(b.calisabilir());
        b.harca(MS + MS / 2);
        assertTrue(b.calisabilir());
        b.harca(MS);
        assertFalse(b.calisabilir());
        assertEquals(MS / 2 * 5, b.harcanan());
    }

    @Test
    void asimSonrakiTicktenDusulur() {
        KurulumButcesi b = new KurulumButcesi();
        b.tickBasla(1, 2 * MS);
        b.harca(MS * 5 / 2);
        b.tickBasla(2, 2 * MS);
        assertEquals(MS / 2, b.harcanan());
        assertEquals(MS * 3 / 2, b.kalan());
    }

    @Test
    void buyukAsimBirkacTickeYayilir() {
        KurulumButcesi b = new KurulumButcesi();
        b.tickBasla(1, 2 * MS);
        b.harca(5 * MS);
        b.tickBasla(2, 2 * MS);
        assertFalse(b.calisabilir(), "3 ms borç 2 ms bütçeyi aşar");
        b.tickBasla(3, 2 * MS);
        assertEquals(MS, b.kalan());
    }

    @Test
    void ayniTickteYenidenBaslamaSifirlamaz() {
        KurulumButcesi b = new KurulumButcesi();
        b.tickBasla(7, 2 * MS);
        b.harca(MS);
        b.tickBasla(7, 2 * MS);
        assertEquals(MS, b.harcanan());
    }

    @Test
    void uzunSuredeOrtalamaButceyiAsmaz() {
        KurulumButcesi b = new KurulumButcesi();
        long parca = MS * 7 / 10;
        long toplam = 0;
        for (long t = 0; t < 100; t++) {
            b.tickBasla(t, 2 * MS);
            while (b.calisabilir()) {
                b.harca(parca);
                toplam += parca;
            }
        }
        assertTrue(toplam <= 100 * 2 * MS + parca, "toplam " + toplam);
        assertTrue(toplam >= 99 * 2 * MS, "bütçe boşa gitmemeli: " + toplam);
    }

    @Test
    void ikiIsVarkenAzHarcayanOnceCalisir() {
        KurulumButcesi b = new KurulumButcesi();
        b.tickBasla(1, 2 * MS);
        assertEquals(KurulumButcesi.YOK, b.siradaki(false, false));
        assertEquals(KurulumButcesi.UZAK, b.siradaki(false, true));
        assertEquals(KurulumButcesi.YAKIN, b.siradaki(true, true), "eşitlikte yakın halka önce");
        b.harca(KurulumButcesi.YAKIN, MS / 10);
        assertEquals(KurulumButcesi.UZAK, b.siradaki(true, true));
        b.harca(KurulumButcesi.UZAK, MS / 5);
        assertEquals(KurulumButcesi.YAKIN, b.siradaki(true, true));
        b.tickBasla(2, 2 * MS);
        assertEquals(KurulumButcesi.YAKIN, b.siradaki(true, true), "iş payları her tick sıfırlanır");
    }

    /**
     * SU-38 senaryosu: yakın halka sürekli yeniden kurulur (10 ms, 0,01 ms'lik parçalar), uzak LOD her 10 tick'te bir 16 ms
     * (koşu 17'deki en kötü tek kurulum) ve 0,12 ms'lik sütun parçalarıyla. Ortak 2 ms bütçe: tick başına iş bütçe + bir parçayı
     * geçmez (eskiden uzak kurulum tek karede 16 ms), iki iş de ilerler, uzak kurulum en çok 17 tick'te biter.
     */
    @Test
    void uzakLodParcalariYakinHalkaylaButceyiPaylasir() {
        KurulumButcesi b = new KurulumButcesi();
        long yakinParca = MS / 100;
        long uzakParca = MS * 12 / 100;
        long yakinKalan = 10 * MS;
        long uzakKalan = 0;
        long uzakBaslangic = -100;
        int yakinBiten = 0;
        int uzakBiten = 0;
        int enUzunUzak = 0;
        for (long t = 0; t < 400; t++) {
            if (uzakKalan == 0 && t - uzakBaslangic >= 10) {
                uzakKalan = 16 * MS;
                uzakBaslangic = t;
            }
            b.tickBasla(t, 2 * MS);
            long is = 0;
            while (b.calisabilir()) {
                int sira = b.siradaki(yakinKalan > 0, uzakKalan > 0);
                if (sira == KurulumButcesi.YOK) {
                    break;
                }
                long parca = sira == KurulumButcesi.YAKIN ? yakinParca : uzakParca;
                b.harca(sira, parca);
                is += parca;
                if (sira == KurulumButcesi.YAKIN && (yakinKalan -= parca) <= 0) {
                    yakinBiten++;
                    yakinKalan = 10 * MS;
                } else if (sira == KurulumButcesi.UZAK && (uzakKalan -= parca) <= 0) {
                    uzakKalan = 0;
                    uzakBiten++;
                    enUzunUzak = Math.max(enUzunUzak, (int) (t - uzakBaslangic + 1));
                }
            }
            assertTrue(is <= 2 * MS + uzakParca, "tick " + t + " işi " + is);
        }
        assertTrue(uzakBiten >= 20, "uzak kurulum aç kaldı: " + uzakBiten);
        assertTrue(yakinBiten >= 30, "yakın halka aç kaldı: " + yakinBiten);
        assertTrue(enUzunUzak <= 17, "uzak kurulum " + enUzunUzak + " tick sürdü");
    }
}
