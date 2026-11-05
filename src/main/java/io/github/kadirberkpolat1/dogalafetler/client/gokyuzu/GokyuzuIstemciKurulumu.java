package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuPaketi;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.DimensionSpecialEffectsManager;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

/** Mod veri yolunda istemci kurulumu: sarmalayıcı efekt kaydı ve paket alıcısı. */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GokyuzuIstemciKurulumu {
    private GokyuzuIstemciKurulumu() {
    }

    /**
     * Forge 1.20.1 bu olayı mod yükleme sırasıyla her moda ayrı iletir ve yalnız {@code register} sunar; o ana kadar
     * kayıtlı nesneyi okumak için olayın {@code effects} haritasına yansımayla bakılır. Okunamazsa vanilla efekti sarılır.
     */
    @SubscribeEvent
    public static void efektKaydet(RegisterDimensionSpecialEffectsEvent olay) {
        ResourceLocation kimlik = BuiltinDimensionTypes.OVERWORLD_EFFECTS;
        DimensionSpecialEffects onceki = null;
        try {
            Field alan = RegisterDimensionSpecialEffectsEvent.class.getDeclaredField("effects");
            alan.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, DimensionSpecialEffects> harita =
                    (Map<ResourceLocation, DimensionSpecialEffects>) alan.get(olay);
            onceki = harita.get(kimlik);
            DogalAfetler.LOGGER.info("[gokyuzu] kanca b: kayıtlı overworld efekti yansımayla okundu: {}",
                    onceki == null ? "yok" : onceki.getClass().getName());
        } catch (ReflectiveOperationException | RuntimeException e) {
            DogalAfetler.LOGGER.warn("[gokyuzu] kanca b: önceki efekt okunamadı ({}), vanilla sarılıyor", e.toString());
        }
        if (onceki == null) {
            onceki = new DimensionSpecialEffects.OverworldEffects();
        }
        olay.register(kimlik, new SarmalayanGokyuzuEfekti(onceki));
    }

    @SubscribeEvent
    public static void istemciKurulumu(FMLClientSetupEvent olay) {
        GokyuzuPaketi.istemciAlicisi(IstemciGokyuzu::durumAl);
    }

    /** Bizden sonra yüklenen bir mod overworld efektini değiştirdiyse görünür olsun diye denetlenir. */
    @SubscribeEvent
    public static void yuklemeBitti(FMLLoadCompleteEvent olay) {
        DimensionSpecialEffects son = DimensionSpecialEffectsManager.getForType(BuiltinDimensionTypes.OVERWORLD_EFFECTS);
        if (son instanceof SarmalayanGokyuzuEfekti s) {
            DogalAfetler.LOGGER.info("[gokyuzu] kanca b etkin: overworld efekti bizim sarmalayıcımız, iç: {}",
                    s.ic().getClass().getName());
        } else {
            IstemciGokyuzu.kanca = IstemciGokyuzu.Kanca.AFTER_SKY;
            DogalAfetler.LOGGER.warn("[gokyuzu] overworld efektini bizden sonra başka bir mod değiştirdi ({}); "
                    + "yedek kanca c (AFTER_SKY) kullanılacak", son.getClass().getName());
        }
    }
}
