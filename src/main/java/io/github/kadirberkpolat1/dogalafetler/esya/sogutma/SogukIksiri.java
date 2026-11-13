package io.github.kadirberkpolat1.dogalafetler.esya.sogutma;

import io.github.kadirberkpolat1.dogalafetler.yiyecek.YiyecekAPI;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Buyukluk;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTanimi;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Yon;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Soğuk İksiri: içince {@link YiyecekAPI#uygula} ile büyük serinlik (azalan verim: aynı tür 60 sn içinde tekrar içilirse etki
 * azalır) ve "Serinlik" durum efekti verir; şişe geri döner. Yemek değeri yoktur (içecek).
 */
public final class SogukIksiri extends Item {
    public static final int ICME_SURESI = 32;
    public static final int SERINLIK_SANIYE = 180;
    public static final String ACIKLAMA = "tooltip.dogalafetler.soguk_iksiri";

    public SogukIksiri(Properties ozellikler) {
        super(ozellikler);
    }

    /** {@code kimlik} (eşya kayıt kimliği) türündeki iksirin yiyecek tanımı: büyük serinlik. */
    public static YiyecekTanimi tanim(String kimlik) {
        return new YiyecekTanimi(kimlik, Yon.SERINLIK, Buyukluk.BUYUK, SERINLIK_SANIYE, false);
    }

    @Override
    public int getUseDuration(ItemStack yigin) {
        return ICME_SURESI;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack yigin) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public SoundEvent getEatingSound() {
        return SoundEvents.GENERIC_DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player oyuncu, InteractionHand el) {
        return ItemUtils.startUsingInstantly(level, oyuncu, el);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack yigin, Level level, LivingEntity icen) {
        Player oyuncu = icen instanceof Player p ? p : null;
        if (oyuncu instanceof ServerPlayer sunucuOyuncu) {
            CriteriaTriggers.CONSUME_ITEM.trigger(sunucuOyuncu, yigin);
            YiyecekAPI.uygula(sunucuOyuncu, tanim(String.valueOf(ForgeRegistries.ITEMS.getKey(this))));
        }
        if (oyuncu != null) {
            oyuncu.awardStat(Stats.ITEM_USED.get(this));
        }
        boolean yaratici = oyuncu != null && oyuncu.getAbilities().instabuild;
        if (!yaratici) {
            yigin.shrink(1);
        }
        if (yaratici) {
            return yigin;
        }
        ItemStack sise = new ItemStack(Items.GLASS_BOTTLE);
        if (yigin.isEmpty()) {
            return sise;
        }
        if (oyuncu != null && !oyuncu.getInventory().add(sise)) {
            oyuncu.drop(sise, false);
        }
        return yigin;
    }

    @Override
    public void appendHoverText(ItemStack yigin, @Nullable Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(ACIKLAMA).withStyle(ChatFormatting.AQUA));
    }
}
