package io.github.kadirberkpolat1.dogalafetler.esya.mantik;

/**
 * Sismografın action bar'daki "tahmini şiddet (%)" değeri (madde 13b). Gerçek şiddet seviyeden gelir (S1 = %20 … S5 =
 * %100); öncü sarsıntılar az bilgi taşıdığı için tahmin bir hata payıyla saptırılır. Pay afet yaklaştıkça (ön-uyarı
 * ilerlemesi 0 → 1) daralır; Hassas Sismograf'ın payı normalin dörtte biridir. Sapma afet başına sabittir (tohum), böylece
 * değer saniyeden saniyeye zıplamaz, yalnız gerçeğe doğru kayar.
 */
public final class SiddetTahmini {
    /** İlerleme 0'daki yarı genişlik (yüzde puanı). */
    public static final double NORMAL_PAY = 24;
    public static final double HASSAS_PAY = 6;

    private SiddetTahmini() {
    }

    public static int gercekYuzde(int seviye) {
        return 20 * Math.max(1, Math.min(5, seviye));
    }

    /** Tahminin yarı genişliği: ilerleme 1'de başlangıcın %30'u. */
    public static double hataPayi(boolean hassas, double ilerleme) {
        double i = Math.max(0, Math.min(1, ilerleme));
        return (hassas ? HASSAS_PAY : NORMAL_PAY) * (1 - 0.7 * i);
    }

    public static int yuzde(int seviye, double ilerleme, boolean hassas, long tohum) {
        double sapma = (Tohum.birim(tohum) * 2 - 1) * hataPayi(hassas, ilerleme);
        return (int) Math.max(1, Math.min(100, Math.round(gercekYuzde(seviye) + sapma)));
    }
}
