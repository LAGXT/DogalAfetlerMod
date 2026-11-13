package io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Hissedilen sıcaklık (S3-02): mekan sıcaklığı ({@link OrtamSicakligi}) + kişisel düzeltmeler. Vücudun ısı alışverişi
 * bununla çalışır.
 * <ul>
 * <li>{@link Katki#ISLAKLIK}: {@code −islakSogutma · W} (buharlaşma; sıcakta da serinletir).</li>
 * <li>{@link Katki#RUZGAR}: yalnız gökyüzü açıkken. Hız {@code V = max(0, y−64)·ruzgarYukseklik + yağış payı + koşma
 * payı} (km/h); düzeltme {@code (WCI(T, V) − T) · yumuşat((V − 4,8)/5) · yumuşat((10 − T)/5)} (WCI: NWS rüzgâr
 * soğuğu). T &gt; 10 °C ya da V ≤ 4,8 iken 0; iki yumuşatma sınırlarda sıçramayı önler.</li>
 * <li>{@link Katki#SOL_EL}: sol elde Buz Kovası {@code buzKovasi}, lav kovası {@code lavKovasi}.</li>
 * </ul>
 */
public final class Hissedilen {
    /** NWS formulünün geçerli olduğu en düşük rüzgâr (km/h). */
    public static final double RUZGAR_ESIGI = 4.8;
    /** Rüzgâr soğuğunun başladığı mekan sıcaklığı (°C). */
    public static final double RUZGAR_SICAKLIK_SINIRI = 10.0;
    /** Rüzgârın yükseklikle artmaya başladığı Y. */
    public static final int RUZGAR_TABAN_Y = 64;
    /** Buz Kovası bu erime değerine varınca su kovasına döner. */
    public static final double ERIME_SINIRI = 300.0;

    private Hissedilen() {
    }

    /**
     * @param islakSogutma      tam ıslak oyuncunun serinlemesi (°C)
     * @param ruzgarYukseklik   Y 64 üstünde blok başına rüzgâr (km/h)
     * @param ruzgarYagmur      yağışta ek rüzgâr (km/h)
     * @param ruzgarFirtina     fırtınada ek rüzgâr (km/h)
     * @param ruzgarKosma       koşarken ek rüzgâr (km/h)
     * @param buzKovasi         sol eldeki Buz Kovasının düzeltmesi (°C)
     * @param lavKovasi         sol eldeki lav kovasının düzeltmesi (°C)
     * @param erimeEsigi        sol eldeki Buz Kovasının erimeye başladığı mekan sıcaklığı (°C)
     */
    public record Ayarlar(double islakSogutma, double ruzgarYukseklik, double ruzgarYagmur, double ruzgarFirtina,
            double ruzgarKosma, double buzKovasi, double lavKovasi, double erimeEsigi) {
        public static final Ayarlar VARSAYILAN = new Ayarlar(5, 0.25, 8, 20, 12, -12, 12, 25);

        public double solEl(SolEl el) {
            return switch (el) {
                case YOK -> 0;
                case BUZ_KOVASI -> buzKovasi;
                case LAV_KOVASI -> lavKovasi;
            };
        }
    }

    /**
     * @param mekan      mekan sıcaklığı (°C)
     * @param islaklik   W (0–1)
     * @param gokyuzu    gökyüzü açık mı (rüzgâr yalnız açık havada)
     * @param y          oyuncunun Y'si
     * @param yagis      oyuncunun yerindeki yağış
     */
    public record Girdi(double mekan, double islaklik, boolean gokyuzu, int y, Yagis yagis, boolean kosuyor,
            SolEl solEl) {
    }

    /**
     * @param duzeltmeler  sıfır olmayan hissedilen kalemleri (yalnız {@link Katki#hissedilen()} anahtarları)
     * @param ruzgarKmh    rüzgâr hızı (gökyüzü kapalıyken 0)
     */
    public record Sonuc(double mekan, double toplam, Map<Katki, Double> duzeltmeler, double ruzgarKmh, SolEl solEl) {
        public double duzeltme(Katki k) {
            return duzeltmeler.getOrDefault(k, 0.0);
        }
    }

    public static Sonuc hesapla(Girdi g, Ayarlar a) {
        EnumMap<Katki, Double> d = new EnumMap<>(Katki.class);
        double islak = -a.islakSogutma() * Islaklik.sinirla(g.islaklik());
        if (islak != 0) {
            d.put(Katki.ISLAKLIK, islak);
        }
        double v = ruzgarHizi(g, a);
        double ruzgar = ruzgarSogugu(g.mekan(), v);
        if (ruzgar != 0) {
            d.put(Katki.RUZGAR, ruzgar);
        }
        double el = a.solEl(g.solEl());
        if (el != 0) {
            d.put(Katki.SOL_EL, el);
        }
        double toplam = g.mekan();
        for (double x : d.values()) {
            toplam += x;
        }
        return new Sonuc(g.mekan(), toplam, Collections.unmodifiableMap(d), v, g.solEl());
    }

    /** Rüzgâr hızı (km/h); gökyüzü kapalıyken 0. */
    public static double ruzgarHizi(Girdi g, Ayarlar a) {
        if (!g.gokyuzu()) {
            return 0;
        }
        double v = Math.max(0, g.y() - RUZGAR_TABAN_Y) * a.ruzgarYukseklik();
        v += switch (g.yagis()) {
            case YOK -> 0;
            case YAGMUR, KAR -> a.ruzgarYagmur();
            case FIRTINA -> a.ruzgarFirtina();
        };
        return g.kosuyor() ? v + a.ruzgarKosma() : v;
    }

    /** Rüzgâr soğuğu düzeltmesi (≤ 0, °C): sınırlarda yumuşatılmış {@code WCI(T, V) − T}. */
    public static double ruzgarSogugu(double t, double v) {
        if (t > RUZGAR_SICAKLIK_SINIRI || v <= RUZGAR_ESIGI) {
            return 0;
        }
        double fark = Math.min(0, wci(t, v) - t);
        return fark * yumusat((v - RUZGAR_ESIGI) / 5.0) * yumusat((RUZGAR_SICAKLIK_SINIRI - t) / 5.0);
    }

    /** NWS rüzgâr soğuğu (°C, km/h). */
    public static double wci(double t, double v) {
        double p = Math.pow(v, 0.16);
        return 13.12 + 0.6215 * t - 11.37 * p + 0.3965 * t * p;
    }

    /** Sol eldeki Buz Kovasının saniyedeki erime artışı: eşik üstünde {@code (mekan − eşik)/10}, altında 0. */
    public static double erimeArtisi(double mekanC, Ayarlar a) {
        return Math.max(0, (mekanC - a.erimeEsigi()) / 10.0);
    }

    private static double yumusat(double x) {
        double t = Math.max(0, Math.min(1, x));
        return t * t * (3 - 2 * t);
    }
}
