package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sunucudan gelen sıcaklık durumunun istemci önbelleği. Paketler saniyede bir gelir; HUD ve efektler ani sıçramasın diye
 * gösterilen değer her istemci tick'inde hedefe yumuşakça yaklaşır ve kare arasında {@code partialTick} ile
 * ara değerlenir. Çıkışta sıfırlanır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciSicaklik {
    private static final float YAKLASMA = 0.25f;

    private static SicaklikDurumPaketi durum = SicaklikDurumPaketi.KAPALI;
    private static float oncekiVucut;
    private static float vucut;
    private static float oncekiOrtam;
    private static float ortam;
    private static boolean ilk = true;
    private static long tick;

    private IstemciSicaklik() {
    }

    static void guncelle(SicaklikDurumPaketi paket) {
        durum = paket;
        if (ilk || !paket.acik()) {
            vucut = oncekiVucut = paket.vucut();
            ortam = oncekiOrtam = paket.ortam();
            ilk = !paket.acik();
        }
    }

    public static SicaklikDurumPaketi durum() {
        return durum;
    }

    public static boolean acik() {
        return durum.acik();
    }

    /** Gösterilen (yumuşatılmış) vücut sıcaklığı. */
    public static float vucut(float partialTick) {
        return Mth.lerp(partialTick, oncekiVucut, vucut);
    }

    public static float ortam(float partialTick) {
        return Mth.lerp(partialTick, oncekiOrtam, ortam);
    }

    /** İstemci tick sayacı (titreşim ve yanıp sönme zamanlaması). */
    public static long tick() {
        return tick;
    }

    @SubscribeEvent
    public static void istemciTick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        tick++;
        oncekiVucut = vucut;
        oncekiOrtam = ortam;
        vucut += (durum.vucut() - vucut) * YAKLASMA;
        ortam += (durum.ortam() - ortam) * YAKLASMA;
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        durum = SicaklikDurumPaketi.KAPALI;
        ilk = true;
        vucut = oncekiVucut = ortam = oncekiOrtam = 0;
    }
}
