package io.github.kadirberkpolat1.dogalafetler.basarim.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SagCikanTuretimiTest {
    private static final UUID A = new UUID(0, 1);
    private static final UUID B = new UUID(0, 2);
    private static final UUID C = new UUID(0, 3);
    private static final UUID D = new UUID(0, 4);

    @Test
    void dogalOlmayanBitisteKimseSagCikmaz() {
        assertEquals(Set.of(), SagCikanTuretimi.sagCikanlar(false, Set.of(A, B), Set.of(A, B), Set.of(), Set.of(),
                Set.of(A, B)));
    }

    @Test
    void sagCikanlarEtkilenenlerleKesisir() {
        // Olaydaki sağ çıkan etkilenen değilse sayılmaz (sözleşme: sağ çıkan ⊆ etkilenen).
        assertEquals(Set.of(A), SagCikanTuretimi.sagCikanlar(true, Set.of(A, B), Set.of(A, C), Set.of(), Set.of(),
                Set.of(A, B, C)));
    }

    @Test
    void payliAlandakilerCanliVeCevrimiciyseEklenir() {
        Set<UUID> sonuc = SagCikanTuretimi.sagCikanlar(true, Set.of(A), Set.of(A), Set.of(B, C, D), Set.of(C),
                Set.of(A, B, C));
        assertEquals(Set.of(A, B), sonuc, "C öldü, D çevrim dışı");
    }

    @Test
    void seviyeKosulu() {
        assertTrue(SagCikanTuretimi.seviyeYeterli(1, 0), "en az seviye yoksa her afet");
        assertTrue(SagCikanTuretimi.seviyeYeterli(0, 0), "seviyesiz afet de");
        assertFalse(SagCikanTuretimi.seviyeYeterli(2, 3));
        assertTrue(SagCikanTuretimi.seviyeYeterli(3, 3));
        assertTrue(SagCikanTuretimi.seviyeYeterli(5, 3));
        assertFalse(SagCikanTuretimi.seviyeYeterli(4, 5));
        assertTrue(SagCikanTuretimi.seviyeYeterli(5, 5));
        assertFalse(SagCikanTuretimi.seviyeYeterli(0, 3), "seviyesiz afet (güneş, ay) seviye başarımı vermez");
    }

    @Test
    void birlikteHayattaUcOyuncu() {
        assertFalse(SagCikanTuretimi.birlikteHayatta(2, 3));
        assertTrue(SagCikanTuretimi.birlikteHayatta(3, 3));
        assertTrue(SagCikanTuretimi.birlikteHayatta(10, 3));
    }
}
