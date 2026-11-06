package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GunesZinciri;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code gokyuzu} config bölümleri (N17). SERVER: güneş zinciri süreleri ve kızıl cücenin dünya görünümü (boyut,
 * gök/sis tonu); Forge sunucu config'ini istemciye eşitlediği için bütün oyuncular aynı göğü görür (madde 3). CLIENT:
 * yalnız oyuncunun kendi makinesine dair yoğunluk ayarları (madde 1, N13).
 */
public final class GokyuzuAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;
    private static AyarKaydi.Bolum<Istemci> istemci;

    private GokyuzuAyarlari() {
    }

    public static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("gokyuzu", "Ortak gökyüzü ve güneş zinciri (madde 16, bölüm D ve H)", Sunucu::new);
        istemci = AyarKaydi.istemciBolumu("gokyuzu", "Gökyüzü görseli (madde 16, N13)", Istemci::new);
    }

    public static GunesZinciri.Ayarlar zincir() {
        Sunucu s = sunucu.get();
        return new GunesZinciri.Ayarlar(s.supernovaSuresiGun.get(), s.kizilCuceGecikmeGun.get(), s.kizilCuceDogusSuresiTick.get(),
                s.kalintiGenislemeGun.get());
    }

    public static double kizilCuceBoyutOrani() {
        return sunucu.get().kizilCuceBoyutOrani.get();
    }

    public static double kizilCuceRenkGucu() {
        return sunucu.get().kizilCuceRenkGucu.get();
    }

    public static double yildizYogunlugu() {
        return istemci.get().yildizYogunlugu.get();
    }

    public static boolean nebulaKatmani() {
        return istemci.get().nebulaKatmani.get();
    }

    private static final class Sunucu {
        private final ForgeConfigSpec.DoubleValue supernovaSuresiGun;
        private final ForgeConfigSpec.DoubleValue kizilCuceGecikmeGun;
        private final ForgeConfigSpec.IntValue kizilCuceDogusSuresiTick;
        private final ForgeConfigSpec.DoubleValue kizilCuceBoyutOrani;
        private final ForgeConfigSpec.DoubleValue kizilCuceRenkGucu;
        private final ForgeConfigSpec.DoubleValue kalintiGenislemeGun;

        private Sunucu(ForgeConfigSpec.Builder k) {
            GunesZinciri.Ayarlar v = GunesZinciri.Ayarlar.VARSAYILAN;
            supernovaSuresiGun = k.comment("Süpernovanın gök yüzünde göründüğü süre, Minecraft günü (bölüm D: 1–2 gün).")
                    .defineInRange("supernovaSuresiGun", v.supernovaSuresiGun(), 1.0, 2.0);
            kizilCuceGecikmeGun = k.comment("Donma %100 olduktan kaç gün sonra kızıl cüce doğar (ilk şafakta); "
                    + "Kutup Sığınağı'nın 3 gününden kısa olamaz.")
                    .defineInRange("kizilCuceGecikmeGun", v.kizilCuceGecikmeGun(), GunesZinciri.EN_AZ_GECIKME_GUN, 365.0);
            kizilCuceDogusSuresiTick = k.comment("Kızıl cüce doğuşu (ufuk ışığı + diskin belirmesi), ölçekli tick.")
                    .defineInRange("kizilCuceDogusSuresiTick", (int) v.dogusSuresiTick(), 1_200, 24_000);
            kizilCuceBoyutOrani = k.comment("Kızıl cücenin eski güneşe göre boyu (bölüm D: %60).")
                    .defineInRange("kizilCuceBoyutOrani", 0.6, 0.2, 1.0);
            kizilCuceRenkGucu = k.comment("Kızıl cücede gök ve sis renginin kızıl/loş tonunun gücü (0 = vanilla renk, "
                    + "1 = tasarım tonu). Yalnız görsel; ışık motoru değişmez.")
                    .defineInRange("kizilCuceRenkGucu", 1.0, 0.0, 1.0);
            kalintiGenislemeGun = k.comment("Süpernova kalıntısı nebulasının %100'den %130'a genişleme süresi, gün.")
                    .defineInRange("kalintiGenislemeGun", v.kalintiGenislemeGun(), 1.0, 1000.0);
        }
    }

    private static final class Istemci {
        private final ForgeConfigSpec.DoubleValue yildizYogunlugu;
        private final ForgeConfigSpec.BooleanValue nebulaKatmani;

        private Istemci(ForgeConfigSpec.Builder k) {
            yildizYogunlugu = k.comment("Yıldız sayısı çarpanı (efekt kalitesinin yıldız sayısıyla çarpılır: Düşük 2000, Orta 4000, "
                    + "Yüksek 6000, Ultra 10000).")
                    .defineInRange("yildizYogunlugu", 1.0, 0.25, 2.0);
            nebulaKatmani = k.comment("Samanyolu, galaksi ve gece nebulası katmanı (kapalıyken yalnız yıldız ve süpernova kalıntısı).")
                    .define("nebulaKatmani", true);
        }
    }
}
