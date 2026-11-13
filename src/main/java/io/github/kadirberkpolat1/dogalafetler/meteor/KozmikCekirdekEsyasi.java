package io.github.kadirberkpolat1.dogalafetler.meteor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Kozmik Çekirdek: büyü parlaması (her zaman), mor-mavi parıltı istemcide; ömür parçayla aynı. Bir zırh parçasıyla
 * iş tezgahında (şekilsiz tarif) birleşince zırhı Kozmik yapar ({@link MeteorZirhi}).
 */
public class KozmikCekirdekEsyasi extends MeteoritParcasiEsyasi {
    public KozmikCekirdekEsyasi(Properties ozellikler) {
        super(ozellikler);
    }

    @Override
    public boolean isFoil(ItemStack yigin) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack yigin, Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(MeteorCevirileri.CEKIRDEK_ACIKLAMASI).withStyle(ChatFormatting.LIGHT_PURPLE,
                ChatFormatting.ITALIC));
        satirlar.add(Component.translatable(MeteorZirhi.cekirdekZirhAciklamasi()).withStyle(ChatFormatting.GRAY));
    }
}
