package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Donmayla oluşan bloklar; yüzey tablosunun hedefi yalnız bunlardır (B4). {@code kiragili_cimen}: {@code SnowyDirtBlock}
 * (üstüne kar gelince {@code snowy=true}), yayılmaz ve toprağa dönmez; böylece yalnız donma/çözülme tarafından değişir.
 */
public final class DonmaBloklari {
    public static final DeferredRegister<Block> BLOKLAR = DeferredRegister.create(ForgeRegistries.BLOCKS,
            DogalAfetler.MOD_ID);

    public static final RegistryObject<Block> KIRAGILI_CIMEN = BLOKLAR.register("kiragili_cimen",
            () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)));

    private DonmaBloklari() {
    }
}
