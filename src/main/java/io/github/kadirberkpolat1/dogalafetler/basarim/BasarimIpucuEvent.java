package io.github.kadirberkpolat1.dogalafetler.basarim;

import java.util.Objects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Faz 4 afetlerinin başarımlara verdiği ipucu ({@code MinecraftForge.EVENT_BUS}, sunucu ana thread'i). Yalnız afetin
 * kendisinin bildiği "o afette ayrıca" koşullar içindir; ipucu oyuncunun süren (AKTIF) afetteki izine yazılır ve afet
 * doğal bitince {@code afetten_sag_cikti} ölçütünün {@code kosul} alanına döner. Sözleşme
 * {@code .vibe/kararlar/basarim.md} §A.
 *
 * <pre>{@code
 * MinecraftForge.EVENT_BUS.post(new BasarimIpucuEvent(oyuncu, BasarimIpucuEvent.Ipucu.CUKURA_DUSTU));
 * }</pre>
 */
public class BasarimIpucuEvent extends Event {
    /** İpucu türleri. */
    public enum Ipucu {
        /** Tsunami: oyuncu dalga tarafından sürüklendi (#2). */
        SURUKLENDI,
        /** Sinkhole: oyuncu çökme sırasında çukura düştü (#4). */
        CUKURA_DUSTU,
        /** Orman yangını: oyuncu bu saniye yanan alanın içinde (saniyede en çok bir kez sayılır; 30'da #5). */
        YANGIN_ALANINDA,
        /** Asit yağmuru (ya da başka afet): oyuncu afetten hasar aldı (#6'yı bozar). */
        HASAR_ALDI
    }

    private final ServerPlayer oyuncu;
    private final Ipucu ipucu;

    public BasarimIpucuEvent(ServerPlayer oyuncu, Ipucu ipucu) {
        this.oyuncu = Objects.requireNonNull(oyuncu, "oyuncu");
        this.ipucu = Objects.requireNonNull(ipucu, "ipucu");
    }

    public ServerPlayer oyuncu() {
        return oyuncu;
    }

    public Ipucu ipucu() {
        return ipucu;
    }
}
