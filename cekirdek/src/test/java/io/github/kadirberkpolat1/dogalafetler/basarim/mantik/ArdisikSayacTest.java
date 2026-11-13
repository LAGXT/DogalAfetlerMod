package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ArdisikSayacTest {
    @Test
    void kosulKesilinceSifirlanir() {
        ArdisikSayac sayac = new ArdisikSayac();
        for (int i = 1; i <= 9; i++) {
            assertEquals(i, sayac.adim(true));
        }
        assertEquals(0, sayac.adim(false), "N16: en az 10 saniye kesintisiz");
        for (int i = 1; i <= 10; i++) {
            assertEquals(i, sayac.adim(true));
        }
        sayac.sifirla();
        assertEquals(0, sayac.deger());
    }
}
