package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Zırh tooltip satırları (N1): "Tüy Upgrade'i: N" ve kayıtlı ekler ("Yünlü", Faz 3 "Termostatlı"); modlu zırhta da. Yünde
 * yünlü zırh tarifinin ipucu: zırh eki özel tariftir, tarif kitabında görünmez.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SicaklikIpuclari {
    private SicaklikIpuclari() {
    }

    @SubscribeEvent
    public static void ipucu(ItemTooltipEvent olay) {
        ItemStack yigin = olay.getItemStack();
        if (yigin.is(ItemTags.WOOL)) {
            olay.getToolTip().add(Component.translatable("tooltip.dogalafetler.yun.tarif").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (!ZirhEkleri.zirhMi(yigin)) {
            return;
        }
        int satir = Math.min(1, olay.getToolTip().size());
        for (ZirhEkleri.Ek ek : ZirhEkleri.ekler()) {
            if (ZirhEkleri.varMi(yigin, ek)) {
                olay.getToolTip().add(satir++, Component.translatable(ek.ceviriAnahtari()).withStyle(ChatFormatting.GOLD));
            }
        }
        int tuy = ZirhEkleri.tuySayisi(yigin);
        if (tuy > 0) {
            olay.getToolTip().add(satir, Component.translatable("tooltip.dogalafetler.tuy", tuy)
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}
