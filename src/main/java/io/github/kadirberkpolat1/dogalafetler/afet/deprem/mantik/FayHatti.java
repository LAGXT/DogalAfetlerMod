package io.github.kadirberkpolat1.dogalafetler.afet.deprem.mantik;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

/**
 * Deprem fay hattının planı (tasarım notu deprem §2): merkezden iki uca açılan, 8–16 blokluk segmentlerden oluşan ve
 * segment uçlarında 1–3 blok yana sıçrayan (kademeli, en échelon) çoklu çizgi. Genişlik ve derinlik merkezde tam, uçlara
 * doğru {@code sin(π·u)^0.7} ile incelir; her sütunun derinliği ±1 pürüzlüdür. Seviye 2'den itibaren hattın iki yanında 45°
 * açılı kısa yan çatlaklar, seviye 4–5'te hattın bir yakasında 1–2 blokluk fay basamağı vardır. Aynı tohum aynı planı verir.
 * Saf Java'dır.
 */
public final class FayHatti {
    /** Bir yarık sütunu: yatay konum, yüzeyden derinlik (blok) ve açılma sırası ({@code yol}: merkezden hat boyu, blok). */
    public record Hucre(int x, int z, int derinlik, double yol) {
    }

    /** Plan: ana yarık, yan çatlaklar ve basamak (yüzeyi {@code derinlik} kadar alçalan sütunlar); hepsi açılma sırasında. */
    public record Plan(List<Hucre> yarik, List<Hucre> yan, List<Hucre> basamak, double yarimBoy) {
        public int toplam() {
            return yarik.size() + yan.size() + basamak.size();
        }
    }

    private FayHatti() {
    }

    /** Profil çarpanı: hattın göreli konumu {@code u} (0–1) için {@code sin(π·u)^0.7}. */
    public static double profil(double u) {
        double s = Math.sin(Math.PI * Math.max(0, Math.min(1, u)));
        return Math.pow(Math.max(0, s), 0.7);
    }

    public static Plan olustur(int merkezX, int merkezZ, double aci, DepremSeviyesi s, long tohum) {
        SplittableRandom r = new SplittableRandom(tohum);
        double dx = Math.cos(aci);
        double dz = Math.sin(aci);
        double px = -dz;
        double pz = dx;
        double yarim = s.uzunluk() / 2.0;
        double[] artiKayma = kaymalar(r, yarim);
        double[] eksiKayma = kaymalar(r, yarim);
        Map<Long, Hucre> yarik = new LinkedHashMap<>();
        for (double t = -yarim; t <= yarim; t += 0.5) {
            double u = (t + yarim) / s.uzunluk();
            double f = profil(u);
            int gen = Math.max(1, (int) Math.round(s.genislik() * f));
            int der = Math.max(1, (int) Math.round(s.derinlik() * Math.max(0.15, f)));
            double kayma = t >= 0 ? artiKayma[(int) Math.min(artiKayma.length - 1, t)]
                    : eksiKayma[(int) Math.min(eksiKayma.length - 1, -t)];
            for (int k = 0; k < gen; k++) {
                double yan = kayma + k - (gen - 1) / 2.0;
                int x = (int) Math.floor(merkezX + 0.5 + dx * t + px * yan);
                int z = (int) Math.floor(merkezZ + 0.5 + dz * t + pz * yan);
                boolean kenar = gen > 1 && (k == 0 || k == gen - 1);
                int d = Math.max(1, der - (kenar ? 1 : 0) + r.nextInt(3) - 1);
                ekle(yarik, new Hucre(x, z, d, Math.abs(t)));
            }
        }
        Map<Long, Hucre> yan = new LinkedHashMap<>();
        if (s.seviye() >= 2) {
            for (double t = -yarim + 4; t <= yarim - 4; t += 6 + r.nextInt(5)) {
                double taraf = r.nextBoolean() ? 1 : -1;
                double uzak = (s.genislik() / 2.0) + 2 + r.nextInt(3);
                double cx = merkezX + 0.5 + dx * t + px * uzak * taraf;
                double cz = merkezZ + 0.5 + dz * t + pz * uzak * taraf;
                double ya = aci + Math.PI / 4 * (r.nextBoolean() ? 1 : -1);
                int boy = 3 + r.nextInt(3);
                for (double k = -boy / 2.0; k <= boy / 2.0; k += 0.5) {
                    int x = (int) Math.floor(cx + Math.cos(ya) * k);
                    int z = (int) Math.floor(cz + Math.sin(ya) * k);
                    long a = anahtar(x, z);
                    if (!yarik.containsKey(a)) {
                        ekle(yan, new Hucre(x, z, 1 + r.nextInt(2), Math.abs(t)));
                    }
                }
            }
        }
        Map<Long, Hucre> basamak = new LinkedHashMap<>();
        if (s.seviye() >= 4) {
            int dusus = s.seviye() >= 5 ? 2 : 1;
            double ic = s.genislik() / 2.0 + 1;
            for (double t = -yarim * 0.8; t <= yarim * 0.8; t += 0.5) {
                double kayma = t >= 0 ? artiKayma[(int) Math.min(artiKayma.length - 1, t)]
                        : eksiKayma[(int) Math.min(eksiKayma.length - 1, -t)];
                for (double y = ic; y <= ic + 5; y += 0.5) {
                    int x = (int) Math.floor(merkezX + 0.5 + dx * t + px * (kayma + y));
                    int z = (int) Math.floor(merkezZ + 0.5 + dz * t + pz * (kayma + y));
                    long a = anahtar(x, z);
                    if (!yarik.containsKey(a) && !yan.containsKey(a)) {
                        ekle(basamak, new Hucre(x, z, dusus, Math.abs(t)));
                    }
                }
            }
        }
        return new Plan(sirala(yarik), sirala(yan), sirala(basamak), yarim);
    }

    /** Her tam blok için yanal kayma: segment (8–16 blok) sonunda 1–3 blok sıçrar, yön rastgele. */
    private static double[] kaymalar(SplittableRandom r, double yarim) {
        double[] kayma = new double[(int) Math.ceil(yarim) + 1];
        int sonraki = 8 + r.nextInt(9);
        double deger = 0;
        for (int i = 0; i < kayma.length; i++) {
            if (i == sonraki) {
                deger += (1 + r.nextInt(3)) * (r.nextBoolean() ? 1 : -1);
                sonraki += 8 + r.nextInt(9);
            }
            kayma[i] = deger;
        }
        return kayma;
    }

    private static void ekle(Map<Long, Hucre> hedef, Hucre h) {
        hedef.merge(anahtar(h.x(), h.z()), h, (a, b) -> new Hucre(a.x(), a.z(), Math.max(a.derinlik(), b.derinlik()),
                Math.min(a.yol(), b.yol())));
    }

    private static List<Hucre> sirala(Map<Long, Hucre> hucreler) {
        List<Hucre> liste = new ArrayList<>(hucreler.values());
        liste.sort(Comparator.comparingDouble(Hucre::yol).thenComparingInt(Hucre::x).thenComparingInt(Hucre::z));
        return List.copyOf(liste);
    }

    public static long anahtar(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }
}
