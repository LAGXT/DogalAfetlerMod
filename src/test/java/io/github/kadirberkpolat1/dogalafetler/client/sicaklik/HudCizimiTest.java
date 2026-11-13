package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import java.util.List;
import org.junit.jupiter.api.Test;

/** K-E74: kafa ikonlu sıcaklık göstergesinin yerleşim, şerit, iğne, kafa tonu, efekt ve animasyon davranışı. */
class HudCizimiTest {
    /** Varsayılan eşikler (S3-04, vücut birimi). */
    private static final float SOGUK = -20;
    private static final float COK_SOGUK = -40;
    private static final float DONMA = -70;
    private static final float SICAK = 20;
    private static final float COK_SICAK = 50;
    private static final float YANMA = 70;

    @Test
    void gostergeVanillaCanIleAclikArasindakiBoslugaSigar() {
        assertEquals(20, HudCizimi.GENISLIK, "can (merkez-10'da biter) ile açlık (merkez+10'da başlar) arası");
        assertTrue(HudCizimi.YUKSEKLIK >= 20 && HudCizimi.YUKSEKLIK <= 24, "20–24 px");
        assertEquals(HudCizimi.GENISLIK, HudCizimi.KAFA + 2 * HudCizimi.KAFA_X);
        assertTrue(HudCizimi.DAMLA_Y + HudCizimi.DAMLA_YUKSEKLIK <= HudCizimi.YUKSEKLIK);
    }

    @Test
    void vucutSayisiSantigratYazar() {
        assertEquals("36.4°", HudCizimi.sayiMetni(-12));
        assertEquals("37.0°", HudCizimi.sayiMetni(0));
        assertEquals("32.0°", HudCizimi.sayiMetni(-100));
        assertEquals("42.0°", HudCizimi.sayiMetni(100));
    }

    @Test
    void seritSoguktaSolYariSicaktaSagYariMerkezdenDisaDolar() {
        assertEquals(4.5f, HudCizimi.yariDolum(50), 1e-6f);
        assertEquals(4.5f, HudCizimi.yariDolum(-50), 1e-6f);
        assertEquals(0f, HudCizimi.yariDolum(0), 1e-6f, "nötrde iki yarı da boş");
        assertEquals(HudCizimi.YARIM_IC, HudCizimi.yariDolum(-130), 1e-6f, "ölçek ucunda yarım tam dolar, taşmaz");
        assertEquals(10, HudCizimi.piksel(true, 0), "sıcak merkezin sağından");
        assertEquals(9, HudCizimi.piksel(false, 0), "soğuk merkezin solundan");
        assertEquals(18, HudCizimi.piksel(true, HudCizimi.YARIM_IC - 1));
        assertEquals(1, HudCizimi.piksel(false, HudCizimi.YARIM_IC - 1), "çerçeve pikseline taşmaz");
        for (int i = 0; i < HudCizimi.YARIM_IC; i++) {
            assertEquals(HudCizimi.GENISLIK - 1, HudCizimi.piksel(true, i) + HudCizimi.piksel(false, i), "merkeze ayna");
        }
        assertNotEquals(HudCizimi.seritRengi(false, 0), HudCizimi.seritRengi(false, 8), "soğuk dışa doğru koyulaşır");
        assertNotEquals(HudCizimi.seritRengi(true, 0), HudCizimi.seritRengi(true, 8), "sıcak dışa doğru koyulaşır");
    }

    @Test
    void centiklerEsiklereDenkVeSimetrik() {
        // Yarım 9 piksel: eşik büyüklüğünün 9/100'ü kadar piksel; eşiğin düştüğü piksel çentik olur.
        assertEquals(8, HudCizimi.centik(SOGUK));
        assertEquals(6, HudCizimi.centik(COK_SOGUK));
        assertEquals(3, HudCizimi.centik(DONMA));
        assertEquals(11, HudCizimi.centik(SICAK));
        assertEquals(14, HudCizimi.centik(COK_SICAK));
        assertEquals(16, HudCizimi.centik(YANMA));
        for (float e : new float[] {SOGUK, COK_SOGUK, DONMA, SICAK, COK_SICAK, YANMA}) {
            assertEquals(HudCizimi.GENISLIK - 1, HudCizimi.centik(e) + HudCizimi.centik(-e), "merkeze göre ayna");
            assertTrue(HudCizimi.centik(e) >= 1 && HudCizimi.centik(e) <= HudCizimi.GENISLIK - 2);
        }
        assertTrue(HudCizimi.centik(SOGUK) > HudCizimi.centik(COK_SOGUK) && HudCizimi.centik(COK_SOGUK) > HudCizimi.centik(DONMA),
                "soğuk çentikler merkezden dışa");
        assertEquals(1, HudCizimi.centik(-250), "ölçek dışı eşik şeridin ucunda kalır");
        assertEquals(18, HudCizimi.centik(250));
    }

