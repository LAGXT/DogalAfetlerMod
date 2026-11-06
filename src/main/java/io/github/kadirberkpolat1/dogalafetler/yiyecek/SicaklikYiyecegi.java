package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTablosu;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTanimi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Madde 14'ün sıcaklık yiyeceği: normal yemek değeri + sıcaklık etkisi ({@link YiyecekAPI#uygula}, sunucuda). Yeme süresi
 * 32 tick, yığın 16; şişe/kase yenince boş kap geri verilir (yığın bitmediyse envantere, sığmazsa yere). Tok da olsa
 * yenir: sıcaklık yiyeceği zamanında yenmelidir.
 */
public final class SicaklikYiyecegi extends Item {
    public static final int YEME_SURESI = 32;
    public static final int YIGIN = 16;

    private final YiyecekTablosu.Kayit kayit;

    SicaklikYiyecegi(YiyecekTablosu.Kayit kayit) {
        super(new Item.Properties().stacksTo(YIGIN).food(new FoodProperties.Builder().nutrition(kayit.besin())
                .saturationMod(kayit.doygunluk()).alwaysEat().build()));
        this.kayit = kayit;
    }

    public YiyecekTablosu.Kayit kayit() {
        return kayit;
    }

    public YiyecekTanimi tanim() {
        return kayit.tanim();
    }

    @Override
    public int getUseDuration(ItemStack yigin) {
        return YEME_SURESI;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack yigin) {
        return kayit.kap() == YiyecekTablosu.Kap.SISE ? UseAnim.DRINK : UseAnim.EAT;
    }

    @Override
    public SoundEvent getEatingSound() {
        return kayit.kap() == YiyecekTablosu.Kap.SISE ? SoundEvents.GENERIC_DRINK : super.getEatingSound();
    }

    @Override
    public ItemStack finishUsingItem(ItemStack yigin, Level level, LivingEntity yiyen) {
        ItemStack kalan = super.finishUsingItem(yigin, level, yiyen);
        if (!level.isClientSide && yiyen instanceof ServerPlayer oyuncu) {
            YiyecekAPI.uygula(oyuncu, kayit.tanim());
        }
        ItemStack kap = bosKap();
        if (kap.isEmpty() || yiyen instanceof Player p && p.getAbilities().instabuild) {
            return kalan;
        }
        if (kalan.isEmpty()) {
            return kap;
        }
        if (yiyen instanceof Player p && !p.getInventory().add(kap)) {
            p.drop(kap, false);
        }
        return kalan;
    }

    /** Yenince geri verilen boş kap (karpuz diliminde yok). */
    public ItemStack bosKap() {
        return switch (kayit.kap()) {
            case SISE -> new ItemStack(Items.GLASS_BOTTLE);
            case KASE -> new ItemStack(Items.BOWL);
            case YOK -> ItemStack.EMPTY;
        };
    }
}
