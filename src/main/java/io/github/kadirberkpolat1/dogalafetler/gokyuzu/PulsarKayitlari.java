package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok.KomurlesmisKutukBlogu;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok.KulKatmaniBlogu;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok.PulsarBlokVerileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok.YakitsizBlokEsyasi;
import io.github.kadirberkpolat1.dogalafetler.veri.CeviriYazici;
import io.github.kadirberkpolat1.dogalafetler.veri.DataGenerators;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Pulsar cehenneminin blokları ve parçacıkları (PL-3): {@code kul_katmani} (kar katmanı gibi 1–8 kat, erimez; 8 kat Kül
 * Bloğu verir), {@code kul_blogu} (kum sesi, kürek), {@code komurlesmis_kutuk} (eksenli, ışık 3, balta, yanmaz: yanıcılık
 * kaydı yok; suya değince birkaç dakikada kömür bloğu, {@link KomurlesmisKutukBlogu}), {@code komurlesmis_tahta} (çalışma
 * masasında 1 kütük → 4 tahta; {@code minecraft:planks}, tekne tarifleri belirli tahta istediğinden tekne olmaz). Kütük
 * ve tahta Nether odunları gibi yakıt değildir ({@code minecraft:non_flammable_wood}, {@link YakitsizBlokEsyasi}); kütük
 * {@code minecraft:logs}'ta değildir (afet sınıflamaları ve odun kömürü tarifi değişmesin). Parçacıklar {@code kul_yagisi},
 * {@code kor}, {@code kul_tozu} (istemci sağlayıcıları {@code client.gokyuzu.PulsarParcacikKaydi}). Dokular
 * {@code tools/assets/pulsar_dokular.py}; blok durumu, model, loot, tarif datagen'de ({@link PulsarBlokVerileri}), vanilla
 * etiketler ortak sağlayıcıdan (K-E12).
 */
public final class PulsarKayitlari {
    private static final DeferredRegister<Block> BLOKLAR = DeferredRegister.create(ForgeRegistries.BLOCKS,
            DogalAfetler.MOD_ID);
    private static final DeferredRegister<Item> ESYALAR = DeferredRegister.create(ForgeRegistries.ITEMS,
            DogalAfetler.MOD_ID);
    private static final DeferredRegister<ParticleType<?>> PARCACIKLAR = DeferredRegister.create(
            ForgeRegistries.PARTICLE_TYPES, DogalAfetler.MOD_ID);

