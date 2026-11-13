package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import com.mojang.serialization.Codec;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.Block;

/**
 * Balıkçılığın kayıtları: {@code buz_deligi} bloğu (eşyası yok), beş çiğ ve beş pişmiş buz balığı, {@code buz_parcasi}
 * (çöp), Kutup Oltası ve yalnız onunla tutulan üç özel kutup balığı (çiğ + pişmiş), ısıya/soğa tarif malzemesi Buzlu İnci ve Tuz
 * Kristali, loot girdi tipi {@code dogalafetler:balikcilik_kategorisi} ve global loot değtiricileri
 * {@code dogalafetler:buz_deligi_balikcilik} (buz deliğinde) ile {@code dogalafetler:deniz_balikciligi} (açık denizde).
 */
public final class BalikcilikKayitlari {
    private static final DeferredRegister<Block> BLOKLAR = DeferredRegister.create(ForgeRegistries.BLOCKS, DogalAfetler.MOD_ID);
    private static final DeferredRegister<Item> ESYALAR = DeferredRegister.create(ForgeRegistries.ITEMS, DogalAfetler.MOD_ID);
    private static final DeferredRegister<LootPoolEntryType> LOOT_GIRDILERI = DeferredRegister.create(
            Registries.LOOT_POOL_ENTRY_TYPE, DogalAfetler.MOD_ID);
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> LOOT_DEGISTIRICILERI = DeferredRegister.create(
            ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, DogalAfetler.MOD_ID);
    private static final DeferredRegister<Enchantment> BUYULER = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS,
            DogalAfetler.MOD_ID);

    /** Madde 14: sıcaklık yiyeceklerinin yığın boyutu. */
    public static final int YIGIN = 16;

    /**
     * Beş buz balığı (madde 15). Doyuruculuk vanilla balıkların yanında: çiğ morina/somon gibi az, pişmiş pişmiş
     * morina–somon aralığında; nadir Donmuş Ringa en doyurucu ve en çok ısıtan.
     */
    public enum Balik {
        BUZ_ALABALIGI(2, 0.1f, 5, 0.6f),
        KUTUP_MORINASI(2, 0.1f, 5, 0.6f),
        DERIN_SOGUK_SOMONU(2, 0.1f, 6, 0.8f),
        BUZ_KEFALI(2, 0.1f, 6, 0.7f),
        DONMUS_RINGA(3, 0.2f, 7, 0.9f);

        private final int cigBesin;
        private final float cigDoygunluk;
        private final int pismisBesin;
        private final float pismisDoygunluk;
        private RegistryObject<Item> cig;
        private RegistryObject<Item> pismis;

        Balik(int cigBesin, float cigDoygunluk, int pismisBesin, float pismisDoygunluk) {
            this.cigBesin = cigBesin;
            this.cigDoygunluk = cigDoygunluk;
            this.pismisBesin = pismisBesin;
            this.pismisDoygunluk = pismisDoygunluk;
        }

        public String ad() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Item cig() {
            return cig.get();
        }

        public Item pismis() {
            return pismis.get();
        }
    }

    /**
     * Üç özel kutup balığı: yalnız {@link KutupOltasi} ile tutulur (buz deliğinde ve açık denizde). Çiğ hali vucudu
     * orta–büyük serinletir (soğutucu yiyecek), pişmiş hali ısıtır; her ikisi {@code YiyecekAPI} desenindedir.
     */
    public enum OzelBalik {
        KRISTAL_LEVREK(BuzBaligiEsyasi.Tur.CIG_ORTA, BuzBaligiEsyasi.Tur.PISMIS, 3, 0.2f, 7, 0.8f, Rarity.UNCOMMON),
        KUTUP_YILANBALIGI(BuzBaligiEsyasi.Tur.CIG_ORTA, BuzBaligiEsyasi.Tur.PISMIS_RINGA, 3, 0.2f, 7, 0.9f, Rarity.UNCOMMON),
        AURORA_BALIGI(BuzBaligiEsyasi.Tur.CIG_BUYUK, BuzBaligiEsyasi.Tur.PISMIS_RINGA, 4, 0.3f, 8, 1.0f, Rarity.RARE);

        private final BuzBaligiEsyasi.Tur cigTur;
        private final BuzBaligiEsyasi.Tur pismisTur;
        private final int cigBesin;
        private final float cigDoygunluk;
        private final int pismisBesin;
        private final float pismisDoygunluk;
        private final Rarity nadirlik;
        private RegistryObject<Item> cig;
        private RegistryObject<Item> pismis;

        OzelBalik(BuzBaligiEsyasi.Tur cigTur, BuzBaligiEsyasi.Tur pismisTur, int cigBesin, float cigDoygunluk,
                int pismisBesin, float pismisDoygunluk, Rarity nadirlik) {
            this.cigTur = cigTur;
            this.pismisTur = pismisTur;
            this.cigBesin = cigBesin;
            this.cigDoygunluk = cigDoygunluk;
            this.pismisBesin = pismisBesin;
            this.pismisDoygunluk = pismisDoygunluk;
            this.nadirlik = nadirlik;
        }

        public String ad() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Item cig() {
            return cig.get();
        }

        public Item pismis() {
            return pismis.get();
        }
    }

    public static final RegistryObject<Block> BUZ_DELIGI = BLOKLAR.register("buz_deligi",
            () -> new BuzDeligiBlogu(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(0.5f).friction(0.98f)
                    .sound(SoundType.GLASS).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK)
                    .isValidSpawn((d, l, p, t) -> false).isRedstoneConductor((d, l, p) -> false)
                    .isSuffocating((d, l, p) -> false).isViewBlocking((d, l, p) -> false)));
    public static final RegistryObject<Item> BUZ_PARCASI = ESYALAR.register("buz_parcasi", () -> new Item(new Item.Properties()));
    /** Kutup Oltası: özel kutup balıklarını tutabilen olta (vanilla oltadan dayanıklı). */
    public static final RegistryObject<Item> KUTUP_OLTASI = ESYALAR.register("kutup_oltasi",
            () -> new KutupOltasi(new Item.Properties().durability(256).rarity(Rarity.UNCOMMON)));
    /** Buzlu İnci: nadir buz altı/deniz hazinesi; Serin Kolye'nin merkezi. */
    public static final RegistryObject<Item> BUZLU_INCI = ESYALAR.register("buzlu_inci",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
    /** Tuz Kristali: Soğuk İksiri'nin malzemesi; buz altı ve deniz eşyası. */
    public static final RegistryObject<Item> TUZ_KRISTALI = ESYALAR.register("tuz_kristali", () -> new Item(new Item.Properties()));
    public static final RegistryObject<LootPoolEntryType> KATEGORI_GIRDISI = LOOT_GIRDILERI.register("balikcilik_kategorisi",
            () -> new LootPoolEntryType(new KategoriGirdisi.Serializer()));
    /** Yineleme (EN Duplicate): olta hazine büyüsü, 3 seviye; tutulan yiğını şansla ikiye katlar ({@link Yineleme}). */
    public static final RegistryObject<Enchantment> YINELEME = BUYULER.register("yineleme", Yineleme::new);

    static {
        LOOT_DEGISTIRICILERI.register("buz_deligi_balikcilik", BuzDeligiLootDegistirici.CODEC);
        LOOT_DEGISTIRICILERI.register("deniz_balikciligi", DenizLootDegistirici.CODEC);
        for (Balik b : Balik.values()) {
            BuzBaligiEsyasi.Tur pismisTur = b == Balik.DONMUS_RINGA ? BuzBaligiEsyasi.Tur.PISMIS_RINGA : BuzBaligiEsyasi.Tur.PISMIS;
            b.cig = ESYALAR.register(b.ad(), () -> new BuzBaligiEsyasi(BuzBaligiEsyasi.Tur.CIG, new Item.Properties()
                    .stacksTo(YIGIN).food(new FoodProperties.Builder().nutrition(b.cigBesin).saturationMod(b.cigDoygunluk)
                            .build())));
            b.pismis = ESYALAR.register("pismis_" + b.ad(), () -> new BuzBaligiEsyasi(pismisTur, new Item.Properties()
                    .stacksTo(YIGIN).food(new FoodProperties.Builder().nutrition(b.pismisBesin)
                            .saturationMod(b.pismisDoygunluk).build())));
        }
        for (OzelBalik b : OzelBalik.values()) {
            b.cig = ESYALAR.register(b.ad(), () -> new BuzBaligiEsyasi(b.cigTur, new Item.Properties().stacksTo(YIGIN)
                    .rarity(b.nadirlik).food(new FoodProperties.Builder().nutrition(b.cigBesin)
                            .saturationMod(b.cigDoygunluk).build())));
            b.pismis = ESYALAR.register("pismis_" + b.ad(), () -> new BuzBaligiEsyasi(b.pismisTur, new Item.Properties()
                    .stacksTo(YIGIN).rarity(b.nadirlik).food(new FoodProperties.Builder().nutrition(b.pismisBesin)
                            .saturationMod(b.pismisDoygunluk).build())));
        }
    }

    private BalikcilikKayitlari() {
    }

    /** Beş çiğ balık, kayıt sırasıyla. */
    public static List<Item> ciglar() {
        List<Item> l = new ArrayList<>();
        for (Balik b : Balik.values()) {
            l.add(b.cig());
        }
        return Collections.unmodifiableList(l);
    }

    /** Beş pişmiş balık, kayıt sırasıyla. */
    public static List<Item> pismisler() {
        List<Item> l = new ArrayList<>();
        for (Balik b : Balik.values()) {
            l.add(b.pismis());
        }
        return Collections.unmodifiableList(l);
    }

    /** Üç özel kutup balığının çiğ halleri, kayıt sırasıyla. */
    public static List<Item> ozelCiglar() {
        List<Item> l = new ArrayList<>();
        for (OzelBalik b : OzelBalik.values()) {
            l.add(b.cig());
        }
        return Collections.unmodifiableList(l);
    }

    static void kaydet(IEventBus modBus) {
        BLOKLAR.register(modBus);
        ESYALAR.register(modBus);
        LOOT_GIRDILERI.register(modBus);
        LOOT_DEGISTIRICILERI.register(modBus);
        BUYULER.register(modBus);
    }
}
