package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Yon;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTablosu;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Yiyecek kayıtları: madde 14'ün dokuz eşyası (kimlikler {@link YiyecekTablosu}'nda sabit), "İçten Isınma" ve "Serinlik"
 * durum efektleri, kalanı seçilebilir şekilsiz tarif tipi ve Kutup Balığı Çorbası'nın malzeme etiketi (etiketi balıkçılık
 * doldurur). Eşyalar vanilla "Yiyecek ve İçecekler" sekmesinde görünür.
 */
public final class YiyecekKayitlari {
    private static final DeferredRegister<Item> ESYALAR = DeferredRegister.create(ForgeRegistries.ITEMS,
            DogalAfetler.MOD_ID);
    private static final DeferredRegister<MobEffect> EFEKTLER = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS,
            DogalAfetler.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERILESTIRICILER =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DogalAfetler.MOD_ID);

    public static final RegistryObject<MobEffect> ICTEN_ISINMA = EFEKTLER.register("icten_isinma",
            () -> new SicaklikEtkisi(Yon.ISINMA));
    public static final RegistryObject<MobEffect> SERINLIK = EFEKTLER.register("serinlik",
            () -> new SicaklikEtkisi(Yon.SERINLIK));

    public static final RegistryObject<RecipeSerializer<KalanSecimliTarif>> KALAN_SECIMLI_TARIF =
            SERILESTIRICILER.register("kalan_secimli_sekilsiz", KalanSecimliTarif.Serilestirici::new);

    /** Kutup Balığı Çorbası'nın pişmiş balık malzemesi (madde 15'in balıkları; etiketi balıkçılık işçisi doldurur). */
    public static final TagKey<Item> PISMIS_KUTUP_BALIGI = TagKey.create(Registries.ITEM,
            DogalAfetler.konum("pismis_kutup_baligi"));

    /** Ad → eşya, tablo sırasıyla. */
    public static final Map<String, RegistryObject<Item>> ESYA;

    static {
        Map<String, RegistryObject<Item>> esya = new LinkedHashMap<>();
        for (YiyecekTablosu.Kayit kayit : YiyecekTablosu.KAYITLAR) {
            esya.put(kayit.ad(), ESYALAR.register(kayit.ad(), () -> new SicaklikYiyecegi(kayit)));
        }
        ESYA = Collections.unmodifiableMap(esya);
    }

    private YiyecekKayitlari() {
    }

    static void kaydet(IEventBus modBus) {
        ESYALAR.register(modBus);
        EFEKTLER.register(modBus);
        SERILESTIRICILER.register(modBus);
        modBus.addListener(YiyecekKayitlari::sekmeyeEkle);
    }

    public static Item esya(String ad) {
        RegistryObject<Item> kayit = ESYA.get(ad);
        if (kayit == null) {
            throw new IllegalArgumentException("yiyecek yok: " + ad);
        }
        return kayit.get();
    }

    public static MobEffect efekt(Yon yon) {
        return yon == Yon.ISINMA ? ICTEN_ISINMA.get() : SERINLIK.get();
    }

    private static void sekmeyeEkle(BuildCreativeModeTabContentsEvent olay) {
        if (olay.getTabKey() == CreativeModeTabs.FOOD_AND_DRINKS) {
            for (RegistryObject<Item> esya : ESYA.values()) {
                olay.accept(esya);
            }
        }
    }
}
