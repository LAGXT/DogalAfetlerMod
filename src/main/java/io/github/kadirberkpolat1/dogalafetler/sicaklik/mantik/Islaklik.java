package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

/**
 * Islaklık W ∈ [0, 1] (S3-03): suda hızla, yağmurda yavaşça dolar; kuru yerde (çatı altı dahil) mekan sıcaklığına ve
 * yakındaki ısı kaynaklarına göre kurur. Islanırken kuruma yoktur; yanan oyuncu anında kurur. Islaklık hissedileni
 * serinletir ({@link Hissedilen}) ve ısı iletkenliğini artırır ({@link IsiAlisverisi.Ayarlar#iletkenlik}).
 */
public final class Islaklik {
    /** Bu ve üstü gösterge efekti, HUD damlası ve damla parçacığı alır. */
    public static final double GORUNUR_ESIK = 0.05;

    private Islaklik() {
    }

    /** Oyuncunun bulunduğu yerin ıslatma kaynağı. */
    public enum Kaynak {
        /** Islatmayan her yer (çatı altı, kar yağışı dahil). */
        KURU,
        /** Yağmur altında, gökyüzü açık. */
        YAGMUR,
        /** Fırtınada, gökyüzü açık. */
        FIRTINA,
        /** Suyun içinde ya da kabarcık sütununda. */
        SU
    }

    /**
     * @param suDolum           suda saniyede dolum (0,25: 4 sn'de tam ıslak)
     * @param yagmurDolum       yağmurda saniyede dolum
     * @param firtinaDolum      fırtınada saniyede dolum
     * @param kurumaTabani      10 °C'de, kaynaksız saniyede kuruma (1/150)
     * @param kaynakHizlandirma yakındaki ısı kaynaklarının her °C'sinin kuruma hızına katı çarpanı
     */
    public record Ayarlar(double suDolum, double yagmurDolum, double firtinaDolum, double kurumaTabani,
            double kaynakHizlandirma) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(0.25, 0.012, 0.02, 1.0 / 150.0, 0.1);

        public double dolum(Kaynak k) {
            return switch (k) {
                case KURU -> 0;
                case YAGMUR -> yagmurDolum;
                case FIRTINA -> firtinaDolum;
                case SU -> suDolum;
            };
        }
    }

    /**
     * Kuru yerde saniyede kuruma: {@code kurumaTabani · clamp(0,5 + mekan/20, 0,3, 4) · (1 + max(0, kaynaklar)·h)}.
     * 10 °C'de ~150 sn, 0 °C'de ~300 sn, 30 °C'de ~75 sn; ateş başında çok daha hızlı.
     */
    public static double kurumaHizi(double mekanC, double kaynaklarC, Ayarlar a) {
        double sicaklik = Math.max(0.3, Math.min(4.0, 0.5 + mekanC / 20.0));
        return a.kurumaTabani() * sicaklik * (1 + Math.max(0, kaynaklarC) * a.kaynakHizlandirma());
    }

    /** {@code saniye} sonraki ıslaklık. */
    public static double adim(double islaklik, Kaynak kaynak, boolean yaniyor, double mekanC, double kaynaklarC,
            double saniye, Ayarlar a) {
        if (yaniyor) {
            return 0;
        }
        if (kaynak != Kaynak.KURU) {
            return sinirla(islaklik + a.dolum(kaynak) * saniye);
        }
        return sinirla(islaklik - kurumaHizi(mekanC, kaynaklarC, a) * saniye);
    }

    /** Bu hızla tamamen kurumaya kalan saniye; kurumuyorsa (hız ≤ 0) −1. */
    public static double kalanSaniye(double islaklik, double kurumaHizi) {
        if (islaklik <= 0) {
            return 0;
        }
        return kurumaHizi > 0 ? islaklik / kurumaHizi : -1;
    }

    public static boolean gorunur(double islaklik) {
        return islaklik >= GORUNUR_ESIK;
    }

    public static double sinirla(double islaklik) {
        return Math.max(0.0, Math.min(1.0, islaklik));
    }
}
