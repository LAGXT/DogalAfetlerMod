package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Mağaraya dolmuş sanal su: sütun başına dolu dikey aralıklar ({@code [y0, y1)} çiftleri), toplam hacim, en alçak hücre
 * ve tam dolu hâlin yüzeyi ({@code ustY}). Gerçek blok tutmaz; dünya hiç değişmez.
 */
public final class MagaraCebi {
    public static final MagaraCebi BOS = new MagaraCebi(new HashMap<>(), 0, 0, 0, 0, false);

    private final Map<Long, int[]> sutunlar;
    private final int hacim;
    private final int minY;
    private final int ustY;
    private final int ziyaret;
    private final boolean acik;
    private final int agizY;
    private final int minX;
    private final int maxX;
    private final int minZ;
    private final int maxZ;

    public MagaraCebi(Map<Long, int[]> sutunlar, int hacim, int minY, int ustY, int ziyaret, boolean acik) {
        this(sutunlar, hacim, minY, ustY, ziyaret, acik, Integer.MAX_VALUE);
    }

    public MagaraCebi(Map<Long, int[]> sutunlar, int hacim, int minY, int ustY, int ziyaret, boolean acik, int agizY) {
        this.sutunlar = sutunlar;
        this.hacim = hacim;
        this.minY = minY;
        this.ustY = ustY;
        this.ziyaret = ziyaret;
        this.acik = acik;
        this.agizY = agizY;
        int x0 = Integer.MAX_VALUE;
        int x1 = Integer.MIN_VALUE;
        int z0 = Integer.MAX_VALUE;
        int z1 = Integer.MIN_VALUE;
        for (long k : sutunlar.keySet()) {
            int x = (int) (k >> 32);
            int z = (int) k;
            x0 = Math.min(x0, x);
            x1 = Math.max(x1, x);
            z0 = Math.min(z0, z);
            z1 = Math.max(z1, z);
        }
        this.minX = x0;
        this.maxX = x1;
        this.minZ = z0;
        this.maxZ = z1;
    }

    public static long anahtar(int x, int z) {
        return ((long) x << 32) | (z & 0xffffffffL);
    }

    public boolean bos() {
        return hacim == 0;
    }

    public int hacim() {
        return hacim;
    }

    public int minY() {
        return minY;
    }

    public int ustY() {
        return ustY;
    }

    public int ziyaretEdilen() {
        return ziyaret;
    }

    /** Keşif sınırına takıldı (boşluk daha büyük ya da açık); su seviyesi en alçak sızıntı noktasıyla sınırlandı. */
    public boolean acik() {
        return acik;
    }

    /** Keşifte değilen en alçak açık hücrenin (mağara ağzı) y'si; su bunun altında kalır. Ağız yoksa {@code MAX_VALUE}. */
    public int agizY() {
        return agizY;
    }

    public int sutunSayisi() {
        return sutunlar.size();
    }

    public Map<Long, int[]> sutunlar() {
        return Collections.unmodifiableMap(sutunlar);
    }

    public long[] sutunAnahtarlari() {
        long[] s = new long[sutunlar.size()];
        int i = 0;
        for (long k : sutunlar.keySet()) {
            s[i++] = k;
        }
        return s;
    }

    public boolean icerir(int x, int y, int z) {
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return false;
        }
        int[] r = sutunlar.get(anahtar(x, z));
        if (r == null) {
            return false;
        }
        for (int i = 0; i < r.length; i += 2) {
            if (y >= r[i] && y < r[i + 1]) {
                return true;
            }
        }
        return false;
    }

    /** Sütunun dolu aralıklarını {@code seviye}'ye kırparak ekler (sakin su). */
    public void aralikEkle(int x, int z, double seviye, SuAraliklari a) {
        if (x < minX || x > maxX || z < minZ || z > maxZ) {
            return;
        }
        int[] r = sutunlar.get(anahtar(x, z));
        if (r == null) {
            return;
        }
        for (int i = 0; i < r.length; i += 2) {
            double u = Math.min(r[i + 1], seviye);
            if (u > r[i]) {
                a.ekle(r[i], u, true);
            }
        }
    }
}
