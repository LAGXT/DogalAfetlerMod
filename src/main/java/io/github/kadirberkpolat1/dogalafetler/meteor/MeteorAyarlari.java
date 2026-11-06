package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZamani;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import net.minecraftforge.common.ForgeConfigSpec;

/** {@code meteor} bölümleri: oynanış SERVER, görsel CLIENT (madde 13c, N13, N17). */
public final class MeteorAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;
    private static AyarKaydi.Bolum<Istemci> istemci;

    private MeteorAyarlari() {
    }

    static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("meteor", "Meteor, krater ve meteor ödülleri (madde 13c, bölüm D/H)", Sunucu::new);
        istemci = AyarKaydi.istemciBolumu("meteor", "Meteor efektleri (madde 13c, N13)", Istemci::new);
    }

    public static OdulZari.Sanslar sanslar() {
        Sunucu s = sunucu.get();
        return new OdulZari.Sanslar(s.parcaSansi.get(), s.buyukParcaSansi.get(), s.cekirdekSansi.get());
    }

    public static int sicakSaniye() {
        return sunucu.get().sicakSaniye.get();
    }

    public static int odulOmruSaniye() {
        return sunucu.get().odulOmruSaniye.get();
    }

    public static int yanmaSaniye() {
        return sunucu.get().yanmaSaniye.get();
    }

    public static double kraterYaricapi(MeteorBoyutu b) {
        return (b.buyuk() ? sunucu.get().buyukKraterYaricapi : sunucu.get().kucukKraterYaricapi).get();
    }

    public static double kraterDerinligi(MeteorBoyutu b) {
        return (b.buyuk() ? sunucu.get().buyukKraterDerinligi : sunucu.get().kucukKraterDerinligi).get();
    }

    public static int kraterBlokButcesi() {
        return sunucu.get().kraterBlokButcesi.get();
    }

    public static int varlikTavani() {
        return sunucu.get().varlikTavani.get();
    }

    public static int dagitimIcYaricap() {
        return sunucu.get().dagitimIcYaricap.get();
    }

    public static int dagitimDisYaricap() {
        return sunucu.get().dagitimDisYaricap.get();
    }

    public static int girisYuksekligi() {
        return sunucu.get().girisYuksekligi.get();
    }

    public static double dusmeHizi() {
        return sunucu.get().dusmeHizi.get();
    }

    /** Görsel otomasyonun yavaş çekimi: düşüş hızını çalışan config'te değiştirir (yalnız sunucu thread'i). */
    public static void dusmeHiziAyarla(double hiz) {
        sunucu.get().dusmeHizi.set(hiz);
    }

    public static int girisAcisiEnAz() {
        return sunucu.get().girisAcisiEnAz.get();
    }

    public static int girisAcisiEnCok() {
        return sunucu.get().girisAcisiEnCok.get();
    }

    public static int sarsintiMenzili(MeteorBoyutu b) {
        return (b.buyuk() ? sunucu.get().sarsintiMenziliBuyuk : sunucu.get().sarsintiMenziliKucuk).get();
    }

    public static double yanginYogunlugu(MeteorBoyutu b) {
        return (b.buyuk() ? sunucu.get().buyukYanginYogunlugu : sunucu.get().kucukYanginYogunlugu).get();
    }

    public static int yanginYaricapEki() {
        return sunucu.get().yanginYaricapEki.get();
    }

    public static double kraterSicakligi(MeteorBoyutu b) {
        return (b.buyuk() ? sunucu.get().buyukKraterSicakligi : sunucu.get().kucukKraterSicakligi).get();
    }

    public static int kraterSicaklikSaniye() {
        return sunucu.get().kraterSicaklikSaniye.get();
    }

    public static double yagmurBuyukOrani() {
        return sunucu.get().yagmurBuyukOrani.get();
    }

    public static double izYogunlugu() {
        return istemci.get().izYogunlugu.get();
    }

    public static double sarsintiOlcegi() {
        return istemci.get().sarsintiOlcegi.get();
    }

    public static double parlamaOlcegi() {
        return istemci.get().parlamaOlcegi.get();
    }

    private static final class Sunucu {
        private final ForgeConfigSpec.DoubleValue parcaSansi;
        private final ForgeConfigSpec.DoubleValue buyukParcaSansi;
        private final ForgeConfigSpec.DoubleValue cekirdekSansi;
        private final ForgeConfigSpec.IntValue sicakSaniye;
        private final ForgeConfigSpec.IntValue odulOmruSaniye;
        private final ForgeConfigSpec.IntValue yanmaSaniye;
        private final ForgeConfigSpec.DoubleValue kucukKraterYaricapi;
        private final ForgeConfigSpec.DoubleValue kucukKraterDerinligi;
        private final ForgeConfigSpec.DoubleValue buyukKraterYaricapi;
        private final ForgeConfigSpec.DoubleValue buyukKraterDerinligi;
        private final ForgeConfigSpec.IntValue kraterBlokButcesi;
        private final ForgeConfigSpec.IntValue varlikTavani;
        private final ForgeConfigSpec.IntValue dagitimIcYaricap;
        private final ForgeConfigSpec.IntValue dagitimDisYaricap;
        private final ForgeConfigSpec.IntValue girisYuksekligi;
        private final ForgeConfigSpec.DoubleValue dusmeHizi;
        private final ForgeConfigSpec.IntValue girisAcisiEnAz;
        private final ForgeConfigSpec.IntValue girisAcisiEnCok;
        private final ForgeConfigSpec.IntValue sarsintiMenziliKucuk;
        private final ForgeConfigSpec.IntValue sarsintiMenziliBuyuk;
        private final ForgeConfigSpec.DoubleValue kucukYanginYogunlugu;
        private final ForgeConfigSpec.DoubleValue buyukYanginYogunlugu;
        private final ForgeConfigSpec.IntValue yanginYaricapEki;
        private final ForgeConfigSpec.DoubleValue kucukKraterSicakligi;
        private final ForgeConfigSpec.DoubleValue buyukKraterSicakligi;
        private final ForgeConfigSpec.IntValue kraterSicaklikSaniye;
        private final ForgeConfigSpec.DoubleValue yagmurBuyukOrani;

        private Sunucu(ForgeConfigSpec.Builder k) {
            OdulZari.Sanslar v = OdulZari.Sanslar.VARSAYILAN;
            parcaSansi = k.comment("Küçük meteorun kraterinde Meteorit Parçası çıkma olasılığı (madde 13c: %3).")
                    .defineInRange("parcaSansi", v.parca(), 0.0, 1.0);
            buyukParcaSansi = k.comment("Büyük meteorun kraterinde Meteorit Parçası olasılığı (madde 13c: %5).")
                    .defineInRange("buyukParcaSansi", v.buyukParca(), 0.0, 1.0);
            cekirdekSansi = k.comment("Büyük meteorun krater ortasında Kozmik Çekirdek olasılığı (madde 13c: %0,3; küçükte hiç).")
                    .defineInRange("cekirdekSansi", v.cekirdek(), 0.0, 1.0);
            sicakSaniye = k.comment("Meteorit Parçası çarpmadan sonra bu kadar saniye sıcaktır: alan/yanına gelen oyuncu yanar; su ya da kar üstünde hemen soğur.")
                    .defineInRange("sicakSaniye", OdulZamani.SICAK_SANIYE, 0, 300);
            odulOmruSaniye = k.comment("Meteor ödülü yerde bu kadar saniye sonra yok olur (madde 13c: 10 dk).")
                    .defineInRange("odulOmruSaniye", OdulZamani.OMUR_SANIYE, 30, 3600);
            yanmaSaniye = k.comment("Sıcak parçaya dokunan oyuncunun yanma süresi (sn).")
                    .defineInRange("yanmaSaniye", 3, 0, 20);
            kucukKraterYaricapi = k.comment("Küçük meteor krater yarıçapı (blok).").defineInRange("kucukKraterYaricapi", 2.5, 0.0, 8.0);
            kucukKraterDerinligi = k.comment("Küçük meteor krater derinliği (merkezde, yüzey dahil blok).")
                    .defineInRange("kucukKraterDerinligi", 2.0, 0.0, 8.0);
            buyukKraterYaricapi = k.comment("Büyük meteor krater yarıçapı (blok).").defineInRange("buyukKraterYaricapi", 5.5, 0.0, 12.0);
            buyukKraterDerinligi = k.comment("Büyük meteor krater derinliği (merkezde, yüzey dahil blok).")
                    .defineInRange("buyukKraterDerinligi", 4.0, 0.0, 10.0);
            kraterBlokButcesi = k.comment("Tick başına krater blok işlemi (bütün kraterler); TPS kısmasıyla çarpılır.")
                    .defineInRange("kraterBlokButcesi", 512, 16, 8192);
            varlikTavani = k.comment("Boyut başına aynı anda uçan en çok meteor; aşılınca yeni meteor atılmaz.")
                    .defineInRange("varlikTavani", 48, 1, 256);
            dagitimIcYaricap = k.comment("Yağmurda meteor oyuncunun en az bu kadar uzağına düşer (blok).")
                    .defineInRange("dagitimIcYaricap", 32, 0, 256);
            dagitimDisYaricap = k.comment("Yağmurda meteor oyuncunun en çok bu kadar uzağına düşer (blok).")
                    .defineInRange("dagitimDisYaricap", 160, 8, 512);
            girisYuksekligi = k.comment("Meteor hedefin bu kadar blok üstünden başlar.").defineInRange("girisYuksekligi", 140, 40, 300);
            dusmeHizi = k.comment("Düşüş hızı (blok/tick; eksen başına paket sınırı 3,9).").defineInRange("dusmeHizi", 3.0, 0.5, 3.8);
            girisAcisiEnAz = k.comment("Giriş açısı alt sınırı (derece, yatayla).").defineInRange("girisAcisiEnAz", 40, 15, 90);
            girisAcisiEnCok = k.comment("Giriş açısı üst sınırı (derece).").defineInRange("girisAcisiEnCok", 70, 15, 90);
            sarsintiMenziliKucuk = k.comment("Küçük çarpmanın ekran sarsıntısı menzili (blok).")
                    .defineInRange("sarsintiMenziliKucuk", 48, 0, 256);
            sarsintiMenziliBuyuk = k.comment("Büyük çarpmanın ekran sarsıntısı menzili (blok).")
                    .defineInRange("sarsintiMenziliBuyuk", 112, 0, 512);
            kucukYanginYogunlugu = k.comment("Küçük meteor ateşinin ortak yangın yoğunluğu (0–1).")
                    .defineInRange("kucukYanginYogunlugu", 0.45, 0.0, 1.0);
            buyukYanginYogunlugu = k.comment("Büyük meteor ateşinin ortak yangın yoğunluğu (0–1).")
                    .defineInRange("buyukYanginYogunlugu", 0.85, 0.0, 1.0);
            yanginYaricapEki = k.comment("Yangın alanı = krater yarıçapı + bu (blok).").defineInRange("yanginYaricapEki", 3, 0, 12);
            kucukKraterSicakligi = k.comment("Küçük kraterin ortam sıcaklığı katkısı (°C, merkezde).")
                    .defineInRange("kucukKraterSicakligi", 14.0, 0.0, 45.0);
            buyukKraterSicakligi = k.comment("Büyük kraterin ortam sıcaklığı katkısı (°C, merkezde).")
                    .defineInRange("buyukKraterSicakligi", 24.0, 0.0, 45.0);
            kraterSicaklikSaniye = k.comment("Krater bu kadar saniye sıcak kalır (ortam kaynağı).")
                    .defineInRange("kraterSicaklikSaniye", 30, 0, 600);
            yagmurBuyukOrani = k.comment("/dogalafet meteor yagmur komutunda büyük meteor oranı (tasarım: %5).")
                    .defineInRange("yagmurBuyukOrani", 0.05, 0.0, 1.0);
        }
    }

    private static final class Istemci {
        private final ForgeConfigSpec.DoubleValue izYogunlugu;
        private final ForgeConfigSpec.DoubleValue sarsintiOlcegi;
        private final ForgeConfigSpec.DoubleValue parlamaOlcegi;

        private Istemci(ForgeConfigSpec.Builder k) {
            izYogunlugu = k.comment("Meteor izi ve çarpma parçacık yoğunluğu çarpanı (efekt kalitesiyle ayrıca çarpılır).")
                    .defineInRange("izYogunlugu", 1.0, 0.0, 2.0);
            sarsintiOlcegi = k.comment("Çarpma ekran sarsıntısı ölçeği (0 kapalı).").defineInRange("sarsintiOlcegi", 1.0, 0.0, 2.0);
            parlamaOlcegi = k.comment("Çarpmanın beyaz parlaması ölçeği (0 kapalı).").defineInRange("parlamaOlcegi", 1.0, 0.0, 1.0);
        }
    }
}
