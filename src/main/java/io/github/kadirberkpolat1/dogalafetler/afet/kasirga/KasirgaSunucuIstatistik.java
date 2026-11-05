package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spike ölçümü: kasırga ve uçan blok tick maliyeti, varlık sayıları, ortalama tick süresi. Her 100 tick'te (aktif kasırga
 * varken) log'a yazar; {@code dur} komutundan birkaç tick sonra kalan varlıkları sayar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class KasirgaSunucuIstatistik {
    private static long kasirgaNs;
    private static long ucanBlokNs;
    private static int ucanBlokTikSayisi;
    private static int pencereTik;
    private static int bekleyenSayim = -1;

    private KasirgaSunucuIstatistik() {
    }

    static void kasirgaTiki(long ns) {
        kasirgaNs += ns;
    }

    static void ucanBlokTiki(long ns) {
        ucanBlokNs += ns;
        ucanBlokTikSayisi++;
    }

    static void sayimiZamanla() {
        bekleyenSayim = 3;
    }

    /** [kasırga, uçan blok] varlık sayıları, bütün boyutlarda. */
    public static int[] say(MinecraftServer sunucu) {
        int kasirga = 0;
        int ucan = 0;
        for (ServerLevel seviye : sunucu.getAllLevels()) {
            for (Entity varlik : seviye.getAllEntities()) {
                if (varlik instanceof KasirgaVarligi) {
                    kasirga++;
                } else if (varlik instanceof UcanBlokVarligi) {
                    ucan++;
                }
            }
        }
        return new int[] {kasirga, ucan};
    }

    @SubscribeEvent
    public static void sunucuTiki(TickEvent.ServerTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer sunucu = olay.getServer();
        if (bekleyenSayim > 0 && --bekleyenSayim == 0) {
            int[] sayi = say(sunucu);
            DogalAfetler.LOGGER.info("[spike-kasirga] dur sonrası varlık sayısı: kasirga={}, ucan_blok={}", sayi[0],
                    sayi[1]);
            bekleyenSayim = -1;
        }
        if (++pencereTik < 100) {
            return;
        }
        if (kasirgaNs > 0 || ucanBlokTikSayisi > 0) {
            int[] sayi = say(sunucu);
            float ortMs = sunucu.getAverageTickTime();
            DogalAfetler.LOGGER.info("[spike-kasirga] sunucu 100 tick: kasirga tik ort {} µs, ucan blok tik ort {} µs/tick"
                    + " ({} varlık-tik), kasirga={}, ucan_blok={}, ort tick {} ms (TPS {})",
                    String.format("%.1f", kasirgaNs / 1000.0 / pencereTik),
                    String.format("%.1f", ucanBlokNs / 1000.0 / pencereTik), ucanBlokTikSayisi, sayi[0], sayi[1],
                    String.format("%.2f", ortMs), String.format("%.1f", Math.min(20, 1000 / Math.max(ortMs, 1e-3))));
        }
        kasirgaNs = 0;
        ucanBlokNs = 0;
        ucanBlokTikSayisi = 0;
        pencereTik = 0;
    }
}
