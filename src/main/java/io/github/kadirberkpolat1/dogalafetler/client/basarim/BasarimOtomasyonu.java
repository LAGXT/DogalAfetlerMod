package io.github.kadirberkpolat1.dogalafetler.client.basarim;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.advancements.Advancement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Başarım görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=basarim} ile, tek oyunculu
 * Quick Play dünyasında ve {@code -PoyunTestiAfeti=kasirga} ile çalışır; üretimde etkisizdir. Sırayla: seviye 5 sınama
 * kasırgasının sireniyle kök toast'ı, doğal bitişte üç başarımın toast'ı ve sohbet duyurusu, ünvanlı sohbet satırı,
 * ünvanlı tab listesi, başarım sekmesinin erken hali (kök + iki kat), gizliler dışındaki bütün başarımlar açıkken
 * sekmenin dört kaydırma konumu (gizli iki başarım görünmez) ve gizliler açılınca son hali. Klavye, fare ve tekerlek
 * girdileri kapanışa kadar sayılır; kendi açmadığı duraklatma ekranını kapatır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class BasarimOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.otomasyon", "");
    private static final String ON = "BASARIM_OTOMASYON ";
    private static final ResourceLocation KOK = DogalAfetler.konum("dogalafetler/root");

    private record Adim(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
    }

    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static int tus;
    private static int fare;
    private static int teker;
    private static int sayac;
    private static int adim = -1;
    private static int adimTick;

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
            if (adim < ADIMLAR.size()) {
                ADIMLAR.get(adim).is().run();
            }
        }
    }

    private static void ekle(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
        ADIMLAR.add(new Adim(ad, is, hazir, enCokTick));
    }

    private static void komut(Minecraft mc, String k) {
        DogalAfetler.LOGGER.info(ON + "komut /{}", k);
        mc.player.connection.sendCommand(k);
    }

    private static BooleanSupplier bekle(int tick) {
        return () -> adimTick >= tick;
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
            // Komut çıktıları sohbet karesini kirletmesin; duyuru ve ünvanlı sohbet satırı kalsın (kapanışta geri açılır).
            for (String k : new String[] {"gamerule sendCommandFeedback false", "gamemode survival",
                    "difficulty peaceful", "gamerule doDaylightCycle false",
                    "time set 6000", "weather clear 100000", "gamerule doWeatherCycle false",
                    "gamerule announceAdvancements true", "advancement revoke @s from dogalafetler:dogalafetler/root",
                    "dogalafet unvan @s yok", "dogalafet stop", "scoreboard objectives add basarim_kanit dummy",
                    "scoreboard objectives setdisplay list basarim_kanit", "xp set @s 0 levels",
                    "xp set @s 0 points"}) {
                komut(mc, k);
            }
        }, bekle(60), 80);
        ekle("kasırga S5", () -> komut(mc, "dogalafet start kasirga 5 @s"), bekle(40), 60);
        ekle("siren", () -> komut(mc, "dogalafet asama ileri"), kazanildi(mc, Basarimlar.KOK), 200);
        ekle("kök toast", () -> { }, bekle(30), 40);
        ekle("kök toast kare", () -> kare(mc, "toast_kok"), bekle(10), 20);
        ekle("aktif", () -> komut(mc, "dogalafet asama ileri"), bekle(60), 80);
        ekle("bitiş", () -> komut(mc, "dogalafet asama ileri"), bekle(40), 60);
        ekle("doğal son", () -> komut(mc, "dogalafet asama ileri"), kazanildi(mc, Basarimlar.FELAKET_USTASI), 200);
        ekle("toast'lar", () -> { }, bekle(25), 30);
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
        ekle("toast temizliği", () -> mc.getToasts().clear(), bekle(20), 30);
        ekle("sekme tam", () -> {
            mc.getToasts().clear();
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
        ekle("sekme gizliler açık", () -> {
            mc.getToasts().clear();
            sekmeyiAc(mc);
        }, bekle(40), 60);
        ekle("en üste kaydır 2", () -> kaydir(mc, 1000), bekle(10), 20);
        ekle("gizliler kare 1", () -> kare(mc, "sekme_gizliler_acik_ust"), bekle(10), 20);
        ekle("kaydır 4", () -> kaydir(mc, -1000), bekle(10), 20);
        ekle("gizliler kare 2", () -> kare(mc, "sekme_gizliler_acik_alt"), bekle(10), 20);
        ekle("kapanış", () -> {
            mc.setScreen(null);
            komut(mc, "gamerule announceAdvancements true");
            komut(mc, "gamerule sendCommandFeedback true");
            komut(mc, "dogalafet basarim bilgi @s");
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
        Advancement kok = liste.getAdvancements().get(KOK);
        if (kok != null) {
            liste.setSelectedTab(kok, true);
        }
        DogalAfetler.LOGGER.info(ON + "sekme açıldı (kök {})", kok != null ? "var" : "yok");
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
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, ekran {})", dosya, mc.getFps(),
                mc.screen == null ? "yok" : mc.screen.getClass().getSimpleName());
    }
}
