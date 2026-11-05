package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Deniz seviyesi yükselmesinin alanı (bölüm H). Yalnız istenen (yüklü) chunk'ların sütun bilgisi önbellekte tutulur:
 * zemin (kara üstü ya da gerçek su yüzeyi), gerçek su olup olmadığı ve deniz dibi. Gerçek su sütunlarından başlayan çok
 * kaynaklı BFS her sütuna bir "kaynak yüzeyi" ve kaynağa uzaklık yazar; yükselme {@code artis} iken sütunun sanal suyu
 * {@code [zemin, kaynak + artis)} olur. Diskte ve ağda yalnız {@code artis} gider; alan her tarafta yerel hesaplanır.
 *
 * <p>Kurallar: komşuya yayılma {@code zemin < kaynak + maksArtis} (su o bloğu aşamaz) ve
 * {@code zemin >= kaynak - tolerans} (gölün altına inen yamaçta su havada asılı kalmaz); en çok {@code maksMesafe} adım.
 * Aynı mesafede birden çok kaynak varsa yüksek yüzey kazanır; sonuç chunk okuma sırasından bağımsızdır.
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
        final float[] kaynak = new float[256];
        final int[] mesafe = new int[256];
        boolean okundu;
        boolean kuyrukta;

        Parca(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
            Arrays.fill(kaynak, Float.NaN);
            Arrays.fill(mesafe, -1);
        }
    }

    private final int maksArtis;
    private final int maksMesafe;
    private final int tolerans;
    private final Map<Long, Parca> parcalar = new HashMap<>();
    private final ArrayDeque<Parca> kuyruk = new ArrayDeque<>();
    private Parca onbellek;
    private boolean kirli;
    private long sonHesapNs;
    private int sonSutunSayisi;

    public DenizSeviyesiAlani(int maksArtis, int maksMesafe, int tolerans) {
        this.maksArtis = maksArtis;
        this.maksMesafe = maksMesafe;
        this.tolerans = tolerans;
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

    /** Blok değişimi: chunk yeniden okunur; yeni veri gelene kadar eski değerler kullanılır. */
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

    /** En çok {@code butce} chunk okur; bir şey değiştiyse alanı yeniden hesaplar. Okunan chunk sayısını döner. */
    public int isle(SutunOkuyucu okuyucu, int butce) {
        int okunan = 0;
        int deneme = kuyruk.size();
        while (okunan < butce && deneme-- > 0 && !kuyruk.isEmpty()) {
            Parca p = kuyruk.poll();
            if (parcalar.get(anahtar(p.cx, p.cz)) != p) {
                continue;
            }
            if (okuyucu.oku(p.cx, p.cz, p.zemin, p.su, p.taban)) {
                p.okundu = true;
                p.kuyrukta = false;
                okunan++;
                kirli = true;
            } else {
                kuyruk.add(p);
            }
        }
        if (kirli) {
            hesapla();
        }
        return okunan;
    }

    /** Bütün önbellek üzerinde çok kaynaklı BFS. */
    public void hesapla() {
        kirli = false;
        long t0 = System.nanoTime();
        int toplam = 0;
        for (Parca p : parcalar.values()) {
            Arrays.fill(p.kaynak, Float.NaN);
            Arrays.fill(p.mesafe, -1);
            if (p.okundu) {
                toplam += 256;
            }
        }
        long[] sira = new long[toplam];
        int[] kx = new int[toplam];
        int[] kz = new int[toplam];
        int n = 0;
        for (Parca p : parcalar.values()) {
            if (!p.okundu) {
                continue;
            }
            for (int i = 0; i < 256; i++) {
                if (!p.su[i]) {
                    continue;
                }
                kx[n] = p.cx * 16 + (i & 15);
                kz[n] = p.cz * 16 + (i >> 4);
                // Yüksek yüzey önce: anahtarın üst yarısı (2048 - zemin) küçükten büyüğe sıralanır.
                sira[n] = ((long) (2048 - p.zemin[i]) << 32) | n;
                n++;
            }
        }
        Arrays.sort(sira, 0, n);
        long[] q = new long[Math.max(16, toplam)];
        int bas = 0;
        int son = 0;
        for (int j = 0; j < n; j++) {
            int idx = (int) sira[j];
            int x = kx[idx];
            int z = kz[idx];
            Parca p = parca(x, z);
            int i = (x & 15) + ((z & 15) << 4);
            p.kaynak[i] = p.zemin[i];
            p.mesafe[i] = 0;
            q[son++] = paket(x, z);
        }
        while (bas < son) {
            long v = q[bas++];
            int x = (int) (v >> 32);
            int z = (int) v;
            Parca p = parca(x, z);
            int i = (x & 15) + ((z & 15) << 4);
            float k = p.kaynak[i];
            int d = p.mesafe[i];
            if (d >= maksMesafe) {
                continue;
            }
            for (int yon = 0; yon < 4; yon++) {
                int nx = x + (yon == 0 ? 1 : yon == 1 ? -1 : 0);
                int nz = z + (yon == 2 ? 1 : yon == 3 ? -1 : 0);
                Parca np = parca(nx, nz);
                if (np == null || !np.okundu) {
                    continue;
                }
                int ni = (nx & 15) + ((nz & 15) << 4);
                if (np.mesafe[ni] >= 0) {
                    continue;
                }
                int zn = np.zemin[ni];
                if (zn < k + maksArtis && zn >= k - tolerans) {
                    np.kaynak[ni] = k;
                    np.mesafe[ni] = d + 1;
                    q[son++] = paket(nx, nz);
                }
            }
        }
        sonSutunSayisi = toplam;
        sonHesapNs = System.nanoTime() - t0;
    }

    private static long paket(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
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
