package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import io.github.kadirberkpolat1.dogalafetler.veri.SesSaglayici;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Güneş patlaması ve kızıl cüce sesleri (tasarım §4). Dosyalar {@code tools/assets/gunes_sesleri.py} ile üretilir;
 * {@code sounds.json} ve altyazılar datagen'dir. Kavurucu rüzgâr sunucunun {@code OrtamSesi} katmanıdır; süpernova ve
 * kızıl cüce sesleri eşitlenmiş evre saatinden istemcide çalınır ({@code client.gunes_patlamasi}).
 */
public final class GunesPatlamasiSesleri {
    private static final DeferredRegister<SoundEvent> SESLER = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);

    public static final ResourceLocation KAVRULMA_RUZGAR = DogalAfetler.konum("gunes.kavrulma_ruzgar");
    public static final ResourceLocation SUPERNOVA_BASINC = DogalAfetler.konum("gunes.supernova_basinc");
    public static final ResourceLocation SUPERNOVA_KRISTAL = DogalAfetler.konum("gunes.supernova_kristal");
    public static final ResourceLocation KIZIL_CUCE_AKOR = DogalAfetler.konum("gunes.kizil_cuce_akor");

    private record Ses(ResourceLocation kimlik, String dosya, String ingilizce, String turkce) {
    }

    private static final List<Ses> LISTE = List.of(
            new Ses(KAVRULMA_RUZGAR, "gunes_kavrulma_ruzgar", "Scorching wind blows", "Kavurucu rüzgâr esiyor"),
            new Ses(SUPERNOVA_BASINC, "gunes_supernova_basinc", "Deep pressure rises", "Derin bir basınç yükseliyor"),
            new Ses(SUPERNOVA_KRISTAL, "gunes_supernova_kristal", "Distant crystal hum", "Uzak kristal uğultusu"),
            new Ses(KIZIL_CUCE_AKOR, "gunes_kizil_cuce_akor", "Warm chord at dawn", "Şafakta sıcak bir akor"));

    private GunesPatlamasiSesleri() {
    }

    static void kaydet(IEventBus modBus) {
        for (Ses s : LISTE) {
            SESLER.register(s.kimlik().getPath(), () -> SoundEvent.createVariableRangeEvent(s.kimlik()));
        }
        SESLER.register(modBus);
        DataGenerators.sesEkle(GunesPatlamasiSesleri::tanimla);
        DataGenerators.ceviriEkle(GunesPatlamasiSesleri::cevirileriYaz);
    }

    public static String altyazi(ResourceLocation ses) {
        return "subtitles." + ses.getNamespace() + "." + ses.getPath();
    }

    private static void tanimla(SesSaglayici saglayici) {
        for (Ses s : LISTE) {
            saglayici.tanimla(s.kimlik().getPath(), SoundDefinition.definition()
                    .subtitle(altyazi(s.kimlik()))
                    .with(SoundDefinition.Sound.sound(DogalAfetler.konum(s.dosya()), SoundDefinition.SoundType.SOUND)
                            .stream(s.kimlik() != KIZIL_CUCE_AKOR && s.kimlik() != SUPERNOVA_BASINC)));
        }
    }

    private static void cevirileriYaz(CeviriYazici y) {
        for (Ses s : LISTE) {
            y.ekle(altyazi(s.kimlik()), s.ingilizce(), s.turkce());
        }
    }
}
