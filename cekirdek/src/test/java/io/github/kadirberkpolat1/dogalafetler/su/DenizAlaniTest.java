package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.IntBinaryOperator;
import org.junit.jupiter.api.Test;

class DenizAlaniTest {
    /** Sahte dunya: x,z -> zemin; su(x,z) dogruysa zemin gercek su yuzeyidir. */
    private static DenizAlani.SutunOkuyucu dunya(IntBinaryOperator zemin, IntBinaryOperator su) {
        return (cx, cz, zeminDizi, suDizi, taban) -> {
            for (int i = 0; i < 256; i++) {
                int x = cx * 16 + (i & 15);
                int z = cz * 16 + (i >> 4);
                zeminDizi[i] = zemin.applyAsInt(x, z);
                suDizi[i] = su.applyAsInt(x, z) != 0;
                taban[i] = suDizi[i] ? zeminDizi[i] - 8 : zeminDizi[i];
            }
            return true;
        };
    }

    private static SuAraliklari aralik(DenizAlani alan, int x, int z, double artis) {
        SuAraliklari a = new SuAraliklari();
        alan.aralikEkle(x, z, artis, a);
        a.birlestir();
        return a;
    }

    @Test
    void kiyidakiAlcakKaraArtisKadarSuAltindaKalir() {
        var okuyucu = dunya((x, z) -> x < 0 ? 63 : 64 + x, (x, z) -> x < 0 ? 1 : 0);
        DenizAlani alan = new DenizAlani(4, 1, 63);
        alan.iste(-1, 0);
        alan.iste(0, 0);
        alan.isle(okuyucu, 100);

        SuAraliklari deniz = aralik(alan, -5, 3, 4);
        assertEquals(1, deniz.adet());
        assertEquals(63, deniz.alt(0));
        assertEquals(67, deniz.ust(0));
        SuAraliklari kara0 = aralik(alan, 0, 3, 4);
        assertEquals(64, kara0.alt(0));
        assertEquals(67, kara0.ust(0));
        assertEquals(1, aralik(alan, 2, 3, 4).adet());
        assertEquals(0, aralik(alan, 3, 3, 4).adet(), "zemini 67 olan sutun 63+4'te kuru kalir");
        assertEquals(0, aralik(alan, 1, 3, 2).adet(), "artis 2 iken zemin 65 kuru");
        assertEquals(0, aralik(alan, 0, 3, 0).adet(), "artis 0 iken sanal su yok");
    }

    @Test
    void chunkSinirindaSurekliVeSiradanBagimsiz() {
        var okuyucu = dunya((x, z) -> x < 8 ? 63 : 64, (x, z) -> x < 8 ? 1 : 0);
        DenizAlani ileri = new DenizAlani(4, 1, 63);
        DenizAlani geri = new DenizAlani(4, 1, 63);
        for (int cx = 0; cx <= 2; cx++) {
            ileri.iste(cx, 0);
            geri.iste(2 - cx, 0);
        }
        ileri.isle(okuyucu, 1000);
        geri.isle(okuyucu, 1000);
        for (int x = 0; x < 48; x++) {
            for (int z = 0; z < 16; z += 5) {
                assertEquals(ileri.kaynak(x, z), geri.kaynak(x, z), 0.0, "x=" + x);
                assertEquals(ileri.esik(x, z), geri.esik(x, z), "x=" + x);
            }
        }
        assertEquals(63f, geri.kaynak(20, 4));
        assertEquals(64, geri.esik(20, 4));
        assertEquals(64, aralik(geri, 40, 4, 4).alt(0));
    }

    @Test
    void duzOvaMesafeSiniriOlmadanDolar() {
        var okuyucu = dunya((x, z) -> x < 0 ? 63 : 64, (x, z) -> x < 0 ? 1 : 0);
        DenizAlani alan = new DenizAlani(4, 1, 63);
        for (int cx = -1; cx <= 2; cx++) {
            alan.iste(cx, 0);
        }
        alan.isle(okuyucu, 1000);
        SuAraliklari uzak = aralik(alan, 47, 9, 4);
        assertEquals(1, uzak.adet(), "yüklü alanın ucundaki ova da dolar (kesme sınırı duvar yapıyordu)");
        assertEquals(64, uzak.alt(0));
        assertEquals(67, uzak.ust(0));
    }

    @Test
    void denizeBagliOlmayanCukurDolmaz() {
        var okuyucu = dunya((x, z) -> {
            if (x < 0) {
                return 63;
            }
            return x >= 3 && x <= 5 ? 60 : 70;
        }, (x, z) -> x < 0 ? 1 : 0);
        DenizAlani alan = new DenizAlani(4, 1, 63);
        alan.iste(-1, 0);
        alan.iste(0, 0);
        alan.isle(okuyucu, 100);
        assertEquals(0, aralik(alan, 4, 7, 4).adet());
        assertTrue(Float.isNaN(alan.kaynak(4, 7)));
    }

