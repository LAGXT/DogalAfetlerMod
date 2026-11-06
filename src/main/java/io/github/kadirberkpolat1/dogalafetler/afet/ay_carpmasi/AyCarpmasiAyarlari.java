package io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * {@code ay_carpmasi} bölümleri (bölüm H, N13, N17): oynanış SERVER (istemciye eşitlenir; enkaz halkası herkeste aynı
 * görünsün diye oradadır), ekran efektleri CLIENT. Tsunami çarpanı ve süresi çekirdeğin {@code cekirdek.tsunamiCarpani}
 * ayarlarındadır; deniz yükselmesi sanal suyun {@code su.*} ayarlarındadır.
 */
public final class AyCarpmasiAyarlari {
    private static AyarKaydi.Bolum<Sunucu> sunucu;
    private static AyarKaydi.Bolum<Istemci> istemci;

    private AyCarpmasiAyarlari() {
    }

    static void kaydet() {
        sunucu = AyarKaydi.sunucuBolumu("ay_carpmasi", "Ay çarpması (bölüm H)", Sunucu::new);
        istemci = AyarKaydi.istemciBolumu("ay_carpmasi", "Ay çarpması ekran efektleri (bölüm H, N13)", Istemci::new);
    }

    public static int meteorSuresiSn() {
        return sunucu.get().meteorSuresiSn.get();
    }

    public static double meteorDakikaBasina() {
        return sunucu.get().meteorDakikaBasina.get();
    }

    /**
     * Çalışan config'te yağmur sıklığını değiştirir (yalnız sunucu thread'i): GameTest sahnesine (oyuncusuz dünyada
     * doğuş noktasına) meteor düşmesin diye 0 yapar, önceki değeri döner.
     */
    static double meteorDakikaBasinaAyarla(double deger) {
        double onceki = meteorDakikaBasina();
        sunucu.get().meteorDakikaBasina.set(deger);
        return onceki;
    }

    public static double meteorBuyukOrani() {
        return sunucu.get().meteorBuyukOrani.get();
    }

    public static int sarsintiBlokSayisi() {
        return sunucu.get().sarsintiBlokSayisi.get();
    }

    public static int sarsintiYaricapi() {
        return sunucu.get().sarsintiYaricapi.get();
    }

    public static double sarsintiYogunlugu() {
        return sunucu.get().sarsintiYogunlugu.get();
    }

    public static boolean halkaAcik() {
        return sunucu.get().halka.get();
    }

    public static double ekranSarsintisiOlcegi() {
        return istemci.get().ekranSarsintisi.get();
    }

    public static double parlamaOlcegi() {
        return istemci.get().parlama.get();
    }

    private static final class Sunucu {
        private final ForgeConfigSpec.IntValue meteorSuresiSn;
        private final ForgeConfigSpec.DoubleValue meteorDakikaBasina;
        private final ForgeConfigSpec.DoubleValue meteorBuyukOrani;
        private final ForgeConfigSpec.IntValue sarsintiBlokSayisi;
        private final ForgeConfigSpec.IntValue sarsintiYaricapi;
        private final ForgeConfigSpec.DoubleValue sarsintiYogunlugu;
        private final ForgeConfigSpec.BooleanValue halka;

        private Sunucu(ForgeConfigSpec.Builder k) {
            meteorSuresiSn = k.comment("Çarpmadan 10 sn sonra başlayan ay parçası yağmurunun süresi (sn; bölüm H: 1-2 dk).")
                    .defineInRange("meteorSuresiSn", 90, 60, 120);
            meteorDakikaBasina = k.comment("Oyuncu başına dakikada düşen ay parçası (7,5 = 8 sn'de bir).")
                    .defineInRange("meteorDakikaBasina", 7.5, 0.0, 60.0);
            meteorBuyukOrani = k.comment("Ay parçası yağmurunda büyük meteor olasılığı.")
                    .defineInRange("meteorBuyukOrani", 0.04, 0.0, 1.0);
            sarsintiBlokSayisi = k.comment("Çarpma sarsıntısında (12 sn) oyuncu başına en çok düşen blok "
                    + "(#dogalafetler:ay_sarsintisi_duser; afetBlokHasari kapalıyken hiç).")
                    .defineInRange("sarsintiBlokSayisi", 35, 0, 200);
            sarsintiYaricapi = k.comment("Düşen blokların oyuncudan en çok uzaklığı (blok).")
                    .defineInRange("sarsintiYaricapi", 48, 8, 128);
            sarsintiYogunlugu = k.comment("Bulunan desteksiz bloğun düşme olasılığı (yoğunluk).")
                    .defineInRange("sarsintiYogunlugu", 0.30, 0.0, 1.0);
            halka = k.comment("Çarpmadan sonra 3 gün gökte enkaz halkası ve kayan yıldızlar görünsün.")
                    .define("halka", true);
        }
    }

    private static final class Istemci {
        private final ForgeConfigSpec.DoubleValue ekranSarsintisi;
        private final ForgeConfigSpec.DoubleValue parlama;

        private Istemci(ForgeConfigSpec.Builder k) {
            ekranSarsintisi = k.comment("Çarpma sarsıntısının kamera genliği çarpanı (0 kapalı).")
                    .defineInRange("ekranSarsintisi", 1.0, 0.0, 2.0);
            parlama = k.comment("Çarpma parlamasının ekran beyazlığı çarpanı (0 kapalı).")
                    .defineInRange("parlama", 1.0, 0.0, 1.0);
        }
    }
}
