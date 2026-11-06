package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CLIENT config'in {@code sicaklik} bölümü (madde 7d: HUD pozisyonu ve görünürlüğü). İstemciye özel API kullanmadığı için
 * sunucuda da güvenle kaydedilir; değerler yalnız istemcide okunur.
 */
public final class SicaklikIstemciAyarlari {
    private static AyarKaydi.Bolum<SicaklikIstemciAyarlari> bolum;

    private final ForgeConfigSpec.BooleanValue hudGorunur;
    private final ForgeConfigSpec.IntValue hudXOfset;
    private final ForgeConfigSpec.IntValue hudYOfset;
    private final ForgeConfigSpec.BooleanValue ortamOkuGoster;
    private final ForgeConfigSpec.BooleanValue ekranEfektleri;
    private final ForgeConfigSpec.BooleanValue kameraTitremesi;

    private SicaklikIstemciAyarlari(ForgeConfigSpec.Builder k) {
        hudGorunur = k.comment("Sıcaklık barı (XP barının üstünde) görünsün mü.").define("hudGorunur", true);
        hudXOfset = k.comment("Barın yatay kaydırması (piksel, GUI ölçeğiyle).").defineInRange("hudXOfset", 0, -1000, 1000);
        hudYOfset = k.comment("Barın dikey kaydırması (piksel; negatif yukarı). Kalpler ve açlık barı barın üstünde kalır.")
                .defineInRange("hudYOfset", 0, -1000, 1000);
        ortamOkuGoster = k.comment("Ortam sıcaklığını gösteren ikinci küçük ok.").define("ortamOkuGoster", true);
        ekranEfektleri = k.comment("Buz kenarı ve sıcak hava dalgası ekran efektleri (madde 7c).").define("ekranEfektleri", true);
        kameraTitremesi = k.comment("Aşırı ısınmada kameranın hafif yalpalaması.").define("kameraTitremesi", true);
    }

    public static void kaydet() {
        bolum = AyarKaydi.istemciBolumu("sicaklik", "Sıcaklık HUD'u ve ekran efektleri (madde 7c, 7d)",
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

    public static boolean ortamOkuGoster() {
        return bolum.get().ortamOkuGoster.get();
    }

    public static boolean ekranEfektleri() {
        return bolum.get().ekranEfektleri.get();
    }

    public static boolean kameraTitremesi() {
        return bolum.get().kameraTitremesi.get();
    }
}
