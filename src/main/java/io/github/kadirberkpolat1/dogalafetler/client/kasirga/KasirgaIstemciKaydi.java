package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.UcanBlokVarligi;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Kasırga varlık çizicileri ve uçan blok veri kancası (mod veri yolu, yalnız istemci). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KasirgaIstemciKaydi {
    private KasirgaIstemciKaydi() {
    }

    @SubscribeEvent
    public static void cizici(EntityRenderersEvent.RegisterRenderers olay) {
        olay.registerEntityRenderer(KasirgaKayitlari.KASIRGA.get(), KasirgaRenderer::new);
        olay.registerEntityRenderer(KasirgaKayitlari.UCAN_BLOK.get(), UcanBlokRenderer::new);
    }

    @SubscribeEvent
    public static void kurulum(FMLClientSetupEvent olay) {
        UcanBlokVarligi.istemciVeriKancasi = IstemciKasirga::ucanBlokGeldi;
    }
}
