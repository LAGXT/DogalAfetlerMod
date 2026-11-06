package io.github.kadirberkpolat1.dogalafetler.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.config.SicaklikSistemiModu;
import io.github.kadirberkpolat1.dogalafetler.config.SunucuAyarlari;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.ModList;

/**
 * Sistemin çalışıp çalışmadığı (N8): {@code sicaklikSistemi} gamerule'ü açık <b>ve</b> SERVER config'teki
 * {@code sicaklikSistemiModu} izin veriyorsa (OTOMATIK: Cold Sweat yüklüyse kapalı). Kapalıyken güncelleme, hasar, paket
 * ve HUD yoktur; iki sistem aynı anda hasar vermez.
 */
public final class SicaklikKapisi {
    public static final String COLD_SWEAT = "cold_sweat";

    private SicaklikKapisi() {
    }

    public static boolean coldSweatYuklu() {
        ModList liste = ModList.get();
        return liste != null && liste.isLoaded(COLD_SWEAT);
    }

    public static boolean acikMi(MinecraftServer server) {
        return SunucuAyarlari.sicaklikSistemiModu().calisirMi(Kurallar.sicaklikKuraliAcik(server.getGameRules()),
                coldSweatYuklu());
    }

    /** Log ve {@code bilgi} komutu için tek satırlık kapı durumu. */
    public static String ozet(MinecraftServer server) {
        SicaklikSistemiModu mod = SunucuAyarlari.sicaklikSistemiModu();
        boolean kural = Kurallar.sicaklikKuraliAcik(server.getGameRules());
        boolean coldSweat = coldSweatYuklu();
        return "mod=" + mod + ", sicaklikSistemi=" + kural + ", Cold Sweat yüklü=" + coldSweat + " → "
                + (mod.calisirMi(kural, coldSweat) ? "AÇIK" : "KAPALI");
    }
}
