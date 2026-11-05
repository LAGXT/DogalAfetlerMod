package io.github.kadirberkpolat1.dogalafetler.core.olay;

import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Event;

/**
 * Bir afet kararlaştırıldı ve ON_UYARI aşamasında başladı (gizli ön-uyarı penceresi, madde 4). Oyunculara hiçbir şey
 * duyurulmaz; Hava Radarı ve Sismograf bu andan sonra {@code AfetYoneticisi.aktifAfet()}'i okuyabilir.
 */
public class AfetBasladiEvent extends Event {
    private final Afet afet;
    private final ServerLevel level;

    public AfetBasladiEvent(Afet afet, ServerLevel level) {
        this.afet = afet;
        this.level = level;
    }

    public Afet afet() {
        return afet;
    }

    public ServerLevel level() {
        return level;
    }
}
