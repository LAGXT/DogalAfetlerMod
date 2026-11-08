package io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok;

import javax.annotation.Nullable;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;

/**
 * Yakıt olmayan blok eşyası (kömürleşmiş kütük ve tahta, Nether odunları gibi): yanma süresi kesin 0. Etiketten gelen
 * yakıt süresi ({@code minecraft:planks}) {@code minecraft:non_flammable_wood} ile zaten düşer; bu geçersiz kılma, bir veri
 * paketi etiketleri değiştirse de fırının onu yakmamasını sağlar.
 */
public final class YakitsizBlokEsyasi extends BlockItem {
    public YakitsizBlokEsyasi(Block blok, Properties ozellikler) {
        super(blok, ozellikler);
    }

    @Override
    public int getBurnTime(ItemStack yigin, @Nullable RecipeType<?> tarifTuru) {
        return 0;
    }
}
