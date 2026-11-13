package io.github.kadirberkpolat1.dogalafetler.esya.sogutma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Soğutucu eşyaların kayıtları (ısıtan eşyaların tersi): {@code sogutma_parcasi} (balıkçılık loot'undan; Meteorit
 * Parçası'nın soğuk karşılığı), {@code sogutma_kulcesi} (3 parça + 1 demir külçe),
 * {@code sogutma_plakasi} (Termostat Plakası'nın tersi), {@code serin_kolye} ve {@code soguk_iksiri}.
 */
public final class SogutmaKayitlari {
    private static final DeferredRegister<Item> ESYALAR = DeferredRegister.create(ForgeRegistries.ITEMS, DogalAfetler.MOD_ID);

    public static final RegistryObject<Item> SOGUTMA_PARCASI = ESYALAR.register("sogutma_parcasi",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> SOGUTMA_KULCESI = ESYALAR.register("sogutma_kulcesi",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SOGUTMA_PLAKASI = ESYALAR.register("sogutma_plakasi",
            () -> new SogutmaPlakasi(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SERIN_KOLYE = ESYALAR.register("serin_kolye",
            () -> new SerinKolye(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> SOGUK_IKSIRI = ESYALAR.register("soguk_iksiri",
            () -> new SogukIksiri(new Item.Properties().stacksTo(16)));

    private SogutmaKayitlari() {
    }

    static void kaydet(IEventBus modBus) {
        ESYALAR.register(modBus);
        modBus.addListener(SogutmaKayitlari::sekmeyeEkle);
    }

    private static void sekmeyeEkle(BuildCreativeModeTabContentsEvent olay) {
        if (olay.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            olay.accept(SOGUTMA_PARCASI);
            olay.accept(SOGUTMA_KULCESI);
            olay.accept(SOGUTMA_PLAKASI);
        } else if (olay.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            olay.accept(SERIN_KOLYE);
        } else if (olay.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            olay.accept(SOGUK_IKSIRI);
        }
    }
}
