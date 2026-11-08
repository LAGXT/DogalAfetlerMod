package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import net.minecraft.util.Mth;

/**
 * Ekran efektlerinin saf şiddet hesapları (§A8, S3-10): vücut sıcaklığının paketteki eşiklere göre konumundan 0–1 şiddet.
 * Bunalmadaki kırmızı kenar izi donmadaki buz kenarının aynasıdır: durumun giriş eşiğinden aynı derinlikte aynı şiddet
 * ve aynı opaklık eğrisi; nabız yalnız yoğunlaştırır. Yanmada kırmızı kenar bunalmanın her anından yoğundur.
 */
public final class EkranSiddeti {
    /** Kenar izinin durum girişindeki şiddeti. */
    private static final float GIRIS = 0.25f;
    /** Giriş eşiğinden şiddetin 1'e vardığı derinlik: 60 vücut birimi (3 °C). */
    private static final float KENAR_ARALIGI = 60f;
    /** Yanmada kırmızı kenarın en düşük şiddeti: bunalmanın nabızlı en yüksek opaklığının üstünde. */
    private static final float YANMA_TABANI = 0.8f;

    private EkranSiddeti() {
    }

    /** Buz kenarı şiddeti 0–1: donma eşiğinde (çok soğuk) 0,25'ten 3 °C aşağısında 1'e; üstünde 0. */
    static float buz(SicaklikDurumPaketi d, float vucut) {
        if (vucut > d.cokSoguk()) {
            return 0f;
        }
        return kenar(d.cokSoguk() - vucut);
    }

    /**
     * Kırmızı kenar şiddeti 0–1: bunalma eşiğinden (sıcak) buz kenarının aynası; yanmada en az 0,8'den aşırı ısınma
     * hasarı eşiğinde 1'e. Bunalmanın altında 0.
     */
    static float kirmizi(SicaklikDurumPaketi d, float vucut) {
        if (vucut < d.sicak()) {
            return 0f;
        }
        float k = kenar(vucut - d.sicak());
        if (vucut >= d.cokSicak()) {
            float f = Mth.clamp((vucut - d.cokSicak()) / Math.max(1f, d.yanma() - d.cokSicak()), 0f, 1f);
            k = Math.max(k, YANMA_TABANI + (1f - YANMA_TABANI) * f);
        }
        return k;
    }

    private static float kenar(float derinlik) {
        return GIRIS + (1f - GIRIS) * Mth.clamp(derinlik / KENAR_ARALIGI, 0f, 1f);
    }

    /** Şiddetin ekrandaki opaklığı (iki kenar için aynı eğri). */
    private static float alfa(float siddet) {
        return (float) Math.pow(siddet, 0.7);
    }

    /** Buz kenarının ekrandaki opaklığı 0–1; donma hasarı eşiğinin altında yavaş nabız. */
    static float buzOpaklik(SicaklikDurumPaketi d, float vucut, float zaman) {
        float b = buz(d, vucut);
        if (b <= 0f) {
            return 0f;
        }
        float nabiz = vucut <= d.donma() ? 0.08f * Mth.sin(zaman * 0.35f) : 0f;
        return Mth.clamp(alfa(b) + nabiz, 0f, 1f);
    }

    /** Kırmızı kenarın ekrandaki opaklığı 0–1: buz kenarıyla aynı eğri, üstüne HUD hapıyla aynı kalp atışı. */
    static float kirmiziOpaklik(SicaklikDurumPaketi d, float vucut, float zaman) {
        float k = kirmizi(d, vucut);
        if (k <= 0f) {
            return 0f;
        }
        return Mth.clamp(alfa(k) * kalpAtisi(d, vucut, zaman), 0f, 1f);
    }

    /** Kırmızı kenarın nabız çarpanı (1 ve üstü): vuruşta yükselip söner; bunalmada sığ (%12), yanmada derin (%30). */
    private static float kalpAtisi(SicaklikDurumPaketi d, float vucut, float zaman) {
        float periyot = HudCizimi.nabizPeriyodu(vucut, d.sicak(), d.yanma());
        float faz = (zaman % periyot) / periyot;
        float derinlik = vucut >= d.cokSicak() ? 0.3f : 0.12f;
        return 1f + derinlik * (float) Math.exp(-faz * 5.0);
    }

    /** Sıcak hava ve yalpa şiddeti 0–1: yalnız yanmada, çok sıcak eşiğinden aşırı ısınma hasarı eşiğine. */
    static float sicak(SicaklikDurumPaketi d, float vucut) {
        if (vucut < d.cokSicak()) {
            return 0f;
        }
        return 0.3f + 0.7f * Mth.clamp((vucut - d.cokSicak()) / Math.max(1f, d.yanma() - d.cokSicak()), 0f, 1f);
    }

    /** Sıcak hava vinyetinin opaklığı 0–1; aşırı ısınma hasarı eşiğinde nabız. */
    static float sicakOpaklik(SicaklikDurumPaketi d, float vucut, float zaman) {
        float s = sicak(d, vucut);
        if (s <= 0f) {
            return 0f;
        }
        float nabiz = vucut >= d.yanma() ? 0.12f * Mth.sin(zaman * 0.45f) : 0f;
        return Mth.clamp(0.2f + 0.55f * s + nabiz, 0f, 1f);
    }

    /** Titreme şiddeti 0–1: üşümede 0,25 (ara ara), donmada buz kenarıyla birlikte artar. */
    static float titreme(SicaklikDurumPaketi d, float vucut) {
        if (vucut > d.soguk()) {
            return 0f;
        }
        return vucut > d.cokSoguk() ? 0.25f : 0.4f + 0.6f * buz(d, vucut);
    }
}
