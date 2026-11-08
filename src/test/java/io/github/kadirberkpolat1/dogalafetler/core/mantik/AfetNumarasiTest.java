package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AfetNumarasiTest {
    /** AS-16: aynı saatte başlayan iki afet farklı numara alır; numara saatin gerisinde kalmaz; no + 1 boş kalır. */
    @Test
    void aynSaatteFarkliNumara() {
        long sonraki = 0;
        long ilk = AfetNumarasi.ver(500, sonraki);
        sonraki = AfetNumarasi.sonraki(ilk);
        long ikinci = AfetNumarasi.ver(500, sonraki);
        sonraki = AfetNumarasi.sonraki(ikinci);
        assertEquals(500, ilk);
        assertTrue(ikinci > ilk + 1, "tsunaminin ikinci dalga kimliği (no + 1) bir sonraki afetle çakışmamalı: " + ikinci);
        assertEquals(10_000, AfetNumarasi.ver(10_000, sonraki), "saat ilerleyince numara saate döner");
    }

    @Test
    void eskiKayitSaatinOtesindenBaslar() {
        long sonraki = AfetNumarasi.eskiKayittan(7_000);
        assertTrue(AfetNumarasi.ver(7_000, sonraki) > 7_000 + 1, "kayıttaki süren afetin (no ≤ saat) numarasıyla çakışmaz");
    }
}
