package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge olayları: vanilla pişmiş yiyeceklerin çok küçük ısınması (madde 14, {@code LivingEntityUseItemEvent.Finish}; kendi
 * eşyalarımız kendi {@code finishUsingItem}'larında uygular), ölümsüz kopyada azalan verim geçmişi ve açılışta tarif
 * sayısı log'u (dedicated kabulü).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class YiyecekOlaylari {
    private YiyecekOlaylari() {
    }

    @SubscribeEvent
    public static void yemeBitti(LivingEntityUseItemEvent.Finish olay) {
        if (olay.getEntity() instanceof ServerPlayer oyuncu) {
            YiyecekAPI.vanillaTanimi(olay.getItem().getItem()).ifPresent(tanim -> YiyecekAPI.uygula(oyuncu, tanim));
        }
    }

    @SubscribeEvent
    public static void kopya(PlayerEvent.Clone olay) {
        if (!olay.isWasDeath() && olay.getOriginal() instanceof ServerPlayer eski
                && olay.getEntity() instanceof ServerPlayer yeni) {
            YiyecekAPI.kopyala(eski, yeni);
        }
    }

    @SubscribeEvent
    public static void sunucuBasladi(ServerStartedEvent olay) {
        MinecraftServer server = olay.getServer();
        List<String> tarifler = new ArrayList<>();
        for (Recipe<?> tarif : server.getRecipeManager().getRecipes()) {
            ItemStack sonuc = tarif.getResultItem(server.registryAccess());
            if (sonuc.getItem() instanceof SicaklikYiyecegi) {
                tarifler.add(tarif.getId().getPath() + "→" + sonuc.getCount());
            }
        }
        tarifler.sort(null);
        int balik = BuiltInRegistries.ITEM.getTag(YiyecekKayitlari.PISMIS_KUTUP_BALIGI).map(t -> t.size()).orElse(0);
        DogalAfetler.LOGGER.info("Yiyecek: {} sıcaklık yiyeceği tarifi yüklendi {}; #{} etiketinde {} eşya; vanillaIsinma={} ({} yiyecek)",
                tarifler.size(), tarifler, YiyecekKayitlari.PISMIS_KUTUP_BALIGI.location(), balik,
                YiyecekAyarlari.vanillaIsinma(), YiyecekAyarlari.vanillaTanimlari().size());
    }
}
