package io.github.kadirberkpolat1.dogalafetler.donma.mantik;

/**
 * Global donma ilerlemesinin (G, 0–100) zamanla değişimi (madde 9). Güneşsiz dönemde G {@code donmaSuresiGun} boyunca 0'dan
 * 100'e çıkar; kızıl cüce çözülmesinde {@code cozulmeSuresiGun} boyunca 100'den {@code taban}'a iner ve altına inmez ("dünya
 * hiçbir zaman tamamen erimesin"); bağımsız çözülme (config, varsayılan kapalı) 100'den 0'a aynı hızla iner. Adım ölçekli
 * tick'tir ({@code AfetYoneticisi.zamanAdimi()}), böylece {@code /dogalafet hiz} süreci hızlandırır.
 */
public final class DonmaIlerlemesi {
    public static final int GUN_TICK = 24_000;

    private DonmaIlerlemesi() {
    }

    public static float donmaAdimi(float g, int adim, double donmaSuresiGun) {
        return Math.min(100f, g + (float) (100.0 * adim / tick(donmaSuresiGun)));
    }

    public static float cozulmeAdimi(float g, int adim, double cozulmeSuresiGun, double taban) {
        if (g <= taban) {
            return g;
        }
        return (float) Math.max(taban, g - (100.0 - taban) * adim / tick(cozulmeSuresiGun));
    }

    public static float bagimsizCozulmeAdimi(float g, int adim, double cozulmeSuresiGun) {
        return (float) Math.max(0.0, g - 100.0 * adim / tick(cozulmeSuresiGun));
    }

    private static double tick(double gun) {
        if (!(gun > 0.0)) {
            throw new IllegalArgumentException("süre pozitif olmalı: " + gun);
        }
        return gun * GUN_TICK;
    }
}
