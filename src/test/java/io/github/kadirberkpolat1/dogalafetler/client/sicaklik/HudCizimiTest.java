package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** S3-07 (§A8): sıcaklık bantının oyuncunun gördüğü yerleşim, dolum, çentik, şevron, metin ve animasyon davranışı. */
class HudCizimiTest {
    /** Varsayılan eşikler (S3-04, vücut birimi). */
    private static final float SOGUK = -20;
    private static final float COK_SOGUK = -40;
    private static final float DONMA = -70;
    private static final float SICAK = 20;
    private static final float COK_SICAK = 50;
    private static final float YANMA = 70;

    private static boolean dolu(float vucut, int x) {
        int[] a = HudCizimi.dolguAraligi(vucut);
        return a[0] <= x && x < a[1];
    }

    @Test
    void hapVucuduSantigratYazar() {
        assertEquals("36.4°", HudCizimi.sayiMetni(-12));
        assertEquals("37.0°", HudCizimi.sayiMetni(0));
        assertEquals("32.0°", HudCizimi.sayiMetni(-100));
        assertEquals("42.0°", HudCizimi.sayiMetni(100));
        assertEquals("-6°", HudCizimi.hissedilenMetni(-6.3f));
        assertEquals("28°", HudCizimi.hissedilenMetni(27.6f));
    }

    @Test
    void soguktaSolYariSicaktaSagYariMerkezdenDisaDolar() {
        int[] soguk = HudCizimi.dolguAraligi(-50);
        assertEquals(HudCizimi.HAP_X - 1, soguk[1], "soğuk dolum hapın kenarından başlar");
        assertEquals(36, soguk[1] - soguk[0]);
        assertTrue(soguk[0] > 0, "tüp çerçevesine taşmaz");

        int[] sicak = HudCizimi.dolguAraligi(50);
        assertEquals(HudCizimi.SICAK_X + 1, sicak[0], "sıcak dolum hapın kenarından başlar");
        assertEquals(36, sicak[1] - sicak[0]);

        int[] notr = HudCizimi.dolguAraligi(0);
        assertEquals(notr[0], notr[1], "nötrde iki yarı da boş");
        int[] uc = HudCizimi.dolguAraligi(-130);
        assertEquals(1, uc[0], "ölçek ucunda tüp tam dolar, çerçeveye taşmaz");
        assertEquals(HudCizimi.GENISLIK - 1, HudCizimi.dolguAraligi(130)[1]);
    }

    @Test
    void centiklerEsiklereDenkVeSimetrik() {
        // Dolum çentiğe kademe değiştiğinde varır: bir pikselden (1,39 birim) fazla öncesinde boş, eşikte ve ötesinde dolu.
        for (float e : new float[] {SOGUK, COK_SOGUK, DONMA, SICAK, COK_SICAK, YANMA}) {
            int x = HudCizimi.centik(e);
            float yon = Math.signum(e);
            assertTrue(!dolu(e - yon * 1.5f, x), "eşik " + e + " öncesinde çentik boş");
            assertTrue(dolu(e, x), "eşikte " + e + " çentik dolu");
            assertTrue(dolu(e + yon * 1.5f, x), "eşik " + e + " aşılınca çentik dolu");
            assertEquals(HudCizimi.GENISLIK - 1 - x, HudCizimi.centik(-e), "merkeze göre ayna");
        }
        assertTrue(HudCizimi.centik(SOGUK) > HudCizimi.centik(COK_SOGUK)
                && HudCizimi.centik(COK_SOGUK) > HudCizimi.centik(DONMA), "soğuk çentikler merkezden dışa");
        assertTrue(HudCizimi.centik(DONMA) > 0 && HudCizimi.centik(SOGUK) < HudCizimi.HAP_X, "soğuk tüpün içinde");
        assertTrue(HudCizimi.centik(SICAK) > HudCizimi.SICAK_X && HudCizimi.centik(YANMA) < HudCizimi.GENISLIK - 1);
    }

    @Test
    void hissedilenUcgeniSoguktaSoldaNotrdeOrtada() {
        assertEquals(HudCizimi.GENISLIK / 2, HudCizimi.ucgen(0.3f));
        assertTrue(HudCizimi.ucgen(-60) < HudCizimi.HAP_X);
        assertTrue(HudCizimi.ucgen(60) > HudCizimi.SICAK_X);
        assertEquals(HudCizimi.ucgen(-100), HudCizimi.ucgen(-250), "ölçek dışı hissedilen tüp ucunda kalır");
    }

    @Test
    void sevronSayisiEgilimHizinaGore() {
        assertEquals(0, HudCizimi.sevron(0.03f));
        assertEquals(1, HudCizimi.sevron(0.1f));
        assertEquals(-2, HudCizimi.sevron(-0.3f));
        assertEquals(3, HudCizimi.sevron(0.7f));
        assertEquals(-3, HudCizimi.sevron(-2f));
        for (int i = 0; i < 3; i++) {
            int k = HudCizimi.sevronKaymasi(i, 17f);
            assertTrue(k >= 0 && k <= 2);
        }
        assertTrue(HudCizimi.sevronKaymasi(0, 0f) != HudCizimi.sevronKaymasi(0, 4f), "şevron zamanla kayar");
    }

