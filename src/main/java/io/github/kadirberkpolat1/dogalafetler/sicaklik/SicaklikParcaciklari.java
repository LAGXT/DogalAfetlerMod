package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Vücut durumu parçacıkları (S3-07, §A8): üşüme ve donmada ağızdan çıkan nefes buharı, bunalma ve yanmada terden düşen
 * damla. Diğer oyunculara sunucu gönderir; oyuncunun kendisine istemci yalnız üçüncü şahısta üretir. Parçacık tanımları
 * datagen'dir ({@link DataGenerators#parcacikEkle}).
 */
public final class SicaklikParcaciklari {
    private static final DeferredRegister<ParticleType<?>> PARCACIKLAR = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, DogalAfetler.MOD_ID);

    public static final RegistryObject<SimpleParticleType> NEFES_BUHARI = PARCACIKLAR.register("nefes_buhari",
            () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> TER_DAMLASI = PARCACIKLAR.register("ter_damlasi",
            () -> new SimpleParticleType(false));

    private SicaklikParcaciklari() {
    }

    public static void kaydet(IEventBus modBus) {
        PARCACIKLAR.register(modBus);
        DataGenerators.parcacikEkle(NEFES_BUHARI, "nefes_buhari", 4);
        DataGenerators.parcacikEkle(TER_DAMLASI, "ter_damlasi", 4);
    }
}
