package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaCekimi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaVarligi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

/**
 * Oyuncunun kasırga çekimi kendi istemcisinde (AF3-01): hareketin sahibi istemcidir, sunucunun hız paketi istemcinin
 * hızını ezer ve sunucuda oyuncunun hızı bilinmez. {@code PlayerTickEvent} START vanilla {@code aiStep/travel}'dan önce
 * çalışır; eklenen hız aynı tick'in hareketine girer ve koşu girdisiyle birlikte birikir. Oyuncu bir aracı sürüyorsa
 * (tekne, at) hareketin sahibi yine bu istemcidir ve çekim araca uygulanır (denetim P2-4). Girdi senkronlanan
 * {@link KasirgaVarligi} verisidir (konum, seviye, boyut, evre, çekim çarpanı, kaldırma tavanı); olay yettiği için mixin yok.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class KasirgaOyuncuCekimi {
    private KasirgaOyuncuCekimi() {
    }

    @SubscribeEvent
    public static void oyuncuTick(TickEvent.PlayerTickEvent olay) {
        Player oyuncu = olay.player;
        if (olay.phase != TickEvent.Phase.START || olay.side != LogicalSide.CLIENT || !oyuncu.isLocalPlayer()
                || IstemciKasirga.KASIRGALAR.isEmpty()) {
            return;
        }
        Entity hedef = hareketSahibi(oyuncu);
        if (hedef == null || !KasirgaCekimi.cekilebilir(hedef)) {
            return;
        }
        double[] toplam = new double[3];
        for (KasirgaVarligi kasirga : IstemciKasirga.KASIRGALAR.values()) {
            if (kasirga.level() != oyuncu.level()) {
                continue;
            }
            double[] ivme = KasirgaCekimi.ivme(hedef, kasirga.cekimKaynagi());
            toplam[0] += ivme[0];
            toplam[1] += ivme[1];
            toplam[2] += ivme[2];
        }
        KasirgaCekimi.uygula(hedef, toplam);
    }

    /**
     * Hareketini bu istemcinin yürüttüğü varlık: binmemiş oyuncunun kendisi, sürdüğü araç; yolcu olduğu (sürmediği) araçta
     * null (o aracı sahibi çeker: sürücüsüz aracı sunucu, başka oyuncunun sürdüğünü onun istemcisi).
     */
    private static Entity hareketSahibi(Player oyuncu) {
        Entity arac = oyuncu.getVehicle();
        if (arac == null) {
            return oyuncu;
        }
        return arac.getControllingPassenger() == oyuncu && arac.isControlledByLocalInstance() ? arac : null;
    }
}
