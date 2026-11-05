package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Deniz seviyesi yükselmesinin alanı (bölüm H). Yalnız istenen (yüklü) chunk'ların sütun bilgisi önbellekte tutulur:
 * zemin (kara üstü ya da gerçek su yüzeyi), gerçek su olup olmadığı ve deniz dibi. Gerçek su sütunlarından başlayan çok
 * kaynaklı BFS her sütuna bir "kaynak yüzeyi" ve kaynağa uzaklık yazar; yükselme {@code artis} iken sütunun sanal suyu
 * {@code [zemin, kaynak + artis)} olur. Diskte ve ağda yalnız {@code artis} gider; alan her tarafta yerel hesaplanır.
 *
 * <p>Kurallar: komşuya yayılma {@code zemin < kaynak + maksArtis} (su o bloğu aşamaz) ve
 * {@code zemin >= kaynak - tolerans} (gölün altına inen yamaçta su havada asılı kalmaz); en çok {@code maksMesafe} adım.
 * Aynı mesafede birden çok kaynak varsa yüksek yüzey kazanır; sonuç chunk okuma sırasından bağımsızdır.
 *
 * <p>İş parçacıkları: {@link #iste}, {@link #isle}, okuyucular sahibi iş parçacığında (sunucu ya da render) çağrılır.
 * {@link #yurutucu(Executor)} verilirse BFS çağıranda alınan anlık görüntü üzerinde arka planda koşar ve sonuç dizileri
 * tek atamayla yayınlanır; okuyucular o ana kadar eski sonucu görür.
 */
public final class DenizSeviyesiAlani {
    /** Bir chunk'ın 256 sütununu ({@code indeks = lx + lz * 16}) doldurur; chunk yüklü değilse {@code false}. */
    @FunctionalInterface
    public interface SutunOkuyucu {
        boolean oku(int cx, int cz, int[] zemin, boolean[] su, int[] taban);
    }

    private static final class Parca {
        final int cx;
        final int cz;
        final int[] zemin = new int[256];
        final boolean[] su = new boolean[256];
        final int[] taban = new int[256];
        volatile float[] kaynak = bosKaynak();
        volatile int[] mesafe = bosMesafe();
        boolean okundu;
        boolean kuyrukta;

        Parca(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }
    }

    private static float[] bosKaynak() {
        float[] k = new float[256];
        Arrays.fill(k, Float.NaN);
        return k;
    }

    private static int[] bosMesafe() {
        int[] m = new int[256];
        Arrays.fill(m, -1);
        return m;
    }

    private final int maksArtis;
    private final int maksMesafe;
    private final int tolerans;
    private final Map<Long, Parca> parcalar = new HashMap<>();
    private final ArrayDeque<Parca> kuyruk = new ArrayDeque<>();
    private final AtomicBoolean hesapta = new AtomicBoolean();
    private Executor yurutucu;
    private Parca onbellek;
    private boolean kirli;
    private volatile long sonHesapNs;
    private volatile int sonSutunSayisi;
    private volatile int hesapSayisi;
    private int degisenOkuma;
    private final int[] okuZemin = new int[256];
    private final boolean[] okuSu = new boolean[256];
    private final int[] okuTaban = new int[256];

    public DenizSeviyesiAlani(int maksArtis, int maksMesafe, int tolerans) {
        this.maksArtis = maksArtis;
        this.maksMesafe = maksMesafe;
        this.tolerans = tolerans;
    }

    /** BFS'in koşacağı yürütücü; {@code null} ise {@link #isle} içinde eşzamanlı. */
    public void yurutucu(Executor y) {
        this.yurutucu = y;
    }

    public boolean hesapSuruyor() {
        return hesapta.get();
    }

    /** Tamamlanmış BFS sayısı (log ve test için). */
    public int hesapSayisi() {
        return hesapSayisi;
    }

    /** İlk okuma ya da verisi değişmiş yeniden okuma sayısı (BFS'i yalnız bunlar tetikler). */
    public int degisenOkumaSayisi() {
        return degisenOkuma;
    }

    public static long anahtar(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xffffffffL);
    }

    public void iste(int cx, int cz) {
        long k = anahtar(cx, cz);
        if (!parcalar.containsKey(k)) {
            Parca p = new Parca(cx, cz);
            parcalar.put(k, p);
            p.kuyrukta = true;
            kuyruk.add(p);
        }
    }

    public void birak(long anahtar) {
        Parca p = parcalar.remove(anahtar);
        if (p != null) {
            if (onbellek == p) {
                onbellek = null;
            }
            if (p.okundu) {
                kirli = true;
            }
        }
    }

    public long[] istenenler() {
        long[] sonuc = new long[parcalar.size()];
        int i = 0;
        for (long k : parcalar.keySet()) {
            sonuc[i++] = k;
        }
        return sonuc;
    }

    /**
     * Chunk yeniden okunmak üzere sıraya girer (blok değişimi ya da periyodik tazeleme); yeni veri gelene kadar eski
     * değerler kullanılır. Okunan veri öncekiyle aynıysa BFS yeniden koşmaz, bu yüzden gereksiz çağrı ucuzdur.
     */
    public void gecersizKil(int cx, int cz) {
        Parca p = parcalar.get(anahtar(cx, cz));
        if (p != null && !p.kuyrukta) {
            p.kuyrukta = true;
            kuyruk.add(p);
        }
    }

    public int bekleyen() {
        return kuyruk.size();
    }

    public int parcaSayisi() {
        return parcalar.size();
    }

    public long sonHesapNs() {
        return sonHesapNs;
    }

    public int sonSutunSayisi() {
        return sonSutunSayisi;
    }

    /**
     * En çok {@code butce} chunk okur; verisi değişen chunk varsa ve önceki hesap bitmişse BFS'i başlatır. Okunan chunk
     * sayısını döner.
     */
    public int isle(SutunOkuyucu okuyucu, int butce) {
        int okunan = 0;
        int deneme = kuyruk.size();
        while (okunan < butce && deneme-- > 0 && !kuyruk.isEmpty()) {
            Parca p = kuyruk.poll();
            if (parcalar.get(anahtar(p.cx, p.cz)) != p) {
                continue;
            }
            if (okuyucu.oku(p.cx, p.cz, okuZemin, okuSu, okuTaban)) {
                boolean degisti = !p.okundu || !Arrays.equals(okuZemin, p.zemin) || !Arrays.equals(okuSu, p.su)
                        || !Arrays.equals(okuTaban, p.taban);
                if (degisti) {
                    System.arraycopy(okuZemin, 0, p.zemin, 0, 256);
                    System.arraycopy(okuSu, 0, p.su, 0, 256);
                    System.arraycopy(okuTaban, 0, p.taban, 0, 256);
                    degisenOkuma++;
                    kirli = true;
                }
                p.okundu = true;
                p.kuyrukta = false;
                okunan++;
            } else {
                kuyruk.add(p);
            }
        }
        if (kirli && !hesapta.get()) {
            kirli = false;
            Runnable is = hesapIsi();
            Executor y = yurutucu;
            if (y == null) {
                is.run();
            } else {
                hesapta.set(true);
                y.execute(() -> {
                    try {
                        is.run();
                    } finally {
                        hesapta.set(false);
                    }
                });
            }
        }
        return okunan;
    }

    /** Bütün önbellek üzerinde çok kaynaklı BFS (eşzamanlı). */
    public void hesapla() {
        kirli = false;
        hesapIsi().run();
    }

    /** Anlık görüntüyü çağıranda alır; dönen iş herhangi bir iş parçacığında koşup sonucu yayınlar. */
    private Runnable hesapIsi() {
        List<Parca> liste = new ArrayList<>();
        for (Parca p : parcalar.values()) {
            if (p.okundu) {
                liste.add(p);
            }
        }
        int n = liste.size();
        int[][] zeminK = new int[n][];
        boolean[][] suK = new boolean[n][];
        Map<Long, Integer> indeks = new HashMap<>(n * 2);
        for (int i = 0; i < n; i++) {
            Parca p = liste.get(i);
            zeminK[i] = p.zemin.clone();
            suK[i] = p.su.clone();
            indeks.put(anahtar(p.cx, p.cz), i);
        }
        int[] pcx = new int[n];
        int[] pcz = new int[n];
        for (int i = 0; i < n; i++) {
            pcx[i] = liste.get(i).cx;
            pcz[i] = liste.get(i).cz;
        }
        return () -> {
            long t0 = System.nanoTime();
            float[][] kaynak = new float[n][256];
            int[][] mesafe = new int[n][256];
            for (int i = 0; i < n; i++) {
                Arrays.fill(kaynak[i], Float.NaN);
                Arrays.fill(mesafe[i], -1);
            }
            int toplam = n * 256;
            long[] sira = new long[toplam];
            int s = 0;
            for (int i = 0; i < n; i++) {
                for (int c = 0; c < 256; c++) {
                    if (suK[i][c]) {
                        // Yüksek yüzey önce: üst yarı (2048 - zemin) küçükten büyüğe; alt yarı parça*256 + sütun.
                        sira[s++] = ((long) (2048 - zeminK[i][c]) << 32) | (i * 256L + c);
                    }
                }
            }
            Arrays.sort(sira, 0, s);
            int[] q = new int[Math.max(16, toplam)];
            int bas = 0;
            int son = 0;
            for (int j = 0; j < s; j++) {
                int v = (int) sira[j];
                int i = v >> 8;
                int c = v & 255;
                kaynak[i][c] = zeminK[i][c];
                mesafe[i][c] = 0;
                q[son++] = v;
            }
            while (bas < son) {
                int v = q[bas++];
                int i = v >> 8;
                int c = v & 255;
                float k = kaynak[i][c];
                int d = mesafe[i][c];
                if (d >= maksMesafe) {
                    continue;
                }
                int x = pcx[i] * 16 + (c & 15);
                int z = pcz[i] * 16 + (c >> 4);
                for (int yon = 0; yon < 4; yon++) {
                    int nx = x + (yon == 0 ? 1 : yon == 1 ? -1 : 0);
                    int nz = z + (yon == 2 ? 1 : yon == 3 ? -1 : 0);
                    int ni;
                    if ((nx >> 4) == pcx[i] && (nz >> 4) == pcz[i]) {
                        ni = i;
                    } else {
                        Integer o = indeks.get(anahtar(nx >> 4, nz >> 4));
                        if (o == null) {
                            continue;
                        }
                        ni = o;
                    }
                    int nc = (nx & 15) + ((nz & 15) << 4);
                    if (mesafe[ni][nc] >= 0) {
                        continue;
                    }
                    int zn = zeminK[ni][nc];
                    if (zn < k + maksArtis && zn >= k - tolerans) {
                        kaynak[ni][nc] = k;
                        mesafe[ni][nc] = d + 1;
                        q[son++] = ni * 256 + nc;
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                Parca p = liste.get(i);
                p.mesafe = mesafe[i];
                p.kaynak = kaynak[i];
            }
            sonSutunSayisi = toplam;
            sonHesapNs = System.nanoTime() - t0;
            hesapSayisi++;
        };
    }

    private Parca parca(int x, int z) {
        int cx = x >> 4;
        int cz = z >> 4;
        Parca s = onbellek;
        if (s != null && s.cx == cx && s.cz == cz) {
            return s;
        }
        s = parcalar.get(anahtar(cx, cz));
        if (s != null) {
            onbellek = s;
        }
        return s;
    }

    private Parca okunmus(int x, int z) {
        Parca p = parca(x, z);
        return p != null && p.okundu ? p : null;
    }

    public boolean yuklu(int x, int z) {
        return okunmus(x, z) != null;
    }

    public float kaynak(int x, int z) {
        Parca p = okunmus(x, z);
        return p == null ? Float.NaN : p.kaynak[(x & 15) + ((z & 15) << 4)];
    }

    public int mesafe(int x, int z) {
        Parca p = okunmus(x, z);
        return p == null ? -1 : p.mesafe[(x & 15) + ((z & 15) << 4)];
    }

    /** Kara üstü ya da gerçek su yüzeyi; bilinmiyorsa {@link Integer#MIN_VALUE}. */
    public int zemin(int x, int z) {
        Parca p = okunmus(x, z);
        return p == null ? Integer.MIN_VALUE : p.zemin[(x & 15) + ((z & 15) << 4)];
    }

    public boolean su(int x, int z) {
        Parca p = okunmus(x, z);
        return p != null && p.su[(x & 15) + ((z & 15) << 4)];
    }

    /** Gerçek su sütununda dip (ilk su bloğunun altı); karada zemin. */
    public int taban(int x, int z) {
        Parca p = okunmus(x, z);
        return p == null ? Integer.MIN_VALUE : p.taban[(x & 15) + ((z & 15) << 4)];
    }

    /** Yükselme {@code artis} iken sütunun sanal su aralığını ekler. */
    public void aralikEkle(int x, int z, double artis, SuAraliklari a) {
        if (artis <= 0) {
            return;
        }
        Parca p = okunmus(x, z);
        if (p == null) {
            return;
        }
        int i = (x & 15) + ((z & 15) << 4);
        float k = p.kaynak[i];
        if (Float.isNaN(k)) {
            return;
        }
        a.ekle(p.zemin[i], k + artis);
    }
}
