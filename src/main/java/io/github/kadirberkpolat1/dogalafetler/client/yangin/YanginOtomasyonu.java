package io.github.kadirberkpolat1.dogalafetler.client.yangin;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=yangin} ile tek oyunculu dünyada çalışır;
 * üretimde etkisizdir. Entegre sunucuda en yakın ormanı bulur, alacakaranlığa alır, seyirci kamerayı yangın cephesinin
 * 36 blok yanına koyar, ağaç tepelerinde bir cephe tutuşturur; Yüksek ve Düşük kalite karelerini, ardından yağmurla sönme
 * karelerini alır, yangını söndürüp kapanır. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class YanginOtomasyonu {
    private static final boolean AKTIF = "yangin".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "YANGIN_OTOMASYON ";
    private static final int KAMERA_UZAKLIGI = 36;
    /** Eski kademenin parçacıkları en çok 280 tick yaşar; kare yalnız yeni kademeyi gösterir. */
    private static final int KALITE_BEKLEMESI = 300;

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static volatile boolean sunucuHazir;
    private static volatile BlockPos odak;

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
                    server.execute(() -> tutustur(server));
                    sonraki();
                }
            }
            case 1 -> {
                if (sayac == 350) {
                    komut(server, "forge tps");
                    komut(server, "dogalafet yangin bilgi");
                    kaliteyle(EfektKalitesi.YUKSEK);
                } else if (sayac == 350 + KALITE_BEKLEMESI) {
                    kare(mc, "vitrin_yuksek");
                    kaliteyle(EfektKalitesi.DUSUK);
                } else if (sayac == 350 + 2 * KALITE_BEKLEMESI) {
                    kare(mc, "vitrin_dusuk");
                    komut(server, "dogalafet yangin bilgi");
                    kaliteyle(null);
                    sonraki();
                }
            }
            case 2 -> {
                if (sayac == 60) {
                    server.execute(() -> {
                        server.overworld().setWeatherParameters(0, 6000, true, false);
                        DogalAfetler.LOGGER.info(ON + "yağmur başladı; aktif {}", YanginYoneticisi.aktifSayi(server.overworld()));
                    });
                } else if (sayac == 100) {
                    kare(mc, "yagmur");
                    komut(server, "dogalafet yangin bilgi");
                } else if (sayac == 300) {
                    kare(mc, "yagmur_sonra");
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

    /** Sunucu thread'inde: kurallar, alacakaranlık, en yakın orman, seyirci kamera. */
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
        oyuncu.teleportTo(level, merkez.getX() - KAMERA_UZAKLIGI + 0.5, 150, merkez.getZ() + 0.5, -90.0F, 10.0F);
        sunucuHazir = true;
    }

    /** Sunucu thread'inde: ağaç tepelerinde 5 odaklı cephe, kamera cepheyi yandan görür. */
    private static void tutustur(IntegratedServer server) {
        ServerLevel level = server.overworld();
        BlockPos m = odak;
        int toplam = 0;
        int tepe = level.getMinBuildHeight();
        for (int dz = -12; dz <= 12; dz += 6) {
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, m.getX(), m.getZ() + dz) - 1;
            tepe = Math.max(tepe, y);
            toplam += YanginYoneticisi.alanTutustur(level, new BlockPos(m.getX(), y, m.getZ() + dz), 4,
                    YanginKaynagi.KOMUT, 0.9);
        }
        int kameraY = tepe;
        for (int dx = 0; dx <= KAMERA_UZAKLIGI; dx += 4) {
            kameraY = Math.max(kameraY, level.getHeight(Heightmap.Types.MOTION_BLOCKING, m.getX() - dx, m.getZ()));
        }
        kameraY += 8;
        float egim = (float) Math.toDegrees(Math.atan2(kameraY - (tepe + 6.0), KAMERA_UZAKLIGI));
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(level, m.getX() - KAMERA_UZAKLIGI + 0.5, kameraY, m.getZ() + 0.5, -90.0F, egim);
        DogalAfetler.LOGGER.info(ON + "tutuşan {} hücre; cephe tepesi y={}, kamera y={} eğim {}", toplam, tepe, kameraY,
                egim);
    }

    private static void komut(IntegratedServer server, String komut) {
        server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), komut));
    }

    private static void kaliteyle(EfektKalitesi kalite) {
        IstemciYangin.kaliteZorla(kalite);
        DogalAfetler.LOGGER.info(ON + "kalite {}", kalite == null ? "config" : kalite);
    }

    private static void kare(Minecraft mc, String ad) {
        String dosya = "yangin_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> {
        });
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, parçacık {})", dosya, mc.getFps(), mc.particleEngine.countParticles());
    }
}
