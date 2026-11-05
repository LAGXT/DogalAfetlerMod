package io.github.kadirberkpolat1.dogalafetler.client.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.uyari.SirenPaketleri;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** İstemci kurulumu: siren paketlerinin istemci tarafını bağlar (N15: yalnız {@code Dist.CLIENT} abonesi). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IstemciUyariKurulumu {
    private IstemciUyariKurulumu() {
    }

    @SubscribeEvent
    public static void istemciKurulumu(FMLClientSetupEvent olay) {
        SirenPaketleri.istemciyiBagla(SirenCalar.ORNEK);
    }
}
