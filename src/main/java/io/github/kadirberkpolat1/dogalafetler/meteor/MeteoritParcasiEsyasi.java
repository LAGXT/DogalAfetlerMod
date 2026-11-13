package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZamani;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Meteorit Parçası ve Kozmik Çekirdek: yerde {@code meteor.odulOmruSaniye} (10 dk) sonra yok olur (M-07). Parça bir zırh
 * parçasıyla iş tezgahında (şekilsiz tarif) birleşince zırhı Meteoritli yapar ({@link MeteorZirhi}).
 */
public class MeteoritParcasiEsyasi extends Item {
    public MeteoritParcasiEsyasi(Properties ozellikler) {
        super(ozellikler);
    }

    @Override
    public int getEntityLifespan(ItemStack yigin, Level level) {
        try {
            return OdulZamani.omurTick(MeteorAyarlari.odulOmruSaniye());
        } catch (IllegalStateException yuklenmedi) {
            return OdulZamani.omurTick(OdulZamani.OMUR_SANIYE);
        }
    }

    @Override
    public void appendHoverText(ItemStack yigin, Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(MeteorZirhi.parcaAciklamasi()).withStyle(ChatFormatting.GRAY));
    }
}
