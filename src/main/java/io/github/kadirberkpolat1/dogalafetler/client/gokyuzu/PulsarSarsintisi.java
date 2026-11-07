package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarSesleri;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Kara delik yutmasının istemci tepkisi ({@code gokyuzu/yutma} paketi, yutma başına bir kez): kütleçekim dalgası sesi
 * konumsuz (dinleyicide, sönümsüz) çalar ve tek seferlik ekran sarsıntısı başlar; genlik sürenin karesiyle söner. Hızlı
 * titreme bileşeni {@code gokyuzu.pulsarTitresim} ile ölçeklenir (0'da yalnız yavaş yuvarlanma kalır, PL-2 erişilebilirlik).
 * Ekrana yazı yoktur (N6).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class PulsarSarsintisi {
    private static long saat;
    private static int kalan;
    private static int toplam;
    private static float genlik;

    private PulsarSarsintisi() {
    }

    public static Consumer<PulsarSesleri.YutmaPaketi> alici() {
        return PulsarSarsintisi::yutma;
    }

    private static void yutma(PulsarSesleri.YutmaPaketi p) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(PulsarSesleri.KARADELIK_YUTMA_KIMLIGI,
                SoundSource.AMBIENT, 1f, 1f, SoundInstance.createUnseededRandom(), false, 0,
                SoundInstance.Attenuation.NONE, 0, 0, 0, true));
        toplam = p.sureTick();
        kalan = toplam;
        genlik = p.siddet();
        DogalAfetler.LOGGER.info("[pulsar] yutma sesi çalındı, sarsıntı {}° {} tick", genlik, toplam);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || Minecraft.getInstance().isPaused()) {
            return;
        }
        saat++;
        if (kalan > 0) {
            kalan--;
        }
    }

    @SubscribeEvent
    public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
        if (kalan <= 0) {
            return;
        }
        double oran = Math.max(0, (kalan - olay.getPartialTick()) / toplam);
        double a = genlik * oran * oran;
        double hizli = a * GokyuzuAyarlari.pulsarTitresim();
        double z = saat + olay.getPartialTick();
        olay.setYaw(olay.getYaw() + (float) (hizli * (Math.sin(z * 2.9) * 0.55 + Math.sin(z * 4.7 + 1) * 0.45)
                + a * 0.6 * Math.sin(z * 0.41)));
        olay.setPitch(olay.getPitch() + (float) (hizli * (Math.sin(z * 3.3 + 2) * 0.55 + Math.sin(z * 5.1) * 0.45)
                + a * 0.4 * Math.sin(z * 0.53 + 1.2)));
        olay.setRoll(olay.getRoll() + (float) (a * 0.6 * Math.sin(z * 0.37 + 0.5)));
    }
}
