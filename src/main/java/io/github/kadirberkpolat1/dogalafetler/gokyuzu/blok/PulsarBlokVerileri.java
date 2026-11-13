package io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KutukIsisi;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Direction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.core.Direction.Axis;
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
 * Pulsar bloklarının datagen çıktıları: loot tabloları (kül katmanı 1–7 katta kat sayısı kadar kendini, 8 katta — Kül
 * Bloğu modeliyle çizilen tam blok — bir Kül Bloğu, ipek dokunuşta da aynısı; diğerleri kendini), blok durumları ve
 * modeller (kül katmanı vanilla kar katmanı gibi 2/4/…/14 piksel kutu, 8 kat kül bloğu modeli; kömürleşmiş kütük
 * eksenli sütun; ısı kademesine göre dört dokulu görünüm, soyulmuş kütük eksenli sütun; tahta tek dokulu küp) ve şekilsiz tarifler (1 kömürleşmiş ya da soyulmuş kütük → 4 kömürleşmiş tahta, grup
 * {@code planks}; 8 Kül Katmanı ↔ 1 Kül Bloğu, 8 katlı külü kıran oyuncu blokla birlikte katmanlarını kaybetmesin); her
 * tarifin kitap kilidi girdisi envantere girince açılır (K-E63). Doku dosyalarını {@code tools/assets/pulsar_dokular.py}
 * üretir.
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
        uretici.addProvider(olay.includeServer(), new Adli("Pulsar blok tarifleri", new Tarifler(cikti)));
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
            for (int kat = 1; kat < SnowLayerBlock.MAX_HEIGHT; kat++) {
                havuz.add(LootItem.lootTableItem(kul).when(katta(kul, kat))
                        .apply(SetItemCountFunction.setCount(ConstantValue.exactly(kat))));
            }
            havuz.add(LootItem.lootTableItem(PulsarKayitlari.KUL_BLOGU.get()).when(katta(kul, SnowLayerBlock.MAX_HEIGHT)));
            add(kul, LootTable.lootTable().withPool(applyExplosionCondition(kul, havuz)));
            dropSelf(PulsarKayitlari.KUL_BLOGU.get());
            dropSelf(PulsarKayitlari.KOMURLESMIS_KUTUK.get());
            dropSelf(PulsarKayitlari.SOYULMUS_KOMURLESMIS_KUTUK.get());
            dropSelf(PulsarKayitlari.KOMURLESMIS_TAHTA.get());
        }

        private static LootItemBlockStatePropertyCondition.Builder katta(Block kul, int kat) {
            return LootItemBlockStatePropertyCondition.hasBlockStateProperties(kul).setProperties(
                    StatePropertiesPredicate.Builder.properties().hasProperty(SnowLayerBlock.LAYERS, kat));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return List.of(PulsarKayitlari.KUL_KATMANI.get(), PulsarKayitlari.KUL_BLOGU.get(),
                    PulsarKayitlari.KOMURLESMIS_KUTUK.get(), PulsarKayitlari.SOYULMUS_KOMURLESMIS_KUTUK.get(),
                    PulsarKayitlari.KOMURLESMIS_TAHTA.get());
        }
    }

    private static final class Tarifler extends RecipeProvider {
        Tarifler(PackOutput cikti) {
            super(cikti);
        }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> c) {
            Item kutuk = PulsarKayitlari.KOMURLESMIS_KUTUK_ESYASI.get();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, PulsarKayitlari.KOMURLESMIS_TAHTA_ESYASI.get(), 4)
                    .requires(kutuk).group("planks").unlockedBy("has_komurlesmis_kutuk", has(kutuk))
                    .save(c, DogalAfetler.konum("komurlesmis_tahta"));
            Item soyulmus = PulsarKayitlari.SOYULMUS_KOMURLESMIS_KUTUK_ESYASI.get();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, PulsarKayitlari.KOMURLESMIS_TAHTA_ESYASI.get(), 4)
                    .requires(soyulmus).group("planks").unlockedBy("has_soyulmus_komurlesmis_kutuk", has(soyulmus))
                    .save(c, DogalAfetler.konum("komurlesmis_tahta_soyulmustan"));
            Block katman = PulsarKayitlari.KUL_KATMANI.get();
            Block blok = PulsarKayitlari.KUL_BLOGU.get();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, blok)
                    .requires(katman, SnowLayerBlock.MAX_HEIGHT).unlockedBy("has_kul_katmani", has(katman))
                    .save(c, DogalAfetler.konum("kul_katmanindan_kul_blogu"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, katman, SnowLayerBlock.MAX_HEIGHT)
                    .requires(blok).unlockedBy("has_kul_blogu", has(blok))
                    .save(c, DogalAfetler.konum("kul_blogundan_kul_katmani"));
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

            // Kütük: dört görünüm kademesi (sönmüş, ılık, sıcak, çok sıcak; KutukIsisi) × üç eksen; ılık mevcut adları korur.
            RotatedPillarBlock kutuk = (RotatedPillarBlock) PulsarKayitlari.KOMURLESMIS_KUTUK.get();
            ModelFile[] dikler = new ModelFile[KutukIsisi.Gorunum.values().length];
            ModelFile[] yataylar = new ModelFile[dikler.length];
            for (KutukIsisi.Gorunum g : KutukIsisi.Gorunum.values()) {
                String ad = "komurlesmis_kutuk" + g.ek();
                ResourceLocation yan = DogalAfetler.konum("block/" + ad);
                ResourceLocation tepe = DogalAfetler.konum("block/" + ad + "_top");
                dikler[g.ordinal()] = models().cubeColumn(ad, yan, tepe);
                yataylar[g.ordinal()] = models().cubeColumnHorizontal(ad + "_horizontal", yan, tepe);
            }
            getVariantBuilder(kutuk).forAllStatesExcept(durum -> {
                int g = KutukIsisi.gorunum(durum.getValue(KomurlesmisKutukBlogu.ISI)).ordinal();
                Axis eksen = durum.getValue(RotatedPillarBlock.AXIS);
                ConfiguredModel.Builder<?> m = ConfiguredModel.builder()
                        .modelFile(eksen == Axis.Y ? dikler[g] : yataylar[g]);
                if (eksen != Axis.Y) {
                    m.rotationX(90);
                    if (eksen == Axis.X) {
                        m.rotationY(90);
                    }
                }
                return m.build();
            }, KomurlesmisKutukBlogu.DONUSUYOR);
            simpleBlockItem(kutuk, dikler[KutukIsisi.Gorunum.ILIK.ordinal()]);

            RotatedPillarBlock soyulmus = (RotatedPillarBlock) PulsarKayitlari.SOYULMUS_KOMURLESMIS_KUTUK.get();
            ResourceLocation soyulmusYan = DogalAfetler.konum("block/soyulmus_komurlesmis_kutuk");
            ResourceLocation soyulmusTepe = DogalAfetler.konum("block/soyulmus_komurlesmis_kutuk_top");
            ModelFile soyulmusDik = models().cubeColumn("soyulmus_komurlesmis_kutuk", soyulmusYan, soyulmusTepe);
            axisBlock(soyulmus, soyulmusDik,
                    models().cubeColumnHorizontal("soyulmus_komurlesmis_kutuk_horizontal", soyulmusYan, soyulmusTepe));
            simpleBlockItem(soyulmus, soyulmusDik);

            ModelFile tahta = models().cubeAll("komurlesmis_tahta", DogalAfetler.konum("block/komurlesmis_tahta"));
            simpleBlock(PulsarKayitlari.KOMURLESMIS_TAHTA.get(), tahta);
            simpleBlockItem(PulsarKayitlari.KOMURLESMIS_TAHTA.get(), tahta);
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
