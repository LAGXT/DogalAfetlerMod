package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Donmayla oluşan bloklar; yüzey tablosunun hedefi yalnız bunlardır (K0-24). {@code kiragili_cimen}: çimen özelliklerinde
 * {@code SnowyDirtBlock}, yayılmaz ve toprağa dönmez; yalnız donma/çözülmeyle değişir. {@code ince_buz}: su sütununun üst
 * bloğunun ilk (donarken) ve son (çözülürken) evresi; koyu, yarı saydam, üstünde yürünür, buz gibi kayar ve ışıkta erir.
 */
public final class DonmaBloklari {
    private static final DeferredRegister<Block> BLOKLAR = DeferredRegister.create(ForgeRegistries.BLOCKS,
            DogalAfetler.MOD_ID);
    private static final DeferredRegister<Item> ESYALAR = DeferredRegister.create(ForgeRegistries.ITEMS,
            DogalAfetler.MOD_ID);

    public static final RegistryObject<Block> KIRAGILI_CIMEN = BLOKLAR.register("kiragili_cimen",
            () -> new SnowyDirtBlock(BlockBehaviour.Properties.copy(Blocks.GRASS_BLOCK)));
    public static final RegistryObject<Block> INCE_BUZ = BLOKLAR.register("ince_buz",
            () -> new IceBlock(BlockBehaviour.Properties.copy(Blocks.ICE).strength(0.3f)));

    static {
        ESYALAR.register("kiragili_cimen", () -> new BlockItem(KIRAGILI_CIMEN.get(), new Item.Properties()));
        ESYALAR.register("ince_buz", () -> new BlockItem(INCE_BUZ.get(), new Item.Properties()));
    }

    private DonmaBloklari() {
    }

    static void kaydet(IEventBus modBus) {
        BLOKLAR.register(modBus);
        ESYALAR.register(modBus);
    }
}
