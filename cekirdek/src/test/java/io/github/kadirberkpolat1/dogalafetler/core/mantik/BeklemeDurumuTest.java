package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.Map;
import org.junit.jupiter.api.Test;

class BeklemeDurumuTest {
    private static final ZamanlayiciAyarlari A = ZamanlayiciAyarlari.VARSAYILAN;

    @Test
    void afetBitinceGenelVeTurBeklemesiBaslar() {
        BeklemeDurumu bekleme = new BeklemeDurumu();
        assertFalse(bekleme.genelBeklemede(0));
        bekleme.afetBitti(AfetTuru.SINKHOLE, 1, 500, A, 1.0);
        assertTrue(bekleme.genelBeklemede(500));
        assertTrue(bekleme.genelBeklemede(500 + A.genelBeklemeTick() - 1));
        assertFalse(bekleme.genelBeklemede(500 + A.genelBeklemeTick()));
        assertTrue(bekleme.turBeklemede(AfetTuru.SINKHOLE, 500 + A.turBekleme(AfetTuru.SINKHOLE) - 1));
        assertFalse(bekleme.turBeklemede(AfetTuru.SINKHOLE, 500 + A.turBekleme(AfetTuru.SINKHOLE)));
        assertFalse(bekleme.turBeklemede(AfetTuru.KASIRGA, 500));
        assertEquals(A.genelBeklemeTick(), bekleme.genelKalan(500));
        assertEquals(0, bekleme.genelKalan(10_000_000));
    }

    @Test
    void seviyesizAfetEnSiddetliGibiUzatir() {
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.AY_CARPMASI, 0, 0, A, 1.0);
        assertEquals(Math.round(A.genelBeklemeTick() * A.siddetUzamasi(AfetTuru.AY_CARPMASI, 0)), bekleme.genelKalan(0));
        assertEquals(A.siddetUzamasi(AfetTuru.DEPREM, 5), A.siddetUzamasi(AfetTuru.AY_CARPMASI, 0), 1e-12);
    }

    @Test
    void ertelemeYalnizTuruEtkiler() {
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.ertele(AfetTuru.TSUNAMI, 100, 6000);
        assertTrue(bekleme.turBeklemede(AfetTuru.TSUNAMI, 6099));
        assertFalse(bekleme.turBeklemede(AfetTuru.TSUNAMI, 6100));
        assertFalse(bekleme.genelBeklemede(100));
    }

    @Test
    void kisaBeklemeUzunuSilmez() {
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.TSUNAMI, 5, 0, A, 1.0);
        long uzun = bekleme.turKalan(AfetTuru.TSUNAMI, 0);
        bekleme.ertele(AfetTuru.TSUNAMI, 0, 10);
        assertEquals(uzun, bekleme.turKalan(AfetTuru.TSUNAMI, 0));
    }

    @Test
    void siklikBeklemeyiBoler() {
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.SINKHOLE, 1, 0, A, 2.0);
        assertEquals(A.genelBeklemeTick() / 2, bekleme.genelKalan(0));
        assertEquals(A.turBekleme(AfetTuru.SINKHOLE) / 2, bekleme.turKalan(AfetTuru.SINKHOLE, 0));
        BeklemeDurumu yavas = new BeklemeDurumu();
        yavas.afetBitti(AfetTuru.SINKHOLE, 1, 0, A, 0.5);
        assertEquals(A.genelBeklemeTick() * 2, yavas.genelKalan(0));
        BeklemeDurumu sifir = new BeklemeDurumu();
        sifir.afetBitti(AfetTuru.SINKHOLE, 1, 0, A, 0);
        assertEquals(A.genelBeklemeTick(), sifir.genelKalan(0), "sıklık 0'da seçim yok; süre değişmez");
    }

    @Test
    void siklikDegisinceKalanOlceklenir() {
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.afetBitti(AfetTuru.DEPREM, 1, 0, A, 1.0);
        long genel = bekleme.genelKalan(2_000);
        long tur = bekleme.turKalan(AfetTuru.DEPREM, 2_000);
        assertTrue(bekleme.siklikDegisti(2_000, 1.0, 10.0));
        assertEquals(Math.round(genel / 10.0), bekleme.genelKalan(2_000));
        assertEquals(Math.round(tur / 10.0), bekleme.turKalan(AfetTuru.DEPREM, 2_000));
        assertTrue(bekleme.siklikDegisti(2_000, 10.0, 1.0));
        assertEquals(genel, bekleme.genelKalan(2_000));
        assertEquals(tur, bekleme.turKalan(AfetTuru.DEPREM, 2_000));
        assertFalse(bekleme.siklikDegisti(2_000, 1.0, 0.0), "sıklık 0 beklemeye dokunmaz");
        assertFalse(bekleme.siklikDegisti(2_000, 1.0, 1.0));
        assertEquals(genel, bekleme.genelKalan(2_000));
        assertFalse(bekleme.siklikDegisti(1_000_000, 1.0, 10.0), "biten bekleme ölçeklenmez");
    }

    @Test
    void yukleVeSifirla() {
        BeklemeDurumu bekleme = new BeklemeDurumu();
        bekleme.yukle(900, Map.of(AfetTuru.DEPREM, 1200L));
        assertEquals(900, bekleme.genelBitis());
        assertEquals(Map.of(AfetTuru.DEPREM, 1200L), bekleme.turBitisleri());
        bekleme.sifirla();
        assertFalse(bekleme.genelBeklemede(0));
        assertTrue(bekleme.turBitisleri().isEmpty());
    }
}
