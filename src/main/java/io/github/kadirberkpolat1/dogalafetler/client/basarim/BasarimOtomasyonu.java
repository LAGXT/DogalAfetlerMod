package io.github.kadirberkpolat1.dogalafetler.client.basarim;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Başarım görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=basarim} ile, tek oyunculu
 * Quick Play dünyasında ve {@code -PoyunTestiAfeti=kasirga} ile çalışır; üretimde etkisizdir. Sırayla: seviye 5 sınama
 * kasırgasının sireniyle kök toast'ı, üç başarımın toast'ı ve sohbet duyurusu (tür ve seviye 3 ayrı tick'lerde verilir,
 * doğal bitiş seviye 5'i ünvanıyla açar: sıra belirlenimci, A-35), ünvanlı sohbet satırı, ünvanlı tab listesi (liste
 * hedefi: tek oyunculu dünyada tek oyuncuyla vanilla listeyi ancak liste hedefi varken çizer), başarım sekmesinin erken
 * hali (kök + iki kat), gizliler dışındaki bütün başarımlar açıkken sekmenin dört kaydırma konumu (gizli iki başarım
 * görünmez) ve gizliler açılınca
 * üç konum (gizliler ortada tam görünür). Sahne komutları entegre sunucunun konsol kaynağından çıktısız verilir:
 * sohbete komut satırı (başarı ya da hata) düşmez; sohbet kareden önce temizlenir (A-33). Toast üreten komuttan önce
 * istemci en az {@value #AKICI_FPS} FPS çizmeli ve toast karesi ancak toast'ın kayma süresi boyunca gerçekten kare
 * çizildikten sonra alınır: görünmeyen workspace'te FPS 1'e düşünce framebuffer bayat kalıyordu (A-34). Sekme
 * açıldıktan sonra istemcinin iç imleç konumu pencerenin köşesine alınır (ipucu kutusu sekmeyi karartmasın). Klavye,
 * fare ve tekerlek girdileri kapanışa kadar sayılır; kendi açmadığı duraklatma ekranını kapatır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class BasarimOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.otomasyon", "");
    private static final String ON = "BASARIM_OTOMASYON ";
    private static final ResourceLocation KOK = DogalAfetler.konum("dogalafetler/root");
    /** Toast üreten komuttan önce beklenen en az FPS. */
    private static final int AKICI_FPS = 20;

    private record Adim(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
    }

    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static int tus;
    private static int fare;
    private static int teker;
    private static int sayac;
    private static int adim = -1;
    private static int adimTick;
    /** Çizilen kare sayısı ({@code RenderTickEvent}), adımın başındaki değeri ve biten adımda çizilen kare. */
    private static int kareSayaci;
    private static int adimKaresi;
    private static int oncekiAdimKaresi;

    private BasarimOtomasyonu() {
    }

    private static boolean aktif() {
        return MOD.equals("basarim");
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
    public static void cizildi(TickEvent.RenderTickEvent olay) {
        if (aktif() && olay.phase == TickEvent.Phase.END) {
            kareSayaci++;
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
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        sayac++;
        if (adim < 0) {
            if (sayac < 200 || !mc.player.hasPermissions(2)) {
                return;
            }
            plan(mc);
            adim = 0;
            adimTick = 0;
            adimKaresi = kareSayaci;
            DogalAfetler.LOGGER.info(ON + "başlıyor: {} adım", ADIMLAR.size());
            ADIMLAR.get(0).is().run();
            return;
        }
        if (adim >= ADIMLAR.size()) {
            return;
        }
        adimTick++;
        Adim a = ADIMLAR.get(adim);
        boolean hazir = a.hazir().getAsBoolean();
        if (hazir || adimTick >= a.enCokTick()) {
            if (!hazir) {
                DogalAfetler.LOGGER.warn(ON + "{}: {} tick'te koşul sağlanmadı, devam", a.ad(), adimTick);
            }
            adim++;
            adimTick = 0;
            oncekiAdimKaresi = kareSayaci - adimKaresi;
            adimKaresi = kareSayaci;
            if (adim < ADIMLAR.size()) {
                ADIMLAR.get(adim).is().run();
            }
        }
    }

    private static void ekle(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
        ADIMLAR.add(new Adim(ad, is, hazir, enCokTick));
    }

    /**
     * Sahne komutu: entegre sunucunun konsol kaynağından (yetki 4), çıktısız; {@code @s} oyuncunun adına çevrilir.
     * Başarı ya da hata satırı sohbete düşmez (inceleme A4).
     */
    private static void komut(Minecraft mc, String k) {
        sunucuKomutu(mc, k, true);
    }

    /** Kanıt komutu: çıktısı sunucu log'una gider (sohbete değil). */
    private static void logKomutu(Minecraft mc, String k) {
        sunucuKomutu(mc, k, false);
    }

    private static void sunucuKomutu(Minecraft mc, String k, boolean sessiz) {
        MinecraftServer server = mc.getSingleplayerServer();
        String satir = k.replace("@s", mc.player.getGameProfile().getName());
        DogalAfetler.LOGGER.info(ON + "komut /{}", satir);
        server.execute(() -> {
            CommandSourceStack kaynak = server.createCommandSourceStack();
            server.getCommands().performPrefixedCommand(sessiz ? kaynak.withSuppressedOutput() : kaynak, satir);
        });
    }

    private static void sohbetiTemizle(Minecraft mc) {
        mc.gui.getChat().clearMessages(false);
    }

    private static BooleanSupplier bekle(int tick) {
        return () -> adimTick >= tick;
    }

    /** En az {@code tick} tick geçti ve adım başından beri en az {@code kare} kare çizildi. */
    private static BooleanSupplier cizimle(int tick, int kare) {
        return () -> adimTick >= tick && kareSayaci - adimKaresi >= kare;
    }

    /** İstemci akıcı çiziyor: toast'ın 5 sn'lik görünürlüğü içinde kareye girecek kadar. */
    private static BooleanSupplier akici(Minecraft mc) {
        return () -> adimTick >= 20 && mc.getFps() >= AKICI_FPS;
    }

    /** Başarım oyuncuda açık mı (entegre sunucudan, sunucu thread'inde okunur). */
    private static BooleanSupplier kazanildi(Minecraft mc, Basarimlar basarim) {
        return () -> {
            MinecraftServer server = mc.getSingleplayerServer();
            UUID kimlik = mc.player.getUUID();
            ResourceLocation yol = DogalAfetler.konum("dogalafetler/" + basarim.yol());
            return server != null && server.submit(() -> {
                ServerPlayer oyuncu = server.getPlayerList().getPlayer(kimlik);
                Advancement a = server.getAdvancements().getAdvancement(yol);
                return oyuncu != null && a != null && oyuncu.getAdvancements().getOrStartProgress(a).isDone();
            }).join();
        };
    }

    private static void plan(Minecraft mc) {
        ekle("kurulum", () -> {
            mc.options.hideGui = false;
            for (String k : new String[] {"gamemode survival @s", "difficulty peaceful",
                    "gamerule doDaylightCycle false", "time set 6000", "weather clear 100000",
                    "gamerule doWeatherCycle false",
                    "gamerule announceAdvancements true", "advancement revoke @s from dogalafetler:dogalafetler/root",
                    "dogalafet unvan @s yok", "scoreboard objectives add basarim_kanit dummy",
                    "scoreboard objectives setdisplay list basarim_kanit",
                    "dogalafet stop", "xp set @s 0 levels", "xp set @s 0 points"}) {
                komut(mc, k);
            }
        }, bekle(60), 80);
        ekle("sohbeti temizle", () -> sohbetiTemizle(mc), bekle(5), 10);
        ekle("kasırga S5", () -> komut(mc, "dogalafet start kasirga 5 @s"), bekle(40), 60);
        ekle("akıcı çizim (kök toast)", () -> { }, akici(mc), 1200);
        ekle("siren", () -> komut(mc, "dogalafet asama ileri"), kazanildi(mc, Basarimlar.KOK), 200);
        ekle("kök toast", () -> { }, cizimle(25, 20), 60);
        ekle("kök toast kare", () -> kare(mc, "toast_kok"), bekle(10), 20);
        ekle("aktif", () -> komut(mc, "dogalafet asama ileri"), bekle(60), 80);
        ekle("akıcı çizim (son)", () -> { }, akici(mc), 1200);
        // Doğal bitişte üç başarım aynı tetikleyicinin dinleyicileri; sıraları garanti değil. Vitrinde sıra sabit: tür ve
        // seviye 3 ayrı tick'lerde verilir, doğal bitiş yalnız seviye 5'i (ünvanlı meydan okuma) açar (A-35).
        ekle("İlk Rüzgâr", () -> komut(mc, "advancement grant @s only dogalafetler:dogalafetler/"
                + Basarimlar.ILK_RUZGAR.yol()), kazanildi(mc, Basarimlar.ILK_RUZGAR), 40);
        ekle("Fırtınaya Meydan Oku", () -> komut(mc, "advancement grant @s only dogalafetler:dogalafetler/"
                + Basarimlar.FIRTINAYA_MEYDAN_OKU.yol()), kazanildi(mc, Basarimlar.FIRTINAYA_MEYDAN_OKU), 40);
        // AKTIF → BITIS: sınama afetinin sönümü hemen biter, afet doğal sonlanır (dördüncü "asama ileri" yok).
        ekle("doğal son", () -> komut(mc, "dogalafet asama ileri"), kazanildi(mc, Basarimlar.FELAKET_USTASI), 200);
        ekle("toast'lar", () -> { }, cizimle(25, 20), 60);
        ekle("toast kare", () -> kare(mc, "toast_felaket_ustasi"), bekle(10), 20);
        ekle("ünvanlı sohbet", () -> mc.player.connection.sendChat("Felaketi atlattık!"), bekle(40), 60);
        ekle("sohbet ekranı", () -> mc.setScreen(new ChatScreen("")), bekle(20), 30);
        ekle("sohbet kare", () -> kare(mc, "sohbet_duyurusu"), bekle(10), 20);
        ekle("tab listesi", () -> {
            mc.setScreen(null);
            mc.options.keyPlayerList.setDown(true);
        }, bekle(20), 30);
        ekle("tab kare", () -> kare(mc, "tab_unvan"), bekle(10), 20);
        ekle("sekme erken", () -> {
            mc.options.keyPlayerList.setDown(false);
            mc.getToasts().clear();
            sohbetiTemizle(mc);
            sekmeyiAc(mc);
        }, bekle(40), 60);
        ekle("sekme erken kare", () -> kare(mc, "sekme_erken"), bekle(10), 20);
        ekle("gizliler dışında hepsi", () -> {
            mc.setScreen(null);
            komut(mc, "gamerule announceAdvancements false");
            for (Basarimlar b : Basarimlar.values()) {
                if (!b.gizli()) {
                    komut(mc, "advancement grant @s only dogalafetler:dogalafetler/" + b.yol());
                }
            }
        }, kazanildi(mc, Basarimlar.KUTUP_USTASI), 200);
        ekle("toast'lar gelsin", () -> { }, bekle(30), 40);
        ekle("sekme tam", () -> {
            mc.getToasts().clear();
            sohbetiTemizle(mc);
            sekmeyiAc(mc);
        }, bekle(40), 60);
        ekle("en üste kaydır", () -> kaydir(mc, 1000), bekle(10), 20);
        ekle("sekme tam kare 1", () -> kare(mc, "sekme_tam_ust"), bekle(10), 20);
        ekle("kaydır 1", () -> kaydir(mc, -100), bekle(10), 20);
        ekle("sekme tam kare 2", () -> kare(mc, "sekme_tam_orta"), bekle(10), 20);
        ekle("kaydır 2", () -> kaydir(mc, -100), bekle(10), 20);
        ekle("sekme tam kare 3", () -> kare(mc, "sekme_tam_orta2"), bekle(10), 20);
        ekle("kaydır 3", () -> kaydir(mc, -1000), bekle(10), 20);
        ekle("sekme tam kare 4", () -> kare(mc, "sekme_tam_alt"), bekle(10), 20);
        ekle("gizliler", () -> {
            mc.setScreen(null);
            komut(mc, "advancement grant @s only dogalafetler:dogalafetler/" + Basarimlar.BIRLIKTE_HAYATTA.yol());
            komut(mc, "advancement grant @s only dogalafetler:dogalafetler/" + Basarimlar.HEPSINI_ATLATTIM.yol());
        }, kazanildi(mc, Basarimlar.HEPSINI_ATLATTIM), 200);
        ekle("gizli toast'lar gelsin", () -> { }, bekle(30), 40);
        ekle("sekme gizliler açık", () -> {
            mc.getToasts().clear();
            sohbetiTemizle(mc);
            sekmeyiAc(mc);
        }, bekle(40), 60);
        ekle("en üste kaydır 2", () -> kaydir(mc, 1000), bekle(10), 20);
        ekle("gizliler kare 1", () -> kare(mc, "sekme_gizliler_acik_ust"), bekle(10), 20);
        // Felaket Ustası kolu (#23 → #24 → #25, #23 → #26) pencerenin ortasına: #25 üst kenarda kırpılmasın (A6).
        ekle("gizlileri ortala", () -> kaydir(mc, -170), bekle(10), 20);
        ekle("gizliler kare 2", () -> kare(mc, "sekme_gizliler_acik_orta"), bekle(10), 20);
        ekle("kaydır 4", () -> kaydir(mc, -1000), bekle(10), 20);
        ekle("gizliler kare 3", () -> kare(mc, "sekme_gizliler_acik_alt"), bekle(10), 20);
        ekle("kapanış", () -> {
            mc.setScreen(null);
            komut(mc, "gamerule announceAdvancements true");
            komut(mc, "scoreboard objectives remove basarim_kanit");
            logKomutu(mc, "dogalafet basarim bilgi @s");
            DogalAfetler.LOGGER.info(ON + "bitti; FPS {}", mc.getFps());
        }, bekle(40), 60);
        ekle("çıkış", () -> {
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
        }, bekle(1), 2);
    }

    private static void sekmeyiAc(Minecraft mc) {
        ClientAdvancements liste = mc.player.connection.getAdvancements();
        mc.setScreen(new AdvancementsScreen(liste));
        imleciKenaraAl(mc);
        Advancement kok = liste.getAdvancements().get(KOK);
        if (kok != null) {
            liste.setSelectedTab(kok, true);
        }
        DogalAfetler.LOGGER.info(ON + "sekme açıldı (kök {})", kok != null ? "var" : "yok");
    }

    /**
     * İstemcinin iç imleç konumunu pencerenin sol üst köşesine alır: sabit imleç bir widget'ın üstünde kalırsa vanilla
     * ipucu kutusu çıkar ve sekmeyi karartır. Ekran açıldıktan sonra çağrılır: {@code setScreen} →
     * {@code releaseMouse} konumu pencere ortasına geri alır. Gerçek imlece ve sahibin oturumuna dokunmaz (yalnız
     * {@code MouseHandler} alanı; SRG adları {@code build/createSrgToMcp/output.srg}).
     */
    private static void imleciKenaraAl(Minecraft mc) {
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, 0.0, "f_91507_");
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, 0.0, "f_91508_");
    }

    /** Seçili sekmeyi dikey kaydırır (ekranın sürükleme yolu; gerçek fare girdisi değildir, sayılmaz). */
    private static void kaydir(Minecraft mc, double dy) {
        if (mc.screen instanceof AdvancementsScreen ekran) {
            ekran.mouseDragged(0, 0, 0, 0, 0);
            ekran.mouseDragged(0, 0, 0, 0, dy);
        }
    }

    private static void kare(Minecraft mc, String ad) {
        String dosya = "basarim_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> { });
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, önceki adımda çizilen kare {}, ekran {})", dosya, mc.getFps(),
                oncekiAdimKaresi, mc.screen == null ? "yok" : mc.screen.getClass().getSimpleName());
    }
}
