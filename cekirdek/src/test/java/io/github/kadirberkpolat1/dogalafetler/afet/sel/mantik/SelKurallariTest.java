package io.github.kadirberkpolat1.dogalafetler.afet.sel.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.su.SelSuyu;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Selin seviye tablosu, sel suyu parametreleri ve ısı kaynağı buharlaşma yarıçapı (K-E80). */
class SelKurallariTest {
    @Test
    void seviyelerYuksekligiVeSureyiArtirirSistemSiniriniAsmaz() {
        double onceki = 0;
        int oncekiToplam = 0;
        for (int s = 1; s <= 5; s++) {
            SelKurallari.Seviye v = SelKurallari.Tablo.VARSAYILAN.seviye(s);
            assertEquals(s, v.seviye());
            assertTrue(v.artisBlok() > onceki && v.artisBlok() <= SelKurallari.EN_COK_ARTIS, "S" + s + " " + v);
            assertTrue(v.toplamTick() > oncekiToplam, "S" + s + " süresi artar");
            assertEquals(v.yukselmeTick() + v.zirveTick() + v.inmeTick(), v.toplamTick());
            onceki = v.artisBlok();
            oncekiToplam = v.toplamTick();
        }
    }

    @Test
    void seviyeSinirlaraKirpilirVeSuyuTablodanKurulur() {
        assertEquals(1, SelKurallari.Tablo.VARSAYILAN.seviye(-4).seviye());
        assertEquals(5, SelKurallari.Tablo.VARSAYILAN.seviye(99).seviye());
        SelKurallari.Tablo asiri = new SelKurallari.Tablo(List.of(0.0, 99.0, 1.0, 1.0, 1.0), List.of(0, 1, 1, 1, 1),
                List.of(-5, 0, 0, 0, 0), List.of(0, 1, 1, 1, 1));
        assertEquals(0.5, asiri.seviye(1).artisBlok(), "en az yarım blok");
        assertEquals(SelKurallari.EN_COK_ARTIS, asiri.seviye(2).artisBlok(), "sistem sınırı");
        assertEquals(0, asiri.seviye(1).zirveTick(), "zirve negatif olamaz");
        assertEquals(SelKurallari.SANIYE_TICK, asiri.seviye(1).yukselmeTick(), "yükselme en az 1 sn");

        SelKurallari.Seviye v = SelKurallari.Tablo.VARSAYILAN.seviye(3);
        SelSuyu su = v.suyu(1234);
        assertEquals(1234, su.baslangic());
        assertEquals(v.artisBlok(), su.hedef());
        assertEquals(v.toplamTick(), su.toplamSure(), 1e-9);
    }

    @Test
    void buharlasmaYaricapiYalnizYeterliIsitanKaynaklardan() {
        assertEquals(6.0, SelKurallari.buharlasmaYaricapi(40, 6, 3, 1.0), "lav");
        assertEquals(2.0, SelKurallari.buharlasmaYaricapi(4, 2, 3, 1.0), "meşale");
        assertEquals(0.0, SelKurallari.buharlasmaYaricapi(2, 2, 3, 1.0), "zayıf kaynak");
        assertEquals(0.0, SelKurallari.buharlasmaYaricapi(-10, 3, 3, 1.0), "soğutucu buharlaştırmaz");
        assertEquals(0.0, SelKurallari.buharlasmaYaricapi(40, 0, 3, 1.0), "yarıçapı olmayan");
        assertEquals(9.0, SelKurallari.buharlasmaYaricapi(40, 6, 3, 1.5), 1e-9, "yarıçap çarpanı");
        assertEquals(12.0, SelKurallari.buharlasmaYaricapi(40, 8, 3, 2.0), "en fazla 12 blok");
    }

    /** K-E81: S1 5 blok / 20 sn, S5 12 blok / 40 sn, aradakiler ikisi arasında doğrusal; eski tablodan (2–7,5 blok, 45–105 sn) hızlı ve yüksek. */
    @Test
    void varsayilanTabloS1veS5UclarindaVeAradakilerDogrusal() {
        SelKurallari.Seviye s1 = SelKurallari.Tablo.VARSAYILAN.seviye(1);
        SelKurallari.Seviye s5 = SelKurallari.Tablo.VARSAYILAN.seviye(5);
        assertEquals(5.0, s1.artisBlok(), 1e-9);
        assertEquals(12.0, s5.artisBlok(), 1e-9);
        assertEquals(20 * SelKurallari.SANIYE_TICK, s1.yukselmeTick());
        assertEquals(40 * SelKurallari.SANIYE_TICK, s5.yukselmeTick());
        for (int s = 2; s <= 4; s++) {
            double u = (s - 1) / 4.0;
            SelKurallari.Seviye v = SelKurallari.Tablo.VARSAYILAN.seviye(s);
            assertEquals(s1.artisBlok() + u * (s5.artisBlok() - s1.artisBlok()), v.artisBlok(), 1e-9, "S" + s + " yükselme");
            assertEquals(s1.yukselmeTick() + u * (s5.yukselmeTick() - s1.yukselmeTick()), v.yukselmeTick(), 1e-9,
                    "S" + s + " yükselme süresi");
        }
        for (int s = 1; s <= 5; s++) {
            SelKurallari.Seviye v = SelKurallari.Tablo.VARSAYILAN.seviye(s);
            double eskiHiz = new double[] {2.0, 3.0, 4.0, 5.5, 7.5}[s - 1] / new int[] {45, 60, 75, 90, 105}[s - 1];
            assertTrue(v.artisBlok() / (v.yukselmeTick() / (double) SelKurallari.SANIYE_TICK) > 3 * eskiHiz,
                    "S" + s + " eskisinden en az 3 kat hızlı yükselir");
        }
    }

    @Test
    void bogulmaTuketimiOrtalamaCarpanKadarVeHerTickTamsayi() {
        for (double carpan : new double[] {2.0, 2.5, 3.0}) {
            long toplam = 0;
            for (long t = 0; t < 1000; t++) {
                int n = SelKurallari.bogulmaTuketimi(1, t, carpan);
                assertTrue(n >= Math.floor(carpan) && n <= Math.ceil(carpan), "tick " + t + " tüketim " + n + " (çarpan " + carpan + ")");
                toplam += n;
            }
            assertEquals(1000 * carpan, toplam, 1.0, "1000 tick'te " + carpan + " kat hava");
        }
    }

    @Test
    void bogulmaTuketimiTabanSifirsaSifirCarpanBirseTaban() {
        for (long t = 0; t < 50; t++) {
            assertEquals(0, SelKurallari.bogulmaTuketimi(0, t, 2.5), "Su Altı Solunumu atlatan tick'te tüketim yok");
            assertEquals(1, SelKurallari.bogulmaTuketimi(1, t, 1.0), "çarpan 1 = vanilla");
            assertEquals(1, SelKurallari.bogulmaTuketimi(1, t, 0.3), "çarpan 1'in altı vanilla'dan yavaş yapmaz");
        }
    }
}
