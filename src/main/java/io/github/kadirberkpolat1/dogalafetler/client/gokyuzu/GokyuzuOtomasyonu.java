package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
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
    private static final String MOD = modOku();
    private static final boolean ACIK = !MOD.isEmpty();
    private static final Deque<Predicate<Minecraft>> KUYRUK = new ArrayDeque<>();
    private static int tus;
    private static int dugme;
    private static int teker;
    private static int bekleme;
    private static boolean basladi;
    /** İlk bekleme bitti mi; ondan sonra açılan her ekran kapatılır (yükleme ekranına dokunulmaz). */
    private static boolean hazir;
    private static float sapma = -90f;
    private static float egim = -40f;

    private GokyuzuOtomasyonu() {
    }

    private static String modOku() {
        Path bayrak = FMLPaths.GAMEDIR.get().resolve(BAYRAK);
        if (FMLEnvironment.production || !Files.exists(bayrak)) {
            return "";
        }
        try {
            String icerik = Files.readString(bayrak).trim();
            return icerik.isEmpty() ? "tam" : icerik;
        } catch (IOException hata) {
            return "tam";
        }
    }

    /** Etkin otomasyon planı ({@code tam}, {@code ezilme}, {@code ezilme-yansimasiz}); kapalıysa boş. */
    static String mod() {
        return MOD;
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
        if (MOD.startsWith("ezilme")) {
            ezilmePlani();
        } else if (MOD.equals("samanyolu")) {
            samanyoluPlani();
        } else {
            plan();
        }
        KUYRUK.add(mc -> {
            mc.player.connection.sendCommand("dogalafet kizilcuce bilgi");
            DogalAfetler.LOGGER.info("[gokyuzu-oto] BİTTİ girdi: tuş={} düğme={} teker={}", tus, dugme, teker);
            mc.stop();
            return true;
        });
        DogalAfetler.LOGGER.info("[gokyuzu-oto] başladı: mod={}, adım={}", MOD, KUYRUK.size());
    }

    @SubscribeEvent
    public static void tik(TickEvent.ClientTickEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        if (olay.phase != TickEvent.Phase.END || !basladi || mc.player == null || mc.level == null) {
            return;
        }
        mc.options.hideGui = true;
        if (hazir && mc.screen != null) {
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
        hazir = true;
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

    /** Koşul görünümde sağlanınca sürer (tick sayımından bağımsız: düşük FPS'te de aynı an). */
    private static void kosul(Predicate<IstemciGokyuzu.Gorunum> kosul) {
        KUYRUK.add(mc -> kosul.test(IstemciGokyuzu.gorunum(0f)));
    }

    private static void cek(String ad) {
        KUYRUK.add(mc -> {
            mc.gui.getChat().clearMessages(false);
            mc.getToasts().clear();
            bekleme = 10;
            return true;
        });
        KUYRUK.add(mc -> {
            Screenshot.grab(mc.gameDirectory, "gokyuzu-" + (MOD.equals("tam") ? "" : MOD + "-") + ad + ".png", mc.getMainRenderTarget(), m -> {
            });
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT,
                    "[gokyuzu-oto] ÇEKİM %s fps=%d kalite=%s görünüm=[%s] saat=%.0f günSaati=%d kanca=%s tuş=%d düğme=%d teker=%d",
                    ad, mc.getFps(), IstemciAyarlari.efektKalitesi(), IstemciGokyuzu.gorunum(0f), IstemciDonem.saat(0f),
                    mc.level.getDayTime() % 24_000L, IstemciGokyuzu.kanca, tus, dugme, teker));
            return true;
        });
    }

    /** {@code saniye} boyunca saniyede bir {@code getFps()} örneği alır (ilki ısınma); ortanca, en az ve en çok log'a gider. */
    private static void fps(String etiket, int saniye) {
        int[] sayac = {0};
        List<Integer> ornekler = new ArrayList<>();
        KUYRUK.add(mc -> {
            if (sayac[0] == 0) {
                GokyuzuCizici.ortalamaCizimMikrosaniye();
            }
            sayac[0]++;
            if (sayac[0] % 20 != 0) {
                return false;
            }
            if (sayac[0] > 20) {
                ornekler.add(mc.getFps());
            }
            if (sayac[0] < (saniye + 1) * 20) {
                return false;
            }
            Collections.sort(ornekler);
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT,
                    "[gokyuzu-oto] FPS %s ortanca=%d en_az=%d en_cok=%d örnek=%d bizim_cizim_cpu=%.0fus kalite=%s "
                            + "görünüm=[%s]", etiket, ornekler.get(ornekler.size() / 2), ornekler.get(0),
                    ornekler.get(ornekler.size() - 1), ornekler.size(), GokyuzuCizici.ortalamaCizimMikrosaniye(),
                    IstemciAyarlari.efektKalitesi(), IstemciGokyuzu.gorunum(0f)));
            return true;
        });
    }

    private static void evre(String evre, float ilerleme) {
        komut(String.format(Locale.ROOT, "dogalafet gokyuzutest evre %s %.3f", evre, ilerleme));
    }

    private static void ay(boolean var) {
        komut("dogalafet gokyuzutest ay " + (var ? "var" : "yok"));
    }

    /** Gün saati ve bakış (doğu, {@code egim} derece yukarı), bekleme, kare. */
    private static void an(long saat, float egim, int tik, String ad) {
        komut("time set " + saat);
        bakis(-90f, egim);
        bekle(tik);
        cek(ad);
    }

    /** Gözlemci kipinde Y=150: ağaçlar ve tepeler ufku kapatmaz (spike G2 kamerası). */
    private static void hazirlik() {
        komut("dogalafet sifirla");
        komut("gamemode spectator");
        komut("gamerule doDaylightCycle false");
        komut("gamerule doWeatherCycle false");
        komut("weather clear");
        komut("tp @s ~ 150 ~ -90 -35");
        bekle(100);
    }

    private static void plan() {
        int gecis = 22 * 20;
        hazirlik();
        an(3000, -35f, 40, "01-normal-gunduz");
        fps("normal-gunduz", 6);
        evre("buyuyen", 0.3f);
        an(3000, -35f, gecis, "02a-buyuyen-baslangic");
        evre("buyuyen", 0.85f);
        an(3000, -35f, 60, "02b-buyuyen-son");
        ay(false);
        an(18000, -70f, 40, "02c-buyuyen-gece-aysiz");
        ay(true);
        komut("time set 3000");
        bakis(-90f, -35f);
        bekle(40);
        evre("supernova", 0.08f);
        kosul(g -> g.evre() == GunesEvresi.SUPERNOVA && g.saniye() >= 3f);
        cek("03a-supernova-cokme");
        kosul(g -> g.saniye() >= GokyuzuEgrileri.SN_COKME_SN + 1.5f);
        cek("03b-supernova-parlama");
        kosul(g -> g.saniye() >= 45f);
        cek("03c-supernova-vitrin");
        fps("supernova-vitrin", 6);
        an(6000, -70f, 40, "03d-supernova-ogle-tepe");
        evre("supernova", 0.45f);
        ay(false);
        an(18000, -70f, 60, "03e-supernova-gece-aysiz");
        ay(true);
        komut("dogalafet asama ileri");
        an(6000, -70f, 30 * 20, "04a-gunessiz-ogle-tepe");
        an(3000, -35f, 40, "04b-gunessiz-sabah-ufuk");
        an(18000, -70f, 40, "05-gunessiz-gece-ayli");
        ay(false);
        an(18000, -70f, 40, "06a-gunessiz-gece-aysiz-vitrin");
        fps("gunessiz-gece-aysiz", 6);
        an(18000, -30f, 40, "06b-gunessiz-gece-aysiz-ufuk");
        komut("dogalafet gokyuzutest donma 100");
        komut("time set 23000");
        bakis(-90f, -10f);
        komut("dogalafet kizilcuce baslat");
        bekle(gecis);
        cek("07a-kizilcuce-dogus-ilk-isik");
        evre("kizil_cuce", 0.25f);
        an(23600, -10f, 60, "07b-kizilcuce-yukselis");
        komut("dogalafet asama ileri");
        an(3000, -35f, 60, "08a-kizilcuce-sabah-aysiz");
        an(6000, -70f, 40, "08b-kizilcuce-ogle-tepe-aysiz");
        fps("kizilcuce-ogle", 6);
        an(18000, -70f, 40, "09a-kizilcuce-gece-aysiz");
        ay(true);
        an(18000, -70f, 40, "09b-kizilcuce-gece-ayli");
        an(3000, -35f, 40, "09c-kizilcuce-sabah-ayli");
        komut("dogalafet sifirla");
        ay(false);
        an(18000, -70f, 60, "10-normal-gece-aysiz");
        an(6000, -70f, 40, "12-normal-gunduz-aysiz");
        ay(true);
        an(18000, -70f, 40, "11-normal-gece-ayli");
        fps("normal-gece-ayli", 6);
    }

    /** Samanyolu bandının gündüz görünümü (görsel değerlendirme P2): güneşsiz ve kızıl cüce gündüzü, karşılaştırma gecesi. */
    private static void samanyoluPlani() {
        hazirlik();
        evre("gunessiz", 0.5f);
        an(6000, -70f, 22 * 20, "s1-gunessiz-ogle-tepe");
        an(3000, -35f, 40, "s2-gunessiz-sabah-ufuk");
        ay(false);
        an(18000, -70f, 40, "s3-gunessiz-gece-aysiz");
        evre("kizil_cuce", 1f);
        an(6000, -70f, 22 * 20, "s4-kizilcuce-ogle-tepe");
    }

    /** K0-26 ezilme denemesi: normal durumda kazananın göğü, aysız + güneşsiz durumda bizim göğümüz. */
    private static void ezilmePlani() {
        hazirlik();
        an(6000, -70f, 40, "e1-normal-ayli-ogle");
        ay(false);
        an(6000, -70f, 40, "e2-normal-aysiz-ogle");
        an(18000, -70f, 40, "e3-normal-aysiz-gece");
        evre("gunessiz", 0.5f);
        an(6000, -70f, 22 * 20, "e4-gunessiz-aysiz-ogle");
        an(18000, -70f, 40, "e5-gunessiz-aysiz-gece");
    }
}
