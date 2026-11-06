package io.github.kadirberkpolat1.dogalafetler.esya.sismograf;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

/**
 * Sismograf (madde 13b). Hotbar'da ya da yan elde taşınırken çalışır: deprem ve sinkhole'un öncü sarsıntısını algılar.
 * Sağ tık son 60 saniyenin sismik dalga grafiğini açar (yalnız istemcide; config ile kapatılabilir). Hassas sürüm
 * daha erken algılar, merkezi daha doğru gösterir ve seviyeyi belirtir.
 */
public final class Sismograf extends Item {
    private final boolean hassas;

    public Sismograf(boolean hassas) {
        super(new Item.Properties().stacksTo(1).rarity(hassas ? Rarity.RARE : Rarity.UNCOMMON));
        this.hassas = hassas;
    }

    public boolean hassas() {
        return hassas;
    }

    /** Hotbar'da ya da yan eldeki sismograf; Hassas olan önceliklidir. Yoksa {@code null}. */
    @Nullable
    public static Sismograf tasinan(Player oyuncu) {
        Sismograf secili = oyuncu.getOffhandItem().getItem() instanceof Sismograf s ? s : null;
        Inventory envanter = oyuncu.getInventory();
        for (int i = 0; i < Inventory.getSelectionSize() && (secili == null || !secili.hassas); i++) {
            if (envanter.getItem(i).getItem() instanceof Sismograf s && (secili == null || s.hassas)) {
                secili = s;
            }
        }
        return secili;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player oyuncu, InteractionHand el) {
        if (level.isClientSide()) {
            DistExecutor.safeRunWhenOn(Dist.CLIENT,
                    () -> io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf.IstemciSismograf::ekraniAc);
        }
        return InteractionResultHolder.sidedSuccess(oyuncu.getItemInHand(el), level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack yigin, @Nullable Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(getDescriptionId() + ".ipucu").withStyle(ChatFormatting.GRAY));
    }
}
