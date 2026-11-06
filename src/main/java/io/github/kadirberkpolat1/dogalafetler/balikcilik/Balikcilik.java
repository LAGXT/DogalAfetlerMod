package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari.Kategori;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.veri.BalikcilikVeriSaglayicilari;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootDataManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Buz altı balıkçılığın kayıt noktası (madde 15): config, kayıtlar, delik açma, olta bekleme ve soğuma, balık tutuldu
 * olayı, yaratıcı sekmeler, datagen katkıları. Yalnız {@code DogalAfetler} kurucusundan çağrılır.
 */
public final class Balikcilik {
    private Balikcilik() {
    }

    public static void kaydet(IEventBus modBus) {
        BalikcilikAyarlari.kaydet();
        BalikcilikKayitlari.kaydet(modBus);
        modBus.addListener(Balikcilik::sekmeler);
        IEventBus forge = MinecraftForge.EVENT_BUS;
        forge.addListener(BuzDeligiAcma::sagTik);
        forge.addListener(BuzBalikciligi::varlikKatildi);
        forge.addListener(BuzBalikciligi::sunucuTick);
        forge.addListener(EventPriority.LOWEST, BuzBalikciligi::balikTutuldu);
        forge.addListener(BuzBalikciligi::sunucuDurdu);
        forge.addListener(BuzBalikciligi::donemDegisti);
        forge.addListener(Balikcilik::sunucuBasladi);
        DataGenerators.ceviriEkle(BalikcilikCevirileri::yaz);
        for (BalikcilikKayitlari.Balik b : BalikcilikKayitlari.Balik.values()) {
            DataGenerators.vanillaEsyaEtiketiEkle(ItemTags.FISHES, b::cig, b::pismis);
        }
        // Lambda: sağlayıcı sınıfı (istemci model üreticilerine bağlı) yalnız datagen'de yüklenir (K-E9).
        DataGenerators.saglayiciEkle(olay -> BalikcilikVeriSaglayicilari.ekle(olay));
    }

    /** Açılışta balıkçılık tablolarının yüklendiği denetlenir ve tek satır log yazılır (dedicated kanıtı). */
    private static void sunucuBasladi(ServerStartedEvent olay) {
        LootDataManager veri = olay.getServer().getLootData();
        List<String> eksik = new ArrayList<>();
        if (veri.getLootTable(BuzDeligiLootDegistirici.TABLO) == LootTable.EMPTY) {
            eksik.add(BuzDeligiLootDegistirici.TABLO.toString());
        }
        for (Kategori k : Kategori.values()) {
            if (veri.getLootTable(BuzDeligiLootDegistirici.kategoriTablosu(k)) == LootTable.EMPTY) {
                eksik.add(BuzDeligiLootDegistirici.kategoriTablosu(k).toString());
            }
        }
        if (eksik.isEmpty()) {
            DogalAfetler.LOGGER.info("[balikcilik] loot tablosu yüklendi: {} ({} kategori), oranlar {}",
                    BuzDeligiLootDegistirici.TABLO, Kategori.values().length, BalikcilikAyarlari.lootAgirliklari());
        } else {
            DogalAfetler.LOGGER.error("[balikcilik] loot tablosu eksik: {}; buz deliğinden balık çıkmaz", eksik);
        }
    }

    private static void sekmeler(BuildCreativeModeTabContentsEvent olay) {
        if (olay.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            for (BalikcilikKayitlari.Balik b : BalikcilikKayitlari.Balik.values()) {
                olay.accept(b.cig());
                olay.accept(b.pismis());
            }
        } else if (olay.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            Item parca = BalikcilikKayitlari.BUZ_PARCASI.get();
            olay.accept(parca);
        }
    }
}
