package io.github.kadirberkpolat1.dogalafetler.client.yangin;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKayitlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
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
 * bir taç cephesi tutuşturur ve seyirci kamerayı cephenin rüzgâr üstüne, ağaç tepelerinin üstüne koyar (tasarım notu §3).
 * Yüksek ve Düşük kalite karelerini, ardından yağmurla sönme karelerini alır; her karede kameradan görünen ateş bloklarını
 * sayar, yangını söndürüp kapanır. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class YanginOtomasyonu {
    private static final boolean AKTIF = "yangin".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "YANGIN_OTOMASYON ";
    /** Cephe ilk karelerde LOD mesafesi (48) içinde kalsın: hücre alevleri kalite kademesine göre çizilir. */
    private static final int KAMERA_UZAKLIGI = 30;
    private static final int KAMERA_YUKSEKLIGI = 12;
    /** Tasarım notu §6 seviye 4: taç yangını ve 8 blok sıçrama. */
    private static final double VITRIN_YOGUNLUGU = 0.7;
    /** Her kare, parçacıklar silindikten bu kadar tick sonra: iki kalite karesi aynı parçacık yaşında, 6 sn arayla. */
    private static final int KARE_ARALIGI = 120;
    private static final int TARAMA = 64;

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static volatile boolean sunucuHazir;
    private static volatile BlockPos odak;
    private static volatile int cepheTepesi;

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
        sayac++;
        if ((adim == 1 || adim == 2) && sayac % 100 == 0) {
            int a = adim;
            int s = sayac;
            server.execute(() -> DogalAfetler.LOGGER.info(ON + "adım {} tick {}: aktif {}", a, s,
                    YanginYoneticisi.aktifSayi(server.overworld())));
        }
        double enBoy = mc.getWindow().getWidth() / (double) Math.max(1, mc.getWindow().getHeight());
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
                if (sayac == KARE_ARALIGI) {
                    komut(server, "forge tps");
                    komut(server, "dogalafet yangin bilgi");
                    kare(mc, server, "vitrin_yuksek", enBoy);
                    kaliteyle(mc, EfektKalitesi.DUSUK);
                } else if (sayac == 2 * KARE_ARALIGI) {
                    kare(mc, server, "vitrin_dusuk", enBoy);
                    komut(server, "dogalafet yangin bilgi");
                    kaliteyle(mc, null);
                    sonraki();
                }
            }
            case 2 -> {
                if (sayac == 20) {
                    server.execute(() -> {
                        server.overworld().setWeatherParameters(0, 6000, true, false);
                        DogalAfetler.LOGGER.info(ON + "yağmur başladı; aktif {}", YanginYoneticisi.aktifSayi(server.overworld()));
                    });
                } else if (sayac == 80) {
                    kare(mc, server, "yagmur", enBoy);
                    komut(server, "dogalafet yangin bilgi");
                } else if (sayac == 320) {
                    kare(mc, server, "yagmur_sonra", enBoy);
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

    /**
     * Sunucu thread'inde: ormanın en sık bölümünde günün rüzgârına dik bir taç cephesi; kamera rüzgâr üstünde, ağaç
     * tepelerinin {@link #KAMERA_YUKSEKLIGI} blok üstünde, cepheye bakar (yangın kameradan uzağa ilerler).
     */
    private static void tutustur(IntegratedServer server) {
        ServerLevel level = server.overworld();
        BlockPos m = enSikOrman(level, odak);
        odak = m;
        double[] ruzgar = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
        double guc = Math.hypot(ruzgar[0], ruzgar[1]);
        double yx = ruzgar[0] / guc;
        double yz = ruzgar[1] / guc;
        int toplam = 0;
        int tepe = level.getMinBuildHeight();
        for (int k = -16; k <= 16; k += 4) {
            BlockPos ust = ustBlok(level, m.getX() + (int) Math.round(-yz * k), m.getZ() + (int) Math.round(yx * k));
            if (ust != null && level.getBlockState(ust).is(BlockTags.LEAVES)) {
                tepe = Math.max(tepe, ust.getY());
                toplam += YanginYoneticisi.alanTutustur(level, ust, 3, YanginKaynagi.KOMUT, VITRIN_YOGUNLUGU);
            }
        }
        cepheTepesi = tepe;
        double kx = m.getX() + 0.5 - yx * KAMERA_UZAKLIGI;
        double kz = m.getZ() + 0.5 - yz * KAMERA_UZAKLIGI;
        int zemin = tepe;
        for (int i = 0; i <= KAMERA_UZAKLIGI; i += 2) {
            zemin = Math.max(zemin, level.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(kx + yx * i),
                    (int) Math.floor(kz + yz * i)));
        }
        double ky = zemin + KAMERA_YUKSEKLIGI;
        float yon = (float) Math.toDegrees(Math.atan2(-yx, yz));
        float egim = (float) Math.toDegrees(Math.atan2(ky - (tepe - 2.0), KAMERA_UZAKLIGI));
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(level, kx, ky, kz, yon, egim);
        DogalAfetler.LOGGER.info(ON + "rüzgâr ({}, {}); cephe {} hücre, tepe y={}; kamera ({}, {}, {}) yön {} eğim {}",
                String.format(Locale.ROOT, "%.2f", ruzgar[0]), String.format(Locale.ROOT, "%.2f", ruzgar[1]),
                toplam, tepe, (int) kx, (int) ky, (int) kz, String.format(Locale.ROOT, "%.1f", yon),
                String.format(Locale.ROOT, "%.1f", egim));
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

    /**
     * Sunucu thread'inde: cephe çevresindeki ateş bloklarından kaçı kameranın görüş alanında (dikey 70°, pencere en/boy
     * oranı) ve kameradan açık görüş hattında (yaprak dahil katı blok araya girmiyor). Kare kanıtının sayısal eşi; ateş
     * blokları biyomlarına göre de sayılır (yağışsız biyomda yağmur yangını söndürmez).
     */
    private static void gorunurlukYaz(IntegratedServer server, String ad, double enBoy) {
        ServerLevel level = server.overworld();
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        Vec3 goz = oyuncu.getEyePosition();
        Vec3 ileri = oyuncu.getViewVector(1.0F);
        Vec3 sag = ileri.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 yukari = sag.cross(ileri);
        double dikey = Math.tan(Math.toRadians(35));
        double yatay = dikey * enBoy;
        BlockPos m = odak;
        int toplam = 0;
        int cercevede = 0;
        int gorunen = 0;
        double uzaklik = 0;
        Map<String, Integer> biyomlar = new TreeMap<>();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = -TARAMA; x <= TARAMA; x++) {
            for (int z = -TARAMA; z <= TARAMA; z++) {
                for (int y = cepheTepesi - 24; y <= cepheTepesi + 8; y++) {
                    p.set(m.getX() + x, y, m.getZ() + z);
                    if (!level.isLoaded(p) || !level.getBlockState(p).is(YanginKayitlari.AFET_ATESI.get())) {
                        continue;
                    }
                    toplam++;
                    Holder<Biome> biyom = level.getBiome(p);
                    biyomlar.merge(biyom.unwrapKey().map(k -> k.location().getPath()).orElse("?")
                            + (biyom.value().hasPrecipitation() ? "" : "(yağışsız)"), 1, Integer::sum);
                    Vec3 hedef = new Vec3(p.getX() + 0.5, p.getY() + 0.3, p.getZ() + 0.5);
                    Vec3 v = hedef.subtract(goz);
                    double ileriBilesen = v.dot(ileri);
                    if (ileriBilesen <= 0 || Math.abs(v.dot(sag) / ileriBilesen) > yatay
                            || Math.abs(v.dot(yukari) / ileriBilesen) > dikey) {
                        continue;
                    }
                    cercevede++;
                    HitResult carpma = level.clip(new ClipContext(goz, hedef, ClipContext.Block.COLLIDER,
                            ClipContext.Fluid.NONE, oyuncu));
                    if (carpma.getType() == HitResult.Type.MISS
                            || carpma.getLocation().distanceTo(goz) >= v.length() - 1.0) {
                        gorunen++;
                        uzaklik += v.length();
                    }
                }
            }
        }
        String ortalama = gorunen == 0 ? "-" : String.format(Locale.ROOT, "%.1f", uzaklik / gorunen);
        DogalAfetler.LOGGER.info(ON + "{}: görünen ateş bloğu {} (çerçevede {}, taranan {}), ortalama uzaklık {} blok, "
                + "aktif {}; biyomlar {}", ad, gorunen, cercevede, toplam, ortalama, YanginYoneticisi.aktifSayi(level),
                biyomlar);
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

    private static void kare(Minecraft mc, IntegratedServer server, String ad, double enBoy) {
        String dosya = "yangin_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> {
        });
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, parçacık {}, kendi üretim {})", dosya, mc.getFps(),
                mc.particleEngine.countParticles(), IstemciYangin.uretileniAl());
        server.execute(() -> gorunurlukYaz(server, ad, enBoy));
    }
}
