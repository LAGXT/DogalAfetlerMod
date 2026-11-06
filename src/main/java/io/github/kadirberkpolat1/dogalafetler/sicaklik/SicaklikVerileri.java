package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Sıcaklık sisteminin veri paketi dosyaları (datagen): iki hasar tipi ({@code damage_type}), hasar tipi etiketleri ve
 * yünlü zırh tarifi ({@code dogalafetler:zirh_eki}). Etiketler: ikisi de zırhı deler ve geri tepmesizdir; donma
 * {@code is_freezing}, aşırı ısınma {@code is_fire} (Ateşe Direnç iksiri aşırı ısınma hasarını önler).
 */
public final class SicaklikVerileri implements DataProvider {
    private final PackOutput cikti;

    private SicaklikVerileri(PackOutput cikti) {
        this.cikti = cikti;
    }

    static void ekle(GatherDataEvent olay) {
        olay.getGenerator().addProvider(olay.includeServer(),
                new SicaklikVerileri(olay.getGenerator().getPackOutput()));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput onbellek) {
        Path veri = cikti.getOutputFolder(PackOutput.Target.DATA_PACK);
        List<CompletableFuture<?>> isler = new ArrayList<>();
        isler.add(DataProvider.saveStable(onbellek, hasarTipi("donma", "freezing"),
                veri.resolve(DogalAfetler.MOD_ID + "/damage_type/donma.json")));
        isler.add(DataProvider.saveStable(onbellek, hasarTipi("asiri_isinma", "burning"),
                veri.resolve(DogalAfetler.MOD_ID + "/damage_type/asiri_isinma.json")));
        isler.add(etiket(onbellek, veri, "bypasses_armor", "donma", "asiri_isinma"));
        isler.add(etiket(onbellek, veri, "no_knockback", "donma", "asiri_isinma"));
        isler.add(etiket(onbellek, veri, "is_freezing", "donma"));
        isler.add(etiket(onbellek, veri, "is_fire", "asiri_isinma"));

        JsonObject tarif = new JsonObject();
        tarif.addProperty("type", DogalAfetler.MOD_ID + ":zirh_eki");
        tarif.addProperty("category", "equipment");
        tarif.addProperty("ek", ZirhEkleri.YUNLU.anahtar());
        JsonObject malzeme = new JsonObject();
        malzeme.addProperty("tag", "minecraft:wool");
        tarif.add("malzeme", malzeme);
        isler.add(DataProvider.saveStable(onbellek, tarif,
                veri.resolve(DogalAfetler.MOD_ID + "/recipes/yunlu_zirh.json")));
        return CompletableFuture.allOf(isler.toArray(CompletableFuture[]::new));
    }

    private static JsonObject hasarTipi(String ad, String efekt) {
        JsonObject j = new JsonObject();
        j.addProperty("message_id", DogalAfetler.MOD_ID + "." + ad);
        j.addProperty("scaling", "never");
        j.addProperty("exhaustion", 0.0f);
        j.addProperty("effects", efekt);
        return j;
    }

    private static CompletableFuture<?> etiket(CachedOutput onbellek, Path veri, String etiket, String... hasarlar) {
        JsonObject j = new JsonObject();
        j.addProperty("replace", false);
        JsonArray degerler = new JsonArray();
        for (String h : hasarlar) {
            degerler.add(DogalAfetler.MOD_ID + ":" + h);
        }
        j.add("values", degerler);
        return DataProvider.saveStable(onbellek, j, veri.resolve("minecraft/tags/damage_type/" + etiket + ".json"));
    }

    @Override
    public String getName() {
        return "Doğal Afetler sıcaklık verileri";
    }
}
