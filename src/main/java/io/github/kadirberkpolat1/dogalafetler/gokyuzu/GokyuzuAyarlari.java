package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GunesZinciri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarSecimi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code gokyuzu} config bölümleri (N17). SERVER: güneş zinciri süreleri ve kızıl cücenin dünya görünümü (boyut,
 * gök/sis tonu); Forge sunucu config'ini istemciye eşitlediği için bütün oyuncular aynı göğü görür (madde 3). CLIENT:
 * yalnız oyuncunun kendi makinesine dair yoğunluk ayarları (madde 1, N13).
 */
public final class GokyuzuAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;
    private static AyarKaydi.Bolum<Istemci> istemci;
    private static GunesZinciri.Ayarlar zincirOnbellegi;

    private GokyuzuAyarlari() {
    }

    public static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("gokyuzu", "Ortak gökyüzü ve güneş zinciri (madde 16, bölüm D ve H)", Sunucu::new);
        istemci = AyarKaydi.istemciBolumu("gokyuzu", "Gökyüzü görseli (madde 16, N13)", Istemci::new);
    }

    /** Güneş zinciri ayarları; süreç her tick okur, değerler değişmedikçe aynı kayıt döner. */
    public static GunesZinciri.Ayarlar zincir() {
        Sunucu s = sunucu.get();
        double supernova = s.supernovaSuresiGun.get();
        double gecikme = s.kizilCuceGecikmeGun.get();
        long dogus = s.kizilCuceDogusSuresiTick.get();
        double kalinti = s.kalintiGenislemeGun.get();
        GunesZinciri.Ayarlar onbellek = zincirOnbellegi;
        if (onbellek == null || onbellek.supernovaSuresiGun() != supernova || onbellek.kizilCuceGecikmeGun() != gecikme
                || onbellek.dogusSuresiTick() != dogus || onbellek.kalintiGenislemeGun() != kalinti) {
            onbellek = new GunesZinciri.Ayarlar(supernova, gecikme, dogus, kalinti);
            zincirOnbellegi = onbellek;
        }
        return onbellek;
    }

    public static double kizilCuceBoyutOrani() {
        return sunucu.get().kizilCuceBoyutOrani.get();
    }

    public static double kizilCuceRenkGucu() {
        return sunucu.get().kizilCuceRenkGucu.get();
    }

    /** Süpernova zincirinin doğal doğuşunda kızıl cüce yerine pulsar doğma olasılığı. */
    public static double pulsarOlasiligi() {
        return sunucu.get().pulsarOlasiligi.get();
    }

    /** Pulsarın ısınma süresi (ölçekli tick): dünya etkileri bu sürede tam güce çıkar. */
    public static long pulsarIsinmaTick() {
        return Math.round(sunucu.get().pulsarIsinmaGun.get() * GunesZinciri.GUN_TICK);
    }

    /** Tam güçte oyuncu başına dakikada pulsar yangını odak sayısı. */
    public static double pulsarYanginDakikaBasina() {
        return sunucu.get().pulsarYanginDakikaBasina.get();
    }

    /** Tam güçte oyuncu başına ölçekli tick'te kavrulma denemesi olasılığı (çimen → kaba toprak). */
    public static double pulsarKavrulmaOlasiligi() {
        return sunucu.get().pulsarKavrulmaOlasiligi.get();
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
        private final ForgeConfigSpec.DoubleValue pulsarOlasiligi;
        private final ForgeConfigSpec.DoubleValue pulsarIsinmaGun;
        private final ForgeConfigSpec.DoubleValue pulsarYanginDakikaBasina;
        private final ForgeConfigSpec.DoubleValue pulsarKavrulmaOlasiligi;

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
            pulsarOlasiligi = k.comment("Süpernova zincirinin doğal doğuşunda kızıl cüce yerine pulsar doğma olasılığı "
                    + "(0 = hiç, 1 = her zaman). Seçim dünya tohumu ve döngüden belirlenir; /dogalafet pulsar baslat zorlar.")
                    .defineInRange("pulsarOlasiligi", PulsarSecimi.VARSAYILAN_OLASILIK, 0.0, 1.0);
            pulsarIsinmaGun = k.comment("Pulsar doğduktan sonra dünya etkilerinin (sıcaklık, kavrulma, yangın, buharlaşma) "
                    + "tam güce çıkma süresi, Minecraft günü.")
                    .defineInRange("pulsarIsinmaGun", 2.0, 0.05, 30.0);
            pulsarYanginDakikaBasina = k.comment("Tam güçte oyuncu başına dakikada yeni yangın odağı (ortak yangın sistemi; "
                    + "TPS kısması ve yangın tavanıyla sınırlı, afetBlokHasari=false iken yok).")
                    .defineInRange("pulsarYanginDakikaBasina", 20.0, 0.0, 120.0);
            pulsarKavrulmaOlasiligi = k.comment("Tam güçte oyuncu başına ölçekli tick'te güneşe açık çimenin kaba toprağa "
                    + "dönme denemesi olasılığı (güneş patlaması kavrulmasıyla aynı kural).")
                    .defineInRange("pulsarKavrulmaOlasiligi", 0.6, 0.0, 1.0);
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
