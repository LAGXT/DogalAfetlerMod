package io.github.kadirberkpolat1.dogalafetler.client.rehber;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.EkranEfektKapisi;
import io.github.kadirberkpolat1.dogalafetler.rehber.RehberMesaji;
import io.github.kadirberkpolat1.dogalafetler.rehber.mantik.KonusmaRitmi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Rehber yazısı katmanı (K-E74): can, zırh, açlık ve sıcaklık bantlarının üstünde ortada, söyleyenin kendi cümlesi (buz
 * mavisi beyaz, hafif koyu zeminli); {@link KonusmaRitmi#GIRIS_TICK} tick'te belirir, son {@link KonusmaRitmi#CIKIS_TICK}
 * tick'te söner. Katman vanilla durum çubuklarından sonra çizilir, böylece Forge'un {@code leftHeight/rightHeight}
 * yükseklikleri (sıcaklık bandının kaydırdığı dahil) son halindedir; alt kenar hotbar üstünden en az 54 px yukarıda kalır
 * ve action bar'ın ({@code h - max(max(left, right) + 9, 68)}) üstüne oturur.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RehberKatmani {
    /** Yazı alt kenarının ekranın altından en az uzaklığı: hotbar üstü (22 px) + 54 px. */
    private static final int EN_AZ_ALT = 22 + 54;
    /** Durum barı yüksekliğinin üstüne bırakılan boşluk (action bar satırı + pay). */
    private static final int BAR_BOSLUGU = 14;
    private static final int YAZI_RENGI = 0xE6F2FF;
    private static final int ZEMIN_RENGI = 0x000000;
    private static final float ZEMIN_OPAKLIGI = 0.35f;
    private static final int YATAY_PAY = 5;
    private static final int DIKEY_PAY = 3;

    private RehberKatmani() {
    }

    @SubscribeEvent
    public static void katmanlariKaydet(RegisterGuiOverlaysEvent olay) {
        olay.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "rehber_yazisi", RehberKatmani::ciz);
    }

    private static void ciz(ForgeGui gui, GuiGraphics g, float araKare, int w, int h) {
        RehberMesaji mesaj = IstemciRehber.mesaj();
        if (mesaj == null || !EkranEfektKapisi.izinli()) {
            return;
        }
        float opaklik = KonusmaRitmi.opaklik(IstemciRehber.gecen(araKare), KonusmaRitmi.SURE_TICK);
        // Font alfası 4'ün altındaki rengi opak çizer; neredeyse görünmez yazı hiç çizilmez.
        int alfa = (int) (opaklik * 255f);
        if (alfa < 8) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Component metin = Component.translatable(mesaj.ceviriAnahtari());
        int genislik = font.width(metin);
        int alt = Math.max(Math.max(gui.leftHeight, gui.rightHeight) + BAR_BOSLUGU, EN_AZ_ALT);
        int x = (w - genislik) / 2;
        int y = h - alt - font.lineHeight;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.fill(x - YATAY_PAY, y - DIKEY_PAY, x + genislik + YATAY_PAY, y + font.lineHeight + DIKEY_PAY,
                ((int) (alfa * ZEMIN_OPAKLIGI) << 24) | ZEMIN_RENGI);
        g.drawString(font, metin, x, y, (alfa << 24) | YAZI_RENGI, true);
        RenderSystem.disableBlend();
    }
}
