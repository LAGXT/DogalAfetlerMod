package io.github.kadirberkpolat1.dogalafetler.client.core;

import io.github.kadirberkpolat1.dogalafetler.client.sicaklik.IstemciSicaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Donma, yanma/ısı ve asit gibi ekran efektlerinin (HUD katmanları, tam ekran örtüler, kamera titreşimi) çizim kapısı; her
 * efekt çizimine başlamadan buradan geçer. Yaratıcı ve izleyici modundaki oyuncuya bu efektler gelmez ({@link #izinli()}).
 * Donma ve ısı efektleri ayrıca yalnız oyuncunun VÜCUT sıcaklığı soğuduğunda ya da ısındığında açılır (paketteki
 * vücut durumu, {@link IstemciSicaklik#sicaklikDurumu()}); ortam sıcaklığının ya da dünya olayının kendisi efekti açmaz.
 */
public final class EkranEfektKapisi {
    private EkranEfektKapisi() {
    }

    /** Oyuncu varsa ve yaratıcı ya da izleyici değilse ekran efektleri çizilebilir. */
    public static boolean izinli(Player oyuncu) {
        return oyuncu != null && !oyuncu.isCreative() && !oyuncu.isSpectator();
    }

    /** Yerel oyuncu için {@link #izinli(Player)}. */
    public static boolean izinli() {
        return izinli(Minecraft.getInstance().player);
    }

    /** Donma efekti: izinli oyuncu ve VÜCUT donma durumunda (üşüme değil, çok soğuk ya da donuyor). */
    public static boolean donmaIzinli() {
        return izinli() && IstemciSicaklik.acik() && IstemciSicaklik.sicaklikDurumu() == SicaklikDurumu.DONMA;
    }

    /** Yanma/ısı efekti: izinli oyuncu ve VÜCUT bunalma ya da yanma durumunda. */
    public static boolean isiIzinli() {
        if (!izinli() || !IstemciSicaklik.acik()) {
            return false;
        }
        SicaklikDurumu durum = IstemciSicaklik.sicaklikDurumu();
        return durum == SicaklikDurumu.BUNALMA || durum == SicaklikDurumu.YANMA;
    }
}
