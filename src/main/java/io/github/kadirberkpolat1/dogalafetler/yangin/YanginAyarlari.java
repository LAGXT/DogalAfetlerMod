package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** {@code yangin} bölümleri: oynanış SERVER, görsel CLIENT (madde 6, N13, N17). */
public final class YanginAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;
    private static AyarKaydi.Bolum<Istemci> istemci;

    private YanginAyarlari() {
    }

    public static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("yangin", "Ortak yangın sistemi (madde 6)", Sunucu::new);
        istemci = AyarKaydi.istemciBolumu("yangin", "Yangın efektleri (madde 6, N13)", Istemci::new);
    }

    public static int tickButcesi() {
        return sunucu.get().tickButcesi.get();
    }

    public static int aktifTavan() {
        return sunucu.get().aktifTavan.get();
    }

    public static double yayilmaHizCarpani() {
        return sunucu.get().yayilmaHizCarpani.get();
    }

    public static boolean yagmurdaSonme() {
        return sunucu.get().yagmurdaSonme.get();
    }

    public static double yagmurSonmeOlasiligi() {
        return sunucu.get().yagmurSonmeOlasiligi.get();
    }

    public static int afetSonrasiSonumSaniye() {
        return sunucu.get().afetSonrasiSonumSaniye.get();
    }

    public static double parcacikYogunlugu() {
        return istemci.get().parcacikYogunlugu.get();
    }

    public static int lodMesafesi() {
        return istemci.get().lodMesafesi.get();
    }

    public static int sutunTavani() {
        return istemci.get().sutunTavani.get();
    }

    public static int sutunMesafesi() {
        return istemci.get().sutunMesafesi.get();
    }

    private static final class Sunucu {
        private final ForgeConfigSpec.IntValue tickButcesi;
        private final ForgeConfigSpec.IntValue aktifTavan;
        private final ForgeConfigSpec.DoubleValue yayilmaHizCarpani;
        private final ForgeConfigSpec.BooleanValue yagmurdaSonme;
        private final ForgeConfigSpec.DoubleValue yagmurSonmeOlasiligi;
        private final ForgeConfigSpec.IntValue afetSonrasiSonumSaniye;

        private Sunucu(ForgeConfigSpec.Builder k) {
            tickButcesi = k.comment("Tick başına işlenen en çok yanan hücre; TPS kısmasıyla (1 / 0,5 / 0,25) çarpılır.")
                    .defineInRange("tickButcesi", 64, 1, 4096);
            aktifTavan = k.comment("Boyut başına en çok yanan hücre; aşılınca en eski hücreler söner.")
                    .defineInRange("aktifTavan", 1500, 16, 20000);
            yayilmaHizCarpani = k.comment("Yayılma olasılığı çarpanı (vanilla yayılma hızı × bu × rüzgâr/eğim/nem). 0 yayılmayı kapatır.")
                    .defineInRange("yayilmaHizCarpani", 1.0, 0.0, 10.0);
            yagmurdaSonme = k.comment("Yağmur altındaki ateş sönebilir.").define("yagmurdaSonme", true);
            yagmurSonmeOlasiligi = k.comment("Yağmurdaki hücrenin her işlenişte sönme olasılığı tabanı (yaşla +0,3'e kadar artar).")
                    .defineInRange("yagmurSonmeOlasiligi", 0.25, 0.0, 1.0);
            afetSonrasiSonumSaniye = k.comment("Afet bitince kalan ateşin en geç sönme süresi (sn); yeni tutuşma durur.")
                    .defineInRange("afetSonrasiSonumSaniye", 30, 0, 600);
        }
    }

    private static final class Istemci {
        private final ForgeConfigSpec.DoubleValue parcacikYogunlugu;
        private final ForgeConfigSpec.IntValue lodMesafesi;
        private final ForgeConfigSpec.IntValue sutunTavani;
        private final ForgeConfigSpec.IntValue sutunMesafesi;

        private Istemci(ForgeConfigSpec.Builder k) {
            parcacikYogunlugu = k.comment("Alev ve duman parçacığı çarpanı (efekt kalitesinin üstüne).")
                    .defineInRange("parcacikYogunlugu", 1.0, 0.0, 4.0);
            lodMesafesi = k.comment("Bu mesafenin ötesinde hücre alevi yerine chunk başına duman kümesi çizilir (blok).")
                    .defineInRange("lodMesafesi", 48, 16, 256);
            sutunTavani = k.comment("Yüksek kalitede aynı anda en çok duman sütunu parçacığı (pirokümülüs dahil); Düşük ×0,25, Orta ×0,5, Ultra ×2.")
                    .defineInRange("sutunTavani", 64, 8, 1024);
            sutunMesafesi = k.comment("Bu mesafenin ötesindeki yangın için duman sütunu üretilmez (blok).")
                    .defineInRange("sutunMesafesi", 160, 32, 512);
        }
    }
}
