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
 * Büyük çarpmada ({@code cokme > 0}) merkez ayrıca çöküntü çukuruna iner ({@link #toplamDerinlik}) ve çanağın ötesinde, {@code
 * kirilmaYaricapi}'na kadar yüzey blokları kırılır (uzaklaştıkça seyrelen halka) ve birkaç göçük çukuru açılır; bunlar
 * {@link KirikHucresi} listesindedir (yüzey çarpma anında aranır, arazi eğimli olabilir).
 * Sert blokları eleme ({@link #korunanlarHaric}) uygulama anında yapılır; afet blok hasarı kapalıysa plan boştur.
 */
public final class KraterProfili {
    public static final double KENAR_GENISLIGI = 1.5;
    /** Astarın magma olan merkez oranı (r ≤ oran · R). */
    public static final double MAGMA_ORANI = 0.4;
    /** Çöküntü çukurunun yarıçap oranı (r ≤ oran · R) ve ek derinliğinin krater derinliğine oranı (cokme = 1'de). */
    public static final double CUKUR_ORANI = 0.4;
    public static final double CUKUR_DERINLIGI_ORANI = 0.8;
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

    /**
     * Kırılan yüzey sütunu: {@code (dx, dz)} sütununun en üstteki bloğundan başlayarak {@code kalinlik} blok kırılır; {@code
     * taban} {@link Malzeme#MAGMA} ise kırılan sütunun altındaki ilk katı blok sıcak magma olur (null: dokunulmaz).
     */
    public record KirikHucresi(int dx, int dz, int kalinlik, Malzeme taban) {
    }

    public record Plan(List<Hucre> kazi, List<AstarHucresi> astar, List<AstarHucresi> kenar, List<KirikHucresi> kirik) {
        public static final Plan BOS = new Plan(List.of(), List.of(), List.of(), List.of());

        public Plan {
            kazi = List.copyOf(kazi);
            astar = List.copyOf(astar);
            kenar = List.copyOf(kenar);
            kirik = List.copyOf(kirik);
        }

        public Plan(List<Hucre> kazi, List<AstarHucresi> astar, List<AstarHucresi> kenar) {
            this(kazi, astar, kenar, List.of());
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

    /** {@link #derinlik} artı merkez çöküntü çukurunun ek derinliği ({@code cokme} 0–1; 0'da ek yok). */
    public static int toplamDerinlik(double r, double yaricap, double derinlik, double cokme) {
        int k = derinlik(r, yaricap, derinlik);
        if (k == 0 || cokme <= 0) {
            return k;
        }
        double cukurYaricapi = CUKUR_ORANI * yaricap;
        if (r >= cukurYaricapi) {
            return k;
        }
        double f = r / cukurYaricapi;
        return k + (int) Math.round(cokme * CUKUR_DERINLIGI_ORANI * derinlik * (1 - f * f));
    }

    public static Plan planla(double yaricap, double derinlik, long tohum, int ustTemizlik, boolean blokHasari) {
        return planla(yaricap, derinlik, tohum, ustTemizlik, blokHasari, 0, 0);
    }

    /**
     * @param kirilmaYaricapi yüzey kırılmasının dış yarıçapı (çanak + kenar halkasından küçükse kırılma yok)
     * @param cokme çöküntü şiddeti 0–1 (0: çukur, kalın kırılma ve göçük yok)
     */
    public static Plan planla(double yaricap, double derinlik, long tohum, int ustTemizlik, boolean blokHasari,
            double kirilmaYaricapi, double cokme) {
        if (!blokHasari || yaricap <= 0) {
            return Plan.BOS;
        }
        double cokmeK = Math.max(0, Math.min(1, cokme));
        Random r = new Random(tohum);
        List<Hucre> kazi = new ArrayList<>();
        List<AstarHucresi> astar = new ArrayList<>();
        List<AstarHucresi> kenar = new ArrayList<>();
        int dis = (int) Math.ceil(yaricap + KENAR_GENISLIGI);
        for (int dx = -dis; dx <= dis; dx++) {
            for (int dz = -dis; dz <= dis; dz++) {
                double uzaklik = Math.sqrt(dx * dx + dz * dz);
                int k = toplamDerinlik(uzaklik, yaricap, derinlik, cokmeK);
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
        return new Plan(kazi, astar, kenar, kirik(yaricap, kirilmaYaricapi, cokmeK, r));
    }

    /** Çanak ve kenar halkasının dışında kırılan yüzey sütunları ile göçük çukurları (merkeze yakından uzağa sıralı). */
    private static List<KirikHucresi> kirik(double yaricap, double kirilmaYaricapi, double cokme, Random r) {
        double ic = yaricap + KENAR_GENISLIGI;
        if (kirilmaYaricapi <= ic) {
            return List.of();
        }
        int dis = (int) Math.ceil(kirilmaYaricapi);
        int boy = 2 * dis + 1;
        int[][] kalinlik = new int[boy][boy];
        int enKalin = 1 + (int) Math.round(cokme * 2);
        for (int dx = -dis; dx <= dis; dx++) {
            for (int dz = -dis; dz <= dis; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= ic || d > kirilmaYaricapi) {
                    continue;
                }
                double f = 1 - (d - ic) / (kirilmaYaricapi - ic);
                if (r.nextDouble() < 0.1 + 0.65 * f) {
                    int t = 1;
                    if (enKalin > 1 && r.nextDouble() < f) {
                        t += r.nextInt(enKalin);
                    }
                    kalinlik[dx + dis][dz + dis] = t;
                }
            }
        }
        int cukurSayisi = (int) Math.round(cokme * (3 + 0.6 * yaricap));
        for (int i = 0; i < cukurSayisi; i++) {
            double aci = r.nextDouble() * 2 * Math.PI;
            double mesafe = ic + r.nextDouble() * (kirilmaYaricapi - ic) * 0.9;
            int cx = (int) Math.round(Math.cos(aci) * mesafe);
            int cz = (int) Math.round(Math.sin(aci) * mesafe);
            int cukurYaricapi = 1 + r.nextInt(2);
            int cukurDerinligi = 2 + r.nextInt(2 + (int) Math.round(cokme * 2));
            for (int ox = -cukurYaricapi; ox <= cukurYaricapi; ox++) {
                for (int oz = -cukurYaricapi; oz <= cukurYaricapi; oz++) {
                    int ud = ox * ox + oz * oz;
                    int x = cx + ox;
                    int z = cz + oz;
                    if (ud > cukurYaricapi * cukurYaricapi || Math.abs(x) > dis || Math.abs(z) > dis
                            || Math.sqrt(x * x + z * z) <= ic) {
                        continue;
                    }
                    int t = Math.max(1, cukurDerinligi - (ud > 0 ? 1 : 0));
                    kalinlik[x + dis][z + dis] = Math.max(kalinlik[x + dis][z + dis], t);
                }
            }
        }
        List<KirikHucresi> liste = new ArrayList<>();
        for (int dx = -dis; dx <= dis; dx++) {
            for (int dz = -dis; dz <= dis; dz++) {
                int t = kalinlik[dx + dis][dz + dis];
                if (t > 0) {
                    boolean lav = cokme > 0 && t >= 2 && r.nextDouble() < 0.25;
                    liste.add(new KirikHucresi(dx, dz, t, lav ? Malzeme.MAGMA : null));
                }
            }
        }
        liste.sort(Comparator.comparingInt(h -> h.dx() * h.dx() + h.dz() * h.dz()));
        return liste;
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
