package io.github.kadirberkpolat1.dogalafetler.client.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.SicaklikYiyecegi;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.YiyecekAPI;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.YiyecekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.YiyecekKayitlari;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Yon;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTanimi;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Yiyecek tooltip satırı (madde 14; N6 dışı eşya bilgisi): "İçten Isınma: orta, 90 sn" / "Serinlik: hafif, 60 sn";
 * Ateşli Güveç'te yan etki uyarısı. Vanilla pişmiş yiyeceklerde config açıksa ve senkronlanmışsa aynı satır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class YiyecekIpuclari {
    private YiyecekIpuclari() {
    }

    @SubscribeEvent
    public static void ipucu(ItemTooltipEvent olay) {
        Optional<YiyecekTanimi> tanim = tanim(olay.getItemStack().getItem());
        if (tanim.isEmpty()) {
            return;
        }
        List<Component> satirlar = olay.getToolTip();
        int yer = Math.min(1, satirlar.size());
        for (Component satir : satirlar(tanim.get())) {
            satirlar.add(yer++, satir);
        }
    }

    /** Tooltip satırları (otomasyon da log'a yazar). */
    public static List<Component> satirlar(YiyecekTanimi t) {
        Component etki = Component.translatable("tooltip.dogalafetler.yiyecek.etki",
                Component.translatable(YiyecekKayitlari.efekt(t.yon()).getDescriptionId()),
                Component.translatable("yiyecek.dogalafetler.buyukluk." + t.buyukluk().anahtar()), t.sureSaniye())
                .withStyle(t.yon() == Yon.ISINMA ? ChatFormatting.GOLD : ChatFormatting.AQUA);
        if (!t.sicaktaGuclenir()) {
            return List.of(etki);
        }
        return List.of(etki, Component.translatable("tooltip.dogalafetler.yiyecek.yan_etki").withStyle(ChatFormatting.RED));
    }

    private static Optional<YiyecekTanimi> tanim(Item esya) {
        if (esya instanceof SicaklikYiyecegi yiyecek) {
            return Optional.of(yiyecek.tanim());
        }
        return YiyecekAyarlari.yuklendiMi() ? YiyecekAPI.vanillaTanimi(esya) : Optional.empty();
    }
}
