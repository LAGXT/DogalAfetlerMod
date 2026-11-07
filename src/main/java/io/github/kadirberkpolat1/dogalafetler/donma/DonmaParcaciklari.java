package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Donmanın parçacık türleri; ikisi de aynı dört kar tanesi karesini kullanır, kare parçacıkta rastgele seçilir. */
public final class DonmaParcaciklari {
    private static final DeferredRegister<ParticleType<?>> PARCACIKLAR = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, DogalAfetler.MOD_ID);

    /** Kar yağışının tanesi. */
    public static final RegistryObject<SimpleParticleType> KAR_TANESI = PARCACIKLAR.register("kar_tanesi",
            () -> new SimpleParticleType(false));
    /** Ayazın havada süzülen minik buz kristali. */
    public static final RegistryObject<SimpleParticleType> BUZ_KRISTALI = PARCACIKLAR.register("buz_kristali",
            () -> new SimpleParticleType(false));

    private DonmaParcaciklari() {
    }

    static void kaydet(IEventBus modBus) {
        PARCACIKLAR.register(modBus);
        DataGenerators.parcacikEkle(KAR_TANESI, "kar_tanesi", 4);
        DataGenerators.parcacikEkle(BUZ_KRISTALI, "kar_tanesi", 4);
    }
}
