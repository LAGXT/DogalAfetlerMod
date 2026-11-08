package io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Kasırganın blok yıkım kuralları (K-04, K-05): seviye × uzaklık, sütun derinliği, blok sınıfları, bütçe, devrilme yönü. */
class KasirgaYikimiTest {
    private static final double E = 1e-9;

    @Test
    void cekimOlasiligiCekirdekteGucluKenardaZayifSeviyeyleArtar() {
        for (int l = 1; l <= 5; l++) {
            KasirgaSeviyesi s = KasirgaSeviyesi.icin(l);
            double yikim = KasirgaYikimi.yikimYaricapi(s.cekimYaricapi());
            double cekirdek = s.tabanYaricapi();
            double merkez = KasirgaYikimi.cekimOlasiligi(l, 0, cekirdek, yikim);
            double kenar = KasirgaYikimi.cekimOlasiligi(l, yikim, cekirdek, yikim);
            assertEquals(merkez, KasirgaYikimi.cekimOlasiligi(l, cekirdek, cekirdek, yikim), E, "çekirdek içinde sabit S" + l);
            assertTrue(kenar > 0 && kenar < merkez, "kenarda zayıf ama sıfır değil S" + l);
            assertEquals(0, KasirgaYikimi.cekimOlasiligi(l, yikim + 0.01, cekirdek, yikim), E, "yıkım yarıçapı dışı S" + l);
            double onceki = merkez;
            for (int i = 1; i <= 20; i++) {
                double p = KasirgaYikimi.cekimOlasiligi(l, yikim * i / 20, cekirdek, yikim);
                assertTrue(p <= onceki + E && p >= 0 && p <= 1, "merkezden uzaklaşınca azalır S" + l);
                onceki = p;
            }
        }
        for (int l = 2; l <= 5; l++) {
            for (double d = 0; d <= 1; d += 0.1) {
                double alt = KasirgaYikimi.cekimOlasiligi(l - 1, d * 10, 3, 10);
                double ust = KasirgaYikimi.cekimOlasiligi(l, d * 10, 3, 10);
                assertTrue(ust >= alt - E, "aynı uzaklıkta seviye arttıkça güçlenir S" + l);
            }
        }
        assertTrue(KasirgaYikimi.cekimOlasiligi(1, 0, 3, 5.4) < 1, "S1 çekirdekte bile hafif");
        assertEquals(1, KasirgaYikimi.cekimOlasiligi(4, 0, 12, 21.6), E);
        assertEquals(1, KasirgaYikimi.cekimOlasiligi(5, 0, 17, 30.6), E);
        assertEquals(30.6, KasirgaYikimi.yikimYaricapi(51), 1e-9);
        assertThrows(IllegalArgumentException.class, () -> KasirgaYikimi.cekimOlasiligi(6, 0, 1, 2));
    }

    @Test
    void sutunDerinligiSeviyeyleArtar() {
        assertEquals(1, KasirgaYikimi.derinlik(1));
        assertEquals(1, KasirgaYikimi.derinlik(2));
        assertEquals(3, KasirgaYikimi.derinlik(3));
        assertEquals(4, KasirgaYikimi.derinlik(4));
        assertEquals(6, KasirgaYikimi.derinlik(5));
        for (int l = 2; l <= 5; l++) {
            assertTrue(KasirgaYikimi.derinlik(l) >= KasirgaYikimi.derinlik(l - 1));
        }
        assertThrows(IllegalArgumentException.class, () -> KasirgaYikimi.derinlik(0));
    }

    @Test
    void seviyelerinAlabildigiBlokSiniflari() {
        for (KasirgaYikimi.Sinif sinif : KasirgaYikimi.Sinif.values()) {
            for (int l = 1; l <= 5; l++) {
                assertTrue(KasirgaYikimi.alinir(l, sinif, 0), "en üst blokta yalnız sertlik/zemin kuralı: " + sinif + " S" + l);
                assertFalse(KasirgaYikimi.alinir(l, sinif, -1));
                assertFalse(KasirgaYikimi.alinir(l, sinif, KasirgaYikimi.derinlik(l)), "derinlik dışı " + sinif + " S" + l);
            }
            assertFalse(KasirgaYikimi.alinir(1, sinif, 1), "S1 en üstün altına inmez: " + sinif);
            assertFalse(KasirgaYikimi.alinir(2, sinif, 1), "S2 en üstün altına inmez: " + sinif);
        }
        assertTrue(KasirgaYikimi.alinir(3, KasirgaYikimi.Sinif.YAPRAK, 2), "S3 iç yaprak katmanlarını alır");
        assertTrue(KasirgaYikimi.alinir(3, KasirgaYikimi.Sinif.BITKI, 1));
        assertFalse(KasirgaYikimi.alinir(3, KasirgaYikimi.Sinif.KUTUK, 1), "S3 gövdeye inmez");
        assertTrue(KasirgaYikimi.alinir(4, KasirgaYikimi.Sinif.KUTUK, 1), "S4 yaprağın altındaki gövdeyi alır");
        assertFalse(KasirgaYikimi.alinir(4, KasirgaYikimi.Sinif.AHSAP_YAPI, 1), "S4 yapının içine inmez");
        assertTrue(KasirgaYikimi.alinir(5, KasirgaYikimi.Sinif.AHSAP_YAPI, 5), "S5 ahşap yapıyı sıyrır");
        for (int l = 1; l <= 5; l++) {
            for (int d = 1; d < KasirgaYikimi.derinlik(l); d++) {
                assertFalse(KasirgaYikimi.alinir(l, KasirgaYikimi.Sinif.ZEMIN, d), "zemin yalnız en üstte S" + l);
                assertFalse(KasirgaYikimi.alinir(l, KasirgaYikimi.Sinif.DIGER, d), "diğer yalnız en üstte S" + l);
            }
        }
        assertFalse(KasirgaYikimi.devirir(1), "S1 ağaç devirmez");
        assertFalse(KasirgaYikimi.devirir(2));
        assertFalse(KasirgaYikimi.devirir(3));
        assertTrue(KasirgaYikimi.devirir(4));
        assertTrue(KasirgaYikimi.devirir(5));
    }

