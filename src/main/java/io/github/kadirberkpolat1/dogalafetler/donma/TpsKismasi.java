package io.github.kadirberkpolat1.dogalafetler.donma;

/**
 * TPS ölçümü ve blok bütçesi kısması (madde 3, madde 9 ÖNCELİK). TPS, sunucunun son 100 tick süresinin ortalamasından
 * hesaplanır ({@code MinecraftServer.tickTimes}, nanosaniye); {@code /forge tps} ile aynı formül: {@code min(20, 1000 / mspt)}.
 */
public final class TpsKismasi {
    private TpsKismasi() {
    }

    public static double tps(long[] tickNanosaniye) {
        if (tickNanosaniye.length == 0) {
            return 20.0;
        }
        long toplam = 0;
        for (long t : tickNanosaniye) {
            toplam += t;
        }
        double ortalama = toplam / (double) tickNanosaniye.length;
        if (ortalama <= 0.0) {
            return 20.0;
        }
        return Math.min(20.0, 1000.0 / (ortalama / 1_000_000.0));
    }

    /** 1 = tam bütçe, 2 = yarı ({@code tps < esik}), 4 = çeyrek ({@code tps < kritikEsik}). */
    public static int bolen(double tps, double esik, double kritikEsik) {
        if (tps < kritikEsik) {
            return 4;
        }
        if (tps < esik) {
            return 2;
        }
        return 1;
    }

    public static int butce(int temel, double tps, double esik, double kritikEsik) {
        return Math.max(1, temel / bolen(tps, esik, kritikEsik));
    }
}
