package io.github.kadirberkpolat1.dogalafetler.meteor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.termostat.Termostat;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.Tags;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Meteor ve termostat veri dosyaları (datagen): şekilsiz Meteorit Külçesi ve Termostat Plakası tarifleri ve tarif kitabı
 * açma başarımları (girdi meteor eşyası envantere girince), Termostatlı zırh ({@code dogalafetler:zirh_eki}, sıcaklık §A3;
 * özel tarif, tarif kitabında görünmez), eşya etiketi {@code #dogalafetler:kozmik_cekirdek} ve dört eşya modeli. İki
 * sağlayıcının adları ayrıdır (K-E5).
 */
public final class MeteorVerileri implements DataProvider {
    private static final String[] ESYALAR = {"meteorit_parcasi", "kozmik_cekirdek", "meteorit_kulcesi",
        "termostat_plakasi"};

    private final PackOutput cikti;
    private final boolean modeller;

    private MeteorVerileri(PackOutput cikti, boolean modeller) {
        this.cikti = cikti;
        this.modeller = modeller;
    }

    static void ekle(GatherDataEvent olay) {
        PackOutput cikti = olay.getGenerator().getPackOutput();
        olay.getGenerator().addProvider(olay.includeServer(), new MeteorVerileri(cikti, false));
        olay.getGenerator().addProvider(olay.includeClient(), new MeteorVerileri(cikti, true));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput onbellek) {
        List<CompletableFuture<?>> isler = new ArrayList<>();
        if (modeller) {
            Path kaynak = cikti.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(DogalAfetler.MOD_ID);
            for (String ad : ESYALAR) {
                JsonObject model = new JsonObject();
                model.addProperty("parent", "minecraft:item/generated");
                JsonObject dokular = new JsonObject();
                dokular.addProperty("layer0", DogalAfetler.MOD_ID + ":item/" + ad);
                model.add("textures", dokular);
                isler.add(DataProvider.saveStable(onbellek, model, kaynak.resolve("models/item/" + ad + ".json")));
            }
        } else {
            Path kok = cikti.getOutputFolder(PackOutput.Target.DATA_PACK);
            Path veri = kok.resolve(DogalAfetler.MOD_ID);
            for (FinishedRecipe tarif : tarifler()) {
                ResourceLocation id = tarif.getId();
                ResourceLocation basarimId = tarif.getAdvancementId();
                isler.add(DataProvider.saveStable(onbellek, tarif.serializeRecipe(),
                        kok.resolve(id.getNamespace() + "/recipes/" + id.getPath() + ".json")));
                isler.add(DataProvider.saveStable(onbellek, tarif.serializeAdvancement(),
                        kok.resolve(basarimId.getNamespace() + "/advancements/" + basarimId.getPath() + ".json")));
            }
            JsonObject zirh = new JsonObject();
            zirh.addProperty("type", DogalAfetler.MOD_ID + ":zirh_eki");
            zirh.addProperty("category", "equipment");
            zirh.addProperty("ek", Termostat.TERMOSTATLI.anahtar());
            zirh.add("malzeme", malzeme("item", "dogalafetler:termostat_plakasi"));
            isler.add(DataProvider.saveStable(onbellek, zirh, veri.resolve("recipes/termostatli_zirh.json")));
            JsonObject etiket = new JsonObject();
            etiket.addProperty("replace", false);
            JsonArray degerler = new JsonArray();
            degerler.add(DogalAfetler.MOD_ID + ":kozmik_cekirdek");
            etiket.add("values", degerler);
            isler.add(DataProvider.saveStable(onbellek, etiket, veri.resolve("tags/items/kozmik_cekirdek.json")));
        }
        return CompletableFuture.allOf(isler.toArray(CompletableFuture[]::new));
    }

    /** Madde 13c tarifleri; kitapta girdi meteor eşyası envantere girince açılır (tarif-2). */
    private static List<FinishedRecipe> tarifler() {
        List<FinishedRecipe> liste = new ArrayList<>();
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MeteorEsyaKayitlari.METEORIT_KULCESI.get())
                .requires(MeteorEsyaKayitlari.METEORIT_PARCASI.get(), 3).requires(Tags.Items.INGOTS_IRON)
                .unlockedBy("has_meteorit_parcasi",
                        InventoryChangeTrigger.TriggerInstance.hasItems(MeteorEsyaKayitlari.METEORIT_PARCASI.get()))
                .save(liste::add, DogalAfetler.konum("meteorit_kulcesi"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, MeteorEsyaKayitlari.TERMOSTAT_PLAKASI.get())
                .requires(MeteorEsyaKayitlari.METEORIT_KULCESI.get()).requires(Tags.Items.INGOTS_GOLD)
                .requires(Tags.Items.DUSTS_REDSTONE).requires(Tags.Items.INGOTS_IRON)
                .unlockedBy("has_meteorit_kulcesi",
                        InventoryChangeTrigger.TriggerInstance.hasItems(MeteorEsyaKayitlari.METEORIT_KULCESI.get()))
                .save(liste::add, DogalAfetler.konum("termostat_plakasi"));
        return liste;
    }

    private static JsonObject malzeme(String tur, String kimlik) {
        JsonObject j = new JsonObject();
        j.addProperty(tur, kimlik);
        return j;
    }

    @Override
    public String getName() {
        return modeller ? "Doğal Afetler meteor eşya modelleri" : "Doğal Afetler meteor ve termostat verileri";
    }
}
