package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.su.Cekilme;
import io.github.kadirberkpolat1.dogalafetler.su.Dalga;
import java.util.function.DoublePredicate;
import org.junit.jupiter.api.Test;

/**
 * AF3-15: otomasyonun kare anları senkron evreden; eski (çekilme 18 sn, dalga AKTIF + 10 sn, kıyı + 28 sn) ve kısa (çekilme
 * 8 sn, dalga + 2 sn, kıyı + 10 sn) zamanlamada aynı evreye düşer. Zaman AKTIF'in başından tick.
 */
class TsunamiKareAnlariTest {
    private static final double KX = 120;
    private static final double KZ = 0;
    private static final double HIZ = 6.0 / 20;

    /** Doğuş tick'i ve kıyıya yol tick'iyle kuzeyden doğuya ilerleyen dalga (açıklık = hız × yol). */
    private static Dalga dalga(double dogus, double yol) {
        return new Dalga(KX - HIZ * yol, 0, 1, 0, dogus, HIZ, 10, 40, 30, 60, 400);
    }

    /** {@code kosul}'un ilk doğru olduğu tick (otomasyonun {@code e:} adımı gibi tick tick). */
    private static int ilk(DoublePredicate kosul) {
        for (int t = 0; t < 4000; t++) {
            if (kosul.test(t)) {
                return t;
            }
        }
        return -1;
    }

    private record Anlar(int cekilme, int ufuk, int vitrin, int carpma, int karada) {
    }

    private static Anlar anlar(long cekilmeSure, double dogus, double yol) {
        Cekilme c = new Cekilme(KX, KZ, 40, 4, 0, cekilmeSure);
        Dalga w = dalga(dogus, yol);
        return new Anlar(ilk(t -> TsunamiKareAnlari.cekilmeIlerlemesi(c, t) >= 0.42),
                ilk(t -> TsunamiKareAnlari.kalanOran(w, KX, KZ, t) <= 0.86),
                ilk(t -> TsunamiKareAnlari.kalanOran(w, KX, KZ, t) <= 0.22),
                ilk(t -> TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, t) >= 60),
                ilk(t -> TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, t) >= 160));
    }

    @Test
    void eskiZamanlamadaEskiSabitAnlaraDenkGelir() {
        Anlar a = anlar(360, 200, 360);
        assertEquals(161, a.cekilme(), 12, "çekilme karesi (eski b:150 + kamera)");
        assertEquals(260, a.ufuk(), 12, "ufuk ~13 sn");
        assertEquals(480, a.vitrin(), 12, "vitrin ~24 sn");
        assertEquals(620, a.carpma(), 1, "çarpma ~31 sn");
        assertEquals(720, a.karada(), 1, "karada ~36 sn");
    }

    @Test
    void kisaZamanlamadaAyniEvreler() {
        Anlar a = anlar(160, 40, 160);
        int kiyi = 200;
        assertTrue(a.ufuk() > 40 && a.ufuk() < a.vitrin(), "ufuk doğuştan sonra: " + a);
        assertTrue(a.vitrin() < kiyi, "vitrin kıyıdan önce: " + a);
        assertEquals(kiyi + 60, a.carpma(), 1);
        assertEquals(kiyi + 160, a.karada(), 1);
        assertTrue(a.cekilme() < kiyi, "çekilme karesi dalga kıyıya varmadan: " + a);
        assertEquals(0.86, 1 - (a.ufuk() - 40) / 160.0, 0.01, "aynı kalan oran");
    }

    @Test
    void sinirlar() {
        Dalga w = dalga(100, 200);
        assertEquals(1, TsunamiKareAnlari.kalanOran(w, KX, KZ, 0), 1e-12, "doğuştan önce 1");
        assertEquals(1, TsunamiKareAnlari.kalanOran(w, KX, KZ, 100), 1e-12);
        assertEquals(0, TsunamiKareAnlari.kalanOran(w, KX, KZ, 300), 1e-12, "kıyıda 0");
        assertEquals(0, TsunamiKareAnlari.kalanOran(w, KX, KZ, 900), 1e-12, "sonrası 0");
        assertEquals(0, TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, 300), 1e-9);
        assertEquals(-200, TsunamiKareAnlari.kiyidanSonra(w, KX, KZ, 100), 1e-9);
        assertEquals(0, TsunamiKareAnlari.kalanOran(w, w.x0() - 5, KZ, 150), 1e-12, "kıyı çizginin gerisinde: 0");
        Cekilme c = new Cekilme(0, 0, 10, 3, 50, 0);
        assertEquals(0, TsunamiKareAnlari.cekilmeIlerlemesi(c, 50), 1e-12);
        assertEquals(1, TsunamiKareAnlari.cekilmeIlerlemesi(c, 51), 1e-12, "süresiz çekilme hemen tamam");
    }
}
