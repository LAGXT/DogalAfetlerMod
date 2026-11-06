package io.github.kadirberkpolat1.dogalafetler.esya.radar;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.EsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Radar ve sismografın datagen dosyaları: dört tarif (madde 13a-b birebir; gelişmişler şekilsiz +1
 * {@code #dogalafetler:kozmik_cekirdek}, etiket boşken yüklenmez: {@code forge:not(forge:tag_empty)}) ve dört eşya modeli. Tarif kimlikleri {@link EsyaKayitlari}'ndaki sabitlerdir.
 * Benzersiz sağlayıcı adı (K-E5).
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

    @Override
    public CompletableFuture<?> run(CachedOutput onbellek) {
        Path veri = cikti.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(DogalAfetler.MOD_ID);
        Path kaynak = cikti.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(DogalAfetler.MOD_ID);
        List<CompletableFuture<?>> isler = new ArrayList<>();
        // 13a: üst Demir/Cam/Demir, orta Kızıltaş/Pusula/Kızıltaş, alt Demir/Bakır/Demir.
        isler.add(kaydet(onbellek, veri, EsyaKayitlari.TARIF_HAVA_RADARI.getPath(), sekilli("hava_radari",
                new String[] {"DCD", "KPK", "DBD"}, Map.of('D', "minecraft:iron_ingot", 'C', "minecraft:glass", 'K',
                        "minecraft:redstone", 'P', "minecraft:compass", 'B', "minecraft:copper_ingot"))));
        // 13b: üst Demir/Kağıt/Demir, orta Çubuk/Kızıltaş/Çubuk, alt Taş/Bakır/Taş.
        isler.add(kaydet(onbellek, veri, EsyaKayitlari.TARIF_SISMOGRAF.getPath(), sekilli("sismograf",
                new String[] {"DKD", "CRC", "TBT"}, Map.of('D', "minecraft:iron_ingot", 'K', "minecraft:paper", 'C',
                        "minecraft:stick", 'R', "minecraft:redstone", 'T', "minecraft:stone", 'B',
                        "minecraft:copper_ingot"))));
        isler.add(kaydet(onbellek, veri, EsyaKayitlari.TARIF_GELISMIS_HAVA_RADARI.getPath(),
                sekilsiz("gelismis_hava_radari", "hava_radari")));
        isler.add(kaydet(onbellek, veri, EsyaKayitlari.TARIF_HASSAS_SISMOGRAF.getPath(),
                sekilsiz("hassas_sismograf", "sismograf")));
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

    private static CompletableFuture<?> kaydet(CachedOutput onbellek, Path veri, String ad, JsonObject tarif) {
        return DataProvider.saveStable(onbellek, tarif, veri.resolve("recipes/" + ad + ".json"));
    }

    private static JsonObject sekilli(String sonuc, String[] desen, Map<Character, String> anahtar) {
        JsonObject j = new JsonObject();
        j.addProperty("type", "minecraft:crafting_shaped");
        j.addProperty("category", "equipment");
        JsonArray satirlar = new JsonArray();
        for (String satir : desen) {
            satirlar.add(satir);
        }
        j.add("pattern", satirlar);
        JsonObject anahtarlar = new JsonObject();
        anahtar.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e -> {
            JsonObject esya = new JsonObject();
            esya.addProperty("item", e.getValue());
            anahtarlar.add(String.valueOf(e.getKey()), esya);
        });
        j.add("key", anahtarlar);
        j.add("result", sonuc(sonuc));
        return j;
    }

    private static JsonObject sekilsiz(String sonuc, String temel) {
        JsonObject j = new JsonObject();
        j.addProperty("type", "minecraft:crafting_shapeless");
        j.addProperty("category", "equipment");
        // R-17: boş etiketin malzemesi Forge'da "Empty Tag" bariyeridir; etiket dolana kadar tarif hiç yüklenmez.
        JsonObject bos = new JsonObject();
        bos.addProperty("type", "forge:tag_empty");
        bos.addProperty("tag", MeteorEsyaKayitlari.KOZMIK_CEKIRDEK_ETIKETI.location().toString());
        JsonObject degil = new JsonObject();
        degil.addProperty("type", "forge:not");
        degil.add("value", bos);
        JsonArray kosullar = new JsonArray();
        kosullar.add(degil);
        j.add("conditions", kosullar);
        JsonArray malzemeler = new JsonArray();
        JsonObject esya = new JsonObject();
        esya.addProperty("item", DogalAfetler.MOD_ID + ":" + temel);
        malzemeler.add(esya);
        JsonObject cekirdek = new JsonObject();
        cekirdek.addProperty("tag", MeteorEsyaKayitlari.KOZMIK_CEKIRDEK_ETIKETI.location().toString());
        malzemeler.add(cekirdek);
        j.add("ingredients", malzemeler);
        j.add("result", sonuc(sonuc));
        return j;
    }

    private static JsonObject sonuc(String ad) {
        JsonObject j = new JsonObject();
        j.addProperty("item", DogalAfetler.MOD_ID + ":" + ad);
        return j;
    }

    @Override
    public String getName() {
        return "Doğal Afetler radar ve sismograf tarifleri ve modelleri";
    }
}
