package io.github.kadirberkpolat1.dogalafetler.sicaklik.uyum;

import com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.TempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.OrtamOlcer;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKapisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.OrtamSicakligi;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;

/**
 * Cold Sweat köprüsü (N8). Yalnız Cold Sweat yüklüyken yüklenir ({@code SicaklikKapisi.coldSweatYuklu()} koruması;
 * bağımlılık {@code compileOnly}, {@code mods.toml}'da {@code mandatory=false}). Bizim sistem kapalıyken dünya döneminin
 * sıcaklık etkisini (güneş patlaması, süpernova artığı, donma ilerlemesi, kızıl cüce ofseti ve kış bölgesi) Cold
 * Sweat'in dünya sıcaklığına bir {@code TempModifier} olarak ekler; böylece oyuncu tek sistemden (Cold Sweat'ten) hasar alır
 * ama afet dönemleri yine hissedilir. Bizim sistem açıkken değiştirici etkisizdir (çift etki olmaz).
 */
public final class ColdSweatKoprusu {
    public static final ResourceLocation KIMLIK = DogalAfetler.konum("donem");

    private ColdSweatKoprusu() {
    }

    public static void kur() {
        MinecraftForge.EVENT_BUS.addListener(ColdSweatKoprusu::degistiricileriKaydet);
        MinecraftForge.EVENT_BUS.addListener(ColdSweatKoprusu::varsayilanlaraEkle);
        DogalAfetler.LOGGER.info("Cold Sweat yüklü: dünya dönemi sıcaklığı Cold Sweat'e '{}' değiştiricisiyle aktarılacak",
                KIMLIK);
    }

    private static void degistiricileriKaydet(TempModifierRegisterEvent olay) {
        olay.register(KIMLIK, DonemDegistiricisi::new);
    }

    private static void varsayilanlaraEkle(DefaultTempModifiersEvent olay) {
        if (olay.getEntity() instanceof ServerPlayer) {
            olay.addModifier(Temperature.Trait.WORLD, new DonemDegistiricisi().tickRate(20));
        }
    }

    /** Dünya döneminin °C katkısı (bizim ortam hesabının dönem kalemleri). */
    static double donemKatkisi(ServerPlayer oyuncu) {
        SicaklikAyarlari.Ozet ozet = SicaklikAyarlari.al();
        OrtamSicakligi.Sonuc s = OrtamSicakligi.hesapla(OrtamOlcer.girdi(oyuncu, ozet), ozet.ortam());
        return s.katki(Katki.GUNES_EVRESI) + s.katki(Katki.DONMA) + s.katki(Katki.KIZIL_CUCE)
                + s.katki(Katki.KIS_BOLGESI);
    }

    /**
     * F-18: sıcaklık yiyeceğinin toplam etkisi (bizim vücut birimi) Cold Sweat çekirdek sıcaklığına bir kerede eklenir. İki
     * ölçek aynı yerde kırılır (0 rahat, ±100 hasar eşiği); dağıtım Cold Sweat'in çekirdek hızına kalır.
     */
    public static void yiyecekEtkisi(ServerPlayer oyuncu, double vucutBirimi) {
        Temperature.add(oyuncu, Temperature.Trait.CORE, vucutBirimi);
    }

    /** Cold Sweat çekirdek sıcaklığı (test ve log). */
    public static double cekirdek(ServerPlayer oyuncu) {
        return Temperature.get(oyuncu, Temperature.Trait.CORE);
    }

    /** Cold Sweat dünya sıcaklığına dönem katkısını ekleyen değiştirici. */
    public static final class DonemDegistiricisi extends TempModifier {
        @Override
        protected Function<Double, Double> calculate(LivingEntity varlik, Temperature.Trait ozellik) {
            if (!(varlik instanceof ServerPlayer oyuncu) || SicaklikKapisi.acikMi(oyuncu.server)) {
                return sicaklik -> sicaklik;
            }
            double ofset = Temperature.convert(donemKatkisi(oyuncu), Temperature.Units.C, Temperature.Units.MC, false);
            return sicaklik -> sicaklik + ofset;
        }
    }
}
