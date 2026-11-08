package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikIstemciAyarlari;
import net.minecraft.client.CameraType;
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
 * Vücut durumu ekran efektleri (§A8, N6 — yazı değil). Donma: kenarlardan içeri büyüyen buz kristalleri, ağır donmada
 * nabızlı. Bunalma: kenarlardan içeri süzülen kırmızı ter ve ısı izleri, hafif; yanmada güçlü ve kalp atışı gibi nabızlı
 * (hasara yaklaştıkça hızlanır), üstüne dalgalanan sıcak hava ve kamera yalpası. Üşümede ara ara hafif ürperme, donmada
 * hızlı titreme. Şiddet vücut sıcaklığının paketteki eşiklere (sunucu config'i) göre konumundan gelir.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SicaklikEkranEfektleri {
    private static final ResourceLocation BUZ = DogalAfetler.konum("textures/misc/sicaklik_buz.png");
    private static final ResourceLocation BUNALMA = DogalAfetler.konum("textures/misc/sicaklik_bunalma.png");
    private static final ResourceLocation SICAK = DogalAfetler.konum("textures/misc/sicaklik_sicak.png");

    private SicaklikEkranEfektleri() {
    }

    @SubscribeEvent
    public static void katmanlariKaydet(RegisterGuiOverlaysEvent olay) {
        olay.registerAbove(VanillaGuiOverlay.FROSTBITE.id(), "sicaklik_efektleri", SicaklikEkranEfektleri::ciz);
    }

    /** Buz kenarı şiddeti 0–1: donma eşiğinde (çok soğuk) 0,25'ten ölçek ucuna 1; üstünde 0. */
    static float buz(SicaklikDurumPaketi d, float vucut) {
        if (vucut > d.cokSoguk()) {
            return 0f;
        }
        return 0.25f + 0.75f * Mth.clamp((d.cokSoguk() - vucut) / (d.cokSoguk() + 100f), 0f, 1f);
    }

    /**
     * Kırmızı kenar şiddeti 0–1: bunalmada 0,2–0,4 (sıcak → çok sıcak eşiği), yanmada 0,55–1 (çok sıcak → aşırı ısınma
     * hasarı eşiği); bunalmanın altında 0.
     */
    static float kirmizi(SicaklikDurumPaketi d, float vucut) {
        if (vucut < d.sicak()) {
            return 0f;
        }
        if (vucut < d.cokSicak()) {
            return 0.2f + 0.2f * Mth.clamp((vucut - d.sicak()) / Math.max(1f, d.cokSicak() - d.sicak()), 0f, 1f);
        }
        return 0.55f + 0.45f * Mth.clamp((vucut - d.cokSicak()) / Math.max(1f, d.yanma() - d.cokSicak()), 0f, 1f);
    }

    /** Sıcak hava ve yalpa şiddeti 0–1: yalnız yanmada, çok sıcak eşiğinden aşırı ısınma hasarı eşiğine. */
    static float sicak(SicaklikDurumPaketi d, float vucut) {
        if (vucut < d.cokSicak()) {
            return 0f;
        }
        return 0.3f + 0.7f * Mth.clamp((vucut - d.cokSicak()) / Math.max(1f, d.yanma() - d.cokSicak()), 0f, 1f);
    }

    /** Titreme şiddeti 0–1: üşümede 0,25 (ara ara), donmada buz kenarıyla birlikte artar. */
    static float titreme(SicaklikDurumPaketi d, float vucut) {
        if (vucut > d.soguk()) {
            return 0f;
        }
        return vucut > d.cokSoguk() ? 0.25f : 0.4f + 0.6f * buz(d, vucut);
    }

    private static void ciz(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        if (!IstemciSicaklik.acik() || !SicaklikIstemciAyarlari.ekranEfektleri()
                || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON) {
            return;
        }
        SicaklikDurumPaketi d = IstemciSicaklik.durum();
        float vucut = IstemciSicaklik.vucut(partialTick);
        float zaman = IstemciSicaklik.tick() + partialTick;
        float b = buz(d, vucut);
        float k = kirmizi(d, vucut);
        float s = sicak(d, vucut);
        if (b <= 0 && k <= 0) {
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
        if (k > 0) {
            g.setColor(1f, 1f, 1f, Mth.clamp(k * kalpAtisi(d, vucut, zaman), 0f, 1f));
            g.blit(BUNALMA, 0, 0, -90, 0f, 0f, w, h, w, h);
        }
        if (s > 0) {
            float nabiz = vucut >= d.yanma() ? 0.12f * Mth.sin(zaman * 0.45f) : 0f;
            g.setColor(1f, 1f, 1f, Mth.clamp(0.2f + 0.55f * s + nabiz, 0f, 1f));
            float kayma = (zaman * 0.9f) % 256f;
            g.blit(SICAK, 0, 0, w, h, 0f, kayma, 256, 256, 256, 256);
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** Kırmızı kenarın nabız çarpanı: bunalmada yavaş ve sığ, yanmada HUD hapıyla aynı kalp atışı (0,7–1). */
    private static float kalpAtisi(SicaklikDurumPaketi d, float vucut, float zaman) {
        float periyot = HudCizimi.nabizPeriyodu(vucut, d.sicak(), d.yanma());
        float faz = (zaman % periyot) / periyot;
        float derinlik = vucut >= d.cokSicak() ? 0.3f : 0.12f;
        return 1f - derinlik + derinlik * (float) Math.exp(-faz * 5.0);
    }

    /** Kamera (Forge veri yolu): yanmada yavaş yalpa, üşümede ara ara ürperme, donmada hızlı titreme. */
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
            float titreme = titreme(d, vucut);
            if (vucut > d.cokSoguk()) {
                // Üşüme: ~6 sn'de bir 1 sn süren ürperme dalgası.
                float dalga = Mth.sin(zaman * 0.05f);
                titreme *= dalga > 0.85f ? (dalga - 0.85f) / 0.15f : 0f;
            }
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
