package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pulsar titreşimi (PL-2, PL-28): frekans, genlik sınırları, video çırpınması, erişilebilirlik ayarı, Düşük, çatırtı. */
class PulsarTitresimiTest {
    private static final double ADIM = 1e-4;

    /** {@code sure} saniyede hızlı bileşenin yatay ekseninin işaret değiştirme sayısı. */
    private static int isaretDegisimi(double bas, double sure, float guc, float ayar, boolean dusuk) {
        float[] o = new float[2];
        int sayi = 0;
        float onceki = Float.NaN;
        for (double t = bas; t < bas + sure; t += ADIM) {
            PulsarTitresimi.hizliOfset(t, guc, ayar, dusuk, 1f, o);
            if (!Float.isNaN(onceki) && Math.signum(o[0]) != Math.signum(onceki)) {
                sayi++;
            }
            onceki = o[0];
        }
        return sayi;
    }

    @Test
    void hizliBilesenOnAltiHzDusukteSekizHz() {
        assertEquals(2 * 16 * 3, isaretDegisimi(100.013, 3.0, 1f, 1f, false), 2, "16 Hz kare: saniyede 32 işaret değişimi");
        assertEquals(2 * 8 * 3, isaretDegisimi(100.013, 3.0, 1f, 1f, true), 2, "Düşük: 8 Hz");
        assertEquals(16f, PulsarTitresimi.frekans(false));
        assertEquals(8f, PulsarTitresimi.frekans(true));
    }

    @Test
    void hizliGenlikSinirlariGucVeAyarla() {
        float tepe = PulsarTitresimi.HIZLI_TEPE_DERECE;
        assertEquals(0.6f * tepe, PulsarTitresimi.hizliGenlik(0f, 1f), 1e-6f);
        assertEquals(tepe, PulsarTitresimi.hizliGenlik(1f, 1f), 1e-6f);
        assertEquals(0.5f * tepe, PulsarTitresimi.hizliGenlik(1f, 0.5f), 1e-6f);
        assertEquals(tepe, PulsarTitresimi.hizliGenlik(5f, 3f), 1e-6f, "aralık dışı kenetlenir");
        float[] o = new float[2];
        for (float guc : new float[] {0f, 0.5f, 1f}) {
            float a = PulsarTitresimi.hizliGenlik(guc, 1f);
            float enCok = 0f;
            for (double t = 7.0; t < 9.0; t += 0.0007) {
                PulsarTitresimi.hizliOfset(t, guc, 1f, false, 1f, o);
                assertTrue(Math.abs(o[0]) <= a + 1e-5f && Math.abs(o[0]) >= PulsarTitresimi.EN_AZ_ADIM * a - 1e-5f,
                        "kare dalga yarım periyot genliği EN_AZ_ADIM·A–A: " + o[0]);
                assertTrue(Math.abs(o[1]) <= PulsarTitresimi.TESTERE_ORANI * a + 1e-5f, "testere ekseni sınırlı");
                enCok = Math.max(enCok, Math.abs(o[0]));
            }
            assertTrue(enCok > 0.95f * a, "tepe genliğe ulaşılır: " + enCok + " / " + a);
        }
    }

    /** Videoda ardışık karelerde en parlak nokta kadrajın ±3/90–4/90'ı kadar yana sıçrar (29,97 kare/sn). */
    @Test
    void videoCirpinmasiArdisikKarelerdeUcDortPiksel() {
        assertEquals(4f / 90f, PulsarTitresimi.cirpinmaOrani(1f, 1f), 1e-6f);
        assertEquals(PulsarTitresimi.CIRPINMA_EN_AZ, PulsarTitresimi.EN_AZ_ADIM * PulsarTitresimi.CIRPINMA_EN_COK, 1e-6f);
        float[] o = new float[2];
        double kare = 1.0 / 29.97;
        int adet = 1800;
        int karsiTaraf = 0;
        float onceki = Float.NaN;
        for (int i = 0; i < adet; i++) {
            PulsarTitresimi.hizliOfset(100.0123 + i * kare, 1f, 1f, false, 1f, o);
            float oran = Math.abs(o[0]) / PulsarTitresimi.KADRAJ_DERECE;
            assertTrue(oran >= 3f / 90f - 1e-5f && oran <= 4f / 90f + 1e-5f,
                    "kare " + i + " sıçrama " + oran * 90f + " px");
            if (!Float.isNaN(onceki) && Math.signum(o[0]) != Math.signum(onceki)) {
                karsiTaraf++;
            }
            onceki = o[0];
        }
        assertTrue(karsiTaraf > 0.85 * (adet - 1), "ardışık karelerin çoğu karşı tarafa sıçrar: " + karsiTaraf);
        karsiTaraf = 0;
        onceki = Float.NaN;
        for (int i = 0; i < adet; i++) {
            PulsarTitresimi.hizliOfset(100.0123 + i * kare, 1f, 1f, true, 1f, o);
            if (!Float.isNaN(onceki) && Math.signum(o[0]) != Math.signum(onceki)) {
                karsiTaraf++;
            }
            onceki = o[0];
        }
        assertTrue(karsiTaraf > 0.4 * (adet - 1) && karsiTaraf < 0.65 * (adet - 1),
                "Düşük (8 Hz): kabaca her iki karede bir sıçrama: " + karsiTaraf);
    }

