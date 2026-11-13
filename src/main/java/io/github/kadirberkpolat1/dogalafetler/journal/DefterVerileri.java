package io.github.kadirberkpolat1.dogalafetler.journal;

import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Afet Defteri'nin datagen dosyaları: şekilsiz tarif (Kitap + Tüy + Mürekkep Kesesi + Pusula), tarif kitabı açma başarımı ve
 * eşya modeli. Tarif, Hava Radarı ve Sismograf gibi oyuncunun ilk tick'inde kitaba girer ve kitap ya da pusula envantere
 * girince de açılır; eşya oyuna ilk girişte oyuncuya verilmez, yapılması gerekir. Vanilla {@code RecipeProvider}'ın adı sabit
 * olduğu için (K-E5) builder çıktısını bu sağlayıcı kendisi yazar.
 */
public final class DefterVerileri implements DataProvider {
    private final PackOutput cikti;

    private DefterVerileri(PackOutput cikti) {
        this.cikti = cikti;
    }

    static void ekle(GatherDataEvent olay) {
        olay.getGenerator().addProvider(olay.includeServer() || olay.includeClient(),
                new DefterVerileri(olay.getGenerator().getPackOutput()));
    }

    private static FinishedRecipe tarif() {
        List<FinishedRecipe> liste = new ArrayList<>(1);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, Defter.ESYA.get())
                .requires(Items.BOOK).requires(Items.FEATHER).requires(Items.INK_SAC).requires(Items.COMPASS)
                .unlockedBy("oyun_basi", PlayerTrigger.TriggerInstance.tick())
                .unlockedBy("has_book", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BOOK))
                .unlockedBy("has_compass", InventoryChangeTrigger.TriggerInstance.hasItems(Items.COMPASS))
                .save(liste::add, Defter.TARIF);
        return liste.get(0);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput onbellek) {
        Path veri = cikti.getOutputFolder(PackOutput.Target.DATA_PACK);
        Path kaynak = cikti.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(DogalAfetler.MOD_ID);
        FinishedRecipe tarif = tarif();
        ResourceLocation id = tarif.getId();
        ResourceLocation basarimId = tarif.getAdvancementId();
        JsonObject model = new JsonObject();
        model.addProperty("parent", "minecraft:item/generated");
        JsonObject dokular = new JsonObject();
        dokular.addProperty("layer0", DogalAfetler.MOD_ID + ":item/afet_defteri");
        model.add("textures", dokular);
        return CompletableFuture.allOf(
                DataProvider.saveStable(onbellek, tarif.serializeRecipe(),
                        veri.resolve(id.getNamespace() + "/recipes/" + id.getPath() + ".json")),
                DataProvider.saveStable(onbellek, tarif.serializeAdvancement(),
                        veri.resolve(basarimId.getNamespace() + "/advancements/" + basarimId.getPath() + ".json")),
                DataProvider.saveStable(onbellek, model, kaynak.resolve("models/item/afet_defteri.json")));
    }

    @Override
    public String getName() {
        return "Doğal Afetler Afet Defteri tarifi ve modeli";
    }
}
