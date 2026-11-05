package io.github.kadirberkpolat1.dogalafetler.client.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarliklari;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.UcanBlokVarligi;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Kasırga istemci kurulumu (N15: yalnız {@code Dist.CLIENT} abonesi). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IstemciKasirgaKurulumu {
    private IstemciKasirgaKurulumu() {
    }

    @SubscribeEvent
    public static void rendererlar(EntityRenderersEvent.RegisterRenderers olay) {
        olay.registerEntityRenderer(KasirgaVarliklari.KASIRGA.get(), KasirgaRenderer::new);
        olay.registerEntityRenderer(KasirgaVarliklari.UCAN_BLOK.get(), UcanBlokRenderer::new);
    }

    @SubscribeEvent
    public static void kurulum(FMLClientSetupEvent olay) {
        UcanBlokVarligi.istemciVeriKancasi = KasirgaIstemciDurumu::ucanBlokGeldi;
    }
}
