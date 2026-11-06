package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.Arrays;

/**
 * Deniz alanının periyodik tazelenmesi: hangi chunk'lar yeniden okunacak. İstemcide sunucudan gelen blok güncellemeleri hiç
 * Forge olayı atmaz ({@code LevelAccessor.blockUpdated} istemcide boş), sunucuda {@code NeighborNotifyEvent} yalnız
 * {@code UPDATE_NEIGHBORS} bayrağıyla gelir; bu yüzden iki taraf da aynı planı kullanır ve yeniden okuma değişmeyen chunk
 * için BFS başlatmaz ({@link DenizSeviyesiAlani#isle}).
 *
 * <p>Her {@code periyot} tick'te bir: odak chunk'larının (oyuncu, kamera, sabitlenmiş bölge) ±{@code yakinYaricap}
 * halkasındaki istenen chunk'ların hepsi, kalan istenen chunk'lardan sıralı dönüşümlü {@code uzakButce} tanesi. Uzak
 * chunk'lar ayrıca blok olaylarıyla ve chunk yüklenmesiyle geçersiz kılınır. Tek iş parçacığı.
 */
public final class TazelemePlani {
    private static final long[] BOS = new long[0];

    private final int yakinYaricap;
    private final int periyot;
    private int uzakButce;
    private long sonTick = Long.MIN_VALUE;
    private int imlec;

    public TazelemePlani(int yakinYaricap, int periyot, int uzakButce) {
        this.yakinYaricap = yakinYaricap;
        this.periyot = periyot;
        this.uzakButce = uzakButce;
    }

    /**
     * 10 tick'lik okuma bütçesini tick'lere yayar (SU-20): tick başına {@code ceil(onTick * max(1, ceil(oyuncu / 2)) * kisma
     * / 10)}, en az 1. Oyuncu sayısıyla ölçeklenir: her oyuncunun yakın halkası (±2 chunk, 25 chunk/sn) ayrı okunur.
     */
    public static int tickButcesi(int onTick, int oyuncu, double kisma) {
        double carpan = Math.max(1, Math.ceil(oyuncu / 2.0));
        return Math.max(1, (int) Math.ceil(onTick * carpan * kisma / 10.0));
    }

    /**
     * İstenen chunk'ları en yakın odağa Chebyshev uzaklığına göre sıralar (SU-27): {@link DenizAlani#iste} yeni chunk'ları
     * bu sırayla kuyruğa ekler, oyuncunun (ve sabit bölgenin) çevresi uzak köşelerden önce okunur. Eşitlikte anahtar.
     */
    public static long[] yakindanUzaga(java.util.Collection<Long> istenen, long[] odaklar) {
        long[] s = new long[istenen.size()];
        int n = 0;
        for (long k : istenen) {
            int cx = (int) (k >> 32);
            int cz = (int) k;
            int en = odaklar.length == 0 ? 0 : Integer.MAX_VALUE;
            for (long o : odaklar) {
                en = Math.min(en, Math.max(Math.abs(cx - (int) (o >> 32)), Math.abs(cz - (int) o)));
            }
            s[n++] = k;
            UZAKLIK.put(k, en);
        }
        Long[] kutu = new Long[n];
        for (int i = 0; i < n; i++) {
            kutu[i] = s[i];
        }
        Arrays.sort(kutu, (a, b) -> {
            int c = Integer.compare(UZAKLIK.get(a), UZAKLIK.get(b));
            return c != 0 ? c : Long.compare(a, b);
        });
        UZAKLIK.clear();
        for (int i = 0; i < n; i++) {
            s[i] = kutu[i];
        }
        return s;
    }

    private static final java.util.HashMap<Long, Integer> UZAKLIK = new java.util.HashMap<>();

    /** Uzak chunk bütçesini değiştirir (config). */
    public void uzakButce(int butce) {
        uzakButce = Math.max(0, butce);
    }

    /**
     * {@code tick} anında yeniden okunacak chunk anahtarları ({@link DenizSeviyesiAlani#anahtar}); periyot dolmadıysa boş.
     * Zaman geri giderse (yeni dünya) plan yeniden başlar.
     */
    public long[] sec(long tick, long[] odaklar, long[] istenenler) {
        if (sonTick != Long.MIN_VALUE && tick >= sonTick && tick - sonTick < periyot) {
            return BOS;
        }
        sonTick = tick;
        if (istenenler.length == 0) {
            return BOS;
        }
        long[] sirali = istenenler.clone();
        Arrays.sort(sirali);
        long[] yakin = new long[sirali.length];
        long[] uzak = new long[sirali.length];
        int ny = 0;
        int nu = 0;
        for (long k : sirali) {
            if (yakinMi(k, odaklar)) {
                yakin[ny++] = k;
            } else {
                uzak[nu++] = k;
            }
        }
        int alinan = Math.min(uzakButce, nu);
        long[] sonuc = Arrays.copyOf(yakin, ny + alinan);
        if (nu > 0) {
            imlec %= nu;
            for (int i = 0; i < alinan; i++) {
                sonuc[ny + i] = uzak[(imlec + i) % nu];
            }
            imlec = (imlec + alinan) % nu;
        }
        return sonuc;
    }

    private boolean yakinMi(long k, long[] odaklar) {
        int cx = (int) (k >> 32);
        int cz = (int) k;
        for (long o : odaklar) {
            if (Math.abs(cx - (int) (o >> 32)) <= yakinYaricap && Math.abs(cz - (int) o) <= yakinYaricap) {
                return true;
            }
        }
        return false;
    }
}
