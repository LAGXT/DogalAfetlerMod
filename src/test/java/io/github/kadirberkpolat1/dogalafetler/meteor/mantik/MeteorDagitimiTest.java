package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class MeteorDagitimiTest {
    @Test
    void sirayiOyuncularaEsitDagitirYiginlanmaYok() {
        for (int oyuncu = 1; oyuncu <= 10; oyuncu++) {
            MeteorDagitimi d = new MeteorDagitimi(new Random(oyuncu).nextInt(1000));
            int[] sayi = new int[oyuncu];
            int toplam = oyuncu * 37 + 3;
            for (int i = 0; i < toplam; i++) {
                sayi[d.siradaki(oyuncu)]++;
            }
            int enAz = Integer.MAX_VALUE;
            int enCok = 0;
            for (int s : sayi) {
                enAz = Math.min(enAz, s);
                enCok = Math.max(enCok, s);
            }
            assertTrue(enCok - enAz <= 1, oyuncu + " oyuncu: en çok/en az farkı " + enAz + ".." + enCok);
        }
    }

    @Test
    void oyuncuSayisiDegisinceDeDengeliKalir() {
        MeteorDagitimi d = new MeteorDagitimi(7);
        int[] sayi = new int[6];
        for (int i = 0; i < 300; i++) {
            int n = i < 100 ? 4 : i < 200 ? 6 : 5;
            int s = d.siradaki(n);
            assertTrue(s >= 0 && s < n);
            sayi[s]++;
        }
        double ort = 300 / 6.0;
        double varyans = 0;
        for (int s : sayi) {
            varyans += (s - ort) * (s - ort) / 6;
        }
        assertTrue(varyans < 0.25 * ort * ort, "varyans " + varyans);
        assertEquals(0, new MeteorDagitimi(3).siradaki(0), "oyuncu yoksa 0");
    }

    @Test
    void ofsetHalkaIcindeVeAlanaGoreDuzgun() {
        Random r = new Random(20261106L);
        int ic = 0;
        double topX = 0;
        double topZ = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            double[] o = MeteorDagitimi.ofset(r, 32, 160);
            double m = Math.hypot(o[0], o[1]);
            assertTrue(m >= 32 - 1e-9 && m <= 160 + 1e-9, "halka dışında: " + m);
            if (m < Math.sqrt((32 * 32 + 160 * 160) / 2.0)) {
                ic++;
            }
            topX += o[0];
            topZ += o[1];
        }
        assertEquals(0.5, ic / (double) n, 0.02, "alan ortası yarıçapının içinde yarısı");
        assertEquals(0, topX / n, 3.0, "yön yanlılığı yok (x)");
        assertEquals(0, topZ / n, 3.0, "yön yanlılığı yok (z)");
    }
}
