package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.BalikcilikAyarGocu;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.DelikOmru;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.HareketsizlikSogumasi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.IsirmaSuresi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code balikcilik} SERVER config bölümü (madde 15, N17; varsayılanlar ve gerekçeleri {@code .vibe/kararlar/balikcilik.md}
 * §C). Ters girilmiş aralıklar (en az &gt; en çok) okunurken sıraya sokulur. Eski varsayılanlar
 * {@link BalikcilikAyarGocu} ile yenisine çekilir (K-E61).
 */
public final class BalikcilikAyarlari {
    private static AyarKaydi.Bolum<BalikcilikAyarlari> bolum;

    private final ForgeConfigSpec.IntValue aktiflikEsigi;
    private final ForgeConfigSpec.IntValue delikOmruEnAzSn;
    private final ForgeConfigSpec.IntValue delikOmruEnCokSn;
    private final ForgeConfigSpec.IntValue donmaAsamasiSn;
    private final ForgeConfigSpec.IntValue chunkBasinaDelik;
    private final ForgeConfigSpec.IntValue aletHasari;
    private final ForgeConfigSpec.IntValue isirmaEnAzSn;
    private final ForgeConfigSpec.IntValue isirmaEnCokSn;
    private final ForgeConfigSpec.IntValue lureKisaltmaSn;
    private final ForgeConfigSpec.IntValue isirmaTabanSn;
    private final ForgeConfigSpec.DoubleValue kutupHizCarpani;
    private final ForgeConfigSpec.IntValue agirlikYaygin;
    private final ForgeConfigSpec.IntValue agirlikOrta;
    private final ForgeConfigSpec.IntValue agirlikNadir;
    private final ForgeConfigSpec.IntValue agirlikCop;
    private final ForgeConfigSpec.IntValue agirlikHazine;
    private final ForgeConfigSpec.IntValue agirlikEsya;
    private final ForgeConfigSpec.IntValue agirlikOzel;
    private final ForgeConfigSpec.IntValue sansCopAzalmasi;
    private final ForgeConfigSpec.IntValue sansHazineArtisi;
    private final ForgeConfigSpec.DoubleValue sogutmaEnCokC;
    private final ForgeConfigSpec.IntValue sogutmaTamSureSn;

    private BalikcilikAyarlari(ForgeConfigSpec.Builder k) {
        k.comment("Config sürümü (göç için; elle değiştirmeyin).")
                .defineInRange("ayarSurumu", BalikcilikAyarGocu.GUNCEL, 0, Integer.MAX_VALUE);
        aktiflikEsigi = k.comment("Buz deliği açılabilmesi için en az global donma ilerlemesi (%). Kızıl cüce döneminde donmuş",
                "su yüzeylerinde eşik aranmaz (madde 15).").defineInRange("aktiflikEsigi", 30, 0, 100);
        delikOmruEnAzSn = k.comment("Deliğin yeniden donana kadarki en kısa ömrü (sn).")
                .defineInRange("delikOmruEnAzSn", 240, 10, 3600);
        delikOmruEnCokSn = k.comment("Deliğin en uzun ömrü (sn); her delik bu aralıktan rastgele bir ömür alır.")
                .defineInRange("delikOmruEnCokSn", 300, 10, 3600);
        donmaAsamasiSn = k.comment("Ömrün sonundaki üç yavaş donma aşamasının her biri (sn); en kısa ömrün dörtte birini aşamaz.")
                .defineInRange("donmaAsamasiSn", 15, 1, 600);
        chunkBasinaDelik = k.comment("Bir chunk'ta aynı anda açık olabilecek en çok buz deliği.")
                .defineInRange("chunkBasinaDelik", 4, 0, 64);
        aletHasari = k.comment("Delik açan kazma/baltanın kaybettiği dayanıklılık (madde 15: 5).")
                .defineInRange("aletHasari", 5, 0, 100);
        isirmaEnAzSn = k.comment("Normal olta (buz deliği ve açık deniz) Lure'suz en kısa ısırma süresi (sn): en yaygın şeyin süresinin alt sınırı.",
                "Buz deliğinde süre çıkacak şeyin nadirliğine göre aralığın diliminden seçilir: yaygın %0–20, orta %20–60, nadir %60–100;",
                "açık denizde tüm aralıktan. Kutup Oltası bu değerleri kutupHizCarpani ile küçültür.")
                .defineInRange("isirmaEnAzSn", 10, 1, 600);
        isirmaEnCokSn = k.comment("Normal olta Lure'suz en uzun ısırma süresi (sn): en nadir şeyin süresinin üst sınırı.")
                .defineInRange("isirmaEnCokSn", 60, 1, 600);
        lureKisaltmaSn = k.comment("Normal olta Lure seviyesi başına kısaltma (sn).").defineInRange("lureKisaltmaSn", 6, 0, 60);
        isirmaTabanSn = k.comment("Normal oltada Lure ile inilebilecek en kısa ısırma süresi (sn).")
                .defineInRange("isirmaTabanSn", 10, 3, 600);
        kutupHizCarpani = k.comment("Kutup Oltası'nın ısırma süresi çarpanı: normal olta aralığı, Lure kısaltması ve taban bu oranla",
                "küçülür (0,5 = 2 kat hızlı: varsayılan 5–30 sn). 1,0 = normal olta ile aynı.")
                .defineInRange("kutupHizCarpani", 0.5, 0.1, 1.0);
        agirlikYaygin = k.comment("Kategori ağırlıkları (oran = ağırlık / toplam). Yaygın: Buz Alabalığı, Kutup Morinası.")
                .defineInRange("agirlikYaygin", LootAgirliklari.VARSAYILAN.yaygin(), 0, 1000);
        agirlikOrta = k.comment("Orta: Derin Soğuk Somonu, Buz Kefali.")
                .defineInRange("agirlikOrta", LootAgirliklari.VARSAYILAN.orta(), 0, 1000);
        agirlikNadir = k.comment("Nadir: Donmuş Ringa.").defineInRange("agirlikNadir", LootAgirliklari.VARSAYILAN.nadir(), 0, 1000);
        agirlikCop = k.comment("Çöp: Buz Parçası, Kar Topu.").defineInRange("agirlikCop", LootAgirliklari.VARSAYILAN.cop(), 0, 1000);
        agirlikHazine = k.comment("Hazine: büyülü kitap (Frost Walker / Lure / Luck of the Sea). Meteor ödülü hiç çıkmaz.")
                .defineInRange("agirlikHazine", LootAgirliklari.VARSAYILAN.hazine(), 0, 1000);
        agirlikEsya = k.comment("Eşya: Soğutma Parçası, Tuz Kristali, Buzlu İnci (her olta ile).")
                .defineInRange("agirlikEsya", LootAgirliklari.VARSAYILAN.esya(), 0, 1000);
        agirlikOzel = k.comment("Özel kutup balıkları (Kristal Levrek, Kutup Yılanbalığı, Aurora Balığı): yalnız Kutup Oltası'yla",
                "atılınca havuza eklenir; başka olta ile bu ağırlık yok sayılır.")
                .defineInRange("agirlikOzel", LootAgirliklari.VARSAYILAN.ozel(), 0, 1000);
        sansCopAzalmasi = k.comment("Şans (Luck of the Sea, şans efekti) seviyesi başına çöp ağırlığı azalması.")
                .defineInRange("sansCopAzalmasi", LootAgirliklari.VARSAYILAN.sansCopAzalmasi(), 0, 100);
        sansHazineArtisi = k.comment("Şans seviyesi başına hazine ağırlığı artışı.")
                .defineInRange("sansHazineArtisi", LootAgirliklari.VARSAYILAN.sansHazineArtisi(), 0, 100);
        sogutmaEnCokC = k.comment("Buz üstünde hareketsiz balık tutan oyuncunun ortamına eklenen en çok soğuk (°C; 0 kapalı).")
                .defineInRange("sogutmaEnCokC", 8.0, 0.0, 40.0);
        sogutmaTamSureSn = k.comment("Soğumanın tam güce ulaştığı hareketsiz süre (sn).")
                .defineInRange("sogutmaTamSureSn", 20, 1, 600);
    }

