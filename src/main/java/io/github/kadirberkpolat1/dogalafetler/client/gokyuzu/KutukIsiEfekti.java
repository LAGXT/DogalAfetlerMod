package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.EkranEfektKapisi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok.KomurlesmisKutukBlogu;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KutukIsisi;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Kömürleşmiş kütük yakınlık ısısı (ekran efekti): "çok sıcak" kademedeki ({@link KutukIsisi#cokSicak}) bir kütüğe
 * {@value #MENZIL} blok yaklaşınca ekranın yalnız KENARLARINDA hafif, şeffaf kızıl-beyaz haze (vinyet benzeri; merkez net,
 * kenardaki alfa en çok ~%17), çok hafif ısı dalgalanması ve çok hafif kamera yalpası/görüş açısı titreşimi gelir; şiddet
 * mesafeyle artar. Tam ekran dolgu yoktur: görüş hiçbir koşulda engellenmez. Vücut sıcaklığı efektinden bağımsızdır
 * (oyuncunun vücudu ısınmamış olsa da çalışır); yalnız yaratıcı/izleyici kapısından geçer
 * ({@link EkranEfektKapisi#izinli()}). Blok durumu sunucudan doğrudan geldiği için ayrı paket yoktur: İstemci her
 * 2 tick'te göz çevresindeki küpte kütükleri tarar.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class KutukIsiEfekti {
    /** Efektin başladığı uzaklık (blok yüzüne, gözden). */
    public static final double MENZIL = 3.0;
    private static final int TARAMA_YARICAPI = 4;
    private static final ResourceLocation ISI = DogalAfetler.konum("textures/misc/isi_dalgasi.png");
    private static final int SERIT = 12;
    /** Kenar hazesinin en dış (kenardaki) alfası: 0–255 içinde ~%17. */
    private static final int KENAR_ALFA = 44;

    private static float hedef;
    private static float yogunluk;
    private static float oncekiYogunluk;
    private static long saat;

    private KutukIsiEfekti() {
    }

    /** Uzaklıktan (gözden kütüğün en yakın noktasına) 0–1 yakınlık: {@link #MENZIL}'de 0, temasta 1, kare eğrisi. */
    static float yakinlik(double uzaklik) {
        float t = Mth.clamp((float) (1.0 - uzaklik / MENZIL), 0f, 1f);
        return t * t;
    }

    /** Göz konumuna göre bir bloğun en yakın noktasına uzaklık. */
    private static double kutuUzakligi(Vec3 goz, BlockPos konum) {
        double dx = Math.max(0.0, Math.abs(goz.x - (konum.getX() + 0.5)) - 0.5);
        double dy = Math.max(0.0, Math.abs(goz.y - (konum.getY() + 0.5)) - 0.5);
        double dz = Math.max(0.0, Math.abs(goz.z - (konum.getZ() + 0.5)) - 0.5);
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static float tara(ClientLevel level, LocalPlayer oyuncu) {
        Block kutuk = PulsarKayitlari.KOMURLESMIS_KUTUK.get();
        Vec3 goz = oyuncu.getEyePosition();
        BlockPos m = BlockPos.containing(goz);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        float toplam = 0f;
        for (int dx = -TARAMA_YARICAPI; dx <= TARAMA_YARICAPI; dx++) {
            for (int dy = -TARAMA_YARICAPI; dy <= TARAMA_YARICAPI; dy++) {
                for (int dz = -TARAMA_YARICAPI; dz <= TARAMA_YARICAPI; dz++) {
                    p.set(m.getX() + dx, m.getY() + dy, m.getZ() + dz);
                    BlockState durum = level.getBlockState(p);
                    if (durum.is(kutuk) && KutukIsisi.cokSicak(durum.getValue(KomurlesmisKutukBlogu.ISI))) {
                        toplam += yakinlik(kutuUzakligi(goz, p));
                    }
                }
            }
        }
        return Math.min(1f, toplam);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer oyuncu = mc.player;
        if (level == null || oyuncu == null || mc.isPaused()) {
            return;
        }
        saat++;
        oncekiYogunluk = yogunluk;
        if (saat % 2 == 0) {
            hedef = EkranEfektKapisi.izinli(oyuncu) ? tara(level, oyuncu) : 0f;
        }
        yogunluk += Mth.clamp(hedef - yogunluk, -0.05f, 0.08f);
    }

    @SubscribeEvent
    public static void cikis(ClientPlayerNetworkEvent.LoggingOut olay) {
        hedef = 0f;
        yogunluk = 0f;
        oncekiYogunluk = 0f;
    }

    private static float guc(float partialTick) {
        return EkranEfektKapisi.izinli() ? Mth.lerp(partialTick, oncekiYogunluk, yogunluk) : 0f;
    }

    /** Kamera: çok hafif ısı titreşimi (yalpa en çok ~0,12°). */
    @SubscribeEvent
    public static void kamera(ViewportEvent.ComputeCameraAngles olay) {
        float g = guc((float) olay.getPartialTick());
        if (g <= 0.01f) {
            return;
        }
        float t = (saat + (float) olay.getPartialTick()) * 0.05f;
        olay.setRoll(olay.getRoll() + g * 0.12f * Mth.sin(t * 2.1f));
        olay.setYaw(olay.getYaw() + g * 0.025f * Mth.sin(t * 6.3f));
        olay.setPitch(olay.getPitch() + g * 0.02f * Mth.sin(t * 5.1f + 1.3f));
    }

    /** Görüş açısı: çok hafif nabız (en çok ~0,45°). */
    @SubscribeEvent
    public static void gorusAcisi(ViewportEvent.ComputeFov olay) {
        float g = guc((float) olay.getPartialTick());
        if (g <= 0.01f || !olay.usedConfiguredFov()) {
            return;
        }
        float t = (saat + (float) olay.getPartialTick()) * 0.05f;
        olay.setFOV(olay.getFOV() + g * (0.3f * Mth.sin(t * 3.1f) + 0.15f * Mth.sin(t * 7.3f + 0.7f)));
    }

    /** Ekran katmanı (mod veri yolu). */
    @Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Katman {
        private Katman() {
        }

        @SubscribeEvent
        public static void katmanlariKaydet(RegisterGuiOverlaysEvent olay) {
            olay.registerAbove(VanillaGuiOverlay.FROSTBITE.id(), "kutuk_yakin_isi", Katman::ciz);
        }

        private static void ciz(ForgeGui gui, GuiGraphics g, float partialTick, int w, int h) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.getCameraType() != CameraType.FIRST_PERSON) {
                return;
            }
            float y = guc(partialTick);
            if (y <= 0.01f) {
                return;
            }
            float zaman = saat + partialTick;
            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            // Yalnız ekranın kenarlarında hafif kızıl-beyaz haze (vinyet benzeri): merkez net, alfa en çok ~%17 (kenarda).
            // Tam ekran dolgu yok; hiçbir koşulda görüş kapanmaz.
            float nabiz = 0.92f + 0.08f * Mth.sin(zaman * 0.3f);
            int serit = Math.max(6, Math.min(w, h) / 5);
            for (int i = 0; i < SERIT; i++) {
                float sonum = 1f - i / (float) SERIT;
                int a = (int) (KENAR_ALFA * y * nabiz * sonum * sonum);
                if (a <= 0) {
                    break;
                }
                int renk = Mth.clamp(a, 0, KENAR_ALFA) << 24 | 0xFFD2C4;
                int k = serit * i / SERIT;
                int k2 = serit * (i + 1) / SERIT;
                g.fill(0, k, w, k2, renk);
                g.fill(0, h - k2, w, h - k, renk);
                g.fill(k, k2, k2, h - k2, renk);
                g.fill(w - k2, k2, w - k, h - k2, renk);
            }
            // Çok hafif ısı dalgalanması: yalnız sol/sağ kenar şeritlerinde, ters yönde kayan iki kat.
            int sw = Math.max(8, w / 6);
            int vYuksekligi = Math.max(8, Math.min(64, 16 * h / sw));
            g.setColor(1f, 1f, 1f, 0.045f * y);
            g.blit(ISI, 0, 0, sw, h, 0f, (zaman * 0.8f) % 64f, 16, vYuksekligi, 64, 64);
            g.setColor(1f, 0.92f, 0.82f, 0.045f * y);
            g.blit(ISI, w - sw, 0, sw, h, 48f, 64f - (zaman * 0.5f) % 64f, 16, vYuksekligi, 64, 64);
            g.setColor(1f, 1f, 1f, 1f);
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
        }
    }
}
