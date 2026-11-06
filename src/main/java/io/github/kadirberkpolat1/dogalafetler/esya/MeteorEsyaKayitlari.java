package io.github.kadirberkpolat1.dogalafetler.esya;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.esya.termostat.TermostatPlakasi;
import io.github.kadirberkpolat1.dogalafetler.meteor.KozmikCekirdekEsyasi;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteoritParcasiEsyasi;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Meteor eşyaları (madde 13c; kimlikler sabit, başarım ve tarifler bunlara bağlı): {@code meteorit_parcasi},
 * {@code kozmik_cekirdek} (+ eşya etiketi {@code #dogalafetler:kozmik_cekirdek}), {@code meteorit_kulcesi},
 * {@code termostat_plakasi}. Radar/sismograf eşyalarının kaydından ayrı bir {@code DeferredRegister}'dır.
 */
public final class MeteorEsyaKayitlari {
    private static final DeferredRegister<Item> ESYALAR = DeferredRegister.create(ForgeRegistries.ITEMS,
            DogalAfetler.MOD_ID);

    public static final RegistryObject<Item> METEORIT_PARCASI = ESYALAR.register("meteorit_parcasi",
            () -> new MeteoritParcasiEsyasi(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> KOZMIK_CEKIRDEK = ESYALAR.register("kozmik_cekirdek",
            () -> new KozmikCekirdekEsyasi(new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> METEORIT_KULCESI = ESYALAR.register("meteorit_kulcesi",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> TERMOSTAT_PLAKASI = ESYALAR.register("termostat_plakasi",
            () -> new TermostatPlakasi(new Item.Properties().rarity(Rarity.UNCOMMON)));

    /** Gelişmiş Hava Radarı ve Hassas Sismograf tarifleri bu etiketi kullanır. */
    public static final TagKey<Item> KOZMIK_CEKIRDEK_ETIKETI = TagKey.create(Registries.ITEM,
            DogalAfetler.konum("kozmik_cekirdek"));

    private MeteorEsyaKayitlari() {
    }

    public static void kaydet(IEventBus modBus) {
        ESYALAR.register(modBus);
        modBus.addListener(MeteorEsyaKayitlari::sekmeyeEkle);
    }

    private static void sekmeyeEkle(BuildCreativeModeTabContentsEvent olay) {
        if (olay.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            olay.accept(METEORIT_PARCASI);
            olay.accept(METEORIT_KULCESI);
            olay.accept(TERMOSTAT_PLAKASI);
            olay.accept(KOZMIK_CEKIRDEK);
        }
    }
}
