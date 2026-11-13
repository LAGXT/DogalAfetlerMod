package io.github.kadirberkpolat1.dogalafetler.esya.sogutma;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Soğutma Plakası: zırh parçasıyla şekilsiz tarifte zırhı Soğutmalı yapar (Termostat Plakası'nın tersi). */
public class SogutmaPlakasi extends Item {
    public SogutmaPlakasi(Properties ozellikler) {
        super(ozellikler);
    }

    @Override
    public void appendHoverText(ItemStack yigin, Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(Sogutma.PLAKA_ACIKLAMASI).withStyle(ChatFormatting.AQUA));
    }
}
