package io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=ay_carpmasi} ile tek oyunculu dünyada
 * çalışır; üretimde etkisizdir. Gece, doğuş noktasının 14 blok üstünde seyirci kamera yaklaşma yönüne (doğu-güneydoğu)
 * ufkun 12° üstüne bakar: ön-uyarı (değişiklik yok), siren boyunca büyüyen ve parçalanan ay, son saniyeler yavaş çekimde
 * ({@code /dogalafet hiz 0.2}), parlama, mantar sütunu, sarsıntı, doğuş noktasına düşen ay parçaları, doğal bitiş, enkaz
 * halkalı aysız gece, gündüz ve {@code sifirla} sonrası geri gelen ay karelenir. Gizli workspace'te istemci ~1 FPS çizer:
 * kare istenen tick'te değil, sonraki çizilmiş karenin sonunda alınır. Klavye, fare ve tekerlek girdileri sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class AyCarpmasiOtomasyonu {
    private static final boolean AKTIF = "ay_carpmasi".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "AY_OTOMASYON ";
    private static final float BAKIS_YAW = -90f + AyEgrileri.AZIMUT_DERECE;

    /** Yaklaşmada (sirenin başından) ve çarpmadan sonra (çarpmadan) ölçekli saniyeyle alınan kareler. */
    private record Kare(String ad, float saniye) {
    }

    private static final Kare[] YAKLASMA = {new Kare("02_siren_5sn", 5f), new Kare("03_parcalanma_24sn", 24f),
            new Kare("04_catlaklar_38sn", 38f), new Kare("05_son_yaklasma_51sn", 51f), new Kare("06_vitrin_54sn", 54f),
            new Kare("07_vitrin_58sn", 58f)};
    private static final Kare[] CARPMA = {new Kare("08_parlama", 0.15f), new Kare("09_mantar_baslangic_4sn", 4f),
            new Kare("10_sarsinti_8sn", 8f), new Kare("11_mantar_meteor_20sn", 20f),
            new Kare("12_mantar_tepe_47sn", 47f), new Kare("13_toz_kalintisi_90sn", 90f)};

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static int kareSirasi;
    private static boolean yavas;
    private static volatile boolean sunucuHazir;
    private static volatile BlockPos kamera;
    private static String istenenKare;

    private AyCarpmasiOtomasyonu() {
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
        if (mc.player == null || mc.level == null || server == null || istenenKare != null) {
            return;
        }
        sayac++;
        IstemciAy.Durum d = IstemciAy.durum(0f);
        switch (adim) {
            case -1 -> {
                if (sayac >= 100) {
                    server.execute(() -> hazirla(server));
                    sonraki();
                }
            }
            case 0 -> {
                if (sunucuHazir && sayac >= 200) {
                    mc.options.hideGui = true;
                    kare("00_once");
                    komut(server, "dogalafet start ay_carpmasi");
                    sonraki();
                }
            }
            case 1 -> {
                if (sayac == 60) {
                    kare("01_onuyari_degisiklik_yok");
                } else if (sayac == 80) {
                    komut(server, "dogalafet asama ileri");
                    sonraki();
                }
            }
            case 2 -> {
                if (!yavas && d.yaklasiyor() && d.saniye() >= 48f) {
                    // Son yaklaşma ve vitrin saniyeleri yavaş çekimde (gizli workspace ~1 FPS çizer).
                    komut(server, "dogalafet hiz 0.2");
                    yavas = true;
                }
                if (d.yaklasiyor() && kareSirasi < YAKLASMA.length && d.saniye() >= YAKLASMA[kareSirasi].saniye()) {
                    kare(YAKLASMA[kareSirasi++].ad());
                } else if (d.carpti()) {
                    kareSirasi = 0;
                    sonraki();
                }
            }
            case 3 -> {
                if (kareSirasi < CARPMA.length && d.carpti() && d.saniye() >= CARPMA[kareSirasi].saniye()) {
                    kare(CARPMA[kareSirasi++].ad());
                    if (kareSirasi == 2) {
                        komut(server, "dogalafet hiz 1");
                    }
                } else if (kareSirasi >= CARPMA.length) {
                    komut(server, "dogalafet asama ileri");
                    komut(server, "dogalafet asama ileri");
                    sonraki();
                }
            }
            case 4 -> {
                if (sayac == 20) {
                    server.execute(() -> bak(server, -55f));
                } else if (sayac == 80) {
                    kare("14_halka_aysiz_gece");
                } else if (sayac == 100) {
                    server.execute(() -> bak(server, -88f));
                } else if (sayac == 160) {
                    kare("15_aysiz_gece_tepe");
                } else if (sayac == 180) {
                    komut(server, "time set 6000");
                    server.execute(() -> bak(server, -30f));
                } else if (sayac == 240) {
                    kare("16_gunduz_halka");
                } else if (sayac == 260) {
                    komut(server, "dogalafet aysonrasi atla");
                    komut(server, "dogalafet asama ileri");
                    komut(server, "dogalafet sifirla");
                    komut(server, "time set 18000");
                    server.execute(() -> bak(server, -12f));
                } else if (sayac == 340) {
                    kare("17_sifirla_ay_geri");
                    sonraki();
                }
            }
            case 5 -> {
                if (sayac >= 60) {
                    DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
                    adim = 6;
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

    private static void komut(IntegratedServer server, String komut) {
        server.execute(() -> {
            int sonuc = server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), komut);
            DogalAfetler.LOGGER.info(ON + "/{} → {}", komut, sonuc);
        });
    }

    /** Sunucu thread'inde: kurallar, gece, doğuş noktasının üstünde seyirci kamera. */
    private static void hazirla(IntegratedServer server) {
        for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 18000", "weather clear",
                "dogalafet sifirla"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), k);
        }
        ServerLevel level = server.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, dogus.getX(), dogus.getZ());
        kamera = new BlockPos(dogus.getX(), ust + 14, dogus.getZ());
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.setGameMode(GameType.SPECTATOR);
        bak(server, -12f);
        DogalAfetler.LOGGER.info(ON + "kamera {} (doğuş {}), yaw {}", kamera.toShortString(), dogus.toShortString(),
                BAKIS_YAW);
        sunucuHazir = true;
    }

    private static void bak(IntegratedServer server, float pitch) {
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(server.overworld(), kamera.getX() + 0.5, kamera.getY(), kamera.getZ() + 0.5, BAKIS_YAW, pitch);
    }

    private static void kare(String ad) {
        istenenKare = ad;
    }

    @SubscribeEvent
    public static void cizimSonu(TickEvent.RenderTickEvent olay) {
        if (!AKTIF || olay.phase != TickEvent.Phase.END || istenenKare == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        String dosya = "ay_carpmasi_" + istenenKare + ".png";
        istenenKare = null;
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> {
        });
        IstemciAy.Durum d = IstemciAy.durum(0f);
        DogalAfetler.LOGGER.info(ON + "kare {} (ay {} sn {}, FPS {}, sunucu TPS {} / {} ms, parçacık {}, kayan yıldız {})",
                dosya, d.ay(), String.format(Locale.ROOT, "%.1f", d.saniye()), mc.getFps(),
                String.format(Locale.ROOT, "%.1f", TpsOlcer.tps()), String.format(Locale.ROOT, "%.1f", TpsOlcer.ortalamaMs()),
                mc.particleEngine.countParticles(), AyCarpmasiGokyuzu.kayanSayisi());
    }
}
