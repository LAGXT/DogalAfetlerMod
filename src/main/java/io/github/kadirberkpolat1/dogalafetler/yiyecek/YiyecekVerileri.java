package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTablosu;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Yiyecek datagen'i: madde 14'ün tarifleri birebir (şekilsiz; süt kovasının kovası vanilla kalanıyla geri döner), tarif
 * kitabı açma başarımları ve eşya modelleri. Vanilla {@code RecipeProvider}'ın adı sabit olduğu için ("Recipes", K-E5) kendi
 * adlı sağlayıcı {@code ShapelessRecipeBuilder} çıktısını kendisi yazar.
 */
public final class YiyecekVerileri {
    private YiyecekVerileri() {
    }

    static void ekle(GatherDataEvent olay) {
        PackOutput cikti = olay.getGenerator().getPackOutput();
        olay.getGenerator().addProvider(olay.includeServer(), new Tarifler(cikti));
        olay.getGenerator().addProvider(olay.includeClient(), new Modeller(cikti));
    }

    /** Madde 14 tarifleri (sıra şartnamedeki gibi). */
    static void tarifler(Consumer<FinishedRecipe> c) {
        sekilsiz("sicak_salep", 3).requires(Items.GLASS_BOTTLE, 3).requires(Items.MILK_BUCKET)
                .requires(Items.HONEY_BOTTLE).unlockedBy("has_milk_bucket", sahip(Items.MILK_BUCKET))
                .save(c, DogalAfetler.konum("sicak_salep"));
        sekilsiz("sicak_cikolata", 3).requires(Items.GLASS_BOTTLE, 3).requires(Items.MILK_BUCKET)
                .requires(Items.COCOA_BEANS, 2).requires(Items.SUGAR).unlockedBy("has_cocoa_beans", sahip(Items.COCOA_BEANS))
                .save(c, DogalAfetler.konum("sicak_cikolata"));
        sekilsiz("sicak_tavuk_corbasi", 1).requires(Items.BOWL).requires(Items.COOKED_CHICKEN).requires(Items.CARROT)
                .requires(Items.POTATO).unlockedBy("has_cooked_chicken", sahip(Items.COOKED_CHICKEN))
                .save(c, DogalAfetler.konum("sicak_tavuk_corbasi"));
        sekilsiz("atesli_guvec", 1).requires(Items.BOWL).requires(Ingredient.of(Items.COOKED_BEEF, Items.COOKED_PORKCHOP))
                .requires(Items.POTATO).requires(Items.BLAZE_POWDER).unlockedBy("has_blaze_powder", sahip(Items.BLAZE_POWDER))
                .save(c, DogalAfetler.konum("atesli_guvec"));
        List<FinishedRecipe> cay = new ArrayList<>();
        sekilsiz("isitici_cay", 2).requires(Items.GLASS_BOTTLE).requires(Items.SWEET_BERRIES, 2)
                .requires(Items.HONEY_BOTTLE).unlockedBy("has_sweet_berries", sahip(Items.SWEET_BERRIES))
                .save(cay::add, DogalAfetler.konum("isitici_cay"));
        cay.forEach(t -> c.accept(new KalanSecimliKayit(t, List.of(Items.HONEY_BOTTLE))));
        sekilsiz("ayran", 3).requires(Items.GLASS_BOTTLE, 3).requires(Items.MILK_BUCKET).requires(Items.SNOWBALL, 2)
                .unlockedBy("has_snowball", sahip(Items.SNOWBALL)).save(c, DogalAfetler.konum("ayran"));
        sekilsiz("buzlu_karpuz", 1).requires(Items.MELON_SLICE).requires(Items.SNOWBALL)
                .unlockedBy("has_melon_slice", sahip(Items.MELON_SLICE)).save(c, DogalAfetler.konum("buzlu_karpuz"));
        sekilsiz("meyve_serbeti", 1).requires(Items.GLASS_BOTTLE).requires(Items.SWEET_BERRIES, 3).requires(Items.SUGAR)
                .requires(Items.SNOWBALL).unlockedBy("has_sweet_berries", sahip(Items.SWEET_BERRIES))
                .save(c, DogalAfetler.konum("meyve_serbeti"));
        sekilsiz("kutup_baligi_corbasi", 1).requires(Items.BOWL).requires(YiyecekKayitlari.PISMIS_KUTUP_BALIGI)
                .requires(Items.POTATO)
                .unlockedBy("has_pismis_kutup_baligi", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ItemPredicate.Builder.item().of(YiyecekKayitlari.PISMIS_KUTUP_BALIGI).build()))
                .save(c, DogalAfetler.konum("kutup_baligi_corbasi"));
    }

    private static ShapelessRecipeBuilder sekilsiz(String ad, int adet) {
        return ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, YiyecekKayitlari.esya(ad), adet);
    }

    private static InventoryChangeTrigger.TriggerInstance sahip(ItemLike esya) {
        return InventoryChangeTrigger.TriggerInstance.hasItems(esya);
    }

    /** Vanilla şekilsiz tarif çıktısını {@link KalanSecimliTarif} tipine çevirir. */
    private record KalanSecimliKayit(FinishedRecipe temel, List<Item> kalanBirakmayan) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject json) {
            temel.serializeRecipeData(json);
            JsonArray dizi = new JsonArray();
            for (Item esya : kalanBirakmayan) {
                dizi.add(String.valueOf(ForgeRegistries.ITEMS.getKey(esya)));
            }
            json.add(KalanSecimliTarif.ALAN, dizi);
        }

        @Override
        public ResourceLocation getId() {
            return temel.getId();
        }

        @Override
        public RecipeSerializer<?> getType() {
            return YiyecekKayitlari.KALAN_SECIMLI_TARIF.get();
        }

        @Override
        public JsonObject serializeAdvancement() {
            return temel.serializeAdvancement();
        }

        @Override
        public ResourceLocation getAdvancementId() {
            return temel.getAdvancementId();
        }
    }

    private static final class Tarifler implements DataProvider {
        private final PackOutput cikti;

        private Tarifler(PackOutput cikti) {
            this.cikti = cikti;
        }

        @Override
        public CompletableFuture<?> run(CachedOutput onbellek) {
            Path veri = cikti.getOutputFolder(PackOutput.Target.DATA_PACK);
            List<FinishedRecipe> liste = new ArrayList<>();
            tarifler(liste::add);
            List<CompletableFuture<?>> isler = new ArrayList<>();
            for (FinishedRecipe tarif : liste) {
                ResourceLocation id = tarif.getId();
                isler.add(DataProvider.saveStable(onbellek, tarif.serializeRecipe(),
                        veri.resolve(id.getNamespace() + "/recipes/" + id.getPath() + ".json")));
                JsonObject basarim = tarif.serializeAdvancement();
                ResourceLocation basarimId = tarif.getAdvancementId();
                if (basarim != null && basarimId != null) {
                    isler.add(DataProvider.saveStable(onbellek, basarim, veri.resolve(
                            basarimId.getNamespace() + "/advancements/" + basarimId.getPath() + ".json")));
                }
            }
            return CompletableFuture.allOf(isler.toArray(CompletableFuture[]::new));
        }

        @Override
        public String getName() {
            return "Doğal Afetler yiyecek tarifleri";
        }
    }

    private static final class Modeller implements DataProvider {
        private final PackOutput cikti;

        private Modeller(PackOutput cikti) {
            this.cikti = cikti;
        }

        @Override
        public CompletableFuture<?> run(CachedOutput onbellek) {
            Path kaynak = cikti.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
            List<CompletableFuture<?>> isler = new ArrayList<>();
            for (YiyecekTablosu.Kayit kayit : YiyecekTablosu.KAYITLAR) {
                JsonObject model = new JsonObject();
                model.addProperty("parent", "minecraft:item/generated");
                JsonObject dokular = new JsonObject();
                dokular.addProperty("layer0", DogalAfetler.MOD_ID + ":item/" + kayit.ad());
                model.add("textures", dokular);
                isler.add(DataProvider.saveStable(onbellek, model,
                        kaynak.resolve(DogalAfetler.MOD_ID + "/models/item/" + kayit.ad() + ".json")));
            }
            return CompletableFuture.allOf(isler.toArray(CompletableFuture[]::new));
        }

        @Override
        public String getName() {
            return "Doğal Afetler yiyecek modelleri";
        }
    }
}
