package io.github.kadirberkpolat1.dogalafetler.client.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** İstemci olayları (N15): saat ilerletme ve sunucudan çıkışta önbellekleri sıfırlama. */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciCekirdekOlaylari {
    private IstemciCekirdekOlaylari() {
    }

    @SubscribeEvent
    public static void istemciTick(TickEvent.ClientTickEvent olay) {
        if (olay.phase == TickEvent.Phase.END) {
            IstemciDonem.tickIlerlet();
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        IstemciDonem.sifirla();
        IstemciAfetDurumu.sifirla();
    }
}
