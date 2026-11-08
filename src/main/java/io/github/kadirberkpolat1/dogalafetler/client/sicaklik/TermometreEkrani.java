package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.network.AgKanali;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDokumIstegi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDokumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikMenuCevirileri;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Islaklik;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SolEl;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.VucutOlcegi;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;

/**
 * Termometre menüsü (S3-08, sözleşme §A9). Soldan sağa: fareyi izleyen oyuncu modeli; vücut °C'si, eğilim oku, durum adı
 * ve 32–42 °C eşik bantlı ölçek; hissedilen °C, mekan °C ve rüzgâr; mekan termometresi (cam tüp + hazne, −30…+50 °C,
 * mekan ve hissedilen işaretçileri). Altında durum ikonları (etkin renkli, pasif gri; fareyle açıklama) ve iki sütun döküm:
 * mekan katkıları (|değer|e göre, tekerlekle kayar) ve hissedilen düzeltmeleri + yalıtım ve kuruma.
 *
 * <p>Oyunu durdurmaz. Açıkken 20 tick'te bir {@link SicaklikDokumIstegi} yollar; döküm gelene dek üst bölüm durum
 * paketinden ({@link IstemciSicaklik}) çizilir. Pano 320×214: Minecraft'ın izin verdiği her GUI ölçeğinde ekran en az
 * 320×240 olduğundan taşmaz. Sıcaklık kapısı kapanınca kendiliğinden kapanır.
 */
public final class TermometreEkrani extends Screen {
    static final ResourceLocation DOKU = DogalAfetler.konum("textures/gui/sicaklik/termometre.png");
    private static final int DOKU_G = 512;
    private static final int DOKU_Y = 256;
    static final int GENISLIK = 320;
    static final int YUKSEKLIK = 214;
    private static final int ISTEK_ARALIGI = 20;
    private static final String ON = SicaklikMenuCevirileri.ON;

    private static final int METIN = 0xFF404040;
    private static final int SOLUK = 0xFF7A7A7A;
    private static final int SICAK_METIN = 0xFFB03A12;
    private static final int SOGUK_METIN = 0xFF1C5AAE;

    /** Oyuncu kutusu (8,18 54×90; iç 9..61 × 19..107): model ortası ve ayak hizası. */
    private static final int MODEL_X = 35;
    private static final int MODEL_Y = 101;
    private static final int MODEL_OLCEK = 34;

    private static final int VUCUT_X = 70;
    private static final int HIS_X = 172;
    /** Vücut ölçeği: 32–42 °C (vücut biriminin tüm aralığı). */
    private static final int BAR_X0 = 84;
    private static final int BAR_X1 = 150;
    private static final int BAR_Y = 66;

    /** Termometre: tüpün sol üstü (12×80; sıvı içi x+3..x+9); ölçek −30 °C (y 92) … +50 °C (y 24). */
    private static final int TUP_X = 292;
    private static final int TUP_Y = 18;
    private static final float TERMO_ALT = -30;
    private static final float TERMO_UST = 50;
    private static final int TERMO_ALT_Y = 92;
    private static final int TERMO_UST_Y = 24;
    private static final float KONFOR_ALT = 12;
    private static final float KONFOR_UST = 28;

    private static final int IKON_Y = 88;
    private static final int[] IKON_X = {70, 112, 131, 150, 169};
    private static final String[] IKON_ANAHTAR = {"islak", "usume", "donma", "bunalma", "yanma"};

    private static final int SOL_X0 = 8;
    private static final int SOL_X1 = 160;
    private static final int SAG_X0 = 164;
    private static final int SAG_X1 = 312;
    private static final int BASLIK_Y = 119;
    private static final int DOKUM_Y = 132;
    private static final int SATIR = 9;
    private static final int GORUNEN_SATIR = 8;
    private static final int ALT_SATIR_Y = 196;

    /** Termometre sıvısının renk durakları (°C → RGB; Cold Sweat paletinden). */
    private static final float[] RENK_C = {-30, 0, 8, 20, 30, 40, 50};
    private static final int[] RENK = {0x2A5FD0, 0x40A2FF, 0x80D4FF, 0xE6E4DC, 0xF8AF35, 0xFF4019, 0xC0160C};

    private int sol;
    private int ust;
    private int sayac;
    private int kaydirma;
    private float mekanSivi = Float.NaN;
    private float mekanSiviOnceki;

