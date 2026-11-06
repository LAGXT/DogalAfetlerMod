package io.github.kadirberkpolat1.dogalafetler.afet.deprem;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
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
 * Deprem blok sınıfları (N12). {@code dogalafetler:deprem_kirilgan}: depremde yerinden kopan "sağlam olmayan" bloklar
 * (yaprak, kalas, kütük dışı ahşap, cam, çit, fener, saksı; tasarım notu deprem §6); bu sınıfın tek yazarı bu
 * sağlayıcıdır, veri paketiyle genişletilebilir. {@code dogalafetler:deprem_dayanikli} (taş, deepslate, obsidyen, cevher) da
 * burada yazılır (K-E12 tek yazar; Faz 4 entegrasyonunda yangın sağlayıcısından taşındı, çıktı bayt bayt aynı).
 */
public final class DepremEtiketleri {
    public static final TagKey<Block> KIRILGAN = TagKey.create(Registries.BLOCK, DogalAfetler.konum("deprem_kirilgan"));
    public static final TagKey<Block> DAYANIKLI = TagKey.create(Registries.BLOCK, DogalAfetler.konum("deprem_dayanikli"));

    private DepremEtiketleri() {
    }

    static void ekle(GatherDataEvent olay) {
        olay.getGenerator().addProvider(olay.includeServer(), new Saglayici(olay.getGenerator().getPackOutput(),
                olay.getLookupProvider(), olay.getExistingFileHelper()));
    }

    private static final class Saglayici extends BlockTagsProvider {
        private static final ResourceLocation FORGE_CAM_PANELLERI = ResourceLocation.fromNamespaceAndPath("forge",
                "glass_panes");
        private static final ResourceLocation FORGE_CEVHERLERI = ResourceLocation.fromNamespaceAndPath("forge", "ores");

        @SuppressWarnings("unchecked")
        private static final TagKey<Block>[] CEVHERLER = new TagKey[] {BlockTags.COAL_ORES, BlockTags.IRON_ORES,
                BlockTags.COPPER_ORES, BlockTags.GOLD_ORES, BlockTags.REDSTONE_ORES, BlockTags.LAPIS_ORES,
                BlockTags.DIAMOND_ORES, BlockTags.EMERALD_ORES};

        private static final Block[] SERT = {Blocks.BEDROCK, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN,
                Blocks.REINFORCED_DEEPSLATE, Blocks.COBBLED_DEEPSLATE, Blocks.END_STONE, Blocks.NETHER_QUARTZ_ORE,
                Blocks.ANCIENT_DEBRIS};

        Saglayici(PackOutput cikti, CompletableFuture<HolderLookup.Provider> arama, ExistingFileHelper dosyalar) {
            super(cikti, arama, DogalAfetler.MOD_ID, dosyalar);
        }

        @Override
        public String getName() {
            return "Deprem blok etiketleri";
        }

        @Override
        protected void addTags(HolderLookup.Provider arama) {
            tag(KIRILGAN).addTags(BlockTags.LEAVES, BlockTags.PLANKS, BlockTags.WOODEN_STAIRS, BlockTags.WOODEN_SLABS,
                    BlockTags.WOODEN_FENCES, BlockTags.FENCE_GATES, BlockTags.WOODEN_TRAPDOORS, BlockTags.IMPERMEABLE,
                    BlockTags.FLOWER_POTS)
                    .add(Blocks.GLASS_PANE, Blocks.LANTERN, Blocks.SOUL_LANTERN, Blocks.BOOKSHELF)
                    .addOptionalTag(FORGE_CAM_PANELLERI);
            tag(DAYANIKLI).add(SERT).addTag(BlockTags.BASE_STONE_OVERWORLD).addTags(CEVHERLER)
                    .addOptionalTag(FORGE_CEVHERLERI);
        }
    }
}
