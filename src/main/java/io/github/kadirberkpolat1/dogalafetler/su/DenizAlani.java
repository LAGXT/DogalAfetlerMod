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
 * Deniz seviyesi yükselmesinin alanı (bölüm H, SU-06, SU-11). Yalnız istenen (yüklü) chunk'ların sütun bilgisi önbellekte
 * tutulur: zemin (kara üstü ya da gerçek su yüzeyi), sütunun durgun gerçek su olup olmadığı ve deniz dibi. Diskte ve ağda
 * yalnız global artış gider; alan her tarafta yerel hesaplanır.
 *
 * <p>Hesap su kütlesi başınadır. Kütle = aynı yüzey yüksekliğindeki bitişik su sütunları. Her kütleden öncelikli taşkın
 * (kova kuyruğu) her sütuna bir {@code eşik} yazar: kütleden o sütuna giden yolların en yükseği en alçak olanın zemini.
 * Kütle {@code a} kadar yükselince seviyesi {@code min(yüzey + a, tavan)} olur; eşiği bu seviyenin altında kalan sütunlar
 * {@code [zemin, seviye)} sanal suyla dolar. Deniz kütlesi (yüzeyi {@code denizSeviyesi} ve altı) sınırsızdır: kıyıdaki
 * çukura ancak su çukuru çevreleyen kumulu aşınca girer. Göl kütlesi taşma ağzında durur: taşkın yüzeyin {@code tolerans}
 * bloktan fazla altındaki bir sütuna değerse tavan o anki eşiktir (su ağızdan aşağı akar, birikmez). Bir sütunu birden çok
 * kütle doldurursa tam yükselmede en yüksek seviyeyi veren kazanır. Böylece komşusu kuru olan ıslak bir sütunun komşusunun
 * zemini her zaman su yüzeyinin üstündedir: havada asılı su duvarı oluşmaz ({@code DenizAlaniTest}). Sonuç chunk okuma
 * sırasından bağımsızdır.
 *
 * <p>İş parçacıkları: {@link #iste}, {@link #isle}, okuyucular sahibi iş parçacığında (sunucu ya da istemci) çağrılır.
 * {@link #yurutucu(Executor)} verilirse hesap çağıranda alınan anlık görüntü üzerinde arka planda koşar ve sonuç parça başına
 * tek atamayla yayınlanır; okuyucular o ana kadar eski sonucu görür.
 */
public final class DenizAlani {
    /** Sistemin desteklediği en büyük artış (config üst sınırının üst sınırı); taşkın bu kadar yükseğe bakar. */
    public static final int EN_COK_ARTIS = 8;

    /** Bir chunk'ın 256 sütununu ({@code indeks = lx + lz * 16}) doldurur; chunk yüklü değilse {@code false}. */
    @FunctionalInterface
    public interface SutunOkuyucu {
        boolean oku(int cx, int cz, int[] zemin, boolean[] su, int[] taban);
    }

    /** Bir parçanın yayınlanmış hesap sonucu: kazanan kütlenin yüzeyi (yoksa NaN), tavanı ve sütunun eşiği. */
    private record Sonuc(float[] kaynak, float[] tavan, int[] esik) {
        static final Sonuc BOS = bos();

        private static Sonuc bos() {
            float[] k = new float[256];
            Arrays.fill(k, Float.NaN);
            return new Sonuc(k, new float[256], new int[256]);
        }
    }

    private static final class Parca {
        final int cx;
        final int cz;
        final int[] zemin = new int[256];
        final boolean[] su = new boolean[256];
        final int[] taban = new int[256];
        volatile Sonuc sonuc = Sonuc.BOS;
        boolean okundu;
        boolean kuyrukta;

        Parca(int cx, int cz) {
            this.cx = cx;
            this.cz = cz;
        }
    }

    private final int maksArtis;
    private final int tolerans;
    private final int denizSeviyesi;
    private final Map<Long, Parca> parcalar = new HashMap<>();
    private final ArrayDeque<Parca> kuyruk = new ArrayDeque<>();
    private final AtomicBoolean hesapta = new AtomicBoolean();
    private Executor yurutucu;
    private Parca onbellek;
    private boolean kirli;
    private volatile long sonHesapNs;
    private volatile int sonSutunSayisi;
    private volatile int hesapSayisi;
    private volatile int sonGovdeSayisi;
    private int degisenOkuma;
    private final int[] okuZemin = new int[256];
    private final boolean[] okuSu = new boolean[256];
    private final int[] okuTaban = new int[256];

    /**
     * @param maksArtis taşkının baktığı en büyük artış (üretimde {@link #EN_COK_ARTIS})
     * @param tolerans göl taşkınının taşma sayılmadan inebileceği blok
     * @param denizSeviyesi bu yüzey ve altındaki kütleler denizdir (vanilla su yüzeyi zemini 63)
     */
    public DenizAlani(int maksArtis, int tolerans, int denizSeviyesi) {
        this.maksArtis = maksArtis;
        this.tolerans = tolerans;
        this.denizSeviyesi = denizSeviyesi;
    }

    /** BFS'in koşacağı yürütücü; {@code null} ise {@link #isle} içinde eşzamanlı. */
    public void yurutucu(Executor y) {
        this.yurutucu = y;
    }

    public boolean hesapSuruyor() {
        return hesapta.get();
    }

    /** Tamamlanmış hesap sayısı (log ve test için). */
    public int hesapSayisi() {
        return hesapSayisi;
    }

    /** İlk okuma ya da verisi değişmiş yeniden okuma sayısı (hesabı yalnız bunlar tetikler). */
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
     * değerler kullanılır. Okunan veri öncekiyle aynıysa hesap yeniden koşmaz, bu yüzden gereksiz çağrı ucuzdur.
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

    /** Son hesaptaki su kütlesi sayısı (log). */
    public int sonGovdeSayisi() {
        return sonGovdeSayisi;
    }

    /**
     * En çok {@code butce} chunk okur; verisi değişen chunk varsa ve önceki hesap bitmişse hesabı başlatır. Okunan chunk
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

    /** Bütün önbellek üzerinde taşkın hesabı (eşzamanlı). */
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
        liste.sort((a, b) -> Long.compare(anahtar(a.cx, a.cz), anahtar(b.cx, b.cz)));
        int n = liste.size();
        int[] zemin = new int[n * 256];
        boolean[] su = new boolean[n * 256];
        Map<Long, Integer> indeks = new HashMap<>(n * 2);
        for (int i = 0; i < n; i++) {
            Parca p = liste.get(i);
            System.arraycopy(p.zemin, 0, zemin, i * 256, 256);
            System.arraycopy(p.su, 0, su, i * 256, 256);
            indeks.put(anahtar(p.cx, p.cz), i);
        }
        // Komşu parça: [i*4 + yön] (0:+x 1:-x 2:+z 3:-z), yoksa -1.
        int[] komsuParca = new int[n * 4];
        for (int i = 0; i < n; i++) {
            Parca p = liste.get(i);
            komsuParca[i * 4] = indeks.getOrDefault(anahtar(p.cx + 1, p.cz), -1);
            komsuParca[i * 4 + 1] = indeks.getOrDefault(anahtar(p.cx - 1, p.cz), -1);
            komsuParca[i * 4 + 2] = indeks.getOrDefault(anahtar(p.cx, p.cz + 1), -1);
            komsuParca[i * 4 + 3] = indeks.getOrDefault(anahtar(p.cx, p.cz - 1), -1);
        }
        return () -> {
            long t0 = System.nanoTime();
            Taskin t = new Taskin(n, zemin, su, komsuParca);
            t.hesapla();
            for (int i = 0; i < n; i++) {
                liste.get(i).sonuc = new Sonuc(Arrays.copyOfRange(t.enKaynak, i * 256, i * 256 + 256),
                        Arrays.copyOfRange(t.enTavan, i * 256, i * 256 + 256),
                        Arrays.copyOfRange(t.enEsik, i * 256, i * 256 + 256));
            }
            sonSutunSayisi = n * 256;
            sonGovdeSayisi = t.govdeSayisi;
            sonHesapNs = System.nanoTime() - t0;
            hesapSayisi++;
        };
    }

    /** Tek hesabın çalışma alanı; sütun indeksi {@code parça * 256 + lx + lz * 16}. */
    private final class Taskin {
        final int n;
        final int[] zemin;
        final boolean[] su;
        final int[] komsuParca;
        final float[] enKaynak;
        final float[] enTavan;
        final int[] enEsik;
        final float[] enSeviye;
        int govdeSayisi;

        Taskin(int n, int[] zemin, boolean[] su, int[] komsuParca) {
            this.n = n;
            this.zemin = zemin;
            this.su = su;
            this.komsuParca = komsuParca;
            enKaynak = new float[n * 256];
            enTavan = new float[n * 256];
            enEsik = new int[n * 256];
            enSeviye = new float[n * 256];
            Arrays.fill(enKaynak, Float.NaN);
        }

        /** Sütunun {@code yon} komşusu ya da -1 (yüklü değil). */
        int komsu(int g, int yon) {
            int i = g >> 8;
            int c = g & 255;
            int lx = c & 15;
            int lz = c >> 4;
            switch (yon) {
                case 0 -> {
                    if (lx < 15) {
                        return g + 1;
                    }
                    int o = komsuParca[i * 4];
                    return o < 0 ? -1 : o * 256 + (lz << 4);
                }
                case 1 -> {
                    if (lx > 0) {
                        return g - 1;
                    }
                    int o = komsuParca[i * 4 + 1];
                    return o < 0 ? -1 : o * 256 + 15 + (lz << 4);
                }
                case 2 -> {
                    if (lz < 15) {
                        return g + 16;
                    }
                    int o = komsuParca[i * 4 + 2];
                    return o < 0 ? -1 : o * 256 + lx;
                }
                default -> {
                    if (lz > 0) {
                        return g - 16;
                    }
                    int o = komsuParca[i * 4 + 3];
                    return o < 0 ? -1 : o * 256 + lx + (15 << 4);
                }
            }
        }

        void hesapla() {
            int toplam = n * 256;
            int[] govde = new int[toplam];
            Arrays.fill(govde, -1);
            int[] damga = new int[toplam];
            Arrays.fill(damga, -1);
            int[] esik = new int[toplam];
            int[] kuyruk = new int[Math.max(16, toplam)];
            int[][] kova = new int[maksArtis][];
            int[] kovaBoy = new int[maksArtis];
            for (int k = 0; k < maksArtis; k++) {
                kova[k] = new int[64];
            }
            int[] uyeler = new int[Math.max(16, toplam)];
            int[] islenen = new int[Math.max(16, toplam)];
            for (int g0 = 0; g0 < toplam; g0++) {
                if (!su[g0] || govde[g0] >= 0) {
                    continue;
                }
                // Kütle: aynı zeminli bitişik su sütunları.
                int b = govdeSayisi++;
                int s = zemin[g0];
                int uye = 0;
                govde[g0] = b;
                uyeler[uye++] = g0;
                for (int q = 0; q < uye; q++) {
                    int g = uyeler[q];
                    for (int yon = 0; yon < 4; yon++) {
                        int ng = komsu(g, yon);
                        if (ng >= 0 && su[ng] && govde[ng] < 0 && zemin[ng] == s) {
                            govde[ng] = b;
                            uyeler[uye++] = ng;
                        }
                    }
                }
                boolean deniz = s <= denizSeviyesi;
                int tavanSeviye = maksArtis;
                Arrays.fill(kovaBoy, 0);
                for (int q = 0; q < uye; q++) {
                    int g = uyeler[q];
                    damga[g] = b;
                    esik[g] = s;
                    kovaEkle(kova, kovaBoy, 0, g);
                }
                int islenenSayi = 0;
                taskin:
                for (int seviye = 0; seviye < maksArtis; seviye++) {
                    for (int q = 0; q < kovaBoy[seviye]; q++) {
                        int g = kova[seviye][q];
                        islenen[islenenSayi++] = g;
                        for (int yon = 0; yon < 4; yon++) {
                            int ng = komsu(g, yon);
                            if (ng < 0 || damga[ng] == b) {
                                continue;
                            }
                            int zn = zemin[ng];
                            if (!deniz && zn < s - tolerans) {
                                tavanSeviye = seviye;
                                break taskin;
                            }
                            int e = Math.max(s + seviye, zn);
                            if (e >= s + maksArtis) {
                                continue;
                            }
                            damga[ng] = b;
                            esik[ng] = e;
                            kovaEkle(kova, kovaBoy, e - s, ng);
                        }
                    }
                }
                float tavan = s + tavanSeviye;
                for (int q = 0; q < islenenSayi; q++) {
                    int g = islenen[q];
                    if (esik[g] < tavan) {
                        aday(g, s, tavan, esik[g]);
                    }
                }
            }
        }

        /** Tam yükselmede en yüksek seviye kazanır; eşitse yüksek yüzey (her artışta en az onun kadar), sonra küçük eşik. */
        void aday(int g, float s, float tavan, int e) {
            float seviye = Math.min(s + maksArtis, tavan);
            float eskiK = enKaynak[g];
            if (!Float.isNaN(eskiK)) {
                float eskiS = enSeviye[g];
                if (seviye < eskiS || seviye == eskiS && (s < eskiK || s == eskiK && e >= enEsik[g])) {
                    return;
                }
            }
            enKaynak[g] = s;
            enTavan[g] = tavan;
            enEsik[g] = e;
            enSeviye[g] = seviye;
        }
    }

    private static void kovaEkle(int[][] kova, int[] boy, int k, int g) {
        if (boy[k] == kova[k].length) {
            kova[k] = Arrays.copyOf(kova[k], boy[k] * 2);
        }
        kova[k][boy[k]++] = g;
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

    private static int i(int x, int z) {
        return (x & 15) + ((z & 15) << 4);
    }

    public boolean yuklu(int x, int z) {
        return okunmus(x, z) != null;
    }

    /** Sütunu dolduran kütlenin yüzeyi; hiçbir kütle doldurmuyorsa NaN. */
    public float kaynak(int x, int z) {
        Parca p = okunmus(x, z);
        return p == null ? Float.NaN : p.sonuc.kaynak()[i(x, z)];
    }

    /** Kütleden sütuna giden en alçak yolun en yüksek zemini; seviye bunun üstüne çıkınca sütun ıslanır. */
    public int esik(int x, int z) {
        Parca p = okunmus(x, z);
        return p == null || Float.isNaN(p.sonuc.kaynak()[i(x, z)]) ? Integer.MAX_VALUE : p.sonuc.esik()[i(x, z)];
    }

    /** Kütlenin çıkabileceği en yüksek seviye (göl taşma ağzı; denizde yüzey + en büyük artış). */
    public float tavan(int x, int z) {
        Parca p = okunmus(x, z);
        return p == null ? Float.NaN : p.sonuc.tavan()[i(x, z)];
    }

    /** Kara üstü ya da gerçek su yüzeyi; bilinmiyorsa {@link Integer#MIN_VALUE}. */
    public int zemin(int x, int z) {
        Parca p = okunmus(x, z);
        return p == null ? Integer.MIN_VALUE : p.zemin[i(x, z)];
    }

    public boolean su(int x, int z) {
        Parca p = okunmus(x, z);
        return p != null && p.su[i(x, z)];
    }

    /** Gerçek su sütununda dip (ilk su bloğunun altı); karada zemin. */
    public int taban(int x, int z) {
        Parca p = okunmus(x, z);
        return p == null ? Integer.MIN_VALUE : p.taban[i(x, z)];
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
        int c = i(x, z);
        Sonuc r = p.sonuc;
        float k = r.kaynak()[c];
        if (Float.isNaN(k)) {
            return;
        }
        double seviye = Math.min(k + Math.min(artis, maksArtis), r.tavan()[c]);
        if (r.esik()[c] < seviye) {
            a.ekle(p.zemin[c], seviye);
        }
    }
}
