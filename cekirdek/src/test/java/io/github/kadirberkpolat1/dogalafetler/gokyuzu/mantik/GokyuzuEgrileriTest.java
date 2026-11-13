package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import java.util.Random;
import org.junit.jupiter.api.Test;

class GokyuzuEgrileriTest {
    private static final float EPS = 1e-6f;

    /** Ps4: eski güneşe dönüşün 20 sn'lik geçişinde kalıntı nebulası belirmez; süpernova yolunda kalıntı sürer. */
    @Test
    void eskiGunesDonusundeKalintiNebulasiBelirmez() {
        boolean yol = GokyuzuEgrileri.oncekiYol(false, true);
        assertTrue(yol, "NORMAL'de dönüş yolu bayrağı kalkar, güneş döndü bayrağı önceki yolu bildirir");
        assertFalse(GokyuzuEgrileri.oncekiYol(false, false));
        for (int i = 0; i <= 100; i++) {
            float gecis = i / 100f;
            assertEquals(0f, GokyuzuEgrileri.kalintiGecisi(GunesEvresi.NORMAL, 0f, gecis * 20f, false,
                    GunesEvresi.KIZIL_CUCE, yol, gecis), 0f, "dönüş geçişi " + gecis);
            assertEquals(0f, GokyuzuEgrileri.kalintiGecisi(GunesEvresi.KIZIL_CUCE, gecis, gecis * 20f, true,
                    GunesEvresi.GUNESSIZ, true, gecis), 0f, "pulsar yolundaki doğuş " + gecis);
        }
        assertEquals(0.85f, GokyuzuEgrileri.kalintiGecisi(GunesEvresi.NORMAL, 0f, 0f, false, GunesEvresi.KIZIL_CUCE,
                false, 0f), EPS, "bayrak olmasaydı geçişin başında kalıntı görünürdü");
        assertEquals(0.85f, GokyuzuEgrileri.kalintiPayi(GunesEvresi.KIZIL_CUCE, 1f, 1e6f, false), EPS,
                "süpernova yolundaki kızıl cücede kalıntı sürer");
        assertEquals(0f, GokyuzuEgrileri.kalintiGecisi(GunesEvresi.NORMAL, 0f, 1e6f, false, GunesEvresi.KIZIL_CUCE,
                false, 1f), 0f, "geçiş bitince NORMAL'de kalıntı yok");
    }

    @Test
    void buyuyenGunesMonotonBuyurVeTuruncuyaKayar() {
        assertEquals(1f, GokyuzuEgrileri.buyuyenOlcek(0f), EPS, "vanilla boyda başlar");
        assertEquals(3.2f, GokyuzuEgrileri.buyuyenOlcek(1f), EPS, "tasarım: 3,2x");
        float onceki = 0f;
        float oncekiMavi = 2f;
        for (int i = 0; i <= 100; i++) {
            float p = i / 100f;
            float olcek = GokyuzuEgrileri.buyuyenOlcek(p);
            float mavi = GokyuzuEgrileri.buyuyenRenk(p)[2];
            assertTrue(olcek >= onceki, "ölçek azalmamalı");
            assertTrue(mavi <= oncekiMavi, "renk turuncuya doğru gitmeli");
            onceki = olcek;
            oncekiMavi = mavi;
        }
        assertArrayEquals(new float[] {1f, 0x9A / 255f, 0x3C / 255f}, GokyuzuEgrileri.buyuyenRenk(1f), 0.01f);
    }

