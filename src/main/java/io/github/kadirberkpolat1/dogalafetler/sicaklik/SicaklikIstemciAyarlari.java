package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CLIENT config'in {@code sicaklik} bölümü (madde 7d, S3-07: HUD konumu, görünürlüğü ve parçaları). İstemciye özel API
 * kullanmadığı için sunucuda da güvenle kaydedilir; değerler yalnız istemcide okunur.
 */
public final class SicaklikIstemciAyarlari {
    private static AyarKaydi.Bolum<SicaklikIstemciAyarlari> bolum;

    private final ForgeConfigSpec.BooleanValue hudGorunur;
    private final ForgeConfigSpec.IntValue hudXOfset;
    private final ForgeConfigSpec.IntValue hudYOfset;
    private final ForgeConfigSpec.BooleanValue sayiGoster;
    private final ForgeConfigSpec.BooleanValue hissedilenGoster;
    private final ForgeConfigSpec.BooleanValue kafaEfektleri;
    private final ForgeConfigSpec.BooleanValue animasyonlar;
    private final ForgeConfigSpec.BooleanValue ekranEfektleri;
    private final ForgeConfigSpec.BooleanValue kameraTitremesi;

    private SicaklikIstemciAyarlari(ForgeConfigSpec.Builder k) {
        hudGorunur = k.comment("Kafa ikonlu sıcaklık göstergesi (can ve açlık barlarının ortasında) görünsün mü.")
                .define("hudGorunur", true);
        hudXOfset = k.comment("Göstergenin yatay kaydırması (piksel, GUI ölçeğiyle).")
                .defineInRange("hudXOfset", 0, -1000, 1000);
        hudYOfset = k.comment("Göstergenin dikey kaydırması (piksel; negatif yukarı). Can ve açlık barları hiçbir zaman kaydırılmaz.")
                .defineInRange("hudYOfset", 0, -1000, 1000);
        sayiGoster = k.comment("Kafanın üstünde vücut sıcaklığı °C olarak küçük yazıyla yazılsın mı (varsayılan kapalı: gösterge minimal).")
                .define("sayiGoster", false);
        hissedilenGoster = k.comment("Hissedilen sıcaklık: şeridi kesen ince beyaz iğne (denge noktası).")
                .define("hissedilenGoster", true);
        kafaEfektleri = k.comment("Kafa efektleri: ter damlası, alev ve duman, ağızdan buhar, buz kristalleri.")
                .define("kafaEfektleri", true);
        animasyonlar = k.comment("Gösterge animasyonları: titreme ve donma sarsıntısı, nabız, durum değişim parlaması, efekt hareketi.")
                .define("animasyonlar", true);
        ekranEfektleri = k.comment("Buz kenarı, kırmızı ter kenarı ve sıcak hava dalgası ekran efektleri.")
                .define("ekranEfektleri", true);
        kameraTitremesi = k.comment("Yanmada kameranın yalpalaması, üşüme ve donmada titremesi.").define("kameraTitremesi", true);
    }

    public static void kaydet() {
        bolum = AyarKaydi.istemciBolumu("sicaklik", "Sıcaklık HUD'u ve ekran efektleri (madde 7c, 7d; S3-07)",
                SicaklikIstemciAyarlari::new);
    }

    public static boolean hudGorunur() {
        return bolum.get().hudGorunur.get();
    }

    public static int hudXOfset() {
        return bolum.get().hudXOfset.get();
    }

    public static int hudYOfset() {
        return bolum.get().hudYOfset.get();
    }

    public static boolean sayiGoster() {
        return bolum.get().sayiGoster.get();
    }

    public static boolean hissedilenGoster() {
        return bolum.get().hissedilenGoster.get();
    }

    public static boolean kafaEfektleri() {
        return bolum.get().kafaEfektleri.get();
    }

    public static boolean animasyonlar() {
        return bolum.get().animasyonlar.get();
    }

    public static boolean ekranEfektleri() {
        return bolum.get().ekranEfektleri.get();
    }

    public static boolean kameraTitremesi() {
        return bolum.get().kameraTitremesi.get();
    }
}
