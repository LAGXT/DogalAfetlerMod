package io.github.kadirberkpolat1.dogalafetler.client.orman_yangini;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini.mantik.OrmanKurallari;
import io.github.kadirberkpolat1.dogalafetler.client.core.EkranEfektKapisi;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import java.util.Optional;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Orman yangını afetinin ortam efektleri (tasarım notu §2 "Ortam"): yangına 48 blok içinde turuncu-kahverengi sis
 * ({@code #7A5236}, görüş %55'e iner), 24 blok içinde ısı dalgalanması (şader yok: kamera açısına 0,05° 3 Hz sinüs ve alt
 * kenarda yukarı kayan yarı saydam ısı dokusu; Yüksek ve Ultra) ve havada süzülen kül. Alev, duman, kıvılcım ve duman
 * sütunu ortak yangının istemcisindedir ({@code client.yangin}). Yangının yeri afetin durum paketindeki sıcak karelerden
 * ({@code ozel.h}: 32 blokluk kare merkezi ve hücre sayısı) okunur.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class IstemciOrmanYangini {
    private static final ResourceLocation ISI = DogalAfetler.konum("textures/misc/isi_dalgasi.png");
    private static final float[] DUMAN = {0x7A / 255f, 0x52 / 255f, 0x36 / 255f};
    private static final double[] VANILLA_AYAR_CARPANI = {1.0, 0.5, 0.2};
    /** Kül parçacığı/tick: Düşük, Orta, Yüksek, Ultra. */
    private static final int[] KUL = {1, 4, 8, 14};
    private static final double KUL_MENZILI = 14;

    private static int[] kareler = new int[0];
    private static int hucre;
    private static double sis;
    private static double isi;
    private static long saat;

    private IstemciOrmanYangini() {
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        kareler = new int[0];
        hucre = 0;
        sis = 0;
        isi = 0;
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.isPaused()) {
            return;
        }
        saat++;
        Optional<AfetDurumPaketi> paket = IstemciAfetDurumu.aktif(AfetTuru.ORMAN_YANGINI).filter(p -> p.boyut().equals(level.dimension().location())
                && (p.asama() == AfetAsamasi.AKTIF || p.asama() == AfetAsamasi.BITIS));
        kareler = paket.map(p -> p.ozel().getIntArray("h")).orElse(new int[0]);
        hucre = paket.map(p -> p.ozel().getInt("n")).orElse(0);
        Vec3 goz = mc.gameRenderer.getMainCamera().getPosition();
        double uzaklik = OrmanKurallari.enYakin(kareler, goz.x, goz.z);
        double hedefSis = OrmanKurallari.yakinlikAgirligi(uzaklik, OrmanKurallari.SIS_MENZILI)
                * OrmanKurallari.buyuklukCarpani(hucre);
        double hedefIsi = OrmanKurallari.yakinlikAgirligi(uzaklik, OrmanKurallari.ISI_MENZILI)
                * Math.min(1.0, hucre / 20.0);
        sis += Mth.clamp(hedefSis - sis, -0.01, 0.01);
        isi += Mth.clamp(hedefIsi - isi, -0.02, 0.02);
        kul(mc, level, goz);
    }

    private static void kul(Minecraft mc, ClientLevel level, Vec3 goz) {
        if (sis <= 0.05) {
            return;
        }
        RandomSource r = level.random;
        double ayar = VANILLA_AYAR_CARPANI[mc.options.particles().get().getId()];
        int sayi = (int) Math.round(KUL[kademe().ordinal()] * sis * ayar);
        for (int i = 0; i < sayi; i++) {
            double px = goz.x + (r.nextDouble() * 2 - 1) * KUL_MENZILI;
            double py = goz.y + r.nextDouble() * 8 - 2;
            double pz = goz.z + (r.nextDouble() * 2 - 1) * KUL_MENZILI;
            level.addParticle(r.nextInt(4) == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.ASH, true, px, py, pz, 0, 0, 0);
        }
    }

    private static EfektKalitesi kademe() {
        return IstemciAyarlari.efektKalitesi();
    }

    private static boolean isiAcik() {
        return isi > 0.01 && (kademe() == EfektKalitesi.YUKSEK || kademe() == EfektKalitesi.ULTRA)
                && EkranEfektKapisi.isiIzinli();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sisRengi(ViewportEvent.ComputeFogColor olay) {
        if (sis <= 0) {
            return;
        }
        float k = (float) (0.85 * sis);
        olay.setRed(Mth.lerp(k, olay.getRed(), DUMAN[0]));
        olay.setGreen(Mth.lerp(k, olay.getGreen(), DUMAN[1]));
        olay.setBlue(Mth.lerp(k, olay.getBlue(), DUMAN[2]));
    }

    /** Duman perdesi: görüş mesafesi %55'e iner (Forge değişikliği yalnız iptal edilen olayda uygular). */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void sis(ViewportEvent.RenderFog olay) {
        if (sis <= 0 || olay.isCanceled()) {
            return;
        }
        float kisalma = (float) (0.45 * sis);
        olay.setFarPlaneDistance(olay.getFarPlaneDistance() * (1 - kisalma));
        olay.setNearPlaneDistance(olay.getNearPlaneDistance() * (1 - 1.5F * kisalma));
        olay.setCanceled(true);
    }

    /** Isı dalgalanması: kamera açılarına 0,05° genlikli 3 Hz sinüs (yaw ve pitch faz kaydırmalı). */
    @SubscribeEvent
    public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
        if (!isiAcik()) {
            return;
        }
        double t = (saat + olay.getPartialTick()) / 20.0;
        float genlik = (float) (0.05 * isi);
        olay.setYaw(olay.getYaw() + genlik * Mth.sin((float) (t * Math.PI * 6)));
        olay.setPitch(olay.getPitch() + genlik * 0.6F * Mth.sin((float) (t * Math.PI * 6 + 1.3)));
    }

    /** Ekranın alt dörtte birinde yukarı kayan yarı saydam ısı dokusu (Yüksek ve Ultra). */
    static void isiKatmani(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        if (!isiAcik() || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        int bant = h / 4;
        float kayma = ((saat + pt) * 0.8f) % 64f;
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, (float) (0.22 * isi));
        g.blit(ISI, 0, h - bant, w, bant, 0f, kayma, 64, 64 * bant / Math.max(1, w), 64, 64);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
