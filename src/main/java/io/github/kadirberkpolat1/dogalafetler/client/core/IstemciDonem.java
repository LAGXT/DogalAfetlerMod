package io.github.kadirberkpolat1.dogalafetler.client.core;

import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;

/**
 * İstemcideki dünya dönemi önbelleği: sunucudan son gelen {@link DonemPaketi} ve onunla ilerletilen afet saati. Paket 1 Hz
 * gelir ve saati sunucuya hizalar; aradaki tahmin yalnız oyun duraklatılmamışken ilerler ve {@value #EN_COK_TAHMIN_TICK}
 * tick'le sınırlıdır (sunucu dururken istemci saati kaçmasın). Yalnız istemci ana thread'inde okunur. Gökyüzü ve diğer
 * istemci sistemleri değişimi {@link #surum()} ile fark eder.
 */
public final class IstemciDonem {
    static final int EN_COK_TAHMIN_TICK = 60;

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
        double gecen = Math.min(EN_COK_TAHMIN_TICK, istemciTick - alindigiTick + kismiTick);
        return durum.saat() + gecen * durum.hiz();
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
