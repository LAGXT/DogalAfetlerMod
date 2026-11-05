package io.github.kadirberkpolat1.dogalafetler.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * CLIENT config ({@code config/dogalafetler-client.toml}): görsel, ses ve HUD ayarları. Yalnız istemcide yüklenir;
 * sınıfın kendisi istemciye özel API kullanmadığı için sunucuda da güvenle kaydedilir.
 */
public final class IstemciAyarlari {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.EnumValue<EfektKalitesi> EFEKT_KALITESI;

    static {
        ForgeConfigSpec.Builder kurucu = new ForgeConfigSpec.Builder();

        kurucu.comment("Görsel ayarlar (madde 1)").push("gorsel");
        EFEKT_KALITESI = kurucu
                .comment("Efekt kalitesi: DUSUK (düşük efekt modu), ORTA, YUKSEK (varsayılan kalite modu), ULTRA.")
                .defineEnum("efektKalitesi", EfektKalitesi.YUKSEK);
        kurucu.pop();

        SPEC = kurucu.build();
    }

    private IstemciAyarlari() {
    }
}
