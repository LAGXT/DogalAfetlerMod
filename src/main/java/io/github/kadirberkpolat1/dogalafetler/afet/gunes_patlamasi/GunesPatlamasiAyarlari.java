package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi;

import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik.GunesPatlamasiAyarGocu;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** {@code gunes_patlamasi} SERVER config bölümü (bölüm D, N17). Meteor sayısı oynanıştır, efekt kalitesinden bağımsızdır. */
public final class GunesPatlamasiAyarlari {
    /**
     * Öncü meteor yağmurunun ön-uyarıdaki payı (sn; K-E67, eski 90): 25 sn'lik sirenle AKTIF'ten önceki son 30 sn. Ön-uyarı
     * artık 5–8 sn olduğundan eski 90 sn penceresi ön-uyarının tamamını kaplıyordu.
     */
    public static final int VARSAYILAN_ONCU_SANIYE = 5;

    private static AyarKaydi.Bolum<GunesPatlamasiAyarlari> bolum;

    private final ForgeConfigSpec.DoubleValue aktifSureGun;
    private final ForgeConfigSpec.DoubleValue meteorBaslangic;
    private final ForgeConfigSpec.DoubleValue meteorSon;
    private final ForgeConfigSpec.DoubleValue buyukMeteorOrani;
    private final ForgeConfigSpec.IntValue meteorYagmuruOncuSaniye;
    private final ForgeConfigSpec.DoubleValue meteorYagmuruOncuDakikaBasina;
    private final ForgeConfigSpec.DoubleValue meteorYagmuruOncuSonDakikaBasina;
    private final ForgeConfigSpec.IntValue meteorYagmuruSaniye;
    private final ForgeConfigSpec.IntValue meteorYagmuruGunlukSaniye;
    private final ForgeConfigSpec.DoubleValue meteorYagmuruDakikaBasina;
    private final ForgeConfigSpec.DoubleValue kavrulmaBaslangicGun;
    private final ForgeConfigSpec.DoubleValue kavrulmaOlasiligi;
    private final ForgeConfigSpec.IntValue kavrulmaYaricap;

    private GunesPatlamasiAyarlari(ForgeConfigSpec.Builder k) {
        k.comment("Config göçünün sürümü (K-E61): dosya bundan eskiyse açılışta eski varsayılanında kalan güneş patlaması "
                + "değerleri yeni varsayılana çekilir, elle değiştirilenlere dokunulmaz. Elle değiştirmeyin.")
                .defineInRange("ayarSurumu", GunesPatlamasiAyarGocu.GUNCEL, 0, Integer.MAX_VALUE);
        aktifSureGun = k.comment("Meteor ve kavrulma sürecinin uzunluğu, Minecraft günü (bölüm D: 5 gün); sonunda süpernova.")
                .defineInRange("aktifSureGun", 5.0, 1.0, 10.0);
        meteorBaslangic = k.comment("İlk gün oyuncu başına dakikadaki meteor (1,5 = 40 sn'de bir).")
                .defineInRange("meteorBaslangicDakikaBasina", 1.5, 0.0, 30.0);
        meteorSon = k.comment("Sürecin sonunda oyuncu başına dakikadaki meteor (5 = 12 sn'de bir); arası doğrusal.")
                .defineInRange("meteorSonDakikaBasina", 5.0, 0.0, 30.0);
        buyukMeteorOrani = k.comment("Büyük bant (1,6–2,5×) olasılığı (Kozmik Çekirdek yalnız büyükte); orta bant meteor.ortaMeteorOrani.")
                .defineInRange("buyukMeteorOrani", 0.10, 0.0, 1.0);
        meteorYagmuruOncuSaniye = k.comment("Öncü meteor yağmuru ön-uyarının son bu kadar saniyesinde başlar ve siren (güneş "
                + "büyürken, 25 sn) boyunca artar (varsayılan: AKTIF'ten önceki son 30 sn; 0: yalnız sirende).")
                .defineInRange("meteorYagmuruOncuSaniye", VARSAYILAN_ONCU_SANIYE, 0, 600);
        meteorYagmuruOncuDakikaBasina = k.comment("Öncü yağmurun başında oyuncu başına dakikadaki meteor.")
                .defineInRange("meteorYagmuruOncuDakikaBasina", 6.0, 0.0, 300.0);
        meteorYagmuruOncuSonDakikaBasina = k.comment("Öncü yağmurun siren sonundaki yoğunluğu (arası doğrusal); AKTIF'te "
                + "meteorYagmuruDakikaBasina'ya sıçrar.")
                .defineInRange("meteorYagmuruOncuSonDakikaBasina", 24.0, 0.0, 300.0);
        meteorYagmuruSaniye = k.comment("AKTIF'in başındaki yoğun meteor yağmurunun süresi (ölçekli sn; 0: yok; afet önce biterse "
                + "yağmur afetle biter).")
                .defineInRange("meteorYagmuruSaniye", 600, 0, 1200);
        meteorYagmuruGunlukSaniye = k.comment("Sonraki her günün başındaki meteor yağmurunun süresi (ölçekli sn; 0: yok).")
                .defineInRange("meteorYagmuruGunlukSaniye", 180, 0, 1200);
        meteorYagmuruDakikaBasina = k.comment("Meteor yağmurunda oyuncu başına dakikadaki meteor (180 = 3/sn); uçan meteor "
                + "tavanı (meteor.varlikTavani) ve krater bütçesi geçerli kalır, tavan doluyken atılamayan meteor düşmez.")
                .defineInRange("meteorYagmuruDakikaBasina", 180.0, 0.0, 300.0);
        kavrulmaBaslangicGun = k.comment("Çimenin kaba toprağa dönmeye başladığı gün (afetBlokHasari kapalıyken hiç).")
                .defineInRange("kavrulmaBaslangicGun", 3.0, 0.0, 10.0);
        kavrulmaOlasiligi = k.comment("Sürecin sonunda oyuncu başına her tick'teki kavrulma denemesinin olasılığı (doğrusal artış).")
                .defineInRange("kavrulmaOlasiligi", 0.5, 0.0, 1.0);
        kavrulmaYaricap = k.comment("Kavrulmanın oyuncu çevresindeki yarıçapı, blok.")
                .defineInRange("kavrulmaYaricap", 64, 8, 160);
    }

    public static void kaydet() {
        bolum = AyarKaydi.sunucuBolumu("gunes_patlamasi", "Güneş patlaması (bölüm D)", GunesPatlamasiAyarlari::new);
        AyarKaydi.sunucuGocu(GunesPatlamasiAyarGocu.PLAN);
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

    public static int meteorYagmuruOncuSaniye() {
        return al().meteorYagmuruOncuSaniye.get();
    }

    public static double meteorYagmuruOncuDakikaBasina() {
        return al().meteorYagmuruOncuDakikaBasina.get();
    }

    public static double meteorYagmuruOncuSonDakikaBasina() {
        return al().meteorYagmuruOncuSonDakikaBasina.get();
    }

    public static int meteorYagmuruSaniye() {
        return al().meteorYagmuruSaniye.get();
    }

    public static int meteorYagmuruGunlukSaniye() {
        return al().meteorYagmuruGunlukSaniye.get();
    }

    public static double meteorYagmuruDakikaBasina() {
        return al().meteorYagmuruDakikaBasina.get();
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
