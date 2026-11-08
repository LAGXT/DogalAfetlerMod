package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikParcaciklari;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Nefes buharı ve ter damlası parçacık sağlayıcıları (mod veri yolu). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SicaklikParcacikKaydi {
    private SicaklikParcacikKaydi() {
    }

    @SubscribeEvent
    public static void parcaciklar(RegisterParticleProvidersEvent olay) {
        olay.registerSpriteSet(SicaklikParcaciklari.NEFES_BUHARI.get(),
                s -> new SicaklikParcacigi.Saglayici(s, SicaklikParcacigi.Tur.BUHAR));
        olay.registerSpriteSet(SicaklikParcaciklari.TER_DAMLASI.get(),
                s -> new SicaklikParcacigi.Saglayici(s, SicaklikParcacigi.Tur.TER));
    }
}
