package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** {@code gunes_patlamasi} SERVER config bölümü (bölüm D, N17). Meteor sayısı oynanıştır, efekt kalitesinden bağımsızdır. */
public final class GunesPatlamasiAyarlari {
    private static AyarKaydi.Bolum<GunesPatlamasiAyarlari> bolum;

    private final ForgeConfigSpec.DoubleValue aktifSureGun;
    private final ForgeConfigSpec.DoubleValue meteorBaslangic;
    private final ForgeConfigSpec.DoubleValue meteorSon;
    private final ForgeConfigSpec.DoubleValue buyukMeteorOrani;
    private final ForgeConfigSpec.DoubleValue kavrulmaBaslangicGun;
    private final ForgeConfigSpec.DoubleValue kavrulmaOlasiligi;
    private final ForgeConfigSpec.IntValue kavrulmaYaricap;

    private GunesPatlamasiAyarlari(ForgeConfigSpec.Builder k) {
        aktifSureGun = k.comment("Meteor ve kavrulma sürecinin uzunluğu, Minecraft günü (bölüm D: 5 gün); sonunda süpernova.")
                .defineInRange("aktifSureGun", 5.0, 1.0, 10.0);
        meteorBaslangic = k.comment("İlk gün oyuncu başına dakikadaki meteor (1,5 = 40 sn'de bir).")
                .defineInRange("meteorBaslangicDakikaBasina", 1.5, 0.0, 30.0);
        meteorSon = k.comment("Sürecin sonunda oyuncu başına dakikadaki meteor (5 = 12 sn'de bir); arası doğrusal.")
                .defineInRange("meteorSonDakikaBasina", 5.0, 0.0, 30.0);
        buyukMeteorOrani = k.comment("Büyük meteor olasılığı (Kozmik Çekirdek yalnız büyük meteorda).")
                .defineInRange("buyukMeteorOrani", 0.05, 0.0, 1.0);
        kavrulmaBaslangicGun = k.comment("Çimenin kaba toprağa dönmeye başladığı gün (afetBlokHasari kapalıyken hiç).")
                .defineInRange("kavrulmaBaslangicGun", 3.0, 0.0, 10.0);
        kavrulmaOlasiligi = k.comment("Sürecin sonunda oyuncu başına her tick'teki kavrulma denemesinin olasılığı (doğrusal artış).")
                .defineInRange("kavrulmaOlasiligi", 0.5, 0.0, 1.0);
        kavrulmaYaricap = k.comment("Kavrulmanın oyuncu çevresindeki yarıçapı, blok.")
                .defineInRange("kavrulmaYaricap", 64, 8, 160);
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("gunes_patlamasi", "Güneş patlaması (bölüm D)", GunesPatlamasiAyarlari::new);
    }

    private static GunesPatlamasiAyarlari al() {
        return bolum.get();
    }

    public static double aktifSureGun() {
        return al().aktifSureGun.get();
    }

    public static double meteorBaslangic() {
        return al().meteorBaslangic.get();
    }

    public static double meteorSon() {
        return al().meteorSon.get();
    }

    public static double buyukMeteorOrani() {
        return al().buyukMeteorOrani.get();
    }

    public static double kavrulmaBaslangicGun() {
        return al().kavrulmaBaslangicGun.get();
    }

    public static double kavrulmaOlasiligi() {
        return al().kavrulmaOlasiligi.get();
    }

    public static int kavrulmaYaricap() {
        return al().kavrulmaYaricap.get();
    }
}
