package io.github.kadirberkpolat1.dogalafetler.uyari;

import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Uyarı ve ses ayarları (madde 5, N5, N17). CLIENT {@code uyari}: oyuncunun siren düzeyi ve ortam seslerinin tavanı. SERVER
 * {@code uyari}: sirenin fade-out süreleri (afet asıl etkisine geçince sunucu durdurma paketinde gönderir). Sınıf istemciye
 * özel API kullanmaz; iki bölüm de her iki tarafta kaydedilir.
 */
public final class UyariAyarlari {
    public static final int VARSAYILAN_SIREN_SES = 28;

    private static AyarKaydi.Bolum<Istemci> istemci;
    private static AyarKaydi.Bolum<Sunucu> sunucu;

    private UyariAyarlari() {
    }

    public static void kaydet() {
        istemci = AyarKaydi.istemciBolumu("uyari", "Uyarı ve ses (madde 5, N5)", Istemci::new);
        sunucu = AyarKaydi.sunucuBolumu("uyari", "Uyarı ve siren (madde 5)", Sunucu::new);
    }

    public static ForgeConfigSpec.IntValue sirenSes() {
        return istemci.get().sirenSes;
    }

    public static int ortamTavan() {
        return istemci.get().ortamTavan.get();
    }

    public static int fadeOutTick(SirenTuru tur) {
        Sunucu s = sunucu.get();
        return (tur == SirenTuru.GUNES ? s.gunesFadeOutTick : s.standartFadeOutTick).get();
    }

    private static final class Istemci {
        private final ForgeConfigSpec.IntValue sirenSes;
        private final ForgeConfigSpec.IntValue ortamTavan;

        private Istemci(ForgeConfigSpec.Builder k) {
            sirenSes = k.comment("Siren ses seviyesi (0-100). Ana Ses ile çarpılır; Müzik ve Sesler ekranındaki \"Sirenler\"",
                    "kaydırıcısı bu değeri değiştirir. Varsayılan 28: sesli sohbette konuşma duyulsun diye kısık (madde 5).")
                    .defineInRange("sirenSes", VARSAYILAN_SIREN_SES, 0, 100);
            ortamTavan = k.comment("Afet ortam seslerinin (uğultu, gürleme) tavanı (0-100, 0 = kapalı). Vanilla kategori",
                    "kaydırıcısının (Hava, Bloklar, Ortam) üstüne çarpılır. Varsayılan 40: ortam sesi sirenden ~3 dB alçak kalır,",
                    "konuşmayı bastırmaz (madde 5).")
                    .defineInRange("ortamTavan", 40, 0, 100);
        }
    }

    private static final class Sunucu {
        private final ForgeConfigSpec.IntValue standartFadeOutTick;
        private final ForgeConfigSpec.IntValue gunesFadeOutTick;

        private Sunucu(ForgeConfigSpec.Builder k) {
            standartFadeOutTick = k.comment("Standart sirenin fade-out süresi (tick, 20 = 1 sn); afet aktif aşamaya geçince.")
                    .defineInRange("standartFadeOutTick", SirenProfili.STANDART.sondurmeTick(), 1, 400);
            gunesFadeOutTick = k.comment("Güneş patlaması sireninin fade-out süresi (tick); meteorlar başlayınca.")
                    .defineInRange("gunesFadeOutTick", SirenProfili.GUNES.sondurmeTick(), 1, 400);
        }
    }
}
