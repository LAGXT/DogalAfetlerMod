package io.github.kadirberkpolat1.dogalafetler.core.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import org.junit.jupiter.api.Test;

class HedefSeciciTest {
    private static List<HedefSecici.Aday<String>> adaylar(String... adlar) {
        List<HedefSecici.Aday<String>> liste = new ArrayList<>();
        for (String ad : adlar) {
            liste.add(new HedefSecici.Aday<>(ad, false));
        }
        return liste;
    }

    @Test
    void ikiVeFazlaOyuncudaAyniOyuncuArtArdaHedefOlmaz() {
        SplittableRandom rastgele = new SplittableRandom(5);
        for (int oyuncu = 2; oyuncu <= 10; oyuncu++) {
            String[] adlar = new String[oyuncu];
            for (int i = 0; i < oyuncu; i++) {
                adlar[i] = "oyuncu" + i;
            }
            List<HedefSecici.Aday<String>> liste = adaylar(adlar);
            String son = null;
            int[] sayilar = new int[oyuncu];
            for (int tur = 0; tur < 5000; tur++) {
                List<String> sira = HedefSecici.sirala(liste, son, rastgele);
                assertEquals(oyuncu - (son == null ? 0 : 1), sira.size());
                String secilen = sira.get(0);
                assertNotEquals(son, secilen, "aynı oyuncu art arda hedef alındı");
                assertTrue(!sira.contains(son), "son hedef aday listesinde kalmamalı");
                sayilar[Integer.parseInt(secilen.substring(6))]++;
                son = secilen;
            }
            for (int sayi : sayilar) {
                assertTrue(sayi > 0, "her oyuncu en az bir kez hedef olmalı");
            }
        }
    }

    @Test
    void tekOyuncuHerZamanHedefOlur() {
        SplittableRandom rastgele = new SplittableRandom(6);
        List<HedefSecici.Aday<String>> liste = adaylar("yalniz");
        assertEquals(List.of("yalniz"), HedefSecici.sirala(liste, "yalniz", rastgele));
        assertEquals(List.of("yalniz"), HedefSecici.sirala(liste, null, rastgele));
        assertTrue(HedefSecici.sirala(new ArrayList<HedefSecici.Aday<String>>(), null, rastgele).isEmpty());
    }

    @Test
    void sonAfetMerkezineUzakOyuncularOnceGelir() {
        SplittableRandom rastgele = new SplittableRandom(7);
        List<HedefSecici.Aday<String>> liste = List.of(new HedefSecici.Aday<>("yakin1", true),
                new HedefSecici.Aday<>("uzak", false), new HedefSecici.Aday<>("yakin2", true));
        for (int i = 0; i < 200; i++) {
            assertEquals("uzak", HedefSecici.sirala(liste, null, rastgele).get(0));
        }
        assertEquals(3, HedefSecici.sirala(liste, null, rastgele).size(), "yakın oyuncular da sırada kalır");
    }
}
