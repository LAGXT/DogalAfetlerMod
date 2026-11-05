package io.github.kadirberkpolat1.dogalafetler;

import com.mojang.logging.LogUtils;
import io.github.kadirberkpolat1.dogalafetler.config.Ayarlar;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekCevirileri;
import io.github.kadirberkpolat1.dogalafetler.komut.DogalAfetKomutu;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Doğal Afetler modunun giriş noktası. Alt sistemler kendi {@code DeferredRegister}'larını ve kayıt
 * metotlarını kendi paketlerinde tutar; bu sınıfa yalnız o metotları çağıran birer satır eklenir.
 */
@Mod(DogalAfetler.MOD_ID)
public final class DogalAfetler {
    public static final String MOD_ID = "dogalafetler";
    public static final Logger LOGGER = LogUtils.getLogger();

    public DogalAfetler(FMLJavaModLoadingContext baglam) {
        IEventBus modBus = baglam.getModEventBus();

        Ayarlar.kaydet(baglam);
        AgKanali.baslat();
        modBus.addListener(DogalAfetler::ortakKurulum);

        DataGenerators.ceviriEkle(CekirdekCevirileri::yaz);
        DataGenerators.ceviriEkle(Kurallar::cevirileriYaz);
        DataGenerators.ceviriEkle(DogalAfetKomutu::cevirileriYaz);
    }

    /** Mod ad alanında bir kaynak konumu. */
    public static ResourceLocation konum(String yol) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, yol);
    }

    private static void ortakKurulum(FMLCommonSetupEvent olay) {
        // GameRules kaydı thread-safe değil; ana thread kuyruğunda yapılır.
        olay.enqueueWork(Kurallar::kaydet);
    }
}
