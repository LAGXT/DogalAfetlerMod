package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.EtkiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.VanillaSicaklik;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Yon;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekAyarGocu;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTanimi;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * SERVER config'in {@code yiyecek} bölümü (madde 14, N17, S3-06). Etki sayıları {@link EtkiAyarlari}'nın bileşenleridir ve
 * varsayılanları {@link EtkiAyarlari#VARSAYILAN}'dan gelir; kurulmuş kayıt ve çözülmüş vanilla listeleri config yüklenince
 * ya da değişince yeniden kurulur. SERVER config istemciye senkronlanır: istemci tooltip'i de buradan okur. Eski varsayılan
 * vanilla ısınma listesi {@link YiyecekAyarGocu} ile kademeli listeye taşınır.
 */
public final class YiyecekAyarlari {
    private static AyarKaydi.Bolum<YiyecekAyarlari> bolum;
    private static volatile EtkiAyarlari etki;
    private static volatile Map<Item, YiyecekTanimi> vanillaIsinmaTanimlari;
    private static volatile Map<Item, YiyecekTanimi> vanillaSerinlikTanimlari;
    private static volatile Boolean vanillaIsinmaTesti;
    private static volatile Boolean vanillaSerinlikTesti;

    private final ForgeConfigSpec.DoubleValue cokKucuk;
    private final ForgeConfigSpec.DoubleValue kucuk;
    private final ForgeConfigSpec.DoubleValue orta;
    private final ForgeConfigSpec.DoubleValue buyuk;
    private final ForgeConfigSpec.DoubleValue cokBuyuk;
    private final ForgeConfigSpec.DoubleValue anlikOran;
    private final ForgeConfigSpec.IntValue azalanVerimPenceresiSaniye;
    private final ForgeConfigSpec.DoubleValue azalanVerimOrani;
    private final ForgeConfigSpec.DoubleValue yanEtkiCarpani;
    private final ForgeConfigSpec.DoubleValue yanEtkiEsigi;
    private final ForgeConfigSpec.BooleanValue vanillaIsinma;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> vanillaYiyecekler;
    private final ForgeConfigSpec.BooleanValue vanillaSerinlik;
    private final ForgeConfigSpec.ConfigValue<List<? extends String>> vanillaSerinYiyecekler;
    private final ForgeConfigSpec.IntValue balikPismisSaniye;
    private final ForgeConfigSpec.IntValue balikCigSaniye;

    private YiyecekAyarlari(ForgeConfigSpec.Builder k) {
        EtkiAyarlari v = EtkiAyarlari.VARSAYILAN;
        k.comment("Config göçünün sürümü (K-E61): dosya bundan eskiyse açılışta eski varsayılanında kalan yiyecek değerleri "
                + "yeni varsayılana çekilir, elle değiştirilenlere dokunulmaz. Elle değiştirmeyin.")
                .defineInRange("ayarSurumu", YiyecekAyarGocu.GUNCEL, 0, Integer.MAX_VALUE);
        k.comment("Etki kademeleri: yiyeceğin toplam sıcaklık etkisi, vücut birimi (ölçeğin bir yanı 100: \"orta %15-25\" = 15-25).",
                "Sıralı olmalı (cokKucuk <= kucuk <= orta <= buyuk <= cokBuyuk); bozuksa varsayılanlar kullanılır.")
                .push("kademe");
        cokKucuk = k.comment("Çok küçük: vanilla pişmiş etler, fırın ürünleri, sulu meyveler ve çiğ balıklar.")
                .defineInRange("cokKucuk", v.cokKucuk(), 0.0, 100.0);
        kucuk = k.comment("Küçük (madde 14: %5-10): Isıtıcı Çay, Buzlu Karpuz, çiğ buz balığı, vanilla çorbalar, süt ve su.")
                .defineInRange("kucuk", v.kucuk(), 0.0, 100.0);
        orta = k.comment("Orta (%15-25): Sıcak Salep, Sıcak Çikolata, Ayran, pişmiş buz balığı.")
                .defineInRange("orta", v.orta(), 0.0, 100.0);
        buyuk = k.comment("Büyük (%30-40): Sıcak Tavuk Çorbası, Kutup Balığı Çorbası, Meyve Şerbeti, Maraş Dondurması.")
                .defineInRange("buyuk", v.buyuk(), 0.0, 100.0);
        cokBuyuk = k.comment("Çok büyük (büyüğün üstü): Ateşli Güveç (en güçlü soğuk savar), Parıltılı Sorbe (en güçlü serinletici).")
                .defineInRange("cokBuyuk", v.cokBuyuk(), 0.0, 100.0);
        k.pop();
        anlikOran = k.comment("Etkinin hemen eklenen payı (0-1); kalanı süre boyunca giderek azalan hızla eklenir.")
                .defineInRange("anlikOran", v.anlikOran(), 0.0, 1.0);
        azalanVerimPenceresiSaniye = k.comment("Aynı yiyecek son yemeden bu süre içinde tekrar yenirse etkisi azalır (sn; 0 = kapalı).")
                .defineInRange("azalanVerimPenceresiSaniye", v.azalanVerimPenceresiSaniye(), 0, YiyecekTanimi.EN_UZUN_SANIYE);
        azalanVerimOrani = k.comment("Pencere içindeki tekrar yemede etkinin çarpanı (0,5 = %50 azalır).")
                .defineInRange("azalanVerimOrani", v.azalanVerimOrani(), 0.0, 1.0);
        yanEtkiCarpani = k.comment("Ateşli Güveç yan etkisi: vücut ya da ortam sıcakken etkinin çarpanı (aşırı ısınmaya iter).")
                .defineInRange("yanEtkiCarpani", v.yanEtkiCarpani(), 1.0, 5.0);
        yanEtkiEsigi = k.comment("Yan etki eşiği: vücut ya da ortam (vücut birimi; 20 = 28 °C ortam) bu değer ve üstündeyse sıcakta sayılır.")
                .defineInRange("yanEtkiEsigi", v.yanEtkiEsigi(), -100.0, 100.0);
        vanillaIsinma = k.comment("Vanilla sıcak yiyecekler (vanillaYiyecekler listesi) içten ısınma versin mi (madde 14).")
                .define("vanillaIsinma", true);
        vanillaYiyecekler = k.comment("Isınma veren yiyecekler: eşya=kademe:saniye (kademe cok_kucuk, kucuk, orta, buyuk, cok_buyuk;",
                "miktar kademe.* değeri). Eski eşya=saniye satırı cok_kucuk sayılır. Başka modların yiyecekleri de eklenebilir.")
                .defineListAllowEmpty(List.of(YiyecekAyarGocu.ISINMA_LISTESI), () -> VanillaSicaklik.VARSAYILAN_ISINMA,
                        VanillaSicaklik::gecerliMi);
        vanillaSerinlik = k.comment("Vanilla soğuk yiyecek ve içecekler (vanillaSerinYiyecekler listesi) serinlik versin mi.")
                .define("vanillaSerinlik", true);
        vanillaSerinYiyecekler = k.comment("Serinlik veren yiyecek ve içecekler: eşya=kademe:saniye (ısınma listesiyle aynı biçim).",
                "minecraft:potion yalnız su şişesinde serinletir (diğer iksirler aynı kimliği taşır). Bir eşya iki listedeyse ısınma geçerlidir.")
                .defineListAllowEmpty(List.of("vanillaSerinYiyecekler"), () -> VanillaSicaklik.VARSAYILAN_SERINLIK,
                        VanillaSicaklik::gecerliMi);
        k.comment("Buz balıkları (madde 15): balıkçılık YiyecekAPI.pismisBalik/cigBalik ile kullanır.").push("balik");
        balikPismisSaniye = k.comment("Pişmiş buz balığının orta ısınma süresi (sn).")
                .defineInRange("pismisSaniye", 90, 1, YiyecekTanimi.EN_UZUN_SANIYE);
        balikCigSaniye = k.comment("Çiğ buz balığının hafif serinlik süresi (sn).")
                .defineInRange("cigSaniye", 60, 1, YiyecekTanimi.EN_UZUN_SANIYE);
        k.pop();
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu(YiyecekAyarGocu.BOLUM, "Sıcaklık yiyecekleri (madde 14)", YiyecekAyarlari::new);
        AyarKaydi.sunucuGocu(YiyecekAyarGocu.PLAN);
    }

    /** Mod veri yolunda: SERVER config yüklenince ya da değişince kurulmuş değerler yeniden kurulur. */
    static void configDegisti(ModConfigEvent olay) {
        if (AyarKaydi.SUNUCU_DOSYASI.equals(olay.getConfig().getFileName())) {
            etki = null;
            vanillaIsinmaTanimlari = null;
            vanillaSerinlikTanimlari = null;
        }
    }

    /** Config yüklendi mi (istemcide dünyaya girmeden önce değildir). */
    public static boolean yuklendiMi() {
        try {
            bolum.get().vanillaIsinma.get();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public static EtkiAyarlari etki() {
        EtkiAyarlari e = etki;
        if (e == null) {
            YiyecekAyarlari a = bolum.get();
            try {
                e = new EtkiAyarlari(a.cokKucuk.get(), a.kucuk.get(), a.orta.get(), a.buyuk.get(), a.cokBuyuk.get(),
                        a.anlikOran.get(), a.azalanVerimPenceresiSaniye.get(), a.azalanVerimOrani.get(),
                        a.yanEtkiCarpani.get(), a.yanEtkiEsigi.get());
            } catch (IllegalArgumentException hata) {
                DogalAfetler.LOGGER.error("Yiyecek config'i geçersiz, varsayılanlar kullanılıyor: {}", hata.getMessage());
                e = EtkiAyarlari.VARSAYILAN;
            }
            etki = e;
        }
        return e;
    }

    public static boolean vanillaIsinma() {
        Boolean test = vanillaIsinmaTesti;
        return test != null ? test : bolum.get().vanillaIsinma.get();
    }

    public static boolean vanillaSerinlik() {
        Boolean test = vanillaSerinlikTesti;
        return test != null ? test : bolum.get().vanillaSerinlik.get();
    }

    /**
     * Yalnız GameTest: {@code vanillaIsinma}'yı config dosyasına yazmadan değiştirir, {@code null} config'e döner.
     * {@code ConfigValue.set} dosyayı yeniden yazar; Forge'un izleyici thread'i dosyayı yeniden yüklerken gelen ikinci
     * {@code set} ayrıştırmayı bozar ({@code [yiyecek] declared twice}, K-E28).
     */
    static void vanillaIsinmaTesti(Boolean deger) {
        vanillaIsinmaTesti = deger;
    }

    /** Yalnız GameTest: {@code vanillaSerinlik} için {@link #vanillaIsinmaTesti} kalıbı. */
    static void vanillaSerinlikTesti(Boolean deger) {
        vanillaSerinlikTesti = deger;
    }

    /** {@code vanillaYiyecekler} listesinin kayıtlı eşyalara çözülmüş hali; bilinmeyen kimlik WARN ile atlanır. */
    public static Map<Item, YiyecekTanimi> vanillaIsinmaTanimlari() {
        Map<Item, YiyecekTanimi> m = vanillaIsinmaTanimlari;
        if (m == null) {
            m = coz(bolum.get().vanillaYiyecekler.get(), Yon.ISINMA, YiyecekAyarGocu.ISINMA_LISTESI);
            vanillaIsinmaTanimlari = m;
        }
        return m;
    }

    /** {@code vanillaSerinYiyecekler} listesinin kayıtlı eşyalara çözülmüş hali; bilinmeyen kimlik WARN ile atlanır. */
    public static Map<Item, YiyecekTanimi> vanillaSerinlikTanimlari() {
        Map<Item, YiyecekTanimi> m = vanillaSerinlikTanimlari;
        if (m == null) {
            m = coz(bolum.get().vanillaSerinYiyecekler.get(), Yon.SERINLIK, "vanillaSerinYiyecekler");
            vanillaSerinlikTanimlari = m;
        }
        return m;
    }

    private static Map<Item, YiyecekTanimi> coz(List<? extends String> satirlar, Yon yon, String anahtar) {
        Map<Item, YiyecekTanimi> yeni = new LinkedHashMap<>();
        for (String satir : satirlar) {
            VanillaSicaklik.Girdi girdi = VanillaSicaklik.coz(satir);
            ResourceLocation kimlik = ResourceLocation.tryParse(girdi.esya());
            if (kimlik == null || !ForgeRegistries.ITEMS.containsKey(kimlik)) {
                DogalAfetler.LOGGER.warn("Yiyecek: {}'de kayıtlı olmayan eşya atlandı: {}", anahtar, girdi.esya());
                continue;
            }
            yeni.put(ForgeRegistries.ITEMS.getValue(kimlik), VanillaSicaklik.tanim(girdi, yon));
        }
        return Map.copyOf(yeni);
    }

    public static int balikPismisSaniye() {
        return bolum.get().balikPismisSaniye.get();
    }

    public static int balikCigSaniye() {
        return bolum.get().balikCigSaniye.get();
    }
}
