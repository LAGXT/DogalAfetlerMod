package io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf;

import io.github.kadirberkpolat1.dogalafetler.esya.mantik.HalkaTampon;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografCevirileri;
import io.github.kadirberkpolat1.dogalafetler.esya.sismograf.SismografPaketi;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Sismografın sağ tık ekranı (madde 13b): son 60 saniyenin sismik dalga grafiği. Krem renkli kayıt kağıdı tamburdan sola
 * akar (ızgara ve dalga aynı hızla kayar), sağ kenarda kalem kolu şimdiki örneği mürekkeple yazar (Hassas'ta mor). Her piksel sütunu 6
 * örneğin en küçük–en büyük aralığıdır (200 dolgu/kare; performansı etkilemez). Oyunu duraklatmaz: dalga açıkken de
 * akmaya devam eder.
 */
public final class SismografEkrani extends Screen {
    static final int GENISLIK = 236;
    static final int YUKSEKLIK = 146;
    private static final int KAGIT_G = 200;
    private static final int KAGIT_Y = 90;
    private static final int MUREKKEP = 0xFF1C2C6E;
    private static final int MUREKKEP_HASSAS = 0xFF5A2EA8;

    private final float[] enKucuk = new float[KAGIT_G];
    private final float[] enBuyuk = new float[KAGIT_G];

    public SismografEkrani(Component baslik) {
        super(baslik);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics g, int fareX, int fareY, float pt) {
        renderBackground(g);
        int x = (width - GENISLIK) / 2;
        int y = (height - YUKSEKLIK) / 2;
        g.fill(x - 1, y - 1, x + GENISLIK + 1, y + YUKSEKLIK + 1, 0xFF15171B);
        g.fill(x, y, x + GENISLIK, y + YUKSEKLIK, 0xFF2C3038);
        g.fill(x, y, x + GENISLIK, y + 1, 0xFF555C68);
        g.fill(x, y + YUKSEKLIK - 1, x + GENISLIK, y + YUKSEKLIK, 0xFF1E2127);
        g.drawString(font, title, x + 8, y + 6, 0xFFE9E1C9, false);

        SismografPaketi d = IstemciSismograf.durum();
        int lamba = d.algilandi() && (IstemciSismograf.tick() / 10) % 2 == 0 ? 0xFFFF5040 : 0xFF5A2A26;
        g.fill(x + GENISLIK - 14, y + 7, x + GENISLIK - 8, y + 13, lamba);

        int kx = x + 10;
        int ky = y + 20;
        g.fill(kx - 2, ky - 2, kx + KAGIT_G + 2, ky + KAGIT_Y + 2, 0xFF6B6150);
        g.fill(kx, ky, kx + KAGIT_G, ky + KAGIT_Y, 0xFFF4EEDC);
        float kayma = ((IstemciSismograf.tick() + pt) / 6f) % 10f;
        for (int i = 0; i <= KAGIT_G / 10; i++) {
            int gx = kx + KAGIT_G - 1 - Math.round(i * 10 + kayma);
            if (gx >= kx) {
                g.fill(gx, ky, gx + 1, ky + KAGIT_Y, 0xFFE1D6BA);
            }
        }
        for (int i = 1; i < KAGIT_Y / 10; i++) {
            g.fill(kx, ky + i * 10, kx + KAGIT_G, ky + i * 10 + 1, 0xFFE1D6BA);
        }
        float saniyeKayma = ((IstemciSismograf.tick() + pt) / 6f) % (KAGIT_G / 6f);
        for (int i = 0; i < 6; i++) {
            int gx = kx + KAGIT_G - 1 - Math.round(i * KAGIT_G / 6f + saniyeKayma);
            if (gx >= kx) {
                g.fill(gx, ky, gx + 1, ky + 3, 0xFFC0504A);
                g.fill(gx, ky + KAGIT_Y - 3, gx + 1, ky + KAGIT_Y, 0xFFC0504A);
            }
        }
        int orta = ky + KAGIT_Y / 2;
        g.fill(kx, orta, kx + KAGIT_G, orta + 1, 0xFFCDBF9C);

        HalkaTampon tampon = IstemciSismograf.tampon();
        tampon.sutunlar(KAGIT_G, enKucuk, enBuyuk);
        float genlik = KAGIT_Y / 2f - 4;
        int murekkep = d.hassas() ? MUREKKEP_HASSAS : MUREKKEP;
        float onceki = Float.NaN;
        for (int s = 0; s < KAGIT_G; s++) {
            if (Float.isNaN(enKucuk[s])) {
                continue;
            }
            float alt = enKucuk[s];
            float ust = enBuyuk[s];
            if (!Float.isNaN(onceki)) {
                alt = Math.min(alt, onceki);
                ust = Math.max(ust, onceki);
            }
            int y1 = Math.round(orta - ust * genlik);
            int y2 = Math.round(orta - alt * genlik) + 1;
            g.fill(kx + s, y1, kx + s + 1, Math.max(y1 + 1, y2), murekkep);
            onceki = (enKucuk[s] + enBuyuk[s]) / 2f;
        }
        int uc = Math.round(orta - tampon.son() * genlik);
        int kolX = kx + KAGIT_G + 6;
        for (int i = 0; i <= 8; i++) {
            int px = kolX - Math.round(i * 7 / 8f);
            int py = ky - 6 + Math.round((uc - (ky - 6)) * i / 8f);
            g.fill(px - 1, py, px + 1, py + 1, 0xFF9AA0AA);
        }
        g.fill(kolX - 2, ky - 9, kolX + 3, ky - 4, 0xFF7C828C);
        g.fill(kx + KAGIT_G - 2, uc - 1, kx + KAGIT_G + 1, uc + 2, 0xFFB02A2A);

        g.drawString(font, Component.translatable(SismografCevirileri.ON + "once"), kx, ky + KAGIT_Y + 4, 0xFF9C9686, false);
        Component simdi = Component.translatable(SismografCevirileri.ON + "simdi");
        g.drawString(font, simdi, kx + KAGIT_G - font.width(simdi), ky + KAGIT_Y + 4, 0xFF9C9686, false);
        Component durumMetni = d.algilandi() && minecraft != null && minecraft.player != null
                ? IstemciSismograf.metin(minecraft.player, d)
                : Component.translatable(SismografCevirileri.ON + "sakin");
        g.drawCenteredString(font, durumMetni, x + GENISLIK / 2, ky + KAGIT_Y + 16, 0xFFD8D2C0);
        super.render(g, fareX, fareY, pt);
    }
}
