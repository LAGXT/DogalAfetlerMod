package io.github.kadirberkpolat1.dogalafetler.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CLIENT config ({@code config/dogalafetler-client.toml}): görsel, ses ve HUD ayarları. Yalnız istemcide yüklenir;
 * sınıfın kendisi istemciye özel API kullanmadığı için sunucuda da güvenle kaydedilir.
 */
public final class IstemciAyarlari {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.EnumValue<EfektKalitesi> EFEKT_KALITESI;
    public static final ForgeConfigSpec.IntValue SIREN_SES;

    static {
        ForgeConfigSpec.Builder kurucu = new ForgeConfigSpec.Builder();

        kurucu.comment("Görsel ayarlar (madde 1)").push("gorsel");
        EFEKT_KALITESI = kurucu
                .comment("Efekt kalitesi: DUSUK (düşük efekt modu), ORTA, YUKSEK (varsayılan kalite modu), ULTRA.")
                .defineEnum("efektKalitesi", EfektKalitesi.YUKSEK);
        kurucu.pop();

        kurucu.comment("Ses ayarları (madde 5, N5)").push("ses");
        SIREN_SES = kurucu
                .comment("Siren ses seviyesi (0-100). Ana Ses ile çarpılır; Müzik ve Sesler ekranındaki \"Sirenler\" kaydırıcısı",
                        "bu değeri değiştirir. Varsayılan 28: sesli sohbette konuşma duyulsun diye kısık (madde 5).")
                .defineInRange("sirenSes", 28, 0, 100);
        kurucu.pop();

        SPEC = kurucu.build();
    }

    private IstemciAyarlari() {
    }
}
