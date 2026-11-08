package io.github.kadirberkpolat1.dogalafetler.esya.radar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.EsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.NotCondition;
import net.minecraftforge.common.crafting.conditions.TagEmptyCondition;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Radar ve sismografın datagen dosyaları: dört tarif (madde 13a-b birebir), tarif kitabı açma başarımları ve dört eşya
 * modeli. Temel iki tarif koşulsuzdur ve oyuncunun ilk tick'inde tarif kitabına girer (tarif-2); gelişmişler şekilsiz +1
 * {@code #dogalafetler:kozmik_cekirdek}, etiket boşken yüklenmez: {@code forge:not(forge:tag_empty)} (R-17), kitapta temel eşya
 * ya da Kozmik Çekirdek envantere girince açılır. Tarif kimlikleri {@link EsyaKayitlari}'ndaki sabitlerdir. Vanilla
 * {@code RecipeProvider}'ın adı sabit olduğu için (K-E5) builder çıktısını bu sağlayıcı kendisi yazar.
 */
public final class RadarSismografVerileri implements DataProvider {
    private final PackOutput cikti;

    private RadarSismografVerileri(PackOutput cikti) {
        this.cikti = cikti;
    }

    public static void ekle(GatherDataEvent olay) {
        olay.getGenerator().addProvider(olay.includeServer() || olay.includeClient(),
                new RadarSismografVerileri(olay.getGenerator().getPackOutput()));
    }

    /** Tarif ve (varsa) hem tarife hem tarif kitabı başarımına eklenen Forge {@code conditions} dizisi. */
    private record Kayit(FinishedRecipe tarif, @Nullable JsonArray kosullar) {
    }

    private static List<Kayit> tarifler() {
        List<Kayit> liste = new ArrayList<>();
        // 13a: üst Demir/Cam/Demir, orta Kızıltaş/Pusula/Kızıltaş, alt Demir/Bakır/Demir.
        temelKilit(ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, EsyaKayitlari.HAVA_RADARI.get())
                .pattern("DCD").pattern("KPK").pattern("DBD")
                .define('B', Items.COPPER_INGOT).define('C', Items.GLASS).define('D', Items.IRON_INGOT)
                .define('K', Items.REDSTONE).define('P', Items.COMPASS))
                .save(t -> liste.add(new Kayit(t, null)), EsyaKayitlari.TARIF_HAVA_RADARI);
        // 13b: üst Demir/Kağıt/Demir, orta Çubuk/Kızıltaş/Çubuk, alt Taş/Bakır/Taş.
        temelKilit(ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, EsyaKayitlari.SISMOGRAF.get())
                .pattern("DKD").pattern("CRC").pattern("TBT")
                .define('B', Items.COPPER_INGOT).define('C', Items.STICK).define('D', Items.IRON_INGOT)
                .define('K', Items.PAPER).define('R', Items.REDSTONE).define('T', Items.STONE))
                .save(t -> liste.add(new Kayit(t, null)), EsyaKayitlari.TARIF_SISMOGRAF);
        // R-17: boş etiketin malzemesi Forge'da "Empty Tag" bariyeridir; etiket dolana kadar tarif hiç yüklenmez.
        JsonArray cekirdekVar = CraftingHelper.serialize(new ICondition[] {
                new NotCondition(new TagEmptyCondition(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK_ETIKETI.location()))});
        InventoryChangeTrigger.TriggerInstance cekirdek = InventoryChangeTrigger.TriggerInstance.hasItems(
                ItemPredicate.Builder.item().of(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK_ETIKETI).build());
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, EsyaKayitlari.GELISMIS_HAVA_RADARI.get())
                .requires(EsyaKayitlari.HAVA_RADARI.get()).requires(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK_ETIKETI)
                .unlockedBy("has_hava_radari", InventoryChangeTrigger.TriggerInstance.hasItems(EsyaKayitlari.HAVA_RADARI.get()))
                .unlockedBy("has_kozmik_cekirdek", cekirdek)
                .save(t -> liste.add(new Kayit(t, cekirdekVar)), EsyaKayitlari.TARIF_GELISMIS_HAVA_RADARI);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, EsyaKayitlari.HASSAS_SISMOGRAF.get())
                .requires(EsyaKayitlari.SISMOGRAF.get()).requires(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK_ETIKETI)
                .unlockedBy("has_sismograf", InventoryChangeTrigger.TriggerInstance.hasItems(EsyaKayitlari.SISMOGRAF.get()))
                .unlockedBy("has_kozmik_cekirdek", cekirdek)
                .save(t -> liste.add(new Kayit(t, cekirdekVar)), EsyaKayitlari.TARIF_HASSAS_SISMOGRAF);
        return liste;
    }

    /**
     * Temel tarifler oyunun başında kitapta: {@code minecraft:tick} oyuncunun ilk tick'inde (var olan dünyada da) açar; demir
     * külçesi ve kızıltaş iki tarifte de olan yaygın malzemelerdir.
     */
    private static ShapedRecipeBuilder temelKilit(ShapedRecipeBuilder tarif) {
        return tarif.unlockedBy("oyun_basi", PlayerTrigger.TriggerInstance.tick())
                .unlockedBy("has_iron_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(Items.IRON_INGOT))
                .unlockedBy("has_redstone", InventoryChangeTrigger.TriggerInstance.hasItems(Items.REDSTONE));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput onbellek) {
        Path veri = cikti.getOutputFolder(PackOutput.Target.DATA_PACK);
        Path kaynak = cikti.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(DogalAfetler.MOD_ID);
        List<CompletableFuture<?>> isler = new ArrayList<>();
        for (Kayit kayit : tarifler()) {
            JsonObject tarif = kayit.tarif().serializeRecipe();
            JsonObject basarim = kayit.tarif().serializeAdvancement();
            if (kayit.kosullar() != null) {
                tarif.add("conditions", kayit.kosullar().deepCopy());
                basarim.add("conditions", kayit.kosullar().deepCopy());
            }
            ResourceLocation id = kayit.tarif().getId();
            ResourceLocation basarimId = kayit.tarif().getAdvancementId();
            isler.add(DataProvider.saveStable(onbellek, tarif,
                    veri.resolve(id.getNamespace() + "/recipes/" + id.getPath() + ".json")));
            isler.add(DataProvider.saveStable(onbellek, basarim,
                    veri.resolve(basarimId.getNamespace() + "/advancements/" + basarimId.getPath() + ".json")));
        }
        for (String ad : List.of("hava_radari", "gelismis_hava_radari", "sismograf", "hassas_sismograf")) {
            JsonObject model = new JsonObject();
            model.addProperty("parent", "minecraft:item/generated");
            JsonObject dokular = new JsonObject();
            dokular.addProperty("layer0", DogalAfetler.MOD_ID + ":item/" + ad);
            model.add("textures", dokular);
            isler.add(DataProvider.saveStable(onbellek, model, kaynak.resolve("models/item/" + ad + ".json")));
        }
        return CompletableFuture.allOf(isler.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Doğal Afetler radar ve sismograf tarifleri ve modelleri";
    }
}