    private TermometreEkrani() {
        super(Component.translatable(ON + "baslik"));
    }

    public static void ac() {
        IstemciDokum.temizle();
        Minecraft.getInstance().setScreen(new TermometreEkrani());
    }

    @Override
    protected void init() {
        sol = (width - GENISLIK) / 2;
        ust = (height - YUKSEKLIK) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!IstemciSicaklik.acik()) {
            onClose();
            return;
        }
        if (sayac++ % ISTEK_ARALIGI == 0) {
            AgKanali.sunucuyaGonder(new SicaklikDokumIstegi());
        }
        float hedef = Mth.clamp(gorunum().mekan, TERMO_ALT - 2, TERMO_UST + 2);
        if (Float.isNaN(mekanSivi)) {
            mekanSivi = hedef;
        }
        mekanSiviOnceki = mekanSivi;
        mekanSivi += (hedef - mekanSivi) * 0.3f;
    }

    @Override
    public boolean keyPressed(int tus, int tarama, int degistirici) {
        if (TermometreTusu.TUS.get().matches(tus, tarama)) {
            onClose();
            return true;
        }
        return super.keyPressed(tus, tarama, degistirici);
    }

    @Override
    public boolean mouseScrolled(double fareX, double fareY, double miktar) {
        if (icinde(fareX, fareY, SOL_X0, DOKUM_Y - 2, SOL_X1, ALT_SATIR_Y + SATIR)) {
            kaydirma -= (int) Math.signum(miktar);
            return true;
        }
        return super.mouseScrolled(fareX, fareY, miktar);
    }

    /** Bir karede çizilen değerler: döküm varsa ondan, yoksa durum paketinden. */
    private record Gorunum(float vucut, float egilim, float mekan, float hissedilen, float islaklik,
            SicaklikDurumu durum, SicaklikDurumPaketi esik, SicaklikDokumPaketi dokum) {
    }

    private static Gorunum gorunum() {
        SicaklikDurumPaketi d = IstemciSicaklik.durum();
        SicaklikDokumPaketi k = IstemciDokum.guncel();
        float vucut = IstemciSicaklik.vucut(1f);
        SicaklikDurumu durum = d.kademe().durum();
        if (k == null) {
            return new Gorunum(vucut, d.egilim(), d.mekan(), d.hissedilen(), d.islaklik(), durum, d, null);
        }
        return new Gorunum(vucut, k.egilim(), k.mekan(), k.hissedilen(), k.islaklik(), durum, d, k);
    }

    @Override
    public void render(GuiGraphics g, int fareX, int fareY, float pt) {
        renderBackground(g);
        Gorunum v = gorunum();
        RenderSystem.enableBlend();
        g.blit(DOKU, sol, ust, 0, 0, GENISLIK, YUKSEKLIK, DOKU_G, DOKU_Y);
        g.drawString(font, title, sol + 8, ust + 6, METIN, false);
        model(g, v, fareX, fareY);
        vucutSutunu(g, v, IstemciSicaklik.vucut(pt));
        hissedilenSutunu(g, v);
        termometre(g, v, Mth.lerp(pt, mekanSiviOnceki, mekanSivi));
        ikonlar(g, v);
        mekanDokumu(g, v);
        hissedilenDokumu(g, v);
        RenderSystem.disableBlend();
        super.render(g, fareX, fareY, pt);
        ipucu(g, v, fareX, fareY);
    }

    /** Fareyi izleyen oyuncu; donmada hafif mavi, yanmada hafif kızıl tonda. */
    private void model(GuiGraphics g, Gorunum v, int fareX, int fareY) {
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        int mx = sol + MODEL_X;
        int my = ust + MODEL_Y;
        g.enableScissor(sol + 9, ust + 19, sol + 61, ust + 107);
        if (v.durum == SicaklikDurumu.DONMA) {
            RenderSystem.setShaderColor(0.78f, 0.9f, 1f, 1f);
        } else if (v.durum == SicaklikDurumu.YANMA) {
            RenderSystem.setShaderColor(1f, 0.82f, 0.74f, 1f);
        }
        InventoryScreen.renderEntityInInventoryFollowsMouse(g, mx, my, MODEL_OLCEK, mx - fareX, my - 55 - fareY,
                minecraft.player);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.disableScissor();
    }

    private void vucutSutunu(GuiGraphics g, Gorunum v, float vucut) {
        int x = sol + VUCUT_X;
        g.drawString(font, Component.translatable(ON + "vucut"), x, ust + 20, METIN, false);
        int renk = durumRengi(v.durum);
        int w = buyukYaz(g, VucutOlcegi.metin(VucutOlcegi.santigrat(vucut)) + " °C", x, ust + 31, renk);
        int ok = okSayisi(v.egilim);
        int oy = ust + 32 + (3 - ok) * 2 + (int) (IstemciSicaklik.tick() / 6 % 2);
        for (int i = 0; i < ok; i++) {
            g.blit(DOKU, x + w + 4, oy + i * 5, 440, v.egilim > 0 ? 0 : 6, 7, 4, DOKU_G, DOKU_Y);
        }
        g.drawString(font, Component.translatable(ON + "durum." + v.durum.anahtar()), x, ust + 51, renk, true);

        SicaklikDurumPaketi e = v.esik;
        int y = ust + BAR_Y;
        g.drawString(font, "32", x, y - 1, METIN, false);
        g.drawString(font, "42", sol + BAR_X1 + 3, y - 1, METIN, false);
        int x0 = sol + BAR_X0;
        int x1 = sol + BAR_X1;
        g.fill(x0 - 1, y - 1, x1 + 1, y + 6, 0xFF202020);
        float[] sinir = {-100, e.donma(), e.cokSoguk(), e.soguk(), e.sicak(), e.cokSicak(), e.yanma(), 100};
        int[] bant = {0xFF1D3F9E, 0xFF2E6FD6, 0xFF8CCBF5, 0xFF79C27A, 0xFFF5A742, 0xFFE5532A, 0xFF9E1A12};
        for (int i = 0; i < bant.length; i++) {
            int a = barKonumu(sinir[i]);
            int b = barKonumu(sinir[i + 1]);
            if (b > a) {
                g.fill(a, y, b, y + 5, bant[i]);
            }
        }
        g.fill(barKonumu(-e.notrBant()), y + 1, barKonumu(e.notrBant()), y + 4, 0x55FFFFFF);
        g.fill(x0, y, x1, y + 1, 0x40FFFFFF);
        int m = Mth.clamp(barKonumu(vucut), x0, x1 - 1);
        g.fill(m - 1, y - 3, m + 2, y - 2, 0xFF101010);
        g.fill(m, y - 2, m + 1, y + 7, 0xFF101010);
        g.fill(m, y, m + 1, y + 5, 0xFFFFFFFF);
    }

    private void hissedilenSutunu(GuiGraphics g, Gorunum v) {
        int x = sol + HIS_X;
        g.drawString(font, Component.translatable(ON + "hissedilen"), x, ust + 20, METIN, false);
        int w = buyukYaz(g, tam(v.hissedilen) + " °C", x, ust + 31, sivi(v.hissedilen));
        g.blit(DOKU, x + w + 4, ust + 35, 428, 0, 6, 7, DOKU_G, DOKU_Y);
        g.blit(DOKU, x, ust + 51, 420, 0, 6, 7, DOKU_G, DOKU_Y);
        g.drawString(font, Component.translatable(ON + "mekan", tam(v.mekan) + " °C"), x + 9, ust + 51, METIN, false);
        if (v.dokum != null) {
            g.drawString(font, Component.translatable(ON + "ruzgar", tam(v.dokum.ruzgarKmh())), x + 9, ust + 63,
                    METIN, false);
        }
    }

    private void termometre(GuiGraphics g, Gorunum v, float sivi) {
        int tx = sol + TUP_X;
        int ty = ust + TUP_Y;
        for (int c = (int) TERMO_ALT; c <= TERMO_UST; c += 10) {
            int y = termoY(c);
            boolean etiket = c % 20 != 0;
            g.fill(tx - (etiket || c == 0 ? 3 : 2), y, tx, y + 1, c == 0 ? 0xFF2E6FD6 : 0xFF505050);
            if (etiket) {
                String s = (c > 0 ? "+" : "") + c;
                g.drawString(font, s, tx - 4 - font.width(s), y - 3, METIN, false);
            }
        }
        g.fill(tx - 1, termoY(KONFOR_UST), tx, termoY(KONFOR_ALT) + 1, 0xFF4CAF50);

        g.blit(DOKU, tx - 2, ust + 94, 360, 0, 16, 16, DOKU_G, DOKU_Y);
        g.blit(DOKU, tx, ty, 330, 0, 12, 80, DOKU_G, DOKU_Y);
        int renk = sivi(sivi);
        g.setColor((renk >> 16 & 0xFF) / 255f, (renk >> 8 & 0xFF) / 255f, (renk & 0xFF) / 255f, 1f);
        g.blit(DOKU, tx - 2, ust + 94, 378, 0, 16, 16, DOKU_G, DOKU_Y);
        g.setColor(1f, 1f, 1f, 1f);
        g.blit(DOKU, tx - 2, ust + 94, 396, 0, 16, 16, DOKU_G, DOKU_Y);
        g.fill(tx + 3, Math.round(termoYf(sivi)), tx + 9, ust + 100, renk);
        g.fill(tx + 3, Math.round(termoYf(sivi)), tx + 9, Math.round(termoYf(sivi)) + 1, 0x60FFFFFF);
        g.blit(DOKU, tx, ty, 344, 0, 12, 80, DOKU_G, DOKU_Y);

        int my = Mth.clamp(Math.round(termoYf(sivi)), ust + TERMO_UST_Y - 2, ust + TERMO_ALT_Y + 2);
        int hy = Mth.clamp(termoY(v.hissedilen), ust + TERMO_UST_Y - 2, ust + TERMO_ALT_Y + 2);
        g.blit(DOKU, tx + 13, my - 3, 420, 0, 6, 7, DOKU_G, DOKU_Y);
        g.blit(DOKU, tx + (Math.abs(hy - my) < 7 ? 19 : 13), hy - 3, 428, 0, 6, 7, DOKU_G, DOKU_Y);
    }

    private void ikonlar(GuiGraphics g, Gorunum v) {
        for (int i = 0; i < IKON_X.length; i++) {
            boolean etkin = ikonEtkin(i, v);
            int x = sol + IKON_X[i];
            int y = ust + IKON_Y;
            if (etkin) {
                g.fill(x - 1, y - 1, x + 17, y + 17, 0x30FFFFFF);
            }
            g.blit(DOKU, x, y, i * 16, etkin ? 224 : 240, 16, 16, DOKU_G, DOKU_Y);
        }
        g.drawString(font, yuzde(v.islaklik), sol + IKON_X[0] + 18, ust + IKON_Y + 4,
                Islaklik.gorunur(v.islaklik) ? SOGUK_METIN : SOLUK, false);
    }

    private void mekanDokumu(GuiGraphics g, Gorunum v) {
        g.drawString(font, kirp(Component.translatable(ON + "mekan_katkilari"), SOL_X1 - SOL_X0 - 8), sol + SOL_X0 + 4,
                ust + BASLIK_Y, METIN, false);
        if (v.dokum == null) {
            bekleniyor(g, SOL_X0, SOL_X1);
            return;
        }
        List<Katki> kalemler = new ArrayList<>();
        for (Katki k : Katki.values()) {
            float d = v.dokum.katki(k);
            if (!k.hissedilen() && !Float.isNaN(d) && Math.abs(d) >= 0.05f) {
                kalemler.add(k);
            }
        }
        kalemler.sort(Comparator.comparingDouble((Katki k) -> -Math.abs(v.dokum.katki(k))));
        int satir = GORUNEN_SATIR;
        kaydirma = Mth.clamp(kaydirma, 0, Math.max(0, kalemler.size() - satir));
        for (int i = 0; i < Math.min(satir, kalemler.size() - kaydirma); i++) {
            Katki k = kalemler.get(i + kaydirma);
            String ad = basHarfBuyuk(Component.translatable("sicaklik.dogalafetler.katki." + k.anahtar()).getString());
            satirYaz(g, Component.literal(ad), v.dokum.katki(k), SOL_X0, SOL_X1, ust + DOKUM_Y + i * SATIR);
        }
        if (kalemler.size() > satir) {
            int alan = satir * SATIR;
            int boy = Math.max(6, alan * satir / kalemler.size());
            int y0 = ust + DOKUM_Y - 1 + (alan - boy) * kaydirma / (kalemler.size() - satir);
            g.fill(sol + SOL_X1 - 3, ust + DOKUM_Y - 1, sol + SOL_X1 - 2, ust + DOKUM_Y - 1 + alan, 0x30000000);
            g.fill(sol + SOL_X1 - 3, y0, sol + SOL_X1 - 2, y0 + boy, 0xFF6A6A6A);
        }
    }

    private void hissedilenDokumu(GuiGraphics g, Gorunum v) {
        g.drawString(font, kirp(Component.translatable(ON + "hissedilen_duzeltmeleri"), SAG_X1 - SAG_X0 - 8),
                sol + SAG_X0 + 4, ust + BASLIK_Y, METIN, false);
        SicaklikDokumPaketi k = v.dokum;
        if (k == null) {
            bekleniyor(g, SAG_X0, SAG_X1);
            return;
        }
        int y = ust + DOKUM_Y;
        int n = 0;
        for (Katki katki : Katki.values()) {
            float d = k.katki(katki);
            if (!katki.hissedilen() || Float.isNaN(d) || Math.abs(d) < 0.05f) {
                continue;
            }
            Component ad = switch (katki) {
                case ISLAKLIK -> Component.translatable(ON + "duzeltme.islaklik", yuzde(k.islaklik()));
                case RUZGAR -> Component.translatable(ON + "duzeltme.ruzgar", tam(k.ruzgarKmh()));
                default -> Component.translatable(ON + "duzeltme.sol_el", solElAdi(k.solEl()));
            };
            satirYaz(g, ad, d, SAG_X0, SAG_X1, y + n++ * SATIR);
        }
        if (n == 0) {
            g.drawString(font, Component.translatable(ON + "duzeltme_yok"), sol + SAG_X0 + 4, y, SOLUK, false);
        }
        int ay = ust + ALT_SATIR_Y;
        g.fill(sol + SAG_X0 + 4, ay - SATIR - 3, sol + SAG_X1 - 4, ay - SATIR - 2, 0xFF9A9A9A);
        degerSatiri(g, Component.translatable(ON + "yalitim"),
                Component.literal(String.format(Locale.ROOT, "×%.1f", 1 + k.yalitim())), METIN, ay - SATIR);
        Component kuruma;
        int renk = METIN;
        if (!Islaklik.gorunur(k.islaklik())) {
            kuruma = Component.translatable(ON + "kuru");
        } else if (k.kurumaSaniye() < 0) {
            kuruma = Component.translatable(ON + "islaniyor");
            renk = SOGUK_METIN;
        } else {
            kuruma = sure(k.kurumaSaniye());
        }
        degerSatiri(g, Component.translatable(ON + "kuruma"), kuruma, renk, ay);
    }

    private void bekleniyor(GuiGraphics g, int x0, int x1) {
        Component m = Component.translatable(ON + "bekleniyor");
        g.drawString(font, m, sol + (x0 + x1 - font.width(m)) / 2, ust + DOKUM_Y + 3 * SATIR, SOLUK, false);
    }

    /** Ad solda (sığmazsa kırpılır), °C değeri sağa yaslı; sıcak katkı kızıl, soğuk mavi. */
    private void satirYaz(GuiGraphics g, Component ad, float deger, int x0, int x1, int y) {
        String d = String.format(Locale.ROOT, "%+.1f °C", deger);
        int dw = font.width(d);
        g.drawString(font, kirp(ad, x1 - x0 - dw - 14), sol + x0 + 4, y, METIN, false);
        g.drawString(font, d, sol + x1 - 6 - dw, y, deger > 0 ? SICAK_METIN : SOGUK_METIN, false);
    }

    private void degerSatiri(GuiGraphics g, Component ad, Component deger, int renk, int y) {
        int dw = font.width(deger);
        g.drawString(font, kirp(ad, SAG_X1 - SAG_X0 - dw - 14), sol + SAG_X0 + 4, y, METIN, false);
        g.drawString(font, deger, sol + SAG_X1 - 6 - dw, y, renk, false);
    }

    private void ipucu(GuiGraphics g, Gorunum v, int fareX, int fareY) {
        List<Component> satirlar = new ArrayList<>();
        for (int i = 0; i < IKON_X.length; i++) {
            if (icinde(fareX, fareY, IKON_X[i], IKON_Y, IKON_X[i] + 16, IKON_Y + 16)) {
                ikonIpucu(satirlar, i, v);
            }
        }
        if (icinde(fareX, fareY, TUP_X - 22, TUP_Y, TUP_X + 26, TUP_Y + 92)) {
            satirlar.add(Component.translatable(ON + "mekan", ondalik(v.mekan) + " °C"));
            satirlar.add(Component.translatable(ON + "hissedilen_deger", ondalik(v.hissedilen) + " °C"));
        }
        if (icinde(fareX, fareY, VUCUT_X, 30, HIS_X - 4, 48)) {
            String hiz = String.format(Locale.ROOT, "%+.2f", v.egilim * VucutOlcegi.DERECE_PER_BIRIM * 60);
            satirlar.add(Component.translatable(ON + "egilim", hiz));
        }
        if (icinde(fareX, fareY, VUCUT_X, BAR_Y - 4, BAR_X1 + 16, BAR_Y + 8)) {
            satirlar.add(Component.translatable(ON + "olcek", VucutOlcegi.metin(VucutOlcegi.EN_DUSUK_C),
                    VucutOlcegi.metin(VucutOlcegi.EN_YUKSEK_C)));
            for (int i = 1; i < IKON_ANAHTAR.length; i++) {
                satirlar.add(Component.translatable(ON + "durum." + IKON_ANAHTAR[i]).append(": ")
                        .append(esikMetni(i, v.esik)).withStyle(ChatFormatting.GRAY));
            }
        }
        if (satirlar.isEmpty()) {
            return;
        }
        List<FormattedCharSequence> bolunmus = new ArrayList<>();
        for (Component s : satirlar) {
            bolunmus.addAll(font.split(s, 190));
        }
        g.renderTooltip(font, bolunmus, fareX, fareY);
    }

    private void ikonIpucu(List<Component> satirlar, int i, Gorunum v) {
        boolean etkin = ikonEtkin(i, v);
        int renk = i == 0 ? 0x5AA8FF : durumRengi(SicaklikDurumu.values()[i]) & 0xFFFFFF;
        satirlar.add(Component.translatable(ON + "durum." + IKON_ANAHTAR[i]).withStyle(s -> s.withColor(renk)));
        satirlar.add(Component.translatable(ON + "aciklama." + IKON_ANAHTAR[i]).withStyle(ChatFormatting.GRAY));
        satirlar.add(Component.translatable(ON + (etkin ? "etkin" : "pasif"))
                .withStyle(etkin ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        if (i == 0) {
            Component kuruma;
            if (v.dokum == null) {
                kuruma = Component.literal("…");
            } else if (!Islaklik.gorunur(v.islaklik)) {
                kuruma = Component.translatable(ON + "kuru");
            } else if (v.dokum.kurumaSaniye() < 0) {
                kuruma = Component.translatable(ON + "islaniyor");
            } else {
                kuruma = Component.translatable(ON + "kuruma_kalan", sure(v.dokum.kurumaSaniye()));
            }
            satirlar.add(Component.translatable(ON + "islaklik_durum", yuzde(v.islaklik), kuruma)
                    .withStyle(ChatFormatting.AQUA));
        } else {
            satirlar.add(esikMetni(i, v.esik).copy().withStyle(ChatFormatting.YELLOW));
        }
    }

    /** İkon sırası {@link SicaklikDurumu} sırasıyla aynıdır (0 ıslaklık, NORMAL yerine). */
    private static boolean ikonEtkin(int i, Gorunum v) {
        return i == 0 ? Islaklik.gorunur(v.islaklik) : v.durum == SicaklikDurumu.values()[i];
    }

    /** Durumun başladığı vücut °C'si: üşüme ve donma alttan, bunalma ve yanma üstten. */
    private static Component esikMetni(int i, SicaklikDurumPaketi e) {
        return switch (i) {
            case 1 -> Component.translatable(ON + "esik.alt", santigrat(e.soguk()));
            case 2 -> Component.translatable(ON + "esik.alt", santigrat(e.cokSoguk()));
            case 3 -> Component.translatable(ON + "esik.ust", santigrat(e.sicak()));
            default -> Component.translatable(ON + "esik.ust", santigrat(e.cokSicak()));
        };
    }

    private static Component solElAdi(SolEl el) {
        return switch (el) {
            case BUZ_KOVASI -> Component.translatable("item.dogalafetler.buz_kovasi");
            case LAV_KOVASI -> Items.LAVA_BUCKET.getDescription();
            case YOK -> Component.literal("—");
        };
    }

    private static Component yuzde(float islaklik) {
        return Component.translatable(ON + "yuzde", Math.round(Mth.clamp(islaklik, 0f, 1f) * 100));
    }

    private static Component sure(float saniye) {
        int s = Math.max(1, (int) Math.ceil(saniye));
        return s >= 60 ? Component.translatable(ON + "sure.dk", s / 60, s % 60)
                : Component.translatable(ON + "sure.sn", s);
    }

    /** Çıktının genişliği (GUI px). */
    private int buyukYaz(GuiGraphics g, String metin, int x, int y, int renk) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(2f, 2f, 1f);
        g.drawString(font, metin, 0, 0, renk, true);
        g.pose().popPose();
        return font.width(metin) * 2;
    }

    private FormattedCharSequence kirp(Component metin, int genislik) {
        if (font.width(metin) <= genislik) {
            return metin.getVisualOrderText();
        }
        String s = font.plainSubstrByWidth(metin.getString(), genislik - font.width("…")) + "…";
        return Component.literal(s).withStyle(metin.getStyle()).getVisualOrderText();
    }

    private boolean icinde(double fx, double fy, int x0, int y0, int x1, int y1) {
        return fx >= sol + x0 && fx < sol + x1 && fy >= ust + y0 && fy < ust + y1;
    }

    private int barKonumu(float birim) {
        float f = (Mth.clamp(birim, -100f, 100f) + 100f) / 200f;
        return sol + BAR_X0 + Math.round(f * (BAR_X1 - BAR_X0));
    }

    private float termoYf(float c) {
        float f = (c - TERMO_ALT) / (TERMO_UST - TERMO_ALT);
        return ust + TERMO_ALT_Y - f * (TERMO_ALT_Y - TERMO_UST_Y);
    }

    private int termoY(float c) {
        return Math.round(termoYf(c));
    }

    private static int okSayisi(float egilim) {
        float a = Math.abs(egilim);
        return a >= 0.5f ? 3 : a >= 0.2f ? 2 : a >= 0.05f ? 1 : 0;
    }

    private static int durumRengi(SicaklikDurumu d) {
        return switch (d) {
            case NORMAL -> 0xFF8EE08E;
            case USUME -> 0xFFA6DDFF;
            case DONMA -> 0xFF5AA8FF;
            case BUNALMA -> 0xFFFFB347;
            case YANMA -> 0xFFFF5A36;
        };
    }

    /** Termometre sıvısının ve hissedilen sayısının rengi (ARGB). */
    static int sivi(float c) {
        if (c <= RENK_C[0]) {
            return 0xFF000000 | RENK[0];
        }
        for (int i = 1; i < RENK_C.length; i++) {
            if (c <= RENK_C[i]) {
                float t = (c - RENK_C[i - 1]) / (RENK_C[i] - RENK_C[i - 1]);
                int a = RENK[i - 1];
                int b = RENK[i];
                int r = Math.round(Mth.lerp(t, a >> 16 & 0xFF, b >> 16 & 0xFF));
                int gr = Math.round(Mth.lerp(t, a >> 8 & 0xFF, b >> 8 & 0xFF));
                int bl = Math.round(Mth.lerp(t, a & 0xFF, b & 0xFF));
                return 0xFF000000 | r << 16 | gr << 8 | bl;
            }
        }
        return 0xFF000000 | RENK[RENK.length - 1];
    }

    private static String santigrat(float birim) {
        return VucutOlcegi.metin(VucutOlcegi.santigrat(birim));
    }

    private static String tam(float c) {
        return Integer.toString(Math.round(c));
    }

    private static String ondalik(float c) {
        return String.format(Locale.ROOT, "%.1f", c);
    }

    /** İlk harf büyük (kalem adları {@code bilgi} komutunda cümle içi küçük harfle yazılır). */
    private static String basHarfBuyuk(String s) {
        if (s.isEmpty()) {
            return s;
        }
        int n = s.offsetByCodePoints(0, 1);
        return s.substring(0, n).toUpperCase(Locale.ROOT) + s.substring(n);
    }
}
