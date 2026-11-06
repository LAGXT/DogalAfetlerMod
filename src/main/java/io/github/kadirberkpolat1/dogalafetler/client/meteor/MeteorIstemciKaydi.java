package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorKayitlari;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Meteor varlık çizicisi, parçacık sağlayıcıları ve çarpma parlaması katmanı (mod veri yolu). */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MeteorIstemciKaydi {
    private MeteorIstemciKaydi() {
    }

    @SubscribeEvent
    public static void cizici(EntityRenderersEvent.RegisterRenderers olay) {
        olay.registerEntityRenderer(MeteorKayitlari.METEOR.get(), MeteorCizici::new);
    }

    @SubscribeEvent
    public static void parcaciklar(RegisterParticleProvidersEvent olay) {
        olay.registerSpriteSet(MeteorKayitlari.PLAZMA.get(), s -> new MeteorParcacigi.Saglayici(s, MeteorParcacigi.Tur.PLAZMA));
        olay.registerSpriteSet(MeteorKayitlari.PLAZMA_AY.get(),
                s -> new MeteorParcacigi.Saglayici(s, MeteorParcacigi.Tur.PLAZMA_AY));
        olay.registerSpriteSet(MeteorKayitlari.DUMAN.get(), s -> new MeteorParcacigi.Saglayici(s, MeteorParcacigi.Tur.DUMAN));
        olay.registerSpriteSet(MeteorKayitlari.KIVILCIM.get(),
                s -> new MeteorParcacigi.Saglayici(s, MeteorParcacigi.Tur.KIVILCIM));
        olay.registerSpriteSet(MeteorKayitlari.KIVILCIM_AY.get(),
                s -> new MeteorParcacigi.Saglayici(s, MeteorParcacigi.Tur.KIVILCIM_AY));
        olay.registerSpriteSet(MeteorKayitlari.TOZ.get(), s -> new MeteorParcacigi.Saglayici(s, MeteorParcacigi.Tur.TOZ));
        olay.registerSpriteSet(MeteorKayitlari.PARILTI.get(),
                s -> new MeteorParcacigi.Saglayici(s, MeteorParcacigi.Tur.PARILTI));
        olay.registerSpriteSet(MeteorKayitlari.KOZMIK.get(), s -> new MeteorParcacigi.Saglayici(s, MeteorParcacigi.Tur.KOZMIK));
    }

    @SubscribeEvent
    public static void katmanlar(RegisterGuiOverlaysEvent olay) {
        olay.registerAboveAll("meteor_parlamasi", (gui, cizim, kismiTick, genislik, yukseklik) -> {
            int alfa = IstemciMeteor.parlamaAlfasi(kismiTick);
            if (alfa > 0) {
                cizim.fill(0, 0, genislik, yukseklik, (alfa << 24) | 0xFFF6E8);
            }
        });
    }
}
