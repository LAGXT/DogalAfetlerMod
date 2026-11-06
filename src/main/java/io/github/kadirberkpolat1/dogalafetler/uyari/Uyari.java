package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;

/** Uyarı alt sisteminin kayıt noktası (K-C33: yalnız {@code DogalAfetler} kurucusundan, koşulsuz). */
public final class Uyari {
    public static final String KAYDIRICI = "options.dogalafetler.sirenler";
    public static final String KAYDIRICI_ACIKLAMA = "options.dogalafetler.sirenler.aciklama";

    private Uyari() {
    }

    public static void kaydet(IEventBus modBus) {
        UyariAyarlari.kaydet();
        UyariSesleri.kaydet(modBus);
        AgKanali.kaydet(UyariPaketleri::kaydet);
        DataGenerators.sesEkle(UyariSesleri::tanimla);
        DataGenerators.ceviriEkle(UyariSesleri::cevirileriYaz);
        DataGenerators.ceviriEkle(Uyari::cevirileriYaz);
    }

    private static void cevirileriYaz(CeviriYazici yazici) {
        yazici.ekle(KAYDIRICI, "Sirens", "Sirenler");
        yazici.ekle(KAYDIRICI_ACIKLAMA,
                "Disaster siren volume, on top of Master Volume. Same setting as uyari.sirenSes in dogalafetler-client.toml.",
                "Afet sireni ses seviyesi; Ana Ses ile çarpılır. dogalafetler-client.toml'daki uyari.sirenSes ile aynı ayar.");
    }
}