    @Test
    void parcaVeEsyaButcesiEskiEsyaHiziniAsmaz() {
        double[] paylar = {0.1, 0.25, 0.5, 1};
        double[] kismalar = {0.25, 0.5, 1};
        double[] carpanlar = {0.25, 1, 2, 4};
        for (int l = 1; l <= 5; l++) {
            int tablo = KasirgaSeviyesi.icin(l).blokKaldirmaTick();
            for (double carpan : carpanlar) {
                int blok = (int) Math.round(tablo * carpan);
                for (double pay : paylar) {
                    for (double kisma : kismalar) {
                        KasirgaYikimi.Butce b = KasirgaYikimi.butce(l, blok, pay, kisma);
                        int taban = Math.max(1, (int) Math.round(blok * pay * kisma));
                        double eskiEsya = KasirgaYikimi.eskiParcaTavani(taban) * KasirgaYikimi.ESKI_ESYA_PAYI;
                        String ad = "S" + l + " blok " + blok + " pay " + pay + " kısma " + kisma;
                        assertTrue(b.parca() * b.esyaPayi() <= eskiEsya + E, "tick başına eşya K-04'ü aşmaz: " + ad);
                        assertEquals(KasirgaYikimi.eskiParcaTavani(taban), b.parca(), "parça tavanı K-04 ile aynı: " + ad);
                        assertTrue(b.deneme() >= taban && b.parca() >= 1, ad);
                        assertTrue(b.esyaPayi() > 0 && b.esyaPayi() <= KasirgaYikimi.ESKI_ESYA_PAYI, ad);
                        assertEquals(l >= 4, b.kutuk() > 0, "kütük bütçesi yalnız S4-5: " + ad);
                    }
                }
            }
        }
        KasirgaYikimi.Butce s1 = KasirgaYikimi.butce(1, KasirgaSeviyesi.icin(1).blokKaldirmaTick(), 1, 1);
        KasirgaYikimi.Butce s5 = KasirgaYikimi.butce(5, KasirgaSeviyesi.icin(5).blokKaldirmaTick(), 1, 1);
        assertEquals(new KasirgaYikimi.Butce(2, 1, 0, 1.0 / 16), s1, "S1 K-04 ile aynı");
        assertEquals(80, s5.deneme(), "S5 32 blok/tick × 2,5");
        assertEquals(8, s5.parca(), "S5 parça tavanı K-04 ile aynı (¼)");
        assertEquals(12, s5.kutuk());
        assertEquals(1.0 / 16, s5.esyaPayi(), E);
        assertSame(KasirgaYikimi.Butce.YOK, KasirgaYikimi.butce(3, 8, 0, 1), "evre payı 0: yıkım yok");
        assertSame(KasirgaYikimi.Butce.YOK, KasirgaYikimi.butce(3, 0, 1, 1), "blokCarpani 0: yıkım yok");
        assertEquals(2, KasirgaYikimi.butce(4, 14, 1, 0.25).kutuk(), "kütük tavanı TPS kısmasıyla iner");
    }

    @Test
    void devrilmeYonuSiklonikTegetVeEksenli() {
        // Doğudaki sütun kuzeye (-z) devrilir: yukarıdan saat yönünün tersi (Cekim ve UcanBlokVarligi ile aynı yön).
        assertArrayEquals(new int[] {0, -1}, KasirgaYikimi.devrilmeYonu(4, 0));
        assertArrayEquals(new int[] {-1, 0}, KasirgaYikimi.devrilmeYonu(0, -4));
        assertArrayEquals(new int[] {0, 1}, KasirgaYikimi.devrilmeYonu(-4, 0));
        assertArrayEquals(new int[] {1, 0}, KasirgaYikimi.devrilmeYonu(0, 4));
        assertArrayEquals(new int[] {1, 0}, KasirgaYikimi.devrilmeYonu(0, 0));
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8 + 0.1;
            int[] y = KasirgaYikimi.devrilmeYonu(Math.cos(a) * 5, Math.sin(a) * 5);
            assertEquals(1, Math.abs(y[0]) + Math.abs(y[1]), "tek eksende birim adım");
            // Teğet: konum vektörüne dik yöne yakın (iç çarpımın büyüklüğü yarıçapın en çok √½'i).
            assertTrue(Math.abs(y[0] * Math.cos(a) + y[1] * Math.sin(a)) <= Math.sqrt(0.5) + E);
        }
    }
}
