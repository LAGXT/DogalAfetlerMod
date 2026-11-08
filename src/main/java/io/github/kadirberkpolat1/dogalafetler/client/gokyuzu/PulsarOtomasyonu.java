package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.KavurmaKuyrugu;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.PulsarKayitlari;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarZinciri;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;

/**
 * Pulsar-2 görsel kanıt otomasyonu (K0-21 rev. 2; tur 4 kareleri {@code gorsel-4/}). Yalnız
 * {@code -Ddogalafetler.otomasyon=pulsar} ile tek oyunculu dünyada çalışır; üretimde etkisizdir. Zincir komutlarla
 * yürütülür: pulsar doğuşu ve tam güç (büyük pulsar, iki ardışık titreşim karesi, 0,5 sn içinde
 * {@value #SERI_KARE} karelik titreşim serisi ve çekirdek merkezinin ekran pikseli) → blok hasarı açık kavurma birikimi
 * ({@code hiz 6}, kavurmanın eşdeğer günü ≥ {@value #CEHENNEM_GUN}) ve cehennem karesi → ses derinliği ölçümü
 * (5×5 hava kuyusunda y 64 / 48 / 16 / −20 ve üstü kapalı tünel, {@code PULSAR_SES}) → pulsar gecesi → 3B paralaks
 * karesi (kamera pulsar yönüne dik ~{@value #YAN_ACI}°'lik kayma) → kara delik yutmasının beş anı ({@code hiz 0.25};
 * her kare {@link IstemciGokyuzu#sabitSaniye} ile tam saniyede, G2-10; mercek karesi FOV {@value #MERCEK_FOV}) → güneşsiz
 * buzul (donma %100) → kızıl cüce doğuşu (ilerleme ≥ {@value #DOGUS_ILERLEME}) → eski güneş ({@code NORMAL},
 * {@value #DONUS_SN} sn geçişten sonra). Kamera en yakın ormanın {@value #UZAKLIK} blok batısında uçan yaratıcı
 * oyuncudur; gök kareleri her istemci tick'inde nesnenin bu karedeki görünen yönüne çevrilir
 * ({@link IstemciGokyuzu#pulsarGorunurYonu}, {@link IstemciGokyuzu#karaDelikGorunurYonu}), zemin kareleri
 * {@value #ZEMIN_YUKSEKLIGI} blok üstten ormana bakar. Kare {@link GorselOtomasyon} A-34 kapısından alınır; satır FPS,
 * TPS, mspt, evre, evre saniyesi, kül yağışı, uçan meteor, 3B dörtgen sayısı, eşdeğer gün, titreşim ofseti ve
 * çekirdek pikselini yazar. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class PulsarOtomasyonu {
    private static final String OTOMASYON = "pulsar";
    private static final boolean AKTIF = OTOMASYON.equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "PULSAR_OTOMASYON ";
    private static final String SES = "PULSAR_SES ";
    private static final String TITRESIM = "PULSAR_TITRESIM ";
    private static final String DOSYA = "pulsar2_";
    private static final int UZAKLIK = 40;
    private static final int KAMERA_YUKSEKLIGI = 16;
    private static final int ZEMIN_YUKSEKLIGI = 6;
    /** Minecraft pitch'inde aşağı pozitiftir: ufuk kadrajın üst üçte birinde, zemin ağırlıkta. */
    private static final float ZEMIN_PITCH = 15f;
    private static final float GECE_PITCH = -5f;
    /** Doğuş kadrajının merkezi ufkun 10° üstü: ufuk alt üçte birde, doğuş yayı (−18°'ye) ve alçaktaki küre ortada. */
    private static final float DOGUS_PITCH = -10f;
    private static final int SERI_KARE = 6;
    /** Kavurma birikimi bu eşdeğer güne varınca cehennem karesi (Ps5). */
    private static final double CEHENNEM_GUN = 3.0;
    /**
     * Kavurma tick başına merkez başına 64 denemeyle sınırlı (hız 1'de ~10,7 gerekir): {@code hiz 6}'nın üstü eşdeğer günü
     * hızlandırmaz; 3 gün ~12 000 tick, TPS düşerse uzar.
     */
    private static final int KAVURMA_EN_COK = 15000;
    private static final float YAKLASMA_SN = 12f;
    /** Mercek karesi: kara delik tam boyda (20°), merceklenmiş iki yay ve foton halkası dar görüş açısıyla okunur. */
    private static final float MERCEK_SN = 35f;
    private static final int MERCEK_FOV = 30;
    private static final float SPAGETTI_SN = 40f;
    /** Parlama karesi örtünün inişinde (parlamanın 0,6. sn'si): kara deliğin koyu silüeti okunur. */
    private static final float PARLAMA_SN = PulsarZinciri.YUTMA_PARLAMA_SN + 0.6f;
    /** Yutumdan sonraki karanlık: kara delik ~8° (yavaş uzaklaşma), mercek yayları, ufukta kor, ilk kırağı. */
    private static final float KARANLIK_SN = 72f;
    /**
     * Karanlık karesinin gün saati: kara delik ~25° yükseklikte, ufuk kadrajın altında (doğal yutma ~6840'ta, PL-5
     * penceresinin içinde başlamış gibi).
     */
    private static final long KARANLIK_SAATI = 8280L;
    /** Paralaks karesinde kameranın pulsar yönüne dik kaymasının görünen açısı (derece). */
    private static final float YAN_ACI = 30f;
    /** Paralaks karesi çapa ({@link GokCapasi#PULSAR}, tau 60 sn) kameraya yetişmeden alınır (tick). */
    private static final int YAN_BEKLEME = 60;
    /** Buzul karesinden önce donmanın göle ve kara yetişme süresi (tick). */
    private static final int BUZUL_TICK = 2400;
    /** Donacak gölün zemin kamerasından uzaklığı, yarı genişliği ve üstünde temizlenen hava (blok). */
    private static final int GOL_UZAKLIGI = 16;
    private static final int GOL_YARI = 5;
    private static final int GOL_TEMIZLIK = 12;
    /**
     * Ses kuyusu: zemin kamerasının arkasında ve kadraj dışında (kuzeyde) 5×5, yüzeyden {@value #KUYU_TABAN}'ye; taban katı.
     * Tünel kuyu kenarından doğuya {@value #TUNEL_BOY} blok, ucunun 5×5 sütunu en az {@value #TUNEL_ORTU} blok örtülü.
     */
    private static final int KUYU_DX = -4;
    private static final int KUYU_DZ = -10;
    private static final int KUYU_YARI = 2;
    private static final int KUYU_TABAN = -22;
    private static final int TUNEL_BOY = 8;
    private static final int TUNEL_ORTU = 6;
    private static final int[] SES_Y = {64, 48, 16, -20};
    /** Taşınmadan sonra ses sönümünün ve ışık güncellemesinin istemciye yetişmesi (tick). */
    private static final int SES_BEKLEME = 25;
    private static final float DOGUS_ILERLEME = 0.3f;
    private static final float DONUS_SN = 20f;

    /** Sabit kamera kaydı (konum, yaw, pitch). */
    private record Kamera(Vec3 konum, float yaw, float pitch) {
    }

    /** İstemci tick'inde kameranın çevrildiği gök nesnesi ({@code YOK}: sunucunun taşıdığı sabit kamera). */
    private enum Kadraj { YOK, PULSAR, YUTMA_ORTA, KARADELIK }

    /**
     * Adım: başta istemci thread'inde {@code kadraj} ve {@code istemci}, sonra sunucu thread'inde {@code sunucu};
     * {@code hazir} olunca (ya da {@code enCok} tick'te) {@code kareler} ardışık alınır ve {@code sonunda} istemcide çalışır.
     * {@code sabitSn} NaN değilse kareler o evre saniyesinde çizilir.
     */
    private record Adim(String ad, Kadraj kadraj, Runnable istemci, Consumer<IntegratedServer> sunucu, BooleanSupplier hazir,
            List<String> kareler, int enCok, float sabitSn, Runnable sonunda) {
        static Adim yeni(String ad, Consumer<IntegratedServer> sunucu, BooleanSupplier hazir, String kare, int enCok) {
            return new Adim(ad, Kadraj.YOK, () -> {
            }, sunucu, hazir, kare == null ? List.of() : List.of(kare), enCok, Float.NaN, () -> {
            });
        }

        Adim kadrajli(Kadraj k) {
            return new Adim(ad, k, istemci, sunucu, hazir, kareler, enCok, sabitSn, sonunda);
        }

        Adim istemcide(Runnable r) {
            return new Adim(ad, kadraj, r, sunucu, hazir, kareler, enCok, sabitSn, sonunda);
        }

        Adim sabitSaniyede(float sn) {
            return new Adim(ad, kadraj, istemci, sunucu, hazir, kareler, enCok, sn, sonunda);
        }

        Adim seriKare(List<String> k) {
            return new Adim(ad, kadraj, istemci, sunucu, hazir, k, enCok, sabitSn, sonunda);
        }

        Adim bitince(Runnable r) {
            return new Adim(ad, kadraj, istemci, sunucu, hazir, kareler, enCok, sabitSn, r);
        }
    }

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static boolean basladi;
    private static int hazirTick = -1;
    private static int kareSirasi;
    private static long ilkKareNs;
    /** Seri karelerin {zaman ms, ofset x, ofset y, piksel x, piksel y}; yoksa NaN. */
    private static final List<float[]> SERI = new ArrayList<>();
    private static float enDusukTps = 20f;
    private static int eskiFov = 70;
    private static volatile boolean sunucuHazir;
    private static volatile Vec3 kameraKonumu;
    private static volatile Kamera zeminKamerasi;
    private static volatile BlockPos ormanMerkezi;
    private static volatile int ucanMeteor;
    private static volatile double esdegerGun;
    private static volatile Kadraj kadraj = Kadraj.YOK;
    /** Adım başına bir kez kadraj satırı (null: bu adımda henüz yazılmadı). */
    private static Kadraj yazilanKadraj;
    /** Paralaks karesi: kaymadan önceki pulsar yönü ve kayma (blok). */
    private static volatile Vector3f yanOncekiYon;
    private static volatile float yanKayma;
    private static volatile BlockPos kuyuMerkezi;
    private static volatile int kuyuYuzeyi;
    private static volatile BlockPos tunelUcu;

    private static final List<Adim> ADIMLAR = List.of(
            Adim.yeni("pulsar doğuşu ve tam güç (gündüz)", s -> {
                komut(s, "dogalafet pulsar baslat");
                DunyaDonemi d = AfetYoneticisi.al(s).donem();
                if (d.gunesEvresi() == GunesEvresi.PULSAR && d.evreIlerlemesi() < 1f) {
                    komut(s, "dogalafet asama ileri pulsar");
                }
                komut(s, "time set 3000");
                kamera(s, -90f, -35f);
            }, () -> sayac >= 80 && evre(GunesEvresi.PULSAR) && donem().evreIlerlemesi() >= 1f, "pulsar_buyuk", 400)
                    .kadrajli(Kadraj.PULSAR),
            Adim.yeni("pulsar titreşimi (iki ardışık kare)", s -> {
            }, () -> sayac >= 20, null, 100).kadrajli(Kadraj.PULSAR)
                    .seriKare(List.of("pulsar_titresim_a", "pulsar_titresim_b")),
            Adim.yeni("titreşim serisi (" + SERI_KARE + " ardışık kare)", s -> {
            }, () -> sayac >= 20, null, 100).kadrajli(Kadraj.PULSAR).seriKare(seri("titresim_seri_", SERI_KARE)),
            Adim.yeni("kavurma birikimi (hız 6, eşdeğer " + CEHENNEM_GUN + " gün)", s -> {
                komut(s, "time set 6000", "dogalafet hiz 6");
                kamera(s, zeminKamerasi);
            }, () -> esdegerGun >= CEHENNEM_GUN, null, KAVURMA_EN_COK).bitince(() -> DogalAfetler.LOGGER.info(ON
                    + "kavurma birikimi: eşdeğer gün {} ({} tick)", String.format(Locale.ROOT, "%.2f", esdegerGun), sayac)),
            Adim.yeni("cehennem", s -> {
                komut(s, "dogalafet hiz 1");
                kamera(s, zeminKamerasi);
                cehennemSay(s);
                meteorDusur(s);
            }, () -> sayac >= 60 && ucanMeteor >= 1, "pulsar_cehennem", 300),
            Adim.yeni("ses derinliği: kuyu ve y " + SES_Y[0], s -> {
                kuyuKaz(s);
                sesKamerasi(s, kuyuYuksekligi(0));
            }, () -> sayac >= 2 * SES_BEKLEME, null, 2 * SES_BEKLEME).bitince(() -> sesSatiri("", kuyuYuksekligi(0))),
            sesAdimi(1),
            sesAdimi(2),
            sesAdimi(3),
            Adim.yeni("ses derinliği: üstü kapalı tünel", s -> {
                BlockPos t = tunelUcu;
                kamera(s, new Kamera(new Vec3(t.getX() + 0.5, t.getY(), t.getZ() + 0.5), -90f, 0f));
            }, () -> sayac >= SES_BEKLEME, null, SES_BEKLEME).bitince(() -> sesSatiri("kapali ", tunelUcu.getY())),
            Adim.yeni("pulsar gecesi", s -> {
                komut(s, "time set 18000");
                kamera(s, new Kamera(zeminKamerasi.konum(), zeminKamerasi.yaw(), GECE_PITCH));
            }, () -> sayac >= 60, "pulsar_gece", 200),
            Adim.yeni("3B paralaks hazırlığı (gündüz, gök kamerası)", s -> {
                komut(s, "time set 3000");
                kamera(s, -90f, -35f);
            }, () -> sayac >= 40, null, 40).kadrajli(Kadraj.PULSAR),
            Adim.yeni("3B pulsar yandan (kamera pulsar yönüne dik ~" + YAN_ACI + "°)", s -> yanaKay(s),
                    () -> sayac >= YAN_BEKLEME, "pulsar_3b_yan", YAN_BEKLEME).kadrajli(Kadraj.PULSAR)
                    .istemcide(PulsarOtomasyonu::yanHazirla).bitince(PulsarOtomasyonu::paralaksSatiri),
            Adim.yeni("kara delik yaklaşması (hız 0.25)", s -> {
                AfetYoneticisi y = AfetYoneticisi.al(s);
                for (int i = 0; i < 3 && y.donem().gunesEvresi() == GunesEvresi.PULSAR; i++) {
                    komut(s, "dogalafet asama ileri pulsar");
                }
                komut(s, "dogalafet hiz 0.25", "time set 3000");
                kamera(s, -90f, -35f);
            }, () -> evre(GunesEvresi.KARADELIK_YUTMA) && evreSaniyesi() >= YAKLASMA_SN, "karadelik_yaklasma", 1400)
                    .kadrajli(Kadraj.YUTMA_ORTA).sabitSaniyede(YAKLASMA_SN),
            Adim.yeni("kara delik merceği (FOV " + MERCEK_FOV + ")", s -> {
            }, () -> evre(GunesEvresi.KARADELIK_YUTMA) && evreSaniyesi() >= MERCEK_SN, "karadelik_mercek", 2400)
                    .kadrajli(Kadraj.KARADELIK).sabitSaniyede(MERCEK_SN).istemcide(() -> fov(MERCEK_FOV))
                    .bitince(() -> fov(eskiFov)),
            Adim.yeni("spagettileşme", s -> {
            }, () -> evre(GunesEvresi.KARADELIK_YUTMA) && evreSaniyesi() >= SPAGETTI_SN, "karadelik_spagetti", 1000)
                    .kadrajli(Kadraj.YUTMA_ORTA).sabitSaniyede(SPAGETTI_SN),
            Adim.yeni("yutma parlaması", s -> {
            }, () -> evre(GunesEvresi.KARADELIK_YUTMA) && evreSaniyesi() >= PARLAMA_SN, "karadelik_parlama", 1600)
                    .kadrajli(Kadraj.YUTMA_ORTA).sabitSaniyede(PARLAMA_SN),
            Adim.yeni("karanlık (kamera kara deliğe)", s -> komut(s, "time set " + KARANLIK_SAATI),
                    () -> evre(GunesEvresi.KARADELIK_YUTMA) && evreSaniyesi() >= KARANLIK_SN, "karadelik_karanlik", 2200)
                    .kadrajli(Kadraj.KARADELIK).sabitSaniyede(KARANLIK_SN),
            Adim.yeni("yutma sonu (hız 50)", s -> komut(s, "dogalafet hiz 50"), () -> evre(GunesEvresi.GUNESSIZ), null, 400),
            Adim.yeni("güneşsiz buzul (tipi, donan göl, karda kül; 120 sn)", s -> {
                buzulSahnesi(s);
                komut(s, "dogalafet hiz 1", "time set 6000", "dogalafet donma 100", "weather rain");
                kamera(s, zeminKamerasi);
            }, () -> sayac >= BUZUL_TICK && evre(GunesEvresi.GUNESSIZ), "gunessiz_buz", BUZUL_TICK + 200),
            Adim.yeni("kızıl cüce doğuşu (şafak, hız 4; ufuk, doğuş yayı ve küre)", s -> {
                komut(s, "weather clear", "time set 23600", "dogalafet asama ileri gunessiz", "dogalafet hiz 4");
                gunesBak(s, DOGUS_PITCH);
            }, () -> evre(GunesEvresi.KIZIL_CUCE) && donem().evreIlerlemesi() >= DOGUS_ILERLEME, "kizil_cuce_dogus", 900),
            Adim.yeni("eski güneş döndü", s -> {
                komut(s, "dogalafet hiz 1", "dogalafet asama ileri kizilcuce", "time set 3000");
                gunesBak(s, Float.NaN);
            }, () -> evre(GunesEvresi.NORMAL) && evreSaniyesi() >= DONUS_SN + 1f, "gunes_dondu", 900));

    private PulsarOtomasyonu() {
    }

    private static List<String> seri(String onek, int n) {
        return IntStream.range(0, n).mapToObj(i -> onek + i).toList();
    }

    /** Kuyuda {@code SES_Y[i]} yüksekliğine taşınıp {@value #SES_BEKLEME} tick sonra sönüm satırı. */
    private static Adim sesAdimi(int i) {
        return Adim.yeni("ses derinliği: y " + SES_Y[i], s -> sesKamerasi(s, SES_Y[i]), () -> sayac >= SES_BEKLEME, null,
                SES_BEKLEME).bitince(() -> sesSatiri("", SES_Y[i]));
    }

    @SubscribeEvent
    public static void tusa(InputEvent.Key olay) {
        if (AKTIF) {
            tus++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tuş {} eylem {}", olay.getKey(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void fareye(InputEvent.MouseButton.Pre olay) {
        if (AKTIF) {
            fare++;
            DogalAfetler.LOGGER.warn(ON + "girdi: fare {} eylem {}", olay.getButton(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void tekerlege(InputEvent.MouseScrollingEvent olay) {
        if (AKTIF) {
            teker++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tekerlek {}", olay.getScrollDelta());
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (!AKTIF || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.info(ON + "kendi açmadığım duraklatma ekranı kapatıldı");
            mc.setScreen(null);
        }
        // Başarım toast'ları F1'de de çizilir; kareye girmesin.
        mc.getToasts().clear();
        if (kadraj != Kadraj.YOK && mc.player != null && mc.level != null) {
            kadrajaCevir(mc);
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null || GorselOtomasyon.kareBekliyor()) {
            return;
        }
        sayac++;
        if (sayac % 10 == 0) {
            server.execute(() -> {
                ucanMeteor = MeteorAPI.aktifMeteorSayisi(server.overworld());
                esdegerGun = KavurmaKuyrugu.esdegerGun(server.overworld());
            });
            if (sunucuHazir) {
                enDusukTps = Math.min(enDusukTps, tps(server));
            }
        }
        if (adim == -1) {
            if (sayac >= 100) {
                DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}", mc.player.getGameProfile().getName());
                GorselOtomasyon.cizimAyari();
                kameraAyari(mc);
                server.execute(() -> hazirla(server));
                sonraki();
            }
            return;
        }
        if (adim >= ADIMLAR.size()) {
            if (adim == ADIMLAR.size() && sayac >= 40) {
                DogalAfetler.LOGGER.info(ON + "bitti; en düşük TPS {}", String.format(Locale.ROOT, "%.1f", enDusukTps));
                GorselOtomasyon.kareOzetiYaz(OTOMASYON);
                DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
                adim++;
                mc.stop();
            }
            return;
        }
        if (!sunucuHazir) {
            sayac = 0;
            return;
        }
        Adim a = ADIMLAR.get(adim);
        if (!basladi) {
            if (!GorselOtomasyon.eylemKapisi(OTOMASYON, "adım " + a.ad())) {
                return;
            }
            basladi = true;
            sayac = 1;
            mc.options.hideGui = true;
            DogalAfetler.LOGGER.info(ON + "adım {}: {}", adim, a.ad());
            kadraj = a.kadraj();
            yazilanKadraj = null;
            a.istemci().run();
            server.execute(() -> a.sunucu().accept(server));
            return;
        }
        if (hazirTick < 0) {
            boolean hazir = sayac > 3 && a.hazir().getAsBoolean();
            if (!hazir && sayac < a.enCok()) {
                return;
            }
            hazirTick = sayac;
            if (!hazir) {
                DogalAfetler.LOGGER.warn(ON + "adım {} zaman aşımı ({} tick): koşul sağlanmadı, yine de devam", a.ad(), sayac);
            }
            if (!a.kareler().isEmpty()) {
                if (!Float.isNaN(a.sabitSn())) {
                    IstemciGokyuzu.sabitSaniye(a.sabitSn());
                }
                GorselOtomasyon.kareIste(OTOMASYON, DOSYA + a.kareler().get(0) + ".png", PulsarOtomasyonu::kareAlindi);
                return;
            }
            a.sonunda().run();
        }
        sonraki();
    }

    /**
     * Kare kapısı açılınca: satır, seride aynı kamerada hemen sonraki kare (ayrı {@code PULSAR_TITRESIM} satırları ve
     * gerçek aralıklar), sonra evre saniyesi canlıya döner ve adımın {@code sonunda}'sı çalışır.
     */
    private static void kareAlindi(GorselOtomasyon.Kare k) {
        Minecraft mc = Minecraft.getInstance();
        Adim a = ADIMLAR.get(adim);
        long simdi = System.nanoTime();
        if (kareSirasi == 0) {
            ilkKareNs = simdi;
        }
        float[] ofset = Pulsar3B.sonOfsetDerece();
        float[] px = cekirdekPikseli(mc, ofset);
        kareSatiri(k, ofset, px);
        if (a.kareler().size() > 1) {
            float[] s = {(simdi - ilkKareNs) / 1e6f, ofset == null ? Float.NaN : ofset[0],
                    ofset == null ? Float.NaN : ofset[1], px == null ? Float.NaN : px[0], px == null ? Float.NaN : px[1]};
            SERI.add(s);
            DogalAfetler.LOGGER.info(TITRESIM + "kare={} t_ms={} ofset_x={} ofset_y={} px_x={} px_y={}",
                    a.kareler().get(kareSirasi), sayi(s[0], 1), sayi(s[1], 3), sayi(s[2], 3), sayi(s[3], 1), sayi(s[4], 1));
        }
        kareSirasi++;
        if (kareSirasi < a.kareler().size()) {
            GorselOtomasyon.kareIste(OTOMASYON, DOSYA + a.kareler().get(kareSirasi) + ".png", PulsarOtomasyonu::kareAlindi);
            return;
        }
        if (a.kareler().size() > 1) {
            seriSatiri(a.kareler().get(0));
        }
        if (!Float.isNaN(a.sabitSn())) {
            IstemciGokyuzu.sabitSaniye(Float.NaN);
        }
        a.sonunda().run();
    }

    /** {@code PULSAR_TITRESIM seri ad=<seri> n=.. ofset_x=a;b;.. ofset_y=.. px_x=.. px_y=.. aralik_ms=.. toplam_ms=..}. */
    private static void seriSatiri(String ilk) {
        String ad = ilk.substring(0, ilk.lastIndexOf('_'));
        StringBuilder aralik = new StringBuilder();
        for (int i = 1; i < SERI.size(); i++) {
            aralik.append(i > 1 ? ";" : "").append(sayi(SERI.get(i)[0] - SERI.get(i - 1)[0], 1));
        }
        DogalAfetler.LOGGER.info(TITRESIM + "seri ad={} n={} ofset_x={} ofset_y={} px_x={} px_y={} aralik_ms={} "
                + "toplam_ms={}", ad, SERI.size(), seriSutunu(1, 3), seriSutunu(2, 3), seriSutunu(3, 1), seriSutunu(4, 1),
                aralik, sayi(SERI.get(SERI.size() - 1)[0], 1));
    }

    private static String seriSutunu(int i, int basamak) {
        return SERI.stream().map(s -> sayi(s[i], basamak)).collect(Collectors.joining(";"));
    }

    private static String sayi(float f, int basamak) {
        return Float.isNaN(f) ? "-" : String.format(Locale.ROOT, "%." + basamak + "f", f);
    }

    private static void kareSatiri(GorselOtomasyon.Kare k, float[] ofset, float[] px) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        DonemPaketi d = IstemciDonem.durum();
        float sabit = ADIMLAR.get(adim).sabitSn();
        int pulsarDortgen = Pulsar3B.sonDortgen();
        int karaDelikDortgen = KaraDelik3B.sonDortgen();
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, TPS {}, mspt {}, evre {}, ilerleme {}, evre sn {}, sabit sn {}, "
                        + "kül yağışı {}, uçan meteor {}, parçacık {}, dönüş yolu {}, blok hasarı {}, güneş döndü {}, "
                        + "dortgen={} (pulsar {} + kara delik {}), eşdeğer gün {}, ofset {} {}, çekirdek px {} {}, FOV {}, "
                        + "kapı beklemesi {} sn)", k.dosya(), k.fps(),
                server == null ? "?" : String.format(Locale.ROOT, "%.1f", tps(server)),
                server == null ? "?" : String.format(Locale.ROOT, "%.1f", server.getAverageTickTime()), d.gunesEvresi().kimlik(),
                String.format(Locale.ROOT, "%.3f", d.evreIlerlemesi()), String.format(Locale.ROOT, "%.1f", evreSaniyesi()),
                Float.isNaN(sabit) ? "-" : String.format(Locale.ROOT, "%.1f", sabit), PulsarIstemciEfektleri.kulYagisiSayaci(),
                ucanMeteor, mc.particleEngine.countParticles(), d.donusYolu(), d.blokHasari(), d.gunesDondu(),
                pulsarDortgen + karaDelikDortgen, pulsarDortgen, karaDelikDortgen,
                String.format(Locale.ROOT, "%.2f", esdegerGun), ofset == null ? "-" : sayi(ofset[0], 3),
                ofset == null ? "-" : sayi(ofset[1], 3), px == null ? "-" : sayi(px[0], 1), px == null ? "-" : sayi(px[1], 1),
                mc.options.fov().get(), String.format(Locale.ROOT, "%.1f", k.beklemeSn()));
    }

    private static void sonraki() {
        adim++;
        sayac = 0;
        basladi = false;
        hazirTick = -1;
        kareSirasi = 0;
        SERI.clear();
    }

    private static DonemPaketi donem() {
        return IstemciDonem.durum();
    }

    private static boolean evre(GunesEvresi e) {
        return donem().gunesEvresi() == e;
    }

    /** Evrenin başından beri geçen ölçekli saniye (canlı; gökyüzü G9 ile aynı saat). */
    private static float evreSaniyesi() {
        return (float) Math.max(0.0, (IstemciDonem.saat(0f) - donem().evreBaslangicSaati()) / 20.0);
    }

    private static float tps(IntegratedServer server) {
        float mspt = server.getAverageTickTime();
        return mspt <= 0f ? 20f : Math.min(20f, 1000f / mspt);
    }

    private static void komut(IntegratedServer s, String... komutlar) {
        for (String k : komutlar) {
            s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), k);
        }
    }

    private static ServerPlayer oyuncu(IntegratedServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static void kamera(IntegratedServer s, float yaw, float pitch) {
        kamera(s, new Kamera(kameraKonumu, yaw, pitch));
    }

    private static void kamera(IntegratedServer s, Kamera k) {
        oyuncu(s).teleportTo(s.overworld(), k.konum().x, k.konum().y, k.konum().z, k.yaw(), k.pitch());
    }

    /**
     * Çalışma anında ({@code options.txt}'ye yazılmaz) FOV efekti 0 ve kamera sallanması kapalı: uçan yaratıcı oyuncunun
     * FOV çarpanı ve sallanma, çekirdek pikselinin {@code fov} ayarından izdüşümünü bozmasın.
     */
    private static void kameraAyari(Minecraft mc) {
        eskiFov = mc.options.fov().get();
        mc.options.fovEffectScale().set(0.0);
        mc.options.bobView().set(false);
        DogalAfetler.LOGGER.info(ON + "kamera ayarı: FOV {}, FOV efekti 0, sallanma kapalı", eskiFov);
    }

    private static void fov(int derece) {
        Minecraft.getInstance().options.fov().set(derece);
        DogalAfetler.LOGGER.info(ON + "FOV {}", derece);
    }

    /**
     * Kamera güneşe (vanilla ya da kızıl cüce) döner. Gök çizicisi güneşi doğu-batı düzleminde tepeden
     * {@code getTimeOfDay · 360°} döndürür; sin &lt; 0 doğu (yaw −90) yarısıdır. {@code pitch} NaN ise güneşin
     * yüksekliği.
     */
    private static void gunesBak(IntegratedServer s, float pitch) {
        double aci = Math.toRadians(s.overworld().getTimeOfDay(1f) * 360.0);
        float yukseklik = (float) Math.toDegrees(Math.asin(Math.cos(aci)));
        float yaw = Math.sin(aci) < 0 ? -90f : 90f;
        float p = Float.isNaN(pitch) ? -yukseklik : pitch;
        kamera(s, yaw, p);
        DogalAfetler.LOGGER.info(ON + "gök kamerası: yaw {}, pitch {}, güneş yüksekliği {}°", yaw,
                String.format(Locale.ROOT, "%.1f", p), String.format(Locale.ROOT, "%.1f", yukseklik));
    }

    /** Kadrajın bu karedeki dünya yönü (kopya); nesne yoksa null. */
    private static Vector3f kadrajYonu(Minecraft mc) {
        return switch (kadraj) {
            case YOK -> null;
            case PULSAR -> pulsarYonu(mc, 1f);
            case KARADELIK -> karaDelikYonu(mc);
            case YUTMA_ORTA -> {
                Vector3f p = pulsarYonu(mc, 1f);
                Vector3f k = karaDelikYonu(mc);
                yield p == null ? k : k == null ? p : p.add(k).normalize();
            }
        };
    }

    private static Vector3f pulsarYonu(Minecraft mc, float pt) {
        return gecerli(IstemciGokyuzu.pulsarGorunurYonu(mc.level, pt));
    }

    private static Vector3f karaDelikYonu(Minecraft mc) {
        return gecerli(IstemciGokyuzu.karaDelikGorunurYonu(mc.level, 1f));
    }

    private static Vector3f gecerli(Vector3f v) {
        return v == null || !v.isFinite() || v.lengthSquared() < 1e-6f ? null : new Vector3f(v).normalize();
    }

    /** İstemci oyuncusunu (kamera) kadrajın yönüne çevirir; ara kare yumuşatması olmasın diye önceki açı da. */
    private static void kadrajaCevir(Minecraft mc) {
        Vector3f y = kadrajYonu(mc);
        if (y == null) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-y.x, y.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(y.y, -1f, 1f)));
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        if (kadraj != yazilanKadraj) {
            yazilanKadraj = kadraj;
            DogalAfetler.LOGGER.info(ON + "kadraj {}: yaw {}, pitch {} (takip)", kadraj,
                    String.format(Locale.ROOT, "%.1f", yaw), String.format(Locale.ROOT, "%.1f", pitch));
        }
    }

    /**
     * Pulsar çekirdeğinin son karedeki ekran pikseli {x, y}: görünen yön, titreşim ofseti kadar yönün sağına
     * (yatay) ve yukarısına döndürülüp kameranın eksenleriyle ({@code fov} dikey görüş açısı) izdüşürülür. Pulsar
     * yoksa ya da kameranın arkasındaysa null.
     */
    private static float[] cekirdekPikseli(Minecraft mc, float[] ofset) {
        GunesEvresi e = donem().gunesEvresi();
        if (mc.level == null || (e != GunesEvresi.PULSAR && e != GunesEvresi.KARADELIK_YUTMA)) {
            return null;
        }
        Vector3f d = pulsarYonu(mc, mc.getFrameTime());
        if (d == null) {
            return null;
        }
        if (ofset != null) {
            Vector3f sag = new Vector3f(d).cross(0f, 1f, 0f);
            if (sag.lengthSquared() > 1e-6f) {
                sag.normalize();
                Vector3f yukari = new Vector3f(sag).cross(d);
                d.add(sag.mul((float) Math.tan(Math.toRadians(ofset[0]))))
                        .add(yukari.mul((float) Math.tan(Math.toRadians(ofset[1])))).normalize();
            }
        }
        Camera c = mc.gameRenderer.getMainCamera();
        float z = d.dot(c.getLookVector());
        if (z <= 1e-4f) {
            return null;
        }
        float x = -d.dot(c.getLeftVector());
        float y = d.dot(c.getUpVector());
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        double f = 1.0 / Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
        double ndcX = x / z * f * h / w;
        double ndcY = y / z * f;
        return new float[] {(float) ((ndcX + 1.0) / 2.0 * w), (float) ((1.0 - ndcY) / 2.0 * h)};
    }

    /** İstemcide, taşımadan önce: pulsarın görünen yönü ve çapa uzaklığında {@value #YAN_ACI}° görünen kayma. */
    private static void yanHazirla() {
        Minecraft mc = Minecraft.getInstance();
        yanOncekiYon = pulsarYonu(mc, 1f);
        yanKayma = (float) (GokCapasi.PULSAR.uzaklik() * Math.tan(Math.toRadians(YAN_ACI)));
    }

    /** Sunucuda: gök kamerası pulsar yönüne dik yatay {@code yanKayma} blok kayar (çapa tau 60 sn ile geride kalır). */
    private static void yanaKay(IntegratedServer s) {
        Vector3f d = yanOncekiYon;
        Vec3 k = kameraKonumu;
        if (d == null) {
            DogalAfetler.LOGGER.warn(ON + "3B yan: pulsar yönü yok, kayma yapılmadı");
            return;
        }
        double yatay = Math.max(1e-3, Math.hypot(d.x, d.z));
        Vec3 hedef = k.add(-d.z / yatay * yanKayma, 0.0, d.x / yatay * yanKayma);
        oyuncu(s).teleportTo(s.overworld(), hedef.x, hedef.y, hedef.z, oyuncu(s).getYRot(), oyuncu(s).getXRot());
        DogalAfetler.LOGGER.info(ON + "3B yan: kamera {} -> {} ({} blok, pulsar yönüne dik)", k, hedef,
                String.format(Locale.ROOT, "%.0f", yanKayma));
    }

    /** Paralaks kanıtı: kaymadan önceki ve karedeki pulsar yönü arasındaki açı. */
    private static void paralaksSatiri() {
        Vector3f once = yanOncekiYon;
        Vector3f simdi = pulsarYonu(Minecraft.getInstance(), 1f);
        String aci = once == null || simdi == null ? "?"
                : String.format(Locale.ROOT, "%.1f", Math.toDegrees(Math.acos(Mth.clamp(once.dot(simdi), -1f, 1f))));
        DogalAfetler.LOGGER.info(ON + "3B yan: paralaks {}° (kayma {} blok, çapa {} blok; beklenen ~{}°)", aci,
                String.format(Locale.ROOT, "%.0f", yanKayma), String.format(Locale.ROOT, "%.0f", GokCapasi.PULSAR.uzaklik()),
                YAN_ACI);
    }

    /**
     * Sunucu thread'inde: kurallar (diğer afetler kapalı, blok hasarı açık), sabah, en yakın orman, uçan yaratıcı
     * kamera.
     */
    private static void hazirla(IntegratedServer s) {
        komut(s, "gamerule sendCommandFeedback false", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
                "gamerule doMobSpawning false", "gamerule kasirgaAfeti false", "gamerule tsunamiAfeti false",
                "gamerule depremAfeti false", "gamerule asitYagmuruAfeti false", "gamerule sinkholeAfeti false",
                "gamerule ormanYanginiAfeti false", "gamerule ayCarpmasiAfeti false", "gamerule afetBlokHasari true",
                "weather clear", "time set 3000", "dogalafet sifirla");
        ServerLevel level = s.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        Pair<BlockPos, Holder<Biome>> bulunan = level.findClosestBiome3d(h -> h.is(Biomes.FOREST) || h.is(Biomes.BIRCH_FOREST)
                || h.is(Biomes.FLOWER_FOREST), dogus, 6400, 32, 64);
        BlockPos merkez = bulunan == null ? dogus : bulunan.getFirst();
        int x = merkez.getX() - UZAKLIK;
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, merkez.getZ());
        ormanMerkezi = new BlockPos(merkez.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING, merkez.getX(),
                merkez.getZ()), merkez.getZ());
        kameraKonumu = new Vec3(x + 0.5, ust + KAMERA_YUKSEKLIGI, merkez.getZ() + 0.5);
        zeminKamerasi = new Kamera(new Vec3(x + 0.5, ust + ZEMIN_YUKSEKLIGI, merkez.getZ() + 0.5), -90f, ZEMIN_PITCH);
        ServerPlayer o = oyuncu(s);
        o.setGameMode(GameType.CREATIVE);
        o.getAbilities().flying = true;
        o.onUpdateAbilities();
        kamera(s, -90f, -35f);
        DogalAfetler.LOGGER.info(ON + "orman {} ({}), kamera {}, zemin kamerası {}", merkez.toShortString(),
                bulunan == null ? "bulunamadı (doğuş)"
                        : bulunan.getSecond().unwrapKey().map(k -> k.location().toString()).orElse("?"),
                kameraKonumu, zeminKamerasi);
        sunucuHazir = true;
    }

    /**
     * Zemin kamerasının {@value #GOL_UZAKLIGI} blok önünde bir blok gömük sığ göl ve kül bloğundan kıyı: göl donar, kıyının
     * karlı üstü ve kül yanları (kül → kar tablosu, PL-16) kareye girer. Komşu güncellemesiz yazılır (su akmaz).
     */
    private static void buzulSahnesi(IntegratedServer s) {
        ServerLevel level = s.overworld();
        int cx = Mth.floor(zeminKamerasi.konum().x) + GOL_UZAKLIGI;
        int cz = Mth.floor(zeminKamerasi.konum().z);
        int taban = Integer.MAX_VALUE;
        for (int dx = -GOL_YARI - 1; dx <= GOL_YARI + 1; dx++) {
            for (int dz = -GOL_YARI - 1; dz <= GOL_YARI + 1; dz++) {
                taban = Math.min(taban, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx + dx, cz + dz));
            }
        }
        int su = taban - 2;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -GOL_YARI - 1; dx <= GOL_YARI + 1; dx++) {
            for (int dz = -GOL_YARI - 1; dz <= GOL_YARI + 1; dz++) {
                int x = cx + dx;
                int z = cz + dz;
                if (Math.abs(dx) > GOL_YARI || Math.abs(dz) > GOL_YARI) {
                    int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
                    level.setBlock(p.set(x, ust, z), PulsarKayitlari.KUL_BLOGU.get().defaultBlockState(),
                            Block.UPDATE_CLIENTS);
                    continue;
                }
                for (int y = su + 1; y <= su + GOL_TEMIZLIK; y++) {
                    level.setBlock(p.set(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                if (level.getBlockState(p.set(x, su - 1, z)).isAir() || !level.getFluidState(p).isEmpty()) {
                    level.setBlock(p, Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(p.set(x, su, z), Blocks.WATER.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        DogalAfetler.LOGGER.info(ON + "buzul sahnesi: göl merkezi {} {} {}, {}x{}, kül bloğu kıyı", cx, su, cz,
                2 * GOL_YARI + 1, 2 * GOL_YARI + 1);
    }

    /**
     * Ses kuyusu (sunucu thread'i): zemin kamerasının arkasında 5×5 sütun en üst bloktan (yaprak dahil)
     * {@value #KUYU_TABAN}'nin üstüne kadar hava, taban taş; kuyu kenarından doğuya 1×2 tünel, ucunun 5×5 sütunu en az
     * {@value #TUNEL_ORTU} blok örtülü. Komşu güncellemesiz yazılır (sıvı ve kum akmaz).
     */
    private static void kuyuKaz(IntegratedServer s) {
        ServerLevel level = s.overworld();
        int cx = Mth.floor(zeminKamerasi.konum().x) + KUYU_DX;
        int cz = Mth.floor(zeminKamerasi.konum().z) + KUYU_DZ;
        int ust = KUYU_TABAN;
        for (int dx = -KUYU_YARI; dx <= KUYU_YARI; dx++) {
            for (int dz = -KUYU_YARI; dz <= KUYU_YARI; dz++) {
                ust = Math.max(ust, level.getHeight(Heightmap.Types.WORLD_SURFACE, cx + dx, cz + dz));
            }
        }
        int yuzey = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, cx, cz) - 1;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -KUYU_YARI; dx <= KUYU_YARI; dx++) {
            for (int dz = -KUYU_YARI; dz <= KUYU_YARI; dz++) {
                for (int y = KUYU_TABAN + 1; y < ust; y++) {
                    level.setBlock(p.set(cx + dx, y, cz + dz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                }
                level.setBlock(p.set(cx + dx, KUYU_TABAN, cz + dz), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        int ux = cx + KUYU_YARI + TUNEL_BOY;
        int ortu = Integer.MAX_VALUE;
        for (int dx = -KUYU_YARI; dx <= KUYU_YARI; dx++) {
            for (int dz = -KUYU_YARI; dz <= KUYU_YARI; dz++) {
                ortu = Math.min(ortu, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ux + dx, cz + dz));
            }
        }
        int ty = Math.max(KUYU_TABAN + 1, ortu - 2 - TUNEL_ORTU);
        for (int x = cx + KUYU_YARI + 1; x <= ux; x++) {
            level.setBlock(p.set(x, ty, cz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            level.setBlock(p.set(x, ty + 1, cz), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        kuyuMerkezi = new BlockPos(cx, KUYU_TABAN, cz);
        kuyuYuzeyi = yuzey;
        tunelUcu = new BlockPos(ux, ty, cz);
        DogalAfetler.LOGGER.info(ON + "ses kuyusu: merkez {} {}, yüzey {}, üst {}, taban {}; tünel ucu {} {} {} "
                + "(örtü üstü {})", cx, cz, yuzey, ust, KUYU_TABAN, ux, ty, cz, ortu);
    }

    /** İlk ölçüm y {@code SES_Y[0]}; yüzey ondan alçaksa yüzeyin bir üstü. */
    private static int kuyuYuksekligi(int i) {
        return i == 0 && kuyuYuzeyi < SES_Y[0] ? kuyuYuzeyi + 1 : SES_Y[i];
    }

    private static void sesKamerasi(IntegratedServer s, int y) {
        BlockPos m = kuyuMerkezi;
        kamera(s, new Kamera(new Vec3(m.getX() + 0.5, y, m.getZ() + 0.5), -90f, 0f));
    }

    /** {@code PULSAR_SES [kapali ]y=.. k=.. kuru=.. yankı=.. gök=.. hedef=..} ({@link PulsarSesi#sonumBilgisi}). */
    private static void sesSatiri(String etiket, int hedef) {
        PulsarSesi.Sonum s = PulsarSesi.sonumBilgisi();
        if (s == null) {
            DogalAfetler.LOGGER.warn(SES + etiket + "yok: pulsar sesi çalmıyor (hedef={})", hedef);
            return;
        }
        DogalAfetler.LOGGER.info(SES + etiket + "y={} k={} kuru={} yankı={} gök={} hedef={}",
                String.format(Locale.ROOT, "%.2f", s.y()), String.format(Locale.ROOT, "%.3f", s.k()),
                String.format(Locale.ROOT, "%.3f", s.kuru()), String.format(Locale.ROOT, "%.3f", s.yanki()),
                String.format(Locale.ROOT, "%.3f", s.gokOrani()), hedef);
    }

    /** Büyük meteor ormanın ortasına (kameranın {@value #UZAKLIK} blok önüne) düşer: izi kareye girer. */
    private static void meteorDusur(IntegratedServer s) {
        BlockPos m = ormanMerkezi;
        komut(s, String.format(Locale.ROOT, "execute positioned %d %d %d run dogalafet meteor dusur buyuk", m.getX(), m.getY(),
                m.getZ()));
        DogalAfetler.LOGGER.info(ON + "meteor kameranın önüne: {}", m.toShortString());
    }

    /** Oyuncunun 64 blok çevresinde yüzeyin en üst bloğu: kül, kaba toprak ve ateş sayısı (cehennemin kanıtı). */
    private static void cehennemSay(IntegratedServer s) {
        ServerLevel level = s.overworld();
        BlockPos m = oyuncu(s).blockPosition();
        int kul = 0;
        int kaba = 0;
        int ates = 0;
        for (int dx = -64; dx <= 64; dx++) {
            for (int dz = -64; dz <= 64; dz++) {
                int x = m.getX() + dx;
                int z = m.getZ() + dz;
                if (!level.hasChunk(x >> 4, z >> 4)) {
                    continue;
                }
                BlockPos ust = new BlockPos(x, level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1, z);
                String ad = BuiltInRegistries.BLOCK.getKey(level.getBlockState(ust).getBlock()).getPath();
                if (ad.startsWith("kul_")) {
                    kul++;
                } else if (ad.equals("coarse_dirt")) {
                    kaba++;
                } else if (ad.contains("fire") || ad.contains("ates")) {
                    ates++;
                }
            }
        }
        DogalAfetler.LOGGER.info(ON + "cehennem: 129x129 alanda yüzeyde {} kül, {} kaba toprak, {} ateş", kul, kaba, ates);
    }
}
