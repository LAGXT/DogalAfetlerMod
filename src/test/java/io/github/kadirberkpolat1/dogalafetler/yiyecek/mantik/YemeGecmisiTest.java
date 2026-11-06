package io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Azalan verim (madde 14, GOREV §6 #2): aynı tür 60 sn içinde tekrar yenirse etki %50; farklı türler bağımsız. */
class YemeGecmisiTest {
    private static final EtkiAyarlari A = EtkiAyarlari.VARSAYILAN;
    private static final String SALEP = "dogalafetler:sicak_salep";
    private static final String CIKOLATA = "dogalafetler:sicak_cikolata";

    @Test
    void ilkYemeTamIkinciYariVerir() {
        YemeGecmisi g = new YemeGecmisi();
        assertEquals(1.0, g.ye(SALEP, 1000, A), 1e-12);
        assertEquals(0.5, g.ye(SALEP, 1000 + 20, A), 1e-12, "1 sn sonra");
    }

    @Test
    void pencereAltmisSaniyeSinirdaTamVerir() {
        YemeGecmisi g = new YemeGecmisi();
        g.ye(SALEP, 0, A);
        assertEquals(0.5, g.carpan(SALEP, 1199, A), 1e-12, "59,95 sn: henüz pencere içinde");
        assertEquals(1.0, g.carpan(SALEP, 1200, A), 1e-12, "tam 60 sn: pencere doldu");
        assertEquals(1.0, g.ye(SALEP, 1200, A), 1e-12);
    }

    @Test
    void pencereSonYemedenSayilir() {
        YemeGecmisi g = new YemeGecmisi();
        g.ye(SALEP, 0, A);
        assertEquals(0.5, g.ye(SALEP, 1000, A), 1e-12);
        assertEquals(0.5, g.ye(SALEP, 2100, A), 1e-12, "ilk yemeden 105 sn ama sondan 55 sn sonra: spam sürer");
        assertEquals(1.0, g.ye(SALEP, 2100 + 1200, A), 1e-12, "60 sn ara verince tam");
    }

    @Test
    void farkliTurlerBagimsiz() {
        YemeGecmisi g = new YemeGecmisi();
        assertEquals(1.0, g.ye(SALEP, 500, A), 1e-12);
        assertEquals(1.0, g.ye(CIKOLATA, 510, A), 1e-12, "farklı yiyecek kendi başına");
        assertEquals(0.5, g.ye(SALEP, 520, A), 1e-12);
        assertEquals(0.5, g.ye(CIKOLATA, 530, A), 1e-12);
    }

    @Test
    void configPencereVeOrani() {
        EtkiAyarlari kisa = new EtkiAyarlari(3, 8, 20, 35, 45, 0.25, 10, 0.25, 1.5, 20);
        YemeGecmisi g = new YemeGecmisi();
        g.ye(SALEP, 0, kisa);
        assertEquals(0.25, g.carpan(SALEP, 199, kisa), 1e-12);
        assertEquals(1.0, g.carpan(SALEP, 200, kisa), 1e-12);
        EtkiAyarlari kapali = new EtkiAyarlari(3, 8, 20, 35, 45, 0.25, 0, 0.5, 1.5, 20);
        YemeGecmisi h = new YemeGecmisi();
        h.ye(SALEP, 0, kapali);
        assertEquals(1.0, h.ye(SALEP, 0, kapali), 1e-12, "pencere 0: azalan verim kapalı");
    }

    @Test
    void geriGidenZamanCezaVermez() {
        YemeGecmisi g = new YemeGecmisi();
        g.ye(SALEP, 5000, A);
        assertEquals(1.0, g.carpan(SALEP, 4000, A), 1e-12, "saat geri gitti (dünya kopyası): tam");
    }

    @Test
    void kalanSureVeBudama() {
        YemeGecmisi g = new YemeGecmisi();
        g.ye(SALEP, 0, A);
        g.ye(CIKOLATA, 1000, A);
        assertEquals(1200 - 300, g.kalanTick(SALEP, 300, A));
        assertEquals(0, g.kalanTick("dogalafetler:ayran", 300, A));
        g.buda(1300, A);
        Map<String, Long> kayit = g.kayitlar();
        assertFalse(kayit.containsKey(SALEP), "penceresi dolan kayıt atılır");
        assertTrue(kayit.containsKey(CIKOLATA));
        YemeGecmisi geri = YemeGecmisi.kayitlardan(kayit);
        assertEquals(0.5, geri.carpan(CIKOLATA, 1300, A), 1e-12, "NBT gidiş-dönüşü");
    }
}
