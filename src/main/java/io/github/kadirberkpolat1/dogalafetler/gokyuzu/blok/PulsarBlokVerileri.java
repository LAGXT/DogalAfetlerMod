package io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Direction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Pulsar bloklarının datagen çıktıları: loot tabloları (kül katmanı kat sayısı kadar kendini, diğerleri kendini), blok
 * durumları ve modeller (kül katmanı vanilla kar katmanı gibi 2/4/…/14 piksel kutu, 8 kat kül bloğu modeli; kömürleşmiş
 * kütük eksenli sütun). Doku dosyalarını {@code tools/assets/pulsar_dokular.py} üretir.
 */
public final class PulsarBlokVerileri {
    private static final ResourceLocation KUL = DogalAfetler.konum("block/kul");

    private PulsarBlokVerileri() {
    }

    public static void ekle(GatherDataEvent olay) {
        DataGenerator uretici = olay.getGenerator();
        PackOutput cikti = uretici.getPackOutput();
        ExistingFileHelper dosyalar = olay.getExistingFileHelper();
        uretici.addProvider(olay.includeServer(), new Adli("Pulsar blok loot tabloları", new LootTableProvider(cikti,
                Set.of(), List.of(new LootTableProvider.SubProviderEntry(BlokLootu::new, LootContextParamSets.BLOCK)))));
        uretici.addProvider(olay.includeClient(), new Modeller(cikti, dosyalar));
    }

    /** {@code getName()}'i final olan vanilla sağlayıcıya benzersiz ad (K-E5). */
    private record Adli(String ad, DataProvider ic) implements DataProvider {
        @Override
        public CompletableFuture<?> run(CachedOutput yazici) {
            return ic.run(yazici);
        }

        @Override
        public String getName() {
            return ad;
        }
    }

    private static final class BlokLootu extends BlockLootSubProvider {
        BlokLootu() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            Block kul = PulsarKayitlari.KUL_KATMANI.get();
            LootPool.Builder havuz = LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F));
            for (int kat = 1; kat <= 8; kat++) {
                havuz.add(LootItem.lootTableItem(kul)
                        .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(kul)
                                .setProperties(StatePropertiesPredicate.Builder.properties()
                                        .hasProperty(SnowLayerBlock.LAYERS, kat)))
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(kat))));
            }
            add(kul, LootTable.lootTable().withPool(applyExplosionCondition(kul, havuz)));
            dropSelf(PulsarKayitlari.KUL_BLOGU.get());
            dropSelf(PulsarKayitlari.KOMURLESMIS_KUTUK.get());
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(PulsarKayitlari.KUL_KATMANI.get(), PulsarKayitlari.KUL_BLOGU.get(),
                    PulsarKayitlari.KOMURLESMIS_KUTUK.get());
        }
    }

    private static final class Modeller extends BlockStateProvider {
        Modeller(PackOutput cikti, ExistingFileHelper dosyalar) {
            super(cikti, DogalAfetler.MOD_ID, dosyalar);
        }

        @Override
        public String getName() {
            return "Pulsar blok durumları ve modeller";
        }

        @Override
        protected void registerStatesAndModels() {
            ModelFile blok = models().cubeAll("kul_blogu", KUL);
            simpleBlock(PulsarKayitlari.KUL_BLOGU.get(), blok);
            simpleBlockItem(PulsarKayitlari.KUL_BLOGU.get(), blok);

            ModelFile[] katlar = new ModelFile[8];
            for (int kat = 1; kat < 8; kat++) {
                katlar[kat - 1] = katModeli(kat * 2);
            }
            katlar[7] = blok;
            getVariantBuilder(PulsarKayitlari.KUL_KATMANI.get()).forAllStates(durum -> ConfiguredModel.builder()
                    .modelFile(katlar[durum.getValue(SnowLayerBlock.LAYERS) - 1]).build());
            simpleBlockItem(PulsarKayitlari.KUL_KATMANI.get(), katlar[0]);

            RotatedPillarBlock kutuk = (RotatedPillarBlock) PulsarKayitlari.KOMURLESMIS_KUTUK.get();
            ResourceLocation yan = DogalAfetler.konum("block/komurlesmis_kutuk");
            ResourceLocation tepe = DogalAfetler.konum("block/komurlesmis_kutuk_top");
            ModelFile dik = models().cubeColumn("komurlesmis_kutuk", yan, tepe);
            axisBlock(kutuk, dik, models().cubeColumnHorizontal("komurlesmis_kutuk_horizontal", yan, tepe));
            simpleBlockItem(kutuk, dik);
        }

        /** Vanilla {@code snow_height<n>} gibi: tabanda {@code yukseklik} piksellik kutu, üst yüzü kesilmez. */
        private ModelFile katModeli(int yukseklik) {
            BlockModelBuilder m = models().getBuilder("kul_katmani_height" + yukseklik)
                    .texture("particle", KUL).texture("texture", KUL);
            m.element().from(0, 0, 0).to(16, yukseklik, 16).allFaces((yon, yuz) -> {
                yuz.texture("#texture");
                if (yon != Direction.UP) {
                    yuz.cullface(yon);
                }
            }).end();
            return m;
        }
    }
}