    @Test
    void golSirtYuksekliginiAsmazArkadakiYamacKuruKalir() {
        // Göl yüzeyi 70 (deniz seviyesi 63'ün üstünde: göl), x=0 sırt 71, arkası 69 ve 66'ya iner: göl yalnız sırta kadar
        // yükselebilir (üstü taşar); sırtın arkasındaki yamaçta havada aslı su olmaz.
        var okuyucu = dunya((x, z) -> switch (x) {
            case 0 -> 71;
            case 1 -> 69;
            case 2 -> 66;
            default -> x < 0 ? 70 : 66;
        }, (x, z) -> x < 0 ? 1 : 0);
        DenizAlani alan = new DenizAlani(4, 1, 63);
        alan.iste(-1, 0);
        alan.iste(0, 0);
        alan.isle(okuyucu, 100);
        SuAraliklari gol = aralik(alan, -3, 2, 4);
        assertEquals(1, gol.adet());
        assertEquals(70, gol.alt(0));
        assertEquals(71, gol.ust(0), "göl sırtın yüksekliğine (71) kadar yükselir");
        assertEquals(0, aralik(alan, 0, 2, 4).adet(), "sırtın kendisi kuru");
        assertEquals(0, aralik(alan, 1, 2, 4).adet(), "sırtın arkası kuru (su taşar, birikmez)");
        assertEquals(0, aralik(alan, 2, 2, 4).adet());
    }

    @Test
    void golTasmaAgzindanYuksegeCikmaz() {
        // Göl yüzeyi 80, çevresi 84'lük kenar; z=8 sırasında x=6'da 82'lik ağız, arkası 60'a iner.
        var okuyucu = dunya((x, z) -> {
            if (x >= 0 && x <= 5 && z >= 3 && z <= 12) {
                return 80;
            }
            if (x == 6 && z == 8) {
                return 82;
            }
            return x >= 7 && z == 8 ? 60 : 84;
        }, (x, z) -> x >= 0 && x <= 5 && z >= 3 && z <= 12 ? 1 : 0);
        DenizAlani alan = new DenizAlani(6, 1, 63);
        alan.iste(0, 0);
        alan.iste(1, 0);
        alan.isle(okuyucu, 100);
        assertEquals(82, aralik(alan, 2, 6, 6).ust(0), "göl ağız yüksekliğinde durur");
        assertEquals(81, aralik(alan, 2, 6, 1).ust(0), "ağızın altında artışın tamamı");
        assertEquals(0, aralik(alan, 6, 8, 6).adet(), "ağız sütunu kuru");
        assertEquals(0, aralik(alan, 9, 8, 6).adet(), "ağızın arkasındaki vadi dolmaz");
    }

    @Test
    void kumulArkasindakiCukurDenizKumuluAsincaDolar() {
        // Deniz 63 (x<0), kıyı kumulu 65 (x 0..1), arkasında 58'lik çukur (x 2..5), sonra 70'lik kara.
        var okuyucu = dunya((x, z) -> {
            if (x < 0) {
                return 63;
            }
            if (x <= 1) {
                return 65;
            }
            return x <= 5 ? 58 : 70;
        }, (x, z) -> x < 0 ? 1 : 0);
        DenizAlani alan = new DenizAlani(6, 1, 63);
        alan.iste(-1, 0);
        alan.iste(0, 0);
        alan.isle(okuyucu, 100);
        assertEquals(0, aralik(alan, 1, 4, 1).adet(), "artış 1: kumul kuru");
        assertEquals(0, aralik(alan, 3, 4, 1).adet(), "artış 1: su kumulu aşmadı, çukur kuru");
        assertEquals(0, aralik(alan, 3, 4, 2).adet(), "artış 2: yüzey kumul hizasında, çukur kuru");
        SuAraliklari cukur = aralik(alan, 3, 4, 3);
        assertEquals(1, cukur.adet(), "artış 3: su kumulu aştı, çukur doldu");
        assertEquals(58, cukur.alt(0));
        assertEquals(66, cukur.ust(0));
        assertEquals(0, aralik(alan, 7, 4, 6).adet(), "zemini 70 olan kara kuru");
    }

