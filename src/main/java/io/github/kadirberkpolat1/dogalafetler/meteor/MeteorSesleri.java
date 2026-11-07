package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.SesSaglayici;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Meteor sesleri (tasarım notu "Meteor" → "Ses"): uçuş kükremesi (8 sn döngü) ve çarpma patlaması (tek seferlik). Dosyalar
 * {@code tools/assets/ses_uret.py} ile üretilir; {@code sounds.json} ve altyazılar datagen'dir. İkisini de istemci çalar
 * ({@code client.meteor}): menzil, mesafe kazancı ve Doppler {@code mantik.MeteorSesMenzili}'ndedir.
 */
public final class MeteorSesleri {
    public static final ResourceLocation UCUS = DogalAfetler.konum("meteor_ucus");
    public static final ResourceLocation PATLAMA = DogalAfetler.konum("meteor_patlama");

    static final String UCUS_ALTYAZI = "subtitles.dogalafetler.meteor_ucus";
    static final String PATLAMA_ALTYAZI = "subtitles.dogalafetler.meteor_patlama";

    private static final DeferredRegister<SoundEvent> KAYIT = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);
    public static final RegistryObject<SoundEvent> UCUS_OLAYI = olay(UCUS);
    public static final RegistryObject<SoundEvent> PATLAMA_OLAYI = olay(PATLAMA);

    private MeteorSesleri() {
    }

    private static RegistryObject<SoundEvent> olay(ResourceLocation ad) {
        return KAYIT.register(ad.getPath(), () -> SoundEvent.createVariableRangeEvent(ad));
    }

    static void kaydet(IEventBus modBus) {
        KAYIT.register(modBus);
    }

    static void tanimla(SesSaglayici saglayici) {
        saglayici.tanimla(UCUS.getPath(), SoundDefinition.definition().subtitle(UCUS_ALTYAZI)
                .with(SoundDefinition.Sound.sound(UCUS, SoundDefinition.SoundType.SOUND)));
        saglayici.tanimla(PATLAMA.getPath(), SoundDefinition.definition().subtitle(PATLAMA_ALTYAZI)
                .with(SoundDefinition.Sound.sound(PATLAMA, SoundDefinition.SoundType.SOUND)));
    }

    static void cevirileriYaz(CeviriYazici yazici) {
        yazici.ekle(UCUS_ALTYAZI, "Meteor roars past", "Meteor uğuldayarak geçiyor");
        yazici.ekle(PATLAMA_ALTYAZI, "Meteor impact explodes", "Meteor çarpıp patlıyor");
    }
}