    @Test
    void hissedilenIgnesiSoguktaSoldaNotrdeOrtada() {
        assertEquals(HudCizimi.MERKEZ, HudCizimi.igneX(0f));
        assertEquals(5, HudCizimi.igneX(-50));
        assertEquals(14, HudCizimi.igneX(50));
        assertEquals(1, HudCizimi.igneX(-100));
        assertEquals(HudCizimi.GENISLIK - 2, HudCizimi.igneX(100));
        assertEquals(HudCizimi.igneX(-100), HudCizimi.igneX(-250), "ölçek dışı hissedilen şeridin ucunda kalır");
    }

    @Test
    void islaklikDamlasiYalnizIslakkenCizilir() {
        assertTrue(HudCizimi.damla(0f, 10f).isEmpty(), "kuruyken damla yok");
        assertTrue(HudCizimi.damla(0.04f, 10f).isEmpty(), "görünür eşiğin altında damla yok");
        assertTrue(!HudCizimi.damla(0.05f, 10f).isEmpty(), "eşikte damla var");
        assertTrue(!HudCizimi.damla(1f, 10f).isEmpty());
    }

    @Test
    void islaklikDamlasiKafaninSagindaKalirVeAlttanDolar() {
        for (float w : new float[] {0.05f, 0.3f, 0.7f, 1f}) {
            for (float zaman : new float[] {0f, 20f, 35f, 50f, 69f}) {
                for (HudCizimi.Parcacik p : HudCizimi.damla(w, zaman)) {
                    assertTrue(p.x() >= HudCizimi.KAFA_X + HudCizimi.KAFA + 1, "kafanın sağında: " + p);
                    assertTrue(p.x() + p.w() <= HudCizimi.DAMLA_X + HudCizimi.DAMLA_GENISLIK, "damla genişliği: " + p);
                    assertTrue(p.y() >= 0 && p.y() + p.h() <= HudCizimi.YUKSEKLIK, "göstergenin yüksekliğinde: " + p);
                }
            }
        }
        long azDolu = HudCizimi.damla(0.1f, 0f).stream().filter(p -> p.argb() == 0xFF5FB4FF).count();
        long cokDolu = HudCizimi.damla(1f, 0f).stream().filter(p -> p.argb() == 0xFF5FB4FF).count();
        assertTrue(azDolu >= 1 && cokDolu > azDolu, "dolu iç satırlar ıslaklıkla artar: " + azDolu + " < " + cokDolu);
    }

    @Test
    void islaklikDamlasiYalnizYogunIslaklikdaDamlar() {
        int damlaAlti = HudCizimi.DAMLA_Y + HudCizimi.DAMLA_YUKSEKLIK;
        boolean damlayan = false;
        for (float zaman = 0; zaman < 70; zaman += 1) {
            damlayan |= HudCizimi.damla(1f, zaman).stream().anyMatch(p -> p.y() > damlaAlti);
            assertTrue(HudCizimi.damla(0.2f, zaman).stream().noneMatch(p -> p.y() > damlaAlti),
                    "hafif ıslaklıkta damlacık düşmez");
        }
        assertTrue(damlayan, "yoğun ıslaklıkta damlacık düşer");
    }

    @Test
    void kafaTonuDurumaGore() {
        assertArrayEquals(new float[] {1f, 1f, 1f}, HudCizimi.kafaTonu(SicaklikDurumu.NORMAL, 0f));
        float[] usume = HudCizimi.kafaTonu(SicaklikDurumu.USUME, 0f);
        float[] donma = HudCizimi.kafaTonu(SicaklikDurumu.DONMA, 0f);
        assertTrue(usume[2] > usume[0] && donma[2] > donma[0], "soğukta mavi ağır basar");
        assertTrue(donma[0] < usume[0], "donma üşümeden daha mavi");
        float[] yanma = HudCizimi.kafaTonu(SicaklikDurumu.YANMA, 0f);
        float[] bunalma = HudCizimi.kafaTonu(SicaklikDurumu.BUNALMA, 0f);
        assertTrue(yanma[0] > yanma[1] && yanma[0] > yanma[2], "yanmada kızıl");
        assertTrue(yanma[1] < bunalma[1], "yanma bunalmadan kızıl");
        assertTrue(HudCizimi.kafaTonu(SicaklikDurumu.YANMA, 0.6f)[1] < yanma[1], "nabız kızıllığı derinleştirir");
    }

    @Test
    void herDurumunKendiKafaEfektiVarNormaldeYok() {
        assertEquals(List.of(), HudCizimi.efekt(SicaklikDurumu.NORMAL, 10f));
        assertEquals("", HudCizimi.efektAdi(SicaklikDurumu.NORMAL));
        for (SicaklikDurumu durum : SicaklikDurumu.values()) {
            if (durum == SicaklikDurumu.NORMAL) {
                continue;
            }
            assertTrue(!HudCizimi.efekt(durum, 10f).isEmpty(), durum + " efekti boş olmamalı");
            assertTrue(!HudCizimi.efektAdi(durum).isEmpty());
        }
        assertEquals("TER", HudCizimi.efektAdi(SicaklikDurumu.BUNALMA));
        assertEquals("ALEV", HudCizimi.efektAdi(SicaklikDurumu.YANMA));
        assertEquals("BUHAR", HudCizimi.efektAdi(SicaklikDurumu.USUME));
        assertEquals("BUZ", HudCizimi.efektAdi(SicaklikDurumu.DONMA));
    }

