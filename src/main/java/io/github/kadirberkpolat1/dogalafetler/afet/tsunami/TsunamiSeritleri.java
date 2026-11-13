package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.su.Seritler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Tsunami cephesinin şeritlerini kurar (K-E76, K-E77: "dalga bir bloka çarparsa ilerleyemez; başka alan varsa oradan sürer ama
 * alçak kalır; gittiği mesafe boyunca küçülür ve söner"). Dalga başlarken bir kez, yalnız yüklü chunk'larda çalışır: cephe
 * {@value #SERIT_GENISLIGI} blokluk (iki sütun) şeritlere bölünür; her şerit kıyıdan karaya her blokta, iki sütunda birden
 * taranır. Bir sütun engeldir: (a) en üst katı bloğu bu mesafedeki dalga tepesinden yüksek ve dalganın kıramayacağı bir bloktur ya da
 * (b) önceki bloğa göre en az 2 blok yükselen, dalganın kıramayacağı bir bloktur (duvar, kayalık, kıramadığı yapı; kırılabilen
 * bloklar {@link TsunamiYikimi} ile yıkılır, engel sayılmaz). Şeridin iki sütunundan biri bile engelse şerit orada, engelin yüzünde
 * durur; böylece dalga duvarın içinden geçmez ve duvardan önce de durmaz (K-E76'daki üç blokluk şeritte ve eksik örneklemde iki
 * sorun vardı). Engelin yanındaki şeritler yükseklik ölçeğinin {@value #DOLANMA_KAYBI} oranına kadar düşer (dolanan su alçak
 * kalır); karada yükseklik azalması {@link Seritler} içindedir. Tek seferlik iş: en kötü durumda şerit × adım × 2 ≈ 110 bin
 * sütun okuması, engelde şerit kendi taramasını bitirir.
 */
final class TsunamiSeritleri {
    static final double SERIT_GENISLIGI = 2.0;
    static final int EN_AZ_SERIT = 8;
    static final int EN_COK_SERIT = 160;
    /** Engelli komşu şeridin yükseklik ölçeğini düşürebileceği en büyük oran. */
    static final double DOLANMA_KAYBI = 0.5;
    /** Engelin etkisinin yanından kaç şerit uzağa ulaştığı. */
    private static final int ETKI_SERIDI = 4;
    private static final int YOKTU = Integer.MIN_VALUE;

    private TsunamiSeritleri() {
    }

    /**
     * @param aciklik dalga başlangıç çizgisinin kıyıya uzaklığı
     * @param karaGiris kıyıdan karaya en uzak ilerleme
     * @param tepe dalganın kıyıdaki yüksekliği
     */
    static Seritler hesapla(ServerLevel level, TsunamiGeometrisi g, double genislik, double aciklik, double karaGiris, double taban,
            double tepe, TsunamiSeviyesi sv) {
        int n = (int) Math.max(EN_AZ_SERIT, Math.min(EN_COK_SERIT, Math.round(genislik / SERIT_GENISLIGI)));
        double w = genislik / n;
        double menzil = aciklik + karaGiris;
        // Yükseklik azalması Dalga'dakiyle aynı formül: karaBasi = aciklik.
        Seritler azalma = new Seritler(aciklik, new byte[0], new byte[0]);
        int adim = (int) Math.floor(karaGiris);
        double[] kara = new double[n];
        double[] engelOrani = new double[n];
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int[] onceki = new int[2];
        for (int i = 0; i < n; i++) {
            double orta = -genislik / 2 + (i + 0.5) * w;
            double[] yan = {orta - w / 4, orta + w / 4};
            onceki[0] = ust(level, g, 0, yan[0], m);
            onceki[1] = ust(level, g, 0, yan[1], m);
            kara[i] = karaGiris;
            for (int d = 1; d <= adim; d++) {
                double yuzey = taban + tepe * azalma.azalma(aciklik + d, menzil);
                boolean engelli = false;
                for (int j = 0; j < 2 && !engelli; j++) {
                    int ust = ust(level, g, d, yan[j], m);
                    if (ust == YOKTU) {
                        continue;
                    }
                    BlockState s = level.getBlockState(m);
                    int once = onceki[j];
                    onceki[j] = ust;
                    if (s.isAir() || !s.getFluidState().isEmpty()) {
                        continue;
                    }
                    boolean yuksek = ust + 1 > yuzey + 0.5;
                    // Doğal arazide basamak eşiği dalga boyuyla büyür (S5 dalgasını iki blokluk kum tepesi durdurmaz); insan yapımı
                    // kırılamayan blokta (duvar) 2 blok yeter.
                    double esik = TsunamiYikimi.dogal(s) ? Math.max(2, Math.ceil(0.35 * (yuzey - taban))) : 2;
                    boolean basamak = once != YOKTU && ust >= once + esik;
                    engelli = (yuksek || basamak) && TsunamiYikimi.sinif(level, m, s, sv) == null;
                }
                if (engelli) {
                    // Engelin yüzünde durur (engel sütununun ortası d + 0,5'tedir).
                    kara[i] = d - 0.5;
                    break;
                }
            }
            engelOrani[i] = karaGiris > 0 ? 1.0 - kara[i] / karaGiris : 0.0;
        }
        double[] erisim = new double[n];
        double[] olcek = new double[n];
        for (int i = 0; i < n; i++) {
            erisim[i] = (aciklik + kara[i]) / menzil;
            double etki = 0;
            for (int k = -ETKI_SERIDI; k <= ETKI_SERIDI; k++) {
                int j = i + k;
                if (k != 0 && j >= 0 && j < n) {
                    etki = Math.max(etki, engelOrani[j] * (1.0 - Math.abs(k) / (ETKI_SERIDI + 1.0)));
                }
            }
            olcek[i] = 1.0 - DOLANMA_KAYBI * etki;
        }
        return Seritler.of(aciklik, erisim, olcek);
    }

    /**
     * Kıyıdan {@code d} blok içerideki, eksenden {@code yan} yandaki sütunun yaprak hariç en üst bloğunun Y'si; chunk yüklü değilse
     * {@link #YOKTU}. {@code m} o bloğa ayarlanır.
     */
    private static int ust(ServerLevel level, TsunamiGeometrisi g, double d, double yan, BlockPos.MutableBlockPos m) {
        int x = (int) Math.floor(g.x(d, yan));
        int z = (int) Math.floor(g.z(d, yan));
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) {
            return YOKTU;
        }
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
        m.set(x, ust, z);
        return ust;
    }
}
