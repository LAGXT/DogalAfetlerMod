package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Kasırga paketinin varlık kayıtları ve spike komutu. */
public final class KasirgaVarliklari {
    private static final DeferredRegister<EntityType<?>> TURLER = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
            DogalAfetler.MOD_ID);

    /** İzleme menzili 20 chunk (sunucunun görüş mesafesiyle sınırlı); konum 4 Hz. */
    public static final RegistryObject<EntityType<KasirgaVarligi>> KASIRGA = TURLER.register("kasirga",
            () -> EntityType.Builder.<KasirgaVarligi>of(KasirgaVarligi::new, MobCategory.MISC)
                    .sized(1f, 1f)
                    .noSave()
                    .fireImmune()
                    .clientTrackingRange(20)
                    .updateInterval(KasirgaVarligi.SENKRON_ARALIGI)
                    .build(DogalAfetler.konum("kasirga").toString()));

    /** Hızlı hareket ettiği için 10 Hz; uzaktan görünmesi yeter (10 chunk). */
    public static final RegistryObject<EntityType<UcanBlokVarligi>> UCAN_BLOK = TURLER.register("ucan_blok",
            () -> EntityType.Builder.<UcanBlokVarligi>of(UcanBlokVarligi::new, MobCategory.MISC)
                    .sized(0.98f, 0.98f)
                    .noSave()
                    .clientTrackingRange(10)
                    .updateInterval(UcanBlokVarligi.SENKRON_ARALIGI)
                    .build(DogalAfetler.konum("ucan_blok").toString()));

    private KasirgaVarliklari() {
    }

    public static void kaydet(IEventBus modBus) {
        TURLER.register(modBus);
        KasirgaSpikeKomutu.kaydet();
    }
}
