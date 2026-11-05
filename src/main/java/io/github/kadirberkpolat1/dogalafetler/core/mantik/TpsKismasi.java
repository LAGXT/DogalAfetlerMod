package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * TPS kısması (madde 3, spike-donma K-D2): tek kaynak {@code MinecraftServer.tickTimes} (nanosaniye halkası). Ortalama
 * yalnız dolu (&gt; 0) girdilerden alınır; ms/tick ve TPS aynı ortalamadan hesaplanır. TPS eşiğin altına inince ağır
 * işlerin bütçesi yarıya, kritik eşiğin altına inince çeyreğe iner.
 */
public final class TpsKismasi {
    public static final double EN_YUKSEK_TPS = 20.0;

    private TpsKismasi() {
    }

    public static double ortalamaMs(long[] tickNanosaniye) {
        long toplam = 0;
        int sayi = 0;
        for (long sure : tickNanosaniye) {
            if (sure > 0) {
                toplam += sure;
                sayi++;
            }
        }
        return sayi == 0 ? 0.0 : toplam / (double) sayi / 1_000_000.0;
    }

    public static double tps(double ortalamaMs) {
        return ortalamaMs <= 0 ? EN_YUKSEK_TPS : Math.min(EN_YUKSEK_TPS, 1000.0 / ortalamaMs);
    }

    /** Bütçe çarpanı: 1, 1/2 ({@code tps < esik}) ya da 1/4 ({@code tps < kritikEsik}). */
    public static double carpan(double tps, double esik, double kritikEsik) {
        if (tps < kritikEsik) {
            return 0.25;
        }
        if (tps < esik) {
            return 0.5;
        }
        return 1.0;
    }
}
