package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.su.SuRenderer;
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
 * Tsunami görsel kanıt otomasyonu (K0-21 rev. 2): yalnız {@code -Potomasyon=tsunami}. Sanal suyun vitrin sahnesinde (kıyı köyü,
 * iskele; {@code /dogalafet su vitrin}) gerçek tsunamiyi komutla başlatır: S5 ve S1 için ön-uyarı, siren, çekilme, ufuktaki
 * dalga, vitrin anı (kıyıya vurmadan önceki son saniyeler), çarpma, karada, durdurma sonrası kareleri; dalga sırasında
 * ortanca kare süresi ve sunucu mspt; mağara suyu ve mağara-sualtı (SU-40 yeniden puanlama). Kurulumdan kapanışa kadar
 * bütün tuş, fare ve tekerlek olayları log'a yazılır (sıfırdan farklıysa koşu geçersiz).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class TsunamiOtomasyonu {
    private static final boolean ACIK = "tsunami".equals(System.getProperty("dogalafetler.otomasyon"));
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

    private TsunamiOtomasyonu() {
    }

    private static void log(String s, Object... a) {
        DogalAfetler.LOGGER.info("TSUNAMI-OTOMASYON " + s, a);
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

    /** Bir tsunami koşusu: vitrin yeniden kurulur, kamera evlerin arasından denize bakar, afet AKTIF'e ilerletilir. */
    private static List<String> tsunami(int seviye, String on) {
        // AKTIF'in ilk anından: çekilme 8 sn, ufuktaki dalga 13 sn, ölçüm 13-18 sn, vitrin anı 24 sn (kıyıya ~28 sn),
        // çarpma 31 sn, karada 36-41 sn.
        return List.of("k:dogalafet su vitrin", "b:300", "k:time set 10500", "k:dogalafet su vitrin kamera tepe", "b:200",
                "s:" + on + "-once", "k:dogalafet start tsunami " + seviye, "b:60", "s:" + on + "-onuyari",
                "k:dogalafet asama ileri", "b:60", "s:" + on + "-siren", "k:dogalafet asama ileri", "b:160",
                "s:" + on + "-cekilme", "b:100", "s:" + on + "-ufuk", "f:" + on + "-yaklasma", "b:120",
                "s:" + on + "-vitrin", "b:140", "s:" + on + "-carpma", "b:100", "s:" + on + "-karada",
                "f:" + on + "-karada", "k:dogalafet stop", "b:100", "s:" + on + "-sonra", "k:dogalafet su temizle", "b:40");
    }

    @SubscribeEvent
    public static void giris(ClientPlayerNetworkEvent.LoggingIn e) {
        if (!ACIK || girdi) {
            return;
        }
        girdi = true;
        Minecraft mc = Minecraft.getInstance();
        mc.options.renderDistance().set(12);
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        mc.options.hideGui = true;
        boolean hepsi = "hepsi".equals(BOLUM);
        List<String> a = new ArrayList<>(List.of("k:gamerule doDaylightCycle false", "k:weather clear 100000",
                "k:gamerule doMobSpawning false", "k:dogalafet denizseviyesi 0", "b:40"));
        if (hepsi || BOLUM.contains("s5")) {
            a.addAll(tsunami(5, "tsunami-s5"));
        }
        if (hepsi || BOLUM.contains("s1")) {
            a.addAll(tsunami(1, "tsunami-s1"));
        }
        if (hepsi || BOLUM.contains("magara")) {
            a.addAll(List.of("m:kur", "b:200", "s:tsunami-magara-once", "m:doldur", "b:160", "f:magara", "s:tsunami-magara",
                    "m:icine", "b:100", "s:tsunami-magara-sualti", "k:dogalafet su temizle", "b:40"));
        }
        a.add("x:son");
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
                double enUzun = s.isEmpty() ? Double.NaN : s.get(s.size() - 1) / 1e6;
                var sunucu = mc.getSingleplayerServer();
                float mspt = sunucu == null ? Float.NaN : sunucu.getAverageTickTime();
                log("FPS {} ortanca_kare_ms={} fps_ortanca={} en_uzun_kare_ms={} kare={} sunucu_mspt={} tps={} su={} yagmur={}",
                        olcum, String.format(Locale.ROOT, "%.2f", ortanca), String.format(Locale.ROOT, "%.1f", 1000 / ortanca),
                        String.format(Locale.ROOT, "%.1f", enUzun), s.size(), String.format(Locale.ROOT, "%.2f", mspt),
                        String.format(Locale.ROOT, "%.1f", Math.min(20, 1000 / mspt)), SuRenderer.olcumOzeti(),
                        TsunamiYagmuru.ozet());
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
            case 's' -> {
                log("KARE {} {}", deger, TsunamiYagmuru.ozet());
                Screenshot.grab(mc.gameDirectory, deger + ".png", mc.getMainRenderTarget(),
                        m -> log("FOTO {} {}", deger, m.getString()));
            }
            case 'm' -> magara(mc, deger);
            case 'x' -> {
                log("BITTI girdi tus={} fare={} teker={}", tus, fare, teker);
                mc.stop();
            }
            default -> log("bilinmeyen adım {}", adim);
        }
    }

    /**
     * Mağara sahnesi (SU-40 yeniden puanlama): oyuncunun altında taş oda (hava y 10..16), damlataş tavanda ışıltaşı yamaları,
     * duvarda deniz fenerleri; tsunaminin kullandığı aynı {@code magaraDoldur} yoluyla dolar; kamera dolu cebin yüzeyinin
     * üstünde (y 14) ve sonra suyun altında.
     */
    private static void magara(Minecraft mc, String ne) {
        if ("kur".equals(ne)) {
            ox = mc.player.getBlockX();
            oz = mc.player.getBlockZ();
            List<String> k = new ArrayList<>(List.of("fill %d 8 %d %d 19 %d stone".formatted(ox - 10, oz - 10, ox + 10, oz + 10),
                    "fill %d 10 %d %d 16 %d air".formatted(ox - 8, oz - 8, ox + 8, oz + 8),
                    "fill %d 17 %d %d 17 %d dripstone_block".formatted(ox - 8, oz - 8, ox + 8, oz + 8),
                    "fill %d 10 %d %d 10 %d gravel".formatted(ox - 8, oz - 8, ox + 8, oz - 4)));
            for (int[] q : new int[][] {{-6, -6}, {-1, -6}, {4, -6}, {-6, -1}, {-1, -1}, {4, -1}, {-6, 4}, {-1, 4}, {4, 4}}) {
                k.add("fill %d 17 %d %d 17 %d glowstone".formatted(ox + q[0], oz + q[1], ox + q[0] + 2, oz + q[1] + 2));
            }
            for (int d = -7; d <= 7; d += 3) {
                k.add("setblock %d 13 %d sea_lantern".formatted(ox - 9, oz + d));
                k.add("setblock %d 13 %d sea_lantern".formatted(ox + d, oz - 9));
            }
            k.add("time set 6000");
            k.add("tp @s %d 14.0 %d -45 27".formatted(ox - 7, oz - 7));
            k.forEach(mc.player.connection::sendCommand);
        } else if ("icine".equals(ne)) {
            mc.player.connection.sendCommand("tp @s %d.5 11 %d.5 -45 -22".formatted(ox - 5, oz - 5));
        } else {
            mc.player.connection.sendCommand("dogalafet su magara %d 10 %d 900 600".formatted(ox, oz));
        }
    }
}
