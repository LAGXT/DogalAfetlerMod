package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Kasırga veri dosyaları (datagen, sunucu): hasar tipi {@code dogalafetler:kasirga} ve blok etiketi
 * {@code #dogalafetler:kasirga_dayanikli} (N12; {@code replace: false}, veri paketiyle genişler). Vanilla etikete katkı yok
 * (K-E12).
 */
final class KasirgaVerileri implements DataProvider {
    /**
     * Kasırganın kaldıramadığı bloklar: obsidyen, ağlayan obsidyen, ana kaya, deepslate ailesi (tam bloklar, cevherler,
     * merdiven/basamak/duvar varyantları; K0-33), güçlendirilmiş deepslate, canlanma çapası.
     */
    static final String[] DAYANIKLI = {"minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:bedrock",
        "minecraft:deepslate", "minecraft:cobbled_deepslate", "minecraft:polished_deepslate", "minecraft:deepslate_bricks",
        "minecraft:cracked_deepslate_bricks", "minecraft:deepslate_tiles", "minecraft:cracked_deepslate_tiles",
        "minecraft:chiseled_deepslate", "minecraft:reinforced_deepslate", "minecraft:respawn_anchor",
        "minecraft:deepslate_coal_ore", "minecraft:deepslate_iron_ore", "minecraft:deepslate_copper_ore",
        "minecraft:deepslate_gold_ore", "minecraft:deepslate_redstone_ore", "minecraft:deepslate_emerald_ore",
        "minecraft:deepslate_lapis_ore", "minecraft:deepslate_diamond_ore",
        "minecraft:cobbled_deepslate_stairs", "minecraft:cobbled_deepslate_slab", "minecraft:cobbled_deepslate_wall",
        "minecraft:polished_deepslate_stairs", "minecraft:polished_deepslate_slab", "minecraft:polished_deepslate_wall",
        "minecraft:deepslate_brick_stairs", "minecraft:deepslate_brick_slab", "minecraft:deepslate_brick_wall",
        "minecraft:deepslate_tile_stairs", "minecraft:deepslate_tile_slab", "minecraft:deepslate_tile_wall"};

    private final PackOutput cikti;

    private KasirgaVerileri(PackOutput cikti) {
        this.cikti = cikti;
    }

    static void ekle(GatherDataEvent olay) {
        olay.getGenerator().addProvider(olay.includeServer(), new KasirgaVerileri(olay.getGenerator().getPackOutput()));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput onbellek) {
        Path veri = cikti.getOutputFolder(PackOutput.Target.DATA_PACK).resolve(DogalAfetler.MOD_ID);
        JsonObject hasar = new JsonObject();
        hasar.addProperty("exhaustion", 0.1);
        hasar.addProperty("message_id", DogalAfetler.MOD_ID + ".kasirga");
        hasar.addProperty("scaling", "never");
        JsonObject etiket = new JsonObject();
        etiket.addProperty("replace", false);
        JsonArray degerler = new JsonArray();
        for (String blok : DAYANIKLI) {
            degerler.add(blok);
        }
        etiket.add("values", degerler);
        return CompletableFuture.allOf(
                DataProvider.saveStable(onbellek, hasar, veri.resolve("damage_type/kasirga.json")),
                DataProvider.saveStable(onbellek, etiket, veri.resolve("tags/blocks/kasirga_dayanikli.json")));
    }

    @Override
    public String getName() {
        return "Doğal Afetler kasırga verileri";
    }
}
