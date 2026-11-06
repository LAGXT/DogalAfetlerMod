package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;

/**
 * Çarpma sarsıntısında düşen "sağlam olmayan" bloklar (ay_carpmasi.md §2 63–75 sn, N12): yapraklar, cam, cam paneller,
 * fenerler. Altı boş olan aday blok düşen blok varlığına döner. Veri paketi etiketi genişletebilir; yalnız
 * {@code dogalafetler:*} etiketi yazılır (K-E12).
 */
public final class AyCarpmasiEtiketleri extends BlockTagsProvider {
    public static final TagKey<Block> SARSINTIDA_DUSER =
            TagKey.create(Registries.BLOCK, DogalAfetler.konum("ay_sarsintisi_duser"));

    private AyCarpmasiEtiketleri(PackOutput cikti, CompletableFuture<HolderLookup.Provider> arama,
            ExistingFileHelper dosyalar) {
        super(cikti, arama, DogalAfetler.MOD_ID, dosyalar);
    }

    static void ekle(GatherDataEvent olay) {
        olay.getGenerator().addProvider(olay.includeServer(), new AyCarpmasiEtiketleri(
                olay.getGenerator().getPackOutput(), olay.getLookupProvider(), olay.getExistingFileHelper()));
    }

    @Override
    public String getName() {
        return "Ay çarpması blok etiketleri";
    }

    @Override
    protected void addTags(HolderLookup.Provider arama) {
        tag(SARSINTIDA_DUSER).addTag(BlockTags.LEAVES).addTag(Tags.Blocks.GLASS).addTag(Tags.Blocks.GLASS_PANES)
                .add(Blocks.LANTERN, Blocks.SOUL_LANTERN);
    }
}
