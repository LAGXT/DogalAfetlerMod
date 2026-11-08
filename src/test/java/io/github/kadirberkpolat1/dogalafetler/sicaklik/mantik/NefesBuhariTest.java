package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/**
 * S3-11: üşümede ağızdan çıkan buhar her arka planda (gök, çimen, kar) seçilir. Gerçek parçacık dokusu
 * ({@code nefes_buhari_0..3}) ile parçacığın opaklık eğrisi birlikte sınanır: bulutçuk ömrünün ilk yarısında yoğun, gri
 * kenarlı (beyaz gökte ve karda kaybolmaz), yumuşak söner. Nefes ritmi: ~2–3 sn'de bir birkaç bulutçuk, donmada daha
 * yoğun ve sıkışık; dünya donması sürerken hiç (çift buhar yok).
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
    void kenarGriGokteVeKardaSecilir() throws IOException {
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
        assertTrue(kenarP / kenar <= icP / ic - 25, "kenar içten koyu: kenar " + kenarP / kenar + " iç " + icP / ic);
        // Kar bloğu (~245) önünde beyaz iç kaybolur; bulutu gri kenar seçtirir.
        assertTrue(kenarP / kenar <= 170, "kenar kar önünde seçilecek kadar gri: " + kenarP / kenar);
        assertTrue(kenarA / kenar >= 0.5, "kenar seçilecek kadar opak: " + kenarA / kenar);
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
                "donmada daha çok bulutçuk");
        int[] usume = nefes(SicaklikDurumu.USUME);
        int[] donma = nefes(SicaklikDurumu.DONMA);
        // Donmada bulutçuklar aynı tick'te üst üste çıkar: bulut küçük kalır ve opaklığı birikir (yayılıp pusa dönmez).
        assertTrue(donma[1] >= 3, "donmada tick başına en az üç bulutçuk: " + donma[1]);
        assertTrue(donma[2] < usume[2], "donma nefesi daha kısa sürede çıkar: " + donma[2] + " / " + usume[2]);
    }

    /** Bir nefes (tick 0'dan): {toplam bulutçuk, bir tick'teki en çok, bulutçuk çıkan tick sayısı}. */
    private static int[] nefes(SicaklikDurumu durum) {
        NefesBuhari.Ritim r = new NefesBuhari.Ritim();
        int toplam = 0;
        int enCok = 0;
        int sure = 0;
        for (long t = 0; t < NefesBuhari.DONGU; t++) {
            int n = r.tick(t, durum, false, 0);
            toplam += n;
            enCok = Math.max(enCok, n);
            sure += n > 0 ? 1 : 0;
        }
        assertEquals(NefesBuhari.bulutcuk(durum), toplam, durum + " nefesinin bütün bulutçukları çıkar");
        return new int[] {toplam, enCok, sure};
    }

    @Test
    void nefesIkiUcSaniyedeBirAlinir() {
        for (SicaklikDurumu durum : new SicaklikDurumu[] {SicaklikDurumu.USUME, SicaklikDurumu.DONMA}) {
            NefesBuhari.Ritim r = new NefesBuhari.Ritim();
            long onceki = -1;
            long sonBaslangic = -1;
            int nefesSayisi = 0;
            for (long t = 0; t < 20 * 30; t++) {
                int n = r.tick(t, durum, false, (int) (t / 7 % 2));
                if (n > 0 && (onceki < 0 || t - onceki > 1)) {
                    if (sonBaslangic >= 0) {
                        long ara = t - sonBaslangic;
                        assertTrue(ara >= 40 && ara <= 60, durum + ": nefesler arası " + ara + " tick (2–3 sn olmalı)");
                    }
                    sonBaslangic = t;
                    nefesSayisi++;
                }
                if (n > 0) {
                    onceki = t;
                }
            }
            assertTrue(nefesSayisi >= 10 && nefesSayisi <= 15, durum + ": 30 sn'de " + nefesSayisi + " nefes");
        }
    }

    @Test
    void dunyaDonarkenVeIlikkenNefesYok() {
        assertTrue(!NefesBuhari.uretilir(SicaklikDurumu.USUME, true), "dünya donmasının nefesi varken çift buhar olmaz");
        assertTrue(!NefesBuhari.uretilir(SicaklikDurumu.DONMA, true));
        assertTrue(NefesBuhari.uretilir(SicaklikDurumu.USUME, false) && NefesBuhari.uretilir(SicaklikDurumu.DONMA, false));
        for (SicaklikDurumu d : new SicaklikDurumu[] {SicaklikDurumu.NORMAL, SicaklikDurumu.BUNALMA, SicaklikDurumu.YANMA}) {
            assertTrue(!NefesBuhari.uretilir(d, false), d + " durumunda nefes buharı yok");
        }
        NefesBuhari.Ritim donarken = new NefesBuhari.Ritim();
        for (long t = 0; t < 200; t++) {
            assertEquals(0, donarken.tick(t, SicaklikDurumu.DONMA, true, 1), "dünya donarken tick " + t);
        }
        // Nefesin ortasında dünya donmaya başlarsa kalan bulutçuklar da çıkmaz, sonradan birikmiş nefes gelmez.
        NefesBuhari.Ritim r = new NefesBuhari.Ritim();
        assertTrue(r.tick(0, SicaklikDurumu.USUME, false, 0) > 0);
        assertEquals(0, r.tick(1, SicaklikDurumu.USUME, true, 0));
        for (long t = 2; t < NefesBuhari.DONGU; t++) {
            assertEquals(0, r.tick(t, SicaklikDurumu.USUME, false, 0), "donma bitince bir sonraki nefese dek sessiz: " + t);
        }
        assertTrue(r.tick(NefesBuhari.DONGU, SicaklikDurumu.USUME, false, 0) > 0, "sonraki nefes zamanında gelir");
    }
}
