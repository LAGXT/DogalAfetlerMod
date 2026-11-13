package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ToolActions;

/**
 * Kutup Oltaı: özel kutup balıklarını (Kristal Levrek, Kutup Yılanbalığı, Aurora Balığı) tutabilen tek olta. Davranışı vanilla
 * olta ile aynıdır (Lure, Luck of the Sea, dayanıklılık); fark loot'tadır: buz deliğinde özel kategori
 * ({@link io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari.Kategori#OZEL}) yalnız bu olta elde
 * iken havuza girer, açık denizde özel balık global loot değtiricisi ({@code deniz_balikciligi_ozel}) {@code MatchTool}
 * koşuluyla bu oltaya bağlıdır.
 */
public final class KutupOltasi extends FishingRodItem {
    public static final String ACIKLAMA = "tooltip.dogalafetler.kutup_oltasi";
    public static final String ACIKLAMA_2 = "tooltip.dogalafetler.kutup_oltasi.2";
    public static final String ACIKLAMA_3 = "tooltip.dogalafetler.kutup_oltasi.3";

    public KutupOltasi(Properties ozellikler) {
        super(ozellikler);
    }

    /** Yiğın Kutup Oltası mı ({@code null} ve boş yiğın değil). */
    public static boolean mi(@Nullable ItemStack yigin) {
        return yigin != null && !yigin.isEmpty() && yigin.is(BalikcilikKayitlari.KUTUP_OLTASI.get());
    }

    /**
     * Oyuncunun attığı olta: vanilla gibi önce ana el, sonra yan el (olta fırlatabilen eşya, {@code FISHING_ROD_CAST});
     * ikisi de değilse boş yiğın.
     */
    public static ItemStack tutulanOlta(Player oyuncu) {
        ItemStack ana = oyuncu.getMainHandItem();
        if (ana.canPerformAction(ToolActions.FISHING_ROD_CAST)) {
            return ana;
        }
        ItemStack yan = oyuncu.getOffhandItem();
        return yan.canPerformAction(ToolActions.FISHING_ROD_CAST) ? yan : ItemStack.EMPTY;
    }

    @Override
    public void appendHoverText(ItemStack yigin, @Nullable Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(ACIKLAMA).withStyle(ChatFormatting.AQUA));
        satirlar.add(Component.translatable(ACIKLAMA_2).withStyle(ChatFormatting.GRAY));
        satirlar.add(Component.translatable(ACIKLAMA_3).withStyle(ChatFormatting.YELLOW));
    }
}
