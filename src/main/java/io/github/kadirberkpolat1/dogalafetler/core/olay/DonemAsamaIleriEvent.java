package io.github.kadirberkpolat1.dogalafetler.core.olay;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

/**
 * {@code /dogalafet asama ileri} aktif afet yokken: dünya dönemini (güneş evresi, donma, mağara suyu …) bir sonraki
 * aşamaya geçirme isteği (N18). Dönemin sahibi olan alt sistem isteği uygulayınca {@link #islendi(Component)} ile
 * komut çıktısını bırakır; ilk işleyen kazanır.
 */
public class DonemAsamaIleriEvent extends Event {
    private final MinecraftServer server;
    private Component mesaj;

    public DonemAsamaIleriEvent(MinecraftServer server) {
        this.server = server;
    }

    public MinecraftServer server() {
        return server;
    }

    public boolean islendiMi() {
        return mesaj != null;
    }

    public void islendi(Component komutCiktisi) {
        if (mesaj == null) {
            mesaj = komutCiktisi;
        }
    }

    public Component mesaj() {
        return mesaj;
    }
}
