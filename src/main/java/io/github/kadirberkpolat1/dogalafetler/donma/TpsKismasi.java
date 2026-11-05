package io.github.kadirberkpolat1.dogalafetler.donma;

/**
 * TPS ölçümü ve blok bütçesi kısması (madde 3, madde 9 ÖNCELİK). Tek kaynak: {@code MinecraftServer.tickTimes} (son 100 tick,
 * nanosaniye); ms/tick ve TPS aynı ortalamadan gelir. Henüz yazılmamış (0) girdiler ortalamaya girmez, yoksa sunucu
 * açılışından sonraki ilk 100 tick'te TPS olduğundan yüksek görünür.
 */
public final class TpsKismasi {
    private TpsKismasi() {
    }

    /** Dolu girdilerin ortalama tick süresi (ms); hiç ölçüm yoksa 0. */
    public static double ortalamaMs(long[] tickNanosaniye) {
        long toplam = 0;
        int adet = 0;
        for (long t : tickNanosaniye) {
            if (t > 0) {
                toplam += t;
                adet++;
            }
        }
        return adet == 0 ? 0.0 : toplam / (double) adet / 1_000_000.0;
    }

    public static double tps(long[] tickNanosaniye) {
        double ms = ortalamaMs(tickNanosaniye);
        return ms <= 0.0 ? 20.0 : Math.min(20.0, 1000.0 / ms);
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
