package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Ortak gökyüzü sisteminin sunucu/ortak kayıtları (madde 16): config bölümleri, güneş zinciri dönem süreci, alt komutlar
 * ve dil girdileri. Yalnız {@code DogalAfetler} kurucusundan, koşulsuz çağrılır. İstemci tarafı
 * ({@code client.gokyuzu}) kendi {@code Dist.CLIENT} abonelikleriyle kurulur.
 */
public final class GokyuzuSistemi {
    private GokyuzuSistemi() {
    }

    public static void kaydet(IEventBus modBus) {
        GokyuzuAyarlari.kaydet();
        GunesZinciriSureci.kaydet();
        PulsarSureci.kaydet();
        GunSaatiSabitleyici.kaydet();
        GokyuzuKomutlari.kaydet();
        PulsarSesleri.kaydet(modBus);
        PulsarKayitlari.kaydet(modBus);
        DataGenerators.ceviriEkle(GokyuzuKomutlari::cevirileriYaz);
    }
}
