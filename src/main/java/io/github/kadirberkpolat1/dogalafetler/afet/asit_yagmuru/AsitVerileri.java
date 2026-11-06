package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru;

import com.google.gson.JsonObject;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.concurrent.CompletableFuture;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Asit yağmurunun veri paketi dosyası (datagen): {@code dogalafetler:asit} hasar tipi. Vanilla hasar tipi etiketine katkı
 * yoktur (K-E12: {@code minecraft:damage_type} etiketlerinin tek yazarı sıcaklık); bu yüzden zırh asit hasarını azaltır
 * ve yıpranır.
 */
final class AsitVerileri implements DataProvider {
    private final PackOutput cikti;

    private AsitVerileri(PackOutput cikti) {
        this.cikti = cikti;
    }

    static void ekle(GatherDataEvent olay) {
        olay.getGenerator().addProvider(olay.includeServer(), new AsitVerileri(olay.getGenerator().getPackOutput()));
    }

    @Override
    public CompletableFuture<?> run(CachedOutput onbellek) {
        JsonObject hasar = new JsonObject();
        hasar.addProperty("message_id", DogalAfetler.MOD_ID + ".asit");
        hasar.addProperty("scaling", "never");
        hasar.addProperty("exhaustion", 0.1f);
        return DataProvider.saveStable(onbellek, hasar, cikti.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(DogalAfetler.MOD_ID + "/damage_type/asit.json"));
    }

    @Override
    public String getName() {
        return "Doğal Afetler asit yağmuru verileri";
    }
}
