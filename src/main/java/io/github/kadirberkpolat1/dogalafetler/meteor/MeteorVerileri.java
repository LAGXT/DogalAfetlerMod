package io.github.kadirberkpolat1.dogalafetler.meteor;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.termostat.Termostat;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Meteor ve termostat veri dosyaları (datagen): şekilsiz Meteorit Külçesi ve Termostat Plakası tarifleri, Termostatlı
 * zırh ({@code dogalafetler:zirh_eki}, sıcaklık §A3), eşya etiketi {@code #dogalafetler:kozmik_cekirdek} ve dört eşya
 * modeli. İki sağlayıcının adları ayrıdır (K-E5).
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
            Path veri = cikti.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(DogalAfetler.MOD_ID);
            isler.add(DataProvider.saveStable(onbellek, sekilsiz("meteorit_kulcesi", malzeme("item", "dogalafetler:meteorit_parcasi"),
                    malzeme("item", "dogalafetler:meteorit_parcasi"), malzeme("item", "dogalafetler:meteorit_parcasi"),
                    malzeme("tag", "forge:ingots/iron")), veri.resolve("recipes/meteorit_kulcesi.json")));
            isler.add(DataProvider.saveStable(onbellek, sekilsiz("termostat_plakasi",
                    malzeme("item", "dogalafetler:meteorit_kulcesi"), malzeme("tag", "forge:ingots/gold"),
                    malzeme("tag", "forge:dusts/redstone"), malzeme("tag", "forge:ingots/iron")),
                    veri.resolve("recipes/termostat_plakasi.json")));
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

    private static JsonObject malzeme(String tur, String kimlik) {
        JsonObject j = new JsonObject();
        j.addProperty(tur, kimlik);
        return j;
    }

    private static JsonObject sekilsiz(String sonuc, JsonObject... malzemeler) {
        JsonObject tarif = new JsonObject();
        tarif.addProperty("type", "minecraft:crafting_shapeless");
        tarif.addProperty("category", "misc");
        JsonArray liste = new JsonArray();
        for (JsonObject m : malzemeler) {
            liste.add(m);
        }
        tarif.add("ingredients", liste);
        JsonObject cikti = new JsonObject();
        cikti.addProperty("item", DogalAfetler.MOD_ID + ":" + sonuc);
        cikti.addProperty("count", 1);
        tarif.add("result", cikti);
        return tarif;
    }

    @Override
    public String getName() {
        return modeller ? "Doğal Afetler meteor eşya modelleri" : "Doğal Afetler meteor ve termostat verileri";
    }
}
