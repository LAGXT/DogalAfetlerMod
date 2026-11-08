package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Koni strobu (PL-28, PL-29): 8 sn'de 87 vuruş, keskin tepe, 4,46 Hz şişme, %45 alt sınır, ayar 0 sabit, Düşük yarı vuruş. */
class PulsarStrobTest {
    private static final double ADIM = 2.5e-5;
    private static final float ESIK = 0.7f;

    /** {@code [bas, bas + sure)} aralığında parlaklığın {@value #ESIK} eşiğini yukarı kesme sayısı (vuruş tepesi). */
    private static int tepeSayisi(double bas, double sure, boolean dusuk) {
        int n = (int) Math.round(sure / ADIM);
        int sayi = 0;
        float onceki = PulsarStrob.koniParlakligi(bas - ADIM, 1f, dusuk);
        for (int i = 0; i < n; i++) {
            float k = PulsarStrob.koniParlakligi(bas + i * ADIM, 1f, dusuk);
            if (k >= ESIK && onceki < ESIK) {
                sayi++;
            }
            onceki = k;
        }
        return sayi;
    }

    @Test
    void sekizSaniyedeSeksenYediVurus() {
        assertEquals(10.875f, PulsarStrob.VURUS_HZ, 0f);
        assertEquals(PulsarStrob.DONGU_VURUS, PulsarStrob.VURUS_HZ * PulsarStrob.DONGU_SN, 0f, "döngü dikişsiz");
        assertEquals(87, tepeSayisi(0.0, 8.0, false));
        assertEquals(87, tepeSayisi(3.3171, 8.0, false), "herhangi bir 8 sn'lik pencere");
        assertEquals(4.46f, PulsarStrob.STROB_HZ, 0.05f, "ölçülen şişmeye yakın");
        float sismeDongude = PulsarStrob.STROB_HZ * PulsarStrob.DONGU_SN;
        assertEquals(Math.round(sismeDongude), sismeDongude, 0f, "şişme döngüde tam sayı: sarılınca sıçramaz");
        assertEquals(PulsarStrob.sisme(0.0), PulsarStrob.sisme(PulsarStrob.DONGU_SN), 1e-5f);
    }

    @Test
    void vurusKisaVeKeskin() {
        assertEquals(0f, PulsarStrob.tepe(0.0, false), 1e-6f);
        assertEquals(1f, PulsarStrob.tepe(PulsarStrob.YUKSELIS_SN - 1e-7, false), 1e-3f);
        assertTrue(PulsarStrob.tepe(PulsarStrob.YUKSELIS_SN + PulsarStrob.SONUM_SN, false) < 0.06f, "~30 ms'de söner");
        double yariBas = Double.NaN;
        double yariSon = Double.NaN;
        for (double t = 0; t < 1.0 / PulsarStrob.VURUS_HZ; t += 1e-5) {
            if (PulsarStrob.tepe(t, false) >= 0.5f) {
                if (Double.isNaN(yariBas)) {
                    yariBas = t;
                }
                yariSon = t;
            }
        }
        double yariGenislik = yariSon - yariBas;
        assertTrue(yariGenislik > 0.005 && yariGenislik < 0.02, "yarı yükseklik genişliği: " + yariGenislik);
    }

    @Test
    void ayarBirdeAltSinirYuzdeKirkBes() {
        float enAz = 1f;
        float enCok = 0f;
        for (int i = 0; i < 400_000; i++) {
            float k = PulsarStrob.koniParlakligi(17.0 + i * ADIM, 1f, false);
            enAz = Math.min(enAz, k);
            enCok = Math.max(enCok, k);
        }
        assertTrue(enAz >= PulsarStrob.EN_DUSUK - 1e-6f, "koni tam kapanmaz: " + enAz);
        assertTrue(enAz < PulsarStrob.EN_DUSUK + 0.01f, "vuruşlar arası alt sınıra iner: " + enAz);
        assertTrue(enCok > 0.97f && enCok <= 1f, "tepe ~%100: " + enCok);
        assertEquals(0.45f, PulsarStrob.EN_DUSUK, 0f);
    }

    @Test
    void ayarOlcekler() {
        float enAz = 1f;
        for (int i = 0; i < 80_000; i++) {
            enAz = Math.min(enAz, PulsarStrob.koniParlakligi(5.0 + i * ADIM, 0.5f, false));
        }
        assertEquals(1f - 0.5f * (1f - PulsarStrob.EN_DUSUK), enAz, 0.01f);
        assertEquals(PulsarStrob.koniParlakligi(2.0123, 1f, false), PulsarStrob.koniParlakligi(2.0123, 9f, false), 0f,
                "ayar kenetlenir");
    }

    @Test
    void ayarSifirdaSabit() {
        for (int i = 0; i < 40_000; i++) {
            double t = i * 3.7e-4;
            assertEquals(1f, PulsarStrob.koniParlakligi(t, 0f, false), 0f);
            assertEquals(1f, PulsarStrob.koniParlakligi(t, 0f, true), 0f);
            assertEquals(1f, PulsarStrob.koniParlakligi(t, -2f, false), 0f);
        }
        assertEquals(1f, PulsarStrob.koniParlakligi(Double.NaN, 1f, false), 0f, "faz bilinmiyor: sabit");
        assertEquals(1f, PulsarStrob.koniParlakligi(3.0, Float.NaN, false), 0f);
    }

    @Test
    void dusukteYariVurusAyniFaz() {
        assertEquals(87, tepeSayisi(0.0, 16.0, true), "16 sn'de 87: vuruş yarıya");
        double periyot = 1.0 / PulsarStrob.VURUS_HZ;
        for (int k = 0; k < 40; k++) {
            double tepeAni = k * periyot + PulsarStrob.YUKSELIS_SN * 0.999;
            float normal = PulsarStrob.tepe(tepeAni, false);
            float dusuk = PulsarStrob.tepe(tepeAni, true);
            assertTrue(normal > 0.99f, "normal kademede her vuruş");
            if (k % 2 == 0) {
                assertEquals(normal, dusuk, 1e-3f, "çift vuruş sesle aynı anda");
            } else {
                assertTrue(dusuk < 0.05f, "tek vuruş Düşük'te atlanır: " + dusuk);
            }
        }
    }

    @Test
    void yavasSismeDortBucukHz() {
        int kesme = 0;
        float onceki = PulsarStrob.sisme(0.0123);
        double sure = 100.0;
        for (int i = 1; i < 200_000; i++) {
            float s = PulsarStrob.sisme(0.0123 + i * sure / 200_000);
            assertTrue(s >= PulsarStrob.SISME_TABAN - 1e-6f && s <= 1f + 1e-6f);
            if ((s >= 0.8f) != (onceki >= 0.8f)) {
                kesme++;
            }
            onceki = s;
        }
        assertEquals(2 * 450, kesme, 2, "4,5 Hz: saniyede dokuz kesme");
        float enAzTepe = 1f;
        float enCokTepe = 0f;
        double periyot = 1.0 / PulsarStrob.VURUS_HZ;
        for (int k = 0; k < 87; k++) {
            float p = PulsarStrob.koniParlakligi(k * periyot + PulsarStrob.YUKSELIS_SN * 0.999, 1f, false);
            enAzTepe = Math.min(enAzTepe, p);
            enCokTepe = Math.max(enCokTepe, p);
        }
        assertTrue(enCokTepe - enAzTepe > 0.15f, "tepe yükseklikleri şişmeyle değişir: " + enAzTepe + "–" + enCokTepe);
    }
}
