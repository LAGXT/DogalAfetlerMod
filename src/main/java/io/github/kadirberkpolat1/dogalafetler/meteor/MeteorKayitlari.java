package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Meteor varlığı ve parçacık türleri. Varlık kaydedilmez ({@code noSave}): uçuştaki meteor sunucu kapanınca ya da chunk'ı
 * boşalınca atılır (M-05). İzleme menzili 16 chunk: uzaktan görülür.
 */
public final class MeteorKayitlari {
    private static final DeferredRegister<EntityType<?>> VARLIKLAR = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,
            DogalAfetler.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARCACIKLAR = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, DogalAfetler.MOD_ID);

    public static final RegistryObject<EntityType<MeteorVarligi>> METEOR = VARLIKLAR.register("meteor",
            () -> EntityType.Builder.<MeteorVarligi>of(MeteorVarligi::new, MobCategory.MISC).sized(1.0f, 1.0f)
                    .clientTrackingRange(16).updateInterval(2).fireImmune().noSave()
                    .setShouldReceiveVelocityUpdates(true).build(DogalAfetler.konum("meteor").toString()));

    /** Sıcak plazma (güneş: beyaz-sarı → turuncu). */
    public static final RegistryObject<SimpleParticleType> PLAZMA = parcacik("meteor_plazmasi");
    /** Soğuk plazma (ay: mavi-beyaz). */
    public static final RegistryObject<SimpleParticleType> PLAZMA_AY = parcacik("meteor_plazmasi_ay");
    public static final RegistryObject<SimpleParticleType> DUMAN = parcacik("meteor_dumani");
    public static final RegistryObject<SimpleParticleType> KIVILCIM = parcacik("meteor_kivilcimi");
    public static final RegistryObject<SimpleParticleType> KIVILCIM_AY = parcacik("meteor_kivilcimi_ay");
    /** Çarpma şok halkasının tozu. */
    public static final RegistryObject<SimpleParticleType> TOZ = parcacik("meteor_tozu");
    /** Meteorit Parçası'nın altın-turuncu parıltısı. */
    public static final RegistryObject<SimpleParticleType> PARILTI = parcacik("meteorit_parlamasi");
    /** Kozmik Çekirdek'in mor-mavi parıltısı. */
    public static final RegistryObject<SimpleParticleType> KOZMIK = parcacik("kozmik_parilti");

    private MeteorKayitlari() {
    }

    private static RegistryObject<SimpleParticleType> parcacik(String ad) {
        return PARCACIKLAR.register(ad, () -> new SimpleParticleType(true));
    }

    static void kaydet(IEventBus modBus) {
        VARLIKLAR.register(modBus);
        PARCACIKLAR.register(modBus);
        DataGenerators.parcacikEkle(PLAZMA, "meteor_isik", 4);
        DataGenerators.parcacikEkle(PLAZMA_AY, "meteor_isik", 4);
        DataGenerators.parcacikEkle(DUMAN, "meteor_duman", 4);
        DataGenerators.parcacikEkle(KIVILCIM, "meteor_isik", 4);
        DataGenerators.parcacikEkle(KIVILCIM_AY, "meteor_isik", 4);
        DataGenerators.parcacikEkle(TOZ, "meteor_duman", 4);
        DataGenerators.parcacikEkle(PARILTI, "meteor_yildiz", 2);
        DataGenerators.parcacikEkle(KOZMIK, "meteor_yildiz", 2);
    }
}
