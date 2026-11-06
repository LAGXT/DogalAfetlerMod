package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

/**
 * Sütun düzeyinde kış ağırlığı (madde 9: "kış bölgelerinin sınırında geçiş yumuşak olsun"). Minecraft sınıfı kullanmaz.
 *
 * <p>{@code w = max(maske, biyom, yükseklik)}:
 * <ul>
 * <li>maske: chunk köşelerindeki 4 maske ağırlığından çift doğrusal ara değer;</li>
 * <li>biyom: her köşe, köşeye değen 2×2 chunk ortasının soğuk biyom oranı; sütunda çift doğrusal (bir chunk'ın sütunları
 * 3×3 chunk'tan etkilenir);</li>
 * <li>yükseklik: {@code smoothstep(yAlt, yUst, yüzeyY)}; {@code yUst} şartnamedeki Y=120, böylece Y ≥ 120 tam kış.</li>
 * </ul>
 * Her bileşen blok başına en çok ~0,05 değişir, maksimum da bu sınırı korur. Ilık ofseti köşe sıcaklıklarının çift doğrusal
 * ara değerinden gelir.
 */
public final class KisAlani {
    /** Chunk ortasındaki biyom örneği; aynı girdi her zaman aynı sonucu vermeli. */
    public interface ChunkOrnegi {
        boolean soguk(int cx, int cz);

        double sicaklik(int cx, int cz);
    }

    /**
     * @param yAlt yükseklik ağırlığının 0 olduğu yüzey Y'si
     * @param yUst yükseklik ağırlığının 1 olduğu yüzey Y'si
     * @param ofsetSicaklikTabani bu biyom sıcaklığının altında ofset yok
     * @param ofsetKatsayisi sıcaklık birimi başına seviye puanı
     * @param ofsetEnCok ofset üst sınırı (seviye puanı)
     */
    public record Ayar(int yAlt, int yUst, double ofsetSicaklikTabani, double ofsetKatsayisi, double ofsetEnCok) {
        public static final Ayar VARSAYILAN = new Ayar(88, 120, 0.3, 8.0, 10.0);

        public Ayar {
            if (yUst <= yAlt) {
                throw new IllegalArgumentException("yUst yAlt'tan büyük olmalı");
            }
        }
    }

    private final KisMaskesi maske;
    private final ChunkOrnegi ornek;
    private final Ayar ayar;

    public KisAlani(KisMaskesi maske, ChunkOrnegi ornek, Ayar ayar) {
        this.maske = maske;
        this.ornek = ornek;
        this.ayar = ayar;
    }

    public Ayar ayar() {
        return ayar;
    }

    /** Blok noktasındaki maske ağırlığı (biyom örneği almaz; geniş aramalar için ucuz). */
    public double maskeNoktada(int x, int z) {
        return maske.agirlikNoktada(x, z);
    }

    public Chunk chunk(int cx, int cz) {
        return new Chunk(cx, cz);
    }

    /** Bir chunk'ın köşe değerleri; sıra 00, 10, 01, 11 (x, z). */
    public final class Chunk implements KisKaynagi {
        private final double[] maskeK = new double[4];
        private final double[] biyomK = new double[4];
        private final double[] sicaklikK = new double[4];

        private Chunk(int cx, int cz) {
            boolean[] soguk = new boolean[9];
            double[] sicak = new double[9];
            for (int dz = -1; dz <= 1; dz++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int i = (dz + 1) * 3 + dx + 1;
                    soguk[i] = ornek.soguk(cx + dx, cz + dz);
                    sicak[i] = ornek.sicaklik(cx + dx, cz + dz);
                }
            }
            for (int j = 0; j <= 1; j++) {
                for (int i = 0; i <= 1; i++) {
                    int k = j * 2 + i;
                    maskeK[k] = maske.agirlikNoktada((cx + i) * 16, (cz + j) * 16);
                    double so = 0;
                    double si = 0;
                    for (int b = 0; b <= 1; b++) {
                        for (int a = 0; a <= 1; a++) {
                            int h = (j + b) * 3 + i + a;
                            so += soguk[h] ? 1 : 0;
                            si += sicak[h];
                        }
                    }
                    biyomK[k] = so / 4.0;
                    sicaklikK[k] = si / 4.0;
                }
            }
        }

        public double maskeAgirligi(int x, int z) {
            return ciftDogrusal(maskeK, x & 15, z & 15);
        }

        public double biyomAgirligi(int x, int z) {
            return ciftDogrusal(biyomK, x & 15, z & 15);
        }

        public double yukseklikAgirligi(int yuzeyY) {
            return yumusakBasamak(ayar.yAlt(), ayar.yUst(), yuzeyY);
        }

        public double sicaklik(int x, int z) {
            return ciftDogrusal(sicaklikK, x & 15, z & 15);
        }

        @Override
        public double agirlik(int x, int z, int yuzeyY) {
            return Math.max(maskeAgirligi(x, z), Math.max(biyomAgirligi(x, z), yukseklikAgirligi(yuzeyY)));
        }

        @Override
        public double ofset(int x, int z) {
            double o = ayar.ofsetKatsayisi() * (sicaklik(x, z) - ayar.ofsetSicaklikTabani());
            return Math.max(0.0, Math.min(ayar.ofsetEnCok(), o));
        }
    }

    /** Köşeler blok sınırlarında, sütun ortası {@code (yerel + 0,5) / 16}: komşu chunk'larda sürekli. */
    static double ciftDogrusal(double[] k, int yerelX, int yerelZ) {
        double fx = (yerelX + 0.5) / 16.0;
        double fz = (yerelZ + 0.5) / 16.0;
        double alt = k[0] + (k[1] - k[0]) * fx;
        double ust = k[2] + (k[3] - k[2]) * fx;
        return alt + (ust - alt) * fz;
    }

    public static double yumusakBasamak(double alt, double ust, double x) {
        double t = (x - alt) / (ust - alt);
        t = t < 0.0 ? 0.0 : (t > 1.0 ? 1.0 : t);
        return t * t * (3.0 - 2.0 * t);
    }
}
