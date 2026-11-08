package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Hissedilen;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Buz Kovası (S3-05): kova + buz. Sol eldeyken serinletir; etkiyi ve erimeyi sıcaklığın 1 Hz güncellemesi uygular
 * ({@code SicaklikYoneticisi}: sol el okuması, {@code Katki.SOL_EL}, gösterge efekti; mekan eşiğin üstündeyken erime NBT'si
 * artar, {@link Hissedilen#ERIME_SINIRI}'na varınca su kovasına döner). Bu sınıf yalnız eşyayı ve ipucunu tanımlar: "Sol elde
 * tutunca serinletir; sıcakta erir" ve erime başlamışsa yüzdesi. Yığın 1 (her kovanın kendi erimesi var).
 */
public final class BuzKovasi extends Item {
    BuzKovasi() {
        super(new Item.Properties().stacksTo(1));
    }

    /** Erime ilerlemesi 0–1; NBT yoksa 0. */
    public static double erimeOrani(ItemStack yigin) {
        CompoundTag etiket = yigin.getTag();
        if (etiket == null || !etiket.contains(SicaklikYoneticisi.ERIME_ETIKETI, Tag.TAG_ANY_NUMERIC)) {
            return 0;
        }
        return Math.max(0, Math.min(1, etiket.getDouble(SicaklikYoneticisi.ERIME_ETIKETI) / Hissedilen.ERIME_SINIRI));
    }

    /** Erime NBT'si saniyede bir yazılır; sol eldeki kova her seferinde aşağı inip kalkmasın. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack eski, ItemStack yeni, boolean yuvaDegisti) {
        return yuvaDegisti || !yeni.is(eski.getItem());
    }

    @Override
    public void appendHoverText(ItemStack yigin, @Nullable Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable("tooltip.dogalafetler.buz_kovasi").withStyle(ChatFormatting.AQUA));
        CompoundTag etiket = yigin.getTag();
        if (etiket != null && etiket.contains(SicaklikYoneticisi.ERIME_ETIKETI, Tag.TAG_ANY_NUMERIC)) {
            satirlar.add(Component.translatable("tooltip.dogalafetler.buz_kovasi.erime",
                    Math.round(erimeOrani(yigin) * 100)).withStyle(ChatFormatting.GRAY));
        }
    }
}