    @Test
    void efektlerGostergeninIcindeKalirYalnizDumanUstteTasar() {
        for (SicaklikDurumu durum : SicaklikDurumu.values()) {
            for (float zaman = 0; zaman < 300; zaman += 0.5f) {
                for (HudCizimi.Parcacik p : HudCizimi.efekt(durum, zaman)) {
                    String yer = durum + " zaman " + zaman + " " + p;
                    assertTrue(p.x() >= 0 && p.x() + p.w() <= HudCizimi.GENISLIK, "yatayda taşmaz: " + yer);
                    assertTrue(p.y() >= -6 && p.y() + p.h() <= HudCizimi.SERIT_Y, "dikeyde şeride binmez: " + yer);
                    assertTrue(p.w() > 0 && p.h() > 0, "boş dikdörtgen yok: " + yer);
                }
            }
        }
    }

    @Test
    void efektlerZamanlaHareketEder() {
        assertNotEquals(HudCizimi.efekt(SicaklikDurumu.BUNALMA, 0f), HudCizimi.efekt(SicaklikDurumu.BUNALMA, 8f), "ter akar");
        assertNotEquals(HudCizimi.efekt(SicaklikDurumu.USUME, 0f), HudCizimi.efekt(SicaklikDurumu.USUME, 9f), "buhar yükselir");
        assertNotEquals(HudCizimi.efekt(SicaklikDurumu.YANMA, 0f), HudCizimi.efekt(SicaklikDurumu.YANMA, 3f), "alev dalgalanır");
    }

    @Test
    void titremeDurumaGore() {
        assertArrayEquals(new int[] {0, 0}, HudCizimi.titreme(SicaklikDurumu.NORMAL, 6, -1));
        assertArrayEquals(new int[] {1, 0}, HudCizimi.titreme(SicaklikDurumu.USUME, 0, -1), "ürperme");
        assertArrayEquals(new int[] {-1, 0}, HudCizimi.titreme(SicaklikDurumu.USUME, 15, -1));
        assertArrayEquals(new int[] {0, 0}, HudCizimi.titreme(SicaklikDurumu.USUME, 6, -1), "ürperme aralıklarla");
    }

    @Test
    void donmaOnceSarsilirSonraDonupKalir() {
        boolean sarsildi = false;
        for (long t = 100; t < 100 + HudCizimi.DONMA_SARSINTI_TICK; t++) {
            int[] d = HudCizimi.titreme(SicaklikDurumu.DONMA, t, 100);
            assertTrue(Math.abs(d[0]) <= 1 && Math.abs(d[1]) <= 1, "donma titremesi ±1 px");
            sarsildi |= d[0] != 0 || d[1] != 0;
        }
        assertTrue(sarsildi, "donma başlarken kafa sarsılır");
        assertArrayEquals(new int[] {0, 0}, HudCizimi.titreme(SicaklikDurumu.DONMA, 150, 100), "sonra donup kalır");
        assertArrayEquals(new int[] {0, 0}, HudCizimi.titreme(SicaklikDurumu.DONMA, 100, -1),
                "durum hiç değişmediyse sarsıntı bitmiştir");
        assertArrayEquals(new int[] {1, 1}, HudCizimi.titreme(SicaklikDurumu.DONMA, 160, 100), "80 tick'te bir ince titreme");
    }

    @Test
    void nabizVeParlamaDurumaGore() {
        assertEquals(0f, HudCizimi.nabiz(SicaklikDurumu.USUME, -30, SICAK, YANMA, 10f));
        assertTrue(HudCizimi.nabiz(SicaklikDurumu.YANMA, 60, SICAK, YANMA, 0f) > HudCizimi.nabiz(SicaklikDurumu.BUNALMA,
                30, SICAK, YANMA, 0f), "yanma nabızı bunalmadan güçlü");
        assertTrue(HudCizimi.nabizPeriyodu(68, SICAK, YANMA) < HudCizimi.nabizPeriyodu(22, SICAK, YANMA),
                "hasara yaklaştıkça nabız hızlanır");

        assertEquals(1f, HudCizimi.parlama(100, 100, 0f));
        assertTrue(HudCizimi.parlama(103, 100, 0f) > 0f && HudCizimi.parlama(103, 100, 0f) < 1f);
        assertEquals(0f, HudCizimi.parlama(100 + HudCizimi.PARLAMA_TICK, 100, 0f), "parlama 6 tick'te biter");
        assertEquals(0f, HudCizimi.parlama(50, -1, 0f), "durum hiç değişmediyse parlama yok");
    }
}
