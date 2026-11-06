package io.github.kadirberkpolat1.dogalafetler.yiyecek;

import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.Etki;
import io.github.kadirberkpolat1.dogalafetler.yiyecek.mantik.YiyecekTanimi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.eventbus.api.Event;

/**
 * Bir sıcaklık yiyeceği (madde 14 eşyası, vanilla pişmiş yiyecek ya da balıkçılığın balığı) yendi
 * ({@code MinecraftForge.EVENT_BUS}, sunucu ana thread'i, iptal edilemez). {@link YiyecekAPI#uygula} her çağrıda bir kez
 * yayınlar; sıcaklık kapısı kapalıyken de gelir ({@link #uygulandi()} {@code false}, kademeler aynı). Başarım "Çorba İçer
 * Isınır" (N16) {@code oncekiKademe == DONUYOR} ve {@code tanim.yon() == ISINMA} ile başlar, etkinin süresi
 * ({@code tanim.sureTick()}) içinde vücudun nötre dönüşünü kendisi izler.
 */
public class YiyecekYendiEvent extends Event {
    private final ServerPlayer oyuncu;
    private final YiyecekTanimi tanim;
    private final Kademe oncekiKademe;
    private final Kademe sonrakiKademe;
    private final double verimCarpani;
    private final Etki etki;
    private final boolean uygulandi;

    public YiyecekYendiEvent(ServerPlayer oyuncu, YiyecekTanimi tanim, Kademe oncekiKademe, Kademe sonrakiKademe,
            double verimCarpani, Etki etki, boolean uygulandi) {
        this.oyuncu = oyuncu;
        this.tanim = tanim;
        this.oncekiKademe = oncekiKademe;
        this.sonrakiKademe = sonrakiKademe;
        this.verimCarpani = verimCarpani;
        this.etki = etki;
        this.uygulandi = uygulandi;
    }

    public ServerPlayer oyuncu() {
        return oyuncu;
    }

    public YiyecekTanimi tanim() {
        return tanim;
    }

    /** Yemeden hemen önceki vücut kademesi. */
    public Kademe oncekiKademe() {
        return oncekiKademe;
    }

    /** Anında kısım eklendikten sonraki kademe (süreli kısım sonra gelir). */
    public Kademe sonrakiKademe() {
        return sonrakiKademe;
    }

    /** Azalan verim çarpanı (1 tam, vars. 0,5 aynı tür 60 sn içinde). */
    public double verimCarpani() {
        return verimCarpani;
    }

    /** Uygulanan etki (kapı kapalıyken sıfır). */
    public Etki etki() {
        return etki;
    }

    /** Sıcaklık sistemi açıktı ve etki uygulandı. */
    public boolean uygulandi() {
        return uygulandi;
    }
}
