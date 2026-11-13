package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.EkranEfektKapisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikIstemciAyarlari;
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
 * Vücut durumu ekran efektleri (§A8, N6 — yazı değil; S3-10). Donma: kenarlardan içeri büyüyen buz kristalleri, ağır
 * donmada nabızlı. Bunalma: buz kenarının kırmızı karşılığı, aynı derinlikte en az onun kadar belirgin kenarlardan içeri
 * süzülen ter ve ısı izleri; yanmada daha yoğun ve kalp atışı gibi nabızlı (hasara yaklaştıkça hızlanır), üstüne
 * dalgalanan sıcak hava ve kamera yalpası. Üşümede ara ara hafif ürperme, donmada hızlı titreme. Kenar izleri vanilla toz
 * kar kırağısı gibi her kamera kipinde çizilir. Şiddet {@link EkranSiddeti}'nden.
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

    private static void ciz(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        if (!IstemciSicaklik.acik() || !SicaklikIstemciAyarlari.ekranEfektleri() || !EkranEfektKapisi.izinli()) {
            return;
        }
        SicaklikDurumPaketi d = IstemciSicaklik.durum();
        float vucut = IstemciSicaklik.vucut(partialTick);
        float zaman = IstemciSicaklik.tick() + partialTick;
        float b = EkranSiddeti.buzOpaklik(d, vucut, zaman);
        float k = EkranSiddeti.kirmiziOpaklik(d, vucut, zaman);
        float s = EkranSiddeti.sicakOpaklik(d, vucut, zaman);
        if (b <= 0 && k <= 0) {
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (b > 0) {
            g.setColor(1f, 1f, 1f, b);
            g.blit(BUZ, 0, 0, -90, 0f, 0f, w, h, w, h);
        }
        if (k > 0) {
            g.setColor(1f, 1f, 1f, k);
            g.blit(BUNALMA, 0, 0, -90, 0f, 0f, w, h, w, h);
        }
        if (s > 0) {
            g.setColor(1f, 1f, 1f, s);
            float kayma = (zaman * 0.9f) % 256f;
            g.blit(SICAK, 0, 0, w, h, 0f, kayma, 256, 256, 256, 256);
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** Kamera (Forge veri yolu): yanmada yavaş yalpa, üşümede ara ara ürperme, donmada hızlı titreme. */
    @Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
    public static final class Kamera {
        private Kamera() {
        }

        @SubscribeEvent
        public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
            if (!IstemciSicaklik.acik() || !SicaklikIstemciAyarlari.kameraTitremesi() || !EkranEfektKapisi.izinli()) {
                return;
            }
            SicaklikDurumPaketi d = IstemciSicaklik.durum();
            float pt = (float) olay.getPartialTick();
            float vucut = IstemciSicaklik.vucut(pt);
            float zaman = IstemciSicaklik.tick() + pt;
            float s = EkranSiddeti.sicak(d, vucut);
            float titreme = EkranSiddeti.titreme(d, vucut);
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
