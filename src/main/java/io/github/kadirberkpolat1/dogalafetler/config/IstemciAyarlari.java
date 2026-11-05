package io.github.kadirberkpolat1.dogalafetler.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CLIENT config'teki {@code gorsel} bölümünün çekirdek tarafından açılan anahtarı (madde 1, N13). Sınıf istemciye özel API
 * kullanmadığı için sunucuda da güvenle kaydedilir; değer yalnız istemcide yüklenir.
 */
public final class IstemciAyarlari {
    private static AyarKaydi.Bolum<Gorsel> gorsel;

    private IstemciAyarlari() {
    }

    public static void kaydet() {
        gorsel = AyarKaydi.istemciBolumu("gorsel", "Görsel ayarlar (madde 1)", Gorsel::new);
    }

    public static EfektKalitesi efektKalitesi() {
        return gorsel.get().efektKalitesi.get();
    }

    private static final class Gorsel {
        private final ForgeConfigSpec.EnumValue<EfektKalitesi> efektKalitesi;

        private Gorsel(ForgeConfigSpec.Builder kurucu) {
            efektKalitesi = kurucu
                    .comment("Efekt kalitesi: DUSUK (düşük efekt modu), ORTA, YUKSEK (varsayılan kalite modu), ULTRA.")
                    .defineEnum("efektKalitesi", EfektKalitesi.YUKSEK);
        }
    }
}
