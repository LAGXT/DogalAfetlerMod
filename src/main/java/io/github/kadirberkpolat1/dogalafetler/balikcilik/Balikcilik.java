package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.veri.BalikcilikVeriSaglayicilari;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
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
        DataGenerators.ceviriEkle(BalikcilikCevirileri::yaz);
        // Lambda: sağlayıcı sınıfı (istemci model üreticilerine bağlı) yalnız datagen'de yüklenir (K-E9).
        DataGenerators.saglayiciEkle(olay -> BalikcilikVeriSaglayicilari.ekle(olay));
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
