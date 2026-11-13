package io.github.kadirberkpolat1.dogalafetler.client.uyum;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.uyum.Uyum;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Punchy sıcaklık entegrasyonunun güvenli istemci olay abonesi. Bu sınıf {@code punchy.api.*} türlerine doğrudan başvurmaz;
 * yalnız {@code Uyum.yuklu(Uyum.PUNCHY)} doğruysa {@link PunchySicaklikKopru}'yu çağırır. Böylece Punchy yüklü olmayan
 * kurulumlarda hiçbir dış sınıf aranmaz.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class PunchyIstemciOlaylari {
    private PunchyIstemciOlaylari() {
    }

    @SubscribeEvent
    public static void istemciTick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || !Uyum.yuklu(Uyum.PUNCHY)) {
            return;
        }
        PunchySicaklikKopru.istemciTick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void cikistaSifirla(ClientPlayerNetworkEvent.LoggingOut olay) {
        if (!Uyum.yuklu(Uyum.PUNCHY)) {
            return;
        }
        PunchySicaklikKopru.sifirla();
    }
}
