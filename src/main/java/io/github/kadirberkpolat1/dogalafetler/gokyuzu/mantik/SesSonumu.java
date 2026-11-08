package io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik;

/**
 * Pulsar ve kara delik seslerinin derinlik/örtü sönümü (pulsar-2 tur 4 D): {@code k = gökOranı × derinlik}. Gök oranı,
 * oyuncunun üstündeki 5×5 sütunda gök ışığı 15 olan hücrelerin payıdır (istemci hesaplar); derinlik deniz seviyesinin
 * ({@value #DENIZ_SEVIYESI}) altına her {@value #KATMAN_BLOK} blokta %25 azalır, y ≤ −1'de 0'dır. Her ses iki varyantla çalar:
 * kuru dosyanın kazancı {@code k}, boğuk-yankılı dosyanınki {@code 0,35 · (1 − k)} (k = 1 yalnız kuru, k = 0 yalnız
 * boğuk-yankılı %35). İki kazancın kareleri toplamı hiçbir k'de 1'i geçmez: karışım dosyanın kendi ses yüksekliğinden
 * (−26 / −23 LUFS) yüksek çıkamaz, MASTER'daki −14 LUFS tavanının altında kalır. Minecraft sınıfı kullanmaz.
 */
public final class SesSonumu {
    public static final int DENIZ_SEVIYESI = 63;
    public static final int KATMAN_BLOK = 16;
    /** Deniz seviyesinin altında her katmanda derinlik kaybı. */
    public static final double KATMAN_KAYBI = 0.25;
    /** k = 0'da (tam kapalı / derin) boğuk-yankılı varyantın kazancı. */
    public static final float YANKI_TAVANI = 0.35f;

    private SesSonumu() {
    }

    /** Derinlik çarpanı 0–1: y ≥ {@value #DENIZ_SEVIYESI} → 1, her {@value #KATMAN_BLOK} blok aşağıda −0,25, y ≤ −1 → 0. */
    public static float derinlik(double y) {
        if (Double.isNaN(y)) {
            return 0f;
        }
        return (float) kenetle(1.0 - KATMAN_KAYBI * (DENIZ_SEVIYESI - y) / KATMAN_BLOK);
    }

    /** Sönüm katsayısı 0–1: gök oranı (0–1'e kenetlenir) × derinlik. */
    public static float k(float gokOrani, double y) {
        float oran = Float.isNaN(gokOrani) ? 0f : (float) kenetle(gokOrani);
        return oran * derinlik(y);
    }

    /** Kuru varyantın kazancı: k. */
    public static float kuruKazanc(float k) {
        return Float.isNaN(k) ? 0f : (float) kenetle(k);
    }

    /** Boğuk-yankılı varyantın kazancı: {@value #YANKI_TAVANI} · (1 − k). */
    public static float yankiKazanc(float k) {
        return YANKI_TAVANI * (1f - kuruKazanc(k));
    }

    private static double kenetle(double x) {
        return Math.max(0.0, Math.min(1.0, x));
    }
}
