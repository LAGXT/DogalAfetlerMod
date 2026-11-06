package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Balıkçılığın eşya etiketleri (datagen {@code BalikcilikVeriSaglayicilari}). Yiyecek işçisi Kutup Balığı Çorbası tarifinde
 * {@link #PISMIS_KUTUP_BALIGI}'nı kullanır (madde 14); başarım işçisi balık sayısını {@link #CIG_KUTUP_BALIGI}'yla sayar.
 */
public final class BalikcilikEtiketleri {
    /** {@code #dogalafetler:pismis_kutup_baligi}: beş pişmiş buz balığı. */
    public static final TagKey<Item> PISMIS_KUTUP_BALIGI = ItemTags.create(DogalAfetler.konum("pismis_kutup_baligi"));
    /** {@code #dogalafetler:cig_kutup_baligi}: beş çiğ buz balığı. */
    public static final TagKey<Item> CIG_KUTUP_BALIGI = ItemTags.create(DogalAfetler.konum("cig_kutup_baligi"));

    private BalikcilikEtiketleri() {
    }
}
