package io.github.kadirberkpolat1.dogalafetler.esya.sogutma;

import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.BalikcilikKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Soğutucu eşyaların ve Kutup Oltası'nın datagen çıktıları: tarifler (Soğutma Külçesi 3 parça + 1 demir külçe, Soğutma
 * Plakası Külçe + Paketli Buz + Lapis + Demir; Termostat Plakası'nın tersi), zırh eki tarifi ({@code dogalafetler:zirh_eki},
 * {@code sogutmali}) ve beş eşya modeli. Sağlayıcı adları benzersizdir (K-E5).
 */
public final class SogutmaVerileri {
    private SogutmaVerileri() {
    }

    static void ekle(GatherDataEvent olay) {
        DataGenerator uretici = olay.getGenerator();
        PackOutput cikti = uretici.getPackOutput();
        ExistingFileHelper dosyalar = olay.getExistingFileHelper();
        uretici.addProvider(olay.includeServer(), new Adli("Soğutucu eşya tarifleri", new Tarifler(cikti)));
        uretici.addProvider(olay.includeServer(), new ZirhEkiTarifi(cikti));
        uretici.addProvider(olay.includeClient(), new Modeller(cikti, dosyalar));
    }

    /** {@code getName()}'i final olan vanilla tarif sağlayıcısına benzersiz ad verir (K-E5). */
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

    private static final class Tarifler extends RecipeProvider {
        Tarifler(PackOutput cikti) {
            super(cikti);
        }

        @Override
        protected void buildRecipes(Consumer<FinishedRecipe> c) {
            Item parca = SogutmaKayitlari.SOGUTMA_PARCASI.get();
            Item kulce = SogutmaKayitlari.SOGUTMA_KULCESI.get();
            Item inci = BalikcilikKayitlari.BUZLU_INCI.get();
            Item tuz = BalikcilikKayitlari.TUZ_KRISTALI.get();

            // 3 parça + 1 demir külçe = Külçe (Meteorit Külçesi ile aynı desen).
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, kulce).requires(parca, 3).requires(Tags.Items.INGOTS_IRON)
                    .unlockedBy("has_sogutma_parcasi", has(parca)).save(c, DogalAfetler.konum("sogutma_kulcesi"));
            // Termostat Plakası'nın tersi: Külçe + Paketli Buz + Lapis + Demir.
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, SogutmaKayitlari.SOGUTMA_PLAKASI.get()).requires(kulce)
                    .requires(Items.PACKED_ICE).requires(Tags.Items.GEMS_LAPIS).requires(Tags.Items.INGOTS_IRON)
                    .unlockedBy("has_sogutma_kulcesi", has(kulce)).save(c, DogalAfetler.konum("sogutma_plakasi"));
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, SogutmaKayitlari.SERIN_KOLYE.get())
                    .define('S', Items.STRING).define('K', kulce).define('B', inci)
                    .pattern("SKS").pattern("S S").pattern(" B ")
                    .unlockedBy("has_buzlu_inci", has(inci)).save(c, DogalAfetler.konum("serin_kolye"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, SogutmaKayitlari.SOGUK_IKSIRI.get())
                    .requires(Items.GLASS_BOTTLE).requires(parca).requires(tuz).requires(Items.SNOWBALL)
                    .unlockedBy("has_sogutma_parcasi", has(parca)).save(c, DogalAfetler.konum("soguk_iksiri"));
            // Kutup Oltası: vanilla olta deseni; üstüne Soğutma Külçesi, sap ucuna Meteorit Parçası.
            ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BalikcilikKayitlari.KUTUP_OLTASI.get())
                    .define('K', kulce).define('/', Items.STICK).define('S', Items.STRING)
                    .define('M', MeteorEsyaKayitlari.METEORIT_PARCASI.get())
                    .pattern("  K").pattern(" /S").pattern("/ M")
                    .unlockedBy("has_sogutma_kulcesi", has(kulce)).save(c, DogalAfetler.konum("kutup_oltasi"));
        }
    }

    /** Soğutmalı zırh: herhangi bir zırh + Soğutma Plakası (Termostatlı zırhın aynası; özel tarif, kitapta görünmez). */
    private static final class ZirhEkiTarifi implements DataProvider {
        private final PackOutput cikti;

        ZirhEkiTarifi(PackOutput cikti) {
            this.cikti = cikti;
        }

        @Override
        public CompletableFuture<?> run(CachedOutput onbellek) {
            Path veri = cikti.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(DogalAfetler.MOD_ID);
            JsonObject zirh = new JsonObject();
            zirh.addProperty("type", DogalAfetler.MOD_ID + ":zirh_eki");
            zirh.addProperty("category", "equipment");
            zirh.addProperty("ek", Sogutma.SOGUTMALI.anahtar());
            JsonObject malzeme = new JsonObject();
            malzeme.addProperty("item", DogalAfetler.MOD_ID + ":sogutma_plakasi");
            zirh.add("malzeme", malzeme);
            return DataProvider.saveStable(onbellek, zirh, veri.resolve("recipes/sogutmali_zirh.json"));
        }

        @Override
        public String getName() {
            return "Soğutmalı zırh tarifi";
        }
    }

    private static final class Modeller extends ItemModelProvider {
        Modeller(PackOutput cikti, ExistingFileHelper dosyalar) {
            super(cikti, DogalAfetler.MOD_ID, dosyalar);
        }

        @Override
        public String getName() {
            return "Soğutucu eşya modelleri";
        }

        @Override
        protected void registerModels() {
            basicItem(SogutmaKayitlari.SOGUTMA_PARCASI.get());
            basicItem(SogutmaKayitlari.SOGUTMA_KULCESI.get());
            basicItem(SogutmaKayitlari.SOGUTMA_PLAKASI.get());
            basicItem(SogutmaKayitlari.SERIN_KOLYE.get());
            basicItem(SogutmaKayitlari.SOGUK_IKSIRI.get());
        }
    }
}
