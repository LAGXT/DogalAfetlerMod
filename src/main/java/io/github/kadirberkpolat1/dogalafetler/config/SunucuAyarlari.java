package io.github.kadirberkpolat1.dogalafetler.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** SERVER config ({@code <dünya>/serverconfig/dogalafetler-server.toml}); istemcilere senkronlanır. */
public final class SunucuAyarlari {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.EnumValue<SicaklikSistemiModu> SICAKLIK_SISTEMI_MODU;

    static {
        ForgeConfigSpec.Builder kurucu = new ForgeConfigSpec.Builder();

        kurucu.comment("Sıcaklık sistemi (madde 7)").push("sicaklik");
        SICAKLIK_SISTEMI_MODU = kurucu
                .comment("OTOMATIK: Cold Sweat yüklüyse kendi sıcaklık sistemimiz kapalı kalır (çift hasar olmaz).",
                        "ACIK: her zaman açık. KAPALI: her zaman kapalı.",
                        "Sistem ayrıca sicaklikSistemi gamerule'ü açıkken çalışır (N8).")
                .defineEnum("sicaklikSistemiModu", SicaklikSistemiModu.OTOMATIK);
        kurucu.pop();

        SPEC = kurucu.build();
    }

    private SunucuAyarlari() {
    }
}
