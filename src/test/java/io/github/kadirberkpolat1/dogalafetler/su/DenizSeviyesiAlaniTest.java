package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.IntBinaryOperator;
import org.junit.jupiter.api.Test;

class DenizSeviyesiAlaniTest {
    /** Sahte dunya: x,z -> zemin; su(x,z) dogruysa zemin gercek su yuzeyidir. */
    private static DenizSeviyesiAlani.SutunOkuyucu dunya(IntBinaryOperator zemin, IntBinaryOperator su) {
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

    private static SuAraliklari aralik(DenizSeviyesiAlani alan, int x, int z, double artis) {
        SuAraliklari a = new SuAraliklari();
        alan.aralikEkle(x, z, artis, a);
        a.birlestir();
        return a;
    }

    @Test
    void kiyidakiAlcakKaraArtisKadarSuAltindaKalir() {
        var okuyucu = dunya((x, z) -> x < 0 ? 63 : 64 + x, (x, z) -> x < 0 ? 1 : 0);
        DenizSeviyesiAlani alan = new DenizSeviyesiAlani(4, 96, 1);
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
        DenizSeviyesiAlani ileri = new DenizSeviyesiAlani(4, 96, 1);
        DenizSeviyesiAlani geri = new DenizSeviyesiAlani(4, 96, 1);
        for (int cx = 0; cx <= 2; cx++) {
            ileri.iste(cx, 0);
            geri.iste(2 - cx, 0);
        }
        ileri.isle(okuyucu, 1000);
        geri.isle(okuyucu, 1000);
        for (int x = 0; x < 48; x++) {
            for (int z = 0; z < 16; z += 5) {
                assertEquals(ileri.kaynak(x, z), geri.kaynak(x, z), 0.0, "x=" + x);
                assertEquals(ileri.mesafe(x, z), geri.mesafe(x, z), "x=" + x);
            }
        }
        assertEquals(63f, geri.kaynak(20, 4));
        assertEquals(13, geri.mesafe(20, 4));
        assertEquals(64, aralik(geri, 40, 4, 4).alt(0));
    }

    @Test
    void mesafeSiniriAsilmaz() {
        var okuyucu = dunya((x, z) -> x < 0 ? 63 : 64, (x, z) -> x < 0 ? 1 : 0);
        DenizSeviyesiAlani alan = new DenizSeviyesiAlani(4, 20, 1);
        for (int cx = -1; cx <= 2; cx++) {
            alan.iste(cx, 0);
        }
        alan.isle(okuyucu, 1000);
        assertEquals(20, alan.mesafe(19, 0));
        assertEquals(1, aralik(alan, 19, 0, 4).adet());
        assertEquals(0, aralik(alan, 20, 0, 4).adet());
    }

    @Test
    void denizeBagliOlmayanCukurDolmaz() {
        var okuyucu = dunya((x, z) -> {
            if (x < 0) {
                return 63;
            }
            return x >= 3 && x <= 5 ? 60 : 70;
        }, (x, z) -> x < 0 ? 1 : 0);
        DenizSeviyesiAlani alan = new DenizSeviyesiAlani(4, 96, 1);
        alan.iste(-1, 0);
        alan.iste(0, 0);
        alan.isle(okuyucu, 100);
        assertEquals(0, aralik(alan, 4, 7, 4).adet());
        assertTrue(Float.isNaN(alan.kaynak(4, 7)));
    }

    @Test
    void kaynakYuzeyindenAlcakYamacDolmaz() {
        var okuyucu = dunya((x, z) -> switch (x) {
            case 0 -> 71;
            case 1 -> 69;
            case 2 -> 66;
            default -> x < 0 ? 70 : 66;
        }, (x, z) -> x < 0 ? 1 : 0);
        DenizSeviyesiAlani alan = new DenizSeviyesiAlani(4, 96, 1);
        alan.iste(-1, 0);
        alan.iste(0, 0);
        alan.isle(okuyucu, 100);
        assertEquals(1, aralik(alan, 0, 2, 4).adet());
        assertEquals(1, aralik(alan, 1, 2, 4).adet(), "tolerans 1: 69 >= 70-1");
        assertEquals(0, aralik(alan, 2, 2, 4).adet(), "gol yuzeyinin 4 blok altindaki yamaca su asili kalmaz");
    }

    @Test
    void gecersizKilinanChunkYenidenHesaplanir() {
        int[] karaYuksekligi = {64};
        var okuyucu = dunya((x, z) -> x < 8 ? 63 : karaYuksekligi[0], (x, z) -> x < 8 ? 1 : 0);
        DenizSeviyesiAlani alan = new DenizSeviyesiAlani(4, 96, 1);
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
}
