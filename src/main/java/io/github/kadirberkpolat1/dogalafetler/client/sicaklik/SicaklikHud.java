package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikDurumPaketi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikIstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.SicaklikDurumu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Sıcaklık göstergesi (K-E74, §A8): vanilla can ve açlık barlarının ortasındaki 20 piksellik boşluğa sığan, ortasında
 * oyuncunun kendi kafası (skin yüzü + şapka katmanı, {@code getSkinTextureLocation}) bulunan küçük rozet. Kafa durumu
 * anlatır: bunalmada ter damlaları ve hafif sıcak ton, yanmada kızıl ton ile alev/duman, üşümede mavimsi ton ve ağızdan
 * buhar, donmada buz tonu, kıra ve kristaller (başlarken sarsılır, sonra donar). Kafanın altında çok ince bir şerit vücut
 * sıcaklığını ortadan dışa dolan renkle gösterir (soğuk sol, sıcak sağ; eşik çentikleri, hasar eşikleri kırmızı), onu kesen
 * ince beyaz iğne hissedilen sıcaklığı (denge noktasını) gösterir; ıslakken kafanın sağında bir su damlası belirir (alttan ıslaklık
 * oranında dolar, kuruyunca kaybolur). İsteğe bağlı {@code °C} sayısı
 * kafanın üstünde küçük yazıyla çizilir. Can ve açlık barları vanilla yerinde kalır (artık {@code leftHeight/rightHeight}
 * kaydırılmaz); gösterge XP seviye sayısının hemen üstünde durduğu için yalnız üstüne binen seçili eşya adı o sırada birkaç piksel
 * yukarı alınır. Gösterge yalnız sunucunun sıcaklık kapısı açıkken ({@code SicaklikKapisi}, S-12) ve hayatta kalma
 * modundaki oyuncuya çizilir (creative/spectator'da gizli).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SicaklikHud {
    /** Göstergenin alt kenarının ekranın altından uzaklığı: XP seviye sayısının üstü (36 px) ve 1 px pay. */
    private static final int ALTTAN = 37 + HudCizimi.YUKSEKLIK;
    /** Seçili eşya adı kutusunun Forge'daki konumu: ekranın altından 61 (üst) ile 48 (alt) piksel. */
    private static final int ESYA_ADI_UST = 61;
    private static final int ESYA_ADI_ALT = 48;
    private static final float SAYI_OLCEGI = 0.75f;
    /** Sayı yazısının kafanın üstünde ayrıca kapladığı yükseklik (ölçekli 9 px'lik satır + pay). */
    private static final int SAYI_YUKSEKLIGI = 8;

    /** Son çizilen göstergenin görsel kanıt alanları (otomasyon log'a yazar). */
    static int sonDx;
    static int sonDy;
    static long sonTick = -1;
    static float sonParlama;
    static float sonNabiz;
    static String sonMetin = "";
    static String sonEfekt = "";
    /** Son çizilen iğnenin gösterge içi x'i. */
    static int sonIgne;
    /** Son çizilen kafanın sol üst köşesi (GUI pikseli; görsel kanıt ölçümü). */
    static int sonHapX;
    static int sonHapY;
    /** Seçili eşya adının bu karede yukarı alınacağı piksel (0: gösterge eşya adına binmiyor ya da çizilmedi). */
    private static int esyaAdiKaydirma;
    private static boolean esyaAdiKaydirildi;

    private SicaklikHud() {
    }

    @SubscribeEvent
    public static void katmanlariKaydet(RegisterGuiOverlaysEvent olay) {
        olay.registerBelow(VanillaGuiOverlay.PLAYER_HEALTH.id(), "sicaklik_bari", SicaklikHud::ciz);
    }

    private static void ciz(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
        esyaAdiKaydirma = 0;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer oyuncu = mc.player;
        if (oyuncu == null || !IstemciSicaklik.acik() || mc.options.hideGui || !gui.shouldDrawSurvivalElements()
                || oyuncu.isCreative() || oyuncu.isSpectator() || !SicaklikIstemciAyarlari.hudGorunur()) {
            return;
        }
        SicaklikDurumPaketi d = IstemciSicaklik.durum();
        SicaklikDurumu durum = d.kademe().durum();
        boolean animasyon = SicaklikIstemciAyarlari.animasyonlar();
        boolean sayi = SicaklikIstemciAyarlari.sayiGoster();
        long t = IstemciSicaklik.tick();
        float zaman = animasyon ? t + partialTick : 0f;
        float vucut = IstemciSicaklik.vucut(partialTick);

        int[] titreme = animasyon ? HudCizimi.titreme(durum, t, IstemciSicaklik.durumDegisimi()) : new int[] {0, 0};
        int x = (w - HudCizimi.GENISLIK) / 2 + SicaklikIstemciAyarlari.hudXOfset() + titreme[0];
        int y = h - ALTTAN + SicaklikIstemciAyarlari.hudYOfset() + titreme[1];
        int ust = y - (sayi ? SAYI_YUKSEKLIGI : 0);
        if (y + HudCizimi.YUKSEKLIK > h - ESYA_ADI_UST && ust < h - ESYA_ADI_ALT) {
            esyaAdiKaydirma = h - ESYA_ADI_ALT - ust + 1;
        }

        float nabiz = animasyon ? HudCizimi.nabiz(durum, vucut, d.sicak(), d.yanma(), zaman) : 0f;
        float parlama = animasyon ? HudCizimi.parlama(t, IstemciSicaklik.durumDegisimi(), partialTick) : 0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        // Halka: kafanın 1 px dışında, durum renginde yarı saydam çerçeve.
        g.fill(x + HudCizimi.KAFA_X - 1, y + HudCizimi.KAFA_Y - 1, x + HudCizimi.KAFA_X + HudCizimi.KAFA + 1,
                y + HudCizimi.KAFA_Y + HudCizimi.KAFA + 1, HudCizimi.renk(durum) & 0xFFFFFF | 0x99000000);
        kafa(g, oyuncu, x + HudCizimi.KAFA_X, y + HudCizimi.KAFA_Y, HudCizimi.kafaTonu(durum, nabiz), parlama);
        boolean efektli = SicaklikIstemciAyarlari.kafaEfektleri();
        if (efektli) {
            for (HudCizimi.Parcacik p : HudCizimi.efekt(durum, zaman)) {
                g.fill(x + p.x(), y + p.y(), x + p.x() + p.w(), y + p.y() + p.h(), p.argb());
            }
        }
        serit(g, x, y, vucut, d);
        int igne = HudCizimi.igneX(IstemciSicaklik.ortam(partialTick));
        if (SicaklikIstemciAyarlari.hissedilenGoster()) {
            g.fill(x + igne, y + HudCizimi.IGNE_Y, x + igne + 1, y + HudCizimi.IGNE_Y + HudCizimi.IGNE_YUKSEKLIK,
                    0xF0FFFFFF);
        }
        islaklik(g, x, y, IstemciSicaklik.islaklik(partialTick), zaman);
        String metin = "";
        if (sayi) {
            metin = HudCizimi.sayiMetni(vucut);
            g.pose().pushPose();
            g.pose().translate(x + HudCizimi.GENISLIK / 2f, y - SAYI_YUKSEKLIGI + 1f, 0f);
            g.pose().scale(SAYI_OLCEGI, SAYI_OLCEGI, 1f);
            g.drawString(mc.font, metin, -mc.font.width(metin) / 2f, 0f, HudCizimi.sayiRengi(durum), true);
            g.pose().popPose();
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        sonDx = titreme[0];
        sonDy = titreme[1];
        sonTick = t;
        sonParlama = parlama;
        sonNabiz = nabiz;
        sonMetin = metin;
        sonEfekt = efektli ? HudCizimi.efektAdi(durum) : "";
        sonIgne = igne;
        sonHapX = x + HudCizimi.KAFA_X;
        sonHapY = y + HudCizimi.KAFA_Y;
    }

    /**
     * Oyuncunun kafası: skin'in yüzü (8,8) ve şapka katmanı (40,8; modelde açıksa), 8x8'den 16x16'ya tam sayı ölçeklenir; ton
     * ve durum değişim parlaması uygulanır.
     */
    private static void kafa(GuiGraphics g, LocalPlayer oyuncu, int kx, int ky, float[] ton, float parlama) {
        ResourceLocation deri = oyuncu.getSkinTextureLocation();
        // fill kendi render tipini kapatırken karışımı kapatır: şapka katmanının saydam pikselleri için her seferinde açılır.
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(ton[0], ton[1], ton[2], 1f);
        g.blit(deri, kx, ky, HudCizimi.KAFA, HudCizimi.KAFA, 8f, 8f, 8, 8, 64, 64);
        if (oyuncu.isModelPartShown(PlayerModelPart.HAT)) {
            g.blit(deri, kx, ky, HudCizimi.KAFA, HudCizimi.KAFA, 40f, 8f, 8, 8, 64, 64);
        }
        g.setColor(1f, 1f, 1f, 1f);
        if (parlama > 0f) {
            g.fill(kx, ky, kx + HudCizimi.KAFA, ky + HudCizimi.KAFA, Math.round(parlama * 0xA0) << 24 | 0xFFFFFF);
        }
    }

    /** İnce şerit: koyu zemin, ortadan dışa dolan renkli yarılar (son piksel kesir kadar saydam) ve eşik çentikleri. */
    private static void serit(GuiGraphics g, int x, int y, float vucut, SicaklikDurumPaketi d) {
        int sy = y + HudCizimi.SERIT_Y;
        g.fill(x, sy, x + HudCizimi.GENISLIK, sy + HudCizimi.SERIT_YUKSEKLIK, 0xB0000000);
        boolean sicak = vucut >= 0;
        float dolum = HudCizimi.yariDolum(vucut);
        int tam = (int) dolum;
        for (int i = 0; i < tam && i < HudCizimi.YARIM_IC; i++) {
            int px = x + HudCizimi.piksel(sicak, i);
            g.fill(px, sy, px + 1, sy + HudCizimi.SERIT_YUKSEKLIK, HudCizimi.seritRengi(sicak, i));
        }
        if (tam < HudCizimi.YARIM_IC && dolum > tam) {
            int px = x + HudCizimi.piksel(sicak, tam);
            int renk = HudCizimi.seritRengi(sicak, tam) & 0xFFFFFF | Math.round((dolum - tam) * 255f) << 24;
            g.fill(px, sy, px + 1, sy + HudCizimi.SERIT_YUKSEKLIK, renk);
        }
        // Durum eşikleri şeridin içinde soluk, hasar eşikleri şeridi aşan kırmızı çentik.
        centik(g, x, y, d.soguk(), false);
        centik(g, x, y, d.cokSoguk(), false);
        centik(g, x, y, d.donma(), true);
        centik(g, x, y, d.sicak(), false);
        centik(g, x, y, d.cokSicak(), false);
        centik(g, x, y, d.yanma(), true);
    }

    private static void centik(GuiGraphics g, int x, int y, float esik, boolean hasar) {
        int cx = x + HudCizimi.centik(esik);
        int sy = y + HudCizimi.SERIT_Y;
        if (hasar) {
            g.fill(cx, sy - 1, cx + 1, sy + HudCizimi.SERIT_YUKSEKLIK + 1, 0xE0FF3B30);
        } else {
            g.fill(cx, sy, cx + 1, sy + HudCizimi.SERIT_YUKSEKLIK, 0x66FFFFFF);
        }
    }

    /** Kafanın sağındaki ıslaklık damlası: yalnız görünür ıslaklıkta çizilir, ıslaklık oranında alttan dolar. */
    private static void islaklik(GuiGraphics g, int x, int y, float islaklik, float zaman) {
        for (HudCizimi.Parcacik p : HudCizimi.damla(islaklik, zaman)) {
            g.fill(x + p.x(), y + p.y(), x + p.x() + p.w(), y + p.y() + p.h(), p.argb());
        }
    }

    /**
     * Seçili eşya adını göstergenin üstüne çıkarır (yalnız bu kare gösterge ona biniyorsa). Başında öteleme eklenir,
     * ad çizildikten sonra geri alınır; başka bir mod katmanı iptal ederse (son çizim yok) öteleme hiç eklenmez.
     */
    @Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
    public static final class EsyaAdi {
        private EsyaAdi() {
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void once(RenderGuiOverlayEvent.Pre olay) {
            if (esyaAdiKaydirma > 0 && olay.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id())) {
                olay.getGuiGraphics().pose().pushPose();
                olay.getGuiGraphics().pose().translate(0f, -esyaAdiKaydirma, 0f);
                esyaAdiKaydirildi = true;
            }
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void sonra(RenderGuiOverlayEvent.Post olay) {
            if (esyaAdiKaydirildi && olay.getOverlay().id().equals(VanillaGuiOverlay.ITEM_NAME.id())) {
                olay.getGuiGraphics().pose().popPose();
                esyaAdiKaydirildi = false;
            }
        }
    }
}
