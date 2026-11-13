package io.github.kadirberkpolat1.dogalafetler.esya.sogutma;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Serin Kolye: envanterde (ana envanter ya da sol el) taşındığı sürece ortam sıcakken serinletir; sıcak olmayan ortama dokunmaz.
 * Etkiyi bu sınıf değil {@code sicaklik.IsiKaynaklari} uygular ({@link Sogutma#kaydet} hedefe çeken soğutucu olarak kaydeder);
 * termometre dökümünde "soğutucu eşyalar" satırında görünür. Bu sınıf yalnız eşyayı ve ipucunu tanımlar.
 */
public final class SerinKolye extends Item {
    public static final String ACIKLAMA = "tooltip.dogalafetler.serin_kolye";

    public SerinKolye(Properties ozellikler) {
        super(ozellikler);
    }

    @Override
    public void appendHoverText(ItemStack yigin, @Nullable Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(ACIKLAMA).withStyle(ChatFormatting.AQUA));
    }
}
