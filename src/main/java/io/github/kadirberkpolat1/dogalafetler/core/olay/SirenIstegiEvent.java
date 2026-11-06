package io.github.kadirberkpolat1.dogalafetler.core.olay;

import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.SirenTuru;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * "Şu oyunculara şu tür siren, şu süre" (madde 5, spike-siren S-02). Sireni çalmak {@code uyari} paketinin işidir.
 * SIREN aşaması başlarken etkilenebilecek bütün oyuncular için, siren sürerken alana yeni giren ya da sunucuya yeni
 * bağlanan oyuncular için (kalan süreyle) yayınlanır. {@code sureTick} gerçek tick'tir ({@code /dogalafet hiz} hesaba
 * katılmıştır).
 */
public class SirenIstegiEvent extends Event {
    private final AfetTuru afetTuru;
    private final SirenTuru sirenTuru;
    private final int sureTick;
    private final List<ServerPlayer> oyuncular;

    public SirenIstegiEvent(AfetTuru afetTuru, SirenTuru sirenTuru, int sureTick, List<ServerPlayer> oyuncular) {
        this.afetTuru = afetTuru;
        this.sirenTuru = sirenTuru;
        this.sureTick = sureTick;
        this.oyuncular = List.copyOf(oyuncular);
    }

    public AfetTuru afetTuru() {
        return afetTuru;
    }

    public SirenTuru sirenTuru() {
        return sirenTuru;
    }

    public int sureTick() {
        return sureTick;
    }

    public List<ServerPlayer> oyuncular() {
        return oyuncular;
    }
}
