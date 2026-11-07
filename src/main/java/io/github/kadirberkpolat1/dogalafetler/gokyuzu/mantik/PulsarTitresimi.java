package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Pulsarın yanal titreşimi (pulsar-2, PL-2): dönme yok; çekirdek ve ışınlar iki eksende sağa sola savrulur. İki bileşen:
 * hızlı kare/testere titreşimi ({@value #HIZLI_HZ} Hz, Düşük kalitede {@value #DUSUK_HZ} Hz; erişilebilirlik ayarıyla
 * ölçeklenir, 0'da yok) ve büyük genlikli yavaş sarsıntı (1–2 Hz; ayardan bağımsız). Düzensiz "çatırtı" parlamaları
 * saniyede ortalama {@value #CATIRTI_HZ}, her biri 0,10–0,15 sn. Girdi gerçek zaman saniyesidir: değerler kare süresinden
 * bağımsızdır. Açılar derece. Minecraft sınıfı kullanmaz.
 */
public final class PulsarTitresimi {
    public static final float HIZLI_HZ = 16f;
    public static final float DUSUK_HZ = 8f;
    /** Çatırtı parlamalarının ortalama sıklığı (1/sn). */
    public static final float CATIRTI_HZ = 2f;
    public static final float CATIRTI_EN_KISA_SN = 0.10f;
    public static final float CATIRTI_EN_UZUN_SN = 0.15f;
    /** Testere (dikey) ekseninin kare eksenine oranı: titreşim bir çizgi boyunca değil, düzlemde savrulur. */
    static final float TESTERE_ORANI = 0.45f;
    /** Her yarım periyodun genliği 0,6–1 arası karmayla değişir (makine gibi düzgün görünmesin). */
    static final float EN_AZ_ADIM = 0.6f;
    /** Çatırtı yuvası: her yuvada en çok bir parlama, olasılık {@code CATIRTI_HZ × YUVA_SN}. */
    private static final double YUVA_SN = 0.25;
    private static final double[] YAVAS_HZ_X = {1.3, 1.9};
    private static final double[] YAVAS_HZ_Y = {1.7, 1.1};

    private PulsarTitresimi() {
    }

    /** Hızlı bileşenin tepe genliği (derece): {@code (0,6 + 0,4·güç) × ayar}; ayar 0–1'e kenetlenir. */
    public static float hizliGenlik(float guc, float ayar) {
        return (0.6f + 0.4f * kenetle(guc)) * kenetle(ayar);
    }

    /** Yavaş sarsıntının tepe genliği (derece): güçle 1,5°'den 2,5°'ye. */
    public static float yavasGenlik(float guc) {
        return 1.5f + kenetle(guc);
    }

    public static float frekans(boolean dusuk) {
        return dusuk ? DUSUK_HZ : HIZLI_HZ;
    }

    /**
     * Hızlı bileşen: {@code cikti[0]} kare dalga (yatay), {@code cikti[1]} testere (dikey, {@value #TESTERE_ORANI} oranında).
     * {@code carpan} kara delik yutmasındaki zirve çarpanıdır (normalde 1).
     */
    public static void hizliOfset(double saniye, float guc, float ayar, boolean dusuk, float carpan, float[] cikti) {
        float a = hizliGenlik(guc, ayar) * Math.max(0f, carpan);
        if (a <= 0f) {
            cikti[0] = 0f;
            cikti[1] = 0f;
            return;
        }
        double f = frekans(dusuk);
        double yarim = saniye * f * 2.0;
        long adim = (long) Math.floor(yarim);
        float isaret = (adim & 1L) == 0L ? 1f : -1f;
        float adimGenlik = EN_AZ_ADIM + (1f - EN_AZ_ADIM) * birim(karma(adim * 0x9E3779B97F4A7C15L + 17L));
        cikti[0] = a * isaret * adimGenlik;
        double testere = saniye * f + 0.3;
        cikti[1] = a * TESTERE_ORANI * (float) (2.0 * (testere - Math.floor(testere)) - 1.0);
    }

    /** Yavaş sarsıntı (1–2 Hz iki sinüs toplamı; her eksende tepe ≤ {@link #yavasGenlik}). */
    public static void yavasOfset(double saniye, float guc, float carpan, float[] cikti) {
        double a = yavasGenlik(guc) * Math.max(0f, carpan);
        double w = 2.0 * Math.PI * saniye;
        cikti[0] = (float) (a * (0.6 * Math.sin(w * YAVAS_HZ_X[0]) + 0.4 * Math.sin(w * YAVAS_HZ_X[1] + 1.1)));
        cikti[1] = (float) (a * (0.6 * Math.sin(w * YAVAS_HZ_Y[0] + 0.5) + 0.4 * Math.sin(w * YAVAS_HZ_Y[1] + 2.3)));
    }

    /**
     * Çatırtı parlaklığı 0–1: zaman {@value #YUVA_SN} sn'lik yuvalara bölünür, yuvanın karması parlama olup olmadığını,
     * başlangıcını ve süresini (0,10–0,15 sn) seçer; parlama anında tam, karesel söner. Yuva sınırını aşmaz.
     */
    public static float catirti(double saniye) {
        long yuva = (long) Math.floor(saniye / YUVA_SN);
        long k = karma(yuva);
        if (birim(k) >= CATIRTI_HZ * YUVA_SN) {
            return 0f;
        }
        float sure = CATIRTI_EN_KISA_SN + (CATIRTI_EN_UZUN_SN - CATIRTI_EN_KISA_SN) * birim(karma(k ^ 0x5DEECE66DL));
        double bas = yuva * YUVA_SN + (YUVA_SN - sure) * birim(karma(k + 0x2545F4914F6CDD1DL));
        double x = (saniye - bas) / sure;
        if (x < 0.0 || x >= 1.0) {
            return 0f;
        }
        return (float) (1.0 - x * x);
    }

    private static float kenetle(float x) {
        return Float.isNaN(x) ? 0f : Math.max(0f, Math.min(1f, x));
    }

    /** SplitMix64 son karıştırması. */
    private static long karma(long x) {
        long z = x + 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Karmanın üst 24 bitinden [0, 1). */
    private static float birim(long k) {
        return (k >>> 40) / (float) (1 << 24);
    }
}
