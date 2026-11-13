package io.github.kadirberkpolat1.dogalafetler.uyum;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.sogutma.SogutmaKayitlari;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Curios veri paketi (K-E74): Serin Kolye {@code curios:necklace} etiketine girer, böylece Curios'un kolye yuvasına konabilir
 * ({@code data/curios/tags/items/necklace.json}). Kolye yuvası Curios'un kendi varsayılan yuvasıdır; yuva tanımı yazmak gerekmez.
 * Serinletme etkisi yuva için {@link CuriosKopru} ile okunur. Elle JSON yoktur; Curios yüklü olmasa da etiket zararsızdır.
 */
final class CuriosVerileri extends ItemTagsProvider {
    /** Curios'un kolye yuvası etiketi. */
    static final TagKey<Item> KOLYE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Uyum.CURIOS, "necklace"));

    private CuriosVerileri(PackOutput cikti, CompletableFuture<HolderLookup.Provider> arama, ExistingFileHelper dosyalar) {
        super(cikti, arama, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()), DogalAfetler.MOD_ID, dosyalar);
    }

    static void ekle(GatherDataEvent olay) {
        olay.getGenerator().addProvider(olay.includeServer(), new CuriosVerileri(olay.getGenerator().getPackOutput(),
                olay.getLookupProvider(), olay.getExistingFileHelper()));
    }

    @Override
    public String getName() {
        return "Curios eşya etiketleri";
    }

    @Override
    protected void addTags(HolderLookup.Provider arama) {
        tag(KOLYE).add(SogutmaKayitlari.SERIN_KOLYE.get());
    }
}
