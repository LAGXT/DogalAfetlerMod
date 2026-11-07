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

    private static volatile Boolean coldSweatTesti;

    public static boolean coldSweatYuklu() {
        Boolean test = coldSweatTesti;
        if (test != null) {
            return test;
        }
        ModList liste = ModList.get();
        return liste != null && liste.isLoaded(COLD_SWEAT);
    }

    /**
     * Yalnız GameTest (K-E41, K-E28 deseni): kapının Cold Sweat'i yüklü sayıp saymadığını geçersiz kılar; {@code null} gerçek
     * {@code ModList}'e döner. Önceki değeri döner.
     */
    public static Boolean coldSweatTesti(Boolean deger) {
        Boolean onceki = coldSweatTesti;
        coldSweatTesti = deger;
        return onceki;
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
