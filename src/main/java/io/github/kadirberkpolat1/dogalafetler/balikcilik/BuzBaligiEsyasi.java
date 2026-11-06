package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.yiyecek.YiyecekAPI;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Buyukluk;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTanimi;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Yon;
import java.util.List;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Buz balığı (madde 15 "YİYECEK ÖZELLİKLERİ", madde 14): yenince normal yemek değerine ek olarak sıcaklık yiyeceği gibi
 * davranır ({@link YiyecekAPI#uygula}: kademe miktarı, durum efekti, 60 sn azalan verim). Çiğ küçük serinlik, pişmiş orta
 * ısınma, pişmiş Donmuş Ringa büyük ısınma (B-19). Azalan verim türü eşyanın kayıt kimliğidir: her balık, çiği ve pişmişi
 * ayrı türdür.
 */
public final class BuzBaligiEsyasi extends Item {
    /** Pişmiş Donmuş Ringa'nın büyük ısınma süresi (sn); yiyecek config'inde karşılığı yok (balıkçılık §E). */
    public static final int RINGA_SANIYE = 120;

    /** Sıcaklık etkisi sınıfı. */
    public enum Tur {
        CIG("tooltip.dogalafetler.buz_baligi.cig", ChatFormatting.AQUA, YiyecekAPI::cigBalik),
        PISMIS("tooltip.dogalafetler.buz_baligi.pismis", ChatFormatting.GOLD, YiyecekAPI::pismisBalik),
        PISMIS_RINGA("tooltip.dogalafetler.buz_baligi.pismis_ringa", ChatFormatting.RED,
                kimlik -> new YiyecekTanimi(kimlik, Yon.ISINMA, Buyukluk.BUYUK, RINGA_SANIYE, false));

        private final String ipucu;
        private final ChatFormatting renk;
        private final Function<String, YiyecekTanimi> tanim;

        Tur(String ipucu, ChatFormatting renk, Function<String, YiyecekTanimi> tanim) {
            this.ipucu = ipucu;
            this.renk = renk;
            this.tanim = tanim;
        }

        /** {@code kimlik} (eşya kayıt kimliği) türündeki balığın yiyecek tanımı. */
        public YiyecekTanimi tanim(String kimlik) {
            return tanim.apply(kimlik);
        }

        public String ipucu() {
            return ipucu;
        }
    }

    private final Tur tur;

    public BuzBaligiEsyasi(Tur tur, Item.Properties ozellikler) {
        super(ozellikler);
        this.tur = tur;
    }

    public Tur tur() {
        return tur;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack yigin, Level level, LivingEntity yiyen) {
        boolean yemek = isEdible();
        ItemStack sonuc = super.finishUsingItem(yigin, level, yiyen);
        if (yemek && !level.isClientSide && yiyen instanceof ServerPlayer oyuncu) {
            YiyecekAPI.uygula(oyuncu, tur.tanim(ForgeRegistries.ITEMS.getKey(this).toString()));
        }
        return sonuc;
    }

    @Override
    public void appendHoverText(ItemStack yigin, @Nullable Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(tur.ipucu).withStyle(tur.renk));
    }
}
