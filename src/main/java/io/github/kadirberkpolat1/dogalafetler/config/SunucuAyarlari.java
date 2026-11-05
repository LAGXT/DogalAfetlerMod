package io.github.kadirberkpolat1.dogalafetler.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** SERVER config'teki {@code sicaklik} bölümünün çekirdek tarafından açılan anahtarı (K0-08, N8). */
public final class SunucuAyarlari {
    private static AyarKaydi.Bolum<Sicaklik> sicaklik;

    private SunucuAyarlari() {
    }

    public static void kaydet() {
        sicaklik = AyarKaydi.sunucuBolumu("sicaklik", "Sıcaklık sistemi (madde 7)", Sicaklik::new);
    }

    public static SicaklikSistemiModu sicaklikSistemiModu() {
        return sicaklik.get().modu.get();
    }

    private static final class Sicaklik {
        private final ForgeConfigSpec.EnumValue<SicaklikSistemiModu> modu;

        private Sicaklik(ForgeConfigSpec.Builder kurucu) {
            modu = kurucu
                    .comment("OTOMATIK: Cold Sweat yüklüyse kendi sıcaklık sistemimiz kapalı kalır (çift hasar olmaz).",
                            "ACIK: her zaman açık. KAPALI: her zaman kapalı.",
                            "Sistem ayrıca sicaklikSistemi gamerule'ü açıkken çalışır (N8).")
                    .defineEnum("sicaklikSistemiModu", SicaklikSistemiModu.OTOMATIK);
        }
    }
}
