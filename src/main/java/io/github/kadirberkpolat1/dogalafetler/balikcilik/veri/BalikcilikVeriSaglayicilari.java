package io.github.kadirberkpolat1.dogalafetler.balikcilik.veri;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikEtiketleri;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikKayitlari;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikKayitlari.Balik;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BuzDeligiBlogu;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BuzDeligiLootDegistirici;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.KategoriGirdisi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.DelikOmru;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikKayitlari.OzelBalik;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.DenizLootDegistirici;
import io.github.kadirberkpolat1.dogalafetler.esya.sogutma.SogutmaKayitlari;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari.Kategori;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.Deserializers;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.MatchTool;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.client.model.generators.BlockModelBuilder;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelBuilder;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Balıkçılığın datagen çıktıları (GOREV §3, madde 2, N12): balıkçılık loot tabloları ({@link #ANA} ve kategori
 * tabloları), global loot değiştiricisi, eşya etiketleri ({@code #dogalafetler:pismis_kutup_baligi},
 * {@code #dogalafetler:cig_kutup_baligi}), pişirme tarifleri (ocak, tütsüleme, kamp ateşi) ve
 * {@code buz_parcasi} → buz, blok durumu ve modeller (dokuları {@code tools/assets/balikcilik_dokular.py} üretir).
 * Sağlayıcı adları benzersizdir (K-E5). Vanilla blok etiketlerine yazılmaz: aynı etiket dosyasını yazan ikinci sağlayıcı
 * birincinin içeriğini siler (B-10); vanilla etiket katkıları yalnız çekirdeğin kancasıyla (K-E12).
 */
public final class BalikcilikVeriSaglayicilari {
    private static final ResourceLocation ANA = BuzDeligiLootDegistirici.TABLO;
    /** Açık denizde vanilla sonucun eşya havuzuyla değişme ihtimali (her olta). */
    private static final float DENIZ_ESYA_OLASILIGI = 0.22f;
    /** Açık denizde Kutup Oltası'yla vanilla sonucun özel balıkla değişme ihtimali. */
    private static final float DENIZ_OZEL_OLASILIGI = 0.30f;
    /**
     * Açık denizde vanilla sonucun Yineleme büyülü kitabıyla değişme ihtimali (her olta; vanilla hazine havuzu %5'tir, kitap
     * onun çok küçük bir parçası).
     */
    private static final float DENIZ_YINELEME_OLASILIGI = 0.005f;

    private BalikcilikVeriSaglayicilari() {
    }

    private static ResourceLocation kategoriTablosu(Kategori k) {
        return BuzDeligiLootDegistirici.kategoriTablosu(k);
    }

    public static void ekle(GatherDataEvent olay) {
        DataGenerator uretici = olay.getGenerator();
        PackOutput cikti = uretici.getPackOutput();
        ExistingFileHelper dosyalar = olay.getExistingFileHelper();
        boolean sunucu = olay.includeServer();
        uretici.addProvider(sunucu, new Adli("Balıkçılık loot tabloları", new LootTableProvider(cikti, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(BalikLootu::new, LootContextParamSets.FISHING)))));
        uretici.addProvider(sunucu, new Degistiriciler(cikti));
        uretici.addProvider(sunucu, new EsyaEtiketleri(cikti, olay.getLookupProvider(), dosyalar));
        uretici.addProvider(sunucu, new Adli("Balıkçılık tarifleri", new Tarifler(cikti)));
        uretici.addProvider(olay.includeClient(), new Modeller(cikti, dosyalar));
    }

    /**
     * {@code getName()}'i final olan vanilla sağlayıcılara (loot tablosu, tarif) benzersiz ad verir: {@code DataGenerator} aynı
     * adlı ikinci sağlayıcıyı reddeder (K-E5).
     */
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

    private static final class BalikLootu implements LootTableSubProvider {
        @Override
        public void generate(BiConsumer<ResourceLocation, LootTable.Builder> c) {
            Map<Kategori, ResourceLocation> tablolar = new EnumMap<>(Kategori.class);
            for (Kategori k : Kategori.values()) {
                tablolar.put(k, kategoriTablosu(k));
            }
            c.accept(ANA, LootTable.lootTable().withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                    .add(KategoriGirdisi.girdi(tablolar))));
            c.accept(kategoriTablosu(Kategori.YAYGIN), tablo(LootItem.lootTableItem(Balik.BUZ_ALABALIGI.cig()),
                    LootItem.lootTableItem(Balik.KUTUP_MORINASI.cig())));
            c.accept(kategoriTablosu(Kategori.ORTA), tablo(LootItem.lootTableItem(Balik.DERIN_SOGUK_SOMONU.cig()),
                    LootItem.lootTableItem(Balik.BUZ_KEFALI.cig())));
            c.accept(kategoriTablosu(Kategori.NADIR), tablo(LootItem.lootTableItem(Balik.DONMUS_RINGA.cig())));
            c.accept(kategoriTablosu(Kategori.COP), tablo(
                    LootItem.lootTableItem(BalikcilikKayitlari.BUZ_PARCASI.get()).setWeight(3)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))),
                    LootItem.lootTableItem(Items.SNOWBALL).setWeight(2)
                            .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 4)))));
            LootItemFunction kitap = hazineBuyusu(Enchantments.FROST_WALKER, Enchantments.FISHING_SPEED,
                    Enchantments.FISHING_LUCK);
            // Buz deliği hazine havuzu: Yineleme kitapları toplam ağırlığın ≈ %28'i (18 + 4 + 2 + 1).
            LootPoolEntryContainer.Builder<?>[] yineleme = yinelemeKitaplari();
            c.accept(kategoriTablosu(Kategori.HAZINE), tablo(LootItem.lootTableItem(Items.BOOK).setWeight(18)
                    .apply(() -> kitap), yineleme[0], yineleme[1], yineleme[2]));
            c.accept(kategoriTablosu(Kategori.ESYA), tablo(esyaGirdileri()));
            c.accept(kategoriTablosu(Kategori.OZEL), tablo(ozelGirdileri()));
            c.accept(DenizLootDegistirici.ESYA_TABLOSU, tablo(esyaGirdileri()));
            c.accept(DenizLootDegistirici.OZEL_TABLOSU, tablo(ozelGirdileri()));
            c.accept(DenizLootDegistirici.YINELEME_TABLOSU, tablo(yinelemeKitaplari()));
        }

        /**
         * {@code enchant_randomly} verilen sırayla: vanilla kurucusu büyüleri kimlik hash'li bir {@code HashSet}'te tutar ve
         * her {@code runData}'da sıra değişir (üretilen dosya sabit kalmaz); işlev JSON'dan kurulur.
         */
        private static LootItemFunction hazineBuyusu(Enchantment... buyuler) {
            JsonObject json = new JsonObject();
            json.addProperty("function", "minecraft:enchant_randomly");
            JsonArray liste = new JsonArray();
            for (Enchantment b : buyuler) {
                liste.add(String.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(b)));
            }
            json.add("enchantments", liste);
            return Deserializers.createFunctionSerializer().create().fromJson(json, LootItemFunction.class);
        }

        private static LootTable.Builder tablo(net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer.Builder<?>... girdiler) {
            LootPool.Builder havuz = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
            for (var g : girdiler) {
                havuz.add(g);
            }
            return LootTable.lootTable().withPool(havuz);
        }
    }

    /** Buz altı ve açık deniz eşya havuzu: Soğutma Parçası sık (3 parçayla Külçe), Tuz Kristali orta, Buzlu İnci nadir. */
    private static LootPoolEntryContainer.Builder<?>[] esyaGirdileri() {
        return new LootPoolEntryContainer.Builder<?>[] {
            LootItem.lootTableItem(SogutmaKayitlari.SOGUTMA_PARCASI.get()).setWeight(7)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2))),
            LootItem.lootTableItem(BalikcilikKayitlari.TUZ_KRISTALI.get()).setWeight(3)
                    .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 3))),
            LootItem.lootTableItem(BalikcilikKayitlari.BUZLU_INCI.get()).setWeight(1)};
    }

    /** Özel kutup balıkları (yalnız Kutup Oltası): Kristal Levrek yaygın, Kutup Yılanbalığı orta, Aurora Balığı nadir. */
    private static LootPoolEntryContainer.Builder<?>[] ozelGirdileri() {
        return new LootPoolEntryContainer.Builder<?>[] {
            LootItem.lootTableItem(OzelBalik.KRISTAL_LEVREK.cig()).setWeight(6),
            LootItem.lootTableItem(OzelBalik.KUTUP_YILANBALIGI.cig()).setWeight(3),
            LootItem.lootTableItem(OzelBalik.AURORA_BALIGI.cig()).setWeight(1)};
    }

    /** Yineleme büyülü kitapları: I ağırlık 4, II 2, III 1 (yüksek seviye daha nadir). */
    private static LootPoolEntryContainer.Builder<?>[] yinelemeKitaplari() {
        return new LootPoolEntryContainer.Builder<?>[] {
            yinelemeKitabi(1, 4), yinelemeKitabi(2, 2), yinelemeKitabi(3, 1)};
    }

    /** {@code set_enchantments}: yalnız {@code Items.BOOK} tabanlı olursa büyülü kitaba (StoredEnchantments) çevrilir. */
    private static LootPoolEntryContainer.Builder<?> yinelemeKitabi(int seviye, int agirlik) {
        return LootItem.lootTableItem(Items.BOOK).setWeight(agirlik).apply(new SetEnchantmentsFunction.Builder()
                .withEnchantment(BalikcilikKayitlari.YINELEME.get(), ConstantValue.exactly(seviye)));
    }

    private static final class Degistiriciler extends GlobalLootModifierProvider {
        Degistiriciler(PackOutput cikti) {
            super(cikti, DogalAfetler.MOD_ID);
        }

        @Override
        protected void start() {
            add("buz_deligi_balikcilik", new BuzDeligiLootDegistirici(
                    new LootItemCondition[] {LootTableIdCondition.builder(BuiltInLootTables.FISHING).build()}, ANA));
            // Açık deniz (buz deliği dışı): her olta ile eşya, yalnız Kutup Oltası'yla özel balık (madde 1, 3).
            add("deniz_balikciligi_esya", new DenizLootDegistirici(
                    new LootItemCondition[] {LootTableIdCondition.builder(BuiltInLootTables.FISHING).build()},
                    DenizLootDegistirici.ESYA_TABLOSU, DENIZ_ESYA_OLASILIGI));
            add("deniz_balikciligi_ozel", new DenizLootDegistirici(
                    new LootItemCondition[] {LootTableIdCondition.builder(BuiltInLootTables.FISHING).build(),
                        MatchTool.toolMatches(ItemPredicate.Builder.item().of(BalikcilikKayitlari.KUTUP_OLTASI.get())).build()},
                    DenizLootDegistirici.OZEL_TABLOSU, DENIZ_OZEL_OLASILIGI));
            // Yineleme büyülü kitabı: her olta ile açık denizde çok nadir hazine.
            add("deniz_balikciligi_yineleme", new DenizLootDegistirici(
                    new LootItemCondition[] {LootTableIdCondition.builder(BuiltInLootTables.FISHING).build()},
                    DenizLootDegistirici.YINELEME_TABLOSU, DENIZ_YINELEME_OLASILIGI));
        }

        @Override
        public String getName() {
            return "Balıkçılık global loot değiştiricileri";
        }
    }

    private static final class EsyaEtiketleri extends ItemTagsProvider {
        EsyaEtiketleri(PackOutput cikti, CompletableFuture<HolderLookup.Provider> arama, ExistingFileHelper dosyalar) {
            super(cikti, arama, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()), DogalAfetler.MOD_ID,
                    dosyalar);
        }

        @Override
        public String getName() {
            return "Balıkçılık eşya etiketleri";
        }

        @Override
        protected void addTags(HolderLookup.Provider arama) {
            for (Balik b : Balik.values()) {
                tag(BalikcilikEtiketleri.CIG_KUTUP_BALIGI).add(b.cig());
                tag(BalikcilikEtiketleri.PISMIS_KUTUP_BALIGI).add(b.pismis());
            }
        }
    }

    private static final class Tarifler extends RecipeProvider {
        Tarifler(PackOutput cikti) {
            super(cikti);
        }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> c) {
            for (Balik b : Balik.values()) {
                Item cig = b.cig();
                Item pismis = b.pismis();
                String ad = "pismis_" + b.ad();
                String acilis = "has_" + b.ad();
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(cig), RecipeCategory.FOOD, pismis, 0.35f, 200)
                        .unlockedBy(acilis, has(cig)).save(c, DogalAfetler.konum(ad));
                SimpleCookingRecipeBuilder.smoking(Ingredient.of(cig), RecipeCategory.FOOD, pismis, 0.35f, 100)
                        .unlockedBy(acilis, has(cig)).save(c, DogalAfetler.konum(ad + "_tutsuleme"));
                SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(cig), RecipeCategory.FOOD, pismis, 0.35f, 600)
                        .unlockedBy(acilis, has(cig)).save(c, DogalAfetler.konum(ad + "_kamp_atesi"));
            }
            Item parca = BalikcilikKayitlari.BUZ_PARCASI.get();
            ShapelessRecipeBuilder.shapeless(RecipeCategory.BUILDING_BLOCKS, Items.ICE).requires(parca, 4)
                    .unlockedBy("has_buz_parcasi", has(parca)).save(c, DogalAfetler.konum("buz_parcasindan_buz"));
            for (OzelBalik b : OzelBalik.values()) {
                Item cig = b.cig();
                Item pismis = b.pismis();
                String ad = "pismis_" + b.ad();
                String acilis = "has_" + b.ad();
                SimpleCookingRecipeBuilder.smelting(Ingredient.of(cig), RecipeCategory.FOOD, pismis, 0.35f, 200)
                        .unlockedBy(acilis, has(cig)).save(c, DogalAfetler.konum(ad));
                SimpleCookingRecipeBuilder.smoking(Ingredient.of(cig), RecipeCategory.FOOD, pismis, 0.35f, 100)
                        .unlockedBy(acilis, has(cig)).save(c, DogalAfetler.konum(ad + "_tutsuleme"));
                SimpleCookingRecipeBuilder.campfireCooking(Ingredient.of(cig), RecipeCategory.FOOD, pismis, 0.35f, 600)
                        .unlockedBy(acilis, has(cig)).save(c, DogalAfetler.konum(ad + "_kamp_atesi"));
            }
        }
    }

    private static final class Modeller extends BlockStateProvider {
        Modeller(PackOutput cikti, ExistingFileHelper dosyalar) {
            super(cikti, DogalAfetler.MOD_ID, dosyalar);
        }

        @Override
        public String getName() {
            return "Balıkçılık blok durumları ve modeller";
        }

        @Override
        protected void registerStatesAndModels() {
            Block delik = BalikcilikKayitlari.BUZ_DELIGI.get();
            ModelFile[] modeller = new ModelFile[DelikOmru.SON_ASAMA + 1];
            for (int a = 0; a <= DelikOmru.SON_ASAMA; a++) {
                modeller[a] = delikModeli(a);
            }
            getVariantBuilder(delik).forAllStates(d -> net.minecraftforge.client.model.generators.ConfiguredModel.builder()
                    .modelFile(modeller[d.getValue(BuzDeligiBlogu.ASAMA)]).build());
            for (Balik b : Balik.values()) {
                itemModels().basicItem(b.cig());
                itemModels().basicItem(b.pismis());
            }
            itemModels().basicItem(BalikcilikKayitlari.BUZ_PARCASI.get());
            for (OzelBalik b : OzelBalik.values()) {
                itemModels().basicItem(b.cig());
                itemModels().basicItem(b.pismis());
            }
            itemModels().basicItem(BalikcilikKayitlari.BUZLU_INCI.get());
            itemModels().basicItem(BalikcilikKayitlari.TUZ_KRISTALI.get());
            // Kutup Oltası: vanilla olta modeli (handheld_rod); atılmış hali vanilla atılmış olta modelinin (fishing_rod_cast) kopyası:
            // aynı gösterim dönüşümleri, böylece misina (varlık çizicisi) oltanın ucuna vanilla'daki gibi oturur. "cast" özelliği
            // istemci kaydıyla (KutupOltasiIstemci).
            ItemModelBuilder atilmis = itemModels().withExistingParent("kutup_oltasi_cast", mcLoc("item/fishing_rod_cast"))
                    .texture("layer0", modLoc("item/kutup_oltasi_cast"));
            itemModels().withExistingParent("kutup_oltasi", mcLoc("item/handheld_rod"))
                    .texture("layer0", modLoc("item/kutup_oltasi"))
                    .override().predicate(mcLoc("cast"), 1).model(atilmis).end();
        }

        /**
         * Buz çerçeve (3 piksel kalın halka, tam yükseklik): ortadaki 10x10 açıklıktan bloğun su sıvı durumunun yüzeyi
         * (y ≈ 14,2) görünür. Aşama 1–3'te açıklığın üstünde (y = 14,6) giderek kapanan yarı saydam buz zarı.
         */
        private ModelFile delikModeli(int asama) {
            BlockModelBuilder m = models().withExistingParent("buz_deligi_" + asama, mcLoc("block/block"))
                    .texture("kenar", modLoc("block/buz_deligi_kenar")).texture("particle", modLoc("block/buz_deligi_kenar"))
                    .renderType("translucent");
            halka(m, 0, 0, 16, 3);
            halka(m, 0, 13, 16, 16);
            halka(m, 0, 3, 3, 13);
            halka(m, 13, 3, 16, 13);
            if (asama > 0) {
                m.texture("zar", modLoc("block/buz_deligi_buzlanma_" + asama));
                m.element().from(3, 14.6f, 3).to(13, 14.6f, 13)
                        .face(Direction.UP).texture("#zar").uvs(3, 3, 13, 13).end()
                        .face(Direction.DOWN).texture("#zar").uvs(3, 3, 13, 13).end()
                        .end();
            }
            return m;
        }

        private static void halka(BlockModelBuilder m, float x0, float z0, float x1, float z1) {
            ModelBuilder<BlockModelBuilder>.ElementBuilder e = m.element().from(x0, 0, z0).to(x1, 16, z1);
            for (Direction d : Direction.values()) {
                var yuz = e.face(d).texture("#kenar");
                boolean sinirda = switch (d) {
                    case DOWN, UP -> true;
                    case NORTH -> z0 == 0;
                    case SOUTH -> z1 == 16;
                    case WEST -> x0 == 0;
                    case EAST -> x1 == 16;
                };
                if (sinirda) {
                    yuz.cullface(d);
                }
                yuz.end();
            }
            e.end();
        }
    }
}
