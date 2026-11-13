package io.github.kadirberkpolat1.dogalafetler.journal;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Afet Defteri alt sisteminin mod kurucusundan çağrılan tek kayıt noktası (K-C33: koşulsuz, açık {@code kaydet}): eşya, tarif
 * kimliği, {@code defter/veri} ağ paketi, {@code /dogalafet defter} komutu ve datagen katkıları (dil, tarif, model).
 * İstatistikleri {@link DefterOlaylari} toplar, {@link DefterVerisi} saklar.
 */
public final class Defter {
    private static final DeferredRegister<Item> ESYALAR = DeferredRegister.create(ForgeRegistries.ITEMS, DogalAfetler.MOD_ID);

    public static final RegistryObject<DefterEsyasi> ESYA = ESYALAR.register("afet_defteri", DefterEsyasi::new);

    /** Tarif kimliği (sabit). */
    public static final ResourceLocation TARIF = DogalAfetler.konum("afet_defteri");

    private Defter() {
    }

    public static void kaydet(IEventBus modBus) {
        ESYALAR.register(modBus);
        modBus.addListener(Defter::sekmeyeEkle);
        DefterPaketi.kaydet();
        DefterKomutu.kaydet();
        DataGenerators.ceviriEkle(DefterCevirileri::yaz);
        DataGenerators.saglayiciEkle(DefterVerileri::ekle);
    }

    /** Balıkçılık, oyuncu bir buz deliği açınca çağırır ({@code BuzDeligiAcma}); sunucu oyuncusuysa deftere yazılır. */
    public static void buzDeligiAcildi(Player oyuncu) {
        if (oyuncu instanceof ServerPlayer sunucuOyuncusu) {
            DefterVerisi.delikAcildi(sunucuOyuncusu);
        }
    }

    private static void sekmeyeEkle(BuildCreativeModeTabContentsEvent olay) {
        if (olay.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            olay.accept(ESYA);
        }
    }
}
