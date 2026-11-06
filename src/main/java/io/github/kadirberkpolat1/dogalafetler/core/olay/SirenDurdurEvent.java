package io.github.kadirberkpolat1.dogalafetler.core.olay;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Siren fade-out ile dursun (madde 5): afet AKTIF'e geçti ya da siren sürerken durduruldu. Siren isteği almış ve hâlâ
 * çevrim içi olan oyuncular.
 */
public class SirenDurdurEvent extends Event {
    private final AfetTuru afetTuru;
    private final List<ServerPlayer> oyuncular;

    public SirenDurdurEvent(AfetTuru afetTuru, List<ServerPlayer> oyuncular) {
        this.afetTuru = afetTuru;
        this.oyuncular = List.copyOf(oyuncular);
    }

    public AfetTuru afetTuru() {
        return afetTuru;
    }

    public List<ServerPlayer> oyuncular() {
        return oyuncular;
    }
}
