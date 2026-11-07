package io.github.kadirberkpolat1.dogalafetler.client.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaParcaciklari;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Donma parçacıklarının istemci sağlayıcıları (mod veri yolu). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DonmaIstemciKaydi {
    private DonmaIstemciKaydi() {
    }

    @SubscribeEvent
    public static void parcaciklar(RegisterParticleProvidersEvent olay) {
        olay.registerSpriteSet(DonmaParcaciklari.KAR_TANESI.get(),
                s -> new KarTanesiParcacigi.Saglayici(s, KarTanesiParcacigi.Tur.KAR));
        olay.registerSpriteSet(DonmaParcaciklari.BUZ_KRISTALI.get(),
                s -> new KarTanesiParcacigi.Saglayici(s, KarTanesiParcacigi.Tur.KRISTAL));
    }
}
