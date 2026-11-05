package io.github.kadirberkpolat1.dogalafetler.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TpsKismasi;
import java.util.Locale;
import net.minecraft.server.MinecraftServer;

/**
 * Tek TPS ölçer (madde 3, spike-donma K-D2): yıkım, yangın, donma/çözülme ve efekt bütçeleri {@link #kismaCarpani()}'nı
 * okur ve işlerini bununla çarpar (1, 1/2, 1/4). Ölçüm saniyede bir {@code MinecraftServer.tickTimes}'ın dolu
 * girdilerinden alınır; eşikler config'tedir ({@code cekirdek.tps}). Kademe değişince tek satır log yazılır; ms/tick ve TPS
 * aynı ortalamadandır.
 */
public final class TpsOlcer {
    private static final int OLCUM_ARALIGI = 20;

    private static volatile double ortalamaMs;
    private static volatile double tps = TpsKismasi.EN_YUKSEK_TPS;
    private static volatile double kismaCarpani = 1.0;
    private static int sayac;

    private TpsOlcer() {
    }

    /** Ağır işlerin bütçe çarpanı: 1, 0.5 ya da 0.25. */
    public static double kismaCarpani() {
        return kismaCarpani;
    }

    public static double tps() {
        return tps;
    }

    public static double ortalamaMs() {
        return ortalamaMs;
    }

    static void guncelle(MinecraftServer server, double esik, double kritikEsik) {
        if (++sayac % OLCUM_ARALIGI != 0) {
            return;
        }
        double ms = TpsKismasi.ortalamaMs(server.tickTimes);
        double yeniTps = TpsKismasi.tps(ms);
        double yeniCarpan = TpsKismasi.carpan(yeniTps, esik, kritikEsik);
        ortalamaMs = ms;
        tps = yeniTps;
        if (yeniCarpan != kismaCarpani) {
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT, "TPS %.1f (%.1f ms/tick): bütçe çarpanı %.2f -> %.2f",
                    yeniTps, ms, kismaCarpani, yeniCarpan));
            kismaCarpani = yeniCarpan;
        }
    }

    static void sifirla() {
        ortalamaMs = 0;
        tps = TpsKismasi.EN_YUKSEK_TPS;
        kismaCarpani = 1.0;
        sayac = 0;
    }
}
