package io.github.kadirberkpolat1.dogalafetler.su;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CekilmeTest {
    private final Cekilme cekilme = new Cekilme(0, 0, 40, 3, 100, 60);

    @Test
    void merkezdeTamDususKenardaSifir() {
        assertEquals(3, cekilme.dusus(0, 0, 160), 1e-9);
        assertEquals(1.5, cekilme.dusus(0, 0, 130), 1e-9);
        assertEquals(0, cekilme.dusus(40, 0, 160), 1e-9);
        assertEquals(0, cekilme.dusus(0, 0, 100), 1e-9);
    }

    @Test
    void gizlenenKatmanTamDususunYukariYuvarlanmasi() {
        assertEquals(3, cekilme.gizlenenKatman(0, 0));
        assertEquals(0, cekilme.gizlenenKatman(41, 0));
    }
}
