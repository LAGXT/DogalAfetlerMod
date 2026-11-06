package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi;

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
 * Ay çarpmasının sesleri (ay_carpmasi.md §4): yaklaşma uğultusu ve parçalanma çıtırtısı (döngülü, {@code OrtamSesi}
 * global katmanı), çarpma gümü ve uzun gürleme (tek seferlik, her oyuncuya ayrı). Dosyalar
 * {@code tools/assets/ay_carpmasi_ses.py} ile üretilir. Siren çekirdeğin ve uyarınındır (afet siren çalmaz).
 */
public final class AyCarpmasiSesleri {
    public static final ResourceLocation YAKLASMA = DogalAfetler.konum("ay_carpmasi_yaklasma");
    public static final ResourceLocation CITIRTI = DogalAfetler.konum("ay_carpmasi_citirti");
    public static final ResourceLocation CARPMA = DogalAfetler.konum("ay_carpmasi_carpma");

    static final String YAKLASMA_ALTYAZI = "subtitles.dogalafetler.ay_carpmasi_yaklasma";
    static final String CITIRTI_ALTYAZI = "subtitles.dogalafetler.ay_carpmasi_citirti";
    static final String CARPMA_ALTYAZI = "subtitles.dogalafetler.ay_carpmasi_carpma";

    private static final DeferredRegister<SoundEvent> KAYIT = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);
    private static RegistryObject<SoundEvent> carpmaOlayi;

    private AyCarpmasiSesleri() {
    }

    private static RegistryObject<SoundEvent> olay(ResourceLocation ad) {
        return KAYIT.register(ad.getPath(), () -> SoundEvent.createVariableRangeEvent(ad));
    }

    static void kaydet(IEventBus modBus) {
        olay(YAKLASMA);
        olay(CITIRTI);
        carpmaOlayi = olay(CARPMA);
        KAYIT.register(modBus);
    }

    static SoundEvent carpma() {
        return carpmaOlayi.get();
    }

    static void tanimla(SesSaglayici saglayici) {
        tanim(saglayici, YAKLASMA, YAKLASMA_ALTYAZI);
        tanim(saglayici, CITIRTI, CITIRTI_ALTYAZI);
        tanim(saglayici, CARPMA, CARPMA_ALTYAZI);
    }

    private static void tanim(SesSaglayici saglayici, ResourceLocation ad, String altyazi) {
        saglayici.tanimla(ad.getPath(), SoundDefinition.definition().subtitle(altyazi)
                .with(SoundDefinition.Sound.sound(ad, SoundDefinition.SoundType.SOUND).stream(true)));
    }

    static void cevirileriYaz(CeviriYazici yazici) {
        yazici.ekle(YAKLASMA_ALTYAZI, "Deep rumble from the sky", "Gökten derin uğultu");
        yazici.ekle(CITIRTI_ALTYAZI, "Moon cracks", "Ay çatırdıyor");
        yazici.ekle(CARPMA_ALTYAZI, "Distant impact roars", "Uzak çarpma gürlüyor");
    }
}