    public static void kaydet() {
        AyarKaydi.sunucuGocu(BalikcilikAyarGocu.PLAN);
        bolum = AyarKaydi.sunucuBolumu("balikcilik", "Buz altı balıkçılık (madde 15)", BalikcilikAyarlari::new);
    }

    private static BalikcilikAyarlari s() {
        return bolum.get();
    }

    public static double aktiflikEsigi() {
        return s().aktiflikEsigi.get();
    }

    public static DelikOmru.Ayarlar delikOmru() {
        BalikcilikAyarlari c = s();
        int a = c.delikOmruEnAzSn.get() * 20;
        int b = c.delikOmruEnCokSn.get() * 20;
        return new DelikOmru.Ayarlar(Math.min(a, b), Math.max(a, b), c.donmaAsamasiSn.get() * 20);
    }

    public static int chunkBasinaDelik() {
        return s().chunkBasinaDelik.get();
    }

    public static int aletHasari() {
        return s().aletHasari.get();
    }

    /** Normal oltanın ısırma süresi ayarları (buz deliği ve açık deniz). */
    public static IsirmaSuresi.Ayarlar isirma() {
        BalikcilikAyarlari c = s();
        int a = c.isirmaEnAzSn.get() * 20;
        int b = c.isirmaEnCokSn.get() * 20;
        return new IsirmaSuresi.Ayarlar(Math.min(a, b), Math.max(a, b), c.lureKisaltmaSn.get() * 20,
                c.isirmaTabanSn.get() * 20);
    }

    /** {@code kutup} ise Kutup Oltası'nın ({@code kutupHizCarpani} ile küçülmüş), değilse normal oltanın ısırma ayarları. */
    public static IsirmaSuresi.Ayarlar isirma(boolean kutup) {
        IsirmaSuresi.Ayarlar normal = isirma();
        return kutup ? IsirmaSuresi.olcekle(normal, s().kutupHizCarpani.get()) : normal;
    }

    /** Kategori ağırlıkları; config yüklenmediyse ya da hepsi 0 ise varsayılan. */
    public static LootAgirliklari lootAgirliklari() {
        try {
            BalikcilikAyarlari c = s();
            return new LootAgirliklari(c.agirlikYaygin.get(), c.agirlikOrta.get(), c.agirlikNadir.get(), c.agirlikCop.get(),
                    c.agirlikHazine.get(), c.agirlikEsya.get(), c.agirlikOzel.get(), c.sansCopAzalmasi.get(),
                    c.sansHazineArtisi.get());
        } catch (IllegalStateException | IllegalArgumentException yok) {
            return LootAgirliklari.VARSAYILAN;
        }
    }

    public static HareketsizlikSogumasi.Ayarlar sogutma() {
        BalikcilikAyarlari c = s();
        return new HareketsizlikSogumasi.Ayarlar(c.sogutmaEnCokC.get(), c.sogutmaTamSureSn.get());
    }
}