    @Test
    void rastgeleArazideHavadaAsiliSuDuvariYok() {
        java.util.Random rastgele = new java.util.Random(20261106L);
        int[][] yukseklik = new int[64][64];
        for (int x = 0; x < 64; x++) {
            for (int z = 0; z < 64; z++) {
                double d = 62 + 4 * Math.sin(x * 0.21) + 3 * Math.cos(z * 0.17 + x * 0.05) + x * 0.12;
                yukseklik[x][z] = (int) Math.round(d) + rastgele.nextInt(2);
            }
        }
        // Deniz: x < 6 ve yüksekliği 63'ün altındaki sütunlar; bir de 20..24 kutusunda yüksek bir göl.
        IntBinaryOperator su = (x, z) -> (x < 6 && yukseklik[x][z] <= 63) || (x >= 20 && x <= 24 && z >= 20 && z <= 24)
                ? 1 : 0;
        IntBinaryOperator zemin = (x, z) -> {
            if (x >= 20 && x <= 24 && z >= 20 && z <= 24) {
                return 70;
            }
            return x < 6 && yukseklik[x][z] <= 63 ? 63 : yukseklik[x][z];
        };
        DenizAlani alan = new DenizAlani(6, 1, 63);
        for (int cx = 0; cx < 4; cx++) {
            for (int cz = 0; cz < 4; cz++) {
                alan.iste(cx, cz);
            }
        }
        alan.isle(dunya(zemin, su), 100);
        int islak = 0;
        for (int artis = 1; artis <= 6; artis++) {
            for (int x = 0; x < 64; x++) {
                for (int z = 0; z < 64; z++) {
                    SuAraliklari a = aralik(alan, x, z, artis);
                    if (a.adet() == 0) {
                        continue;
                    }
                    islak++;
                    double ust = a.ust(a.adet() - 1);
                    int[][] komsular = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                    for (int[] k : komsular) {
                        int nx = x + k[0];
                        int nz = z + k[1];
                        if (nx < 0 || nz < 0 || nx >= 64 || nz >= 64) {
                            continue;
                        }
                        SuAraliklari n = aralik(alan, nx, nz, artis);
                        boolean tutar = n.adet() > 0 ? n.ust(n.adet() - 1) == ust : zemin.applyAsInt(nx, nz) >= ust;
                        assertTrue(tutar, "artış " + artis + ": (" + x + "," + z + ") yüzeyi " + ust
                                + " komşu (" + nx + "," + nz + ") zemini " + zemin.applyAsInt(nx, nz) + " -> havada su duvarı");
                    }
                }
            }
        }
        assertTrue(islak > 200, "sahne yeterince islanmadı: " + islak);
    }

    @Test
    void arkaPlanYurutucusuAyniSonucuVerir() throws Exception {
        var okuyucu = dunya((x, z) -> x < 8 ? 63 : 64 + (x + z) % 3, (x, z) -> x < 8 ? 1 : 0);
        DenizAlani senkron = new DenizAlani(4, 1, 63);
        DenizAlani asenkron = new DenizAlani(4, 1, 63);
        java.util.concurrent.ExecutorService yurutucu = java.util.concurrent.Executors.newSingleThreadExecutor();
        asenkron.yurutucu(yurutucu);
        for (int cx = 0; cx <= 2; cx++) {
            for (int cz = 0; cz <= 1; cz++) {
                senkron.iste(cx, cz);
                asenkron.iste(cx, cz);
            }
        }
        senkron.isle(okuyucu, 100);
        asenkron.isle(okuyucu, 100);
        yurutucu.shutdown();
        assertTrue(yurutucu.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS));
        assertFalse(asenkron.hesapSuruyor());
        assertEquals(1, asenkron.hesapSayisi());
        for (int x = 0; x < 48; x++) {
            for (int z = 0; z < 32; z++) {
                assertEquals(senkron.kaynak(x, z), asenkron.kaynak(x, z), 0.0, "x=" + x + " z=" + z);
                assertEquals(senkron.esik(x, z), asenkron.esik(x, z));
            }
        }
    }

    @Test
    void gecersizKilinanChunkYenidenHesaplanir() {
        int[] karaYuksekligi = {64};
        var okuyucu = dunya((x, z) -> x < 8 ? 63 : karaYuksekligi[0], (x, z) -> x < 8 ? 1 : 0);
        DenizAlani alan = new DenizAlani(4, 1, 63);
        alan.iste(0, 0);
        alan.isle(okuyucu, 10);
        assertEquals(1, aralik(alan, 12, 0, 4).adet());
        karaYuksekligi[0] = 80;
        alan.gecersizKil(0, 0);
        assertTrue(alan.bekleyen() > 0);
        alan.isle(okuyucu, 10);
        assertEquals(0, aralik(alan, 12, 0, 4).adet());
        assertFalse(alan.bekleyen() > 0);
    }

    @Test
    void degismeyenYenidenOkumaHesabiTetiklemez() {
        int[] karaYuksekligi = {64};
        var okuyucu = dunya((x, z) -> x < 8 ? 63 : karaYuksekligi[0], (x, z) -> x < 8 ? 1 : 0);
        DenizAlani alan = new DenizAlani(4, 1, 63);
        alan.iste(0, 0);
        alan.iste(1, 0);
        alan.isle(okuyucu, 10);
        assertEquals(1, alan.hesapSayisi());
        assertEquals(2, alan.degisenOkumaSayisi());

        alan.gecersizKil(0, 0);
        alan.gecersizKil(1, 0);
        assertEquals(2, alan.isle(okuyucu, 10), "iki chunk yeniden okundu");
        assertEquals(1, alan.hesapSayisi(), "değişmeyen okuma BFS başlatmaz");
        assertEquals(2, alan.degisenOkumaSayisi());

        karaYuksekligi[0] = 80;
        alan.gecersizKil(0, 0);
        alan.gecersizKil(1, 0);
        alan.isle(okuyucu, 10);
        assertEquals(2, alan.hesapSayisi());
        assertEquals(4, alan.degisenOkumaSayisi());
        assertEquals(0, aralik(alan, 12, 0, 4).adet());
    }
}
