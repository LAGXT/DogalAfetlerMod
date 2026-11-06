package io.github.kadirberkpolat1.dogalafetler.afet.kasirga;

import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.Carpanlar;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaKaliteButcesi;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/** {@code kasirga} bölümleri: oynanış SERVER, görsel CLIENT (madde 1, 10, N13, N17; spike K0-33). */
public final class KasirgaAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;
    private static AyarKaydi.Bolum<Istemci> istemci;

    private KasirgaAyarlari() {
    }

    static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("kasirga", "Kasırga (bölüm A, madde 10)", Sunucu::new);
        istemci = AyarKaydi.istemciBolumu("kasirga", "Kasırga efektleri (madde 1, N13)", Istemci::new);
    }

    /** Kasırga başına aynı anda havada en çok gerçek uçan blok (K0-33: ≤ 40). */
    public static int ucanBlokTavani() {
        return sunucu.get().ucanBlokTavani.get();
    }

    /** Kaldırılan oyuncu ve mob kasırga zemininden en çok bu kadar yükselir. */
    public static double kaldirmaTavani() {
        return sunucu.get().kaldirmaTavani.get();
    }

    public static Carpanlar carpanlar() {
        Sunucu s = sunucu.get();
        return new Carpanlar(s.boyut.get(), s.hiz.get(), s.sure.get(), s.cekim.get(), s.blok.get(), s.hasar.get());
    }

    public static KasirgaKaliteButcesi.Lod lod() {
        Istemci i = istemci.get();
        double taban = i.lodTabanYari.get();
        double enkaz = Math.max(taban, i.lodEnkazYok.get());
        return new KasirgaKaliteButcesi.Lod(Math.min(taban, i.lodYakin.get()), taban, enkaz,
                Math.max(enkaz, i.lodYalnizHuni.get()));
    }

    /** İstemcide aynı anda kasırga başına en çok yalnız görsel enkaz kopyası. */
    public static int gorselKopyaTavani() {
        return istemci.get().gorselKopyaTavani.get();
    }

    public static double kameraSarsintisi() {
        return istemci.get().kameraSarsintisi.get();
    }

    public static boolean sisEtkisi() {
        return istemci.get().sisEtkisi.get();
    }

    private static final class Sunucu {
        private final ForgeConfigSpec.IntValue ucanBlokTavani;
        private final ForgeConfigSpec.DoubleValue kaldirmaTavani;
        private final ForgeConfigSpec.DoubleValue boyut;
        private final ForgeConfigSpec.DoubleValue hiz;
        private final ForgeConfigSpec.DoubleValue sure;
        private final ForgeConfigSpec.DoubleValue cekim;
        private final ForgeConfigSpec.DoubleValue blok;
        private final ForgeConfigSpec.DoubleValue hasar;

        private Sunucu(ForgeConfigSpec.Builder k) {
            ucanBlokTavani = k.comment("Kasırga başına aynı anda havada en çok gerçek uçan blok (K0-33: en çok 40). Tavan doluyken",
                    "koparılan blok parçalanır: tick başına denemenin en çok dörtte biri, yalnız 1/16'sı eşya düşürür.")
                    .defineInRange("ucanBlokTavani", 40, 0, 40);
            kaldirmaTavani = k.comment("Kaldırılan oyuncu ve mob kasırga zemininden en çok bu kadar yükselir (blok); düşme hasarı vanilla.")
                    .defineInRange("kaldirmaTavani", 25.0, 0.0, 64.0);
            boyut = k.comment("Seviye tablosu çarpanı: huni çapı/yüksekliği, etek ve çekim yarıçapı (madde 10).")
                    .defineInRange("boyutCarpani", 1.0, 0.25, 3.0);
            hiz = k.comment("Seviye tablosu çarpanı: hareket hızı (S1 2,0 → S5 4,4 blok/sn).").defineInRange("hizCarpani", 1.0, 0.0, 4.0);
            sure = k.comment("Seviye tablosu çarpanı: olgun evre süresi (S1 60 → S5 180 sn).").defineInRange("sureCarpani", 1.0, 0.1, 10.0);
            cekim = k.comment("Seviye tablosu çarpanı: oyuncu, mob ve eşyalara uygulanan çekim/kaldırma kuvveti (0 kapalı).")
                    .defineInRange("cekimCarpani", 1.0, 0.0, 4.0);
            blok = k.comment("Seviye tablosu çarpanı: tick başına blok kaldırma (S1 2 → S5 20; TPS kısmasıyla ayrıca çarpılır).")
                    .defineInRange("blokCarpani", 1.0, 0.0, 4.0);
            hasar = k.comment("Seviye tablosu çarpanı: huni çekirdeğinin hasarı (S1 1 → S5 4 can/sn) ve enkaz çarpması.")
                    .defineInRange("hasarCarpani", 1.0, 0.0, 10.0);
        }
    }

    private static final class Istemci {
        private final ForgeConfigSpec.DoubleValue lodYakin;
        private final ForgeConfigSpec.DoubleValue lodTabanYari;
        private final ForgeConfigSpec.DoubleValue lodEnkazYok;
        private final ForgeConfigSpec.DoubleValue lodYalnizHuni;
        private final ForgeConfigSpec.IntValue gorselKopyaTavani;
        private final ForgeConfigSpec.DoubleValue kameraSarsintisi;
        private final ForgeConfigSpec.BooleanValue sisEtkisi;

        private Istemci(ForgeConfigSpec.Builder k) {
            lodYakin = k.comment("Kamera kasırga merkezine bu mesafeden (blok) yakınken taban toz sprite'ları dörtte bire iner ve",
                    "sprite'ların ekran kaplama sınırı yarıya düşer (K0-33 yakın kare süresi); 0 kapatır.")
                    .defineInRange("lodYakin", 48.0, 0.0, 512.0);
            lodTabanYari = k.comment("Bu mesafeden (blok) sonra taban toz sprite'ları yarıya iner; içeride seçili kalitenin tamamı.")
                    .defineInRange("lodTabanYari", 96.0, 16.0, 512.0);
            lodEnkazYok = k.comment("Bu mesafeden sonra yalnız görsel enkaz kopyaları üretilmez (gerçek uçan bloklar görünür).")
                    .defineInRange("lodEnkazYok", 160.0, 16.0, 512.0);
            lodYalnizHuni = k.comment("Bu mesafeden sonra yalnız huni ve duvar bulutu çizilir.")
                    .defineInRange("lodYalnizHuni", 256.0, 16.0, 1024.0);
            gorselKopyaTavani = k.comment("Kasırga başına aynı anda en çok yalnız görsel enkaz kopyası (spike: 240).")
                    .defineInRange("gorselKopyaTavani", 240, 0, 1000);
            kameraSarsintisi = k.comment("Kasırgaya 40 bloktan yakınken kamera sarsıntısı ölçeği (0 kapalı).")
                    .defineInRange("kameraSarsintisi", 1.0, 0.0, 2.0);
            sisEtkisi = k.comment("Kasırgaya 64 bloktan yakınken yeşilimsi gri sis ve kısalan görüş.").define("sisEtkisi", true);
        }
    }
}
