package io.github.kadirberkpolat1.dogalafetler.balikcilik;

import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.DelikOmru;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.HareketsizlikSogumasi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.IsirmaSuresi;
import io.github.kadirberkpolat1.dogalafetler.balikcilik.mantik.LootAgirliklari;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code balikcilik} SERVER config bölümü (madde 15, N17; varsayılanlar ve gerekçeleri {@code .vibe/kararlar/balikcilik.md}
 * §C). Ters girilmiş aralıklar (en az &gt; en çok) okunurken sıraya sokulur.
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
    private final ForgeConfigSpec.IntValue agirlikYaygin;
    private final ForgeConfigSpec.IntValue agirlikOrta;
    private final ForgeConfigSpec.IntValue agirlikNadir;
    private final ForgeConfigSpec.IntValue agirlikCop;
    private final ForgeConfigSpec.IntValue agirlikHazine;
    private final ForgeConfigSpec.IntValue sansCopAzalmasi;
    private final ForgeConfigSpec.IntValue sansHazineArtisi;
    private final ForgeConfigSpec.DoubleValue sogutmaEnCokC;
    private final ForgeConfigSpec.IntValue sogutmaTamSureSn;
    private final ForgeConfigSpec.DoubleValue cigAnlik;
    private final ForgeConfigSpec.DoubleValue cigToplam;
    private final ForgeConfigSpec.IntValue cigSureSn;
    private final ForgeConfigSpec.DoubleValue pismisAnlik;
    private final ForgeConfigSpec.DoubleValue pismisToplam;
    private final ForgeConfigSpec.IntValue pismisSureSn;
    private final ForgeConfigSpec.DoubleValue ringaAnlik;
    private final ForgeConfigSpec.DoubleValue ringaToplam;
    private final ForgeConfigSpec.IntValue ringaSureSn;

    private BalikcilikAyarlari(ForgeConfigSpec.Builder k) {
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
        isirmaEnAzSn = k.comment("Lure'suz en kısa ısırma süresi (sn; madde 15: 30–60).")
                .defineInRange("isirmaEnAzSn", 30, 1, 600);
        isirmaEnCokSn = k.comment("Lure'suz en uzun ısırma süresi (sn).").defineInRange("isirmaEnCokSn", 60, 1, 600);
        lureKisaltmaSn = k.comment("Lure seviyesi başına kısaltma (sn; vanilla 5).").defineInRange("lureKisaltmaSn", 5, 0, 60);
        isirmaTabanSn = k.comment("Lure ile inilebilecek en kısa ısırma süresi (sn).")
                .defineInRange("isirmaTabanSn", 10, 3, 600);
        agirlikYaygin = k.comment("Kategori ağırlıkları (oran = ağırlık / toplam). Yaygın: Buz Alabağı, Kutup Morinası.")
                .defineInRange("agirlikYaygin", LootAgirliklari.VARSAYILAN.yaygin(), 0, 1000);
        agirlikOrta = k.comment("Orta: Derin Soğuk Somonu, Buz Kefali.")
                .defineInRange("agirlikOrta", LootAgirliklari.VARSAYILAN.orta(), 0, 1000);
        agirlikNadir = k.comment("Nadir: Donmuş Ringa.").defineInRange("agirlikNadir", LootAgirliklari.VARSAYILAN.nadir(), 0, 1000);
        agirlikCop = k.comment("Çöp: Buz Parçası, Kar Topu.").defineInRange("agirlikCop", LootAgirliklari.VARSAYILAN.cop(), 0, 1000);
        agirlikHazine = k.comment("Hazine: büyülü kitap (Frost Walker / Lure / Luck of the Sea). Meteor ödülü hiç çıkmaz.")
                .defineInRange("agirlikHazine", LootAgirliklari.VARSAYILAN.hazine(), 0, 1000);
        sansCopAzalmasi = k.comment("Şans (Luck of the Sea, şans efekti) seviyesi başına çöp ağırlığı azalması.")
                .defineInRange("sansCopAzalmasi", LootAgirliklari.VARSAYILAN.sansCopAzalmasi(), 0, 100);
        sansHazineArtisi = k.comment("Şans seviyesi başına hazine ağırlığı artışı.")
                .defineInRange("sansHazineArtisi", LootAgirliklari.VARSAYILAN.sansHazineArtisi(), 0, 100);
        sogutmaEnCokC = k.comment("Buz üstünde hareketsiz balık tutan oyuncunun ortamına eklenen en çok soğuk (°C; 0 kapalı).")
                .defineInRange("sogutmaEnCokC", 8.0, 0.0, 40.0);
        sogutmaTamSureSn = k.comment("Soğumanın tam güce ulaştığı hareketsiz süre (sn).")
                .defineInRange("sogutmaTamSureSn", 20, 1, 600);
        cigAnlik = k.comment("Çiğ buz balığı: anında vücut sıcaklığı değişimi (birim, −100…+100 ölçeği; negatif serinletir).")
                .defineInRange("cigAnlik", -2.0, -40.0, 40.0);
        cigToplam = k.comment("Çiğ: süre boyunca giderek azalarak eklenen toplam (madde 14 hafif %5–10).")
                .defineInRange("cigToplam", -6.0, -40.0, 40.0);
        cigSureSn = k.comment("Çiğ: etkinin süresi (sn).").defineInRange("cigSureSn", 60, 1, 600);
        pismisAnlik = k.comment("Pişmiş buz balığı: anında değişim (madde 14 orta %15–25 toplamının ilk parçası).")
                .defineInRange("pismisAnlik", 4.0, -40.0, 40.0);
        pismisToplam = k.comment("Pişmiş: süre boyunca eklenen toplam.").defineInRange("pismisToplam", 14.0, -40.0, 40.0);
        pismisSureSn = k.comment("Pişmiş: süre (sn).").defineInRange("pismisSureSn", 90, 1, 600);
        ringaAnlik = k.comment("Pişmiş Donmuş Ringa (madde 15: daha çok ısınır): anında değişim.")
                .defineInRange("ringaAnlik", 6.0, -40.0, 40.0);
        ringaToplam = k.comment("Pişmiş Donmuş Ringa: süre boyunca eklenen toplam.").defineInRange("ringaToplam", 19.0, -40.0, 40.0);
        ringaSureSn = k.comment("Pişmiş Donmuş Ringa: süre (sn).").defineInRange("ringaSureSn", 120, 1, 600);
    }

    public static void kaydet() {
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

    public static IsirmaSuresi.Ayarlar isirma() {
        BalikcilikAyarlari c = s();
        int a = c.isirmaEnAzSn.get() * 20;
        int b = c.isirmaEnCokSn.get() * 20;
        return new IsirmaSuresi.Ayarlar(Math.min(a, b), Math.max(a, b), c.lureKisaltmaSn.get() * 20,
                c.isirmaTabanSn.get() * 20);
    }

    /** Kategori ağırlıkları; config yüklenmediyse ya da hepsi 0 ise varsayılan. */
    public static LootAgirliklari lootAgirliklari() {
        try {
            BalikcilikAyarlari c = s();
            return new LootAgirliklari(c.agirlikYaygin.get(), c.agirlikOrta.get(), c.agirlikNadir.get(), c.agirlikCop.get(),
                    c.agirlikHazine.get(), c.sansCopAzalmasi.get(), c.sansHazineArtisi.get());
        } catch (IllegalStateException | IllegalArgumentException yok) {
            return LootAgirliklari.VARSAYILAN;
        }
    }

    public static HareketsizlikSogumasi.Ayarlar sogutma() {
        BalikcilikAyarlari c = s();
        return new HareketsizlikSogumasi.Ayarlar(c.sogutmaEnCokC.get(), c.sogutmaTamSureSn.get());
    }

    /** Yeme anındaki sıcaklık etkisi ({@code VucutSicakligi.etkiEkle} argümanları). */
    public record YemekEtkisi(double anlik, int sureTick, double toplam) {
    }

    public static YemekEtkisi yemekEtkisi(BuzBaligiEsyasi.Tur tur) {
        BalikcilikAyarlari c = s();
        return switch (tur) {
            case CIG -> new YemekEtkisi(c.cigAnlik.get(), c.cigSureSn.get() * 20, c.cigToplam.get());
            case PISMIS -> new YemekEtkisi(c.pismisAnlik.get(), c.pismisSureSn.get() * 20, c.pismisToplam.get());
            case PISMIS_RINGA -> new YemekEtkisi(c.ringaAnlik.get(), c.ringaSureSn.get() * 20, c.ringaToplam.get());
        };
    }
}
