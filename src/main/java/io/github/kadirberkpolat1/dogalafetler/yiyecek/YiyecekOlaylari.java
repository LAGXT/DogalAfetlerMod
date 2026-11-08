package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTablosu;
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
 * Forge olayları: vanilla yiyecek ve içeceklerin kademeli ısınma ve serinliği (madde 14, S3-06;
 * {@code LivingEntityUseItemEvent.Finish}, olayın eşyası yemeden önceki kopyadır: su şişesinin NBT'si okunur; kendi
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
            YiyecekAPI.vanillaTanimi(olay.getItem()).ifPresent(tanim -> YiyecekAPI.uygula(oyuncu, tanim));
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
        List<String> atlanan = new ArrayList<>();
        for (YiyecekTablosu.Kayit kayit : YiyecekTablosu.KAYITLAR) {
            if (server.getRecipeManager().byKey(DogalAfetler.konum(kayit.ad())).isEmpty()) {
                atlanan.add(kayit.ad());
            }
        }
        int balik = BuiltInRegistries.ITEM.getTag(YiyecekKayitlari.PISMIS_KUTUP_BALIGI).map(t -> t.size()).orElse(0);
        DogalAfetler.LOGGER.info("Yiyecek: {} sıcaklık yiyeceği tarifi yüklendi {}; koşullu atlanan {} {}; #{} etiketinde {} eşya;"
                + " vanillaIsinma={} ({} yiyecek), vanillaSerinlik={} ({} yiyecek)", tarifler.size(), tarifler, atlanan.size(),
                atlanan, YiyecekKayitlari.PISMIS_KUTUP_BALIGI.location(), balik, YiyecekAyarlari.vanillaIsinma(),
                YiyecekAyarlari.vanillaIsinmaTanimlari().size(), YiyecekAyarlari.vanillaSerinlik(),
                YiyecekAyarlari.vanillaSerinlikTanimlari().size());
    }
}
