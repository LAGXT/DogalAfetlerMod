package io.github.kadirberkpolat1.dogalafetler.veri;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * Vanilla ({@code minecraft:*}) blok etiketlerine bütün alt sistemlerin katkısını tek dosyada yazan sağlayıcı (K-E12). Etiket
 * sağlayıcısı dosya başına tam içerik yazar: aynı etiketi iki sağlayıcı yazarsa sonuncusu sessizce kazanır. Bu yüzden alt
 * sistemler vanilla etiketlere {@link DataGenerators#vanillaBlokEtiketiEkle} ile katkı ekler, kendi sağlayıcıları yalnız
 * {@code dogalafetler:*} etiketlerini yazar.
 */
final class OrtakEtiketler extends BlockTagsProvider {
    private final Map<TagKey<Block>, List<Supplier<? extends Block>>> katkilar;

    OrtakEtiketler(PackOutput cikti, CompletableFuture<HolderLookup.Provider> arama, ExistingFileHelper dosyalar,
            Map<TagKey<Block>, List<Supplier<? extends Block>>> katkilar) {
        super(cikti, arama, DogalAfetler.MOD_ID, dosyalar);
        this.katkilar = katkilar;
    }

    @Override
    public String getName() {
        return "Ortak vanilla blok etiketleri";
    }

    @Override
    protected void addTags(HolderLookup.Provider arama) {
        katkilar.forEach((etiket, bloklar) -> {
            var ekleyici = tag(etiket);
            for (Supplier<? extends Block> blok : bloklar) {
                ekleyici.add(blok.get());
            }
        });
    }
}
