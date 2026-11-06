package io.github.kadirberkpolat1.dogalafetler.su;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** Sanal su config'i (madde 8, bölüm B ve H, N17): SERVER {@code su} oynanış, CLIENT {@code su} görsel değerleri. */
public final class SuAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;
    private static AyarKaydi.Bolum<Istemci> istemci;

    private SuAyarlari() {
    }

    public static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("su", "Ortak sanal su (madde 8, bölüm B ve H)", Sunucu::new);
        istemci = AyarKaydi.istemciBolumu("su", "Sanal su görünümü (madde 1, 8)", Istemci::new);
    }

    public static Sunucu sunucu() {
        return sunucu.get();
    }

    public static Istemci istemci() {
        return istemci.get();
    }

    public static final class Sunucu {
        public final ForgeConfigSpec.DoubleValue aysonrasiBeklemeDk;
        public final ForgeConfigSpec.DoubleValue denizHedefArtis;
        public final ForgeConfigSpec.IntValue denizUstSinir;
        public final ForgeConfigSpec.DoubleValue denizYukselmeDk;
        public final ForgeConfigSpec.BooleanValue denizKalici;
        public final ForgeConfigSpec.DoubleValue denizGeriDonusBeklemeDk;
        public final ForgeConfigSpec.DoubleValue denizGeriDonusDk;
        public final ForgeConfigSpec.IntValue magaraKalisSn;
        public final ForgeConfigSpec.DoubleValue magaraDolmaHizi;
        public final ForgeConfigSpec.DoubleValue magaraKurumaBlok;
        public final ForgeConfigSpec.DoubleValue magaraKurumaAraligiSn;
        public final ForgeConfigSpec.IntValue magaraMaksHacim;
        public final ForgeConfigSpec.IntValue magaraMaksKesif;
        public final ForgeConfigSpec.IntValue alanOkumaButcesi;
        public final ForgeConfigSpec.IntValue tazelemeUzakButce;

        private Sunucu(ForgeConfigSpec.Builder k) {
            aysonrasiBeklemeDk = k.comment("Ay patladıktan sonra deniz yükselmesine kadar gerçek dakika (bölüm H).")
                    .defineInRange("aysonrasiBeklemeDk", 10.0, 0.0, 1440.0);
            denizHedefArtis = k.comment("Deniz seviyesinin toplam artışı (blok); üst sınıra kırpılır.")
                    .defineInRange("denizHedefArtis", 4.0, 0.0, 8.0);
            denizUstSinir = k.comment("Deniz artışının üst sınırı (blok, en çok 8); komut da buna kırpılır.")
                    .defineInRange("denizUstSinir", 6, 1, 8);
            denizYukselmeDk = k.comment("Yükselmenin süresi (gerçek dakika, kademeli ve doğrusal).")
                    .defineInRange("denizYukselmeDk", 5.0, 0.1, 240.0);
            denizKalici = k.comment("Yükselmiş seviye kalıcı mı; false ise bekleyip yavaşça eski hâline döner.")
                    .define("denizKalici", true);
            denizGeriDonusBeklemeDk = k.comment("Kalıcı değilse zirvede bekleme (gerçek dakika).")
                    .defineInRange("denizGeriDonusBeklemeDk", 60.0, 0.0, 100000.0);
            denizGeriDonusDk = k.comment("Kalıcı değilse eski hâline dönüş süresi (gerçek dakika).")
                    .defineInRange("denizGeriDonusDk", 5.0, 0.1, 240.0);
            magaraKalisSn = k.comment("Mağaraya dolan suyun kuruma başlamadan önce kalış süresi (sn).")
                    .defineInRange("magaraKalisSn", 600, 0, 86400);
            magaraDolmaHizi = k.comment("Mağara suyu yüzeyinin dolarken yükselme hızı (blok/sn).")
                    .defineInRange("magaraDolmaHizi", 2.0, 0.05, 50.0);
            magaraKurumaBlok = k.comment("Kururken her adımda inilen blok.").defineInRange("magaraKurumaBlok", 0.25, 0.01, 4.0);
            magaraKurumaAraligiSn = k.comment("Kuruma adımı (sn); hız = blok / aralık.")
                    .defineInRange("magaraKurumaAraligiSn", 6.0, 0.1, 600.0);
            magaraMaksHacim = k.comment("Bir mağara cebinin en çok hücresi.").defineInRange("magaraMaksHacim", 4000, 1, 20000);
            magaraMaksKesif = k.comment("Mağara keşfinin en çok hücresi (aşılırsa en alçak sızıntı seviyesi).")
                    .defineInRange("magaraMaksKesif", 20000, 100, 100000);
            alanOkumaButcesi = k.comment("Deniz alanı: 10 tick'te okunan en çok chunk; tick'lere yayılır, her 2 oyuncuda bir katlanır, TPS kısmasıyla çarpılır.")
                    .defineInRange("alanOkumaButcesi", 32, 1, 256);
            tazelemeUzakButce = k.comment("Deniz alanı tazelemesi: saniyede yeniden okunan uzak chunk.")
                    .defineInRange("tazelemeUzakButce", 16, 0, 256);
        }

        /** Kuruma hızı (blok / ölçekli tick). */
        public double kurumaHizi() {
            return magaraKurumaBlok.get() / (magaraKurumaAraligiSn.get() * 20.0);
        }
    }

    public static final class Istemci {
        public final ForgeConfigSpec.IntValue lodMesafeChunk;
        public final ForgeConfigSpec.DoubleValue parcacikYogunlugu;
        public final ForgeConfigSpec.IntValue okumaButcesi;
        public final ForgeConfigSpec.DoubleValue yakinKurulumButcesiMs;

        private Istemci(ForgeConfigSpec.Builder k) {
            lodMesafeChunk = k.comment("Uzak (LOD) sanal su çiziminin en büyük mesafesi (chunk); görüş mesafesi ve kalite ayrıca sınırlar.")
                    .defineInRange("lodMesafeChunk", 16, 4, 32);
            parcacikYogunlugu = k.comment("Buhar ve köpük parçacığı çarpanı (kalite çarpanının üstüne).")
                    .defineInRange("parcacikYogunlugu", 1.0, 0.0, 4.0);
            okumaButcesi = k.comment("İstemci deniz alanı: 10 tick'te okunan en çok chunk (tick'lere yayılır).")
                    .defineInRange("okumaButcesi", 64, 1, 512);
            yakinKurulumButcesiMs = k.comment("Yakın su halkası kurulumunun tick başına zaman bütçesi (ms); kalanı sonraki tick'e kalır.")
                    .defineInRange("yakinKurulumButcesiMs", 2.0, 0.25, 50.0);
        }
    }
}
