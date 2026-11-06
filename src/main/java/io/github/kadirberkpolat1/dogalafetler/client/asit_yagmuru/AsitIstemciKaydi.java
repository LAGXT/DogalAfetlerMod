package io.github.kadirberkpolat1.dogalafetler.client.asit_yagmuru;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.AsitKayitlari;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Asit yağmuru parçacık sağlayıcıları ve ekran efekti katmanı (mod veri yolu). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AsitIstemciKaydi {
    private AsitIstemciKaydi() {
    }

    @SubscribeEvent
    public static void parcaciklar(RegisterParticleProvidersEvent olay) {
        olay.registerSpriteSet(AsitKayitlari.SICRAMA.get(), s -> new AsitParcacigi.Saglayici(s, AsitParcacigi.Tur.SICRAMA));
        olay.registerSpriteSet(AsitKayitlari.DUMAN.get(), s -> new AsitParcacigi.Saglayici(s, AsitParcacigi.Tur.DUMAN));
        olay.registerSpriteSet(AsitKayitlari.BUHAR.get(), s -> new AsitParcacigi.Saglayici(s, AsitParcacigi.Tur.BUHAR));
        olay.registerSpriteSet(AsitKayitlari.BULUT.get(), s -> new AsitParcacigi.Saglayici(s, AsitParcacigi.Tur.BULUT));
    }

    @SubscribeEvent
    public static void katmanlar(RegisterGuiOverlaysEvent olay) {
        olay.registerBelow(VanillaGuiOverlay.FROSTBITE.id(), "asit_yagmuru", AsitEkranEfekti::ciz);
    }
}
