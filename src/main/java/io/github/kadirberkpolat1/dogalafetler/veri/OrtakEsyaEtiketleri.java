package io.github.kadirberkpolat1.dogalafetler.veri;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Vanilla ({@code minecraft:*}) eşya etiketlerine bütün alt sistemlerin katkısını tek dosyada yazan sağlayıcı (K-E12, blok
 * sürümü {@link OrtakEtiketler}). Alt sistemler {@link DataGenerators#vanillaEsyaEtiketiEkle} ile katkı ekler.
 */
final class OrtakEsyaEtiketleri extends ItemTagsProvider {
    private final Map<TagKey<Item>, List<Supplier<? extends Item>>> katkilar;

    OrtakEsyaEtiketleri(PackOutput cikti, CompletableFuture<HolderLookup.Provider> arama, ExistingFileHelper dosyalar,
            Map<TagKey<Item>, List<Supplier<? extends Item>>> katkilar) {
        super(cikti, arama, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()), DogalAfetler.MOD_ID, dosyalar);
        this.katkilar = katkilar;
    }

    @Override
    public String getName() {
        return "Ortak vanilla eşya etiketleri";
    }

    @Override
    protected void addTags(HolderLookup.Provider arama) {
        katkilar.forEach((etiket, esyalar) -> {
            var ekleyici = tag(etiket);
            for (Supplier<? extends Item> esya : esyalar) {
                ekleyici.add(esya.get());
            }
        });
    }
}
