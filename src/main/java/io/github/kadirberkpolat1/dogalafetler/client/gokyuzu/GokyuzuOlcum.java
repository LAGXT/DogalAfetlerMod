package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import java.util.Arrays;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

/**
 * Spike ölçümü: sis rengi olayından (gökyüzünden hemen önce) {@code AFTER_SKY} aşamasına kadar geçen GPU süresi
 * ({@code GL_TIME_ELAPSED} sorgusu) ve CPU süresi. Vanilla ve bizim gökyüzümüz aynı aralıkta ölçülür; ekran kare
 * hızından (gizli workspace) bağımsızdır. İki sorgu sırayla kullanılır, sonuç bir sonraki karede okunur (bekleme yok).
 * GPU başka süreçlerle paylaşıldığında ortalama şişer; en küçük ve ortanca değer bu yüzden ayrıca tutulur.
 */
final class GokyuzuOlcum {
    static volatile boolean aktif;

    private static final int[] SORGU = new int[2];
    private static final boolean[] BEKLIYOR = new boolean[2];
    private static final long[] ORNEKLER = new long[4096];
    private static int ornekSayisi;
    private static int sira;
    private static boolean acik;
    private static long cpuBaslangic;
    private static long gpuToplamNs;
    private static long gpuSayisi;
    private static long cpuToplamNs;
    private static long cpuSayisi;

    private GokyuzuOlcum() {
    }

    static void basla() {
        if (!aktif) {
            return;
        }
        if (SORGU[0] == 0) {
            SORGU[0] = GL15.glGenQueries();
            SORGU[1] = GL15.glGenQueries();
        }
        if (acik) {
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
            acik = false;
        }
        int q = SORGU[sira];
        if (BEKLIYOR[sira]) {
            // Önceki turdaki sonuç hazırsa oku; değilse at (bekleme yaratmamak için).
            if (GL15.glGetQueryObjecti(q, GL15.GL_QUERY_RESULT_AVAILABLE) != 0) {
                long ns = GL33.glGetQueryObjecti64(q, GL15.GL_QUERY_RESULT);
                gpuToplamNs += ns;
                gpuSayisi++;
                if (ornekSayisi < ORNEKLER.length) {
                    ORNEKLER[ornekSayisi++] = ns;
                }
            }
            BEKLIYOR[sira] = false;
        }
        GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, q);
        acik = true;
        cpuBaslangic = System.nanoTime();
    }

    static void bitir() {
        if (!acik) {
            return;
        }
        GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
        acik = false;
        cpuToplamNs += System.nanoTime() - cpuBaslangic;
        cpuSayisi++;
        BEKLIYOR[sira] = true;
        sira ^= 1;
    }

    /** {gpu µs ort, cpu µs ort, gpu örnek, cpu örnek, gpu µs en küçük, gpu µs ortanca}; sayacı sıfırlar. */
    static double[] ozetVeSifirla() {
        long[] s = Arrays.copyOf(ORNEKLER, ornekSayisi);
        Arrays.sort(s);
        double[] r = {gpuSayisi == 0 ? -1 : gpuToplamNs / 1000.0 / gpuSayisi,
                cpuSayisi == 0 ? -1 : cpuToplamNs / 1000.0 / cpuSayisi, gpuSayisi, cpuSayisi,
                s.length == 0 ? -1 : s[0] / 1000.0, s.length == 0 ? -1 : s[s.length / 2] / 1000.0};
        gpuToplamNs = gpuSayisi = cpuToplamNs = cpuSayisi = 0;
        ornekSayisi = 0;
        return r;
    }
}
