package io.github.kadirberkpolat1.dogalafetler.donma.veri;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaBloklari;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaIsleyici;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaTablosuYukleyici;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Donma sisteminin datagen çıktıları (GOREV §3, N12): dönüşüm tablosu veri paketi ({@code data/dogalafetler/donma/vanilla.json}),
 * {@code dogalafetler:donma/*} blok etiketleri (vanilla etiketlere kendi bloklarımız ortak sağlayıcıdan, {@code DonmaSistemi.kaydet},
 * K-E12), blok loot tabloları, blok durumu ve modeller (doku dosyaları {@code tools/assets/donma_dokular.py} üretir).
 */
public final class DonmaVeriSaglayicilari {
    private DonmaVeriSaglayicilari() {
    }

    public static void ekle(GatherDataEvent olay) {
        DataGenerator uretici = olay.getGenerator();
        PackOutput cikti = uretici.getPackOutput();
        ExistingFileHelper dosyalar = olay.getExistingFileHelper();
        uretici.addProvider(olay.includeServer(), new TabloSaglayici(cikti));
        uretici.addProvider(olay.includeServer(), new Etiketler(cikti, olay.getLookupProvider(), dosyalar));
        uretici.addProvider(olay.includeServer(), new LootTableProvider(cikti, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(BlokLootu::new, LootContextParamSets.BLOCK))));
        uretici.addProvider(olay.includeClient(), new Modeller(cikti, dosyalar));
    }

    /**
     * Varsayılan dönüşüm tablosu: yalnız su sütunu (su → ince buz → buz). Çimen blok olarak değişmez; kar örtüsüyle karlı
     * çimen olur (kar katmanı ekle ↔ kaldır). Yüzey satırları veri paketiyle eklenebilir.
     */
    private record TabloSaglayici(PackOutput cikti) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput yazici) {
            JsonArray donusumler = new JsonArray();
            JsonObject su = new JsonObject();
            su.addProperty("tur", "su_sutunu");
            su.addProperty("kaynak", "minecraft:water");
            su.addProperty("hedef", "minecraft:ice");
            su.addProperty("ince", DogalAfetler.konum("ince_buz").toString());
            donusumler.add(su);
            JsonObject kok = new JsonObject();
            kok.add("donusumler", donusumler);
            return DataProvider.saveStable(yazici, kok, cikti.getOutputFolder(PackOutput.Target.DATA_PACK)
                    .resolve(DogalAfetler.MOD_ID).resolve(DonmaTablosuYukleyici.KLASOR).resolve("vanilla.json"));
        }

        @Override
        public String getName() {
            return "Donma dönüşüm tablosu";
        }
    }

    private static final class Etiketler extends BlockTagsProvider {
        Etiketler(PackOutput cikti, CompletableFuture<HolderLookup.Provider> arama, ExistingFileHelper dosyalar) {
            super(cikti, arama, DogalAfetler.MOD_ID, dosyalar);
        }

        /** DataGenerator sağlayıcıları ada göre ayırır; varsayılan ad mod başına tek blok etiketi sağlayıcısına izin verir. */
        @Override
        public String getName() {
            return "Donma blok etiketleri";
        }

        @Override
        protected void addTags(HolderLookup.Provider arama) {
            tag(DonmaIsleyici.KAR_ZEMINI).addTag(BlockTags.DIRT).addTag(BlockTags.LEAVES).addTag(BlockTags.SAND)
                    .addTag(BlockTags.BASE_STONE_OVERWORLD).add(Blocks.GRAVEL, Blocks.CLAY);
            // Üstü düz yaygın yapı blokları (tam bloklar; basamak ve merdiven yok): kar kalın buz aşamasında gelir (D8).
            tag(DonmaIsleyici.YAPI_KAR_ZEMINI).addTag(BlockTags.PLANKS).addTag(BlockTags.LOGS).addTag(BlockTags.STONE_BRICKS)
                    .add(Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE, Blocks.BRICKS, Blocks.MUD_BRICKS, Blocks.SMOOTH_STONE,
                            Blocks.POLISHED_ANDESITE, Blocks.POLISHED_DIORITE, Blocks.POLISHED_GRANITE, Blocks.COBBLED_DEEPSLATE,
                            Blocks.POLISHED_DEEPSLATE, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.SANDSTONE,
                            Blocks.SMOOTH_SANDSTONE, Blocks.CUT_SANDSTONE, Blocks.RED_SANDSTONE, Blocks.SMOOTH_RED_SANDSTONE,
                            Blocks.CUT_RED_SANDSTONE, Blocks.QUARTZ_BLOCK, Blocks.SMOOTH_QUARTZ, Blocks.TERRACOTTA);
            // Pulsar külü kar altında kalır (PL-5): kül bloğu doğal zemin, kömürleşmiş kütük yapı gibi geç karlanır.
            tag(DonmaIsleyici.KAR_ZEMINI).add(PulsarKayitlari.KUL_BLOGU.get());
            tag(DonmaIsleyici.YAPI_KAR_ZEMINI).add(PulsarKayitlari.KOMURLESMIS_KUTUK.get());
        }
    }

    private static final class BlokLootu extends BlockLootSubProvider {
        BlokLootu() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            // İpek dokunuş kırağılı çimeni değil çimeni verir: blok oyuncunun eline geçmez (K0-24 hedef kuralı, D9).
            add(DonmaBloklari.KIRAGILI_CIMEN.get(), b -> LootTable.lootTable().withPool(LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1.0F))
                    .add(AlternativesEntry.alternatives(LootItem.lootTableItem(Blocks.GRASS_BLOCK).when(HAS_SILK_TOUCH),
                            applyExplosionCondition(b, LootItem.lootTableItem(Blocks.DIRT))))));
            dropWhenSilkTouch(DonmaBloklari.INCE_BUZ.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(DonmaBloklari.KIRAGILI_CIMEN.get(), DonmaBloklari.INCE_BUZ.get());
        }
    }

    private static final class Modeller extends BlockStateProvider {
        Modeller(PackOutput cikti, ExistingFileHelper dosyalar) {
            super(cikti, DogalAfetler.MOD_ID, dosyalar);
        }

        @Override
        protected void registerStatesAndModels() {
            ModelFile kiragili = models().cubeBottomTop("kiragili_cimen", mcLoc("block/grass_block_snow"),
                    mcLoc("block/dirt"), modLoc("block/kiragili_cimen_ust")).texture("particle", modLoc("block/kiragili_cimen_ust"));
            ModelFile karli = models().getExistingFile(mcLoc("block/grass_block_snow"));
            getVariantBuilder(DonmaBloklari.KIRAGILI_CIMEN.get())
                    .partialState().with(SnowyDirtBlock.SNOWY, false).modelForState().modelFile(kiragili).addModel()
                    .partialState().with(SnowyDirtBlock.SNOWY, true).modelForState().modelFile(karli).addModel();
            simpleBlockItem(DonmaBloklari.KIRAGILI_CIMEN.get(), kiragili);
            ModelFile ince = models().cubeAll("ince_buz", modLoc("block/ince_buz")).renderType("translucent");
            simpleBlock(DonmaBloklari.INCE_BUZ.get(), ince);
            simpleBlockItem(DonmaBloklari.INCE_BUZ.get(), ince);
        }
    }
}
