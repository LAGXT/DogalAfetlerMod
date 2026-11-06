package io.github.kadirberkpolat1.dogalafetler.client.orman_yangini;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Orman yangını ısı dalgalanması katmanı (mod veri yolu). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class OrmanIstemciKaydi {
    private OrmanIstemciKaydi() {
    }

    @SubscribeEvent
    public static void katmanlar(RegisterGuiOverlaysEvent olay) {
        olay.registerBelow(VanillaGuiOverlay.FROSTBITE.id(), "orman_yangini_isi", IstemciOrmanYangini::isiKatmani);
    }
}
