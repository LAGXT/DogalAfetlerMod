package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.SesSaglayici;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Siren ses olayları ve {@code sounds.json} tanımları. Dosyalar {@code tools/assets/ses_uret.py} ile üretilir
 * ({@code assets/dogalafetler/sounds/<ad>.ogg}).
 */
public final class SirenSesleri {
    public static final DeferredRegister<SoundEvent> KAYIT = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);

    /** Yalnız vanilla Altyazı açıkken görünür (madde 5); ayrı bir uyarı yazısı değildir. */
    public static final String ALTYAZI = "subtitles.dogalafetler.siren";

    private static final Map<SirenTuru, RegistryObject<SoundEvent>> OLAYLAR = new EnumMap<>(SirenTuru.class);

    static {
        for (SirenTuru tur : SirenTuru.values()) {
            OLAYLAR.put(tur, KAYIT.register(tur.sesAdi(),
                    () -> SoundEvent.createVariableRangeEvent(DogalAfetler.konum(tur.sesAdi()))));
        }
    }

    private SirenSesleri() {
    }

    public static SoundEvent olay(SirenTuru tur) {
        return OLAYLAR.get(tur).get();
    }

    /**
     * 10 sn'den uzun döngüler akışla (stream) çalınır, kısa olan bellekte durur (docs/tasarim/ses.md §5). Akışlı sesler
     * Forge'da {@code PlayStreamingSourceEvent}, bellektekiler {@code PlaySoundSourceEvent} tetikler.
     */
    static boolean akisla(SirenTuru tur) {
        return tur == SirenTuru.GUNES;
    }

    static void tanimla(SesSaglayici saglayici) {
        for (SirenTuru tur : SirenTuru.values()) {
            saglayici.tanimla(tur.sesAdi(), SoundDefinition.definition()
                    .subtitle(ALTYAZI)
                    .with(SoundDefinition.Sound.sound(DogalAfetler.konum(tur.sesAdi()), SoundDefinition.SoundType.SOUND)
                            .stream(akisla(tur))));
        }
    }
}
