package io.github.kadirberkpolat1.dogalafetler.meteor;

import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.OdulZari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.eventbus.api.Event;

/**
 * Bir meteor çarpması tamamlandı ({@code MinecraftForge.EVENT_BUS}, sunucu ana thread'i): karada krater, yangın ve ödüller
 * yerleştikten sonra, suda çarpma anında. {@code odul} zarın sonucudur (bırakılan eşya); başarımlar eşya alımından
 * tetiklenir (§A).
 */
public class MeteorCarptiEvent extends Event {
    private final ServerLevel level;
    private final BlockPos konum;
    private final double olcek;
    private final YanginKaynagi kaynak;
    private final OdulZari.Sonuc odul;
    private final boolean suya;

    public MeteorCarptiEvent(ServerLevel level, BlockPos konum, double olcek, YanginKaynagi kaynak,
            OdulZari.Sonuc odul, boolean suya) {
        this.level = level;
        this.konum = konum;
        this.olcek = olcek;
        this.kaynak = kaynak;
        this.odul = odul;
        this.suya = suya;
    }

    public ServerLevel level() {
        return level;
    }

    /** Çarpılan yüzey bloğu (krater merkezi). */
    public BlockPos konum() {
        return konum;
    }

    /** Ödül sınıfı (ölçeğin bantı). */
    public MeteorBoyutu boyut() {
        return MeteorBoyutu.olcekten(olcek);
    }

    /** Sürekli ölçek ({@code mantik.MeteorOlcegi}). */
    public double olcek() {
        return olcek;
    }

    public YanginKaynagi kaynak() {
        return kaynak;
    }

    public OdulZari.Sonuc odul() {
        return odul;
    }

    public boolean suya() {
        return suya;
    }
}
