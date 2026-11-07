package io.github.kadirberkpolat1.dogalafetler.client.gokyuzu;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.KaraDelikYutmaEgrileri;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarZinciri;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
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
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Pulsar-2 görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=pulsar} ile tek oyunculu
 * dünyada çalışır; üretimde etkisizdir. Zincir komutlarla yürütülür: pulsar doğuşu ve tam güç (büyük pulsar, aynı
 * saniyede iki ardışık titreşim karesi) → blok hasarı açık kavurma birikimi ({@code hiz 30}) ve cehennem karesi (yanan
 * çimen, kül, kor, kül yağışı, kameranın önüne düşürülen meteorun izi) → pulsar gecesi → kara delik yutmasının dört anı
 * ({@code hiz 0.25} ile ağır çekim; her kare {@link IstemciGokyuzu#sabitSaniye} ile tam saniyede çizilir, düşük FPS'te de
 * aynı an, G2-10; kamera güneş yolunda pulsar ile yutan kara deliğin ortasına her tick çevrilir) → güneşsiz buzul
 * (donma %100) → kızıl cüce doğuşu (ilerleme ≥ {@value #DOGUS_ILERLEME}) → eski güneş ({@code NORMAL}, {@value #DONUS_SN} sn
 * geçişten sonra). Kamera en yakın ormanın {@value #UZAKLIK} blok batısında uçan yaratıcı oyuncudur: gök kareleri
 * ağaçların {@value #KAMERA_YUKSEKLIGI} blok üstünden pulsara, zemin kareleri {@value #ZEMIN_YUKSEKLIGI} blok üstten ormana
 * bakar. Kare {@link GorselOtomasyon} A-34 kapısından alınır; satır FPS, TPS, mspt, evre, evre saniyesi, kül yağışı
 * sayacı ve uçan meteoru yazar. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class PulsarOtomasyonu {
    private static final String OTOMASYON = "pulsar";
    private static final boolean AKTIF = OTOMASYON.equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "PULSAR_OTOMASYON ";
    private static final String DOSYA = "pulsar2_";
    private static final int UZAKLIK = 40;
    private static final int KAMERA_YUKSEKLIGI = 16;
    private static final int ZEMIN_YUKSEKLIGI = 6;
    /** Minecraft pitch'inde aşağı pozitiftir: ufuk kadrajın üst üçte birinde, zemin ağırlıkta. */
    private static final float ZEMIN_PITCH = 15f;
    private static final float GECE_PITCH = -5f;
    private static final float YAKLASMA_SN = 12f;
    private static final float SPAGETTI_SN = 40f;
    private static final float PARLAMA_SN = PulsarZinciri.YUTMA_PARLAMA_SN + KaraDelikYutmaEgrileri.PARLAMA_TEPE_SN;
    private static final float KARANLIK_SN = 80f;
    private static final float DOGUS_ILERLEME = 0.3f;
    private static final float DONUS_SN = 20f;

    /** Sabit kamera kaydı (konum, yaw, pitch). */
    private record Kamera(Vec3 konum, float yaw, float pitch) {
    }

    /**
     * Adım: istemci akıcıyken sunucu thread'inde {@code sunucu}, sonra {@code hazir} olunca (ya da {@code enCok} tick'te)
     * kare. {@code sabitSn} NaN değilse kare o evre saniyesinde çizilir; {@code ardisik} varsa ilk kareden hemen sonra
     * aynı kamerada ikinci kare alınır.
     */
    private record Adim(String ad, Consumer<IntegratedServer> sunucu, BooleanSupplier hazir, String kare, int enCok,
            float sabitSn, String ardisik) {
        Adim(String ad, Consumer<IntegratedServer> sunucu, BooleanSupplier hazir, String kare, int enCok) {
            this(ad, sunucu, hazir, kare, enCok, Float.NaN, null);
        }
    }

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static boolean basladi;
    private static int hazirTick = -1;
    private static boolean ardisikIstendi;
    private static long ilkKareNs;
    private static float enDusukTps = 20f;
    private static volatile boolean sunucuHazir;
    private static volatile Vec3 kameraKonumu;
    private static volatile Kamera zeminKamerasi;
    private static volatile BlockPos ormanMerkezi;
    private static volatile int ucanMeteor;
    /** Yutma kareleri boyunca kamera her istemci tick'inde pulsar ile kara deliğin ortasına çevrilir. */
    private static volatile boolean yutmaKadraji;
    private static boolean kadrajYazildi;

    private static final List<Adim> ADIMLAR = List.of(
            new Adim("pulsar doğuşu ve tam güç (gündüz)", s -> {
                komut(s, "dogalafet pulsar baslat");
                DunyaDonemi d = AfetYoneticisi.al(s).donem();
                if (d.gunesEvresi() == GunesEvresi.PULSAR && d.evreIlerlemesi() < 1f) {
                    komut(s, "dogalafet asama ileri pulsar");
                }
                komut(s, "time set 3000");
                gunesBak(s);
            }, () -> sayac >= 80 && evre(GunesEvresi.PULSAR) && donem().evreIlerlemesi() >= 1f, "pulsar_buyuk", 400),
            new Adim("pulsar titreşimi (iki ardışık kare)", s -> {
            }, () -> sayac >= 20, "pulsar_titresim_a", 100, Float.NaN, "pulsar_titresim_b"),
            new Adim("kavurma birikimi (hız 30, 45 sn)", s -> {
                komut(s, "time set 6000", "dogalafet hiz 30");
                kamera(s, zeminKamerasi);
            }, () -> sayac >= 900, null, 1000),
            new Adim("cehennem", s -> {
                komut(s, "dogalafet hiz 1");
                kamera(s, zeminKamerasi);
                cehennemSay(s);
                meteorDusur(s);
            }, () -> sayac >= 60 && ucanMeteor >= 1, "pulsar_cehennem", 300),
            new Adim("pulsar gecesi", s -> {
                komut(s, "time set 18000");
                kamera(s, new Kamera(zeminKamerasi.konum(), zeminKamerasi.yaw(), GECE_PITCH));
            }, () -> sayac >= 60, "pulsar_gece", 200),
            new Adim("kara delik yaklaşması (hız 0.25)", s -> {
                AfetYoneticisi y = AfetYoneticisi.al(s);
                for (int i = 0; i < 3 && y.donem().gunesEvresi() == GunesEvresi.PULSAR; i++) {
                    komut(s, "dogalafet asama ileri pulsar");
                }
                komut(s, "dogalafet hiz 0.25", "time set 3000");
                kamera(s, -90f, -35f);
                yutmaKadraji = true;
            }, () -> evre(GunesEvresi.KARADELIK_YUTMA) && evreSaniyesi() >= YAKLASMA_SN, "karadelik_yaklasma", 1400,
                    YAKLASMA_SN, null),
            new Adim("spagettileşme", s -> {
            }, () -> evre(GunesEvresi.KARADELIK_YUTMA) && evreSaniyesi() >= SPAGETTI_SN, "karadelik_spagetti", 2600,
                    SPAGETTI_SN, null),
            new Adim("yutma parlaması", s -> {
            }, () -> evre(GunesEvresi.KARADELIK_YUTMA) && evreSaniyesi() >= PARLAMA_SN, "karadelik_parlama", 1600,
                    PARLAMA_SN, null),
            new Adim("karanlık", s -> {
            }, () -> evre(GunesEvresi.KARADELIK_YUTMA) && evreSaniyesi() >= KARANLIK_SN, "karadelik_karanlik", 2200,
                    KARANLIK_SN, null),
            new Adim("yutma sonu (hız 50)", s -> {
                yutmaKadraji = false;
                komut(s, "dogalafet hiz 50");
            }, () -> evre(GunesEvresi.GUNESSIZ), null, 400),
            new Adim("güneşsiz buzul", s -> {
                komut(s, "dogalafet hiz 1", "time set 6000", "dogalafet donma 100");
                kamera(s, zeminKamerasi);
            }, () -> sayac >= 200 && evre(GunesEvresi.GUNESSIZ), "gunessiz_buz", 400),
            new Adim("kızıl cüce doğuşu (şafak, hız 4)", s -> {
                komut(s, "time set 23600", "dogalafet asama ileri gunessiz", "dogalafet hiz 4");
                kamera(s, -90f, -8f);
            }, () -> evre(GunesEvresi.KIZIL_CUCE) && donem().evreIlerlemesi() >= DOGUS_ILERLEME, "kizil_cuce_dogus", 900),
            new Adim("eski güneş döndü", s -> {
                komut(s, "dogalafet hiz 1", "dogalafet asama ileri kizilcuce", "time set 3000");
                gunesBak(s);
            }, () -> evre(GunesEvresi.NORMAL) && evreSaniyesi() >= DONUS_SN + 1f, "gunes_dondu", 900));

    private PulsarOtomasyonu() {
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
        if (yutmaKadraji && mc.player != null && mc.level != null) {
            yutmayaCevir(mc);
        }
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null || GorselOtomasyon.kareBekliyor()) {
            return;
        }
        sayac++;
        if (sayac % 10 == 0) {
            server.execute(() -> ucanMeteor = MeteorAPI.aktifMeteorSayisi(server.overworld()));
            if (sunucuHazir) {
                enDusukTps = Math.min(enDusukTps, tps(server));
            }
        }
        if (adim == -1) {
            if (sayac >= 100) {
                DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}", mc.player.getGameProfile().getName());
                GorselOtomasyon.cizimAyari();
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
            if (a.kare() != null) {
                if (!Float.isNaN(a.sabitSn())) {
                    IstemciGokyuzu.sabitSaniye(a.sabitSn());
                }
                GorselOtomasyon.kareIste(OTOMASYON, DOSYA + a.kare() + ".png", PulsarOtomasyonu::kareAlindi);
                return;
            }
        }
        sonraki();
    }

    /** Kare kapısı açılınca: satır, gerekirse aynı kamerada hemen ardışık kare, sonra evre saniyesi canlıya döner. */
    private static void kareAlindi(GorselOtomasyon.Kare k) {
        kareSatiri(k);
        Adim a = ADIMLAR.get(adim);
        if (a.ardisik() != null && !ardisikIstendi) {
            ardisikIstendi = true;
            ilkKareNs = System.nanoTime();
            GorselOtomasyon.kareIste(OTOMASYON, DOSYA + a.ardisik() + ".png", PulsarOtomasyonu::kareAlindi);
            return;
        }
        if (ardisikIstendi) {
            DogalAfetler.LOGGER.info(ON + "ardışık kare aralığı {} ms (aynı kamera)",
                    String.format(Locale.ROOT, "%.0f", (System.nanoTime() - ilkKareNs) / 1e6));
        }
        if (!Float.isNaN(a.sabitSn())) {
            IstemciGokyuzu.sabitSaniye(Float.NaN);
        }
    }

    private static void kareSatiri(GorselOtomasyon.Kare k) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        DonemPaketi d = IstemciDonem.durum();
        float sabit = ADIMLAR.get(adim).sabitSn();
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, TPS {}, mspt {}, evre {}, ilerleme {}, evre sn {}, sabit sn {}, "
                        + "kül yağışı {}, uçan meteor {}, parçacık {}, dönüş yolu {}, blok hasarı {}, güneş döndü {}, "
                        + "kapı beklemesi {} sn)", k.dosya(), k.fps(),
                server == null ? "?" : String.format(Locale.ROOT, "%.1f", tps(server)),
                server == null ? "?" : String.format(Locale.ROOT, "%.1f", server.getAverageTickTime()), d.gunesEvresi().kimlik(),
                String.format(Locale.ROOT, "%.3f", d.evreIlerlemesi()), String.format(Locale.ROOT, "%.1f", evreSaniyesi()),
                Float.isNaN(sabit) ? "-" : String.format(Locale.ROOT, "%.1f", sabit), PulsarIstemciEfektleri.kulYagisiSayaci(),
                ucanMeteor, mc.particleEngine.countParticles(), d.donusYolu(), d.blokHasari(), d.gunesDondu(),
                String.format(Locale.ROOT, "%.1f", k.beklemeSn()));
    }

    private static void sonraki() {
        adim++;
        sayac = 0;
        basladi = false;
        hazirTick = -1;
        ardisikIstendi = false;
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
     * Kamera pulsara (güneşin yerine) döner. Gök çizicisi güneşi doğu-batı düzleminde tepeden
     * {@code getTimeOfDay · 360°} döndürür; sin &lt; 0 doğu (yaw −90) yarısıdır.
     */
    private static void gunesBak(IntegratedServer s) {
        double aci = Math.toRadians(s.overworld().getTimeOfDay(1f) * 360.0);
        float yukseklik = (float) Math.toDegrees(Math.asin(Math.cos(aci)));
        float yaw = Math.sin(aci) < 0 ? -90f : 90f;
        kamera(s, yaw, -yukseklik);
        DogalAfetler.LOGGER.info(ON + "gök kamerası: yaw {}, güneş yüksekliği {}°", yaw,
                String.format(Locale.ROOT, "%.1f", yukseklik));
    }

    /**
     * İstemci oyuncusunu (kamera) güneş yolunda pulsar ile yutan kara deliğin ortasına çevirir (yön çizimle aynı hesaptan,
     * {@link IstemciGokyuzu#gunesYoluYonu}); ara kare yumuşatması olmasın diye önceki açı da.
     */
    private static void yutmayaCevir(Minecraft mc) {
        org.joml.Vector3f y = IstemciGokyuzu.gunesYoluYonu(KaraDelikYutmaEgrileri.KARADELIK_ACI / 2f, mc.level, 1f);
        float yaw = (float) Math.toDegrees(Math.atan2(-y.x, y.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Mth.clamp(y.y, -1f, 1f)));
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        if (!kadrajYazildi) {
            kadrajYazildi = true;
            DogalAfetler.LOGGER.info(ON + "yutma kadrajı: pulsar ile kara deliğin ortası ({}° öte), yaw {}, pitch {} (takip)",
                    KaraDelikYutmaEgrileri.KARADELIK_ACI / 2f, String.format(Locale.ROOT, "%.1f", yaw),
                    String.format(Locale.ROOT, "%.1f", pitch));
        }
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
