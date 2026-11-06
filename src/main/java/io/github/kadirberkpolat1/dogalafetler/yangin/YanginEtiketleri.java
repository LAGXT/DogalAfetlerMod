package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** N12 blok sınıfları; veri paketiyle genişletilir. Yanıcılık vanilla değerinden gelir, {@link #YANMAZ} onu ezer. */
public final class YanginEtiketleri {
    public static final TagKey<Block> YANMAZ = etiket("yanmaz");
    public static final TagKey<Block> ASIT_DAYANIKLI = etiket("asit_dayanikli");
    public static final TagKey<Block> DEPREM_DAYANIKLI = etiket("deprem_dayanikli");

    private YanginEtiketleri() {
    }

    private static TagKey<Block> etiket(String ad) {
        return TagKey.create(Registries.BLOCK, DogalAfetler.konum(ad));
    }
}
