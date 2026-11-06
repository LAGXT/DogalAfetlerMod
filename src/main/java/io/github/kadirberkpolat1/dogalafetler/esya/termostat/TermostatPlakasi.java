package io.github.kadirberkpolat1.dogalafetler.esya.termostat;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Termostat Plakası: zırh parçasıyla şekilsiz tarifte zırhı Termostatlı yapar (N3). */
public class TermostatPlakasi extends Item {
    public TermostatPlakasi(Properties ozellikler) {
        super(ozellikler);
    }

    @Override
    public void appendHoverText(ItemStack yigin, Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(Termostat.PLAKA_ACIKLAMASI).withStyle(ChatFormatting.GRAY));
    }
}
