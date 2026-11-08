package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sıcaklık sisteminin kayıtları: zırh eki tarif tipi, {@code islak} gösterge efekti (S3-03) ve iki hasar tipi. Hasar tipleri veri paketi kaydıdır (1.20.1'de
 * {@code damage_type} JSON'u, datagen {@code SicaklikVerileri}); kodda yalnız anahtarları vardır.
 */
public final class SicaklikKayitlari {
    private static final DeferredRegister<RecipeSerializer<?>> SERILESTIRICILER =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DogalAfetler.MOD_ID);

    private static final DeferredRegister<MobEffect> EFEKTLER = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
            DogalAfetler.MOD_ID);

    public static final RegistryObject<RecipeSerializer<ZirhEkiTarifi>> ZIRH_EKI_TARIFI =
            SERILESTIRICILER.register("zirh_eki", ZirhEkiTarifi.Serilestirici::new);

    /** Islak oyuncunun gösterge efekti (ikonu {@code textures/mob_effect/islak.png}). */
    public static final RegistryObject<MobEffect> ISLAK = EFEKTLER.register("islak", () -> new GostergeEtkisi(0x3F76E4));

    public static final ResourceKey<DamageType> DONMA =
            ResourceKey.create(Registries.DAMAGE_TYPE, DogalAfetler.konum("donma"));
    public static final ResourceKey<DamageType> ASIRI_ISINMA =
            ResourceKey.create(Registries.DAMAGE_TYPE, DogalAfetler.konum("asiri_isinma"));

    private SicaklikKayitlari() {
    }

    static void kaydet(IEventBus modBus) {
        SERILESTIRICILER.register(modBus);
        EFEKTLER.register(modBus);
    }

    public static DamageSource hasar(ServerLevel level, ResourceKey<DamageType> tur) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(tur));
    }
}
