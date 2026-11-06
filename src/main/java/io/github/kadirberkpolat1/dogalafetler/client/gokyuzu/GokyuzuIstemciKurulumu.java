package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.google.common.collect.ImmutableMap;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.dimension.BuiltinDimensionTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.DimensionSpecialEffectsManager;
import net.minecraftforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

/**
 * Mod veri yolunda istemci kurulumu. Kanca üç kademelidir: (1) kayıt olayında o ana kadar kayıtlı overworld efektini sar;
 * (2) yükleme sonunda bizden sonra kayıt yapan bir mod bizi ezdiyse Forge'un efekt haritasını yeniden kurup kazananı
 * sar; (3) o da olmazsa yedek kanca (c) {@code AFTER_SKY} — bilinen sınır: ay ve güneş gizlenemez.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GokyuzuIstemciKurulumu {
    private static final ResourceLocation OVERWORLD = BuiltinDimensionTypes.OVERWORLD_EFFECTS;

    private GokyuzuIstemciKurulumu() {
    }

    /**
     * Forge 1.20.1 bu olayı mod yükleme sırasıyla her moda ayrı iletir ve yalnız {@code register} sunar; o ana kadar
     * kayıtlı nesneyi okumak için olayın {@code effects} haritasına yansımayla bakılır. Okunamazsa kayıt yapılmaz
     * (bizden önceki modun efekti silinmesin); yükleme sonunda kazanan sarılır.
     */
    @SubscribeEvent
    public static void efektKaydet(RegisterDimensionSpecialEffectsEvent olay) {
        DimensionSpecialEffects onceki;
        try {
            Field alan = RegisterDimensionSpecialEffectsEvent.class.getDeclaredField("effects");
            alan.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, DimensionSpecialEffects> harita =
                    (Map<ResourceLocation, DimensionSpecialEffects>) alan.get(olay);
            onceki = harita.get(OVERWORLD);
            DogalAfetler.LOGGER.info("[gokyuzu] kanca b: kayıtlı overworld efekti yansımayla okundu: {}",
                    onceki == null ? "yok" : onceki.getClass().getName());
        } catch (ReflectiveOperationException | RuntimeException e) {
            DogalAfetler.LOGGER.warn("[gokyuzu] kanca b: kayıtlı efekt okunamadı ({}); kayıt yapılmıyor, yükleme sonunda "
                    + "kazanan sarılacak", e.toString());
            return;
        }
        if (onceki == null) {
            onceki = new DimensionSpecialEffects.OverworldEffects();
        }
        olay.register(OVERWORLD, new SarmalayanGokyuzuEfekti(onceki));
    }

    /**
     * Efekt haritası {@code ClientLevel} yapıcısında (dünyaya girişte) okunur; yükleme sonu henüz ondan öncedir. Harita ana
     * thread'de değiştirilir.
     */
    @SubscribeEvent
    public static void yuklemeBitti(FMLLoadCompleteEvent olay) {
        olay.enqueueWork(GokyuzuIstemciKurulumu::kancayiDogrula);
    }

    private static void kancayiDogrula() {
        DimensionSpecialEffects son = DimensionSpecialEffectsManager.getForType(OVERWORLD);
        if (son instanceof SarmalayanGokyuzuEfekti s) {
            DogalAfetler.LOGGER.info("[gokyuzu] kanca b etkin: overworld efekti bizim sarmalayıcımız, iç: {}",
                    s.ic().getClass().getName());
            return;
        }
        DogalAfetler.LOGGER.warn("[gokyuzu] ezildi: overworld efekti bizim sarmalayıcımız değil ({}); kazanan sarılacak",
                son.getClass().getName());
        try {
            Field alan = DimensionSpecialEffectsManager.class.getDeclaredField(SahteGokyuzuModu.efektAlaniAdi());
            alan.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<ResourceLocation, DimensionSpecialEffects> eski =
                    (Map<ResourceLocation, DimensionSpecialEffects>) alan.get(null);
            Map<ResourceLocation, DimensionSpecialEffects> yeni = new LinkedHashMap<>(eski);
            yeni.put(OVERWORLD, new SarmalayanGokyuzuEfekti(son));
            alan.set(null, ImmutableMap.copyOf(yeni));
            DimensionSpecialEffects kontrol = DimensionSpecialEffectsManager.getForType(OVERWORLD);
            if (!(kontrol instanceof SarmalayanGokyuzuEfekti)) {
                throw new IllegalStateException("yeniden kurulan harita okunmuyor: " + kontrol.getClass().getName());
            }
            DogalAfetler.LOGGER.info("[gokyuzu] EFFECTS yeniden kuruldu: {} sarıldı (kanca b, kazananı sar)",
                    son.getClass().getName());
        } catch (ReflectiveOperationException | RuntimeException e) {
            IstemciGokyuzu.kanca = IstemciGokyuzu.Kanca.AFTER_SKY;
            DogalAfetler.LOGGER.warn("[gokyuzu] EFFECTS yeniden kurulamadı ({}); yedek kanca c (AFTER_SKY) kullanılacak",
                    e.toString());
            DogalAfetler.LOGGER.warn("[gokyuzu] BİLİNEN SINIR: kanca c'de {} kendi ayını ve güneşini çizer, gizlenemez; "
                    + "aysız gece ve güneşsiz dönemde ay/güneş görünmeye devam eder (bölüm H, D)", son.getClass().getName());
        }
    }
}
