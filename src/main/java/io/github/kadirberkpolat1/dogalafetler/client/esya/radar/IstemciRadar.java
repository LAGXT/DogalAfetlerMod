package io.github.kadirberkpolat1.dogalafetler.client.esya.radar;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarPaketi;
import java.util.function.Consumer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sunucudan gelen radar durumunun istemci önbelleği. Paket saniyede en çok bir kez gelir; gösterge geri sayımı paketten
 * sonra geçen istemci tick'iyle yumuşatır. Çıkışta sıfırlanır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciRadar {
    private static RadarPaketi durum = RadarPaketi.YOK;
    private static long tick;
    private static long alindiTick;
    private static int alinanPaket;

    private IstemciRadar() {
    }

    public static Consumer<RadarPaketi> alici() {
        return IstemciRadar::guncelle;
    }

    static void guncelle(RadarPaketi paket) {
        durum = paket;
        alindiTick = tick;
        alinanPaket++;
    }

    public static RadarPaketi durum() {
        return durum;
    }

    /** Oturumda gelen radar paketi sayısı (otomasyon log'u). */
    static int alinanPaket() {
        return alinanPaket;
    }

    static long tick() {
        return tick;
    }

    /** Kalan saniye, son paketten beri geçen süre düşülür; bilinmiyorsa -1. */
    static long kalanSn() {
        if (durum.kalanSn() < 0) {
            return -1;
        }
        return Math.max(0, durum.kalanSn() - (tick - alindiTick) / 20);
    }

    @SubscribeEvent
    public static void istemciTick(TickEvent.ClientTickEvent olay) {
        if (olay.phase == TickEvent.Phase.END) {
            tick++;
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        durum = RadarPaketi.YOK;
    }
}
