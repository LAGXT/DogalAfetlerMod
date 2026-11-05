package io.github.kadirberkpolat1.dogalafetler.core.olay;

import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Event;

/** Afetin aşaması ya da alt aşaması değişti ({@code eski == yeni} ise yalnız alt aşama değişmiştir). */
public class AfetAsamaDegistiEvent extends Event {
    private final Afet afet;
    private final ServerLevel level;
    private final AfetAsamasi eski;
    private final AfetAsamasi yeni;
    private final int eskiAltAsama;
    private final int yeniAltAsama;

    public AfetAsamaDegistiEvent(Afet afet, ServerLevel level, AfetAsamasi eski, AfetAsamasi yeni, int eskiAltAsama,
            int yeniAltAsama) {
        this.afet = afet;
        this.level = level;
        this.eski = eski;
        this.yeni = yeni;
        this.eskiAltAsama = eskiAltAsama;
        this.yeniAltAsama = yeniAltAsama;
    }

    public Afet afet() {
        return afet;
    }

    public ServerLevel level() {
        return level;
    }

    public AfetAsamasi eski() {
        return eski;
    }

    public AfetAsamasi yeni() {
        return yeni;
    }

    public int eskiAltAsama() {
        return eskiAltAsama;
    }

    public int yeniAltAsama() {
        return yeniAltAsama;
    }
}
