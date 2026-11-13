package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

import java.util.Arrays;

/**
 * Kalıcı kış maskesi (madde 9, ÇÖZÜLME): dünya seed'inden türetilen düşük frekanslı değer gürültüsü. Minecraft
 * sınıfı kullanmaz, chunk başına O(1)'dir ve aynı seed her zaman aynı maskeyi verir.
 *
 * <p>Gürültünün dağılımı tekdüze değildir; bu yüzden sınıf yüklenirken sabit bir örneklemle ölçülür ve eşik
 * istenen orana (varsayılan %25) denk gelen dilimden seçilir. Sınır, eşiğin çevresindeki {@code bant} genişliğinde
 * {@code smoothstep} ile 0–1 ağırlığa yayılır.
 */
public final class KisMaskesi {
    public static final double VARSAYILAN_ORAN = 0.25;
    public static final double VARSAYILAN_DALGA_BOYU = 1200.0;
    public static final double VARSAYILAN_BANT = 0.03;

    private static final long TUZ = 0x6B69735F6D61736BL;
    private static final long IKINCI_OKTAV_TUZU = 0x2545F4914F6CDD1DL;
    private static final double BIRINCI_AGIRLIK = 0.65;
    private static final double IKINCI_AGIRLIK = 0.35;
    private static final double IKINCI_FREKANS = 3.0;
    private static final double[] DAGILIM = dagilimiOlc();

    private final long seed;
    private final double dalgaBoyu;
    private final double esik;
    private final double bant;

    public KisMaskesi(long dunyaSeed, double oran, double dalgaBoyuBlok, double bant) {
        if (!(oran >= 0.0 && oran <= 1.0)) {
            throw new IllegalArgumentException("oran [0, 1] dışında: " + oran);
        }
        if (!(dalgaBoyuBlok >= 16.0)) {
            throw new IllegalArgumentException("dalga boyu en az 16 blok olmalı: " + dalgaBoyuBlok);
        }
        if (!(bant >= 0.0)) {
            throw new IllegalArgumentException("bant negatif: " + bant);
        }
        this.seed = karistir(dunyaSeed ^ TUZ);
        this.dalgaBoyu = dalgaBoyuBlok;
        this.esik = esikBul(oran);
        this.bant = bant;
    }

    /** Blok konumunda ham gürültü, [0, 1]. */
    public double gurultu(int blokX, int blokZ) {
        return ham(seed, blokX / dalgaBoyu, blokZ / dalgaBoyu);
    }

    /** Chunk merkezindeki kış ağırlığı: 0 = ılık, 1 = kalıcı kış, arası = sınır kuşağı. */
    public double agirlik(int chunkX, int chunkZ) {
        return agirlikNoktada(chunkX * 16 + 8, chunkZ * 16 + 8);
    }

    /** Blok noktasındaki kış ağırlığı (sütun düzeyindeki çift doğrusal ara değerin köşeleri). */
    public double agirlikNoktada(int blokX, int blokZ) {
        return yumusakBasamak(esik - bant, esik + bant, gurultu(blokX, blokZ));
    }

    public boolean kisMi(int chunkX, int chunkZ) {
        return agirlik(chunkX, chunkZ) >= 0.5;
    }

    public double esik() {
        return esik;
    }

    private static double esikBul(double oran) {
        if (oran <= 0.0) {
            return Double.POSITIVE_INFINITY;
        }
        if (oran >= 1.0) {
            return Double.NEGATIVE_INFINITY;
        }
        int i = (int) Math.round((1.0 - oran) * (DAGILIM.length - 1));
        return DAGILIM[i];
    }

    /** İki oktavlı değer gürültüsü; {@code u, v} birinci oktavın kafes biriminde. */
    static double ham(long s, double u, double v) {
        return BIRINCI_AGIRLIK * deger(s, u, v)
                + IKINCI_AGIRLIK * deger(s ^ IKINCI_OKTAV_TUZU, u * IKINCI_FREKANS + 0.37, v * IKINCI_FREKANS + 0.71);
    }

    private static double deger(long s, double u, double v) {
        double tabanU = Math.floor(u);
        double tabanV = Math.floor(v);
        long iu = (long) tabanU;
        long iv = (long) tabanV;
        double su = besinciDerece(u - tabanU);
        double sv = besinciDerece(v - tabanV);
        double a = kafes(s, iu, iv);
        double b = kafes(s, iu + 1, iv);
        double c = kafes(s, iu, iv + 1);
        double d = kafes(s, iu + 1, iv + 1);
        double ab = a + (b - a) * su;
        double cd = c + (d - c) * su;
        return ab + (cd - ab) * sv;
    }

    private static double besinciDerece(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double kafes(long s, long x, long z) {
        long h = karistir(s ^ (x * 0x9E3779B97F4A7C15L) ^ karistir(z * 0xC2B2AE3D27D4EB4FL));
        return (h >>> 11) * 0x1.0p-53;
    }

    /** SplitMix64 karıştırıcısı. */
    static long karistir(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double yumusakBasamak(double alt, double ust, double x) {
        if (!(ust > alt)) {
            return x >= alt ? 1.0 : 0.0;
        }
        double t = (x - alt) / (ust - alt);
        t = t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t);
        return t * t * (3.0 - 2.0 * t);
    }

    /** Gürültünün dağılımı: sabit seed, R2 dizisiyle 4096² kafes hücresine yayılmış 65536 örnek. */
    private static double[] dagilimiOlc() {
        int n = 1 << 16;
        double[] d = new double[n];
        long s = karistir(0x5EEDL);
        for (int k = 0; k < n; k++) {
            double u = ((k * 0.7548776662466927) % 1.0) * 4096.0;
            double v = ((k * 0.5698402909980532) % 1.0) * 4096.0;
            d[k] = ham(s, u, v);
        }
        Arrays.sort(d);
        return d;
    }
}
