package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * S3-11: üşümede ağızdan çıkan buhar her arka planda seçilir. Gerçek parçacık dokusu ({@code nefes_buhari_0..3}) ile
 * parçacığın opaklık eğrisi birlikte sınanır: bulutçuk ömrünün ilk yarısında yoğun, açık gri kenarlı (beyaz gökte
 * kaybolmaz), yumuşak söner; nefes birkaç bulutçuktan oluşur, donmada daha yoğun.
 */
class NefesBuhariTest {
    /** Ömrün ilk yarısında gösterilen kareler ({@code SpriteSet.get(yaş, ömür)}, 4 kare). */
    private static final int[] ILK_KARELER = {0, 1};

    private static BufferedImage kare(int i) throws IOException {
        try (InputStream girdi = NefesBuhariTest.class.getResourceAsStream(
                "/assets/dogalafetler/textures/particle/nefes_buhari_" + i + ".png")) {
            return ImageIO.read(girdi);
        }
    }

    private static int alfa(BufferedImage k, int x, int y) {
        return x < 0 || y < 0 || x >= k.getWidth() || y >= k.getHeight() ? 0 : k.getRGB(x, y) >>> 24;
    }

    private static double parlaklik(int argb) {
        return 0.2126 * (argb >> 16 & 0xFF) + 0.7152 * (argb >> 8 & 0xFF) + 0.0722 * (argb & 0xFF);
    }

    @Test
    void bulutcukOmrununIlkYarisindaBelirgin() throws IOException {
        for (int i : ILK_KARELER) {
            BufferedImage k = kare(i);
            double toplam = 0;
            int dolu = 0;
            for (int y = 0; y < k.getHeight(); y++) {
                for (int x = 0; x < k.getWidth(); x++) {
                    int a = alfa(k, x, y);
                    if (a > 0) {
                        toplam += a / 255.0;
                        dolu++;
                    }
                }
            }
            double dokuOrt = toplam / dolu;
            for (float oran = 0f; oran <= 0.45f; oran += 0.05f) {
                double gorunen = dokuOrt * NefesBuhari.ALFA * NefesBuhari.opaklik(oran);
                assertTrue(gorunen >= 0.5, "kare " + i + " oran " + oran + ": ortalama opaklık " + gorunen
                        + " (doku " + dokuOrt + ")");
            }
        }
    }

    @Test
    void kenarAcikGriGokteKaybolmaz() throws IOException {
        BufferedImage k = kare(0);
        double kenarP = 0;
        double kenarA = 0;
        int kenar = 0;
        double icP = 0;
        int ic = 0;
        for (int y = 0; y < k.getHeight(); y++) {
            for (int x = 0; x < k.getWidth(); x++) {
                if (alfa(k, x, y) == 0) {
                    continue;
                }
                boolean sinir = alfa(k, x + 1, y) == 0 || alfa(k, x - 1, y) == 0 || alfa(k, x, y + 1) == 0
                        || alfa(k, x, y - 1) == 0;
                if (sinir) {
                    kenarP += parlaklik(k.getRGB(x, y));
                    kenarA += alfa(k, x, y) / 255.0;
                    kenar++;
                } else {
                    icP += parlaklik(k.getRGB(x, y));
                    ic++;
                }
            }
        }
        assertTrue(kenar > 0 && ic > 0, "bulutçuğun kenarı ve içi olmalı");
        assertTrue(kenarP / kenar <= icP / ic - 25, "kenar içten koyu (açık gri): kenar " + kenarP / kenar + " iç "
                + icP / ic);
        assertTrue(kenarA / kenar >= 0.35, "kenar seçilecek kadar opak: " + kenarA / kenar);
    }

    @Test
    void yumusakSoner() {
        assertEquals(1f, NefesBuhari.opaklik(0f), 1e-6);
        assertTrue(NefesBuhari.opaklik(1f) <= 0.05f, "ömür sonunda görünmez");
        float onceki = 1f;
        for (float oran = 1 / 30f; oran <= 1f; oran += 1 / 30f) {
            float o = NefesBuhari.opaklik(oran);
            assertTrue(o <= onceki + 1e-6 && onceki - o <= 0.12f, "oran " + oran + ": " + onceki + " -> " + o);
            onceki = o;
        }
        assertTrue(NefesBuhari.buyume(1f) > NefesBuhari.buyume(0f), "bulutçuk yayılır");
    }

    @Test
    void nefesBulutOlurDonmadaDahaYogun() {
        assertTrue(NefesBuhari.bulutcuk(SicaklikDurumu.USUME) >= 5, "üşüme nefesi küçük bir bulut");
        assertTrue(NefesBuhari.bulutcuk(SicaklikDurumu.DONMA) > NefesBuhari.bulutcuk(SicaklikDurumu.USUME),
                "donmada daha yoğun");
    }
}
