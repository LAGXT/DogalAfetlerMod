package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikIstemciAyarlari;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Donma ve aşırı ısınma ekran efektleri (madde 7c, N6 — yazı değil): çok soğukta kenarlardan içeri büyüyen buz
 * kristalleri, sıcakta turuncu-kırmızı dalgalanan sıcak hava vinyeti ve hafif kamera yalpası; donarken titreme. Şiddet
 * vücut sıcaklığının eşiklere göre konumundan gelir (eşikler pakette, sunucu config'inden).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SicaklikEkranEfektleri {
    private static final ResourceLocation BUZ = DogalAfetler.konum("textures/misc/sicaklik_buz.png");
    private static final ResourceLocation SICAK = DogalAfetler.konum("textures/misc/sicaklik_sicak.png");

    private SicaklikEkranEfektleri() {
    }

    @SubscribeEvent
    public static void katmanlariKaydet(RegisterGuiOverlaysEvent olay) {
        olay.registerAbove(VanillaGuiOverlay.FROSTBITE.id(), "sicaklik_efektleri", SicaklikEkranEfektleri::ciz);
    }

    /** Buz kenarı şiddeti 0–1: çok soğuk eşiğinden ölçek ucuna. */
    static float buz(SicaklikDurumPaketi d, float vucut) {
        return Mth.clamp((d.cokSoguk() - vucut) / (d.cokSoguk() + 100f), 0f, 1f);
    }

    /** Sıcak titreme şiddeti 0–1: sıcak eşiğinden aşırı ısınma hasarı eşiğine. */
    static float sicak(SicaklikDurumPaketi d, float vucut) {
        return Mth.clamp((vucut - d.sicak()) / Math.max(1f, d.yanma() - d.sicak()), 0f, 1f);
    }

    private static void ciz(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        if (!IstemciSicaklik.acik() || !SicaklikIstemciAyarlari.ekranEfektleri()
                || Minecraft.getInstance().options.getCameraType() != net.minecraft.client.CameraType.FIRST_PERSON) {
            return;
        }
        SicaklikDurumPaketi d = IstemciSicaklik.durum();
        float vucut = IstemciSicaklik.vucut(partialTick);
        float zaman = IstemciSicaklik.tick() + partialTick;
        float b = buz(d, vucut);
        float s = sicak(d, vucut);
        if (b <= 0 && s <= 0) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (b > 0) {
            float nabiz = vucut <= d.donma() ? 0.08f * Mth.sin(zaman * 0.35f) : 0f;
            g.setColor(1f, 1f, 1f, Mth.clamp((float) Math.pow(b, 0.7) + nabiz, 0f, 1f));
            g.blit(BUZ, 0, 0, -90, 0f, 0f, w, h, w, h);
        }
        if (s > 0) {
            float nabiz = vucut >= d.yanma() ? 0.12f * Mth.sin(zaman * 0.45f) : 0f;
            g.setColor(1f, 1f, 1f, Mth.clamp(0.25f + 0.6f * s + nabiz, 0f, 1f) * Math.min(1f, s * 4f));
            float kayma = (zaman * 0.9f) % 256f;
            g.blit(SICAK, 0, 0, w, h, 0f, kayma, 256, 256, 256, 256);
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** Kamera yalpası (Forge veri yolu): sıcakta yavaş sallantı, donarken hızlı titreme. */
    @Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
    public static final class Kamera {
        private Kamera() {
        }

        @SubscribeEvent
        public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
            if (!IstemciSicaklik.acik() || !SicaklikIstemciAyarlari.kameraTitremesi()) {
                return;
            }
            SicaklikDurumPaketi d = IstemciSicaklik.durum();
            float pt = (float) olay.getPartialTick();
            float vucut = IstemciSicaklik.vucut(pt);
            float zaman = IstemciSicaklik.tick() + pt;
            float s = sicak(d, vucut);
            float titreme = vucut <= d.donma() ? buz(d, vucut) : 0f;
            if (s <= 0 && titreme <= 0) {
                return;
            }
            float roll = s * (0.9f * Mth.sin(zaman * 0.21f) + 0.4f * Mth.sin(zaman * 0.53f + 1.3f))
                    + titreme * 0.35f * Mth.sin(zaman * 2.7f);
            olay.setRoll(olay.getRoll() + roll);
            olay.setPitch(olay.getPitch() + s * 0.25f * Mth.sin(zaman * 0.37f) + titreme * 0.2f * Mth.sin(zaman * 3.1f));
        }
    }
}
