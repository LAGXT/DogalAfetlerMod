package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * N12 etiketlerinin varsayılanları: taş, deepslate, obsidyen, cevherler, bedrock vb. Başka modların cevherleri isteğe bağlı
 * {@code forge:ores} ile girer; veri paketi etiketi genişletebilir.
 */
final class YanginEtiketSaglayici extends BlockTagsProvider {
    private static final ResourceLocation FORGE_CEVHERLERI = ResourceLocation.fromNamespaceAndPath("forge", "ores");

    @SuppressWarnings("unchecked")
    private static final TagKey<Block>[] CEVHERLER = new TagKey[] {BlockTags.COAL_ORES, BlockTags.IRON_ORES,
            BlockTags.COPPER_ORES, BlockTags.GOLD_ORES, BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES,
            BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES};

    private static final Block[] SERT = {Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN,
            Blocks.REINFORCED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.END_STONE, Blocks.NETHER_QUARTZ_ORE,
            Blocks.ANCIENT_DEBRIS};

    private YanginEtiketSaglayici(PackOutput cikti, CompletableFuture<HolderLookup.Provider> arama,
            ExistingFileHelper dosyalar) {
        super(cikti, arama, DogalAfetler.MOD_ID, dosyalar);
    }

    static void ekle(GatherDataEvent olay) {
        olay.getGenerator().addProvider(olay.includeServer(), new YanginEtiketSaglayici(
                olay.getGenerator().getPackOutput(), olay.getLookupProvider(), olay.getExistingFileHelper()));
    }

    @Override
    protected void addTags(HolderLookup.Provider arama) {
        tag(YanginEtiketleri.DEPREM_DAYANIKLI).add(SERT).addTag(BlockTags.BASE_STONE_OVERWORLD).addTags(CEVHERLER)
                .addOptionalTag(FORGE_CEVHERLERI);
        tag(YanginEtiketleri.ASIT_DAYANIKLI).add(SERT).addTag(BlockTags.BASE_STONE_OVERWORLD).addTags(CEVHERLER)
                .addOptionalTag(FORGE_CEVHERLERI);
        tag(YanginEtiketleri.YANMAZ).add(SERT)
                .add(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.SMOOTH_STONE, Blocks.DEEPSLATE_BRICKS,
                        Blocks.DEEPSLATE_TILES)
                .addTag(BlockTags.BASE_STONE_OVERWORLD).addTag(BlockTags.BASE_STONE_NETHER).addTag(BlockTags.STONE_BRICKS)
                .addTags(CEVHERLER).addOptionalTag(FORGE_CEVHERLERI);
    }
}
