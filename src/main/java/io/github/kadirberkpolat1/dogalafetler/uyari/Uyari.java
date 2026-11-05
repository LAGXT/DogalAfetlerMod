package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.komut.KomutKaydi;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Uyarı alt sisteminin kayıt noktası (ana sınıf yalnız {@link #kaydet} çağırır). */
public final class Uyari {
    public static final String KAYDIRICI = "options.dogalafetler.sirenler";
    public static final String KAYDIRICI_ACIKLAMA = "options.dogalafetler.sirenler.aciklama";

    private Uyari() {
    }

    public static void kaydet(IEventBus modBus) {
        SirenSesleri.KAYIT.register(modBus);
        modBus.addListener(Uyari::ortakKurulum);
        KomutKaydi.ekle(SirenKomutu.olustur());
        DataGenerators.sesEkle(SirenSesleri::tanimla);
        DataGenerators.ceviriEkle(Uyari::cevirileriYaz);
    }

    private static void ortakKurulum(FMLCommonSetupEvent olay) {
        SirenPaketleri.kaydet();
    }

    private static void cevirileriYaz(CeviriYazici yazici) {
        yazici.ekle(SirenSesleri.ALTYAZI, "Siren wails", "Siren çalıyor");
        yazici.ekle(KAYDIRICI, "Sirens", "Sirenler");
        yazici.ekle(KAYDIRICI_ACIKLAMA,
                "Disaster siren volume, on top of Master Volume. Same setting as sirenSes in dogalafetler-client.toml.",
                "Afet sireni ses seviyesi; Ana Ses ile çarpılır. dogalafetler-client.toml'daki sirenSes ile aynı ayar.");
        yazici.ekle(SirenKategorisi.DIL_ANAHTARI, "Sirens (category)", "Sirenler (kategori)");
    }
}
