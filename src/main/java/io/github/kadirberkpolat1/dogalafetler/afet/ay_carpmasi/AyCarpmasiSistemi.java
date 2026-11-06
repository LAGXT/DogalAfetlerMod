package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;

/** Ay çarpmasının mod kurucusundan çağrılan tek kayıt noktası (K-C33). */
public final class AyCarpmasiSistemi {
    private AyCarpmasiSistemi() {
    }

    public static void kaydet(IEventBus modBus) {
        AyCarpmasiAyarlari.kaydet();
        AyCarpmasiSesleri.kaydet(modBus);
        AfetKaydi.kaydet(AfetTuru.AY_CARPMASI, AyCarpmasi::new);
        DataGenerators.sesEkle(AyCarpmasiSesleri::tanimla);
        DataGenerators.ceviriEkle(AyCarpmasiSesleri::cevirileriYaz);
        DataGenerators.saglayiciEkle(AyCarpmasiEtiketleri::ekle);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, false, ServerStartedEvent.class,
                AyCarpmasiSistemi::sunucuBasladi);
    }

    /**
     * Çekirdek aktif afeti geri yükledikten sonra (NORMAL öncelik): ay YAKLASIYOR ama aktif afet ay çarpması değilse (afet kaydı
     * okunamadı ya da silindi) ay o hâlde takılı kalmasın, VAR'a döner (inceleme-ay-carpmasi P2).
     */
    private static void sunucuBasladi(ServerStartedEvent olay) {
        DunyaDonemi donem = DunyaDonemi.al(olay.getServer());
        if (donem.ayDurumu() != AyDurumu.YAKLASIYOR) {
            return;
        }
        boolean ayAfeti = AfetYoneticisi.al(olay.getServer()).aktifAfet().filter(AyCarpmasi.class::isInstance).isPresent();
        if (!ayAfeti) {
            donem.ayDurumuAyarla(AyDurumu.VAR, AyDurumu.OLAY_YOK);
            DogalAfetler.LOGGER.warn("Ay YAKLASIYOR ama aktif ay çarpması yok (kayıt okunamadı?); ay VAR'a döndü");
        }
    }
}
