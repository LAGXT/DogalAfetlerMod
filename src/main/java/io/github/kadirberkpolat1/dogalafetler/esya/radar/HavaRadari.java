package io.github.kadirberkpolat1.dogalafetler.esya.radar;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Hava Radarı (madde 13a). Ana ya da yan elde tutulunca çalışır: sunucu durumu saniyede en çok bir kez yalnız o oyuncuya
 * gönderir, istemci küçük bir gösterge çizer. Gelişmiş sürüm daha uzun menzilli olup seviye, hareket yönü ve son 5
 * afeti de gösterir.
 */
public final class HavaRadari extends Item {
    private final boolean gelismis;

    public HavaRadari(boolean gelismis) {
        super(new Item.Properties().stacksTo(1).rarity(gelismis ? Rarity.RARE : Rarity.UNCOMMON));
        this.gelismis = gelismis;
    }

    public boolean gelismis() {
        return gelismis;
    }

    /** Ana ya da yan eldeki radar; ikisinde de varsa gelişmiş olan. Yoksa {@code null}. */
    @Nullable
    public static HavaRadari eldeki(Player oyuncu) {
        HavaRadari secili = null;
        for (InteractionHand el : InteractionHand.values()) {
            if (oyuncu.getItemInHand(el).getItem() instanceof HavaRadari radar && (secili == null || radar.gelismis)) {
                secili = radar;
            }
        }
        return secili;
    }

    @Override
    public void appendHoverText(ItemStack yigin, @Nullable Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(getDescriptionId() + ".ipucu").withStyle(ChatFormatting.GRAY));
    }
}
