package io.github.kadirberkpolat1.dogalafetler.client.core;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.mantik.TpsKismasi;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Görsel otomasyonların ortak ölçüm satırı ve A-34 kare kapısı. Yalnız {@code -Potomasyon=<ad>} ile (sistem özelliği
 * {@code dogalafetler.otomasyon}) etkin test kodudur; üretimde olay işleyicileri hiçbir şey yapmaz.
 *
 * <p><b>Ölçüm (K-E29 (b)).</b> {@link #olcumBasla} ile {@link #olcumBitir} arasında her çizilen karenin duvar saati süresi ve
 * render thread CPU süresi ({@link ThreadMXBean}; makinedeki başka süreçlerin yükünden duvar saatinden az etkilenir)
 * {@code RenderTickEvent} START'tan START'a toplanır. Bitişte tek satır yazılır:
 * {@code OLCUM <otomasyon> sahne=<ad> kademe=<Düşük|Orta|Yüksek|Ultra> duvar_ms=.. cpu_ms=.. mspt=.. gradle=.. yabanci=..
 * kare=.. taban=..}. {@code duvar_ms} ve {@code cpu_ms} ortancadır; {@code mspt} entegre sunucunun son 100 tick'inin ortalaması ({@code tickTimes}; pencere 100 tick, üstel
 * {@code getAverageTickTime} son ~10 tick'e bakardı);
 * {@code gradle} komut satırında {@code GradleWrapperMain} geçen süreçler, {@code yabanci} bunlardan bu istemciyi başlatan
 * sarmalayıcı (ata süreç) dışındakiler; {@code kare} penceredeki kare sayısı; {@code taban} aynı kameranın afetsiz taban
 * sahnesinin adı ya da {@code -}. {@code tools/senaryo/olcum_tablosu.py} satırları tek TSV'ye toplar ve kuralı uygular.
 *
 * <p><b>Kare kapısı (A-34).</b> {@link #kareIste} isteği kaydeder; kare {@code RenderTickEvent} END'de, istekten sonra en az
 * {@code enAzKare} (≥ 2; ardışık seri karesinde 1) yeni kare çizilmiş ve FPS ≥ {@value #AKICI_FPS} iken alınır.
 * {@value #KAPI_SN} sn'de sağlanmazsa yine alınır, WARN yazılır ve kare geçersiz sayılır. Ekran açıkken iç imleç beklerken
 * her karenin başında sol üst köşeye alınır ({@link #imleciKenaraAl}). Otomasyon bekleyen kare varken ({@link #kareBekliyor})
 * adım ilerletmez.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class GorselOtomasyon {
    /** A-34 kapısının FPS alt sınırı (şef). */
    public static final int AKICI_FPS = 15;
    /** A-34 kapısının en uzun beklemesi (sn); aşılırsa kare geçersizdir. */
    public static final int KAPI_SN = 60;
    /**
     * Herhangi bir görsel otomasyon ({@code -Potomasyon=<ad>}) açık mı. Ölçüm ve tanı kodu (tanı satırları, render thread CPU
     * okuması) bu kapının arkasındadır; üretimde çalışmaz.
     */
    public static final boolean AKTIF = !System.getProperty("dogalafetler.otomasyon", "").isEmpty();
    private static final ThreadMXBean IPLIK = ManagementFactory.getThreadMXBean();

    /** Çizilmiş kare sayacı ({@code RenderTickEvent} END). */
    private static long cizilen;

    private static String olcumOtomasyon;
    private static String olcumSahne;
    private static String olcumKademe;
    private static String olcumTaban;
    private static long[] duvar = new long[4096];
    private static long[] cpu = new long[4096];
    private static int kareSayisi;
    private static long sonKare;
    private static long sonCpu;

    private static String kareOtomasyon;
    private static String kareDosya;
    private static int kareEnAz;
    private static long kareIstekNs;
    private static long kareIstekCizilen;
    private static Consumer<Kare> kareSonra;
    private static int kareToplam;
    private static int kareGecersiz;
    private static int eylemBekleme;

    /** Bir ölçüm penceresinin sonucu ({@code OLCUM} satırının alanları). */
    public record Olcum(String otomasyon, String sahne, String kademe, double duvarMs, double cpuMs, double mspt,
            int gradle, int yabanci, int kare) {
        public double fps() {
            return 1000 / duvarMs;
        }
    }

    /** Alınan kare: kapı açıldığında FPS, istekten sonra çizilen kare, bekleme ve geçerlilik. */
    public record Kare(String dosya, int fps, long yeniKare, double beklemeSn, boolean gecerli) {
    }

    private GorselOtomasyon() {
    }

    /** İstemci şu an A-34 alt sınırında ya da üstünde çiziyor. */
    public static boolean akici() {
        return Minecraft.getInstance().getFps() >= AKICI_FPS;
    }

    /**
     * Eylemden önceki A-34 FPS kapısı (dar zamanlı kareler kapıyı eylemden önce geçer); bekleme sırasında her istemci tick'inde
     * bir kez çağrılır. Akıcıysa {@code true}; değilse {@value #KAPI_SN} sn dolunca WARN yazıp yine {@code true} döner (eylem
     * sürer, geçersizlik eylemin kendi karesinde kapıdan yazılır). Aynı anda tek eylem beklenir.
     */
    public static boolean eylemKapisi(String otomasyon, String eylem) {
        if (akici()) {
            eylemBekleme = 0;
            return true;
        }
        if (++eylemBekleme < KAPI_SN * 20) {
            return false;
        }
        DogalAfetler.LOGGER.warn("FPS-KAPISI {} {} öncesi {} sn'de açılmadı (FPS {}); yine de devam", belirtec(otomasyon), eylem,
                KAPI_SN, Minecraft.getInstance().getFps());
        eylemBekleme = 0;
        return true;
    }

    /** Başlangıçtan beri çizilen kare sayısı. */
    public static long cizilenKare() {
        return cizilen;
    }

    /**
     * Ölçüm koşusunun çizim ayarı, çalışma anında ({@code options.txt}'ye yazılmaz): vsync kapalı (gizli workspace'te vsync
     * takası istemciyi 1 FPS'e kısar, K-13) ve kare sınırı 260 (afetsiz taban tavana yapışmasın, AOi-3). Dokuz otomasyon
     * açılışta çağırır; böylece bütün {@code OLCUM} satırları aynı ayarla ölçülür.
     */
    public static void cizimAyari() {
        Minecraft mc = Minecraft.getInstance();
        mc.options.enableVsync().set(false);
        mc.options.framerateLimit().set(260);
        DogalAfetler.LOGGER.info("GORSEL-OTOMASYON çizim ayarı: vsync kapalı, maxFps 260");
    }

    // ---------------------------------------------------------------- ölçüm

    /** Efekt kademesi istemci ayarından ({@code gorsel.efektKalitesi}) okunur. */
    public static void olcumBasla(String otomasyon, String sahne, String taban) {
        olcumBasla(otomasyon, sahne, null, taban);
    }

    /**
     * Ölçüm penceresini açar. {@code kademe} {@code null} ise istemci ayarından okunur (otomasyon kademeyi kendi zorluyorsa
     * onu verir). {@code taban} aynı kameranın afetsiz taban sahnesi ya da {@code null}. Ad alanlarındaki boşluklar {@code _}
     * olur (satır boşlukla ayrılır).
     */
    public static void olcumBasla(String otomasyon, String sahne, EfektKalitesi kademe, String taban) {
        if (olcumSahne != null) {
            DogalAfetler.LOGGER.warn("OLCUM {} sahne={} bitirilmeden {} başladı; önceki pencere atıldı", olcumOtomasyon,
                    olcumSahne, sahne);
        }
        olcumOtomasyon = belirtec(otomasyon);
        olcumKademe = kademeAdi(kademe != null ? kademe : IstemciAyarlari.efektKalitesi());
        olcumTaban = taban == null ? "-" : belirtec(taban);
        kareSayisi = 0;
        sonKare = 0;
        olcumSahne = belirtec(sahne);
    }

    public static boolean olcuyor() {
        return olcumSahne != null;
    }

    /** Pencereyi kapatır, {@code OLCUM} satırını yazar ve sonucu döndürür; açık pencere yoksa {@code null}. */
    public static Olcum olcumBitir() {
        if (olcumSahne == null) {
            DogalAfetler.LOGGER.warn("OLCUM açık pencere yokken bitirildi");
            return null;
        }
        long[] d = Arrays.copyOf(duvar, kareSayisi);
        long[] c = Arrays.copyOf(cpu, kareSayisi);
        Arrays.sort(d);
        Arrays.sort(c);
        IntegratedServer sunucu = Minecraft.getInstance().getSingleplayerServer();
        int[] g = gradle();
        Olcum o = new Olcum(olcumOtomasyon, olcumSahne, olcumKademe, ortanca(d), ortanca(c),
                sunucu == null ? Double.NaN : TpsKismasi.ortalamaMs(sunucu.tickTimes), g[0], g[1], d.length);
        DogalAfetler.LOGGER.info("OLCUM {} sahne={} kademe={} duvar_ms={} cpu_ms={} mspt={} gradle={} yabanci={} kare={} taban={}",
                o.otomasyon(), o.sahne(), o.kademe(), sayi(o.duvarMs()), sayi(o.cpuMs()), sayi(o.mspt()), o.gradle(),
                o.yabanci(), o.kare(), olcumTaban);
        olcumSahne = null;
        return o;
    }

    private static double ortanca(long[] sirali) {
        return sirali.length == 0 ? Double.NaN : sirali[sirali.length / 2] / 1e6;
    }

    private static String sayi(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    private static String belirtec(String ad) {
        return ad.trim().replaceAll("\\s+", "_");
    }

    public static String kademeAdi(EfektKalitesi k) {
        return switch (k) {
            case DUSUK -> "Düşük";
            case ORTA -> "Orta";
            case YUKSEK -> "Yüksek";
            case ULTRA -> "Ultra";
        };
    }

    /**
     * Ölçüm geçerliliği (şef kararı, tsunami tur 4; K0-21 rev. 2 (i)): komut satırında {@code GradleWrapperMain} geçen süreçler
     * ve bunlardan bu istemcinin ata süreci olmayanlar.
     */
    private static int[] gradle() {
        Set<Long> ata = new HashSet<>();
        for (ProcessHandle p = ProcessHandle.current().parent().orElse(null); p != null; p = p.parent().orElse(null)) {
            ata.add(p.pid());
        }
        int[] n = new int[2];
        ProcessHandle.allProcesses().filter(h -> h.info().commandLine().orElse("").contains("GradleWrapperMain"))
                .forEach(h -> {
                    n[0]++;
                    if (!ata.contains(h.pid())) {
                        n[1]++;
                    }
                });
        return n;
    }

    // ---------------------------------------------------------------- kare kapısı

    /** {@code enAzKare} = 2 ile {@link #kareIste(String, String, int, Consumer)}. */
    public static void kareIste(String otomasyon, String dosya, Consumer<Kare> sonra) {
        kareIste(otomasyon, dosya, 2, sonra);
    }

    /**
     * Kare isteği: {@code dosya} ({@code screenshots/} altında) kapı açılınca alınır, ardından {@code sonra} (otomasyonun kendi
     * kare satırı) çağrılır. {@code enAzKare} 1'den küçükse 1 sayılır; 1 yalnız önceki karenin {@code sonra}'sından istenen
     * ardışık seri karesi içindir (sahne zaten çizili, bir sonraki render karesi alınır).
     */
    public static void kareIste(String otomasyon, String dosya, int enAzKare, Consumer<Kare> sonra) {
        if (kareDosya != null) {
            DogalAfetler.LOGGER.warn("KARE-KAPISI {} {} alınmadan {} istendi; önceki istek düştü", kareOtomasyon, kareDosya,
                    dosya);
        }
        kareOtomasyon = belirtec(otomasyon);
        kareEnAz = Math.max(1, enAzKare);
        kareIstekNs = System.nanoTime();
        kareIstekCizilen = cizilen;
        kareSonra = sonra;
        kareDosya = dosya;
        if (Minecraft.getInstance().screen != null) {
            imleciKenaraAl();
        }
    }

    public static boolean kareBekliyor() {
        return kareDosya != null;
    }

    /** Kapanış satırından önce yazılır: {@code KARE-KAPISI <otomasyon> ozet toplam=.. gecersiz=..}. */
    public static void kareOzetiYaz(String otomasyon) {
        DogalAfetler.LOGGER.info("KARE-KAPISI {} ozet toplam={} gecersiz={}", belirtec(otomasyon), kareToplam, kareGecersiz);
    }

    /**
     * İç imleci (yalnız {@code MouseHandler}'ın konum alanları; SRG {@code f_91507_}/{@code f_91508_}) sol üst köşeye alır:
     * vanilla ipucu kutusu widget üstünde kalmaz. İşletim sistemi imleci ve sahibin oturumu etkilenmez, girdi olayı üretmez.
     * {@code Minecraft.setScreen} fare tutuluyken konumu pencere ortasına geri yazar; ekran açıldıktan sonra çağrılmalıdır.
     */
    public static void imleciKenaraAl() {
        Minecraft mc = Minecraft.getInstance();
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, 0.0, "f_91507_");
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, 0.0, "f_91508_");
    }

    @SubscribeEvent
    public static void cizim(TickEvent.RenderTickEvent olay) {
        if (!AKTIF) {
            return;
        }
        if (olay.phase == TickEvent.Phase.START) {
            if (olcumSahne != null) {
                long simdi = System.nanoTime();
                long c = IPLIK.getCurrentThreadCpuTime();
                if (sonKare != 0) {
                    kareEkle(simdi - sonKare, c - sonCpu);
                }
                sonKare = simdi;
                sonCpu = c;
            }
            if (kareDosya != null && Minecraft.getInstance().screen != null) {
                imleciKenaraAl();
            }
            return;
        }
        cizilen++;
        if (kareDosya != null) {
            kapi();
        }
    }

    private static void kareEkle(long duvarNs, long cpuNs) {
        if (kareSayisi == duvar.length) {
            duvar = Arrays.copyOf(duvar, kareSayisi * 2);
            cpu = Arrays.copyOf(cpu, kareSayisi * 2);
        }
        duvar[kareSayisi] = duvarNs;
        cpu[kareSayisi++] = cpuNs;
    }

    private static void kapi() {
        Minecraft mc = Minecraft.getInstance();
        long yeni = cizilen - kareIstekCizilen;
        double sn = (System.nanoTime() - kareIstekNs) / 1e9;
        boolean gecerli = yeni >= kareEnAz && mc.getFps() >= AKICI_FPS;
        if (!gecerli && sn < KAPI_SN) {
            return;
        }
        String dosya = kareDosya;
        Consumer<Kare> sonra = kareSonra;
        kareDosya = null;
        kareSonra = null;
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> {
        });
        kareToplam++;
        Kare k = new Kare(dosya, mc.getFps(), yeni, sn, gecerli);
        String bekleme = String.format(Locale.ROOT, "%.1f", sn);
        if (gecerli) {
            DogalAfetler.LOGGER.info("KARE-KAPISI {} {} kapi=acik fps={} yeni_kare={} bekleme_sn={}", kareOtomasyon, dosya,
                    k.fps(), yeni, bekleme);
        } else {
            kareGecersiz++;
            DogalAfetler.LOGGER.warn("KARE-KAPISI {} {} kapi=GECERSIZ fps={} yeni_kare={} bekleme_sn={} (FPS >= {} ve >= {} yeni "
                    + "kare {} sn'de sağlanmadı)", kareOtomasyon, dosya, k.fps(), yeni, bekleme, AKICI_FPS, kareEnAz, KAPI_SN);
        }
        if (sonra != null) {
            sonra.accept(k);
        }
    }
}
