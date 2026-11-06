package io.github.kadirberkpolat1.dogalafetler.core.mantik;

/**
 * TPS kısması (madde 3, spike-donma K-D2): tek kaynak {@code MinecraftServer.tickTimes} (nanosaniye halkası). Ortalama
 * yalnız dolu (&gt; 0) girdilerden alınır; ms/tick ve TPS aynı ortalamadan hesaplanır. TPS eşiğin altına inince ağır
 * işlerin bütçesi yarıya, kritik eşiğin altına inince çeyreğe iner; geri dönüş histerezisle, daha yüksek toparlanma
 * eşiklerinde olur (eşik çevresinde salınan TPS kademeyi saniyede bir gidip getirmesin).
 */
public final class TpsKismasi {
    public static final double EN_YUKSEK_TPS = 20.0;

    /**
     * Kademe eşikleri: düşüş {@code esik} (1 → 1/2) ve {@code kritikEsik} (→ 1/4); dönüş {@code toparlanma} (→ 1) ve
     * {@code kritikToparlanma} (1/4 → 1/2). Tutarsız değerler sıkıştırılır: kritik ≤ eşik ≤ toparlanma, kritik ≤
     * kritikToparlanma ≤ toparlanma.
     */
    public record Esikler(double esik, double kritikEsik, double toparlanma, double kritikToparlanma) {
        public static final Esikler VARSAYILAN = new Esikler(18.0, 15.0, 19.0, 16.5);

        public Esikler {
            kritikEsik = Math.min(kritikEsik, esik);
            toparlanma = Math.max(toparlanma, esik);
            kritikToparlanma = Math.max(kritikEsik, Math.min(kritikToparlanma, toparlanma));
        }
    }

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

    /** Yeni bütçe çarpanı (1, 1/2 ya da 1/4); {@code onceki} bir önceki çarpandır. */
    public static double carpan(double tps, double onceki, Esikler esikler) {
        if (tps < esikler.kritikEsik()) {
            return 0.25;
        }
        if (onceki <= 0.25) {
            if (tps >= esikler.toparlanma()) {
                return 1.0;
            }
            return tps >= esikler.kritikToparlanma() ? 0.5 : 0.25;
        }
        if (onceki <= 0.5) {
            return tps >= esikler.toparlanma() ? 1.0 : 0.5;
        }
        return tps < esikler.esik() ? 0.5 : 1.0;
    }
}
