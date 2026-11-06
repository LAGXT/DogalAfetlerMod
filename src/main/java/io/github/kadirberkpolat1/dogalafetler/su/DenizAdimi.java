package io.github.kadirberkpolat1.dogalafetler.su;

/** Deniz seviyesi sürecinin saf adım hesapları (bölüm H, N10): yükselme, geri dönüş, buharlaşma. */
public final class DenizAdimi {
    /** Gerçek dakikanın ölçekli tick karşılığı (hız 1, 20 TPS). */
    public static final long DAKIKA = 1200;

    private DenizAdimi() {
    }

    /** {@code mevcut}'u {@code hedef}'e {@code hiz} blok/tick ile {@code adim} tick yaklaştırır; hedefi aşmaz. */
    public static float yaklas(float mevcut, float hedef, double hiz, int adim) {
        if (adim <= 0 || mevcut == hedef) {
            return mevcut;
        }
        double fark = hiz * adim;
        return mevcut < hedef ? (float) Math.min(hedef, mevcut + fark) : (float) Math.max(hedef, mevcut - fark);
    }

    /** Hedef artışı {@code [0, min(ustSinir, EN_COK_ARTIS)]} aralığına kırpar. */
    public static float hedef(float deger, int ustSinir) {
        float sinir = Math.min(ustSinir, DenizAlani.EN_COK_ARTIS);
        return Float.isNaN(deger) ? 0f : Math.max(0f, Math.min(sinir, deger));
    }

    public static long dakikaTick(double dakika) {
        return Math.round(dakika * DAKIKA);
    }

    /**
     * N10: buharlaşma derinliği önce sanal yükselmeyi yer. {@code taban} buharlaşma başladığındaki artış; sonuç hiçbir zaman
     * {@code mevcut}'tan büyük olmaz (derinlik azalırsa yükselme geri gelmez).
     */
    public static float buharlasmaSonrasi(float mevcut, float taban, float derinlik) {
        return Math.min(mevcut, Math.max(0f, taban - derinlik));
    }
}
