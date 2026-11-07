package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikIstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikKapisi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Kademe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sıcaklık barı (madde 7d): XP barının hemen üstünde, onunla aynı genişlikte; mavi → nötr → kırmızı renk geçişi, üstte
 * vücut işaretçisi, altta ortam oku. Hasar kademesinde bar titrer ve işaretçi uyarı renginde yanıp söner. Kalpler ve
 * açlık barı Forge'un {@code leftHeight/rightHeight} yüksekliğiyle barın üstüne kayar. Yazı yoktur (N6). Cold Sweat
 * istemcide yüklüyse (sunucu kapısı açık kalsa da, ör. {@code sicaklikSistemiModu=ACIK} ya da Cold Sweat'siz sunucu) bar
 * çizilmez ve vanilla barlar kaydırılmaz: Cold Sweat kendi göstergesini çizer.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SicaklikHud {
    static final ResourceLocation DOKU = DogalAfetler.konum("textures/gui/sicaklik/bar.png");
    private static final int DOKU_G = 256;
    private static final int DOKU_Y = 32;
    static final int GENISLIK = 182;
    private static final int YUKSEKLIK = 5;
    /** XP barının üstü ekranın altından 29 px; barın üstü 41 px (aradaki 7 px XP seviye sayısınındır). */
    private static final int ALTTAN = 41;
    /** İşaretçi barın 4 px üstünden başlar; kalpler işaretçinin üstünde kalsın. */
    private static final int ISARETCI_UST = 4;
    /** Kalplerin altı ile işaretçinin tepesi arası (vanilla düşük canda kalpleri 1 px aşağı titretir). */
    private static final int KALP_BOSLUGU = 2;

    /** Son çizilen barın titreşim ofseti (GUI px), yanık satırı ve istemci tick'i: görsel kanıt otomasyonu log'a yazar. */
    static int sonDx;
    static int sonDy;
    static boolean sonYanik;
    static long sonTick = -1;

    private SicaklikHud() {
    }

    @SubscribeEvent
    public static void katmanlariKaydet(RegisterGuiOverlaysEvent olay) {
        olay.registerBelow(VanillaGuiOverlay.PLAYER_HEALTH.id(), "sicaklik_bari", SicaklikHud::ciz);
    }

    private static void ciz(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!IstemciSicaklik.acik() || mc.options.hideGui || !gui.shouldDrawSurvivalElements()
                || !SicaklikIstemciAyarlari.hudGorunur() || SicaklikKapisi.coldSweatYuklu()) {
            return;
        }
        SicaklikDurumPaketi d = IstemciSicaklik.durum();
        long t = IstemciSicaklik.tick();
        int yOfset = SicaklikIstemciAyarlari.hudYOfset();
        int x = (w - GENISLIK) / 2 + SicaklikIstemciAyarlari.hudXOfset();
        int y = h - ALTTAN + yOfset;
        int gerekli = ALTTAN - yOfset + ISARETCI_UST + 9 + KALP_BOSLUGU;
        gui.leftHeight = Math.max(gui.leftHeight, gerekli);
        gui.rightHeight = Math.max(gui.rightHeight, gerekli);

        float vucut = IstemciSicaklik.vucut(partialTick);
        Kademe kademe = d.kademe();
        boolean tehlike = kademe.hasarli();
        boolean uyari = tehlike || kademe == Kademe.COK_SOGUK || kademe == Kademe.COK_SICAK;
        int dx = 0;
        int dy = 0;
        if (tehlike) {
            dx = (int) Math.floorMod(t / 2 * 7 + 3, 3) - 1;
            dy = (int) Math.floorMod(t / 2 * 5 + 1, 3) - 1;
        } else if (uyari && (t / 3) % 4 == 0) {
            dx = (t / 3) % 8 == 0 ? 1 : -1;
        }
        x += dx;
        y += dy;
        boolean yanik = tehlike && (t / 8) % 2 == 0;
        sonDx = dx;
        sonDy = dy;
        sonYanik = yanik;
        sonTick = t;

        RenderSystem.enableBlend();
        g.blit(DOKU, x, y, 0, yanik ? 8 : 0, GENISLIK, YUKSEKLIK, DOKU_G, DOKU_Y);
        int bant = Math.round(d.notrBant() / 200f * (GENISLIK - 2));
        int orta = x + GENISLIK / 2;
        g.fill(orta - bant, y + 1, orta - bant + 1, y + YUKSEKLIK - 1, 0x66FFFFFF);
        g.fill(orta + bant, y + 1, orta + bant + 1, y + YUKSEKLIK - 1, 0x66FFFFFF);

        if (SicaklikIstemciAyarlari.ortamOkuGoster()) {
            int ox = konum(x, IstemciSicaklik.ortam(partialTick));
            g.setColor(1f, 1f, 1f, 0.85f);
            g.blit(DOKU, ox - 2, y + YUKSEKLIK, 208, 0, 5, 3, DOKU_G, DOKU_Y);
            g.setColor(1f, 1f, 1f, 1f);
        }
        int vx = konum(x, vucut);
        int u = 184;
        if (uyari && (!tehlike || yanik)) {
            u = vucut < 0 ? 192 : 200;
        }
        g.blit(DOKU, vx - 3, y - ISARETCI_UST, u, 0, 7, 9, DOKU_G, DOKU_Y);
        RenderSystem.disableBlend();
    }

    /** Vücut birimini (−100…+100) barın iç piksel konumuna çevirir. */
    private static int konum(int barX, float deger) {
        float f = (Mth.clamp(deger, -100f, 100f) + 100f) / 200f;
        return barX + 1 + Math.round(f * (GENISLIK - 3));
    }
}
