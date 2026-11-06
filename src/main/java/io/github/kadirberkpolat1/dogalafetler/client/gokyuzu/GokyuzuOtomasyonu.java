package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Yalnız geliştirme ortamında ve oyun klasöründe {@value #BAYRAK} dosyası varken çalışan görsel kanıt otomasyonu (K0-21
 * rev. 2): klavye/fare kullanmadan {@code /dogalafet} komutlarıyla evreleri geçer, sabit bakışla kare kaydeder, FPS'i
 * log'a yazar ve kapanır. Oyun içi girdi olayları (tuş, fare düğmesi, tekerlek) açılıştan kapanışa sayılır; sıfır
 * olmalı. Otomasyon açmadığı ekranı kapatır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class GokyuzuOtomasyonu {
    static final String BAYRAK = "dogalafetler-gokyuzu-otomasyon";
    private static final boolean ACIK = !FMLEnvironment.production
            && Files.exists(FMLPaths.GAMEDIR.get().resolve(BAYRAK));
    private static final Deque<Predicate<Minecraft>> KUYRUK = new ArrayDeque<>();
    private static int tus;
    private static int dugme;
    private static int teker;
    private static int bekleme;
    private static boolean basladi;
    private static float sapma = -90f;
    private static float egim = -40f;

    private GokyuzuOtomasyonu() {
    }

    @SubscribeEvent
    public static void tusOlayi(InputEvent.Key olay) {
        if (ACIK) {
            tus++;
        }
    }

    @SubscribeEvent
    public static void dugmeOlayi(InputEvent.MouseButton.Pre olay) {
        if (ACIK) {
            dugme++;
        }
    }

    @SubscribeEvent
    public static void tekerOlayi(InputEvent.MouseScrollingEvent olay) {
        if (ACIK) {
            teker++;
        }
    }

    @SubscribeEvent
    public static void girdi(ClientPlayerNetworkEvent.LoggingIn olay) {
        if (!ACIK || basladi) {
            return;
        }
        basladi = true;
        bekleme = 200;
        plan();
        DogalAfetler.LOGGER.info("[gokyuzu-oto] başladı, adım={}", KUYRUK.size());
    }

    @SubscribeEvent
    public static void tik(TickEvent.ClientTickEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        if (olay.phase != TickEvent.Phase.END || !basladi || mc.player == null || mc.level == null) {
            return;
        }
        mc.options.hideGui = true;
        if (mc.screen != null) {
            DogalAfetler.LOGGER.warn("[gokyuzu-oto] otomasyonun açmadığı ekran kapatıldı: {}", mc.screen.getClass().getSimpleName());
            mc.setScreen(null);
        }
        if (mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
        }
        bak(mc.player);
        if (bekleme > 0) {
            bekleme--;
            return;
        }
        while (!KUYRUK.isEmpty() && bekleme == 0) {
            if (!KUYRUK.peek().test(mc)) {
                return;
            }
            KUYRUK.poll();
        }
    }

    private static void bak(LocalPlayer p) {
        p.setYRot(sapma);
        p.setXRot(egim);
        p.yRotO = sapma;
        p.xRotO = egim;
        p.setYHeadRot(sapma);
        p.yHeadRotO = sapma;
    }

    private static void komut(String k) {
        KUYRUK.add(mc -> {
            DogalAfetler.LOGGER.info("[gokyuzu-oto] komut: /{}", k);
            mc.player.connection.sendCommand(k);
            bekleme = 2;
            return true;
        });
    }

    private static void bekle(int tik) {
        KUYRUK.add(mc -> {
            bekleme = tik;
            return true;
        });
    }

    private static void bakis(float yeniSapma, float yeniEgim) {
        KUYRUK.add(mc -> {
            sapma = yeniSapma;
            egim = yeniEgim;
            return true;
        });
    }

    private static void cek(String ad) {
        KUYRUK.add(mc -> {
            mc.gui.getChat().clearMessages(false);
            mc.getToasts().clear();
            bekleme = 10;
            return true;
        });
        KUYRUK.add(mc -> {
            Screenshot.grab(mc.gameDirectory, "gokyuzu-" + ad + ".png", mc.getMainRenderTarget(), m -> {
            });
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT,
                    "[gokyuzu-oto] ÇEKİM %s fps=%d kalite=%s görünüm=[%s] saat=%.0f günSaati=%d kanca=%s tuş=%d düğme=%d teker=%d",
                    ad, mc.getFps(), IstemciAyarlari.efektKalitesi(), IstemciGokyuzu.gorunum(0f), IstemciDonem.saat(0f),
                    mc.level.getDayTime() % 24_000L, IstemciGokyuzu.kanca, tus, dugme, teker));
            return true;
        });
    }

    private static void plan() {
        komut("dogalafet sifirla");
        komut("gamerule doDaylightCycle false");
        komut("gamerule doWeatherCycle false");
        komut("weather clear");
        komut("time set 3000");
        bakis(-90f, -40f);
        bekle(100);
        cek("01-normal-gunduz");
        komut("dogalafet gokyuzutest evre buyuyen 0.85");
        bekle(60);
        cek("02-buyuyen");
        komut("dogalafet gokyuzutest evre supernova 0.06");
        bekle(4 * 20);
        cek("03a-supernova-cokme");
        bekle(6 * 20);
        cek("03b-supernova-parlama");
        bekle(30 * 20);
        cek("03c-supernova-vitrin");
        komut("dogalafet asama ileri");
        bekle(25 * 20);
        cek("04-gunessiz-gunduz");
        komut("time set 18000");
        bakis(-90f, -75f);
        bekle(60);
        cek("05-gunessiz-gece-ayli");
        komut("dogalafet gokyuzutest ay yok");
        bekle(60);
        cek("06-gunessiz-gece-aysiz");
        komut("time set 23300");
        bakis(-90f, -12f);
        komut("dogalafet kizilcuce baslat");
        bekle(45 * 20);
        cek("07-kizilcuce-dogus");
        komut("dogalafet asama ileri");
        komut("time set 3000");
        bakis(-90f, -40f);
        bekle(60);
        cek("08-kizilcuce-gunduz");
        komut("time set 18000");
        bakis(-90f, -75f);
        bekle(60);
        cek("09-kizilcuce-gece-aysiz");
        komut("dogalafet sifirla");
        komut("dogalafet gokyuzutest ay yok");
        bekle(60);
        cek("10-normal-gece-aysiz");
        komut("dogalafet gokyuzutest ay var");
        bekle(60);
        cek("11-normal-gece-ayli");
        komut("dogalafet kizilcuce bilgi");
        KUYRUK.add(mc -> {
            DogalAfetler.LOGGER.info("[gokyuzu-oto] BİTTİ girdi: tuş={} düğme={} teker={}", tus, dugme, teker);
            mc.stop();
            return true;
        });
    }
}
