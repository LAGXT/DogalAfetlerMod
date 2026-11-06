package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Asit yağmurunun kayıtları: dört istemci parçacığı (sıçrama, cızlama dumancığı, yüzey buharı, bulut tavanı) ve hasar
 * tipi. Hasar tipi veri paketi kaydıdır ({@code AsitVerileri}); zırhı delmez, zırh hasarı azaltır ve yıpranır.
 */
public final class AsitKayitlari {
    private static final DeferredRegister<ParticleType<?>> PARCACIKLAR = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, DogalAfetler.MOD_ID);

    public static final RegistryObject<SimpleParticleType> SICRAMA = PARCACIKLAR.register("asit_sicramasi",
            () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DUMAN = PARCACIKLAR.register("asit_dumani",
            () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BUHAR = PARCACIKLAR.register("asit_buhari",
            () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> BULUT = PARCACIKLAR.register("asit_bulutu",
            () -> new SimpleParticleType(false));

    public static final ResourceKey<DamageType> ASIT = ResourceKey.create(Registries.DAMAGE_TYPE, DogalAfetler.konum("asit"));

    private AsitKayitlari() {
    }

    static void kaydet(IEventBus modBus) {
        PARCACIKLAR.register(modBus);
        DataGenerators.parcacikEkle(SICRAMA, "asit_sicramasi", 4);
        DataGenerators.parcacikEkle(DUMAN, "asit_dumani", 4);
        DataGenerators.parcacikEkle(BUHAR, "asit_dumani", 4);
        DataGenerators.parcacikEkle(BULUT, "asit_bulutu", 4);
    }

    public static DamageSource hasar(ServerLevel level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(ASIT));
    }
}
