package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
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
    }
}
