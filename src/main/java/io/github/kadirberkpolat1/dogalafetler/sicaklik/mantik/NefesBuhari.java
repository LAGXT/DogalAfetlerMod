package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Ağız buharının görünen davranışı (§A8, S3-11): ~2,5 sn'de bir nefes; nefes birkaç bulutçuktan oluşan küçük bir buluttur,
 * donmada daha yoğun ve sıkışık (bulutçuklar üst üste biner). Bulutçuk ömrünün ilk %45'inde tam opak kalır, sonra
 * yumuşakça söner ve yayılır. Üst dünyada dünya donması sürerken nefes üretilmez (onun nefesi zaten çizilir; çift buhar
 * olmaz). İstemci parçacığı ve sunucunun diğer oyunculara gönderdiği nefes aynı değerleri kullanır.
 */
public final class NefesBuhari {
    /** Parçacığın taban opaklığı (doku alfasıyla çarpılır). */
    public static final float ALFA = 0.92f;
    /** Nefes döngüsü (tick): 2,5 sn. */
    public static final int DONGU = 50;
    /** Opaklığın tam kaldığı ömür payı. */
    private static final float TAM = 0.45f;

    private NefesBuhari() {
    }

    /** Bu durumda ve dünya donması koşulunda nefes buharı üretilir mi. */
    public static boolean uretilir(SicaklikDurumu durum, boolean dunyaDonuyor) {
        return bulutcuk(durum) > 0 && !dunyaDonuyor;
    }

    /** Bir nefesteki bulutçuk sayısı (rastgele ek hariç): üşümede 6, donmada 9; diğerlerinde nefes yok. */
    public static int bulutcuk(SicaklikDurumu durum) {
        return switch (durum) {
            case USUME -> 6;
            case DONMA -> 9;
            default -> 0;
        };
    }

    /**
     * Bir tick'te çıkan en çok bulutçuk: üşümede 1 (nefes ~6 tick'te ağızdan süzülür), donmada 3 (bulutçuklar üst üste
     * biner, bulut küçük ve yoğun kalır; tek tek çıksalar ön kameraya doğru yayılıp soluk bir pusa dönüşür).
     */
    public static int tickBasina(SicaklikDurumu durum) {
        return durum == SicaklikDurumu.DONMA ? 3 : 1;
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

    /** Bir oyuncunun nefes sırası (istemcide kendi buharı): her tick bu tick'te çıkacak bulutçuk sayısını verir. */
    public static final class Ritim {
        private int kalan;

        /**
         * @param ek  nefesin rastgele ek bulutçuğu (0–1), yalnız nefes başında kullanılır
         */
        public int tick(long tick, SicaklikDurumu durum, boolean dunyaDonuyor, int ek) {
            if (!uretilir(durum, dunyaDonuyor)) {
                kalan = 0;
                return 0;
            }
            if (tick % DONGU == 0) {
                kalan = bulutcuk(durum) + ek;
            }
            int n = Math.min(kalan, tickBasina(durum));
            kalan -= n;
            return n;
        }

        public void sifirla() {
            kalan = 0;
        }
    }
}
