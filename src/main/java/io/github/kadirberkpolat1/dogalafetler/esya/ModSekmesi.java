package io.github.kadirberkpolat1.dogalafetler.esya;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Modun kendi yaratıcı sekmesi "Doğal Afetler" (ikon Kül Bloğu). İçerik {@code dogalafetler} ad alanındaki bütün kayıtlı
 * eşyalardan, kayıt sırasıyla, sekme her kurulduğunda doldurulur: sonradan eklenen eşya ayrıca yazılmadan görünür. Alt
 * sistemlerin vanilla sekmelere ekledikleri ({@code BuildCreativeModeTabContentsEvent}) yerinde kalır.
 */
public final class ModSekmesi {
    public static final String CEVIRI_ANAHTARI = "itemGroup." + DogalAfetler.MOD_ID;

    private static final DeferredRegister<CreativeModeTab> SEKMELER = DeferredRegister.create(
            Registries.CREATIVE_MODE_TAB, DogalAfetler.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SEKME = SEKMELER.register(DogalAfetler.MOD_ID,
            () -> CreativeModeTab.builder().title(Component.translatable(CEVIRI_ANAHTARI))
                    .icon(() -> new ItemStack(PulsarKayitlari.KUL_BLOGU.get()))
                    .displayItems((parametreler, cikti) -> {
                        for (Item esya : ForgeRegistries.ITEMS) {
                            ResourceLocation kimlik = ForgeRegistries.ITEMS.getKey(esya);
                            if (kimlik != null && DogalAfetler.MOD_ID.equals(kimlik.getNamespace())) {
                                cikti.accept(esya);
                            }
                        }
                    }).build());

    private ModSekmesi() {
    }

    public static void kaydet(IEventBus modBus) {
        SEKMELER.register(modBus);
        DataGenerators.ceviriEkle(ModSekmesi::cevirileriYaz);
    }

    static void cevirileriYaz(CeviriYazici y) {
        y.ekle(CEVIRI_ANAHTARI, "Natural Disasters", "Doğal Afetler");
    }
}
