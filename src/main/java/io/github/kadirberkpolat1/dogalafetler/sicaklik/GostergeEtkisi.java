package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.ItemStack;

/**
 * Yalnız gösterge olan durum efekti (F-05 kalıbı; örn. {@code dogalafetler:islak}): kendisi hiçbir şey yapmaz, durumu
 * sunucu güncellemesi her saniye tazeler. Süt silmez (durum sürerken ikonun kaybolması yanıltırdı); kategori nötr.
 */
public final class GostergeEtkisi extends MobEffect {
    GostergeEtkisi(int renk) {
        super(MobEffectCategory.NEUTRAL, renk);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }
}
