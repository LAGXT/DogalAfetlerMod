package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Kapalı boşluğa sanal su doldurma (bölüm B, mağaralar). İki adım:
 * <ol>
 * <li>Keşif: tohumdan "önce en alçak" öncelikli taşkın (su aşağı akar), en çok {@code maksZiyaret} hücre. Keşif bir
 * {@link Tur#ACIK} hücreye (gökyüzünü gören hava, yüklü olmayan chunk) değerse o hücrenin y'si ağız eşiğidir: su mağara
 * ağzından dışarı akacağı için eşiği aşamaz. Sınıra takılırsa bekleyen en alçak hücre de sızıntı seviyesidir.</li>
 * <li>Dolum: en alçak hücreden başlayıp düz yüzeyle kat kat yükselir; bir kat {@code maksHacim}'i aşacaksa durur.</li>
 * </ol>
 */
public final class MagaraDolumu {
    /** Hücrenin su açısından durumu. */
    public enum Tur {
        /** Su tutabilir (hava ya da geçirgen blok); bloğa dokunulmaz, hücre su içinde çizilir ve suda sayılır. */
        BOS,
        /** Suyu geçirmez (dolu blok, gerçek sıvı). */
        KATI,
        /** Mağaranın dışı (gökyüzünü gören hava, yüklü olmayan chunk): su buraya gelirse akar gider. */
        ACIK
    }

    /** Bloğun kendisinin özü; Minecraft'a bağlı karar {@code SanalSu.blokOzu}'nda, burası saf mantık. */
    public enum BlokOzu {
        HAVA,
        /** Çarpışmasız ya da vanilla'da su tutabilen blok (meşale, ot, liken, sarkıt, ray ...). */
        GECIRGEN,
        KATI
    }

    @FunctionalInterface
    public interface Hucre {
        Tur tur(int x, int y, int z);
    }

    /**
     * Hücre sınıfı. {@code acikYuzeyY}: sütunun yaprak hariç hareket engelleyen yüzeyi ({@code MOTION_BLOCKING_NO_LEAVES});
     * onun hizası ve üstü gökyüzünü görür. Gök ışığı yerine yükseklik kullanılır: yaprak ışığı 1 azaltır, taç altı da açıktır.
     */
    public static Tur sinifla(boolean yuklu, BlokOzu ozu, int y, int acikYuzeyY) {
        if (!yuklu) {
            return Tur.ACIK;
        }
        if (ozu == BlokOzu.KATI) {
            return Tur.KATI;
        }
        return y >= acikYuzeyY ? Tur.ACIK : Tur.BOS;
    }

    private static final int KAYMA = 1 << 20;
    private static final int MASKE = (1 << 21) - 1;

    private MagaraDolumu() {
    }

    private static long paket(int dx, int dy, int dz) {
        return ((long) (dy + KAYMA) << 42) | ((long) (dx + KAYMA) << 21) | (dz + KAYMA);
    }

    private static int dx(long k) {
        return (int) ((k >> 21) & MASKE) - KAYMA;
    }

    private static int dy(long k) {
        return (int) (k >>> 42) - KAYMA;
    }

    private static int dz(long k) {
        return (int) (k & MASKE) - KAYMA;
    }

    private static final int[][] KOMSU = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    public static MagaraCebi doldur(Hucre hucre, int x0, int y0, int z0, int maksHacim, int maksZiyaret) {
        if (maksZiyaret >= KAYMA) {
            throw new IllegalArgumentException("maksZiyaret çok büyük: " + maksZiyaret);
        }
        if (hucre.tur(x0, y0, z0) != Tur.BOS) {
            return MagaraCebi.BOS;
        }
        PriorityQueue<Long> yigin = new PriorityQueue<>();
        Set<Long> gorulen = new HashSet<>();
        Set<Long> kati = new HashSet<>();
        long tohum = paket(0, 0, 0);
        yigin.add(tohum);
        gorulen.add(tohum);
        List<Long> kesif = new ArrayList<>();
        int agiz = Integer.MAX_VALUE;
        while (!yigin.isEmpty() && kesif.size() < maksZiyaret) {
            long k = yigin.poll();
            kesif.add(k);
            int x = dx(k);
            int y = dy(k);
            int z = dz(k);
            for (int[] d : KOMSU) {
                int nx = x + d[0];
                int ny = y + d[1];
                int nz = z + d[2];
                if (Math.abs(nx) >= KAYMA || Math.abs(ny) >= KAYMA || Math.abs(nz) >= KAYMA) {
                    continue;
                }
                long nk = paket(nx, ny, nz);
                if (gorulen.contains(nk) || kati.contains(nk)) {
                    continue;
                }
                Tur tur = hucre.tur(x0 + nx, y0 + ny, z0 + nz);
                if (tur == Tur.BOS) {
                    gorulen.add(nk);
                    yigin.add(nk);
                } else {
                    kati.add(nk);
                    if (tur == Tur.ACIK) {
                        agiz = Math.min(agiz, ny);
                    }
                }
            }
        }
        boolean acik = !yigin.isEmpty();
        Set<Long> bolge = new HashSet<>(kesif);
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        long enAlcak = tohum;
        for (long k : kesif) {
            int y = dy(k);
            if (y < minY) {
                minY = y;
                enAlcak = k;
            }
            maxY = Math.max(maxY, y);
        }
        int ustSinir = Math.min(maxY + 1, agiz);
        if (acik) {
            ustSinir = Math.min(ustSinir, dy(yigin.peek()));
        }
        int agizY = agiz == Integer.MAX_VALUE ? Integer.MAX_VALUE : y0 + agiz;
        // Kat kat dolum: her hücre eklendiği seviyeyle işaretlenir; hacmi aşan kat geri alınmaz, dolum orada durur.
        Map<Long, Integer> eklenme = new HashMap<>();
        ArrayDeque<Long> is = new ArrayDeque<>();
        List<Long> bekleyen = new ArrayList<>();
        bekleyen.add(enAlcak);
        int seviye = minY;
        for (int l = minY + 1; l <= ustSinir; l++) {
            List<Long> kalan = new ArrayList<>();
            for (long k : bekleyen) {
                if (eklenme.containsKey(k)) {
                    continue;
                }
                if (dy(k) < l) {
                    eklenme.put(k, l);
                    is.add(k);
                } else {
                    kalan.add(k);
                }
            }
            bekleyen = kalan;
            while (!is.isEmpty()) {
                long k = is.poll();
                int x = dx(k);
                int y = dy(k);
                int z = dz(k);
                for (int[] d : KOMSU) {
                    long nk = paket(x + d[0], y + d[1], z + d[2]);
                    if (!bolge.contains(nk) || eklenme.containsKey(nk)) {
                        continue;
                    }
                    if (y + d[1] < l) {
                        eklenme.put(nk, l);
                        is.add(nk);
                    } else {
                        bekleyen.add(nk);
                    }
                }
            }
            if (eklenme.size() > maksHacim) {
                break;
            }
            seviye = l;
        }
        Map<Long, List<Integer>> sutunY = new HashMap<>();
        int hacim = 0;
        int dolMinY = Integer.MAX_VALUE;
        for (Map.Entry<Long, Integer> e : eklenme.entrySet()) {
            if (e.getValue() > seviye) {
                continue;
            }
            long k = e.getKey();
            int x = x0 + dx(k);
            int y = y0 + dy(k);
            int z = z0 + dz(k);
            sutunY.computeIfAbsent(MagaraCebi.anahtar(x, z), a -> new ArrayList<>()).add(y);
            hacim++;
            dolMinY = Math.min(dolMinY, y);
        }
        if (hacim == 0) {
            return new MagaraCebi(new HashMap<>(), 0, 0, 0, kesif.size(), acik, agizY);
        }
        Map<Long, int[]> sutunlar = sutunlaraCevir(sutunY);
        return new MagaraCebi(sutunlar, hacim, dolMinY, y0 + seviye, kesif.size(), acik, agizY);
    }

    /** Sütun başına y listelerini sıralı {@code [y0, y1)} çiftlerine çevirir. */
    private static Map<Long, int[]> sutunlaraCevir(Map<Long, List<Integer>> sutunY) {
        Map<Long, int[]> sutunlar = new HashMap<>();
        for (Map.Entry<Long, List<Integer>> e : sutunY.entrySet()) {
            List<Integer> ys = e.getValue();
            ys.sort(null);
            List<Integer> r = new ArrayList<>();
            int bas = ys.get(0);
            int onceki = bas;
            for (int i = 1; i < ys.size(); i++) {
                int y = ys.get(i);
                if (y != onceki + 1) {
                    r.add(bas);
                    r.add(onceki + 1);
                    bas = y;
                }
                onceki = y;
            }
            r.add(bas);
            r.add(onceki + 1);
            int[] dizi = new int[r.size()];
            for (int i = 0; i < dizi.length; i++) {
                dizi[i] = r.get(i);
            }
            sutunlar.put(e.getKey(), dizi);
        }
        return sutunlar;
    }

    /**
     * Kaydedilmiş bir cebi yeniden kurar (SU-24): {@code (x0, y0, z0)} cebin en alçak hücresinden ({@link MagaraCebi#enAlcak})
     * yalnız {@code ustY}'nin altındaki boş hücrelere taşar. Dünya aynıysa {@link #doldur} ile aynı hücreleri verir (keşif
     * en alçak önce olduğu için dolu kümenin {@code ustY} altındaki komşuları zaten dolu kümededir). {@code maksHacim}
     * aşılırsa ya da tohum boş değilse (dünya değişti) boş cep döner.
     */
    public static MagaraCebi seviyeyeKadar(Hucre hucre, int x0, int y0, int z0, int ustY, int agizY, int maksHacim) {
        if (y0 >= ustY || hucre.tur(x0, y0, z0) != Tur.BOS) {
            return MagaraCebi.BOS;
        }
        Set<Long> dolu = new HashSet<>();
        ArrayDeque<Long> kuyruk = new ArrayDeque<>();
        long tohum = paket(0, 0, 0);
        dolu.add(tohum);
        kuyruk.add(tohum);
        while (!kuyruk.isEmpty()) {
            long k = kuyruk.poll();
            int x = dx(k);
            int y = dy(k);
            int z = dz(k);
            for (int[] d : KOMSU) {
                int ny = y + d[1];
                if (y0 + ny >= ustY || Math.abs(x + d[0]) >= KAYMA || Math.abs(ny) >= KAYMA || Math.abs(z + d[2]) >= KAYMA) {
                    continue;
                }
                long nk = paket(x + d[0], ny, z + d[2]);
                if (dolu.contains(nk) || hucre.tur(x0 + x + d[0], y0 + ny, z0 + z + d[2]) != Tur.BOS) {
                    continue;
                }
                if (dolu.size() >= maksHacim) {
                    return MagaraCebi.BOS;
                }
                dolu.add(nk);
                kuyruk.add(nk);
            }
        }
        Map<Long, List<Integer>> sutunY = new HashMap<>();
        int minY = Integer.MAX_VALUE;
        for (long k : dolu) {
            int y = y0 + dy(k);
            sutunY.computeIfAbsent(MagaraCebi.anahtar(x0 + dx(k), z0 + dz(k)), a -> new ArrayList<>()).add(y);
            minY = Math.min(minY, y);
        }
        return new MagaraCebi(sutunlaraCevir(sutunY), dolu.size(), minY, ustY, dolu.size(), false, agizY);
    }
}
