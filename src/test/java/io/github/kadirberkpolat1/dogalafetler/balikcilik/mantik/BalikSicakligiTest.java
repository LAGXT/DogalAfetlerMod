package io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** B-19: balık sıcaklıkları yiyecek kademeleriyle (küçük 8, orta 20, büyük 35; %25 anlık) birebir. */
class BalikSicakligiTest {
    @Test
    void kademeSayilariYiyecekleAyni() {
        assertEquals(-8, BalikSicakligi.CIG_KUCUK.toplam());
        assertEquals(60 * 20, BalikSicakligi.CIG_KUCUK.sureTick());
        assertEquals(20, BalikSicakligi.PISMIS_ORTA.toplam());
        assertEquals(90 * 20, BalikSicakligi.PISMIS_ORTA.sureTick());
        assertEquals(35, BalikSicakligi.RINGA_BUYUK.toplam());
        assertEquals(120 * 20, BalikSicakligi.RINGA_BUYUK.sureTick());
    }

    @Test
    void ceyregiAnindaKalaniSureli() {
        assertEquals(-2, BalikSicakligi.CIG_KUCUK.anlik(), 1e-9);
        assertEquals(-6, BalikSicakligi.CIG_KUCUK.sureli(), 1e-9);
        assertEquals(5, BalikSicakligi.PISMIS_ORTA.anlik(), 1e-9);
        assertEquals(15, BalikSicakligi.PISMIS_ORTA.sureli(), 1e-9);
        assertEquals(8.75, BalikSicakligi.RINGA_BUYUK.anlik(), 1e-9);
        assertEquals(26.25, BalikSicakligi.RINGA_BUYUK.sureli(), 1e-9);
    }
}
