package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Ağız buharının görünen davranışı (§A8, S3-11): bir nefes birkaç bulutçuktan oluşan küçük bir buluttur (donmada daha
 * yoğun); bulutçuk ömrünün ilk %45'inde tam opak kalır, sonra yumuşakça söner ve yayılır. İstemci parçacığı ve sunucunun
 * diğer oyunculara gönderdiği nefes aynı değerleri kullanır.
 */
public final class NefesBuhari {
    /** Parçacığın taban opaklığı (doku alfasıyla çarpılır). */
    public static final float ALFA = 0.92f;
    /** Opaklığın tam kaldığı ömür payı. */
    private static final float TAM = 0.45f;

    private NefesBuhari() {
    }

    /** Bir nefesteki bulutçuk sayısı (rastgele ek hariç): üşümede 6, donmada 9; diğerlerinde nefes yok. */
    public static int bulutcuk(SicaklikDurumu durum) {
        return switch (durum) {
            case USUME -> 6;
            case DONMA -> 9;
            default -> 0;
        };
    }

    /** Ömrün {@code oran} (0–1) noktasındaki opaklık çarpanı: {@value #TAM}'e kadar 1, sonra kosinüsle 0'a. */
    public static float opaklik(float oran) {
        if (oran <= TAM) {
            return 1f;
        }
        float t = Math.min(1f, (oran - TAM) / (1f - TAM));
        return 0.5f * (1f + (float) Math.cos(Math.PI * t));
    }

    /** Ömrün {@code oran} noktasındaki boy çarpanı: ömür sonunda 2,2 kat. */
    public static float buyume(float oran) {
        return 1f + oran * 1.2f;
    }
}