    @Test
    void yalnizEtkinDurumIkonlariSoldanSaga() {
        assertEquals(List.of(), HudCizimi.ikonlar(SicaklikDurumu.NORMAL, 0.02f));
        assertEquals(List.of(HudCizimi.Ikon.ISLAK, HudCizimi.Ikon.USUME), HudCizimi.ikonlar(SicaklikDurumu.USUME, 0.6f));
        assertEquals(List.of(HudCizimi.Ikon.YANMA), HudCizimi.ikonlar(SicaklikDurumu.YANMA, 0f));
        assertEquals(List.of(HudCizimi.Ikon.ISLAK), HudCizimi.ikonlar(SicaklikDurumu.NORMAL, 0.05f));
        assertEquals(1, HudCizimi.damlaDolulugu(0.05f), "görünür ıslaklıkta damla boş görünmez");
        assertEquals(HudCizimi.IKON, HudCizimi.damlaDolulugu(1f));
        assertTrue(HudCizimi.damlaDolulugu(0.3f) < HudCizimi.damlaDolulugu(0.7f));
    }

    @Test
    void animasyonlarDurumaGore() {
        assertArrayEquals(new int[] {0, 0}, HudCizimi.titreme(SicaklikDurumu.NORMAL, 6));
        boolean donmaTitrer = false;
        for (long t = 0; t < 12; t++) {
            int[] d = HudCizimi.titreme(SicaklikDurumu.DONMA, t);
            assertTrue(Math.abs(d[0]) <= 1 && Math.abs(d[1]) <= 1, "donma titremesi ±1 px");
            donmaTitrer |= d[0] != 0 || d[1] != 0;
        }
        assertTrue(donmaTitrer);

        assertEquals(0f, HudCizimi.nabiz(SicaklikDurumu.USUME, -30, SICAK, YANMA, 10f));
        assertTrue(HudCizimi.nabiz(SicaklikDurumu.YANMA, 60, SICAK, YANMA, 0f) > HudCizimi.nabiz(SicaklikDurumu.BUNALMA,
                30, SICAK, YANMA, 0f), "yanma nabzı bunalmadan güçlü");
        assertTrue(HudCizimi.nabizPeriyodu(68, SICAK, YANMA) < HudCizimi.nabizPeriyodu(22, SICAK, YANMA),
                "hasara yaklaştıkça nabız hızlanır");

        assertEquals(1f, HudCizimi.parlama(100, 100, 0f));
        assertTrue(HudCizimi.parlama(103, 100, 0f) > 0f && HudCizimi.parlama(103, 100, 0f) < 1f);
        assertEquals(0f, HudCizimi.parlama(100 + HudCizimi.PARLAMA_TICK, 100, 0f), "parlama 6 tick'te biter");
        assertEquals(0f, HudCizimi.parlama(50, -1, 0f), "durum hiç değişmediyse parlama yok");
    }

    /**
     * S3-10: hapın vücut sayısı her durumda okunur. Sayı rengi ile hap içinin (atlasın gerçek pikseli; nabızın en parlak
     * anı, siyah ve beyaz dünya zemini üstünde) karşıtlığı WCAG AA ölçüsüyle en az 4,5:1; parlama anında sayıyı ayıran
     * koyu kontur ile en az 7:1.
     */
    @Test
    void hapSayisiHerDurumdaZemindenAyrilir() throws IOException {
        BufferedImage atlas;
        try (InputStream girdi = HudCizimiTest.class.getResourceAsStream(
                "/assets/dogalafetler/textures/gui/sicaklik/hud.png")) {
            atlas = ImageIO.read(girdi);
        }
        // Atlas: haplar v=16, u = 36 · sıra; beyaz nabız/parlama hapı u=180. (17, 5) sayının ortasındaki iç piksel.
        int beyazIc = atlas.getRGB(180 + 17, 16 + 5);
        for (SicaklikDurumu durum : SicaklikDurumu.values()) {
            int ic = atlas.getRGB(HudCizimi.hapSirasi(durum) * 36 + 17, 16 + 5);
            float nabiz = HudCizimi.nabiz(durum, YANMA, SICAK, YANMA, 0f);
            int nabizKatmani = (Math.round((beyazIc >>> 24) * nabiz) << 24) | (HudCizimi.renk(durum) & 0xFFFFFF);
            for (int zemin : new int[] {0x000000, 0xFFFFFF}) {
                int arka = ust(nabizKatmani, ust(ic, zemin));
                double k = kontrast(HudCizimi.sayiRengi(durum), arka);
                assertTrue(k >= 4.5, durum + " zemin " + Integer.toHexString(zemin) + ": sayı "
                        + Integer.toHexString(HudCizimi.sayiRengi(durum)) + " hap içi " + Integer.toHexString(arka)
                        + " karşıtlık " + k);
            }
            double kontur = kontrast(HudCizimi.sayiRengi(durum), HudCizimi.KONTUR);
            assertTrue(kontur >= 7, durum + ": sayı ile kontur karşıtlığı " + kontur);
        }
    }

    /** ARGB ön renk, RGB zeminin üstünde (normal karışım); sonuç RGB. */
    private static int ust(int on, int zemin) {
        double a = (on >>> 24) / 255.0;
        int s = 0;
        for (int kaydir = 0; kaydir <= 16; kaydir += 8) {
            double c = (on >> kaydir & 0xFF) * a + (zemin >> kaydir & 0xFF) * (1 - a);
            s |= (int) Math.round(c) << kaydir;
        }
        return s;
    }

    private static double kontrast(int a, int b) {
        double la = parlaklik(a);
        double lb = parlaklik(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** WCAG göreli parlaklık. */
    private static double parlaklik(int rgb) {
        return 0.2126 * kanal(rgb >> 16 & 0xFF) + 0.7152 * kanal(rgb >> 8 & 0xFF) + 0.0722 * kanal(rgb & 0xFF);
    }

    private static double kanal(int c) {
        double s = c / 255.0;
        return s <= 0.04045 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }
}