    public static final RegistryObject<Block> KUL_KATMANI = BLOKLAR.register("kul_katmani",
            () -> new KulKatmaniBlogu(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).replaceable()
                    .forceSolidOff().strength(0.1f).sound(SoundType.SAND).pushReaction(PushReaction.DESTROY)
                    .isViewBlocking((durum, level, konum) -> durum.getValue(SnowLayerBlock.LAYERS) >= 8)));
    public static final RegistryObject<Block> KUL_BLOGU = BLOKLAR.register("kul_blogu",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.5f)
                    .sound(SoundType.SAND)));
    public static final RegistryObject<Block> KOMURLESMIS_KUTUK = BLOKLAR.register("komurlesmis_kutuk",
            () -> new KomurlesmisKutukBlogu(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASS).strength(2.0f).sound(SoundType.WOOD).lightLevel(durum -> 3)));
    public static final RegistryObject<Block> KOMURLESMIS_TAHTA = BLOKLAR.register("komurlesmis_tahta",
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK)
                    .instrument(NoteBlockInstrument.BASS).strength(2.0f, 3.0f).sound(SoundType.WOOD)));

    /** Gökten süzülen gri-siyah kül pulu (istemci kül yağışı). */
    public static final RegistryObject<SimpleParticleType> KUL_YAGISI = PARCACIKLAR.register("kul_yagisi",
            () -> new SimpleParticleType(false));
    /** Turuncu-kızıl, kısa ömürlü, yükselen kıvılcım. */
    public static final RegistryObject<SimpleParticleType> KOR = PARCACIKLAR.register("kor",
            () -> new SimpleParticleType(false));
    /** Kül katmanında yürüyünce yerde kısa savrulan toz. */
    public static final RegistryObject<SimpleParticleType> KUL_TOZU = PARCACIKLAR.register("kul_tozu",
            () -> new SimpleParticleType(false));

    /**
     * İstemci kanca (N15): kül katmanı üstünde kor görünsün mü (evre PULSAR ve blok hasarı açık). Ortak kod istemci
     * sınıfına dokunmaz; istemci kurulumu doldurur, boşsa kor yok.
     */
    public static volatile Supplier<Boolean> korGorunur;

    static {
        ESYALAR.register("kul_katmani", () -> new BlockItem(KUL_KATMANI.get(), new Item.Properties()));
        ESYALAR.register("kul_blogu", () -> new BlockItem(KUL_BLOGU.get(), new Item.Properties()));
    }

    public static final RegistryObject<Item> KOMURLESMIS_KUTUK_ESYASI = ESYALAR.register("komurlesmis_kutuk",
            () -> new YakitsizBlokEsyasi(KOMURLESMIS_KUTUK.get(), new Item.Properties()));
    public static final RegistryObject<Item> KOMURLESMIS_TAHTA_ESYASI = ESYALAR.register("komurlesmis_tahta",
            () -> new YakitsizBlokEsyasi(KOMURLESMIS_TAHTA.get(), new Item.Properties()));

    private PulsarKayitlari() {
    }

    static void kaydet(IEventBus modBus) {
        BLOKLAR.register(modBus);
        ESYALAR.register(modBus);
        PARCACIKLAR.register(modBus);
        modBus.addListener(PulsarKayitlari::sekmeyeEkle);
        MinecraftForge.EVENT_BUS.addListener(PulsarEtkileri::sunucuDurdu);
        MinecraftForge.EVENT_BUS.addListener(KavurmaKuyrugu::sunucuDurdu);
        DataGenerators.parcacikEkle(KUL_YAGISI, "kul_yagisi", 4);
        DataGenerators.parcacikEkle(KOR, "kor", 4);
        DataGenerators.parcacikEkle(KUL_TOZU, "kul_tozu", 4);
        DataGenerators.ceviriEkle(PulsarKayitlari::cevirileriYaz);
        DataGenerators.vanillaBlokEtiketiEkle(BlockTags.MINEABLE_WITH_SHOVEL, KUL_KATMANI, KUL_BLOGU);
        DataGenerators.vanillaBlokEtiketiEkle(BlockTags.MINEABLE_WITH_AXE, KOMURLESMIS_KUTUK, KOMURLESMIS_TAHTA);
        DataGenerators.vanillaBlokEtiketiEkle(BlockTags.PLANKS, KOMURLESMIS_TAHTA);
        DataGenerators.vanillaEsyaEtiketiEkle(ItemTags.PLANKS, KOMURLESMIS_TAHTA_ESYASI);
        DataGenerators.vanillaEsyaEtiketiEkle(ItemTags.NON_FLAMMABLE_WOOD, KOMURLESMIS_KUTUK_ESYASI,
                KOMURLESMIS_TAHTA_ESYASI);
        // Lambda: sağlayıcı sınıfı (istemci model üreticilerine bağlı) yalnız datagen'de yüklenir.
        DataGenerators.saglayiciEkle(olay -> PulsarBlokVerileri.ekle(olay));
    }

    private static void sekmeyeEkle(BuildCreativeModeTabContentsEvent olay) {
        if (olay.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            olay.accept(KUL_KATMANI);
            olay.accept(KUL_BLOGU);
            olay.accept(KOMURLESMIS_KUTUK);
        } else if (olay.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            olay.accept(KOMURLESMIS_KUTUK);
            olay.accept(KOMURLESMIS_TAHTA);
        }
    }

    static void cevirileriYaz(CeviriYazici y) {
        y.ekle("block.dogalafetler.kul_katmani", "Ash Layer", "Kül Katmanı");
        y.ekle("block.dogalafetler.kul_blogu", "Block of Ash", "Kül Bloğu");
        y.ekle("block.dogalafetler.komurlesmis_kutuk", "Charred Log", "Kömürleşmiş Kütük");
        y.ekle("block.dogalafetler.komurlesmis_tahta", "Charred Planks", "Kömürleşmiş Tahta");
    }
}
