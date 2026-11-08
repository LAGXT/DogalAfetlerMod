package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuAyarGocu;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GunesZinciri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarSecimi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code gokyuzu} config bölümleri (N17). SERVER: güneş zinciri süreleri ve kızıl cücenin dünya görünümü (boyut,
 * gök/sis tonu) ve pulsar zinciri (pulsar-2); Forge sunucu config'ini istemciye eşitlediği için bütün oyuncular aynı göğü
 * görür (madde 3). Değişen varsayılanlar {@link GokyuzuAyarGocu} ile göç eder (K-E61). CLIENT: yalnız oyuncunun kendi
 * makinesine dair yoğunluk ayarları (madde 1, N13).
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
        AyarKaydi.sunucuGocu(GokyuzuAyarGocu.SURUM_YOLU, GokyuzuAyarGocu.GUNCEL, GokyuzuAyarGocu::plan);
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

    /**
     * Tam güçte bir kavurma denemesinin sütun seçme olasılığı; deneme sayısı alanla orantılı
     * ({@code PulsarKavurma.DENEME_YOGUNLUGU}).
     */
    public static double pulsarKavrulmaOlasiligi() {
        return sunucu.get().pulsarKavrulmaOlasiligi.get();
    }

    /** Pulsar döneminin süresi, oyun günü (K-C29): dolunca kara delik yutar. */
    public static int pulsarGun() {
        return sunucu.get().pulsarGun.get();
    }

    /** Tam güçte oyuncu başına gerçek dakikada pulsar meteoru. */
    public static double pulsarMeteorDakikaBasina() {
        return sunucu.get().pulsarMeteorDakikaBasina.get();
    }

    /** Pulsar kavurma kuyruğunun tick başına blok değişimi tavanı (TPS kısmasından önce). */
    public static int pulsarBlokButcesi() {
        return sunucu.get().pulsarBlokButcesi.get();
    }

    /** Pulsarda yüzeyde seyrek lav gözesi açılır mı. */
    public static boolean pulsarLav() {
        return sunucu.get().pulsarLav.get();
    }

    /** Kara delik yutmasından sonraki buzul kıyametin süresi, oyun günü (ardından ilk şafakta kızıl cüce doğar). */
    public static int pulsarSonrasiGunessizGun() {
        return sunucu.get().pulsarSonrasiGunessizGun.get();
    }

    /** Pulsar titreşiminin şiddet çarpanı (0 = sabit, yalnız yavaş sarsıntı; fotosensitivite). */
    public static double pulsarTitresim() {
        return istemci.get().pulsarTitresim.get();
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
        private final ForgeConfigSpec.IntValue pulsarGun;
        private final ForgeConfigSpec.DoubleValue pulsarMeteorDakikaBasina;
        private final ForgeConfigSpec.IntValue pulsarBlokButcesi;
        private final ForgeConfigSpec.BooleanValue pulsarLav;
        private final ForgeConfigSpec.IntValue pulsarSonrasiGunessizGun;

        private Sunucu(ForgeConfigSpec.Builder k) {
            GunesZinciri.Ayarlar v = GunesZinciri.Ayarlar.VARSAYILAN;
            k.comment("Config göçünün sürümü (K-E61): dosya bundan eskiyse açılışta eski varsayılanında kalan gökyüzü değerleri "
                    + "yeni varsayılana çekilir, elle değiştirilenlere dokunulmaz. Elle değiştirmeyin.")
                    .defineInRange("ayarSurumu", GokyuzuAyarGocu.GUNCEL, 0, Integer.MAX_VALUE);
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
                    .defineInRange("pulsarIsinmaGun", 1.0, 0.05, 30.0);
            pulsarYanginDakikaBasina = k.comment("Tam güçte oyuncu başına dakikada yeni yangın odağı (ortak yangın sistemi; "
                    + "TPS kısması ve yangın tavanıyla sınırlı, afetBlokHasari=false iken yok).")
                    .defineInRange("pulsarYanginDakikaBasina", 24.0, 0.0, 120.0);
            pulsarKavrulmaOlasiligi = k.comment("Tam güçte bir kavurma denemesinin sütun seçme olasılığı (deneme sayısı alanla "
                    + "orantılı; varsayılanla 3 oyun gününde 96 blok yarıçapta yüzey küle döner).")
                    .defineInRange("pulsarKavrulmaOlasiligi", 0.6, 0.0, 1.0);
            pulsarGun = k.comment("Pulsar döneminin süresi, Minecraft günü (oyun günü: uyku ve /dogalafet gun sayılır, "
                    + "/dogalafet hiz sayılmaz). Dolunca pulsarı uzak bir kara delik yutar.")
                    .defineInRange("pulsarGun", 15, 1, 120);
            pulsarMeteorDakikaBasina = k.comment("Tam güçte oyuncu başına gerçek dakikada pulsar meteoru (güneş/ay "
                    + "yağmurunun 180'inden seyrek; uçan meteor tavanı ortak).")
                    .defineInRange("pulsarMeteorDakikaBasina", 12.0, 0.0, 180.0);
            pulsarBlokButcesi = k.comment("Pulsar kavurmasının (kül, kömürleşme, cam, lav) tick başına en çok blok değişimi; "
                    + "TPS kısmasıyla çarpılır.")
                    .defineInRange("pulsarBlokButcesi", 256, 0, 4096);
            pulsarLav = k.comment("Pulsarda deniz seviyesinin üstünde, oyunculardan uzakta çok seyrek tek bloklu lav gözesi "
                    + "açılır (afetBlokHasari=false iken yok).")
                    .define("pulsarLav", true);
            pulsarSonrasiGunessizGun = k.comment("Kara delik pulsarı yuttuktan sonraki buzul kıyametin süresi, Minecraft günü; "
                    + "ardından ilk şafakta kızıl cüce doğar ve doğuş bitince eski güneş geri gelir.")
                    .defineInRange("pulsarSonrasiGunessizGun", 10, 1, 120);
        }
    }

    private static final class Istemci {
        private final ForgeConfigSpec.DoubleValue yildizYogunlugu;
        private final ForgeConfigSpec.BooleanValue nebulaKatmani;
        private final ForgeConfigSpec.DoubleValue pulsarTitresim;

        private Istemci(ForgeConfigSpec.Builder k) {
            yildizYogunlugu = k.comment("Yıldız sayısı çarpanı (efekt kalitesinin yıldız sayısıyla çarpılır: Düşük 2000, Orta 4000, "
                    + "Yüksek 6000, Ultra 10000).")
                    .defineInRange("yildizYogunlugu", 1.0, 0.25, 2.0);
            nebulaKatmani = k.comment("Samanyolu, galaksi ve gece nebulası katmanı (kapalıyken yalnız yıldız ve süpernova kalıntısı).")
                    .define("nebulaKatmani", true);
            pulsarTitresim = k.comment("Pulsarın hızlı yanal titreşiminin ve çatırtı parlamalarının şiddeti (0 = sabit, yalnız "
                    + "yavaş sarsıntı; ışığa duyarlı oyuncular için).")
                    .defineInRange("pulsarTitresim", 1.0, 0.0, 1.0);
        }
    }
}
