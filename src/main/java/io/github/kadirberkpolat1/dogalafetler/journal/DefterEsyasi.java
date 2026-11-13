package io.github.kadirberkpolat1.dogalafetler.journal;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Afet Defteri eşyası. Sağ tık, sunucuda defterin verisini kurup oyuncuya tek paketle yollar ({@link DefterPaketi#gonder});
 * ekranı istemci o paket gelince açar. Eşya tarifle yapılır, oyuna ilk girişte verilmez. Veri eşyada değil oyuncudadır:
 * defter kaybolsa, yenisi yapılsa istatistikler aynen kalır.
 */
public final class DefterEsyasi extends Item {
    public DefterEsyasi() {
        super(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player oyuncu, InteractionHand el) {
        if (!level.isClientSide() && oyuncu instanceof ServerPlayer sunucuOyuncusu) {
            DefterPaketi.gonder(sunucuOyuncusu);
        }
        return InteractionResultHolder.sidedSuccess(oyuncu.getItemInHand(el), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack yigin, @Nullable Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(getDescriptionId() + ".ipucu").withStyle(ChatFormatting.GRAY));
    }
}
