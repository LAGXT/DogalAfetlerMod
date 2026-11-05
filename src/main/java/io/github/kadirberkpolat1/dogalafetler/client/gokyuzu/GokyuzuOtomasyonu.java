package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Spike otomasyonu: klavye/fare olmadan kanıt toplar. Yalnız {@code -Ddogalafetler.spike=...} (Gradle:
 * {@code -Pspike=gokyuzu | gokyuzu-ozel | gokyuzu-kalicilik}) ile açılır; aksi halde hiçbir şey yapmaz. Pencere
 * sahibinin fare hareketi kamerayı çevirmesin diye fare bırakılır ve bakış açısı her tick sabitlenir.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class GokyuzuOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.spike", "");
    private static final Deque<Adim> KUYRUK = new ArrayDeque<>();
    private static boolean basladi;
    private static int bekleme;
    private static boolean bakisKilitli;
    /** İlk bekleme bitti mi; ondan sonra açık kalan her ekran kapatılır. */
    private static boolean hazir;
    private static float hedefSapma = -90f;
    private static float hedefEgim = -70f;

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
        switch (MOD) {
            case "gokyuzu-kalicilik" -> kalicilikPlani();
            case "gokyuzu-ozel" -> ozelPlani();
            case "gokyuzu-ezilme" -> ezilmePlani("ezilme_");
            case "gokyuzu-ezilme-yansimasiz" -> ezilmePlani("ezilme_yansimasiz_");
            default -> matrisPlani();
        }
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
            // K0-21 rev.: otomasyon hiç ekran açmaz; açılan ekran (başkasının tuşu, PauseScreen) kapatılır.
            DogalAfetler.LOGGER.warn("[gokyuzu-oto] otomasyonun açmadığı ekran kapatıldı: {}",
                    mc.screen.getClass().getSimpleName());
            mc.setScreen(null);
        }
        if (mc.mouseHandler.isMouseGrabbed()) {
            mc.mouseHandler.releaseMouse();
        }
        if (bakisKilitli) {
            bakisUygula(mc.player);
        }
        if (bekleme > 0) {
            bekleme--;
            return;
        }
        hazir = true;
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

    private static void bakisUygula(LocalPlayer p) {
        p.setYRot(hedefSapma);
        p.setXRot(hedefEgim);
        p.yRotO = hedefSapma;
        p.xRotO = hedefEgim;
        p.setYHeadRot(hedefSapma);
        p.yHeadRotO = hedefSapma;
    }

    private static String yuk() {
        try {
            String[] p = Files.readString(Path.of("/proc/loadavg")).trim().split(" ");
            return p[0] + "/" + p[1];
        } catch (IOException | RuntimeException e) {
            return "?";
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

    /** Toast ve sohbet temizlenir, birkaç kare çizildikten sonra ana çerçeve kaydedilir. */
    private static void cek(String ad) {
        KUYRUK.add(mc -> {
            mc.getToasts().clear();
            mc.gui.getChat().clearMessages(false);
            bekleme = 10;
            return true;
        });
        KUYRUK.add(mc -> {
            Screenshot.grab(mc.gameDirectory, ad + ".png", mc.getMainRenderTarget(),
                    m -> DogalAfetler.LOGGER.info("[gokyuzu-oto] görüntü: {} → {}", ad, m.getString()));
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT,
                    "[gokyuzu-oto] ÇEKİM %s fps=%d kalite=%s durum=[%s] zaman=%d bakış=(%.1f, %.1f) y=%.1f pencere=%dx%d "
                            + "ekran=%s yük=%s",
                    ad, mc.getFps(), IstemciAyarlari.EFEKT_KALITESI.get(), IstemciGokyuzu.durum(),
                    mc.level.getDayTime() % 24000L, mc.player.getYRot(), mc.player.getXRot(), mc.player.getY(),
                    mc.getWindow().getWidth(), mc.getWindow().getHeight(),
                    mc.screen == null ? "yok" : mc.screen.getClass().getSimpleName(), yuk()));
            return true;
        });
    }

    /**
     * {@code saniye} boyunca her saniye {@code getFps()} örneği alır (ilki ısınma diye atılır); aynı aralıkta gökyüzü
     * bölümünün GPU/CPU süresini {@link GokyuzuOlcum} toplar.
     */
    private static void fps(String etiket, int saniye) {
        int[] sayac = {0};
        List<Integer> ornekler = new ArrayList<>();
        KUYRUK.add(mc -> {
            if (sayac[0] == 0) {
                GokyuzuCizici.ortalamaCizimMikrosaniye();
                GokyuzuOlcum.aktif = true;
                GokyuzuOlcum.ozetVeSifirla();
            }
            sayac[0]++;
            if (sayac[0] % 20 != 0) {
                return false;
            }
            int ornek = sayac[0] / 20;
            if (ornek > 1) {
                ornekler.add(mc.getFps());
            }
            if (ornek < saniye + 1) {
                return false;
            }
            Collections.sort(ornekler);
            double ort = ornekler.stream().mapToInt(Integer::intValue).average().orElse(0);
            double[] o = GokyuzuOlcum.ozetVeSifirla();
            DogalAfetler.LOGGER.info(String.format(Locale.ROOT,
                    "[gokyuzu-oto] FPS-OLCUM %s kalite=%s fps_ort=%.1f fps_ortanca=%d min=%d maks=%d (%d sn) "
                            + "gok_gpu_ort=%.0fus gok_gpu_ortanca=%.0fus gok_gpu_min=%.0fus gok_cpu=%.0fus (%d kare) "
                            + "bizim_ciz_cpu=%.0fus durum=[%s] yük=%s",
                    etiket, IstemciAyarlari.EFEKT_KALITESI.get(), ort, ornekler.get(ornekler.size() / 2),
                    ornekler.get(0), ornekler.get(ornekler.size() - 1), saniye, o[0], o[5], o[4], o[1], (long) o[2],
                    GokyuzuCizici.ortalamaCizimMikrosaniye(), IstemciGokyuzu.durum(), yuk()));
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

    private static void bakis(float sapma, float egim) {
        calistir(() -> {
            hedefSapma = sapma;
            hedefEgim = egim;
            bakisKilitli = true;
        });
        komut(String.format(Locale.ROOT, "tp @s ~ ~ ~ %.1f %.1f", sapma, egim));
    }

    private static void hazirlik() {
        komut("gamemode spectator");
        komut("gamerule doDaylightCycle false");
        komut("gamerule doWeatherCycle false");
        komut("weather clear");
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

    /** 10 kombinasyon × gündüz/gece, zemin+6 bloktan tepeye bakış (ağaç silueti kadrajda). */
    private static void matrisPlani() {
        hazirlik();
        komut("execute positioned over motion_blocking_no_leaves run tp @s ~ ~6 ~ -90 -70");
        bakis(-90f, -70f);
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
        ozelAnlar();
        kalicilikSonDurum();
    }

    /**
     * Özel anlar Y=150'den (ağaçlar ufku kapatmasın): kızıl cüce doğuşu, süpernova tepe anı, büyüyen güneş; kalite
     * karşılaştırması, FPS turları, kanca (c) karşılaştırması; sonda kalıcılık için ayırt edici bir durum bırakılır.
     */
    private static void ozelPlani() {
        hazirlik();
        ozelAnlar();

        // Kalite karşılaştırması: Düşük / Orta / Yüksek / Ultra, 2 tur iç içe.
        bakis(-90f, -70f);
        EfektKalitesi[] kaliteler = {EfektKalitesi.DUSUK, EfektKalitesi.ORTA, EfektKalitesi.YUKSEK, EfektKalitesi.ULTRA};
        for (int tur = 1; tur <= 2; tur++) {
            for (EfektKalitesi k : kaliteler) {
                kalite(k);
                durum("off", "gunessiz", 0.5f);
                komut("time set 18000");
                bekle(40);
                if (tur == 1) {
                    cek("kalite_" + k.name().toLowerCase(Locale.ROOT) + "_nebula_gece");
                }
                fps("kalite_nebula_gece_tur" + tur, 5);
                durum("on", "supernova", 0.02f);
                komut("time set 6000");
                bekle(40);
                fps("kalite_supernova_tepe_tur" + tur, 5);
            }
        }
        kalite(EfektKalitesi.YUKSEK);

        // FPS tablosu (Yüksek): sahneler 2 tur sırayla, sapma birikmesin diye iç içe.
        String[][] sahneler = {
                {"vanilla_gece", "on", "normal", "0", "18000"},
                {"nebula_gece", "off", "gunessiz", "0.5", "18000"},
                {"supernova_tepe", "on", "supernova", "0.02", "6000"},
                {"kizilcuce_gunduz", "on", "kizilcuce", "1", "6000"},
                {"vanilla_gunduz", "on", "normal", "0", "6000"},
        };
        for (int tur = 1; tur <= 2; tur++) {
            for (String[] s : sahneler) {
                durum(s[1], s[2], Float.parseFloat(s[3]));
                komut("time set " + s[4]);
                bekle(40);
                fps(s[0] + "_tur" + tur, 6);
            }
        }

        // Kanca (c): vanilla gökyüzü + AFTER_SKY'de derin uzay; ay ve güneş gizlenemez, örtü denemesi.
        kanca(IstemciGokyuzu.Kanca.AFTER_SKY, false);
        durum("off", "gunessiz", 0.5f);
        zamanVeCek(18000, "kanca_c_after_sky_ay_gorunur_gece");
        zamanVeCek(6000, "kanca_c_after_sky_gunes_gorunur_gunduz");
        fps("kanca_c_after_sky_gunessiz_gunduz", 5);
        kanca(IstemciGokyuzu.Kanca.AFTER_SKY, true);
        zamanVeCek(18000, "kanca_c_after_sky_ay_ortulu_gece");
        bakis(-90f, -20f);
        zamanVeCek(13500, "kanca_c_after_sky_ay_ortulu_ufukta_13500");
        kanca(IstemciGokyuzu.Kanca.SARMALAYICI, false);
        zamanVeCek(13500, "kanca_b_karsilastirma_aysiz_ufukta_13500");

        kalicilikSonDurum();
    }

    /** Özel anlar Y=150'den (ağaçlar ufku kapatmasın); güneş boyu vanilla ile aynı açıdan karşılaştırılır. */
    private static void ozelAnlar() {
        komut("tp @s ~ 150 ~ -90 -10");
        bakis(-90f, -10f);
        bekle(100);

        durum("on", "kizilcuce", 0.25f);
        zamanVeCek(23000, "ozel_kizilcuce_dogus_ay-on_23000");
        zamanVeCek(23300, "ozel_kizilcuce_dogus_ay-on_23300");
        zamanVeCek(23600, "ozel_kizilcuce_yukselis_ay-on_23600");
        durum("off", "kizilcuce", 0.25f);
        zamanVeCek(23000, "ozel_kizilcuce_dogus_ay-off_23000");
        zamanVeCek(23600, "ozel_kizilcuce_yukselis_ay-off_23600");
        durum("off", "normal", 0f);
        zamanVeCek(23600, "ozel_karsilastirma_normal_gunes_dogusu_23600");

        bakis(-90f, -70f);
        durum("on", "supernova", 0.02f);
        zamanVeCek(6000, "ozel_supernova_tepe_6000");
        bakis(-90f, -35f);
        zamanVeCek(3000, "ozel_supernova_tepe_ufuk_3000");
        durum("off", "supernova", 0.4f);
        zamanVeCek(3000, "ozel_supernova_sonum_ufuk_3000");
        durum("on", "normal", 0f);
        zamanVeCek(3000, "ozel_karsilastirma_normal_ufuk_3000");
        durum("on", "buyuyen", 0f);
        zamanVeCek(3000, "ozel_buyuyen_0_ufuk_3000");
        durum("on", "buyuyen", 0.3f);
        zamanVeCek(3000, "ozel_buyuyen_baslangic_ufuk_3000");
        durum("on", "buyuyen", 1f);
        zamanVeCek(3000, "ozel_buyuyen_son_ufuk_3000");
        durum("off", "gunessiz", 0.5f);
        zamanVeCek(6000, "ozel_gunessiz_ogle_ufuk_6000");

        bakis(-90f, -70f);
        durum("on", "normal", 0f);
        zamanVeCek(6000, "ozel_karsilastirma_normal_ogle_tepe_6000");
        durum("on", "kizilcuce", 1f);
        zamanVeCek(6000, "ozel_kizilcuce_ogle_tepe_6000");
    }

    /** Kalıcılık: ayırt edici son durum (ay yok, kızıl cüce, öğle); ikinci açılışta komutsuz aynı görüntü beklenir. */
    private static void kalicilikSonDurum() {
        bakis(-90f, -70f);
        durum("off", "kizilcuce", 1f);
        zamanVeCek(6000, "kalicilik_1_once");
        bekle(40);
        durdur();
    }

    /**
     * Ezilme senaryosu ({@code -Pspike=gokyuzu-ezilme} ve {@code gokyuzu-ezilme-yansimasiz}): sahte ikinci gökyüzü modu
     * etkinken normal durumda onun çizimi, aysız+güneşsiz durumda ay ve güneşin yokluğu, normal güneş + ay yok iken
     * gündüz iç nesneye devir ve gece bizim çizim.
     */
    private static void ezilmePlani(String onek) {
        hazirlik();
        komut("execute positioned over motion_blocking_no_leaves run tp @s ~ ~6 ~ -90 -70");
        bakis(-90f, -70f);
        bekle(100);
        durum("on", "normal", 0f);
        zamanVeCek(6000, onek + "normal");
        zamanVeCek(18000, onek + "normal_gece");
        durum("off", "gunessiz", 0.5f);
        zamanVeCek(18000, onek + "ay-off_gunessiz_gece");
        zamanVeCek(6000, onek + "ay-off_gunessiz_gunduz");
        durum("off", "normal", 0f);
        zamanVeCek(6000, onek + "ay-off_normal_gunduz");
        zamanVeCek(18000, onek + "ay-off_normal_gece");
        durum("on", "normal", 0f);
        bekle(40);
        durdur();
    }

    private static void kalicilikPlani() {
        calistir(() -> DogalAfetler.LOGGER.info(
                "[gokyuzu-oto] kalıcılık: komut gönderilmeden önbellekteki durum = {}", IstemciGokyuzu.durum()));
        // Yalnız bakış sabitlenir; gökyüzü durumu, saat ve konum dünya verisinden gelir.
        calistir(() -> {
            hedefSapma = -90f;
            hedefEgim = -70f;
            bakisKilitli = true;
        });
        bekle(60);
        cek("kalicilik_2_sonra");
        fps("kalicilik_2_sonra", 3);
        bekle(40);
        durdur();
    }
}
