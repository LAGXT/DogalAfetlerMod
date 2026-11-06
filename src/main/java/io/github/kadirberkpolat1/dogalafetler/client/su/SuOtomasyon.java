package io.github.kadirberkpolat1.dogalafetler.client.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2): yalnız {@code -Potomasyon=su} (JVM {@code dogalafetler.otomasyon=su}) ile açılır.
 * Girişten sonra sahneleri sırayla kurar (komut), ortanca kare süresini ölçer, ekran görüntüsü alır ve oyunu kapatır.
 * Kurulumdan kapanışa kadar bütün tuş, fare ve tekerlek olayları log'a yazılır (sıfırdan farklıysa koşu geçersiz).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SuOtomasyon {
    private static final boolean ACIK = "su".equals(System.getProperty("dogalafetler.otomasyon"));
    private static final String BOLUM = System.getProperty("dogalafetler.otomasyon.bolum", "hepsi");
    private static final Deque<String> ADIMLAR = new ArrayDeque<>();
    private static final List<Long> KARELER = new ArrayList<>();
    private static int tus;
    private static int fare;
    private static int teker;
    private static boolean girdi;
    private static long bekle = 200;
    private static long sonKare;
    private static String olcum;
    private static int olcumTick;
    private static int ox;
    private static int oz;

    private SuOtomasyon() {
    }

    private static void log(String s, Object... a) {
        DogalAfetler.LOGGER.info("SU-OTOMASYON " + s, a);
    }

    @SubscribeEvent
    public static void tus(InputEvent.Key e) {
        if (ACIK) {
            tus++;
            log("GIRDI tus={} eylem={}", e.getKey(), e.getAction());
        }
    }

    @SubscribeEvent
    public static void fare(InputEvent.MouseButton.Pre e) {
        if (ACIK) {
            fare++;
            log("GIRDI fare={} eylem={}", e.getButton(), e.getAction());
        }
    }

    @SubscribeEvent
    public static void teker(InputEvent.MouseScrollingEvent e) {
        if (ACIK) {
            teker++;
            log("GIRDI teker={}", e.getScrollDelta());
        }
    }

    @SubscribeEvent
    public static void giris(ClientPlayerNetworkEvent.LoggingIn e) {
        if (!ACIK || girdi) {
            return;
        }
        girdi = true;
        Minecraft mc = Minecraft.getInstance();
        mc.options.renderDistance().set(12);
        mc.options.hideGui = true;
        boolean hepsi = "hepsi".equals(BOLUM);
        List<String> a = new ArrayList<>(List.of("k:gamerule doDaylightCycle false", "k:weather clear 100000",
                "k:dogalafet denizseviyesi 0", "k:dogalafet su kiyi", "b:200"));
        if (hepsi || BOLUM.contains("deniz")) {
            a.addAll(List.of("k:time set 12650", "b:200", "f:deniz-afetsiz", "s:sanal-su-deniz-once", "k:dogalafet denizseviyesi 4",
                    "b:300", "f:deniz-afet", "s:sanal-su-deniz-vitrin", "k:dogalafet denizseviyesi 0", "b:60"));
        }
        if (hepsi || BOLUM.contains("dalga")) {
            a.addAll(List.of("k:time set 5000", "k:execute as @s at @s run tp @s ^ ^5 ^28 ~180 6", "b:200", "f:dalga-afetsiz",
                    "k:dogalafet su dalga 10", "b:60", "f:dalga-afet", "s:sanal-su-dalga", "k:dogalafet su temizle", "b:40"));
        }
        if (hepsi || BOLUM.contains("magara")) {
            a.addAll(List.of("m:kur", "b:200", "f:magara-afetsiz", "m:doldur", "b:160", "f:magara-afet", "s:sanal-su-magara",
                    "k:dogalafet asama ileri", "k:dogalafet hiz 8", "b:50", "s:sanal-su-magara-kuruyor", "k:dogalafet hiz 1",
                    "k:dogalafet su temizle"));
        }
        a.add("son");
        ADIMLAR.addAll(a);
        log("BASLADI bolum={} adim={}", BOLUM, ADIMLAR.size());
    }

    @SubscribeEvent
    public static void kare(TickEvent.RenderTickEvent e) {
        if (ACIK && e.phase == TickEvent.Phase.START && olcum != null) {
            long simdi = System.nanoTime();
            if (sonKare != 0) {
                KARELER.add(simdi - sonKare);
            }
            sonKare = simdi;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (!ACIK || e.phase != TickEvent.Phase.END || !girdi) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PauseScreen) {
            log("kendi açmadığım duraklatma ekranı kapatıldı");
            mc.setScreen(null);
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (olcum != null) {
            if (++olcumTick >= 100) {
                List<Long> s = new ArrayList<>(KARELER);
                Collections.sort(s);
                double ortanca = s.isEmpty() ? Double.NaN : s.get(s.size() / 2) / 1e6;
                log("FPS {} ortanca_kare_ms={} fps_ortanca={} kare={} mc_fps={} su={}", olcum,
                        String.format(Locale.ROOT, "%.2f", ortanca), String.format(Locale.ROOT, "%.1f", 1000 / ortanca),
                        s.size(), mc.getFps(), SuRenderer.olcumOzeti());
                olcum = null;
            }
            return;
        }
        if (--bekle > 0) {
            return;
        }
        String adim = ADIMLAR.poll();
        if (adim == null) {
            return;
        }
        log("ADIM {}", adim);
        String deger = adim.length() > 2 ? adim.substring(2) : "";
        bekle = 1;
        switch (adim.charAt(0)) {
            case 'k' -> mc.player.connection.sendCommand(deger);
            case 'b' -> bekle = Long.parseLong(deger);
            case 'f' -> {
                olcum = deger;
                olcumTick = 0;
                sonKare = 0;
                KARELER.clear();
                SuRenderer.olcumSifirla();
            }
            case 's' -> Screenshot.grab(mc.gameDirectory, deger + ".png", mc.getMainRenderTarget(),
                    m -> log("FOTO {} {}", deger, m.getString()));
            case 'm' -> magara(mc, deger);
            default -> {
                log("BITTI girdi tus={} fare={} teker={}", tus, fare, teker);
                mc.stop();
            }
        }
    }

    /** Mağara sahnesi: oyuncunun altında y 8..22 arasında taş bir oda, ışıklı tavan ve sütunlar; kamera içeride. */
    private static void magara(Minecraft mc, String ne) {
        if ("kur".equals(ne)) {
            ox = mc.player.getBlockX();
            oz = mc.player.getBlockZ();
            for (String k : List.of("fill %d 8 %d %d 22 %d stone".formatted(ox - 10, oz - 10, ox + 10, oz + 10),
                    "fill %d 10 %d %d 19 %d air".formatted(ox - 8, oz - 8, ox + 8, oz + 8),
                    "fill %d 20 %d %d 20 %d glowstone".formatted(ox - 8, oz - 8, ox + 8, oz + 8),
                    "fill %d 10 %d %d 19 %d cobblestone".formatted(ox - 3, oz + 2, ox - 3, oz + 2),
                    "fill %d 10 %d %d 13 %d mossy_cobblestone".formatted(ox + 3, oz - 1, ox + 3, oz - 1),
                    "fill %d 10 %d %d 10 %d gravel".formatted(ox - 8, oz - 8, ox + 8, oz - 4),
                    "time set 6000", "tp @s %d 17.5 %d -45 28".formatted(ox - 7, oz - 7))) {
                mc.player.connection.sendCommand(k);
            }
        } else {
            mc.player.connection.sendCommand("dogalafet su magara %d 10 %d 1200 600".formatted(ox, oz));
        }
    }
}
