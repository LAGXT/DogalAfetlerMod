package io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.AyCarpmasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import java.util.Locale;
import net.minecraft.client.Minecraft;
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
 * çalışır; üretimde etkisizdir. Gece, seyirci kamera doğuş noktasının {@value #KAMERA_YUKSEKLIGI} blok üstünde (ağaçlar
 * ve arazi ufku kapatmaz) yaklaşma yönüne (doğu-güneydoğu) bakar. Sıra: tepeye bakan önce karesi; vitrin açısından
 * afetsiz FPS tabanı ({@code s5_ay_taban}); ön-uyarı (değişiklik yok); siren boyunca ufkun 12° üstüne bakarak büyüyen ve
 * parçalanan ay; son saniyeler yavaş çekimde ({@code /dogalafet hiz 0.2}) vitrin açısından (ufkun 4° üstü: ay, parlama,
 * mantar sütunu ve ufuk aynı kadrajda); parlama, tam genlikte kısa sarsıntı dizisi (H.3), mantar sütunu; meteorlar
 * başlayınca en ağır an ölçülür ({@code s5_ay_yuksek}, taban {@code s5_ay_taban}); doğal bitiş, enkaz halkalı aysız gece
 * (halka karesinden {@value #KAYAN_ONCE} tick önce zorlanmış kayan yıldız: kendiliğinden ~3 dk'da bir doğar, ömrü 20 tick,
 * en parlak anı 10. tick), gündüz ve {@code sifirla} sonrası tepede geri gelen ay (önce / aysız / sonra aynı açıdan). Kareler
 * {@link GorselOtomasyon} A-34 kapısından alınır; evre saniyesine bağlı kare saniyesinde istenir, satır isteğin ve
 * alınışın saniyesini yazar. Zamana bağlı diziden (başlatma, siren) ve ölçümden önce FPS kapısı beklenir. Klavye, fare
 * ve tekerlek girdileri sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class AyCarpmasiOtomasyonu {
    private static final String OTOMASYON = "ay_carpmasi";
    private static final boolean AKTIF = OTOMASYON.equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "AY_OTOMASYON ";
    private static final float BAKIS_YAW = -90f + AyEgrileri.AZIMUT_DERECE;
    private static final int KAMERA_YUKSEKLIGI = 40;
    /** Minecraft pitch'inde yukarı negatiftir. */
    private static final float VITRIN_PITCH = -4f;
    private static final float YAKLASMA_PITCH = -12f;
    private static final float TEPE_PITCH = -88f;
    private static final int OLCUM_TICK = 100;
    /** H.3 dizisi: sarsıntı tam genlikteyken (3,5–10 sn) kareler, her biri öncekinin alınışından 2 tick sonra istenir. */
    private static final int SARSINTI_KARE = 5;
    private static final int SARSINTI_ARA = 2;
    private static final float SARSINTI_DIZI_SN = 5f;
    private static final int KAYAN_ONCE = 8;
    private static final int HALKA_KARESI = 80;

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
    private static int sarsintiNo;
    private static int sonSarsinti = -SARSINTI_ARA;
    private static int olcumBas = -1;
    private static boolean olcumBitti;
    private static boolean yavas;
    private static volatile boolean sunucuHazir;
    private static volatile BlockPos kamera;

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
        if (mc.player == null || mc.level == null || server == null || GorselOtomasyon.kareBekliyor()) {
            return;
        }
        sayac++;
        IstemciAy.Durum d = IstemciAy.durum(0f);
        switch (adim) {
            case -1 -> {
                if (sayac >= 100) {
                    GorselOtomasyon.cizimAyari();
                    server.execute(() -> hazirla(server));
                    sonraki();
                }
            }
            case 0 -> {
                if (sunucuHazir && sayac >= 200) {
                    mc.options.hideGui = true;
                    kare("00_once");
                    sonraki();
                }
            }
            case 1 -> {
                if (sayac == 1) {
                    server.execute(() -> bak(server, VITRIN_PITCH));
                } else if (sayac >= 40 && GorselOtomasyon.eylemKapisi(OTOMASYON, "s5_ay_taban ölçümü")) {
                    GorselOtomasyon.olcumBasla(OTOMASYON, "s5_ay_taban", null);
                    sonraki();
                }
            }
            case 2 -> {
                if (sayac >= OLCUM_TICK) {
                    GorselOtomasyon.olcumBitir();
                    server.execute(() -> bak(server, YAKLASMA_PITCH));
                    sonraki();
                }
            }
            case 3 -> {
                if (sayac >= 20 && GorselOtomasyon.eylemKapisi(OTOMASYON, "başlatma")) {
                    komut(server, "dogalafet start ay_carpmasi");
                    sonraki();
                }
            }
            case 4 -> {
                if (sayac == 60) {
                    kare("01_onuyari_degisiklik_yok");
                } else if (sayac >= 80 && GorselOtomasyon.eylemKapisi(OTOMASYON, "siren")) {
                    komut(server, "dogalafet asama ileri");
                    sonraki();
                }
            }
            case 5 -> {
                if (!yavas && d.yaklasiyor() && d.saniye() >= 48f) {
                    // Son yaklaşma ve vitrin saniyeleri yavaş çekimde: kare kapısı beklerken an kaçmasın.
                    komut(server, "dogalafet hiz 0.2");
                    server.execute(() -> bak(server, VITRIN_PITCH));
                    yavas = true;
                }
                if (d.yaklasiyor() && kareSirasi < YAKLASMA.length && d.saniye() >= YAKLASMA[kareSirasi].saniye()) {
                    kare(YAKLASMA[kareSirasi++].ad());
                } else if (d.carpti()) {
                    kareSirasi = 0;
                    sonraki();
                }
            }
            case 6 -> {
                if (kareSirasi == 2 && sarsintiNo < SARSINTI_KARE && d.saniye() >= SARSINTI_DIZI_SN
                        && sayac - sonSarsinti >= SARSINTI_ARA) {
                    sonSarsinti = sayac;
                    kare("sarsinti_" + ++sarsintiNo);
                } else if (kareSirasi < CARPMA.length && d.carpti() && d.saniye() >= CARPMA[kareSirasi].saniye()
                        && (kareSirasi != 2 || sarsintiNo == SARSINTI_KARE)) {
                    kare(CARPMA[kareSirasi++].ad());
                    if (kareSirasi == 2) {
                        komut(server, "dogalafet hiz 1");
                    }
                } else if (kareSirasi >= CARPMA.length && olcumBitti) {
                    komut(server, "dogalafet asama ileri");
                    komut(server, "dogalafet asama ileri");
                    sonraki();
                }
                if (!olcumBitti && d.carpti() && d.saniye() >= AyEgrileri.METEOR_BAS_SN) {
                    // En ağır an: meteorlar, büyüyen mantar sütunu, sönen ufuk ışığı ve sarsıntı birlikte.
                    if (olcumBas < 0) {
                        if (GorselOtomasyon.eylemKapisi(OTOMASYON, "s5_ay_yuksek ölçümü")) {
                            GorselOtomasyon.olcumBasla(OTOMASYON, "s5_ay_yuksek", "s5_ay_taban");
                            olcumBas = sayac;
                        }
                    } else if (sayac - olcumBas >= OLCUM_TICK) {
                        GorselOtomasyon.olcumBitir();
                        olcumBitti = true;
                    }
                }
            }
            case 7 -> {
                if (sayac == 20) {
                    server.execute(() -> bak(server, -55f));
                } else if (sayac == HALKA_KARESI - KAYAN_ONCE) {
                    AyCarpmasiGokyuzu.kayanYildizZorla(mc.level.random);
                    DogalAfetler.LOGGER.info(ON + "kayan yıldız zorlandı (halka karesinden {} tick önce)", KAYAN_ONCE);
                } else if (sayac == HALKA_KARESI) {
                    kare("14_halka_aysiz_gece");
                } else if (sayac == 100) {
                    server.execute(() -> bak(server, TEPE_PITCH));
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
                    server.execute(() -> bak(server, TEPE_PITCH));
                } else if (sayac == 340) {
                    kare("17_sifirla_ay_geri");
                    sonraki();
                }
            }
            case 8 -> {
                if (sayac >= 60) {
                    GorselOtomasyon.kareOzetiYaz(OTOMASYON);
                    DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
                    adim = 9;
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

    /** Sunucu thread'inde: kurallar, gece, doğuş noktasının üstünde tepeye bakan seyirci kamera. */
    private static void hazirla(IntegratedServer server) {
        for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 18000", "weather clear",
                "dogalafet sifirla"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), k);
        }
        ServerLevel level = server.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, dogus.getX(), dogus.getZ());
        kamera = new BlockPos(dogus.getX(), ust + KAMERA_YUKSEKLIGI, dogus.getZ());
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.setGameMode(GameType.SPECTATOR);
        bak(server, TEPE_PITCH);
        DogalAfetler.LOGGER.info(ON + "kamera {} (doğuş {}), yaw {}, vitrin pitch {}", kamera.toShortString(),
                dogus.toShortString(), BAKIS_YAW, VITRIN_PITCH);
        sunucuHazir = true;
    }

    private static void bak(IntegratedServer server, float pitch) {
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(server.overworld(), kamera.getX() + 0.5, kamera.getY(), kamera.getZ() + 0.5, BAKIS_YAW, pitch);
    }

    /** Kare isteği; satır kapı açılınca isteğin ve alınışın evre saniyesini, sarsıntı genliğini yazar. */
    private static void kare(String ad) {
        float istek = IstemciAy.durum(0f).saniye();
        GorselOtomasyon.kareIste(OTOMASYON, "ay_carpmasi_" + ad + ".png", k -> {
            Minecraft mc = Minecraft.getInstance();
            IstemciAy.Durum d = IstemciAy.durum(0f);
            double sarsinti = d.carpti() ? AyEgrileri.SARSINTI_DERECE * AyEgrileri.sarsinti(d.saniye())
                    * IstemciAy.ayar(AyCarpmasiAyarlari::ekranSarsintisiOlcegi) : 0;
            DogalAfetler.LOGGER.info(ON + "kare {} (ay {} sn {}, istek sn {}, FPS {}, sunucu TPS {} / {} ms, parçacık {}, "
                            + "kayan yıldız {}, sarsıntı {}°)", k.dosya(), d.ay(), sayi(d.saniye()), sayi(istek), k.fps(),
                    sayi(TpsOlcer.tps()), sayi(TpsOlcer.ortalamaMs()), mc.particleEngine.countParticles(),
                    AyCarpmasiGokyuzu.kayanSayisi(), String.format(Locale.ROOT, "%.2f", sarsinti));
        });
    }

    private static String sayi(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }
}
