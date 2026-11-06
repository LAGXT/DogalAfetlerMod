package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.SesSaglayici;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Uyarı alt sisteminin ses olayları ve {@code sounds.json} tanımları. Dosyalar {@code tools/assets/ses_uret.py} ile
 * üretilir ({@code assets/dogalafetler/sounds/<ad>.ogg}). Alt yazılar yalnız vanilla Alt Yazı açıkken görünür (madde 5, N6).
 */
public final class UyariSesleri {
    public static final String SIREN_ALTYAZI = "subtitles.dogalafetler.siren";
    public static final String ORTAM_TEST_ALTYAZI = "subtitles.dogalafetler.ortam_test";
    /** {@link OrtamSesi} API'sinin sınama katmanı ("uzak bir uğultu"); GameTest ve istemci otomasyonu kullanır. */
    public static final ResourceLocation ORTAM_TEST = DogalAfetler.konum("ortam_test");

    private static final DeferredRegister<SoundEvent> KAYIT = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);

    private UyariSesleri() {
    }

    static void kaydet(IEventBus modBus) {
        for (SirenProfili profil : SirenProfili.values()) {
            olayKaydet(DogalAfetler.konum(profil.sesAdi()));
        }
        olayKaydet(ORTAM_TEST);
        KAYIT.register(modBus);
    }

    private static void olayKaydet(ResourceLocation ad) {
        KAYIT.register(ad.getPath(), () -> SoundEvent.createVariableRangeEvent(ad));
    }

    static void tanimla(SesSaglayici saglayici) {
        for (SirenProfili profil : SirenProfili.values()) {
            saglayici.tanimla(profil.sesAdi(), SoundDefinition.definition()
                    .subtitle(SIREN_ALTYAZI)
                    .with(SoundDefinition.Sound.sound(DogalAfetler.konum(profil.sesAdi()), SoundDefinition.SoundType.SOUND)
                            .stream(profil.akisla())));
        }
        saglayici.tanimla(ORTAM_TEST.getPath(), SoundDefinition.definition()
                .subtitle(ORTAM_TEST_ALTYAZI)
                .with(SoundDefinition.Sound.sound(ORTAM_TEST, SoundDefinition.SoundType.SOUND).stream(true)));
    }

    static void cevirileriYaz(CeviriYazici yazici) {
        yazici.ekle(SIREN_ALTYAZI, "Siren wails", "Siren çalıyor");
        yazici.ekle(ORTAM_TEST_ALTYAZI, "Distant rumble", "Uzak bir uğultu");
    }
}
