package io.github.kadirberkpolat1.dogalafetler.client.su;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2): yalnız {@code -Potomasyon=su} (JVM {@code dogalafetler.otomasyon=su}) ile açılır.
 * Girişten sonra sahneleri sırayla kurar (komut), ortanca kare süresini ölçer, ekran görüntüsü alır ve oyunu kapatır.
 * Kareler A-34 kapısından sonra alınır: FPS en az {@value #AKICI_FPS} ve istekten sonra en az iki yeni çizilmiş kare, iç
 * imleç köşede; kare sonraki çizilmiş karenin sonunda alınır (gizli workspace'te bayat framebuffer olmasın). Kurulumdan
 * kapanışa kadar bütün tuş, fare ve tekerlek olayları log'a yazılır (sıfırdan farklıysa koşu geçersiz).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class SuOtomasyon {
    private static final boolean ACIK = "su".equals(System.getProperty("dogalafetler.otomasyon"));
    private static final String BOLUM = System.getProperty("dogalafetler.otomasyon.bolum", "hepsi");
    private static final Deque<String> ADIMLAR = new ArrayDeque<>();
    private static final List<Long> KARELER = new ArrayList<>();
    private static final int AKICI_FPS = 15;
    /** A-34 kapısı en çok 60 sn bekler; açılmazsa kare yine alınır ama geçersizdir ({@code UYARI}). */
    private static final int KAPI_TICK = 1200;
    private static final ThreadMXBean IPLIK = ManagementFactory.getThreadMXBean();
    private static final List<Long> KARE_CPU = new ArrayList<>();
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
    private static double basX;
    private static double basZ;
    private static double yonX;
    private static double yonZ;
    private static long sonCpu;
    private static double tabanMs = Double.NaN;
    private static long cizilen;
    private static String bekleyenKare;
    private static long kapiKare;
    private static int kapiTick;
    private static boolean kareAl;

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
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        mc.options.hideGui = true;
        boolean hepsi = "hepsi".equals(BOLUM);
        List<String> a = new ArrayList<>(List.of("k:gamerule doDaylightCycle false", "k:weather clear 100000",
                "k:dogalafet denizseviyesi 0", "k:dogalafet su vitrin", "b:300"));
        if (hepsi || BOLUM.contains("deniz")) {
            // Vitrin (tasarım notu §3): gün batımı, ay yok, iskelenin ucundan kıyı köyü; sonra +4 denizin altından.
            a.addAll(List.of("k:time set 12500", "k:dogalafet su ay yok", "b:200", "f:deniz-afetsiz", "s:sanal-su-deniz-once",
                    "k:dogalafet denizseviyesi 4", "b:300", "f:deniz-afet", "s:sanal-su-deniz-vitrin",
                    "k:time set 6000", "k:dogalafet su vitrin kamera sualti", "b:100", "s:sanal-su-sualti", "k:dogalafet denizseviyesi 0",
                    "b:40"));
        }
        if (hepsi || BOLUM.contains("dalga")) {
            a.addAll(List.of("k:time set 10500", "k:dogalafet su vitrin kamera tepe", "b:200", "f:dalga-afetsiz",
                    "s:sanal-su-dalga-once", "k:dogalafet su vitrin dalga 16", "b:145", "f:dalga-afet", "s:sanal-su-dalga",
                    "k:dogalafet su temizle", "b:40"));
        }
        if (hepsi || BOLUM.contains("magara")) {
            a.addAll(List.of("m:kur", "b:200", "f:magara-afetsiz", "s:sanal-su-magara-once", "m:doldur", "b:160",
                    "f:magara-afet", "s:sanal-su-magara", "m:icine", "b:100", "s:sanal-su-magara-sualti",
                    "m:kamera", "b:20", "k:dogalafet asama ileri", "k:dogalafet hiz 8", "b:110", "s:sanal-su-magara-kuruyor",
                    "k:dogalafet hiz 1", "k:dogalafet su temizle"));
        }
        a.add("x:son");
        ADIMLAR.addAll(a);
        log("BASLADI bolum={} adim={}", BOLUM, ADIMLAR.size());
    }

    @SubscribeEvent
    public static void kare(TickEvent.RenderTickEvent e) {
        if (!ACIK) {
            return;
        }
        if (e.phase == TickEvent.Phase.START) {
            if (olcum != null) {
                long simdi = System.nanoTime();
                long cpu = IPLIK.getCurrentThreadCpuTime();
                if (sonKare != 0) {
                    KARELER.add(simdi - sonKare);
                    KARE_CPU.add(cpu - sonCpu);
                }
                sonKare = simdi;
                sonCpu = cpu;
            }
            return;
        }
        cizilen++;
        if (kareAl) {
            kareAl = false;
            String ad = bekleyenKare;
            bekleyenKare = null;
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, ad + ".png", mc.getMainRenderTarget(), m -> log("FOTO {} {}", ad, m.getString()));
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
        if (bekleyenKare != null) {
            if (!kareAl) {
                kapiAc(mc);
            }
            return;
        }
        if (olcum != null) {
            if (++olcumTick >= 100) {
                List<Long> s = new ArrayList<>(KARELER);
                Collections.sort(s);
                List<Long> c = new ArrayList<>(KARE_CPU);
                Collections.sort(c);
                double duvar = s.isEmpty() ? Double.NaN : s.get(s.size() / 2) / 1e6;
                double cpu = c.isEmpty() ? Double.NaN : c.get(c.size() / 2) / 1e6;
                if (olcum.endsWith("-afetsiz")) {
                    tabanMs = duvar;
                }
                var sunucu = mc.getSingleplayerServer();
                float mspt = sunucu == null ? Float.NaN : sunucu.getAverageTickTime();
                // K-E29 (b) ölçüm satırı: duvar saati ve render thread CPU ortanca kare ms, aynı sahnenin afetsiz tabanı, mspt,
                // Gradle sayısı (yabancı = bu istemcinin atası olmayan), efekt kademesi.
                log("FPS {} kademe={} duvar_ms={} cpu_ms={} taban_ms={} mspt={} gradle=[{}] fps_ortanca={} kare={} mc_fps={} tps={} "
                        + "su={}", olcum, kademe(), String.format(Locale.ROOT, "%.2f", duvar),
                        String.format(Locale.ROOT, "%.2f", cpu), String.format(Locale.ROOT, "%.2f", tabanMs),
                        String.format(Locale.ROOT, "%.2f", mspt), gradle(), String.format(Locale.ROOT, "%.1f", 1000 / duvar),
                        s.size(), mc.getFps(), String.format(Locale.ROOT, "%.1f", Math.min(20, 1000 / mspt)),
                        SuRenderer.olcumOzeti());
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
                KARE_CPU.clear();
                SuRenderer.olcumSifirla();
            }
            case 's' -> {
                imleciKenaraAl(mc);
                bekleyenKare = deger;
                kapiKare = cizilen;
                kapiTick = 0;
            }
            case 'm' -> magara(mc, deger);
            case 'o' -> oyuncu(mc, deger);
            case 'x' -> {
                log("BITTI girdi tus={} fare={} teker={}", tus, fare, teker);
                mc.stop();
            }
            default -> log("bilinmeyen adım {}", adim);
        }
    }

    /** A-34: FPS en az {@value #AKICI_FPS} ve istekten sonra en az iki yeni çizilmiş kare; süre dolarsa kare geçersiz sayılır. */
    private static void kapiAc(Minecraft mc) {
        long yeni = cizilen - kapiKare;
        boolean acik = mc.getFps() >= AKICI_FPS && yeni >= 2;
        if (acik || ++kapiTick >= KAPI_TICK) {
            log(acik ? "KAPI {} açık fps={} yeni_kare={} tick={}"
                    : "KAPI {} UYARI açılmadı fps={} yeni_kare={} tick={} (kare geçersiz)", bekleyenKare, mc.getFps(), yeni, kapiTick);
            kareAl = true;
        }
    }

    /**
     * İstemcinin iç imleç konumunu pencerenin sol üst köşesine alır (A-34): gerçek imlece ve sahibin oturumuna dokunmaz
     * (yalnız {@code MouseHandler} alanı; SRG adları {@code build/createSrgToMcp/output.srg}).
     */
    private static void imleciKenaraAl(Minecraft mc) {
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, 0.0, "f_91507_");
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, 0.0, "f_91508_");
    }

    private static String kademe() {
        return switch (IstemciAyarlari.efektKalitesi()) {
            case DUSUK -> "Düşük";
            case ORTA -> "Orta";
            case YUKSEK -> "Yüksek";
            case ULTRA -> "Ultra";
        };
    }

    /**
     * Ölçüm geçerliliği (TS-16 rev. 3): komut satırında {@code GradleWrapperMain} geçen süreçler; {@code yabanci} bu istemciyi
     * başlatan kendi {@code runClient} sarmalayıcısı (ata süreç) dışındakiler.
     */
    private static String gradle() {
        Set<Long> ata = new HashSet<>();
        for (ProcessHandle p = ProcessHandle.current().parent().orElse(null); p != null; p = p.parent().orElse(null)) {
            ata.add(p.pid());
        }
        long[] n = new long[2];
        ProcessHandle.allProcesses().filter(h -> h.info().commandLine().orElse("").contains("GradleWrapperMain"))
                .forEach(h -> {
                    n[0]++;
                    if (!ata.contains(h.pid())) {
                        n[1]++;
                    }
                });
        return "toplam=" + n[0] + " yabanci=" + n[1];
    }

    /**
     * Yerel oyuncunun sanal sudaki durumu (§6 #15, istemcide): su ve göz suda mı, hava, can, dikey hız, sanal batıklık;
     * {@code bas} konumu ve bakışın tersini (dalga oyuncuya doğru gelir) kaydeder, sonraki ölçümler dalga yönündeki kaymayı
     * yazar. Oyuncuyu dalgada kendi istemcisi iter (SU-03); kayma bu yolun kanıtıdır.
     */
    private static void oyuncu(Minecraft mc, String etiket) {
        var p = mc.player;
        if ("bas".equals(etiket)) {
            basX = p.getX();
            basZ = p.getZ();
            var bak = p.getLookAngle();
            double boy = Math.max(1e-6, Math.hypot(bak.x, bak.z));
            yonX = -bak.x / boy;
            yonZ = -bak.z / boy;
        }
        double kayma = (p.getX() - basX) * yonX + (p.getZ() - basZ) * yonZ;
        log("OYUNCU {} konum=({} {} {}) suSeviyesi={} suda={} gozSuda={} hava={} can={} dy={} poz={} sanalBatiklik={} "
                + "sanalSuda={} dalgaYonundeKayma={}", etiket, String.format(Locale.ROOT, "%.2f", p.getX()),
                String.format(Locale.ROOT, "%.2f", p.getY()), String.format(Locale.ROOT, "%.2f", p.getZ()),
                String.format(Locale.ROOT, "%.2f", SanalSu.suSeviyesi(p.level(), p.getBlockX(), p.getBlockZ())),
                p.isInWater(), p.isUnderWater(), p.getAirSupply(), String.format(Locale.ROOT, "%.1f", p.getHealth()),
                String.format(Locale.ROOT, "%.3f", p.getDeltaMovement().y), p.getPose(),
                String.format(Locale.ROOT, "%.2f", SanalSu.batiklikOrani(p)),
                SanalSu.icindeMi(p), String.format(Locale.ROOT, "%.2f", kayma));
    }

    /**
     * Mağara sahnesi: oyuncunun altında taş bir oda (hava y 10..16), damlataş tavanda ışıltaşı yamaları, sarkıtlar ve asılı
     * fenerler, yosunlu duvarlar, bir duvarda deniz fenerleri, iki sütun ve çakıl zemin; kamera bir köşede dolu cebin yüzeyinin
     * ({@code ustY} = 13) bir blok üstünde (y 14; altında kalan kamera yansıma görmez, SU-40), tavan ışıklarının yansıması
     * kadrajın ortasında (SU-35).
     */
    private static void magara(Minecraft mc, String ne) {
        if ("kur".equals(ne)) {
            ox = mc.player.getBlockX();
            oz = mc.player.getBlockZ();
            List<String> k = new ArrayList<>(List.of("fill %d 8 %d %d 19 %d stone".formatted(ox - 10, oz - 10, ox + 10, oz + 10),
                    "fill %d 10 %d %d 16 %d air".formatted(ox - 8, oz - 8, ox + 8, oz + 8),
                    "fill %d 17 %d %d 17 %d dripstone_block".formatted(ox - 8, oz - 8, ox + 8, oz + 8),
                    "fill %d 10 %d %d 16 %d mossy_cobblestone".formatted(ox - 9, oz - 9, ox + 9, oz - 9),
                    "fill %d 10 %d %d 16 %d mossy_cobblestone".formatted(ox - 9, oz - 9, ox - 9, oz + 9),
                    "fill %d 13 %d %d 13 %d sea_lantern".formatted(ox + 9, oz - 8, ox + 9, oz + 8),
                    "fill %d 10 %d %d 16 %d deepslate_bricks".formatted(ox - 3, oz + 2, ox - 3, oz + 2),
                    "fill %d 10 %d %d 16 %d mossy_cobblestone".formatted(ox + 3, oz - 1, ox + 3, oz - 1),
                    "fill %d 10 %d %d 10 %d gravel".formatted(ox - 8, oz - 8, ox + 8, oz - 4)));
            // Tavanda ışıltaşı yamaları, sarkıtlar ve asılı fenerler: su yüzeyinde sıcak yansıma noktaları (vitrin).
            int[][] isik = {{-6, -6}, {-1, -6}, {4, -6}, {-6, -1}, {-1, -1}, {4, -1}, {-6, 4}, {-1, 4}, {4, 4}};
            for (int[] q : isik) {
                k.add("fill %d 17 %d %d 17 %d glowstone".formatted(ox + q[0], oz + q[1], ox + q[0] + 2, oz + q[1] + 2));
            }
            for (int d = -7; d <= 7; d += 3) {
                k.add("setblock %d 13 %d sea_lantern".formatted(ox - 9, oz + d));
                k.add("setblock %d 13 %d sea_lantern".formatted(ox + d, oz - 9));
            }
            int[][] sarkit = {{-3, -6}, {2, -3}, {-6, -1}, {6, 1}, {-2, 5}, {3, 6}, {7, -5}, {-7, 6}};
            for (int[] q : sarkit) {
                k.add("setblock %d 16 %d pointed_dripstone[vertical_direction=down,thickness=frustum]".formatted(ox + q[0], oz + q[1]));
                k.add("setblock %d 15 %d pointed_dripstone[vertical_direction=down,thickness=tip]".formatted(ox + q[0], oz + q[1]));
            }
            int[][] fener = {{-1, -4}, {4, 2}, {-4, 2}};
            for (int[] q : fener) {
                k.add("setblock %d 16 %d lantern[hanging=true]".formatted(ox + q[0], oz + q[1]));
            }
            k.add("time set 6000");
            k.add("tp @s %d 14.0 %d -45 27".formatted(ox - 7, oz - 7));
            for (String komut : k) {
                mc.player.connection.sendCommand(komut);
            }
        } else if ("icine".equals(ne)) {
            // Su altından yukarı: yüzeyin altı ve sıcak demetler (izleyici: kamera sürüklenmez).
            mc.player.connection.sendCommand("tp @s %d.5 11 %d.5 -45 -22".formatted(ox - 5, oz - 5));
        } else if ("kamera".equals(ne)) {
            mc.player.connection.sendCommand("gamemode spectator");
            mc.player.connection.sendCommand("tp @s %d 14.0 %d -45 27".formatted(ox - 7, oz - 7));
        } else {
            mc.player.connection.sendCommand("dogalafet su magara %d 10 %d 900 600".formatted(ox, oz));
        }
    }
}
