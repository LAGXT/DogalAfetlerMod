package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** K-E84: gezegenler göğün tek bir küçük kapağına sıkışmaz, birbirine ve ufka göre uygun yerde durur; boy uzaklığa göre değişir. */
class GezegenYerlesimiTest {
    /** Her gezegenin en büyük görünür yarıçapı (derece; Satürn halka dahil, Venüs en yakın yaklaşmada). */
    private static final float[] EN_BUYUK_YARICAP = {2.2f, 3.8f, 3.2f, 4.0f, 5.8f, 1.4f, 1.0f, 0.9f};

    private static float[] yon(int gezegen, float yanal, float boylamsal) {
        float[] v = new float[3];
        GezegenYerlesimi.yon(gezegen, yanal, boylamsal, v, 0);
        return v;
    }

    private static double aci(float[] a, float[] b) {
        double c = a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
        return Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, c))));
    }

    @Test
    void yonlerBirimVektordurVeYuvadaKurulur() {
        for (int i = 0; i < GezegenYerlesimi.SAYI; i++) {
            float[] v = yon(i, 0f, 0f);
            assertEquals(1.0, Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]), 1e-5, "birim, gezegen " + i);
            assertEquals(-Math.cos(Math.toRadians(GezegenYerlesimi.yuvaKutup(i))), v[1], 1e-5, "güneş karşıtı nokta -Y");
            double azimut = Math.toDegrees(Math.atan2(v[2], v[0]));
            double fark = Math.abs(((azimut - GezegenYerlesimi.yuvaAzimut(i)) % 360 + 540) % 360 - 180);
            assertEquals(0.0, fark, 1e-3, "azimut, gezegen " + i);
        }
    }

    /** Eskiden hepsi 40°'lik kapakta toplanıyordu; şimdi yuvalar göğün çok daha geniş bir alanına yayılır. */
    @Test
    void gezegenlerGecenin40DereceKapaginaSikismaz() {
        double enUzak = 0;
        double enYakin = 180;
        for (int i = 0; i < GezegenYerlesimi.SAYI; i++) {
            enUzak = Math.max(enUzak, GezegenYerlesimi.yuvaKutup(i));
            enYakin = Math.min(enYakin, GezegenYerlesimi.yuvaKutup(i));
        }
        assertTrue(enUzak >= 60 && enYakin <= 30, "kutup açıları yayılmalı: " + enYakin + "…" + enUzak);
        int azimutKovasi = 0;
        for (int k = 0; k < 4; k++) {
            for (int i = 0; i < GezegenYerlesimi.SAYI; i++) {
                double az = GezegenYerlesimi.yuvaAzimut(i);
                if (az >= k * 90 && az < (k + 1) * 90) {
                    azimutKovasi++;
                    break;
                }
            }
        }
        assertTrue(azimutKovasi >= 3, "en az üç çeyrek azimutta gezegen var: " + azimutKovasi);
    }

    /** Her sürüklenme uç noktasında (±10° azimut, ±5° kutup) ve en büyük boylarda iki disk çakışmaz. */
    @Test
    void gezegenlerEnKotuSuruklenmedeCakismaz() {
        float[] u = {-1f, 0f, 1f};
        double enAz = 1e9;
        for (int i = 0; i < GezegenYerlesimi.SAYI; i++) {
            for (int j = i + 1; j < GezegenYerlesimi.SAYI; j++) {
                for (float ai : u) {
                    for (float aj : u) {
                        for (float ki : u) {
                            for (float kj : u) {
                                double bosluk = aci(yon(i, ai, ki), yon(j, aj, kj)) - EN_BUYUK_YARICAP[i] - EN_BUYUK_YARICAP[j];
                                enAz = Math.min(enAz, bosluk);
                                assertTrue(bosluk > 5.0, "gezegen " + i + " ve " + j + " boşluk " + bosluk);
                            }
                        }
                    }
                }
            }
        }
        assertTrue(enAz > 5.0);
    }

    /** Gece yarısı (güneş karşıtı nokta tepede) sekiz gezegen de ufkun en az 15° üstünde. */
    @Test
    void geceYarisiSekiziDeUfkunUstundedir() {
        float[] u = {-1f, 0f, 1f};
        for (int i = 0; i < GezegenYerlesimi.SAYI; i++) {
            for (float a : u) {
                for (float k : u) {
                    double yukseklik = Math.toDegrees(Math.asin(-yon(i, a, k)[1]));
                    assertTrue(yukseklik >= 15.0, "gezegen " + i + " gece yarısı yükseklik " + yukseklik);
                }
            }
        }
    }

    @Test
    void suruklenmeVeKutupSiniriAsilmaz() {
        for (int i = 0; i < GezegenYerlesimi.SAYI; i++) {
            for (float k : new float[] {-50f, -1f, 0f, 1f, 50f}) {
                float[] v = yon(i, 99f, k);
                double kutup = Math.toDegrees(Math.acos(-v[1]));
                assertTrue(kutup >= GezegenYerlesimi.EN_AZ_KUTUP - 1e-3 && kutup <= GezegenYerlesimi.EN_COK_KUTUP + 1e-3,
                        "kutup " + kutup);
            }
        }
    }

    /** Aynı gezegen uzaklaşınca küçülür; aynı uzaklıkta büyük gezegen küçükten büyük görünür. */
    @Test
    void boyUzakliktaKuculurBuyukGezegenBuyukGorunur() {
        float yakin = GezegenYerlesimi.gorunurDerece(12104, 0.4);
        float orta = GezegenYerlesimi.gorunurDerece(12104, 1.0);
        float uzak = GezegenYerlesimi.gorunurDerece(12104, 1.7);
        assertTrue(yakin > orta && orta > uzak, "Venüs yaklaşınca büyür: " + yakin + " " + orta + " " + uzak);
        assertTrue(GezegenYerlesimi.gorunurDerece(139820, 5.0) > GezegenYerlesimi.gorunurDerece(49244, 5.0));
    }

    /** Tipik uzaklıklarda boylar belirgin farklıdır ve hiçbiri gözle seçilemeyecek kadar küçük değildir. */
    @Test
    void tipikBoylarFarkliVeGorunurdur() {
        float merkur = GezegenYerlesimi.gorunurDerece(4879, 1.1);
        float venus = GezegenYerlesimi.gorunurDerece(12104, 1.0);
        float mars = GezegenYerlesimi.gorunurDerece(6779, 1.5);
        float jupiter = GezegenYerlesimi.gorunurDerece(139820, 4.5);
        float saturn = GezegenYerlesimi.gorunurDerece(116460, 8.5);
        float uranus = GezegenYerlesimi.gorunurDerece(50724, 18.5);
        float neptun = GezegenYerlesimi.gorunurDerece(49244, 29.5);
        float pluton = GezegenYerlesimi.gorunurDerece(2377, 38.0);
        float[] hepsi = {merkur, venus, mars, jupiter, saturn, uranus, neptun, pluton};
        for (float d : hepsi) {
            assertTrue(d >= 1.2f && d <= 12f, "görünür aralık: " + d);
        }
        assertTrue(jupiter > saturn && saturn > uranus && uranus > neptun && neptun >= pluton,
                "dış gezegenler uzaklıkla küçülür: " + jupiter + " " + saturn + " " + uranus + " " + neptun + " " + pluton);
        assertTrue(jupiter > 1.5f * neptun, "Jüpiter Neptün'den belirgin büyük");
        assertTrue(venus > mars, "yakın ve büyük Venüs Mars'tan büyük");
    }
}
