package io.github.kadirberkpolat1.dogalafetler.client.yangin;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKayitlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=yangin} ile tek oyunculu dünyada çalışır;
 * üretimde etkisizdir. Entegre sunucuda en yakın ormanın en sık bölümünü bulur, alacakaranlığa alır, günün rüzgârına dik
 * bir taç cephesi tutuşturur. Seyirci kamera cepheyi YANDAN (cephe çizgisinin ucundan, biraz rüzgâr üstünden) görür; uzaklık,
 * görünen ateş bloklarının kare kaplaması tahmini hedefi geçen en uzak aday olarak seçilir. Yüksek ve Düşük kalite
 * karelerini, uzaktan duman sütunu/pirokümülüs karesini ve yakın kamerayla yağmurla sönme karelerini alır, yangını
 * söndürüp kapanır. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class YanginOtomasyonu {
    private static final boolean AKTIF = "yangin".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "YANGIN_OTOMASYON ";
    /** Tasarım notu §6 seviye 4: taç yangını ve 8 blok sıçrama. */
    private static final double VITRIN_YOGUNLUGU = 0.7;
    /** Kalite kareleri arası (tick): parçacıklar her geçişte silinir, iki kare aynı parçacık yaşında. */
    private static final int KARE_ARALIGI = 120;
    /** Kamera uzaklığı adayları (cephenin yakın ucundan blok), uzaktan yakına. */
    private static final int[] VITRIN_ADAYLARI = {24, 20, 16, 13, 10};
    private static final int[] YAGMUR_ADAYLARI = {26, 20, 15, 11};
    /**
     * Görünen ateş bloklarının izdüşüm alanı toplamı hedefi (kare oranı). Koşu 6'da ölçülen sıcak piksel / bu tahmin
     * ≈ 0,3 idi: 0,35 tahmin, kadrajda ~%10 ateş demektir.
     */
    private static final double VITRIN_KAPLAMA = 0.35;
    private static final double YAGMUR_KAPLAMA = 0.2;
    private static final int SUTUN_UZAKLIGI = 64;
    private static final int TARAMA = 64;
    private static final double DIKEY_YARI_ACI = Math.toRadians(35);

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static volatile boolean sunucuHazir;
    private static volatile BlockPos odak;
    private static volatile int cepheTepesi;
    private static volatile double enBoy = 16.0 / 9.0;

    private YanginOtomasyonu() {
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
        IntegratedServer server = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || server == null) {
            return;
        }
        enBoy = mc.getWindow().getWidth() / (double) Math.max(1, mc.getWindow().getHeight());
        sayac++;
        if ((adim == 1 || adim == 2) && sayac % 100 == 0) {
            int a = adim;
            int s = sayac;
            server.execute(() -> DogalAfetler.LOGGER.info(ON + "adım {} tick {}: aktif {}", a, s,
                    YanginYoneticisi.aktifSayi(server.overworld())));
        }
        switch (adim) {
            case -1 -> {
                if (sayac >= 100) {
                    DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}", mc.player.getGameProfile().getName());
                    server.execute(() -> hazirla(server));
                    sonraki();
                }
            }
            case 0 -> {
                if (sunucuHazir && sayac >= 300) {
                    mc.options.hideGui = true;
                    kaliteyle(mc, EfektKalitesi.YUKSEK);
                    server.execute(() -> tutustur(server));
                    sonraki();
                }
            }
            case 1 -> {
                if (sayac == 70) {
                    server.execute(() -> kameraSec(server, "vitrin", VITRIN_ADAYLARI, VITRIN_KAPLAMA, false));
                } else if (sayac == KARE_ARALIGI) {
                    komut(server, "forge tps");
                    komut(server, "dogalafet yangin bilgi");
                    kare(mc, server, "vitrin_yuksek");
                    kaliteyle(mc, EfektKalitesi.DUSUK);
                } else if (sayac == 2 * KARE_ARALIGI) {
                    kare(mc, server, "vitrin_dusuk");
                    komut(server, "dogalafet yangin bilgi");
                    kaliteyle(mc, null);
                    server.execute(() -> sutunKamerasi(server));
                } else if (sayac == 2 * KARE_ARALIGI + 160) {
                    kare(mc, server, "sutun");
                    sonraki();
                }
            }
            case 2 -> {
                if (sayac == 1) {
                    server.execute(() -> kameraSec(server, "yagmur", YAGMUR_ADAYLARI, YAGMUR_KAPLAMA, true));
                } else if (sayac == 40) {
                    server.execute(() -> {
                        server.overworld().setWeatherParameters(0, 6000, true, false);
                        DogalAfetler.LOGGER.info(ON + "yağmur başladı; aktif {}", YanginYoneticisi.aktifSayi(server.overworld()));
                    });
                } else if (sayac == 65) {
                    kare(mc, server, "yagmur");
                    komut(server, "dogalafet yangin bilgi");
                } else if (sayac == 340) {
                    kare(mc, server, "yagmur_sonra");
                    komut(server, "dogalafet yangin bilgi");
                    sonraki();
                }
            }
            case 3 -> {
                if (sayac == 1) {
                    server.execute(() -> {
                        int sonen = YanginYoneticisi.sondur(server.overworld(), null);
                        server.overworld().setWeatherParameters(6000, 0, false, false);
                        DogalAfetler.LOGGER.info(ON + "bitti; söndürülen {}; FPS {}", sonen, mc.getFps());
                    });
                } else if (sayac >= 60) {
                    DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
                    adim = 99;
                    mc.stop();
                }
            }
            default -> {
            }
        }
    }

    private static void sonraki() {
        adim++;
        sayac = 0;
    }

    /** Sunucu thread'inde: kurallar, alacakaranlık, en yakın orman, seyirci kamera (chunk'lar yüklensin). */
    private static void hazirla(IntegratedServer server) {
        for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 13000", "weather clear"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), k);
        }
        ServerLevel level = server.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        Pair<BlockPos, Holder<Biome>> bulunan = level.findClosestBiome3d(h -> h.is(Biomes.FOREST)
                || h.is(Biomes.DARK_FOREST) || h.is(Biomes.BIRCH_FOREST) || h.is(Biomes.FLOWER_FOREST), dogus, 6400, 32, 64);
        BlockPos merkez = bulunan == null ? dogus : bulunan.getFirst();
        DogalAfetler.LOGGER.info(ON + "orman: {} {}", merkez.toShortString(),
                bulunan == null ? "bulunamadı (doğuş)" : bulunan.getSecond().unwrapKey().map(k -> k.location().toString())
                        .orElse("?"));
        odak = merkez;
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.setGameMode(GameType.SPECTATOR);
        oyuncu.teleportTo(level, merkez.getX() + 0.5, 150, merkez.getZ() + 0.5, 0.0F, 30.0F);
        sunucuHazir = true;
    }

    /** Sunucu thread'inde: ormanın en sık bölümünde günün rüzgârına dik 33 blokluk bir taç cephesi tutuşturur. */
    private static void tutustur(IntegratedServer server) {
        ServerLevel level = server.overworld();
        BlockPos m = enSikOrman(level, odak);
        odak = m;
        double[] w = ruzgarYonu(level);
        int toplam = 0;
        int tepe = level.getMinBuildHeight();
        for (int k = -16; k <= 16; k += 4) {
            BlockPos ust = ustBlok(level, m.getX() + (int) Math.round(-w[1] * k), m.getZ() + (int) Math.round(w[0] * k));
            if (ust != null && level.getBlockState(ust).is(BlockTags.LEAVES)) {
                tepe = Math.max(tepe, ust.getY());
                toplam += YanginYoneticisi.alanTutustur(level, ust, 3, YanginKaynagi.KOMUT, VITRIN_YOGUNLUGU);
            }
        }
        cepheTepesi = tepe;
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(level, m.getX() + 0.5 + w[1] * 40, tepe + 8, m.getZ() + 0.5 - w[0] * 40,
                yaw(-w[1], w[0]), 15.0F);
        DogalAfetler.LOGGER.info(ON + "rüzgâr ({}, {}); cephe {} hücre, tepe y={}", f(w[0]), f(w[1]), toplam, tepe);
    }

    /**
     * Sunucu thread'inde: kamerayı cephenin yanına koyar. Cephe çizgisi rüzgâra diktir (p); kamera çizginin -p ucundan
     * {@code aday} blok dışarıda, 4 blok rüzgâr üstünde, taçların 3+ blok üstündedir ve cephenin yakın kısmına bakar;
     * yangın kadrajda soldan sağa (rüzgâr altına) ilerler. Kaplama tahmini hedefi geçen en uzak aday seçilir.
     * {@code yalnizYagmurlu}: yalnız yağışı {@code RAIN} olan biyomdaki ateş sayılır (yağışsız savanada yağmur yangını
     * söndürmez, Y-08; yağmur karesi sönmeyi göstermeli).
     */
    private static void kameraSec(IntegratedServer server, String ad, int[] adaylar, double hedefKaplama,
            boolean yalnizYagmurlu) {
        ServerLevel level = server.overworld();
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        List<BlockPos> atesler = atesBloklari(level);
        if (yalnizYagmurlu) {
            int toplam = atesler.size();
            atesler.removeIf(a -> level.getBiome(a).value().getPrecipitationAt(a) != Biome.Precipitation.RAIN);
            DogalAfetler.LOGGER.info(ON + "kamera {}: {} ateş bloğunun {}'i yağmurlu biyomda", ad, toplam, atesler.size());
        }
        if (atesler.isEmpty()) {
            DogalAfetler.LOGGER.warn(ON + "kamera {}: ateş bloğu yok, kamera değişmedi", ad);
            return;
        }
        double[] w = ruzgarYonu(level);
        double px = -w[1];
        double pz = w[0];
        double cx = 0;
        double cy = 0;
        double cz = 0;
        int tepe = Integer.MIN_VALUE;
        for (BlockPos a : atesler) {
            cx += a.getX() + 0.5;
            cy += a.getY() + 0.5;
            cz += a.getZ() + 0.5;
            tepe = Math.max(tepe, a.getY());
        }
        cx /= atesler.size();
        cy /= atesler.size();
        cz /= atesler.size();
        double yakinUc = 0;
        for (BlockPos a : atesler) {
            yakinUc = Math.max(yakinUc, -((a.getX() + 0.5 - cx) * px + (a.getZ() + 0.5 - cz) * pz));
        }
        yakinUc = Math.min(yakinUc, 24);
        double hedefX = cx - px * (yakinUc - 8);
        double hedefZ = cz - pz * (yakinUc - 8);
        Vec3 hedef = new Vec3(hedefX, cy, hedefZ);
        Vec3 secilen = null;
        Gorunurluk secilenG = null;
        int secilenAday = -1;
        StringBuilder adaylarLog = new StringBuilder();
        for (int aday : adaylar) {
            double kx = cx - px * (yakinUc + aday) - w[0] * 4;
            double kz = cz - pz * (yakinUc + aday) - w[1] * 4;
            int zemin = tepe + 3;
            for (int i = 0; i <= aday; i += 2) {
                double t = i / (double) Math.max(1, aday);
                zemin = Math.max(zemin, level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                        (int) Math.floor(kx + (hedefX - kx) * t * 0.5), (int) Math.floor(kz + (hedefZ - kz) * t * 0.5)) + 1);
            }
            Vec3 goz = new Vec3(kx, zemin + 1.62, kz);
            Gorunurluk g = gorunurluk(level, oyuncu, goz, hedef.subtract(goz).normalize(), atesler);
            adaylarLog.append(String.format(Locale.ROOT, " %d:%.3f/%d", aday, g.kaplama, g.gorunen));
            if (secilenG == null || (secilenG.kaplama < hedefKaplama && g.kaplama > secilenG.kaplama)) {
                secilen = goz;
                secilenG = g;
                secilenAday = aday;
            }
            if (g.kaplama >= hedefKaplama) {
                secilen = goz;
                secilenG = g;
                secilenAday = aday;
                break;
            }
        }
        Vec3 bakis = hedef.subtract(secilen).normalize();
        oyuncu.teleportTo(level, secilen.x, secilen.y - 1.62, secilen.z, yaw(bakis.x, bakis.z), pitch(bakis));
        DogalAfetler.LOGGER.info(ON + "kamera {}: aday (uzaklık:kaplama/görünen){}; seçilen {} blok, kaplama tahmini {}, "
                + "görünen ateş {}, ortalama uzaklık {} blok", ad, adaylarLog, secilenAday, f(secilenG.kaplama),
                secilenG.gorunen, f(secilenG.ortalamaUzaklik));
    }

    /** Sunucu thread'inde: duman sütunu ve pirokümülüs için cephenin yanından 64 blok öteden, hafif yukarı bakan kamera. */
    private static void sutunKamerasi(IntegratedServer server) {
        ServerLevel level = server.overworld();
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        BlockPos m = odak;
        double[] w = ruzgarYonu(level);
        double kx = m.getX() + 0.5 + w[1] * SUTUN_UZAKLIGI - w[0] * 8;
        double kz = m.getZ() + 0.5 - w[0] * SUTUN_UZAKLIGI - w[1] * 8;
        int zemin = Math.max(cepheTepesi + 6, level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) kx, (int) kz) + 4);
        Vec3 goz = new Vec3(kx, zemin + 1.62, kz);
        Vec3 bakis = new Vec3(m.getX() + 0.5, cepheTepesi + 14, m.getZ() + 0.5).subtract(goz).normalize();
        oyuncu.teleportTo(level, kx, zemin, kz, yaw(bakis.x, bakis.z), pitch(bakis));
        DogalAfetler.LOGGER.info(ON + "kamera sutun: ({}, {}, {}) eğim {}", (int) kx, zemin, (int) kz, f(pitch(bakis)));
    }

    /** Bulunan orman noktası çoğunlukla kenardır: ±24 blok içinde tepesi yaprak olan sütunu en çok merkez seçilir. */
    private static BlockPos enSikOrman(ServerLevel level, BlockPos m) {
        BlockPos en = m;
        int enSkor = -1;
        for (int ox = -24; ox <= 24; ox += 8) {
            for (int oz = -24; oz <= 24; oz += 8) {
                int skor = 0;
                for (int dx = -12; dx <= 12; dx += 2) {
                    for (int dz = -12; dz <= 12; dz += 2) {
                        BlockPos ust = ustBlok(level, m.getX() + ox + dx, m.getZ() + oz + dz);
                        if (ust != null && level.getBlockState(ust).is(BlockTags.LEAVES)) {
                            skor++;
                        }
                    }
                }
                if (skor > enSkor) {
                    enSkor = skor;
                    en = new BlockPos(m.getX() + ox, m.getY(), m.getZ() + oz);
                }
            }
        }
        DogalAfetler.LOGGER.info(ON + "en sık orman {} (yaprak tepeli sütun {}/169)", en.toShortString(), enSkor);
        return en;
    }

    /** Sütunun en üst engelleyici bloğu; chunk yüklü değilse {@code null}. */
    private static BlockPos ustBlok(ServerLevel level, int x, int z) {
        BlockPos p = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z);
        return level.isLoaded(p) ? p : null;
    }

    /** Cephe çevresindeki ateş blokları ({@link #TARAMA} blok, taç tepesinin 24 altı ile 8 üstü). */
    private static List<BlockPos> atesBloklari(ServerLevel level) {
        List<BlockPos> sonuc = new ArrayList<>();
        BlockPos m = odak;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -TARAMA; x <= TARAMA; x++) {
            for (int z = -TARAMA; z <= TARAMA; z++) {
                for (int y = cepheTepesi - 24; y <= cepheTepesi + 8; y++) {
                    p.set(m.getX() + x, y, m.getZ() + z);
                    if (level.isLoaded(p) && level.getBlockState(p).is(YanginKayitlari.AFET_ATESI.get())) {
                        sonuc.add(p.immutable());
                    }
                }
            }
        }
        return sonuc;
    }

    private record Gorunurluk(int cercevede, int gorunen, double ortalamaUzaklik, double kaplama,
            Map<String, Integer> biyomlar) {
    }

    /**
     * Ateş bloklarından kaçı kameranın görüş alanında (dikey 70°, pencere en/boy oranı) ve kameradan açık görüş hattında
     * (yaprak dahil katı blok araya girmiyor). Kaplama tahmini: görünen her blok için kameraya bakan 1×1 karenin kare
     * alanına oranı {@code 1 / (4 d² tan²(35°) · enBoy)} toplamı. Ateş blokları biyomlarına göre de sayılır.
     */
    private static Gorunurluk gorunurluk(ServerLevel level, ServerPlayer oyuncu, Vec3 goz, Vec3 ileri,
            List<BlockPos> atesler) {
        Vec3 sag = ileri.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 yukari = sag.cross(ileri);
        double dikey = Math.tan(DIKEY_YARI_ACI);
        double yatay = dikey * enBoy;
        int cercevede = 0;
        int gorunen = 0;
        double uzaklik = 0;
        double kaplama = 0;
        Map<String, Integer> biyomlar = new TreeMap<>();
        for (BlockPos a : atesler) {
            Holder<Biome> biyom = level.getBiome(a);
            biyomlar.merge(biyom.unwrapKey().map(k -> k.location().getPath()).orElse("?")
                    + (biyom.value().hasPrecipitation() ? "" : "(yağışsız)"), 1, Integer::sum);
            Vec3 hedef = new Vec3(a.getX() + 0.5, a.getY() + 0.3, a.getZ() + 0.5);
            Vec3 v = hedef.subtract(goz);
            double ileriBilesen = v.dot(ileri);
            if (ileriBilesen <= 0 || Math.abs(v.dot(sag) / ileriBilesen) > yatay
                    || Math.abs(v.dot(yukari) / ileriBilesen) > dikey) {
                continue;
            }
            cercevede++;
            HitResult carpma = level.clip(new ClipContext(goz, hedef, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    oyuncu));
            double d = v.length();
            if (carpma.getType() == HitResult.Type.MISS || carpma.getLocation().distanceTo(goz) >= d - 1.0) {
                gorunen++;
                uzaklik += d;
                kaplama += Math.min(0.05, 1.0 / (4.0 * d * d * dikey * dikey * enBoy));
            }
        }
        return new Gorunurluk(cercevede, gorunen, gorunen == 0 ? 0 : uzaklik / gorunen, kaplama, biyomlar);
    }

    private static void gorunurlukYaz(IntegratedServer server, String ad) {
        ServerLevel level = server.overworld();
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        List<BlockPos> atesler = atesBloklari(level);
        Gorunurluk g = gorunurluk(level, oyuncu, oyuncu.getEyePosition(), oyuncu.getViewVector(1.0F), atesler);
        DogalAfetler.LOGGER.info(ON + "{}: görünen ateş bloğu {} (çerçevede {}, taranan {}), ortalama uzaklık {} blok, "
                + "kaplama tahmini {}, aktif {}; biyomlar {}", ad, g.gorunen, g.cercevede, atesler.size(),
                f(g.ortalamaUzaklik), f(g.kaplama), YanginYoneticisi.aktifSayi(level), g.biyomlar);
    }

    /** Günün rüzgârının birim yönü (yayılmayla aynı formül). */
    private static double[] ruzgarYonu(ServerLevel level) {
        double[] r = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
        double guc = Math.hypot(r[0], r[1]);
        return new double[] {r[0] / guc, r[1] / guc};
    }

    private static float yaw(double x, double z) {
        return (float) Math.toDegrees(Math.atan2(-x, z));
    }

    private static float pitch(Vec3 bakis) {
        return (float) -Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, bakis.y))));
    }

    private static String f(double sayi) {
        return String.format(Locale.ROOT, "%.3f", sayi);
    }

    private static void komut(IntegratedServer server, String komut) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), komut));
    }

    /** Kalite değişince eski kademenin parçacıkları silinir ({@code setLevel} motoru boşaltır): kare yalnız yenisini gösterir. */
    private static void kaliteyle(Minecraft mc, EfektKalitesi kalite) {
        IstemciYangin.kaliteZorla(kalite);
        mc.particleEngine.setLevel(mc.level);
        IstemciYangin.uretileniAl();
        DogalAfetler.LOGGER.info(ON + "kalite {} (parçacıklar silindi)", kalite == null ? "config" : kalite);
    }

    private static void kare(Minecraft mc, IntegratedServer server, String ad) {
        String dosya = "yangin_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> {
        });
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, parçacık {}, kendi üretim {}, canlı sütun {}, küme {})", dosya,
                mc.getFps(), mc.particleEngine.countParticles(), IstemciYangin.uretileniAl(), IstemciYangin.canliSutun(),
                IstemciYangin.canliKume());
        server.execute(() -> gorunurlukYaz(server, ad));
    }
}
