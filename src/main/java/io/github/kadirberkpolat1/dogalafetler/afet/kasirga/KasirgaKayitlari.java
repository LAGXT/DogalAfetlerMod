package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.SesSaglayici;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.SoundDefinition;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Kasırganın varlık türleri, ses olayları, hasar tipi ve blok etiketi. Varlıklar kaydedilmez ({@code noSave}): kasırga durumu
 * afetin NBT'sindedir ({@code DunyaDonemi}), varlık yalnız görüntü ve etki taşıyıcısıdır (spike B8).
 */
public final class KasirgaKayitlari {
    private static final DeferredRegister<EntityType<?>> VARLIKLAR = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
            DogalAfetler.MOD_ID);
    private static final DeferredRegister<SoundEvent> SESLER = DeferredRegister.create(Registries.SOUND_EVENT,
            DogalAfetler.MOD_ID);

    /** İzleme menzili 20 chunk (sunucunun görüş mesafesiyle sınırlı): huni ve duvar bulutu uzaktan görünür; konum 4 Hz. */
    public static final RegistryObject<EntityType<KasirgaVarligi>> KASIRGA = VARLIKLAR.register("kasirga",
            () -> EntityType.Builder.<KasirgaVarligi>of(KasirgaVarligi::new, MobCategory.MISC).sized(1f, 1f).noSave()
                    .fireImmune().clientTrackingRange(20).updateInterval(KasirgaVarligi.SENKRON_ARALIGI)
                    .build(DogalAfetler.konum("kasirga").toString()));

    /** Hızlı hareket ettiği için 10 Hz; 10 chunk menzil. */
    public static final RegistryObject<EntityType<UcanBlokVarligi>> UCAN_BLOK = VARLIKLAR.register("ucan_blok",
            () -> EntityType.Builder.<UcanBlokVarligi>of(UcanBlokVarligi::new, MobCategory.MISC).sized(0.98f, 0.98f).noSave()
                    .clientTrackingRange(10).updateInterval(UcanBlokVarligi.SENKRON_ARALIGI)
                    .build(DogalAfetler.konum("ucan_blok").toString()));

    /** Uzak katman: "yük treni" uğultusu, 40-120 Hz (tasarım notu §4). */
    public static final ResourceLocation UGULTU = DogalAfetler.konum("kasirga_ugultu");
    /** Yakın katman: derin rüzgâr kükremesi ve 2 Hz basınç pompası. */
    public static final ResourceLocation RUZGAR = DogalAfetler.konum("kasirga_ruzgar");
    public static final String UGULTU_ALTYAZI = "subtitles.dogalafetler.kasirga_ugultu";
    public static final String RUZGAR_ALTYAZI = "subtitles.dogalafetler.kasirga_ruzgar";

    public static final ResourceKey<DamageType> HASAR = ResourceKey.create(Registries.DAMAGE_TYPE,
            DogalAfetler.konum("kasirga"));

    /** Kasırganın kaldıramadığı bloklar (N12; varsayılan obsidyen, deepslate ailesi ve cevherleri/varyantları, ana kaya); veri paketiyle genişler. */
    public static final TagKey<Block> DAYANIKLI = TagKey.create(Registries.BLOCK, DogalAfetler.konum("kasirga_dayanikli"));

    private KasirgaKayitlari() {
    }

    static void kaydet(IEventBus modBus) {
        VARLIKLAR.register(modBus);
        for (ResourceLocation ses : new ResourceLocation[] {UGULTU, RUZGAR}) {
            SESLER.register(ses.getPath(), () -> SoundEvent.createVariableRangeEvent(ses));
        }
        SESLER.register(modBus);
    }

    public static DamageSource hasar(ServerLevel level) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HASAR));
    }

    static void sesleriTanimla(SesSaglayici saglayici) {
        saglayici.tanimla(UGULTU.getPath(), SoundDefinition.definition().subtitle(UGULTU_ALTYAZI)
                .with(SoundDefinition.Sound.sound(UGULTU, SoundDefinition.SoundType.SOUND).stream(true)));
        saglayici.tanimla(RUZGAR.getPath(), SoundDefinition.definition().subtitle(RUZGAR_ALTYAZI)
                .with(SoundDefinition.Sound.sound(RUZGAR, SoundDefinition.SoundType.SOUND).stream(true)));
    }

    static void cevirileriYaz(CeviriYazici y) {
        y.ekle(UGULTU_ALTYAZI, "Tornado roars", "Kasırga uğulduyor");
        y.ekle(RUZGAR_ALTYAZI, "Wind howls", "Rüzgâr kükrüyor");
    }
}
