package io.github.kadirberkpolat1.dogalafetler.config;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** Config spec'lerini Forge'a kaydeder. */
public final class Ayarlar {
    public static final String SUNUCU_DOSYASI = "dogalafetler-server.toml";
    public static final String ISTEMCI_DOSYASI = "dogalafetler-client.toml";

    private Ayarlar() {
    }

    public static void kaydet(ModLoadingContext baglam) {
        baglam.registerConfig(ModConfig.Type.SERVER, SunucuAyarlari.SPEC, SUNUCU_DOSYASI);
        baglam.registerConfig(ModConfig.Type.CLIENT, IstemciAyarlari.SPEC, ISTEMCI_DOSYASI);
    }
}
