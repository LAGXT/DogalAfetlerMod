package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Yangın bloğu ve parçacık türleri. */
public final class YanginKayitlari {
    private static final DeferredRegister<Block> BLOKLAR = DeferredRegister.create(ForgeRegistries.BLOCKS,
            DogalAfetler.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARCACIKLAR = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, DogalAfetler.MOD_ID);

    public static final RegistryObject<AfetAtesiBlogu> AFET_ATESI = BLOKLAR.register("afet_atesi",
            () -> new AfetAtesiBlogu(BlockBehaviour.Properties.of().mapColor(MapColor.FIRE).replaceable().noCollission()
                    .instabreak().lightLevel(durum -> 15).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY)
                    .noLootTable()));
    public static final RegistryObject<SimpleParticleType> ALEV = PARCACIKLAR.register("yangin_alevi",
            () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> DUMAN = PARCACIKLAR.register("yangin_dumani",
            () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SUTUN = PARCACIKLAR.register("yangin_sutunu",
            () -> new SimpleParticleType(true));

    private YanginKayitlari() {
    }

    static void kaydet(IEventBus modBus) {
        BLOKLAR.register(modBus);
        PARCACIKLAR.register(modBus);
    }
}
