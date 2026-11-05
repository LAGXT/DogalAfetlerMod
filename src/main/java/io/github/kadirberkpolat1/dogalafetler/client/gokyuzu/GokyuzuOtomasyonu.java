package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spike otomasyonu: klavye/fare olmadan kanıt toplar. Yalnız {@code -Dogalafetler.spike=gokyuzu} (Gradle:
 * {@code -Pspike=gokyuzu}) ya da {@code gokyuzu-kalicilik} ile açılır; aksi halde hiçbir şey yapmaz.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class GokyuzuOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.spike", "");
    private static final Deque<Adim> KUYRUK = new ArrayDeque<>();
    private static boolean basladi;
    private static int bekleme;

    private GokyuzuOtomasyonu() {
    }

    /** Bir otomasyon adımı; {@code true} dönerse bitmiştir, {@code false} ise sonraki tick yeniden çağrılır. */
    private interface Adim {
        boolean calis(Minecraft mc);
    }

    @SubscribeEvent
    public static void girdi(ClientPlayerNetworkEvent.LoggingIn olay) {
        if (basladi || !MOD.startsWith("gokyuzu")) {
            return;
        }
        basladi = true;
        bekleme = 240;
        if (MOD.equals("gokyuzu-kalicilik")) {
            kalicilikPlani();
        } else {
            matrisPlani();
        }
        DogalAfetler.LOGGER.info("[gokyuzu-oto] başladı: mod={}, adım={}", MOD, KUYRUK.size());
    }

    @SubscribeEvent
    public static void tik(TickEvent.ClientTickEvent olay) {
        Minecraft mc = Minecraft.getInstance();
        if (olay.phase != TickEvent.Phase.END || !basladi || mc.player == null || mc.level == null) {
            return;
        }
        if (bekleme > 0) {
            bekleme--;
            return;
        }
        while (!KUYRUK.isEmpty()) {
            if (!KUYRUK.peek().calis(mc)) {
                return;
            }
            KUYRUK.poll();
            if (bekleme > 0) {
                return;
            }
        }
    }

    // ------------------------------------------------------------------ adımlar

    private static void komut(String k) {
        KUYRUK.add(mc -> {
            DogalAfetler.LOGGER.info("[gokyuzu-oto] komut: /{}", k);
            mc.player.connection.sendCommand(k);
            return true;
        });
    }

    private static void bekle(int tik) {
        KUYRUK.add(mc -> {
            bekleme = tik;
            return true;
        });
    }

    private static void calistir(Runnable r) {
        KUYRUK.add(mc -> {
            r.run();
            return true;
        });
    }

    private static void cek(String ad) {
        KUYRUK.add(mc -> {
            Screenshot.grab(mc.gameDirectory, ad + ".png", mc.getMainRenderTarget(),
                    m -> DogalAfetler.LOGGER.info("[gokyuzu-oto] görüntü: {} → {}", ad, m.getString()));
            DogalAfetler.LOGGER.info("[gokyuzu-oto] FPS {} = {} (kalite {}, durum {})", ad, mc.getFps(),
                    IstemciAyarlari.EFEKT_KALITESI.get(), IstemciGokyuzu.durum());
            return true;
        });
    }

    /** {@code saniye} boyunca her saniye {@code getFps()} örneği alır; ilk örnek ısınma diye atılır. */
    private static void fps(String etiket, int saniye) {
        int[] sayac = {0};
        int[] toplam = {0};
        int[] enAz = {Integer.MAX_VALUE};
        int[] enCok = {0};
        KUYRUK.add(mc -> {
            sayac[0]++;
            if (sayac[0] % 20 != 0) {
                return false;
            }
            int ornek = sayac[0] / 20;
            if (ornek > 1) {
                int f = mc.getFps();
                toplam[0] += f;
                enAz[0] = Math.min(enAz[0], f);
                enCok[0] = Math.max(enCok[0], f);
            }
            if (ornek < saniye + 1) {
                return false;
            }
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT,
                    "[gokyuzu-oto] FPS-OLCUM %s kalite=%s ort=%.1f min=%d maks=%d (%d sn) durum=%s",
                    etiket, IstemciAyarlari.EFEKT_KALITESI.get(), toplam[0] / (double) saniye, enAz[0], enCok[0],
                    saniye, IstemciGokyuzu.durum()));
            return true;
        });
    }

    private static void kalite(EfektKalitesi k) {
        calistir(() -> {
            IstemciAyarlari.EFEKT_KALITESI.set(k);
            DogalAfetler.LOGGER.info("[gokyuzu-oto] efekt kalitesi: {}", k);
        });
    }

    private static void kanca(IstemciGokyuzu.Kanca k, boolean ortu) {
        calistir(() -> {
            IstemciGokyuzu.kanca = k;
            IstemciGokyuzu.ayOrtusu = ortu;
            DogalAfetler.LOGGER.info("[gokyuzu-oto] kanca: {}, ay örtüsü: {}", k, ortu);
        });
    }

    private static void durdur() {
        calistir(() -> {
            DogalAfetler.LOGGER.info("[gokyuzu-oto] bitti, oyun kapatılıyor");
            Minecraft.getInstance().stop();
        });
    }

    private static void hazirlik() {
        calistir(() -> Minecraft.getInstance().options.hideGui = true);
        komut("gamemode spectator");
        komut("gamerule doDaylightCycle false");
        komut("gamerule doWeatherCycle false");
        komut("weather clear");
    }

    private static void bakis(float sapma, float egim) {
        komut(String.format(Locale.ROOT, "tp @s ~ ~ ~ %.1f %.1f", sapma, egim));
    }

    private static void durum(String ay, String gunes, float ilerleme) {
        komut(String.format(Locale.ROOT, "dogalafet spike gokyuzu %s %s %.3f", ay, gunes, ilerleme));
    }

    private static void zamanVeCek(int zaman, String ad) {
        komut("time set " + zaman);
        bekle(60);
        cek(ad);
    }

    // ------------------------------------------------------------------ planlar

    private static void matrisPlani() {
        hazirlik();
        komut("execute positioned over motion_blocking_no_leaves run tp @s ~ ~6 ~ -90 -70");
        bekle(100);

        String[] evreler = {"normal", "buyuyen", "supernova", "gunessiz", "kizilcuce"};
        float[] ilerlemeler = {0f, 0.7f, 0.1f, 0.5f, 1f};
        for (String ay : new String[] {"on", "off"}) {
            for (int i = 0; i < evreler.length; i++) {
                durum(ay, evreler[i], ilerlemeler[i]);
                zamanVeCek(6000, "ay-" + ay + "_" + evreler[i] + "_gunduz");
                zamanVeCek(18000, "ay-" + ay + "_" + evreler[i] + "_gece");
            }
        }

        // Özel anlar: kızıl cüce doğuşu (şafak, doğuya ufka bakış) ve süpernova tepe anı.
        bakis(-90f, -8f);
        durum("on", "kizilcuce", 0.25f);
        zamanVeCek(23000, "ozel_kizilcuce_dogus_ay-on_23000");
        durum("off", "kizilcuce", 0.25f);
        zamanVeCek(23000, "ozel_kizilcuce_dogus_ay-off_23000");
        bakis(-90f, -70f);
        durum("on", "supernova", 0.02f);
        zamanVeCek(6000, "ozel_supernova_tepe_6000");
        bakis(-90f, -38f);
        zamanVeCek(3000, "ozel_supernova_tepe_ufuk_3000");
        durum("on", "buyuyen", 1f);
        zamanVeCek(3000, "ozel_buyuyen_son_ufuk_3000");
        bakis(-90f, -70f);

        // FPS tablosu (Yüksek), sonra Düşük ve Ultra.
        String[][] sahneler = {
                {"vanilla_gece", "on", "normal", "0", "18000"},
                {"vanilla_gunduz", "on", "normal", "0", "6000"},
                {"nebula_gece", "off", "gunessiz", "0.5", "18000"},
                {"supernova_tepe", "on", "supernova", "0.02", "6000"},
                {"kizilcuce_gunduz", "on", "kizilcuce", "1", "6000"},
        };
        for (EfektKalitesi k : new EfektKalitesi[] {EfektKalitesi.YUKSEK, EfektKalitesi.DUSUK, EfektKalitesi.ULTRA}) {
            kalite(k);
            for (String[] s : sahneler) {
                if (k != EfektKalitesi.YUKSEK && s[0].startsWith("vanilla")) {
                    continue;
                }
                durum(s[1], s[2], Float.parseFloat(s[3]));
                komut("time set " + s[4]);
                bekle(60);
                fps(s[0], 6);
            }
            cek("kalite_" + k.name().toLowerCase(Locale.ROOT) + "_kizilcuce_gunduz");
            durum("off", "gunessiz", 0.5f);
            zamanVeCek(18000, "kalite_" + k.name().toLowerCase(Locale.ROOT) + "_nebula_gece");
        }
        kalite(EfektKalitesi.YUKSEK);

        // Kanca (c) karşılaştırması: vanilla gökyüzü + AFTER_SKY'de derin uzay; ay gizlenemez, örtü denemesi.
        kanca(IstemciGokyuzu.Kanca.AFTER_SKY, false);
        durum("off", "gunessiz", 0.5f);
        zamanVeCek(18000, "kanca_c_after_sky_ay_gorunur_gece");
        zamanVeCek(6000, "kanca_c_after_sky_gunes_gorunur_gunduz");
        kanca(IstemciGokyuzu.Kanca.AFTER_SKY, true);
        zamanVeCek(18000, "kanca_c_after_sky_ay_ortulu_gece");
        fps("kanca_c_after_sky_nebula_gece", 5);
        kanca(IstemciGokyuzu.Kanca.SARMALAYICI, false);

        // Kalıcılık için son durum: ikinci açılışta (gokyuzu-kalicilik) aynı görüntü beklenir.
        durum("off", "gunessiz", 0.5f);
        zamanVeCek(18000, "kalicilik_1_once");
        bekle(40);
        durdur();
    }

    private static void kalicilikPlani() {
        calistir(() -> {
            Minecraft.getInstance().options.hideGui = true;
            DogalAfetler.LOGGER.info("[gokyuzu-oto] kalıcılık: komut gönderilmeden önbellekteki durum = {}",
                    IstemciGokyuzu.durum());
        });
        bekle(60);
        cek("kalicilik_2_sonra");
        fps("kalicilik_2_sonra", 3);
        bekle(40);
        durdur();
    }
}
