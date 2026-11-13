package io.github.kadirberkpolat1.dogalafetler.client.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Dosyadan adım okuyan görsel otomasyon (K0-21 rev. 2): yalnız {@code -Potomasyon=adim -PotomasyonBolum=<adım dosyası>} ile
 * açılır; gölgelendirici paketinin sahnelerini kod değiştirmeden kurmak içindir. Satırlar sırayla yürür: {@code k <komut>}
 * (başında / olmadan), {@code b <tick>} bekleme, {@code s <ad>} ekran görüntüsü ({@code screenshots/<ad>.png}, A-34 kare
 * kapısı), {@code r <ayar dosyası>} gölgelendirici ayarlarını o dosyayla değiştirip Oculus'u yeniden yükler (yansımayla
 * {@code net.irisshaders.iris.Iris.reload}; paket adı {@code DogalAfetlerShader}), {@code x} kapanış. Boş satır ve {@code #} ile
 * başlayan satır atlanır. Tuş, fare ve tekerlek olayları sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class AdimOtomasyonu {
    private static final boolean ACIK = "adim".equals(System.getProperty("dogalafetler.otomasyon"));
    private static final String OTOMASYON = "adim";
    private static final Deque<String> ADIMLAR = new ArrayDeque<>();
    private static int tus;
    private static int fare;
    private static int teker;
    private static boolean girdi;
    private static long bekle = 200;

    private AdimOtomasyonu() {
    }

    private static void log(String s, Object... a) {
        DogalAfetler.LOGGER.info("ADIM-OTOMASYON " + s, a);
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
        GorselOtomasyon.cizimAyari();
        mc.options.hideGui = true;
        Path dosya = Path.of(System.getProperty("dogalafetler.otomasyon.bolum", ""));
        try {
            for (String satir : Files.readAllLines(dosya, StandardCharsets.UTF_8)) {
                String s = satir.strip();
                if (!s.isEmpty() && !s.startsWith("#")) {
                    ADIMLAR.add(s);
                }
            }
        } catch (IOException ex) {
            log("adım dosyası okunamadı: {}", dosya.toAbsolutePath());
        }
        ADIMLAR.add("x");
        log("BASLADI dosya={} adim={}", dosya.toAbsolutePath(), ADIMLAR.size());
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
        if (mc.player == null || mc.level == null || GorselOtomasyon.kareBekliyor() || --bekle > 0) {
            return;
        }
        String adim = ADIMLAR.poll();
        if (adim == null) {
            return;
        }
        log("ADIM {}", adim);
        String deger = adim.length() > 2 ? adim.substring(2).strip() : "";
        bekle = 1;
        switch (adim.charAt(0)) {
            case 'k' -> mc.player.connection.sendCommand(deger.startsWith("/") ? deger.substring(1) : deger);
            case 'b' -> bekle = Long.parseLong(deger);
            case 'r' -> paketiYenidenYukle(mc, deger);
            case 's' -> GorselOtomasyon.kareIste(OTOMASYON, deger + ".png",
                    k -> log("FOTO {} kapi={} fps={}", deger, k.gecerli() ? "acik" : "GECERSIZ", k.fps()));
            case 'x' -> {
                GorselOtomasyon.kareOzetiYaz(OTOMASYON);
                log("BITTI girdi tus={} fare={} teker={}", tus, fare, teker);
                mc.stop();
            }
            default -> log("bilinmeyen adım {}", adim);
        }
    }

    private static void paketiYenidenYukle(Minecraft mc, String ayar) {
        try {
            Files.copy(Path.of(ayar), mc.gameDirectory.toPath().resolve("shaderpacks/DogalAfetlerShader.txt"),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Class.forName("net.irisshaders.iris.Iris").getMethod("reload").invoke(null);
            log("YENIDEN-YUKLE {}", ayar);
        } catch (ReflectiveOperationException | IOException ex) {
            log("YENIDEN-YUKLE başarısız {}: {}", ayar, ex);
        }
    }
}
