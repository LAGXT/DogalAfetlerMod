package io.github.kadirberkpolat1.dogalafetler;

import com.mojang.logging.LogUtils;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.GunesPatlamasiSistemi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.Balikcilik;
import io.github.kadirberkpolat1.dogalafetler.basarim.Basarim;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.config.CekirdekAyarlari;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.config.SunucuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekCevirileri;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiAfeti;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaSistemi;
import io.github.kadirberkpolat1.dogalafetler.esya.EsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GokyuzuSistemi;
import io.github.kadirberkpolat1.dogalafetler.komut.CekirdekKomutlari;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.meteor.Meteor;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.Sicaklik;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import io.github.kadirberkpolat1.dogalafetler.su.SuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.uyari.Uyari;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginSistemi;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.Yiyecek;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Doğal Afetler modunun giriş noktası. Alt sistemler kendi {@code DeferredRegister}'larını ve kayıt metotlarını kendi
 * paketlerinde tutar; bu sınıfa yalnız o metotları çağıran birer satır eklenir (config bölümleri, ağ paketi katkıları,
 * afet kayıtları, alt komutlar, datagen katkıları).
 */
@Mod(DogalAfetler.MOD_ID)
public final class DogalAfetler {
    public static final String MOD_ID = "dogalafetler";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DogalAfetler(FMLJavaModLoadingContext baglam) {
        IEventBus modBus = baglam.getModEventBus();

        AgKanali.baslat();
        modBus.addListener(DogalAfetler::ortakKurulum);

        CekirdekAyarlari.kaydet();
        SunucuAyarlari.kaydet();
        IstemciAyarlari.kaydet();
        // Afet kayıtları (Faz 4: AfetKaydi.kaydet yapan satırlar) buraya, ozelliktenKaydet'in üstüne: test afeti gerçek
        // afeti kayıtlı türü atlar; tersi sırada gerçek kayıt "zaten kayıtlı" ile modu düşürür (K-E22).
        OyunTestiAfeti.ozelliktenKaydet();
        SuAyarlari.kaydet();
        SanalSu.kaydet();
        Sicaklik.kaydet(modBus);
        Uyari.kaydet(modBus);
        GokyuzuSistemi.kaydet();
        YanginSistemi.kaydet(modBus);
        DonmaSistemi.kaydet(modBus);
        Yiyecek.kaydet(modBus);
        EsyaKayitlari.kaydet(modBus);
        Meteor.kaydet(modBus);
        Balikcilik.kaydet(modBus);
        Basarim.kaydet(modBus);
        GunesPatlamasiSistemi.kaydet(modBus);
        Kancalar.bagla();

        DataGenerators.ceviriEkle(CekirdekCevirileri::yaz);
        DataGenerators.ceviriEkle(Kurallar::cevirileriYaz);
        DataGenerators.ceviriEkle(CekirdekKomutlari::cevirileriYaz);

        // Config bölümleri spec kurulmadan eklenmeli: bu satır kurucunun sonunda kalır.
        AyarKaydi.kaydet(baglam);
    }

    /** Mod ad alanında bir kaynak konumu. */
    public static ResourceLocation konum(String yol) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, yol);
    }

    private static void ortakKurulum(FMLCommonSetupEvent olay) {
        // GameRules ve SimpleChannel kayıtları thread-safe değil; ana thread kuyruğunda, sabit sırayla yapılır.
        olay.enqueueWork(() -> {
            Kurallar.kaydet();
            AgKanali.paketleriKaydet();
        });
    }
}
