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
import io.github.kadirberkpolat1.dogalafetler.komut.CekirdekKomutlari;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.extensions.common.IClientMobEffectExtensions;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Hava Radarı göstergesi (madde 13a; N6'nın istisnası: yalnız radar elde tutulurken). Küçük bir CRT paneli: solda saniyede
 * bir tur atan taramalı radar kapı (afet merkezi bakış yönüne göre bir nokta olarak), sağda afetin adı, yön oku ve mesafe,
 * varış ya da kalan süre. Güneş patlaması ve ay çarpması sirenden sonra mor bir "gökyüzü anomalisi" ve geri sayım olarak
 * görünür (N7). Her yerde süren bölgesel afet (asit yağmuru) yön oku ve mesafe yerine "her yer" yazar, nokta kabın
 * ortasındadır (AF2-06). Gelişmiş radar mor-mavi çerçevelidir ve altına kısa satırlarla şiddet seviyesi + hareket oku/hız ve son 5
 * afeti ("Kasırga S2 3dk") ekler; yükseklik {@link GostergeYerlesimi#panelYuksekligi} ile sınırlıdır (R-18). Bütün panel tek
 * {@code drawManaged} toplu çizimidir (R-19). Yer CLIENT config'tedir; alt köşelerde hotbar, kalpler, sıcaklık barı ve
 * sismograf action bar'ıyla, sağ üstte efekt ikonlarıyla çakışmaz ({@link GostergeYerlesimi}). Konum afet türüne ve panel
 * metnine bağlı değildir: sabit yerleşim genişliği ve sabit alt bölgelerle hesaplanır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RadarGostergesi {
    private static final int R = GostergeYerlesimi.KAP_YARICAP;
    private static final int KAP = GostergeYerlesimi.KAP;
    private static final int IC = GostergeYerlesimi.IC;
    private static final int SATIR = 10;
    private static final int ALT_SATIR = GostergeYerlesimi.ALT_SATIR;
    /** Tarama açısı (derece/tick): 20 tick'te bir tur. */
    private static final double TARAMA_HIZI = 18.0;
    /** Dikey yerleşim (hotbar, durum sütunu, action bar çakışması) hesabının panel metninden bağımsız sabit genişliği (GUI pikseli). */
    private static final int YERLESIM_GENISLIGI = 170;

    private static final int ZEMIN = 0xD80A140D;
    private static final int KAP_ZEMIN = 0xFF04120A;
    private static final int HALKA = 0xFF1E5C30;
    private static final int TARAMA = 0xFF63FF86;
    private static final int YAZI = 0xFFC6FFD2;
    private static final int SOLUK = 0xFF7FAF8A;
    private static final int SONUK = 0xFF5E8A69;
    private static final int CERCEVE = 0xFF2F7A45;
    private static final int CERCEVE_GELISMIS = 0xFF7A63E0;
    private static final int VURGU_GELISMIS = 0xFF4FA3FF;
    private static final int ANOMALI = 0xFFD7A6FF;
    private static final int ANOMALI_SOLUK = 0xFFB79BD6;
    private static final int[] SEVIYE_RENGI = {0xFFB0B0B0, 0xFF9BE15D, 0xFFF2D04B, 0xFFF29B38, 0xFFF25C38, 0xFFE8303A};

    /** Son çizilen panel (GUI pikseli) ve ekran boyutu: görsel kanıt otomasyonu çakışma denetimini log'a yazar. */
    static int sonX = -1;
    static int sonY;
    static int sonG;
    static int sonY2;
    static int sonEkranG;
    static int sonEkranY;
    static GostergeYerlesimi.Alt sonAlt = GostergeYerlesimi.VARSAYILAN;
    /** Oturumda görülen en geniş panel (GUI pikseli); panel genişliği bunun altına inmez. */
    private static int kararliGenislik;
    /** Log'a yazılan son panel boyutu (yükseklik ya da ekran değişince bir satır). */
    private static long sonLoglanan = -1;

    /** Bir metin satırı; {@code okAcisi} NaN değilse metnin solunda o açıyla dönmüş ok çizilir. */
    private record Satir(Component metin, int renk, double okAcisi, int okRengi) {
        static Satir duz(Component metin, int renk) {
            return new Satir(metin, renk, Double.NaN, 0);
        }

        boolean oklu() {
            return !Double.isNaN(okAcisi);
        }

        int genislik(Font font) {
            return font.width(metin) + (oklu() ? 12 : 0);
        }
    }

    /** Gelişmiş alt satırı: önde renkli bir parça (şiddet seviyesi), ardından isteğe bağlı ok ve metin. */
    private record AltSatir(Component on, int onRenk, double okAcisi, Component metin, int renk) {
        int genislik(Font font) {
            int g = on == null ? 0 : font.width(on) + 4;
            return g + (Double.isNaN(okAcisi) ? 0 : 12) + (metin == null ? 0 : font.width(metin));
        }
    }

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
        double goreli = 0;
        double mesafe = 0;
        List<Satir> ust = new ArrayList<>();
        if (d.gorunum() == RadarGorunumu.AFET && d.herYer()) {
            ust.add(Satir.duz(Component.translatable(d.tur().ceviriAnahtari()), YAZI));
            ust.add(Satir.duz(Component.translatable(CekirdekKomutlari.HER_YER), YAZI));
            ust.add(Satir.duz(sure(d), SOLUK));
        } else if (d.gorunum() == RadarGorunumu.AFET) {
            double mx = d.merkezX() + 0.5;
            double mz = d.merkezZ() + 0.5;
            mesafe = YonMesafe.mesafe(oyuncu.getX(), oyuncu.getZ(), mx, mz);
            goreli = YonMesafe.goreliAci(YonMesafe.pusulaAcisi(oyuncu.getX(), oyuncu.getZ(), mx, mz), oyuncu.getYRot());
            ust.add(Satir.duz(Component.translatable(d.tur().ceviriAnahtari()), YAZI));
            ust.add(new Satir(Component.translatable(RadarCevirileri.ON + "mesafe", Math.round(mesafe)), YAZI, goreli, YAZI));
            ust.add(Satir.duz(sure(d), SOLUK));
        } else if (d.gorunum() == RadarGorunumu.ANOMALI) {
            ust.add(Satir.duz(Component.translatable(RadarCevirileri.ON + "anomali"), ANOMALI));
            ust.add(Satir.duz(Component.translatable(d.tur().ceviriAnahtari()), ANOMALI_SOLUK));
            ust.add(Satir.duz(sure(d), ANOMALI));
        } else {
            ust.add(Satir.duz(Component.translatable(RadarCevirileri.ON + "sakin"), SOLUK));
            ust.add(Satir.duz(Component.translatable(RadarCevirileri.ON + "menzil", d.menzil()), SONUK));
        }
        List<AltSatir> alt = d.gelismis() ? altSatirlar(d, oyuncu.getYRot()) : List.of();

        int yaziX = IC + KAP + 6;
        int pw = 104;
        for (Satir satir : ust) {
            pw = Math.max(pw, yaziX + satir.genislik(font) + IC + 2);
        }
        for (AltSatir satir : alt) {
            pw = Math.max(pw, IC + satir.genislik(font) + IC + 2);
        }
        // Panel genişliği oturum boyunca yalnız büyür: geçiş (sakin, afet, anomali) satırları genişliği küçültmez, sol kenar oynamaz.
        pw = Math.max(pw, kararliGenislik);
        kararliGenislik = pw;
        int ph = GostergeYerlesimi.panelYuksekligi(alt.size());
        boolean yararli = false;
        boolean zararli = false;
        for (MobEffectInstance etki : oyuncu.getActiveEffects()) {
            if (etki.showIcon() && IClientMobEffectExtensions.of(etki).isVisibleInGui(etki)) {
                yararli |= etki.getEffect().isBeneficial();
                zararli |= !etki.getEffect().isBeneficial();
            }
        }
        // Konum afet türüne, panel metnine ve geçici HUD durumuna (action bar, su altı hava barları) göre DEĞİŞMEZ: dikey yer
        // ve hotbar/durum sütunu çakışması sabit bir yerleşim genişliği ve sabit alt bölgelerle hesaplanır; yalnız köşe
        // kenarı gerçek genişliğe göre hizalanır (sağ köşelerde sağ kenar, sol köşelerde sol kenar sabit).
        GostergeYerlesimi.Alt altBolge = GostergeYerlesimi.VARSAYILAN;
        int efektAlti = GostergeYerlesimi.efektAlti(yararli, zararli);
        GostergeYerlesimi.Konum gercek = GostergeYerlesimi.konum(w, h, pw, ph, RadarIstemciAyarlari.kose(),
                RadarIstemciAyarlari.xOfset(), RadarIstemciAyarlari.yOfset(), altBolge, efektAlti);
        GostergeYerlesimi.Konum dikey = GostergeYerlesimi.konum(w, h, Math.max(YERLESIM_GENISLIGI, pw), ph,
                RadarIstemciAyarlari.kose(), RadarIstemciAyarlari.xOfset(), RadarIstemciAyarlari.yOfset(), altBolge, efektAlti);
        GostergeYerlesimi.Konum k = new GostergeYerlesimi.Konum(gercek.x(), dikey.y());
        sonX = k.x();
        sonY = k.y();
        sonG = pw;
        sonY2 = ph;
        sonEkranG = w;
        sonEkranY = h;
        sonAlt = altBolge;
        long boyut = ((long) ph << 32) | ((long) h << 16) | (d.gelismis() ? 1 : 0);
        if (boyut != sonLoglanan) {
            sonLoglanan = boyut;
            DogalAfetler.LOGGER.info("[radar] panel {}x{} GUI px ({}), ekran {}x{}: yükseklik %{}", pw, ph,
                    d.gelismis() ? "Gelişmiş, " + alt.size() + " alt satır" : "normal", w, h, Math.round(100.0 * ph / h));
        }
        double taramaAcisi = (zaman * TARAMA_HIZI) % 360.0;
        double noktaAcisi = goreli;
        double noktaMesafe = mesafe;
        int genislik = pw;
        g.drawManaged(() -> panel(g, font, d, k.x(), k.y(), genislik, ph, ust, alt, taramaAcisi, zaman, noktaAcisi,
                noktaMesafe));
    }

    /** Gelişmiş radarın kısa alt satırları (en çok {@link GostergeYerlesimi#EN_COK_ALT_SATIR}). */
    private static List<AltSatir> altSatirlar(RadarPaketi d, float yaw) {
        List<AltSatir> alt = new ArrayList<>();
        if (d.gorunum() == RadarGorunumu.AFET) {
            Component seviye = d.seviye() > 0 ? Component.translatable("seviye.dogalafetler." + d.seviye()) : null;
            int renk = SEVIYE_RENGI[Math.max(0, Math.min(5, d.seviye()))];
            if (Float.isNaN(d.hareketYonu())) {
                alt.add(new AltSatir(seviye, renk, Double.NaN, Component.translatable(RadarCevirileri.ON + "durgun"), SOLUK));
            } else {
                alt.add(new AltSatir(seviye, renk, YonMesafe.goreliAci(d.hareketYonu(), yaw), Component.translatable(
                        RadarCevirileri.ON + "hiz", String.format(Locale.ROOT, "%.1f", d.hiz())), VURGU_GELISMIS));
            }
        }
        alt.add(new AltSatir(null, 0, Double.NaN, Component.translatable(RadarCevirileri.ON + "gecmis"), VURGU_GELISMIS));
        if (d.gecmis().isEmpty()) {
            alt.add(new AltSatir(null, 0, Double.NaN, Component.translatable(RadarCevirileri.ON + "gecmis_bos"), SONUK));
        }
        for (RadarPaketi.Gecmis kayit : d.gecmis()) {
            Component satir = Component.translatable(kayit.tur().ceviriAnahtari());
            if (kayit.seviye() > 0) {
                satir = Component.empty().append(satir).append(" ")
                        .append(Component.translatable(RadarCevirileri.ON + "seviye_kisa", kayit.seviye()));
            }
            satir = Component.empty().append(satir).append(" ").append(yas(kayit.yasDakika()));
            alt.add(new AltSatir(null, 0, Double.NaN, satir,
                    kayit.seviye() > 0 ? SEVIYE_RENGI[Math.min(5, kayit.seviye())] : ANOMALI_SOLUK));
        }
        return alt;
    }

    private static void panel(GuiGraphics g, Font font, RadarPaketi d, int x, int y, int pw, int ph, List<Satir> ust,
            List<AltSatir> alt, double taramaAcisi, float zaman, double goreli, double mesafe) {
        int cerceve = d.gelismis() ? CERCEVE_GELISMIS : CERCEVE;
        g.fill(x, y, x + pw, y + ph, ZEMIN);
        g.fill(x, y, x + pw, y + 1, cerceve);
        g.fill(x, y + ph - 1, x + pw, y + ph, cerceve);
        g.fill(x, y, x + 1, y + ph, cerceve);
        g.fill(x + pw - 1, y, x + pw, y + ph, cerceve);
        if (d.gelismis()) {
            g.fill(x + 1, y + 1, x + pw - 1, y + 2, VURGU_GELISMIS & 0x80FFFFFF);
        }
        kap(g, x + IC + R, y + IC + R, d, taramaAcisi, zaman, goreli, mesafe);

        int ty = y + IC + 1 + (KAP - ust.size() * SATIR) / 2;
        for (int i = 0; i < ust.size(); i++) {
            Satir satir = ust.get(i);
            int sx = x + IC + KAP + 6;
            if (satir.oklu()) {
                ok(g, sx + 4, ty + i * SATIR + 4, satir.okAcisi(), satir.okRengi());
                sx += 12;
            }
            g.drawString(font, satir.metin(), sx, ty + i * SATIR, satir.renk(), true);
        }
        if (alt.isEmpty()) {
            return;
        }
        int ay = y + IC + KAP + IC;
        g.fill(x + IC, ay, x + pw - IC, ay + 1, VURGU_GELISMIS & 0x90FFFFFF);
        ay += GostergeYerlesimi.AYIRAC;
        for (AltSatir satir : alt) {
            int sx = x + IC;
            if (satir.on() != null) {
                g.drawString(font, satir.on(), sx, ay, satir.onRenk(), true);
                sx += font.width(satir.on()) + 4;
            }
            if (!Double.isNaN(satir.okAcisi())) {
                ok(g, sx + 4, ay + 4, satir.okAcisi(), VURGU_GELISMIS);
                sx += 12;
            }
            if (satir.metin() != null) {
                g.drawString(font, satir.metin(), sx, ay, satir.renk(), true);
            }
            ay += ALT_SATIR;
        }
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

    /** Geçmiş satırının kısa yaşı: "şimdi", "3dk", "2sa", "1g". */
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
    private static void kap(GuiGraphics g, int cx, int cy, RadarPaketi d, double aci, float zaman, double goreli,
            double mesafe) {
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
