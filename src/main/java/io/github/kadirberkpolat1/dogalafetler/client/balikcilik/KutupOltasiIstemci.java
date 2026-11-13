package io.github.kadirberkpolat1.dogalafetler.client.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikKayitlari;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.entity.EntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Kutup Oltası'nın istemci kaydı (yalnız istemci): "cast" model özelliği (vanilla olta yalnız kendi eşyasına kayıtlıdır; atılmış
 * olta modeli {@code kutup_oltasi_cast} bu kayıtla Kutup Oltası'nda da görünür, mantık vanilla ile aynı) ve nadir bir şey
 * gelirken şamandırayı sarı çizen şamandıra çizicisi ({@link SariSamandiraCizici}; vanilla çizicinin yerini alır).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KutupOltasiIstemci {
    private KutupOltasiIstemci() {
    }

    /** Vanilla {@code FISHING_BOBBER} çizicisinin yerine, aynı çizimi yapan ama işaretli şamandırayı sarı çizen çizici. */
    @SubscribeEvent
    public static void cizici(EntityRenderersEvent.RegisterRenderers olay) {
        olay.registerEntityRenderer(EntityType.FISHING_BOBBER, SariSamandiraCizici::new);
    }

    @SubscribeEvent
    public static void kurulum(FMLClientSetupEvent olay) {
        olay.enqueueWork(() -> ItemProperties.register(BalikcilikKayitlari.KUTUP_OLTASI.get(), new ResourceLocation("cast"),
                (yigin, level, varlik, tohum) -> {
                    if (varlik == null) {
                        return 0.0f;
                    }
                    boolean ana = varlik.getMainHandItem() == yigin;
                    boolean yan = varlik.getOffhandItem() == yigin;
                    if (varlik.getMainHandItem().getItem() instanceof FishingRodItem) {
                        yan = false;
                    }
                    return (ana || yan) && varlik instanceof Player oyuncu && oyuncu.fishing != null ? 1.0f : 0.0f;
                }));
    }
}
