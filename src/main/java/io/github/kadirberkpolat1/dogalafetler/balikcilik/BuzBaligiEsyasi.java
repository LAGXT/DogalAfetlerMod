package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.VucutSicakligi;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Buz balığı (madde 15 "YİYECEK ÖZELLİKLERİ", madde 14): yenince normal yemek değerine ek olarak vücut sıcaklığını
 * değiştirir: çiğ hafif serinletir, pişmiş orta ısıtır, pişmiş Donmuş Ringa daha çok ısıtır. Değerler config'tedir
 * ({@link BalikcilikAyarlari#yemekEtkisi}); etki {@link VucutSicakligi#etkiEkle} ile uygulanır (sıcaklık sistemi kapalıysa
 * hiçbir şey yazılmaz).
 */
public final class BuzBaligiEsyasi extends Item {
    /** Sıcaklık etkisi sınıfı. */
    public enum Tur {
        CIG("tooltip.dogalafetler.buz_baligi.cig", ChatFormatting.AQUA),
        PISMIS("tooltip.dogalafetler.buz_baligi.pismis", ChatFormatting.GOLD),
        PISMIS_RINGA("tooltip.dogalafetler.buz_baligi.pismis_ringa", ChatFormatting.RED);

        private final String ipucu;
        private final ChatFormatting renk;

        Tur(String ipucu, ChatFormatting renk) {
            this.ipucu = ipucu;
            this.renk = renk;
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
            BalikcilikAyarlari.YemekEtkisi e = BalikcilikAyarlari.yemekEtkisi(tur);
            VucutSicakligi.etkiEkle(oyuncu, e.anlik(), e.sureTick(), e.toplam());
        }
        return sonuc;
    }

    @Override
    public void appendHoverText(ItemStack yigin, @Nullable Level level, List<Component> satirlar, TooltipFlag bayrak) {
        satirlar.add(Component.translatable(tur.ipucu).withStyle(tur.renk));
    }
}
