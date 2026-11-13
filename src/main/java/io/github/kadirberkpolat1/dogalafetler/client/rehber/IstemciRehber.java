package io.github.kadirberkpolat1.dogalafetler.client.rehber;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.EkranEfektKapisi;
import io.github.kadirberkpolat1.dogalafetler.rehber.RehberMesaji;
import io.github.kadirberkpolat1.dogalafetler.rehber.RehberMesajPaketi;
import io.github.kadirberkpolat1.dogalafetler.rehber.mantik.KonusmaRitmi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikParcaciklari;
import java.util.function.Consumer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rehber cümlesinin istemci durumu (K-E74): sunucunun söylediği cümle, kaçıncı tick'te olduğu ve oyuncunun kendi ağız buharı.
 * Sunucu buharı oyuncunun kendisine göndermez ({@code SicaklikYoneticisi} deseni), kendi buharını burada üretiriz: üçüncü
 * şahısta modun nefes bulutu, birinci şahısta ekranı kaplamasın diye küçük bir {@code CLOUD}. Ritim sunucununkiyle aynıdır
 * ({@link KonusmaRitmi}). Oyun duraklatılınca sayaç durur; yaratıcı/izleyici modunda yazı ve buhar yoktur
 * ({@link EkranEfektKapisi}).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciRehber {
    /** Birinci şahıs buharının boyut çarpanı (vanilla bulut ~0,3 blok). */
    private static final float KUCUK_BUHAR = 0.3f;

    private static RehberMesaji mesaj;
    private static int gecen;

    private IstemciRehber() {
    }

    /** Paket işleyicisinin alıcısı ({@code DistExecutor.safeCallWhenOn}, K-C14). */
    public static Consumer<RehberMesajPaketi> alici() {
        return IstemciRehber::al;
    }

    private static void al(RehberMesajPaketi paket) {
        RehberMesaji yeni = RehberMesaji.anahtarlaBul(paket.anahtar());
        if (yeni != null) {
            mesaj = yeni;
            gecen = 0;
        }
    }

    /** Şu an gösterilen cümle; yoksa ({@code null}) yazı çizilmez. */
    static RehberMesaji mesaj() {
        return mesaj;
    }

    /** Cümlenin başlangıcından geçen tick (kesirli: ara kare dahil). */
    static float gecen(float araKare) {
        return gecen + araKare;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END || mesaj == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer oyuncu = mc.player;
        if (oyuncu == null || mc.level == null) {
            mesaj = null;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        gecen++;
        if (gecen >= KonusmaRitmi.SURE_TICK) {
            mesaj = null;
            return;
        }
        if (EkranEfektKapisi.izinli(oyuncu) && KonusmaRitmi.pufVar(mesaj.puflar(), gecen)) {
            agizBuhari(mc, oyuncu);
        }
    }

    @SubscribeEvent
    public static void baglantiKesildi(ClientPlayerNetworkEvent.LoggingOut olay) {
        mesaj = null;
        gecen = 0;
    }

    /** Ağızdan öne doğru bir buhar parçası (sunucunun başkalarına gönderdiğiyle aynı yer ve yön). */
    private static void agizBuhari(Minecraft mc, LocalPlayer oyuncu) {
        RandomSource r = oyuncu.getRandom();
        Vec3 bak = oyuncu.getViewVector(1f);
        Vec3 agiz = oyuncu.getEyePosition().add(bak.scale(0.35)).add(0, -0.12, 0);
        if (mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            // Kamera gözde: biraz öne ve aşağıya, yavaş ve küçük; önce parçacık kuyruğuna girer, sonra ölçekçilenir.
            for (int i = 0; i < 2; i++) {
                Particle p = mc.particleEngine.createParticle(ParticleTypes.CLOUD, agiz.x + bak.x * 0.1,
                        agiz.y - 0.1, agiz.z + bak.z * 0.1, bak.x * 0.02 + (r.nextDouble() - 0.5) * 0.01,
                        0.004 + (r.nextDouble() - 0.5) * 0.006, bak.z * 0.02 + (r.nextDouble() - 0.5) * 0.01);
                if (p != null) {
                    p.scale(KUCUK_BUHAR);
                }
            }
            return;
        }
        for (int i = 0; i < 3; i++) {
            mc.level.addParticle(SicaklikParcaciklari.NEFES_BUHARI.get(), agiz.x, agiz.y, agiz.z,
                    bak.x * 0.09 + (r.nextDouble() - 0.5) * 0.03, bak.y * 0.06 + 0.006 + (r.nextDouble() - 0.5) * 0.015,
                    bak.z * 0.09 + (r.nextDouble() - 0.5) * 0.03);
        }
    }
}
