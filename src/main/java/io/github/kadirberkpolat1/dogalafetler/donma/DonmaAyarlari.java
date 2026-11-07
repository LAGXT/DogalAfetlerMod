package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaEsikleri;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisAlani;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.YetismeButcesi;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code donma} config bölümleri (N17; varsayılanlar ve gerekçeleri {@code .vibe/kararlar/donma.md} §C). SERVER: süreler,
 * taban, kalıcılık, bütçe, adım eşikleri, kalıcı kış maskesi, buharlaşma. CLIENT: parçacık yoğunluğu. Kalıcı kış maskesinin
 * parametreleri çözülme başlarken dünya dönemine yazılır ({@link KisBolgesi}); buradaki değerler yalnız yeni döngü içindir.
 * Geçersiz eşik kombinasyonu ERROR log'a düşer ve varsayılan eşikler kullanılır.
 */
public final class DonmaAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;
    private static AyarKaydi.Bolum<Istemci> istemci;
    private static volatile DonmaEsikleri onbellekEsikler;
    private static volatile List<?> onbellekAnahtar;
    private static volatile int onbellekKarYagisi;

    private DonmaAyarlari() {
    }

    public static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("donma", "Dünya donması, çözülme ve buharlaşma (madde 9, N9, N10)", Sunucu::new);
        istemci = AyarKaydi.istemciBolumu("donma", "Dünya donması efektleri (madde 9)", Istemci::new);
    }

    private static Sunucu s() {
        return sunucu.get();
    }

    public static double donmaSuresiGun() {
        return s().donmaSuresi.get();
    }

    public static double cozulmeSuresiGun() {
        return s().cozulmeSuresi.get();
    }

    public static double cozulmeTabani() {
        return s().cozulmeTabani.get();
    }

    public static boolean kalici() {
        return s().kalici.get();
    }

    public static boolean bagimsizCozulme() {
        return s().bagimsizCozulme.get();
    }

    public static int seviyeAdimi() {
        return s().seviyeAdimi.get();
    }

    public static YetismeButcesi butce() {
        return new YetismeButcesi(s().tickBasinaMaksBlok.get(), DonmaZamanlayici.BLOK_MALIYETI,
                s().tickBasinaMaksMs.get() * 1_000_000L);
    }

    public static double buharlasmaEnCok() {
        return s().buharlasmaEnCok.get();
    }

    public static double buharlasmaSuresiGun() {
        return s().buharlasmaSuresi.get();
    }

    /** Yağmurun istemcide kar olarak göründüğü ilerleme, madde 9 sırasına çekilmiş (K-D33; {@link #esikler}). */
    public static int karYagisiEsigi() {
        esikler();
        return onbellekKarYagisi;
    }

    public static double kisOrani() {
        return s().kisOrani.get();
    }

    public static double kisDalgaBoyu() {
        return s().kisDalgaBoyu.get();
    }

    public static double kisGecisBandi() {
        return s().kisGecisBandi.get();
    }

    public static KisAlani.Ayar kisAyar() {
        Sunucu c = s();
        return new KisAlani.Ayar(c.kisYGecisAlt.get(), c.kisYGecisUst.get(), c.ilikOfsetSicaklikTabani.get(),
                c.ilikOfsetKatsayisi.get(), c.ilikOfsetEnCok.get());
    }

    /** Titreşim genliği; config henüz yüklenmediyse (ilk veri paketi yüklemesi) varsayılan. */
    static int titresimGuvenli() {
        try {
            return s().titresimGenligi.get();
        } catch (IllegalStateException yuklenmedi) {
            return DonmaEsikleri.VARSAYILAN.titresimGenligi();
        }
    }

    public static double parcacikYogunlugu() {
        return istemci.get().parcacikYogunlugu.get();
    }

    /**
     * Adım eşikleri (config değişince yeniden kurulur; geçersizse varsayılan). Madde 9 sırası (D2, K-D33): ilk kar kıyı
     * buzundan önce, yağmur yerine kar kıyı buzundan sonra ve kalın buzdan önce gelmeli; bozuksa eşik en yakın sınıra
     * çekilir ve WARN yazılır.
     */
    public static DonmaEsikleri esikler() {
        Sunucu c = s();
        List<?> anahtar = List.of(c.karEsikleri.get(), c.karCozulmeEsikleri.get(), c.ilkBuzEsigi.get(),
                c.buzKalinlikAdimi.get(), c.kiyiKatsayisi.get(), c.kiyiUzaklikSiniri.get(), c.maksBuzKalinligi.get(),
                c.buzCozulmeTabani.get(), c.buzCozulmeKiyiKatsayisi.get(), c.buzCozulmeEnAz.get(), c.buzCozulmeAdimi.get(),
                c.titresimGenligi.get(), c.inceBuzPayi.get(), c.yapiKarEsigi.get(), c.yapiKarCozulmeEsigi.get(),
                c.karYagisiEsigi.get());
        DonmaEsikleri e = onbellekEsikler;
        if (e != null && anahtar.equals(onbellekAnahtar)) {
            return e;
        }
        try {
            int[] kar = dizi(c.karEsikleri.get());
            int[] sirali = DonmaEsikleri.siraliKarEsikleri(kar, c.ilkBuzEsigi.get(), c.titresimGenligi.get());
            if (sirali != kar) {
                DogalAfetler.LOGGER.warn("[donma] ilk kar eşiği ({}) kıyı buzundan ({}) önce olmalı (madde 9 sırası); {} kullanılıyor",
                        kar[0], c.ilkBuzEsigi.get(), sirali[0]);
            }
            e = new DonmaEsikleri(sirali, dizi(c.karCozulmeEsikleri.get()), c.ilkBuzEsigi.get(),
                    c.buzKalinlikAdimi.get(), c.kiyiKatsayisi.get(), c.kiyiUzaklikSiniri.get(), c.maksBuzKalinligi.get(),
                    c.buzCozulmeTabani.get(), c.buzCozulmeKiyiKatsayisi.get(), c.buzCozulmeEnAz.get(),
                    c.buzCozulmeAdimi.get(), c.titresimGenligi.get(), c.inceBuzPayi.get())
                    .yapiKari(c.yapiKarEsigi.get(), c.yapiKarCozulmeEsigi.get());
        } catch (IllegalArgumentException hata) {
            DogalAfetler.LOGGER.error("[donma] config eşikleri geçersiz ({}); varsayılan eşikler kullanılıyor", hata.getMessage());
            e = DonmaEsikleri.VARSAYILAN;
        }
        int karYagisi = e.siraliKarYagisi(c.karYagisiEsigi.get());
        if (karYagisi != c.karYagisiEsigi.get()) {
            DogalAfetler.LOGGER.warn("[donma] kar yağışı eşiği ({}) kıyı buzundan ({}) sonra, kalın buzdan ({}) önce olmalı"
                    + " (madde 9 sırası); {} kullanılıyor", c.karYagisiEsigi.get(), e.ilkBuzEsigi(), e.kalinBuzEsigi(), karYagisi);
        }
        onbellekKarYagisi = karYagisi;
        onbellekEsikler = e;
        onbellekAnahtar = anahtar;
        return e;
    }

    private static int[] dizi(List<? extends Integer> liste) {
        return liste.stream().mapToInt(Integer::intValue).toArray();
    }

    private static final class Sunucu {
        final ForgeConfigSpec.DoubleValue donmaSuresi;
        final ForgeConfigSpec.DoubleValue cozulmeSuresi;
        final ForgeConfigSpec.DoubleValue cozulmeTabani;
        final ForgeConfigSpec.BooleanValue kalici;
        final ForgeConfigSpec.BooleanValue bagimsizCozulme;
        final ForgeConfigSpec.IntValue tickBasinaMaksBlok;
        final ForgeConfigSpec.IntValue tickBasinaMaksMs;
        final ForgeConfigSpec.IntValue seviyeAdimi;
        final ForgeConfigSpec.ConfigValue<List<? extends Integer>> karEsikleri;
        final ForgeConfigSpec.ConfigValue<List<? extends Integer>> karCozulmeEsikleri;
        final ForgeConfigSpec.IntValue ilkBuzEsigi;
        final ForgeConfigSpec.IntValue buzKalinlikAdimi;
        final ForgeConfigSpec.IntValue kiyiKatsayisi;
        final ForgeConfigSpec.IntValue kiyiUzaklikSiniri;
        final ForgeConfigSpec.IntValue maksBuzKalinligi;
        final ForgeConfigSpec.IntValue buzCozulmeTabani;
        final ForgeConfigSpec.IntValue buzCozulmeKiyiKatsayisi;
        final ForgeConfigSpec.IntValue buzCozulmeEnAz;
        final ForgeConfigSpec.IntValue buzCozulmeAdimi;
        final ForgeConfigSpec.IntValue titresimGenligi;
        final ForgeConfigSpec.IntValue inceBuzPayi;
        final ForgeConfigSpec.IntValue karYagisiEsigi;
        final ForgeConfigSpec.IntValue yapiKarEsigi;
        final ForgeConfigSpec.IntValue yapiKarCozulmeEsigi;
        final ForgeConfigSpec.DoubleValue kisOrani;
        final ForgeConfigSpec.DoubleValue kisDalgaBoyu;
        final ForgeConfigSpec.DoubleValue kisGecisBandi;
        final ForgeConfigSpec.IntValue kisYGecisAlt;
        final ForgeConfigSpec.IntValue kisYGecisUst;
        final ForgeConfigSpec.DoubleValue ilikOfsetSicaklikTabani;
        final ForgeConfigSpec.DoubleValue ilikOfsetKatsayisi;
        final ForgeConfigSpec.DoubleValue ilikOfsetEnCok;
        final ForgeConfigSpec.DoubleValue buharlasmaEnCok;
        final ForgeConfigSpec.DoubleValue buharlasmaSuresi;

        Sunucu(ForgeConfigSpec.Builder k) {
            donmaSuresi = k.comment("Süpernova sonrası donmanın %0'dan %100'e süresi (Minecraft günü; madde 9 'birkaç gün').")
                    .defineInRange("donmaSuresi", 6.0, 0.05, 365.0);
            cozulmeSuresi = k.comment("Kızıl cüce çözülmesinin %100'den tabana süresi (Minecraft günü; madde 9 varsayılan 10).")
                    .defineInRange("cozulmeSuresi", 10.0, 0.05, 365.0);
            cozulmeTabani = k.comment("Çözülmenin indiği taban ilerleme (%; madde 9 varsayılan 40). Dünya tamamen erimez.")
                    .defineInRange("cozulmeTabani", 40.0, 0.0, 100.0);
            kalici = k.comment("Donma kalıcı mı: true ise kızıl cüce döneminde de çözülme olmaz (madde 9).")
                    .define("kalici", false);
            bagimsizCozulme = k.comment("Kızıl cüce dışında bağımsız çözülme: güneş normalken ilerleme çözülme süresinde 0'a iner"
                    + " (madde 9, varsayılan kapalı).").define("bagimsizCozulme", false);
            tickBasinaMaksBlok = k.comment("Tick başına en çok blok değişimi (TPS kısmasından önce; madde 9).")
                    .defineInRange("tickBasinaMaksBlok", 256, 1, 8192);
            tickBasinaMaksMs = k.comment("Donma işçisinin bir tick'te harcayabileceği en uzun süre (ms).")
                    .defineInRange("tickBasinaMaksMs", 4, 1, 40);
            seviyeAdimi = k.comment("Bir chunk geçişinde ilerlemenin en çok ilerlediği puan (yetişme adımı).")
                    .defineInRange("seviyeAdimi", 5, 1, 100);
            k.comment("Adım eşikleri (sütunun etkin seviyesi, %). Donarken donma, çözülürken çözülme eşikleri. Donma sırası"
                    + " (madde 9): ilk kar (çimende karlı çimen) < ilkBuzEsigi < kalın buz; bozulursa ilk kar"
                    + " ilkBuzEsigi-1'e çekilir.").push("esikler");
            karEsikleri = k.comment("Kar katmanı 1..n'nin geldiği seviyeler (artan; ilki ilkBuzEsigi'nden küçük).")
                    .defineList("karEsikleri", List.of(15, 55, 70, 85, 95), o -> o instanceof Integer);
            karCozulmeEsikleri = k.comment("Kar katmanı 1..n'nin kalktığı seviyeler (her biri donma eşiğinden küçük değil).")
                    .defineList("karCozulmeEsikleri", List.of(55, 65, 75, 85, 95), o -> o instanceof Integer);
            ilkBuzEsigi = k.comment("Kıyıdaki su sütununda ilk buzun geldiği seviye.").defineInRange("ilkBuzEsigi", 20, 0, 100);
            buzKalinlikAdimi = k.comment("Buz her bu kadar puanda bir blok kalınlaşır.").defineInRange("buzKalinlikAdimi", 20, 1, 100);
            kiyiKatsayisi = k.comment("Kıyıdan her blok içeride ilk buz bu kadar puan geç gelir.")
                    .defineInRange("kiyiKatsayisi", 2, 0, 20);
            kiyiUzaklikSiniri = k.comment("Kıyı uzaklığının ölçüldüğü en büyük değer (blok).")
                    .defineInRange("kiyiUzaklikSiniri", 15, 0, 15);
            maksBuzKalinligi = k.comment("En kalın buz (blok); su sütununun en alt bloğu ayrıca hiç donmaz.")
                    .defineInRange("maksBuzKalinligi", 4, 0, 16);
            buzCozulmeTabani = k.comment("Kıyıdaki buzun çözüldüğü seviye.").defineInRange("buzCozulmeTabani", 45, 0, 100);
            buzCozulmeKiyiKatsayisi = k.comment("Kıyıdan her blok içeride buz bu kadar puan geç çözülür.")
                    .defineInRange("buzCozulmeKiyiKatsayisi", 3, 0, 20);
            buzCozulmeEnAz = k.comment("Göl ortasındaki buzun da kalktığı en düşük seviye.").defineInRange("buzCozulmeEnAz", 10, 0, 100);
            buzCozulmeAdimi = k.comment("Çözülürken üst katmanlar her bu kadar puanda bir incelir.")
                    .defineInRange("buzCozulmeAdimi", 10, 1, 100);
            titresimGenligi = k.comment("Sütuna özgü sabit titreşim (±puan); bütün eşiklerden küçük olmalı.")
                    .defineInRange("titresimGenligi", 8, 0, 20);
            inceBuzPayi = k.comment("İlk buz katmanının ilk/son bu kadar puanında üst blok ince buz (kalınlık adımlarından küçük).")
                    .defineInRange("inceBuzPayi", 6, 0, 50);
            karYagisiEsigi = k.comment("Yağmurun istemcide kar olarak göründüğü ilerleme (%). Sıra (madde 9): ilkBuzEsigi < kar yağışı"
                    + " ≤ kalın buz (ilkBuzEsigi + buzKalinlikAdimi); dışındaysa en yakın sınıra çekilir.")
                    .defineInRange("karYagisiEsigi", DonmaEsikleri.VARSAYILAN_KAR_YAGISI, 0, 100);
            yapiKarEsigi = k.comment("Yapı bloklarına (#dogalafetler:donma/yapi_kar_zemini: tahta, taş/tuğla tam bloklar) karın"
                    + " geldiği seviye; kalın buz aşaması (madde 9 'ilerleyen aşamalarda yapılar').")
                    .defineInRange("yapiKarEsigi", 40, 9, 100);
            yapiKarCozulmeEsigi = k.comment("Yapı karının kalktığı seviye (yapiKarEsigi'nden küçük olamaz).")
                    .defineInRange("yapiKarCozulmeEsigi", 55, 9, 100);
            k.pop();
            k.comment("Kalıcı kış bölgeleri (kızıl cüce çözülmesi). Çözülme başlarken dünyaya yazılır; sonradan değişmez.").push("kis");
            kisOrani = k.comment("Seed'li gürültü maskesiyle seçilen kara oranı (madde 9 'yaklaşık %25').")
                    .defineInRange("kisOrani", 0.25, 0.0, 1.0);
            kisDalgaBoyu = k.comment("Maskenin dalga boyu (blok); kış bölgeleri birkaç yüz blokluk adalar.")
                    .defineInRange("kisDalgaBoyu", 1200.0, 16.0, 100000.0);
            kisGecisBandi = k.comment("Maske sınırının yumuşak geçiş bandı (gürültü birimi).")
                    .defineInRange("kisGecisBandi", 0.03, 0.0, 0.5);
            kisYGecisAlt = k.comment("Yükseklik ağırlığının başladığı yüzey Y'si.").defineInRange("kisYGecisAlt", 88, -64, 320);
            kisYGecisUst = k.comment("Bu yüzey Y'sinden itibaren tam kış (madde 9 varsayılan Y=120).")
                    .defineInRange("kisYGecisUst", 120, -63, 320);
            ilikOfsetSicaklikTabani = k.comment("Bu biyom sıcaklığının altında ılık ofset yok.")
                    .defineInRange("ilikOfsetSicaklikTabani", 0.3, -2.0, 3.0);
            ilikOfsetKatsayisi = k.comment("Biyom sıcaklık birimi başına erken çözülme puanı ('önce ılık bölgeler').")
                    .defineInRange("ilikOfsetKatsayisi", 8.0, 0.0, 50.0);
            ilikOfsetEnCok = k.comment("Ilık ofsetin üst sınırı (puan).").defineInRange("ilikOfsetEnCok", 10.0, 0.0, 50.0);
            k.pop();
            buharlasmaEnCok = k.comment("Güneş patlamasında suyun yüzeyden en çok kuruyacağı derinlik (blok; N9 varsayılan 6).")
                    .defineInRange("buharlasmaEnCok", 6.0, 0.0, 64.0);
            buharlasmaSuresi = k.comment("Buharlaşmanın en çoğa vardığı süre (Minecraft günü; güneş patlamasının 5 günü).")
                    .defineInRange("buharlasmaSuresi", 5.0, 0.05, 365.0);
        }
    }

    private static final class Istemci {
        final ForgeConfigSpec.DoubleValue parcacikYogunlugu;

        Istemci(ForgeConfigSpec.Builder k) {
            parcacikYogunlugu = k.comment("Ayaz, kar ve buhar parçacıklarının yoğunluk çarpanı (efekt kalitesiyle birlikte).")
                    .defineInRange("parcacikYogunlugu", 1.0, 0.0, 3.0);
        }
    }
}
