package io.github.kadirberkpolat1.dogalafetler.client.sicaklik;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=sicaklik|sicaklik_coldsweat} ile çalışır;
 * üretimde etkisizdir. {@code sicaklik}: oyuncu OP olunca vücut sıcaklığını sırayla ayarlayıp HUD ve ekran efektlerinin
 * ekran görüntülerini alır, sonra sunucu konsolunun komutları için bekler ve kapanır. {@code sicaklik_coldsweat}: Cold Sweat
 * yüklü tek oyunculu dünyada kapı durumunu loglar ve bir görüntü alır. Klavye, fare ve tekerlek girdileri kapanışa
 * kadar sayılır (sıfırdan farklıysa koşu geçersizdir). Kendi açmadığı duraklatma ekranını kapatır; adımlar durum koşuluyla
 * ilerler.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SicaklikOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.otomasyon", "");
    private static final String ON = "SICAKLIK_OTOMASYON ";

    private record Sahne(String ad, int deger, boolean ikinciKare) {
    }

    private static final List<Sahne> SAHNELER = List.of(new Sahne("notr", 0, false), new Sahne("soguk", -42, false),
            new Sahne("cok_soguk", -66, false), new Sahne("donuyor", -96, true), new Sahne("sicak", 42, false),
            new Sahne("cok_sicak", 66, false), new Sahne("yaniyor", 96, true));

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static int sahne;

    private SicaklikOtomasyonu() {
    }

    private static boolean aktif() {
        return MOD.startsWith("sicaklik");
    }

    @SubscribeEvent
    public static void tusa(InputEvent.Key olay) {
        if (aktif()) {
            tus++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tuş {} eylem {}", olay.getKey(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void fareye(InputEvent.MouseButton.Pre olay) {
        if (aktif()) {
            fare++;
            DogalAfetler.LOGGER.warn(ON + "girdi: fare {} eylem {}", olay.getButton(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void tekerlege(InputEvent.MouseScrollingEvent olay) {
        if (aktif()) {
            teker++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tekerlek {}", olay.getScrollDelta());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (!aktif() || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.info(ON + "kendi açmadığım duraklatma ekranı kapatıldı");
            mc.setScreen(null);
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (mc.screen instanceof DeathScreen) {
            mc.player.respawn();
            mc.setScreen(null);
        }
        sayac++;
        if (MOD.equals("sicaklik_coldsweat")) {
            coldSweat(mc);
        } else {
            hud(mc);
        }
    }

    private static void hud(Minecraft mc) {
        String ad = mc.player.getGameProfile().getName();
        if (adim < 0) {
            if (!mc.player.hasPermissions(2) || !IstemciSicaklik.acik() || sayac < 100) {
                return;
            }
            DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}, OP, HUD paketi geldi", ad);
            adim = 0;
            sayac = 0;
        }
        if (adim == 0) {
            if (sahne >= SAHNELER.size()) {
                mc.player.connection.sendCommand("dogalafet sicaklik set " + ad + " 0");
                DogalAfetler.LOGGER.info(ON + "bitti; FPS {}", mc.getFps());
                adim = 2;
                sayac = 0;
                return;
            }
            Sahne s = SAHNELER.get(sahne);
            mc.player.setXRot(8f);
            mc.player.connection.sendCommand("dogalafet sicaklik set " + ad + " " + s.deger());
            adim = 1;
            sayac = 0;
        } else if (adim == 1) {
            Sahne s = SAHNELER.get(sahne);
            if (sayac == 40) {
                kare(mc, s.ad());
            } else if (sayac == 70 && s.ikinciKare()) {
                // Gizli workspace'te istemci ~1 FPS çizer: titreşimin ikinci kareşi yeni bir çizimden gelsin.
                kare(mc, s.ad() + "_b");
            } else if (sayac >= (s.ikinciKare() ? 76 : 46)) {
                sahne++;
                adim = 0;
            }
        } else if (adim == 2 && sayac >= 400) {
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            adim = 3;
            mc.stop();
        }
    }

    private static void coldSweat(Minecraft mc) {
        if (adim < 0 && sayac >= 200) {
            DogalAfetler.LOGGER.info(ON + "coldsweat: cold_sweat yüklü={}, bizim HUD paketi açık={}",
                    ModList.get().isLoaded("cold_sweat"), IstemciSicaklik.acik());
            mc.player.connection.sendCommand("dogalafet sicaklik bilgi " + mc.player.getGameProfile().getName());
            adim = 0;
            sayac = 0;
        } else if (adim == 0 && sayac == 60) {
            kare(mc, "coldsweat");
            DogalAfetler.LOGGER.info(ON + "coldsweat: 60 tick sonra bizim HUD paketi açık={}", IstemciSicaklik.acik());
        } else if (adim == 0 && sayac >= 100) {
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            adim = 3;
            mc.stop();
        }
    }

    private static void kare(Minecraft mc, String ad) {
        String dosya = "sicaklik_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> { });
        DogalAfetler.LOGGER.info(ON + "kare {} (vücut {} kademe {}, FPS {})", dosya,
                String.format(Locale.ROOT, "%.1f", IstemciSicaklik.durum().vucut()), IstemciSicaklik.durum().kademe(),
                mc.getFps());
    }
}
