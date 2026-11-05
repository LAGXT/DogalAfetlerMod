package io.github.kadirberkpolat1.dogalafetler.client.core;

import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;

/**
 * İstemcideki dünya dönemi önbelleği: sunucudan son gelen {@link DonemPaketi} ve onunla ilerletilen afet saati. Yalnız
 * istemci ana thread'inde okunur. Gökyüzü ve diğer istemci sistemleri değişimi {@link #surum()} ile fark eder.
 */
public final class IstemciDonem {
    private static DonemPaketi durum = DonemPaketi.VARSAYILAN;
    private static long istemciTick;
    private static long alindigiTick;
    private static int surum;

    private IstemciDonem() {
    }

    public static DonemPaketi durum() {
        return durum;
    }

    /** Her yeni pakette ve sıfırlamada artar. */
    public static int surum() {
        return surum;
    }

    /** İstemcide tahmin edilen ölçekli afet saati; {@code kismiTick} kare arası ({@code partialTick}). */
    public static double saat(float kismiTick) {
        return durum.saat() + (istemciTick - alindigiTick + kismiTick) * durum.hiz();
    }

    static void guncelle(DonemPaketi paket) {
        durum = paket;
        alindigiTick = istemciTick;
        surum++;
    }

    static void tickIlerlet() {
        istemciTick++;
    }

    static void sifirla() {
        durum = DonemPaketi.VARSAYILAN;
        alindigiTick = istemciTick;
        surum++;
    }
}
