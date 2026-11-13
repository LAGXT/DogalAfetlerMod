package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DonmaEsikleriTest {
    private final DonmaEsikleri e = DonmaEsikleri.VARSAYILAN;

    @Test
    void karKatmaniIlerlemeyleArtarCimendeTekKattaKalir() {
        assertEquals(0, e.karKatmani(14, false));
        assertEquals(1, e.karKatmani(15, false));
        assertEquals(5, e.karKatmani(100, false));
        assertEquals(1, e.karKatmani(100, true));
        int onceki = 0;
        for (int l = 0; l <= 110; l++) {
            int k = e.karKatmani(l, false);
            assertTrue(k >= onceki, "kar katmanı azaldı: " + l);
            assertTrue(e.karKatmaniCozulme(l, false) <= k, "çözülmede kar donmadakinden fazla: " + l);
            onceki = k;
        }
    }

    @Test
    void kiyidaOnceVeKalinDonar() {
        assertEquals(1, e.buzKalinligi(25, 0));
        assertEquals(0, e.buzKalinligi(25, 10));
        assertEquals(4, e.buzKalinligi(100, 0));
        assertEquals(3, e.buzKalinligi(100, 15));
        assertEquals(e.buzKalinligi(100, 15), e.buzKalinligi(100, 40), "uzaklık sınırda kesilmeli");
        for (int c = 0; c <= 15; c++) {
            int onceki = 0;
            for (int l = 0; l <= 110; l++) {
                int k = e.buzKalinligi(l, c);
                assertTrue(k >= onceki && k <= 4, "kalınlık tekdüze değil: l=" + l + " c=" + c);
                onceki = k;
            }
        }
    }

    /** Madde 9 ÇÖZÜLME: 100'den inerken önce kar, sonra buz kenardan içeri açılır. */
    @Test
    void cozulurkenKarBuzdanOnceKalkarGolOrtasiKiyidanSonraAcilir() {
        int karBitti = -1;
        int[] buzAcildi = new int[16];
        java.util.Arrays.fill(buzAcildi, -1);
        for (int l = 100; l >= 0; l--) {
            if (karBitti < 0 && e.karKatmaniCozulme(l, false) == 0) {
                karBitti = l;
            }
            for (int c = 0; c <= 15; c++) {
                if (buzAcildi[c] < 0 && e.buzKalinligiCozulme(l, c) == 0) {
                    buzAcildi[c] = l;
                }
            }
        }
        for (int c = 0; c <= 15; c++) {
            assertTrue(karBitti > buzAcildi[c], "kar buzdan önce kalkmalı: kar " + karBitti + ", buz(c=" + c + ") "
                    + buzAcildi[c]);
            if (c > 0) {
                assertTrue(buzAcildi[c] <= buzAcildi[c - 1], "göl ortası kıyıdan sonra açılmalı: c=" + c);
            }
        }
        assertTrue(buzAcildi[15] < buzAcildi[0], "kıyı ile orta arasında fark olmalı");
    }

    @Test
    void taban40taKiyidaBuzYokOrtadaKalinKarYok() {
        int l = 40;
        assertEquals(0, e.karKatmaniCozulme(l, false));
        assertEquals(0, e.karKatmaniCozulme(l, true));
        assertEquals(0, e.buzKalinligiCozulme(l, 0));
        assertTrue(e.buzKalinligiCozulme(l, 15) >= 3, "göl ortası kalın kalmalı");
    }

    @Test
    void sifirSeviyedeEnBuyukTitresimleBileHicbirSeyKalmaz() {
        int l = e.titresimGenligi();
        assertEquals(0, e.karKatmani(l, false));
        assertEquals(0, e.buzKalinligi(l, 0));
        assertEquals(0, e.karKatmaniCozulme(l, false));
        for (int c = 0; c <= 40; c++) {
            assertEquals(0, e.buzKalinligiCozulme(l, c), "c=" + c);
        }
    }

    @Test
    void titresimDeterministikVeSinirli() {
        for (int i = 0; i < 2000; i++) {
            int t = DonmaEsikleri.titresim(99L, i * 31 - 9000, i * 17 + 4000, 8);
            assertTrue(t >= -8 && t <= 8, "titreşim sınır dışı: " + t);
            assertEquals(t, DonmaEsikleri.titresim(99L, i * 31 - 9000, i * 17 + 4000, 8));
        }
        assertEquals(0, e.titresimsiz().titresimGenligi());
    }

    /** D2, K-D33: varsayılan sıra madde 9 (kırağı 10 < ilk kar 15 < kıyı buzu 20 < kar yağışı 30 ≤ kalın buz 40). */
    @Test
    void varsayilanSiraKiragiKarKiyiBuzuKalinBuz() {
        assertEquals(0, e.karKatmani(14, false));
        assertEquals(0, e.buzSeviyesi(19, 0), "kıyı buzu ilk kardan sonra");
        assertEquals(1, e.karKatmani(19, false), "19'da kar var, buz yok");
        assertTrue(e.buzSeviyesi(20, 0) > 0);
        assertEquals(e.yapiKarEsigi(), 40, "yapı karı kalın buz aşamasında (ilk buz + kalınlık adımı)");
        assertTrue(e.buzKalinligi(40, 0) >= 2, "40'ta kıyıda kalın buz");
        int kar = DonmaEsikleri.VARSAYILAN_KAR_YAGISI;
        assertEquals(kar, e.siraliKarYagisi(kar), "varsayılan kar yağışı sırada");
        assertTrue(kar > e.ilkBuzEsigi() && kar <= e.kalinBuzEsigi(), "kıyı buzu < kar yağışı ≤ kalın buz");
        assertTrue(kar < 35, "vitrin %35 kar yağışında (tasarım §3/§7)");
    }

    /** K-D33: sıra dışı kar yağışı eşiği en yakın sınıra çekilir. */
    @Test
    void karYagisiKiyiBuzuIleKalinBuzArasinaCekilir() {
        assertEquals(21, e.siraliKarYagisi(15), "kıyı buzundan önce olamaz");
        assertEquals(21, e.siraliKarYagisi(20), "kıyı buzuyla aynı olamaz");
        assertEquals(40, e.siraliKarYagisi(40), "kalın buzla aynı olabilir");
        assertEquals(40, e.siraliKarYagisi(100), "kalın buzdan sonra olamaz");
        DonmaEsikleri baska = new DonmaEsikleri(new int[] {15, 55}, new int[] {55, 65}, 25, 10, 2, 15, 4, 45, 3, 10, 10, 8);
        assertEquals(35, baska.kalinBuzEsigi());
        assertEquals(26, baska.siraliKarYagisi(0));
    }

    @Test
    void bozukSiraIlkKarIlkBuzdanOnceyeCekilir() {
        int[] kar = {25, 55, 70, 85, 95};
        int[] sirali = DonmaEsikleri.siraliKarEsikleri(kar, 20, 8);
        assertEquals(19, sirali[0]);
        assertEquals(55, sirali[1]);
        assertEquals(25, kar[0], "girdi değişmez");
        int[] dogru = {15, 55, 70, 85, 95};
        assertTrue(DonmaEsikleri.siraliKarEsikleri(dogru, 20, 8) == dogru, "sıra doğruysa aynı dizi");
        assertEquals(9, DonmaEsikleri.siraliKarEsikleri(new int[] {12, 55}, 9, 8)[0], "titreşimin üstünde kalır");
    }

    /** D8: yapı karı doğal zeminden geç gelir, önce kalkar (kıyı buzu 45'ten önce). */
    @Test
    void yapiKariGecGelirOnceKalkar() {
        assertEquals(0, e.yapiKarKatmani(39, 1));
        assertEquals(1, e.yapiKarKatmani(40, 1));
        assertEquals(5, e.yapiKarKatmani(100, 1));
        assertEquals(0, e.yapiKarKatmani(54, -1));
        assertTrue(e.yapiKarKatmani(54, -1) == 0 && e.buzSeviyesiCozulme(54, 0) > 0, "çatı karı kıyı buzundan önce kalkar");
        DonmaEsikleri ozel = e.yapiKari(60, 70);
        assertEquals(0, ozel.yapiKarKatmani(59, 1));
        assertEquals(0, ozel.titresimsiz().yapiKarKatmani(69, -1), "titreşimsiz kopya yapı eşiklerini taşır");
        assertThrows(IllegalArgumentException.class, () -> e.yapiKari(50, 40));
    }

    @Test
    void gecersizEsiklerReddedilir() {
        int[] kar = {25, 55, 70, 85, 95};
        int[] coz = {55, 65, 75, 85, 95};
        assertThrows(IllegalArgumentException.class,
                () -> new DonmaEsikleri(kar, coz, 20, 20, 2, 15, 4, 45, 3, 10, 10, 10), "titreşim en düşük eşiğe ulaşamaz");
        assertThrows(IllegalArgumentException.class,
                () -> new DonmaEsikleri(new int[] {25, 20}, new int[] {30, 40}, 20, 20, 2, 15, 4, 45, 3, 10, 10, 8));
        assertThrows(IllegalArgumentException.class,
                () -> new DonmaEsikleri(kar, new int[] {20, 65, 75, 85, 95}, 20, 20, 2, 15, 4, 45, 3, 10, 10, 8),
                "çözülme eşiği donmadakinden küçük olamaz");
    }
}
