package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
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
    /** Kendi alev/kar tanesi parçacıklarının aralığı (tick): sunucunun varsayılan güncelleme aralığı. */
    private static final int PARCACIK_ARALIGI = 20;

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
        if (tick % PARCACIK_ARALIGI == 0) {
            kendiParcaciklari(Minecraft.getInstance());
        }
    }

    /**
     * Hasarlı kademede oyuncunun kendi üstündeki alev (aşırı ısınma) ya da kar tanesi (donma), sunucunun diğer oyunculara
     * gönderdiğiyle aynı sayı ve saçılımda. Sunucu bunu oyuncunun kendisine göndermez; birinci şahısta göğüsteki 8×8
     * sprite ekranda dev bir leke olur, o yüzden yalnız kamera birinci şahıs değilken üretilir.
     */
    private static void kendiParcaciklari(Minecraft mc) {
        LocalPlayer o = mc.player;
        if (o == null || mc.level == null || mc.isPaused() || o.isSpectator() || !durum.acik()
                || !durum.kademe().hasarli() || mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            return;
        }
        boolean donma = durum.kademe() == Kademe.DONUYOR;
        ParticleOptions tur = donma ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME;
        RandomSource r = o.getRandom();
        for (int i = donma ? 6 : 4; i > 0; i--) {
            mc.level.addParticle(tur, o.getX() + r.nextGaussian() * 0.3, o.getY() + 1.0 + r.nextGaussian() * 0.5,
                    o.getZ() + r.nextGaussian() * 0.3, r.nextGaussian() * 0.01, r.nextGaussian() * 0.01,
                    r.nextGaussian() * 0.01);
        }
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        durum = SicaklikDurumPaketi.KAPALI;
        ilk = true;
        vucut = oncekiVucut = ortam = oncekiOrtam = 0;
    }
}