    @Test
    void supernovaKoreografisiMonoton() {
        assertTrue(GokyuzuEgrileri.supernovaCekirdekOlcek(0f) > 3f, "çökme şişmiş güneşten başlar");
        assertTrue(GokyuzuEgrileri.supernovaCekirdekOlcek(GokyuzuEgrileri.SN_COKME_SN) < 0.5f, "çökme sonunda nokta");
        assertEquals(0f, GokyuzuEgrileri.supernovaFlas(GokyuzuEgrileri.SN_COKME_SN - 0.1f), EPS, "parlama çökmeden sonra");
        float tepe = 0f;
        for (float s = 0f; s < 60f; s += 0.05f) {
            tepe = Math.max(tepe, GokyuzuEgrileri.supernovaFlas(s));
        }
        assertTrue(tepe > 0.9f, "parlama tepesi");
        float oncekiOlcek = Float.MAX_VALUE;
        float oncekiHalka = 0f;
        for (float s = 0f; s < 120f; s += 0.25f) {
            float olcek = GokyuzuEgrileri.supernovaCekirdekOlcek(s);
            float halka = GokyuzuEgrileri.supernovaHalka(s, 0.5f);
            assertTrue(olcek <= oncekiOlcek + EPS, "çekirdek çökerken büyümemeli");
            assertTrue(halka >= oncekiHalka - EPS, "halka geri çekilmemeli");
            oncekiOlcek = olcek;
            oncekiHalka = halka;
        }
        float oncekiParlak = 2f;
        float oncekiHalkaP = 0f;
        for (int i = 0; i <= 100; i++) {
            float p = i / 100f;
            assertTrue(GokyuzuEgrileri.supernovaParlaklik(p) <= oncekiParlak, "süpernova sönerken parlamamalı");
            assertTrue(GokyuzuEgrileri.supernovaHalka(300f, p) >= oncekiHalkaP, "halka ilerlemeyle genişler");
            oncekiParlak = GokyuzuEgrileri.supernovaParlaklik(p);
            oncekiHalkaP = GokyuzuEgrileri.supernovaHalka(300f, p);
        }
    }

    @Test
    void kizilCuceDogusuVeKalintiMonoton() {
        assertEquals(0f, GokyuzuEgrileri.kizilCuceDiskAlfa(0f), EPS, "önce yalnız ufuk ışığı");
        assertEquals(1f, GokyuzuEgrileri.kizilCuceDiskAlfa(1f), EPS);
        float oncekiDisk = -1f;
        float oncekiUfuk = 99f;
        float oncekiKalinti = 0f;
        for (int i = 0; i <= 100; i++) {
            float p = i / 100f;
            assertTrue(GokyuzuEgrileri.kizilCuceDiskAlfa(p) >= oncekiDisk);
            assertTrue(GokyuzuEgrileri.kizilCuceUfukGucu(p) <= oncekiUfuk);
            assertTrue(GokyuzuEgrileri.kalintiGenisleme(p) >= oncekiKalinti);
            oncekiDisk = GokyuzuEgrileri.kizilCuceDiskAlfa(p);
            oncekiUfuk = GokyuzuEgrileri.kizilCuceUfukGucu(p);
            oncekiKalinti = GokyuzuEgrileri.kalintiGenisleme(p);
        }
        assertEquals(1.3f, GokyuzuEgrileri.kalintiGenisleme(1f), EPS, "tavan %130");
    }

    @Test
    void evreGecisi20SaniyedeDogrusal() {
        assertEquals(0f, GokyuzuEgrileri.gecis(0), EPS);
        assertEquals(0.5f, GokyuzuEgrileri.gecis(200), EPS);
        assertEquals(1f, GokyuzuEgrileri.gecis(400), EPS);
        assertEquals(1f, GokyuzuEgrileri.gecis(1_000_000), EPS);
        assertEquals(0f, GokyuzuEgrileri.gecis(-30), EPS, "saat geri adımında da 0'ın altına inmez");
        assertArrayEquals(new float[] {0.5f, 0.25f, 0f}, GokyuzuEgrileri.karistir(0xFF0000, 0x008000, 0.5f), 0.01f);
    }

    @Test
    void monotonIlerlemeGeriGitmezAnahtarDegisinceSifirlanir() {
        MonotonIlerleme m = new MonotonIlerleme();
        Random r = new Random(5);
        float onceki = m.guncelle(1L, 0.2f, 1f);
        assertEquals(0.2f, onceki, EPS, "ilk değer doğrudan alınır (sonradan giren)");
        for (int i = 0; i < 400; i++) {
            float hedef = 0.2f + i * 0.002f + (r.nextFloat() - 0.5f) * 0.05f;
            float simdi = m.guncelle(1L, hedef, 1f);
            assertTrue(simdi >= onceki - EPS, "gösterilen ilerleme geri gitmemeli");
            onceki = simdi;
        }
        assertEquals(0.05f, m.guncelle(2L, 0.05f, 1f), EPS, "yeni evre: sıfırdan");
        float yakin = 0.05f;
        for (int i = 0; i < 200; i++) {
            yakin = m.guncelle(2L, 0.9f, 1f);
        }
        assertEquals(0.9f, yakin, 0.001f, "hedefe yetişir");
    }
}
