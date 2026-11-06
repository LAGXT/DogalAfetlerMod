package io.github.kadirberkpolat1.dogalafetler.client.esya.radar;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.HavaRadari;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarCevirileri;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarIstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarPaketi;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.GostergeYerlesimi;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.RadarGorunumu;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.SureBicimi;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.YonMesafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hava Radarı göstergesi (madde 13a; N6'nın istisnası: yalnız radar elde tutulurken). Küçük bir CRT paneli: solda dönen
 * taramalı radar kapı (afet merkezi bakış yönüne göre bir nokta olarak), sağda afetin adı, yön oku ve mesafe, varış ya da
 * kalan süre. Güneş patlaması ve ay çarpması sirenden sonra mor bir "gökyüzü anomalisi" ve geri sayım olarak görünür
 * (N7). Gelişmiş radar mor-mavi çerçevelidir ve altına şiddet seviyesi, hareket yönü/hızı ve son 5 afeti ekler. Yer
 * CLIENT config'tedir; alt köşelerde hotbar, kalpler ve sıcaklık barıyla çakışmaz ({@link GostergeYerlesimi}).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RadarGostergesi {
    private static final int R = 16;
    private static final int KAP = 2 * R + 1;
    private static final int IC = 4;
    private static final int SATIR = 10;

    private static final int ZEMIN = 0xD80A140D;
    private static final int KAP_ZEMIN = 0xFF04120A;
    private static final int HALKA = 0xFF1E5C30;
    private static final int TARAMA = 0xFF63FF86;
    private static final int YAZI = 0xFFC6FFD2;
    private static final int SOLUK = 0xFF7FAF8A;
    private static final int CERCEVE = 0xFF2F7A45;
    private static final int CERCEVE_GELISMIS = 0xFF7A63E0;
    private static final int VURGU_GELISMIS = 0xFF4FA3FF;
    private static final int ANOMALI = 0xFFD7A6FF;
    private static final int[] SEVIYE_RENGI = {0xFFB0B0B0, 0xFF9BE15D, 0xFFF2D04B, 0xFFF29B38, 0xFFF25C38, 0xFFE8303A};

    /** Son çizilen panel (GUI pikseli) ve ekran boyutu: görsel kanıt otomasyonu çakışma denetimini log'a yazar. */
    static int sonX = -1;
    static int sonY;
    static int sonG;
    static int sonY2;
    static int sonEkranG;
    static int sonEkranY;

    private RadarGostergesi() {
    }

    @SubscribeEvent
    public static void katmanlariKaydet(RegisterGuiOverlaysEvent olay) {
        olay.registerBelow(VanillaGuiOverlay.CHAT_PANEL.id(), "hava_radari", RadarGostergesi::ciz);
    }

    private static void ciz(ForgeGui gui, GuiGraphics g, float pt, int w, int h) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer oyuncu = mc.player;
        RadarPaketi d = IstemciRadar.durum();
        if (oyuncu == null || mc.options.hideGui || !d.elde() || HavaRadari.eldeki(oyuncu) == null
                || !RadarIstemciAyarlari.gostergeGorunur()) {
            sonX = -1;
            return;
        }
        Font font = mc.font;
        float zaman = IstemciRadar.tick() + pt;
        List<Component> satirlar = new ArrayList<>();
        List<Integer> renkler = new ArrayList<>();
        double goreli = 0;
        double mesafe = 0;
        if (d.gorunum() == RadarGorunumu.AFET) {
            double mx = d.merkezX() + 0.5;
            double mz = d.merkezZ() + 0.5;
            mesafe = YonMesafe.mesafe(oyuncu.getX(), oyuncu.getZ(), mx, mz);
            goreli = YonMesafe.goreliAci(YonMesafe.pusulaAcisi(oyuncu.getX(), oyuncu.getZ(), mx, mz), oyuncu.getYRot());
            ekle(satirlar, renkler, Component.translatable(d.tur().ceviriAnahtari()), YAZI);
            ekle(satirlar, renkler, Component.translatable(RadarCevirileri.ON + "mesafe", Math.round(mesafe)), YAZI);
            ekle(satirlar, renkler, sure(d), SOLUK);
        } else if (d.gorunum() == RadarGorunumu.ANOMALI) {
            ekle(satirlar, renkler, Component.translatable(RadarCevirileri.ON + "anomali"), ANOMALI);
            ekle(satirlar, renkler, Component.translatable(d.tur().ceviriAnahtari()), 0xFFB79BD6);
            ekle(satirlar, renkler, sure(d), ANOMALI);
        } else {
            ekle(satirlar, renkler, Component.translatable(RadarCevirileri.ON + "sakin"), SOLUK);
            ekle(satirlar, renkler, Component.translatable(RadarCevirileri.ON + "menzil", d.menzil()), 0xFF5E8A69);
        }
        List<Component> alt = new ArrayList<>();
        List<Integer> altRenk = new ArrayList<>();
        boolean okluHareket = false;
        if (d.gelismis()) {
            if (d.gorunum() == RadarGorunumu.AFET && d.seviye() > 0) {
                ekle(alt, altRenk, Component.translatable(RadarCevirileri.ON + "seviye",
                        Component.translatable("seviye.dogalafetler." + d.seviye())), SEVIYE_RENGI[Math.min(5, d.seviye())]);
            }
            if (d.gorunum() == RadarGorunumu.AFET) {
                okluHareket = !Float.isNaN(d.hareketYonu());
                ekle(alt, altRenk, okluHareket ? Component.translatable(RadarCevirileri.ON + "hiz",
                        String.format(Locale.ROOT, "%.1f", d.hiz())) : Component.translatable(RadarCevirileri.ON + "durgun"),
                        SOLUK);
            }
            ekle(alt, altRenk, Component.translatable(RadarCevirileri.ON + "gecmis"), VURGU_GELISMIS);
            if (d.gecmis().isEmpty()) {
                ekle(alt, altRenk, Component.translatable(RadarCevirileri.ON + "gecmis_bos"), 0xFF5E8A69);
            }
            for (RadarPaketi.Gecmis kayit : d.gecmis()) {
                Component ad = Component.translatable(kayit.tur().ceviriAnahtari());
                Component satir = kayit.seviye() > 0
                        ? Component.empty().append(ad).append(" · ").append(Component.translatable("seviye.dogalafetler."
                                + kayit.seviye())).append(" · ").append(yas(kayit.yasDakika()))
                        : Component.empty().append(ad).append(" · ").append(yas(kayit.yasDakika()));
                ekle(alt, altRenk, satir, kayit.seviye() > 0 ? SEVIYE_RENGI[Math.min(5, kayit.seviye())] : 0xFFB79BD6);
            }
        }

        int yaziX = IC + KAP + 6;
        int okPay = d.gorunum() == RadarGorunumu.AFET ? 12 : 0;
        int enGenis = 0;
        for (int i = 0; i < satirlar.size(); i++) {
            enGenis = Math.max(enGenis, font.width(satirlar.get(i)) + (i == 1 ? okPay : 0));
        }
        int pw = Math.max(yaziX + enGenis + IC + 2, 104);
        for (int i = 0; i < alt.size(); i++) {
            pw = Math.max(pw, IC + font.width(alt.get(i)) + (i == (d.seviye() > 0 ? 1 : 0) && okluHareket ? 12 : 0) + IC + 2);
        }
        int ph = IC + KAP + IC + (alt.isEmpty() ? 0 : 3 + alt.size() * SATIR);
        GostergeYerlesimi.Konum k = GostergeYerlesimi.konum(w, h, pw, ph, RadarIstemciAyarlari.kose(),
                RadarIstemciAyarlari.xOfset(), RadarIstemciAyarlari.yOfset());
        int x = k.x();
        int y = k.y();
        sonX = x;
        sonY = y;
        sonG = pw;
        sonY2 = ph;
        sonEkranG = w;
        sonEkranY = h;

        int cerceve = d.gelismis() ? CERCEVE_GELISMIS : CERCEVE;
        g.fill(x, y, x + pw, y + ph, ZEMIN);
        g.fill(x, y, x + pw, y + 1, cerceve);
        g.fill(x, y + ph - 1, x + pw, y + ph, cerceve);
        g.fill(x, y, x + 1, y + ph, cerceve);
        g.fill(x + pw - 1, y, x + pw, y + ph, cerceve);
        if (d.gelismis()) {
            g.fill(x + 1, y + 1, x + pw - 1, y + 2, VURGU_GELISMIS & 0x80FFFFFF);
        }

        int cx = x + IC + R;
        int cy = y + IC + R;
        kap(g, cx, cy, d, zaman, goreli, mesafe);

        int ty = y + IC + 1 + (KAP - satirlar.size() * SATIR) / 2;
        for (int i = 0; i < satirlar.size(); i++) {
            int sx = x + yaziX;
            if (i == 1 && okPay > 0) {
                ok(g, sx + 4, ty + i * SATIR + 4, goreli, YAZI);
                sx += okPay;
            }
            g.drawString(font, satirlar.get(i), sx, ty + i * SATIR, renkler.get(i), true);
        }
        if (!alt.isEmpty()) {
            int ay = y + IC + KAP + IC;
            g.fill(x + IC, ay, x + pw - IC, ay + 1, (d.gelismis() ? VURGU_GELISMIS : HALKA) & 0x90FFFFFF);
            ay += 3;
            for (int i = 0; i < alt.size(); i++) {
                int sx = x + IC;
                if (okluHareket && i == (d.seviye() > 0 ? 1 : 0)) {
                    double hareketGoreli = YonMesafe.goreliAci(d.hareketYonu(), oyuncu.getYRot());
                    ok(g, sx + 4, ay + i * SATIR + 4, hareketGoreli, VURGU_GELISMIS);
                    sx += 12;
                }
                g.drawString(font, alt.get(i), sx, ay + i * SATIR, altRenk.get(i), true);
            }
        }
    }

    private static void ekle(List<Component> satirlar, List<Integer> renkler, Component satir, int renk) {
        satirlar.add(satir);
        renkler.add(renk);
    }

    private static Component sure(RadarPaketi d) {
        String metin = SureBicimi.bicim(IstemciRadar.kalanSn());
        String anahtar;
        if (d.asama() == AfetAsamasi.AKTIF) {
            anahtar = "kalan";
        } else {
            anahtar = d.gorunum() == RadarGorunumu.ANOMALI ? "basliyor" : "varis";
        }
        return Component.translatable(RadarCevirileri.ON + anahtar, metin);
    }

    private static Component yas(int dakika) {
        if (dakika < 1) {
            return Component.translatable(RadarCevirileri.ON + "yas.simdi");
        }
        if (dakika < 60) {
            return Component.translatable(RadarCevirileri.ON + "yas.dk", dakika);
        }
        if (dakika < 1440) {
            return Component.translatable(RadarCevirileri.ON + "yas.sa", dakika / 60);
        }
        return Component.translatable(RadarCevirileri.ON + "yas.gun", dakika / 1440);
    }

    /** Dönen taramalı radar kapı. Afet noktası bakışa göre (yukarı = ön) ve menzile oranla yerleşir. */
    private static void kap(GuiGraphics g, int cx, int cy, RadarPaketi d, float zaman, double goreli, double mesafe) {
        boolean anomali = d.gorunum() == RadarGorunumu.ANOMALI;
        int tarama = anomali ? 0xFFC58BFF : TARAMA;
        int halka = anomali ? 0xFF4B2C73 : HALKA;
        for (int dy = -R; dy <= R; dy++) {
            int yari = (int) Math.floor(Math.sqrt(R * R - dy * dy + 0.5));
            g.fill(cx - yari, cy + dy, cx + yari + 1, cy + dy + 1, KAP_ZEMIN);
        }
        if (anomali) {
            float nabiz = (float) (0.5 + 0.5 * Math.sin(zaman * 0.15));
            int a = (int) (40 + 60 * nabiz);
            for (int dy = -R + 2; dy <= R - 2; dy++) {
                int yari = (int) Math.floor(Math.sqrt((R - 2) * (R - 2) - dy * dy + 0.5));
                g.fill(cx - yari, cy + dy, cx + yari + 1, cy + dy + 1, (a << 24) | 0x7A3BC8);
            }
            halkaCiz(g, cx, cy, (zaman * 0.35f) % R, 0x90E0B8FF);
        }
        halkaCiz(g, cx, cy, R, halka | 0xFF000000);
        halkaCiz(g, cx, cy, R * 0.5f, halka & 0xB0FFFFFF);
        g.fill(cx, cy - R + 1, cx + 1, cy + R, halka & 0x60FFFFFF);
        g.fill(cx - R + 1, cy, cx + R, cy + 1, halka & 0x60FFFFFF);
        double aci = (zaman * 6.0) % 360.0;
        for (int iz = 5; iz >= 0; iz--) {
            int alfa = 255 - iz * 42;
            double a = Math.toRadians(aci - iz * 6);
            int renk = (alfa << 24) | (tarama & 0xFFFFFF);
            for (int r = 1; r < R; r++) {
                int px = cx + (int) Math.round(Math.sin(a) * r);
                int py = cy - (int) Math.round(Math.cos(a) * r);
                g.fill(px, py, px + 1, py + 1, renk);
            }
        }
        g.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
        if (d.gorunum() == RadarGorunumu.AFET) {
            double oran = d.menzil() > 0 ? Math.min(1, mesafe / d.menzil()) : 0;
            double r = 2 + oran * (R - 4);
            double a = Math.toRadians(goreli);
            int bx = cx + (int) Math.round(Math.sin(a) * r);
            int by = cy - (int) Math.round(Math.cos(a) * r);
            double fark = Math.abs(YonMesafe.normalize(aci - goreli));
            int parlak = fark < 40 ? 0xFFFFFFFF : 0xFFFF6A4D;
            g.fill(bx - 1, by - 1, bx + 2, by + 2, 0xFFE0402A);
            g.fill(bx, by, bx + 1, by + 1, parlak);
        }
    }

    private static void halkaCiz(GuiGraphics g, int cx, int cy, float r, int renk) {
        int adim = Math.max(12, (int) (r * 6));
        for (int i = 0; i < adim; i++) {
            double a = Math.PI * 2 * i / adim;
            int px = cx + (int) Math.round(Math.cos(a) * r);
            int py = cy + (int) Math.round(Math.sin(a) * r);
            g.fill(px, py, px + 1, py + 1, renk);
        }
    }

    /** {@code aci} kadar dönmüş (0 = yukarı) 9 piksellik ok; merkez (x, y). */
    private static void ok(GuiGraphics g, int x, int y, double aci, int renk) {
        PoseStack p = g.pose();
        p.pushPose();
        p.translate(x, y, 0);
        p.mulPose(Axis.ZP.rotationDegrees((float) aci));
        g.fill(-1, -5, 1, -4, renk);
        g.fill(-2, -4, 2, -3, renk);
        g.fill(-3, -3, 3, -2, renk);
        g.fill(-4, -2, 4, -1, renk);
        g.fill(-1, -1, 1, 5, renk);
        p.popPose();
    }
}
