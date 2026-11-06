package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

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
 * Tsunaminin kendi sesleri (uyarı-ses §A4): yaklaşan dalganın döngülü uğultusu ({@code uyari.OrtamSesi} katmanı) ve
 * kıyıya vurma darbesi ({@code level.playSound}). Dosyalar {@code tools/assets/tsunami_ses.py} ile üretilir.
 */
public final class TsunamiSesleri {
    public static final ResourceLocation UGULTU = DogalAfetler.konum("tsunami_ugultu");
    public static final ResourceLocation CARPMA_ADI = DogalAfetler.konum("tsunami_carpma");
    static final String UGULTU_ALTYAZI = "subtitles.dogalafetler.tsunami_ugultu";
    static final String CARPMA_ALTYAZI = "subtitles.dogalafetler.tsunami_carpma";

    private static final DeferredRegister<SoundEvent> KAYIT = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);
    static final RegistryObject<SoundEvent> CARPMA = KAYIT.register(CARPMA_ADI.getPath(),
            () -> SoundEvent.createVariableRangeEvent(CARPMA_ADI));

    private TsunamiSesleri() {
    }

    static void kaydet(IEventBus modBus) {
        KAYIT.register(UGULTU.getPath(), () -> SoundEvent.createVariableRangeEvent(UGULTU));
        KAYIT.register(modBus);
    }

    static void tanimla(SesSaglayici saglayici) {
        saglayici.tanimla(UGULTU.getPath(), SoundDefinition.definition().subtitle(UGULTU_ALTYAZI)
                .with(SoundDefinition.Sound.sound(UGULTU, SoundDefinition.SoundType.SOUND).stream(true)));
        saglayici.tanimla(CARPMA_ADI.getPath(), SoundDefinition.definition().subtitle(CARPMA_ALTYAZI)
                .with(SoundDefinition.Sound.sound(CARPMA_ADI, SoundDefinition.SoundType.SOUND)));
    }

    static void cevirileriYaz(CeviriYazici yazici) {
        yazici.ekle(UGULTU_ALTYAZI, "Wave roars", "Dalga uğulduyor");
        yazici.ekle(CARPMA_ALTYAZI, "Wave crashes", "Dalga kıyıya vuruyor");
    }
}
