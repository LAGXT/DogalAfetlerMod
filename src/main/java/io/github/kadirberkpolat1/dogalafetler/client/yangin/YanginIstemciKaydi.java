package io.github.kadirberkpolat1.dogalafetler.client.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKayitlari;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Yangın parçacıklarının istemci sağlayıcıları (mod veri yolu). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class YanginIstemciKaydi {
    private YanginIstemciKaydi() {
    }

    @SubscribeEvent
    public static void parcaciklar(RegisterParticleProvidersEvent olay) {
        olay.registerSpriteSet(YanginKayitlari.ALEV.get(), s -> new YanginParcacigi.Saglayici(s, YanginParcacigi.Tur.ALEV));
        olay.registerSpriteSet(YanginKayitlari.DUMAN.get(), s -> new YanginParcacigi.Saglayici(s, YanginParcacigi.Tur.DUMAN));
        olay.registerSpriteSet(YanginKayitlari.SUTUN.get(), s -> new YanginParcacigi.Saglayici(s, YanginParcacigi.Tur.SUTUN));
        olay.registerSpriteSet(YanginKayitlari.KIVILCIM.get(),
                s -> new YanginParcacigi.Saglayici(s, YanginParcacigi.Tur.KIVILCIM));
        olay.registerSpriteSet(YanginKayitlari.KUME.get(), s -> new YanginParcacigi.Saglayici(s, YanginParcacigi.Tur.KUME));
    }
}
