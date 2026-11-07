package io.github.kadirberkpolat1.dogalafetler.client.tsunami;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.tsunami.TsunamiAfeti;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.su.SuRenderer;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.particle.BubbleParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.SplashParticle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

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
    private static final int KAPANIS_SN = 30;
    private static final Deque<String> ADIMLAR = new ArrayDeque<>();
    private static final List<Long> KARELER = new ArrayList<>();
    /** Render thread'inin kare başına CPU süresi: makinedeki başka süreçlerin yükünden (kardeş işçiler) duvar saatinden az etkilenir. */
    private static final List<Long> KARE_CPU = new ArrayList<>();
    private static final java.lang.management.ThreadMXBean IPLIK = java.lang.management.ManagementFactory.getThreadMXBean();
    private static long sonCpu;
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
    /**
     * Kök neden ölçümü (TS-15): ölçüm penceresinde render thread CPU süresinin aşamalara dağılımı (ns, toplam). Render
     * aşamaları bir önceki {@link RenderLevelStageEvent}'ten bu yana geçen süredir (ilki {@code after_sky}: ışık, ayıklama,
     * bölüm yüklemesi ve gökyüzü); {@code istemci_tick} kareler arasındaki istemci tick'leri, {@code takas_diger} kalan
     * aralık (tampon takası, görev kuyruğu). Toplamları kare başına CPU süresine eşittir.
     */
    private static final Map<String, Long> ASAMA = new LinkedHashMap<>();
    private static long asamaSon;
    private static long kareSonu;
    private static long tickBas;
    private static long tickAcik;
    private static Field parcacikAlani;
    /**
     * K26-1: ölçüm penceresinde yeni BUBBLE/SPLASH parçacıkları ve sudaki varlık-tick'leri. Bir {@code doWaterSplashEffect}
     * çağrısı ⌈{@code 1 + 20·genişlik}⌉ BUBBLE ve aynı sayıda SPLASH üretir; SPLASH'ı sanal su dalga cephesi de ürettiği için
     * sıçrama sayısı BUBBLE'dan okunur: {@code 20 · yeniBubble / Σ⌈⌉} = varlık başına saniyede sıçrama.
     */
    private static final Set<Particle> SICRAMA_ONCEKI = Collections.newSetFromMap(new IdentityHashMap<>());
    private static long yeniSplash;
    private static long yeniBubble;
    private static long sudaTick;
    private static double sicramaBirim;
    /** Karar 3 (tur 5): istemci tick'inin örneklemeli ayrıştırması, ölçüm penceresinden sonraki kendi penceresinde. */
    private static final int ORNEK_MS = 2;
    private static final int PROFIL_ILK = 8;
    private static final String MOD_PAKETI = "io.github.kadirberkpolat1.dogalafetler.";
    private static String profil;
    private static int profilTick;
    private static long profilBas;
    private static Ornekleyici ornekleyici;
    /** Pencere başındaki yıkım sayacı (kırılan, değişen bölüm, kirletilen bölüm); tsunami yoksa {@code null}. */
    private static long[] yikimBas;

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
                "f:" + on + "-afetsiz", "s:" + on + "-once", "k:dogalafet start tsunami " + seviye, "b:60",
                "s:" + on + "-onuyari", "k:dogalafet asama ileri", "b:60", "s:" + on + "-siren", "k:dogalafet asama ileri",
                "b:160", "s:" + on + "-cekilme", "b:100", "s:" + on + "-ufuk", "f:" + on + "-yaklasma", "b:120",
                "s:" + on + "-vitrin", "b:140", "s:" + on + "-carpma", "b:100", "s:" + on + "-karada",
                "f:" + on + "-karada", "k:dogalafet stop", "b:100", "s:" + on + "-sonra", "k:dogalafet su temizle", "b:40");
    }

    /**
     * FPS kabulü (TS-12, K0-33 ölçüsü): aynı vitrin kamerasından, verilen efekt kalitesiyle önce afetsiz taban, sonra S5'in
     * karadaki penceresi (AKTİF'in 620. tick'i, normal koşudaki "karada" penceresiyle aynı), sonra {@code stop}'tan 200 tick
     * sonra afetsiz "sonra" tabanı: yıkılan köy kameranın görüş hattını açar ve vanilla daha çok bölüm çizer (ölçüm 7'de
     * görünür bölüm 456 → 1343); bu taban afetin çalışma maliyetini dünyanın değişen geometrisinden ayırır. {@code ayar}
     * adımları (blok hasarı kapalı, yağmur kapalı) kök neden ayrıştırması içindir; afetten önce uygulanır, sonra geri alınır.
     * Karada FPS penceresinden sonra 100 tick istemci tick'i örneklenir ({@code PROFIL} satırı, {@link Ornekleyici}).
     */
    private static List<String> olcum(String ad, EfektKalitesi kalite, List<String> ayar, List<String> geri) {
        List<String> a = new ArrayList<>(List.of("q:" + kalite.name(), "k:dogalafet su vitrin", "b:300", "k:time set 10500",
                "k:dogalafet su vitrin kamera tepe", "b:200", "f:" + ad + "-afetsiz"));
        a.addAll(ayar);
        a.addAll(List.of("k:dogalafet start tsunami 5", "b:20", "k:dogalafet asama ileri", "b:20",
                "k:dogalafet asama ileri", "b:620", "f:" + ad + "-karada", "s:" + ad + "-karada", "p:" + ad + "-karada",
                "k:dogalafet stop", "b:200", "f:" + ad + "-sonra", "k:dogalafet su temizle", "b:40"));
        a.addAll(geri);
        return a;
    }

    /**
     * #2 Dalga Avcısı uçtan uca (GOREV §6 #16): hayatta kalma modundaki oyuncu S2 tsunamisinde kıyıda, dalga tabanının en çok 2
     * blok üstündeki ilk zeminde dalganın yolunda durur; dalga onu sürükler (sunucu log'unda {@code SURUKLENDI}), afet
     * hızlandırılmış saatle doğal biter ve sunucuya başarımın verilip verilmediği sorulur ("Test passed"). İlk koşuda oyuncu
     * kıyıdan 6 blok içeride y 69'da (taban 62 + 7) dalga tepesinin üstünde kaldı, sürüklenmedi.
     */
    private static List<String> avci() {
        return List.of("k:dogalafet su vitrin", "b:300", "k:time set 6000", "k:dogalafet su vitrin kamera tepe", "b:100",
                "k:gamemode survival", "k:dogalafet start tsunami 2", "b:20", "k:dogalafet asama ileri", "b:20",
                "k:dogalafet asama ileri", "b:30", "a:yerles", "b:60", "s:tsunami-avci-once", "b:480",
                "s:tsunami-avci-dalga", "b:80", "a:durum", "k:dogalafet hiz 6", "b:400", "k:dogalafet hiz 1", "b:40",
                "a:durum", "k:execute if entity @s[advancements={dogalafetler:dogalafetler/dalga_avcisi=true}]", "b:20",
                "k:gamemode spectator", "k:dogalafet su temizle", "b:40");
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
        if (BOLUM.contains("olc")) {
            // Isınma: ilk ölçüm JIT ve ilk sahnenin chunk yüklemesiyle kirleniyordu (ölçüm 2'de Düşük tabanı Yüksek'ten düşük).
            a.addAll(List.of("k:dogalafet su vitrin", "b:300", "k:dogalafet su vitrin kamera tepe", "b:600"));
            for (EfektKalitesi k : new EfektKalitesi[] {EfektKalitesi.DUSUK, EfektKalitesi.YUKSEK, EfektKalitesi.ULTRA}) {
                a.addAll(olcum("tsunami-olc-" + k.name().toLowerCase(Locale.ROOT), k, List.of(), List.of()));
            }
            a.add("q:YUKSEK");
        }
        if (BOLUM.contains("ayrinti")) {
            a.addAll(olcum("tsunami-olc-yuksek-efektsiz", EfektKalitesi.YUKSEK, List.of("c:tsunami.yikimEfektTick=0"),
                    List.of("c:tsunami.yikimEfektTick=1")));
            a.addAll(olcum("tsunami-olc-yuksek-yarimbutce", EfektKalitesi.YUKSEK, List.of("c:tsunami.yikimCarpani=0.5"),
                    List.of("c:tsunami.yikimCarpani=1.0")));
            a.addAll(olcum("tsunami-olc-yuksek-yikimsiz", EfektKalitesi.YUKSEK, List.of("k:gamerule afetBlokHasari false"),
                    List.of("k:gamerule afetBlokHasari true")));
            a.add("q:YUKSEK");
        }
        if (BOLUM.contains("tekrar")) {
            // K0-33 ölçüsü Yüksek: her sahne yeni kumsal olduğu için tek ölçüm gürültülü; varsayılan ayarlarla üç kez.
            a.addAll(List.of("k:dogalafet su vitrin", "b:300", "k:dogalafet su vitrin kamera tepe", "b:600"));
            for (int i = 1; i <= 3; i++) {
                a.addAll(olcum("tsunami-olc-yuksek-" + i, EfektKalitesi.YUKSEK, List.of(), List.of()));
            }
        }
        if (BOLUM.contains("kabul")) {
            // Ts1 kabulü (TS-16 rev. 2): her S5 sahnesi Yüksek'te "sonra" tabanı < 10 ms ise karada ≥ 30 FPS, değilse "sonra"
            // tabanına oranla ≥ %50 kare süresi; "önce"/"sonra" oranları bilgi.
            a.addAll(List.of("k:dogalafet su vitrin", "b:300", "k:dogalafet su vitrin kamera tepe", "b:600"));
            for (int i = 1; i <= 4; i++) {
                a.addAll(olcum("tsunami-kabul-" + i, EfektKalitesi.YUKSEK, List.of(), List.of()));
            }
        }
        if (BOLUM.contains("deney")) {
            // Ts3-1 ve Karar 3 ayrıştırması (gorsel-15): sahne 1 bölüm birimi ve eşyasız (yıkım eşya düşürmez), sahne 2 ve 4
            // sütun birimi (varsayılan), sahne 3 bölüm birimi.
            a.addAll(List.of("k:dogalafet su vitrin", "b:300", "k:dogalafet su vitrin kamera tepe", "b:600"));
            a.addAll(olcum("tsunami-deney-1-esyasiz", EfektKalitesi.YUKSEK,
                    List.of("c:tsunami.yikimEsyaOrani=0", "c:tsunami.yikimSutunBirimi=false"),
                    List.of("c:tsunami.yikimEsyaOrani=0.3", "c:tsunami.yikimSutunBirimi=true")));
            a.addAll(olcum("tsunami-deney-2-sutun", EfektKalitesi.YUKSEK, List.of(), List.of()));
            a.addAll(olcum("tsunami-deney-3-bolum", EfektKalitesi.YUKSEK, List.of("c:tsunami.yikimSutunBirimi=false"),
                    List.of("c:tsunami.yikimSutunBirimi=true")));
            a.addAll(olcum("tsunami-deney-4-sutun", EfektKalitesi.YUKSEK, List.of(), List.of()));
        }
        if (hepsi || BOLUM.contains("avci")) {
            a.addAll(avci());
        }
        a.add("x:son");
        ADIMLAR.addAll(a);
        log("BASLADI bolum={} adim={}", BOLUM, ADIMLAR.size());
    }

    @SubscribeEvent
    public static void kare(TickEvent.RenderTickEvent e) {
        if (!ACIK || olcum == null) {
            return;
        }
        long cpu = IPLIK.getCurrentThreadCpuTime();
        if (e.phase == TickEvent.Phase.START) {
            long simdi = System.nanoTime();
            if (sonKare != 0) {
                KARELER.add(simdi - sonKare);
                KARE_CPU.add(cpu - sonCpu);
                ekle("istemci_tick", tickAcik);
                ekle("takas_diger", cpu - kareSonu - tickAcik);
            }
            tickAcik = 0;
            sonKare = simdi;
            sonCpu = cpu;
            asamaSon = cpu;
        } else if (sonKare != 0) {
            ekle("seviye_sonrasi", cpu - asamaSon);
            kareSonu = cpu;
        }
    }

    @SubscribeEvent
    public static void asama(RenderLevelStageEvent e) {
        if (ACIK && olcum != null && sonKare != 0) {
            long cpu = IPLIK.getCurrentThreadCpuTime();
            String ad = e.getStage().toString();
            ekle(ad.substring(ad.indexOf(':') + 1), cpu - asamaSon);
            asamaSon = cpu;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tickBasi(TickEvent.ClientTickEvent e) {
        if (ACIK && olcum != null && e.phase == TickEvent.Phase.START) {
            tickBas = IPLIK.getCurrentThreadCpuTime();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void tickSonu(TickEvent.ClientTickEvent e) {
        if (ACIK && olcum != null && e.phase == TickEvent.Phase.END && tickBas != 0) {
            tickAcik += IPLIK.getCurrentThreadCpuTime() - tickBas;
            tickBas = 0;
        }
    }

    private static void ekle(String ad, long ns) {
        ASAMA.merge(ad, ns, Long::sum);
    }

    private static String asamaOzeti(int kare) {
        return kare == 0 ? "" : ASAMA.entrySet().stream()
                .map(g -> g.getKey() + "=" + String.format(Locale.ROOT, "%.2f", g.getValue() / 1e6 / kare))
                .collect(Collectors.joining(" "));
    }

    private static String enCok(Map<String, Integer> say) {
        return say.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5)
                .map(g -> g.getKey() + "=" + g.getValue()).collect(Collectors.joining(" "));
    }

    /**
     * Render thread'inin yığınını {@value #ORNEK_MS} ms'de bir okur; yalnız {@code Minecraft.tick} içindeki örnekler
     * sınıflandırılır: {@code cagri} {@code Minecraft.tick}'in doğrudan çağırdığı çerçeve, {@code varlik} {@code tickNonPassenger}
     * / {@code tickPassenger} altındaki ilk üç çerçeve (varlık sınıfının tick yolu), {@code mod} tick içindeki en dış ve en iç
     * mod çerçevesi (mixin işleyicileri dahil). Her örnek okuması bir safepoint ister; FPS penceresinde çalışmaz.
     */
    private static final class Ornekleyici extends Thread {
        private final Thread hedef;
        private volatile boolean dur;
        private int ornek;
        private int tickte;
        private final Map<String, Integer> cagri = new HashMap<>();
        private final Map<String, Integer> varlik = new HashMap<>();
        private final Map<String, Integer> mod = new HashMap<>();

        Ornekleyici(Thread hedef) {
            super("tsunami-otomasyon-ornek");
            this.hedef = hedef;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (!dur) {
                try {
                    Thread.sleep(ORNEK_MS);
                } catch (InterruptedException e) {
                    return;
                }
                StackTraceElement[] y = hedef.getStackTrace();
                synchronized (this) {
                    ornek++;
                    siniflandir(y);
                }
            }
        }

        private void siniflandir(StackTraceElement[] y) {
            int mc = -1;
            for (int i = 0; i < y.length && mc < 0; i++) {
                if ("tick".equals(y[i].getMethodName()) && "net.minecraft.client.Minecraft".equals(y[i].getClassName())) {
                    mc = i;
                }
            }
            if (mc < 0) {
                return;
            }
            tickte++;
            cagri.merge(mc == 0 ? "Minecraft.tick" : kisa(y[mc - 1]), 1, Integer::sum);
            String dis = null;
            String ic = null;
            for (int i = mc - 1; i >= 0; i--) {
                String m = y[i].getMethodName();
                if (("tickNonPassenger".equals(m) || "tickPassenger".equals(m)) && y[i].getClassName().endsWith("ClientLevel")) {
                    StringBuilder b = new StringBuilder();
                    for (int j = i - 1; j >= Math.max(0, i - 3); j--) {
                        b.append(b.length() == 0 ? "" : ">").append(kisa(y[j]));
                    }
                    varlik.merge(b.length() == 0 ? kisa(y[i]) : b.toString(), 1, Integer::sum);
                    break;
                }
            }
            for (int i = mc - 1; i >= 0; i--) {
                if (y[i].getClassName().startsWith(MOD_PAKETI) || y[i].getMethodName().contains("dogalafetler")) {
                    if (dis == null) {
                        dis = kisa(y[i]);
                    }
                    ic = kisa(y[i]);
                }
            }
            mod.merge(dis == null ? "yok" : dis.equals(ic) ? dis : dis + ">" + ic, 1, Integer::sum);
        }

        private static String kisa(StackTraceElement e) {
            String s = e.getClassName();
            return s.substring(s.lastIndexOf('.') + 1) + "." + e.getMethodName();
        }

        /** Anahtar başına tick başına ms: örnek payı × pencere duvar süresi / tick. */
        synchronized String ozet(int tick, double pencereMs) {
            double birim = ornek == 0 ? 0 : pencereMs / ornek / tick;
            return String.format(Locale.ROOT, "ornek=%d tickte=%d pencere_ms=%.0f tick_ms=%.2f", ornek, tickte, pencereMs,
                    tickte * birim) + " cagri=[" + ilk(cagri, birim) + "] varlik=[" + ilk(varlik, birim) + "] mod=["
                    + ilk(mod, birim) + "]";
        }

        private static String ilk(Map<String, Integer> say, double birim) {
            return say.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(PROFIL_ILK)
                    .map(g -> g.getKey() + "=" + String.format(Locale.ROOT, "%.2f", g.getValue() * birim))
                    .collect(Collectors.joining(" "));
        }
    }

    @SuppressWarnings("unchecked")
    private static Collection<Queue<Particle>> kuyruklar(ParticleEngine motor) throws ReflectiveOperationException {
        if (parcacikAlani == null) {
            for (Field f : ParticleEngine.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType()) && f.getGenericType().getTypeName().contains("Queue")) {
                    f.setAccessible(true);
                    parcacikAlani = f;
                    break;
                }
            }
        }
        return ((Map<?, Queue<Particle>>) parcacikAlani.get(motor)).values();
    }

    /** Parçacık sınıfına göre sayım: yıkım (TerrainParticle), sanal sudaki varlıklar (Bubble/Splash), su paketi (Cloud/Drip). */
    private static String parcacikTurleri(ParticleEngine motor) {
        try {
            Map<String, Integer> say = new HashMap<>();
            for (Queue<Particle> q : kuyruklar(motor)) {
                for (Particle p : q) {
                    String ad = p.getClass().getName();
                    say.merge(ad.substring(ad.lastIndexOf('.') + 1), 1, Integer::sum);
                }
            }
            return enCok(say);
        } catch (ReflectiveOperationException | RuntimeException hata) {
            return "? " + hata;
        }
    }

    private static void sicramaTick(Minecraft mc) {
        Set<Particle> simdi = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            for (Queue<Particle> q : kuyruklar(mc.particleEngine)) {
                for (Particle p : q) {
                    if (p instanceof BubbleParticle || p instanceof SplashParticle) {
                        simdi.add(p);
                        if (olcumTick > 0 && !SICRAMA_ONCEKI.contains(p)) {
                            if (p instanceof BubbleParticle) {
                                yeniBubble++;
                            } else {
                                yeniSplash++;
                            }
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException hata) {
            return;
        }
        SICRAMA_ONCEKI.clear();
        SICRAMA_ONCEKI.addAll(simdi);
        if (olcumTick == 0) {
            return;
        }
        for (Entity v : mc.level.entitiesForRendering()) {
            if (v.isInWater()) {
                sudaTick++;
                sicramaBirim += Math.ceil(1 + 20 * v.getBbWidth());
            }
        }
    }

    private static String sicramaOzeti(int tick) {
        return String.format(Locale.ROOT, "bubble_yeni=%d splash_yeni=%d suda_ort=%.1f bubble_varlik_tick=%.2f "
                + "splash_varlik_tick=%.2f sicrama_varlik_sn=%.2f", yeniBubble, yeniSplash, (double) sudaTick / (tick - 1),
                sudaTick == 0 ? 0.0 : (double) yeniBubble / sudaTick, sudaTick == 0 ? 0.0 : (double) yeniSplash / sudaTick,
                sicramaBirim == 0 ? 0.0 : 20 * yeniBubble / sicramaBirim);
    }

    /**
     * Entegre sunucudaki tsunaminin yıkım sayacı (kırılan, değişen bölüm, kirletilen bölüm); yoksa {@code null}. Render
     * thread'inden okunur: yalnız ölçüm, sunucu tick'iyle yarışır (tek tick'lik kayma).
     */
    private static long[] yikimSayaci(Minecraft mc) {
        var sunucu = mc.getSingleplayerServer();
        return sunucu == null ? null : AfetYoneticisi.al(sunucu).aktifAfet().filter(TsunamiAfeti.class::isInstance)
                .map(a -> ((TsunamiAfeti) a).yikimSayaci()).orElse(null);
    }

    /** Pencere boyunca kırılan blok, değişen ve kirletilen bölüm (TS-15b birim karşılaştırması). */
    private static String yikimFarki(Minecraft mc) {
        long[] son = yikimSayaci(mc);
        if (son == null || yikimBas == null) {
            return "yok";
        }
        return "kirilan=" + (son[0] - yikimBas[0]) + " bolum=" + (son[1] - yikimBas[1]) + " kirli=" + (son[2] - yikimBas[2]);
    }

    private static String varlikTurleri(Minecraft mc) {
        Map<String, Integer> say = new HashMap<>();
        int suda = 0;
        for (Entity v : mc.level.entitiesForRendering()) {
            say.merge(EntityType.getKey(v.getType()).getPath(), 1, Integer::sum);
            if (v.isInWater()) {
                suda++;
            }
        }
        return "suda=" + suda + " " + enCok(say);
    }

    /**
     * Ölçüm geçerliliği (şef kararı, tur 4): komut satırında {@code GradleWrapperMain} geçen süreçler; {@code yabanci} bu
     * istemciyi başlatan kendi {@code runClient} sarmalayıcısı (ata süreç) dışındakiler.
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

    private static String yuk() {
        try {
            return java.nio.file.Files.readString(java.nio.file.Path.of("/proc/loadavg")).trim();
        } catch (java.io.IOException | RuntimeException hata) {
            return "?";
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
            sicramaTick(mc);
            if (++olcumTick >= 100) {
                List<Long> s = new ArrayList<>(KARELER);
                Collections.sort(s);
                List<Long> c = new ArrayList<>(KARE_CPU);
                Collections.sort(c);
                double ortanca = s.isEmpty() ? Double.NaN : s.get(s.size() / 2) / 1e6;
                double enUzun = s.isEmpty() ? Double.NaN : s.get(s.size() - 1) / 1e6;
                double cpuOrtanca = c.isEmpty() ? Double.NaN : c.get(c.size() / 2) / 1e6;
                var sunucu = mc.getSingleplayerServer();
                float mspt = sunucu == null ? Float.NaN : sunucu.getAverageTickTime();
                log("FPS {} ortanca_kare_ms={} fps_ortanca={} cpu_kare_ms={} en_uzun_kare_ms={} kare={} yuk=[{}] sunucu_mspt={} "
                        + "tps={} gradle=[{}] su={} yagmur={} bolum=[{}] parcacik={} varlik={} kopuk={} asama_cpu_ms=[{}] "
                        + "parcacik_tur=[{}] varlik_tur=[{}] sicrama=[{}] yikim=[{}]", olcum,
                        String.format(Locale.ROOT, "%.2f", ortanca), String.format(Locale.ROOT, "%.1f", 1000 / ortanca),
                        String.format(Locale.ROOT, "%.2f", cpuOrtanca), String.format(Locale.ROOT, "%.1f", enUzun), s.size(),
                        yuk(), String.format(Locale.ROOT, "%.2f", mspt),
                        String.format(Locale.ROOT, "%.1f", Math.min(20, 1000 / mspt)), gradle(),
                        SuRenderer.olcumOzeti(), TsunamiYagmuru.ozet(), mc.levelRenderer.getChunkStatistics(),
                        mc.particleEngine.countParticles(), mc.level.getEntityCount(), TsunamiKopugu.sonSegment(),
                        asamaOzeti(c.size()), parcacikTurleri(mc.particleEngine), varlikTurleri(mc), sicramaOzeti(olcumTick),
                        yikimFarki(mc));
                olcum = null;
            }
            return;
        }
        if (profil != null) {
            if (++profilTick >= 100) {
                ornekleyici.dur = true;
                ornekleyici.interrupt();
                log("PROFIL {} {} varlik_tur=[{}]", profil, ornekleyici.ozet(profilTick, (System.nanoTime() - profilBas) / 1e6),
                        varlikTurleri(mc));
                profil = null;
                ornekleyici = null;
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
                ASAMA.clear();
                tickBas = 0;
                tickAcik = 0;
                SICRAMA_ONCEKI.clear();
                yeniSplash = 0;
                yeniBubble = 0;
                sudaTick = 0;
                sicramaBirim = 0;
                yikimBas = yikimSayaci(mc);
                SuRenderer.olcumSifirla();
            }
            case 's' -> {
                log("KARE {} {} kopuk={}", deger, TsunamiYagmuru.ozet(), TsunamiKopugu.sonSegment());
                Screenshot.grab(mc.gameDirectory, deger + ".png", mc.getMainRenderTarget(),
                        m -> log("FOTO {} {}", deger, m.getString()));
            }
            case 'p' -> {
                profil = deger;
                profilTick = 0;
                profilBas = System.nanoTime();
                ornekleyici = new Ornekleyici(Thread.currentThread());
                ornekleyici.start();
            }
            case 'm' -> magara(mc, deger);
            case 'y' -> TsunamiYagmuru.kapali = "0".equals(deger);
            case 'q' -> kalite(EfektKalitesi.valueOf(deger));
            case 'c' -> sunucuAyari(deger);
            case 'a' -> avciAdimi(mc, deger);
            case 'x' -> {
                log("BITTI girdi tus={} fare={} teker={}", tus, fare, teker);
                kapanisBekcisi();
                mc.stop();
            }
            default -> log("bilinmeyen adım {}", adim);
        }
    }

    /**
     * İstemci dünyayı kaydedip penceresini kapattıktan sonra JVM'den çıkmayabiliyor ({@code System.exit} kancalarında asılı
     * kalır; runClient dönmez, Gradle daemon'ı ve oyun yetim kalır). Ölçümler BITTI'den önce tamamdır; kapanış
     * {@value #KAPANIS_SN} sn'de bitmezse JVM kancasız durdurulur. Satır stderr'e yazılır: log4j o anda kapanmış olabilir.
     */
    private static void kapanisBekcisi() {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(KAPANIS_SN * 1000L);
            } catch (InterruptedException e) {
                return;
            }
            System.err.println("TSUNAMI-OTOMASYON KAPANIS ASILDI " + KAPANIS_SN + " sn; Runtime.halt(0)");
            System.err.flush();
            Runtime.getRuntime().halt(0);
        }, "tsunami-otomasyon-kapanis");
        t.setDaemon(true);
        t.start();
    }

    /** Efekt kalitesini (CLIENT {@code gorsel.efektKalitesi}) çalışma anında değiştirir; dosyaya yazılmaz. */
    @SuppressWarnings("unchecked")
    private static void kalite(EfektKalitesi k) {
        for (ModConfig c : ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.CLIENT)) {
            if (DogalAfetler.MOD_ID.equals(c.getModId()) && c.getSpec() instanceof ForgeConfigSpec spec
                    && spec.getValues().get("gorsel.efektKalitesi") instanceof ForgeConfigSpec.EnumValue<?> deger) {
                ((ForgeConfigSpec.EnumValue<EfektKalitesi>) deger).set(k);
            }
        }
        log("KALITE istenen={} etkin={}", k, IstemciAyarlari.efektKalitesi());
    }

    /**
     * Kök neden ayrıştırması: entegre sunucunun SERVER config değerini ({@code <yol>=<değer>}, ör.
     * {@code tsunami.yikimEfektTick=0}) çalışma anında değiştirir; dosyaya yazılmaz. Afet değeri her tick okur.
     */
    @SuppressWarnings("unchecked")
    private static void sunucuAyari(String atama) {
        String yol = atama.substring(0, atama.indexOf('='));
        String deger = atama.substring(atama.indexOf('=') + 1);
        for (ModConfig c : ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.SERVER)) {
            if (DogalAfetler.MOD_ID.equals(c.getModId()) && c.getSpec() instanceof ForgeConfigSpec spec
                    && spec.getValues().get(yol) instanceof ForgeConfigSpec.ConfigValue<?> v) {
                if (v.get() instanceof Integer) {
                    ((ForgeConfigSpec.ConfigValue<Integer>) v).set(Integer.parseInt(deger));
                } else if (v.get() instanceof Double) {
                    ((ForgeConfigSpec.ConfigValue<Double>) v).set(Double.parseDouble(deger));
                } else if (v.get() instanceof Boolean) {
                    ((ForgeConfigSpec.ConfigValue<Boolean>) v).set(Boolean.parseBoolean(deger));
                }
                log("AYAR {}={} etkin={}", yol, deger, v.get());
                return;
            }
        }
        log("AYAR {} bulunamadı", yol);
    }

    /**
     * Dalga Avcısı adımları: {@code yerles} oyuncuyu tsunaminin kıyı noktasından dalga yönünde 6 blok içeri, zemine, denize
     * bakar koyar; {@code durum} oyuncunun canını, oyun modunu ve afet aşamasını yazar.
     */
    private static void avciAdimi(Minecraft mc, String ne) {
        AfetDurumPaketi p = IstemciAfetDurumu.aktif().filter(d -> d.tur() == AfetTuru.TSUNAMI).orElse(null);
        if ("durum".equals(ne)) {
            log("AVCI durum can={} mod={} konum=({} {} {}) afet={}", String.format(Locale.ROOT, "%.1f", mc.player.getHealth()),
                    mc.gameMode == null ? "?" : mc.gameMode.getPlayerMode(), String.format(Locale.ROOT, "%.1f", mc.player.getX()),
                    String.format(Locale.ROOT, "%.1f", mc.player.getY()), String.format(Locale.ROOT, "%.1f", mc.player.getZ()),
                    p == null ? "yok" : p.asama() + "/" + p.altAsama());
            return;
        }
        CompoundTag o = p == null ? null : p.ozel();
        if (o == null || !o.contains("kx")) {
            log("AVCI tsunami verisi yok; oyuncu yerleştirilmedi");
            return;
        }
        float yx = o.getFloat("yx");
        float yz = o.getFloat("yz");
        double x = o.getFloat("kx") + yx;
        double z = o.getFloat("kz") + yz;
        int y = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(x), Mth.floor(z));
        for (int i = 1; i <= 16; i++) {
            double xi = o.getFloat("kx") + yx * i;
            double zi = o.getFloat("kz") + yz * i;
            int yi = mc.level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(xi), Mth.floor(zi));
            if (yi <= o.getFloat("tb") + 3 && mc.level.getFluidState(new net.minecraft.core.BlockPos(Mth.floor(xi), yi - 1,
                    Mth.floor(zi))).isEmpty()) {
                x = xi;
                z = zi;
                y = yi;
                break;
            }
        }
        float yaw = (float) Math.toDegrees(Math.atan2(yx, -yz));
        String komut = String.format(Locale.ROOT, "tp @s %.2f %d %.2f %.1f 5", x, y, z, yaw);
        mc.player.connection.sendCommand(komut);
        log("AVCI yerlesti {} (kıyı {} {}, yön {} {})", komut, o.getFloat("kx"), o.getFloat("kz"), yx, yz);
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
