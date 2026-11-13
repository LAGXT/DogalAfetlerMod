package io.github.kadirberkpolat1.dogalafetler.rehber;

import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Rehber yazılarının (K-E74) kayıt noktası: ağ paketi, sunucu olayları ve dil anahtarları. Yalnız {@code DogalAfetler}
 * kurucusundan, koşulsuz çağrılır.
 */
public final class Rehber {
    private Rehber() {
    }

    public static void kaydet() {
        AgKanali.kaydet(k -> k.sunucudanIstemciye("rehber/mesaj", RehberMesajPaketi.class, RehberMesajPaketi::yaz,
                RehberMesajPaketi::oku, RehberMesajPaketi::isle));
        IEventBus forge = MinecraftForge.EVENT_BUS;
        forge.addListener(RehberOlaylari::sunucuTick);
        forge.addListener(RehberOlaylari::sagTik);
        forge.addListener(RehberOlaylari::kopyalandi);
        forge.addListener(RehberOlaylari::oyuncuCikti);
        forge.addListener(RehberOlaylari::sunucuDurdu);
        DataGenerators.ceviriEkle(RehberCevirileri::yaz);
    }
}
