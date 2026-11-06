package io.github.kadirberkpolat1.dogalafetler.meteor.mantik;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Çarpma krateri: paraboloit çanak. Yarıçapı {@code R} olan sütunlarda çarpılan yüzey bloğundan (dy = 0) aşağı
 * {@link #derinlik} kadar blok kazılır, üstte {@code ustTemizlik} blok boşaltılır; kazılan sütunun hemen altı astarlanır
 * (merkezde sıcak magma, dışta koyu kaya) ve çanak dışındaki {@link #KENAR_GENISLIGI} blokluk halkaya enkaz yığılır.
 * Sert blokları eleme ({@link #korunanlarHaric}) uygulama anında yapılır; afet blok hasarı kapalıysa plan boştur.
 */
public final class KraterProfili {
    public static final double KENAR_GENISLIGI = 1.5;
    /** Astarın magma olan merkez oranı (r ≤ oran · R). */
    public static final double MAGMA_ORANI = 0.4;
    private static final double KENAR_OLASILIGI = 0.55;

    public record Hucre(int dx, int dy, int dz) {
    }

    /** Astar ve kenar malzemesi; gerçek blok görünüme göre (güneş / ay) seçilir. */
    public enum Malzeme {
        MAGMA,
        KOYU,
        ORTA,
        ACIK
    }

    public record AstarHucresi(int dx, int dy, int dz, Malzeme malzeme) {
    }

    public record Plan(List<Hucre> kazi, List<AstarHucresi> astar, List<AstarHucresi> kenar) {
        public static final Plan BOS = new Plan(List.of(), List.of(), List.of());

        public Plan {
            kazi = List.copyOf(kazi);
            astar = List.copyOf(astar);
            kenar = List.copyOf(kenar);
        }
    }

    private KraterProfili() {
    }

    /** Merkezden {@code r} uzaklıktaki sütunda yüzey dahil kazılan blok sayısı (yarıçap dışında 0, içinde en az 1). */
    public static int derinlik(double r, double yaricap, double derinlik) {
        if (r > yaricap || yaricap <= 0) {
            return 0;
        }
        double f = r / yaricap;
        return Math.max(1, (int) Math.round(derinlik * (1 - f * f)));
    }

    public static Plan planla(double yaricap, double derinlik, long tohum, int ustTemizlik, boolean blokHasari) {
        if (!blokHasari || yaricap <= 0) {
            return Plan.BOS;
        }
        Random r = new Random(tohum);
        List<Hucre> kazi = new ArrayList<>();
        List<AstarHucresi> astar = new ArrayList<>();
        List<AstarHucresi> kenar = new ArrayList<>();
        int dis = (int) Math.ceil(yaricap + KENAR_GENISLIGI);
        for (int dx = -dis; dx <= dis; dx++) {
            for (int dz = -dis; dz <= dis; dz++) {
                double uzaklik = Math.sqrt(dx * dx + dz * dz);
                int k = derinlik(uzaklik, yaricap, derinlik);
                if (k > 0) {
                    for (int dy = ustTemizlik; dy > -k; dy--) {
                        kazi.add(new Hucre(dx, dy, dz));
                    }
                    Malzeme m = uzaklik <= MAGMA_ORANI * yaricap ? Malzeme.MAGMA : kayac(r);
                    astar.add(new AstarHucresi(dx, -k, dz, m));
                } else if (uzaklik <= yaricap + KENAR_GENISLIGI && r.nextDouble() < KENAR_OLASILIGI) {
                    kenar.add(new AstarHucresi(dx, 1, dz, kayac(r)));
                }
            }
        }
        kazi.sort(Comparator.comparingInt(Hucre::dy).reversed());
        return new Plan(kazi, astar, kenar);
    }

    private static Malzeme kayac(Random r) {
        double z = r.nextDouble();
        return z < 0.4 ? Malzeme.KOYU : z < 0.7 ? Malzeme.ORTA : Malzeme.ACIK;
    }

    /** {@code sertMi} olan hücreler dışındaki kazılacak hücreler (sıra korunur). */
    public static List<Hucre> korunanlarHaric(List<Hucre> hucreler, Predicate<Hucre> sertMi) {
        List<Hucre> sonuc = new ArrayList<>(hucreler.size());
        for (Hucre h : hucreler) {
            if (!sertMi.test(h)) {
                sonuc.add(h);
            }
        }
        return sonuc;
    }
}
