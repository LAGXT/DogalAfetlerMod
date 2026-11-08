package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikIstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sıcaklık bantı (§A8, S3-07; N6 rev.): XP barının üstünde, onunla aynı genişlikte 11 px. Ortada vücut sıcaklığı hapı
 * ({@code 36.4°}, durum renginde), iki yanında merkezden dışa dolan soğuk (mavi → buz) ve sıcak (turuncu → kırmızı) yarım
 * tüpler, tüplerde eşik çentikleri ve altında hissedilen sıcaklığın içi boş üçgeni; solda etkin durum ikonları (ıslak
 * damla doluluk = ıslaklık, üşüme, donma, bunalma, yanma), sağda eğilim şevronları ve hissedilen °C. Animasyonlar: üşüme
 * ürpermesi ve donma titremesi, bunalma/yanma nabzı (hasara yaklaştıkça hızlanır), durum değişim parlaması, kayan şevron,
 * damlayan ıslak ikon. Kalpler ve açlık barı Forge'un {@code leftHeight/rightHeight} yüksekliğiyle bantın üstüne kayar.
 * Bant ve kaydırma yalnız sunucunun sıcaklık kapısı açıkken ({@code SicaklikKapisi}, S-12): Cold Sweat sunucuda yüklüyken
 * kapı kapalıdır, bant çizilmez ve vanilla barlar kaydırılmaz.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SicaklikHud {
    static final ResourceLocation DOKU = DogalAfetler.konum("textures/gui/sicaklik/hud.png");
    private static final int DOKU_G = 256;
    private static final int DOKU_Y = 64;
    /** Atlas: soğuk tüp (0,0) dolgusu (0,5); sıcak tüp (80,0) dolgusu (80,5); haplar v=16; ikonlar v=32; şevron v=48. */
    private static final int SICAK_TUP_U = 80;
    private static final int HAP_V = 16;
    private static final int HAP_ADIM = 36;
    private static final int BEYAZ_HAP_U = 180;
    private static final int IKON_V = 32;
    private static final int DOLU_DAMLA_U = 9;
    private static final int NORMAL_IKON_U = 54;
    private static final int SEVRON_V = 48;
    private static final int UCGEN_U = 12;
    /** XP barının üstü ekranın altından 29 px; bantın altı 36 px (aradaki 7 px XP seviye sayısınındır), üstü 47 px. */
    private static final int ALTTAN = 47;
    /** Kalplerin altı ile bantın tepesi arası (vanilla düşük canda kalpleri 1 px aşağı titretir). */
    private static final int KALP_BOSLUGU = 2;

    /** Son çizilen bantın görsel kanıt alanları (otomasyon log'a yazar). */
    static int sonDx;
    static int sonDy;
    static long sonTick = -1;
    static float sonParlama;
    static float sonNabiz;
    static int sonSevron;
    static String sonMetin = "";
    static String sonIkonlar = "";
    static int sonUcgen;
    /** Son çizilen hapın sol üst köşesi (GUI pikseli; görsel kanıt ölçümü). */
    static int sonHapX;
    static int sonHapY;

    private SicaklikHud() {
    }

    @SubscribeEvent
    public static void katmanlariKaydet(RegisterGuiOverlaysEvent olay) {
        olay.registerBelow(VanillaGuiOverlay.PLAYER_HEALTH.id(), "sicaklik_bari", SicaklikHud::ciz);
    }

    private static void ciz(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        if (!IstemciSicaklik.acik() || mc.options.hideGui || !gui.shouldDrawSurvivalElements()
                || !SicaklikIstemciAyarlari.hudGorunur()) {
            return;
        }
        SicaklikDurumPaketi d = IstemciSicaklik.durum();
        SicaklikDurumu durum = d.kademe().durum();
        boolean animasyon = SicaklikIstemciAyarlari.animasyonlar();
        long t = IstemciSicaklik.tick();
        float zaman = t + partialTick;
        int yOfset = SicaklikIstemciAyarlari.hudYOfset();
        int gerekli = ALTTAN - yOfset + 9 + KALP_BOSLUGU;
        gui.leftHeight = Math.max(gui.leftHeight, gerekli);
        gui.rightHeight = Math.max(gui.rightHeight, gerekli);

        int[] titreme = animasyon ? HudCizimi.titreme(durum, t) : new int[] {0, 0};
        int x = (w - HudCizimi.GENISLIK) / 2 + SicaklikIstemciAyarlari.hudXOfset() + titreme[0];
        int y = h - ALTTAN + yOfset + titreme[1];
        float vucut = IstemciSicaklik.vucut(partialTick);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (SicaklikIstemciAyarlari.durumIkonlari()) {
            ikonlar(g, x, y, durum, IstemciSicaklik.islaklik(partialTick), animasyon, t);
        } else {
            sonIkonlar = "";
        }
        tupler(g, x, y, vucut, d);
        float nabiz = animasyon ? HudCizimi.nabiz(durum, vucut, d.sicak(), d.yanma(), zaman) : 0f;
        float parlama = animasyon ? HudCizimi.parlama(t, IstemciSicaklik.durumDegisimi(), partialTick) : 0f;
        hap(g, mc.font, x, y, vucut, durum, nabiz, parlama);
        int sevron = HudCizimi.sevron(d.egilim());
        sevronlar(g, x, y, sevron, animasyon, zaman);
        if (SicaklikIstemciAyarlari.hissedilenGoster()) {
            hissedilen(g, mc.font, x, y, sevron, partialTick);
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        sonDx = titreme[0];
        sonDy = titreme[1];
        sonTick = t;
        sonParlama = parlama;
        sonNabiz = nabiz;
        sonSevron = sevron;
    }

    private static void parca(GuiGraphics g, int x, int y, int u, int v, int gen, int yuk) {
        g.blit(DOKU, x, y, u, v, gen, yuk, DOKU_G, DOKU_Y);
    }

    private static void ikonlar(GuiGraphics g, int x, int y, SicaklikDurumu durum, float islaklik, boolean animasyon,
            long t) {
        List<HudCizimi.Ikon> liste = HudCizimi.ikonlar(durum, islaklik);
        int ix = x - 2 - liste.size() * HudCizimi.IKON_ARALIGI;
        int iy = y + 1;
        StringBuilder kayit = new StringBuilder();
        for (HudCizimi.Ikon ikon : liste) {
            parca(g, ix, iy, ikon.u, IKON_V, HudCizimi.IKON, HudCizimi.IKON);
            if (ikon == HudCizimi.Ikon.ISLAK) {
                int dolu = HudCizimi.damlaDolulugu(islaklik);
                parca(g, ix, iy + HudCizimi.IKON - dolu, DOLU_DAMLA_U, IKON_V + HudCizimi.IKON - dolu, HudCizimi.IKON,
                        dolu);
                int faz = (int) (t % 32);
                if (animasyon && faz < 9) {
                    int alfa = 0xCC - faz * 0x14;
                    g.fill(ix + 4, iy + HudCizimi.IKON + faz / 3, ix + 5, iy + HudCizimi.IKON + faz / 3 + 1,
                            alfa << 24 | 0x5FB4FF);
                }
            }
            kayit.append(kayit.length() == 0 ? "" : ",").append(ikon.name());
            ix += HudCizimi.IKON_ARALIGI;
        }
        sonIkonlar = kayit.toString();
    }

    private static void tupler(GuiGraphics g, int x, int y, float vucut, SicaklikDurumPaketi d) {
        int ty = y + HudCizimi.TUP_Y;
        parca(g, x, ty, 0, 0, HudCizimi.YARIM, HudCizimi.TUP_YUKSEKLIK);
        parca(g, x + HudCizimi.SICAK_X, ty, SICAK_TUP_U, 0, HudCizimi.YARIM, HudCizimi.TUP_YUKSEKLIK);
        int[] dolgu = HudCizimi.dolguAraligi(vucut);
        if (dolgu[1] > dolgu[0]) {
            int u = vucut < 0 ? dolgu[0] : SICAK_TUP_U + dolgu[0] - HudCizimi.SICAK_X;
            parca(g, x + dolgu[0], ty + 1, u, HudCizimi.TUP_YUKSEKLIK + 1, dolgu[1] - dolgu[0], 3);
        }
        // Durum eşikleri ince açık, hasar eşikleri tüpü aşan kırmızı çentik.
        centik(g, x, ty, d.soguk(), false);
        centik(g, x, ty, d.cokSoguk(), false);
        centik(g, x, ty, d.donma(), true);
        centik(g, x, ty, d.sicak(), false);
        centik(g, x, ty, d.cokSicak(), false);
        centik(g, x, ty, d.yanma(), true);
    }

    private static void centik(GuiGraphics g, int x, int ty, float esik, boolean hasar) {
        int cx = x + HudCizimi.centik(esik);
        if (hasar) {
            g.fill(cx, ty - 1, cx + 1, ty + HudCizimi.TUP_YUKSEKLIK + 1, 0xE0FF3B30);
        } else {
            g.fill(cx, ty + 1, cx + 1, ty + HudCizimi.TUP_YUKSEKLIK - 1, 0x99FFFFFF);
        }
    }

    /** Hap, nabız ve parlama katmanları, en üstte koyu konturlu açık renk sayı (her durumda okunur, S3-10). */
    private static void hap(GuiGraphics g, Font font, int x, int y, float vucut, SicaklikDurumu durum, float nabiz,
            float parlama) {
        int hx = x + HudCizimi.HAP_X;
        sonHapX = hx;
        sonHapY = y;
        parca(g, hx, y, HudCizimi.hapSirasi(durum) * HAP_ADIM, HAP_V, HudCizimi.HAP_G, HudCizimi.YUKSEKLIK);
        int renk = HudCizimi.renk(durum);
        if (nabiz > 0f) {
            g.setColor((renk >> 16 & 0xFF) / 255f, (renk >> 8 & 0xFF) / 255f, (renk & 0xFF) / 255f, nabiz);
            parca(g, hx, y, BEYAZ_HAP_U, HAP_V, HudCizimi.HAP_G, HudCizimi.YUKSEKLIK);
            g.setColor(1f, 1f, 1f, 1f);
        }
        if (parlama > 0f) {
            g.setColor(1f, 1f, 1f, parlama);
            parca(g, hx, y, BEYAZ_HAP_U, HAP_V, HudCizimi.HAP_G, HudCizimi.YUKSEKLIK);
            g.setColor(1f, 1f, 1f, 1f);
        }
        if (SicaklikIstemciAyarlari.sayiGoster()) {
            String metin = HudCizimi.sayiMetni(vucut);
            int mx = hx + (HudCizimi.HAP_G - font.width(metin)) / 2 + 1;
            int my = y + 2;
            g.drawString(font, metin, mx - 1, my, HudCizimi.KONTUR, false);
            g.drawString(font, metin, mx + 1, my, HudCizimi.KONTUR, false);
            g.drawString(font, metin, mx, my - 1, HudCizimi.KONTUR, false);
            g.drawString(font, metin, mx, my + 1, HudCizimi.KONTUR, false);
            g.drawString(font, metin, mx, my, HudCizimi.sayiRengi(durum), false);
            sonMetin = metin;
        } else {
            HudCizimi.Ikon ikon = HudCizimi.durumIkonu(durum);
            parca(g, hx + (HudCizimi.HAP_G - HudCizimi.IKON) / 2, y + 1, ikon == null ? NORMAL_IKON_U : ikon.u, IKON_V,
                    HudCizimi.IKON, HudCizimi.IKON);
            sonMetin = "";
        }
    }

    /** Soğuma aşağı bakan mavi, ısınma yukarı bakan turuncu şevronlar; hareket yönünde kayarak söner. */
    private static void sevronlar(GuiGraphics g, int x, int y, int sevron, boolean animasyon, float zaman) {
        boolean asagi = sevron < 0;
        for (int i = 0; i < Math.abs(sevron); i++) {
            int kayma = animasyon ? HudCizimi.sevronKaymasi(i, zaman) : 0;
            int sy = asagi ? y + 2 + kayma : y + 5 - kayma;
            g.setColor(1f, 1f, 1f, 1f - kayma * 0.25f);
            parca(g, x + HudCizimi.GENISLIK + 3 + i * 6, sy, asagi ? 0 : 6, SEVRON_V, 5, 4);
        }
        g.setColor(1f, 1f, 1f, 1f);
    }

    private static void hissedilen(GuiGraphics g, Font font, int x, int y, int sevron, float partialTick) {
        int ux = x + HudCizimi.ucgen(IstemciSicaklik.ortam(partialTick));
        parca(g, ux - 2, y + HudCizimi.TUP_Y + HudCizimi.TUP_YUKSEKLIK, UCGEN_U, SEVRON_V, 5, 3);
        sonUcgen = ux - x;
        float c = IstemciSicaklik.hissedilen(partialTick);
        int tx = x + HudCizimi.GENISLIK + 3 + (sevron == 0 ? 0 : Math.abs(sevron) * 6 + 1);
        g.drawString(font, HudCizimi.hissedilenMetni(c), tx, y + 2, HudCizimi.hissedilenRengi(c), true);
    }
}