    @Test
    void ayarSifirdaHizliBilesenYokYavasSarsintiKalir() {
        float[] h = new float[2];
        float[] y = new float[2];
        float enCokYavas = 0f;
        for (double t = 0; t < 4; t += 0.003) {
            PulsarTitresimi.hizliOfset(t, 1f, 0f, false, 2.5f, h);
            assertEquals(0f, h[0], 0f);
            assertEquals(0f, h[1], 0f);
            PulsarTitresimi.yavasOfset(t, 1f, 1f, y);
            assertTrue(Math.abs(y[0]) <= PulsarTitresimi.yavasGenlik(1f) + 1e-5f
                    && Math.abs(y[1]) <= PulsarTitresimi.yavasGenlik(1f) + 1e-5f);
            enCokYavas = Math.max(enCokYavas, Math.max(Math.abs(y[0]), Math.abs(y[1])));
        }
        assertTrue(enCokYavas > 1.5f, "yavaş sarsıntı büyük genlikli: " + enCokYavas);
        assertEquals(1.5f, PulsarTitresimi.yavasGenlik(0f), 1e-6f);
        assertEquals(2.5f, PulsarTitresimi.yavasGenlik(1f), 1e-6f);
    }

    @Test
    void yavasSarsintiBirIkiHz() {
        float[] y = new float[2];
        int sayi = 0;
        float onceki = Float.NaN;
        double sure = 60.0;
        for (double t = 0.0123; t < sure; t += 0.001) {
            PulsarTitresimi.yavasOfset(t, 1f, 1f, y);
            if (!Float.isNaN(onceki) && Math.signum(y[0]) != Math.signum(onceki)) {
                sayi++;
            }
            onceki = y[0];
        }
        double hz = sayi / 2.0 / sure;
        assertTrue(hz >= 1.0 && hz <= 2.0, "yavaş bileşen 1–2 Hz: " + hz);
    }

    @Test
    void kareSuresindenBagimsiz() {
        float[] a = new float[2];
        float[] b = new float[2];
        double t = 1234.5678;
        PulsarTitresimi.hizliOfset(t, 0.7f, 1f, false, 1f, a);
        for (int i = 0; i < 50; i++) {
            PulsarTitresimi.hizliOfset(t + i * 0.016, 0.7f, 1f, false, 1f, b);
        }
        PulsarTitresimi.hizliOfset(t, 0.7f, 1f, false, 1f, b);
        assertEquals(a[0], b[0], 0f, "aynı an aynı ofset: önceki karelere bağlı durum yok");
        assertEquals(a[1], b[1], 0f);
    }

    @Test
    void catirtiSaniyedeIkiKisaParlama() {
        double sure = 2000.0;
        double adim = 0.0005;
        int parlama = 0;
        double acik = 0;
        double enKisa = Double.MAX_VALUE;
        double enUzun = 0;
        double bas = -1;
        boolean onceki = false;
        for (double t = 0; t < sure; t += adim) {
            float c = PulsarTitresimi.catirti(t);
            assertTrue(c >= 0f && c <= 1f);
            boolean simdi = c > 0f;
            if (simdi && !onceki) {
                parlama++;
                bas = t;
            } else if (!simdi && onceki) {
                double uzunluk = t - bas;
                enKisa = Math.min(enKisa, uzunluk);
                enUzun = Math.max(enUzun, uzunluk);
            }
            if (simdi) {
                acik += adim;
            }
            onceki = simdi;
        }
        double oran = parlama / sure;
        assertTrue(Math.abs(oran - PulsarTitresimi.CATIRTI_HZ) < 0.1, "çatırtı ~2/sn: " + oran);
        assertTrue(enKisa >= PulsarTitresimi.CATIRTI_EN_KISA_SN - 2 * adim, "en kısa: " + enKisa);
        assertTrue(enUzun <= PulsarTitresimi.CATIRTI_EN_UZUN_SN + 2 * adim, "en uzun: " + enUzun);
        assertTrue(acik / sure > 0.2 && acik / sure < 0.3, "zamanın ~%25'i parlamada: " + acik / sure);
    }
}
