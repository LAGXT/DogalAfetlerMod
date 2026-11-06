package io.github.kadirberkpolat1.dogalafetler.afet.deprem;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import io.github.kadirberkpolat1.dogalafetler.veri.SesSaglayici;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Deprem (bölüm C) ve sinkhole'un ortak kayıtları ve depremin mod kurucusundan çağrılan tek kayıt noktası (K-C33): afet
 * fabrikası, config bölümleri, iki ses ({@code deprem.gurleme} döngülü ortam katmanı, {@code sinkhole.cokus} tek seferlik
 * güm), {@code dogalafetler:deprem_kirilgan} etiketi ve dil katkıları.
 */
public final class DepremSistemi {
    private static final DeferredRegister<SoundEvent> SESLER = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);

    public static final ResourceLocation GURLEME_ADI = DogalAfetler.konum("deprem.gurleme");
    public static final ResourceLocation COKUS_ADI = DogalAfetler.konum("sinkhole.cokus");
    public static final RegistryObject<SoundEvent> GURLEME = SESLER.register(GURLEME_ADI.getPath(),
            () -> SoundEvent.createVariableRangeEvent(GURLEME_ADI));
    public static final RegistryObject<SoundEvent> COKUS = SESLER.register(COKUS_ADI.getPath(),
            () -> SoundEvent.createVariableRangeEvent(COKUS_ADI));

    private DepremSistemi() {
    }

    public static void kaydet(IEventBus modBus) {
        SESLER.register(modBus);
        DepremAyarlari.kaydet();
        DepremIstemciAyarlari.kaydet();
        AfetKaydi.kaydet(AfetTuru.DEPREM, Deprem::new);
        DataGenerators.sesEkle(DepremSistemi::sesleriTanimla);
        DataGenerators.ceviriEkle(DepremSistemi::cevirileriYaz);
        DataGenerators.saglayiciEkle(DepremEtiketleri::ekle);
    }

    private static void sesleriTanimla(SesSaglayici s) {
        s.tanimla(GURLEME_ADI.getPath(), SoundDefinition.definition().subtitle("subtitles.dogalafetler.deprem.gurleme")
                .with(SoundDefinition.Sound.sound(DogalAfetler.konum("deprem_gurleme"), SoundDefinition.SoundType.SOUND)));
        s.tanimla(COKUS_ADI.getPath(), SoundDefinition.definition().subtitle("subtitles.dogalafetler.sinkhole.cokus")
                .with(SoundDefinition.Sound.sound(DogalAfetler.konum("sinkhole_cokus"), SoundDefinition.SoundType.SOUND)
                        .attenuationDistance(64)));
    }

    private static void cevirileriYaz(CeviriYazici y) {
        y.ekle("subtitles.dogalafetler.deprem.gurleme", "Ground rumbles", "Yer gürlüyor");
        y.ekle("subtitles.dogalafetler.sinkhole.cokus", "Ground collapses", "Zemin çöküyor");
    }
}
