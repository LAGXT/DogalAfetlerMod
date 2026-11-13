package io.github.kadirberkpolat1.dogalafetler.client.ay_carpmasi;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.AyCarpmasiAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyEgrileri;
import io.github.kadirberkpolat1.dogalafetler.afet.ay_carpmasi.mantik.AyYonu;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.uyum.IrisUyumu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.AyDurumu;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
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
 * ve arazi ufku kapatmaz) yaklaşma yönüne (olayın başladığı anda vanilla ayın bulunduğu yer) bakar. Sıra: tepeye bakan önce karesi; vitrin açısından
 * afetsiz FPS tabanı ({@code s5_ay_taban}); ön-uyarı (değişiklik yok); siren boyunca ufkun 12° üstüne bakarak büyüyen ve
 * parçalanan ay; son saniyeler ağır çekimde (sunucu tick'inde 20. sn'den hız 0,1 = {@code /dogalafet hiz} alt sınırı) vitrin
 * açısından (ufkun 4° üstü: ay, parlama ve patlama aynı kadrajda); 3 boyutlu patlama 0,3 / 1 / 2 sn, 3. sn'de kamera
 * {@value #YUKSELTME} blok yükseltilmiş (patlama dünyada sabit: kadrajda aşağı kayar), 8. sn'de patlama yok (tam genlikte kısa
 * sarsıntı dizisinden sonra, H.3). Yaklaşma, patlama ve sarsıntı karelerinde istemcinin ay saati kare alınana dek karenin
 * saniyesinde sabit tutulur ({@link IstemciAy#sabitle}; pc2'de FPS &lt; 15 iken kare kapısı 60 sn bekler); satır sabit ve
 * canlı saniyeyi yazar. Meteorlar
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
    /**
     * Sahne saati: vanilla ay doğuda ufkun ~35° üstünde (gün zamanı 14880; gün döngüsü kapalı). Yaklaşma yönü ayın bu andaki yeridir
     * (K-E83); kamera onun azimutuna bakar. Gölgelendirici paketi açıkken paketin güneş yolu eğimi azimutu kaydırır.
     */
    private static final int BASLANGIC_ZAMANI = 14880;
    private static volatile float bakisYaw = -90f + AyEgrileri.AZIMUT_DERECE;
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
    private record Kare(String ad, float saniye, int yukari) {
        Kare(String ad, float saniye) {
            this(ad, saniye, 0);
        }
    }

    /** Patlamanın dünyaya bağlı olduğunu gösteren karede kameranın yükseltilmesi (blok). */
    private static final int YUKSELTME = 80;
    private static final Kare[] YAKLASMA = {new Kare("02_siren_2sn", 2f), new Kare("03_parcalanma_10sn", 10f),
            new Kare("04_catlaklar_16sn", 16f), new Kare("05_son_yaklasma_21sn", 21.5f), new Kare("06_vitrin_22sn", 22.5f),
            new Kare("07_vitrin_24sn", 24.2f)};
    /** Patlama anları (0,3 / 1 / 2 / 8 sn) ve yükseltilmiş kamera; {@value #SARSINTI_SIRASI}. kareden önce sarsıntı dizisi. */
    private static final Kare[] CARPMA = {new Kare("08_patlama_0sn", 0.3f), new Kare("08b_patlama_1sn", 1f),
            new Kare("09_patlama_2sn", 2f), new Kare("09b_patlama_3sn_yukari", 3f, YUKSELTME),
            new Kare("10_patlama_8sn_yok", 8f), new Kare("11_meteor_20sn", 20f), new Kare("12_halka_47sn", 47f),
            new Kare("13_gok_90sn", 90f)};
    private static final int SARSINTI_SIRASI = 4;
    private static volatile int yukseltme;

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
    /** Ağır çekim {yaklaşma saniyesi, hız}; 0,1 {@code /dogalafet hiz} alt sınırıdır (daha küçüğü 0,1'e kenetlenir). */
    private static final float[][] YAVASLATMA = {{20f, 0.1f}};
    private static volatile int yavaslatma;
    private static volatile boolean sunucuHazir;
    private static volatile BlockPos kamera;

    private AyCarpmasiOtomasyonu() {
    }

    /**
     * Sunucu thread'inde: ağır çekim yaklaşma saniyesinden kurulur, istemci kare kapısında beklese de gecikmez. Patlamanın
     * 3. sn karesinden sonra istemci hızı 1'e döndürür.
     */
    @SubscribeEvent
    public static void sunucuTick(TickEvent.ServerTickEvent olay) {
        if (!AKTIF || olay.phase != TickEvent.Phase.END || !sunucuHazir || yavaslatma >= YAVASLATMA.length) {
            return;
        }
        MinecraftServer server = olay.getServer();
        DunyaDonemi d = DunyaDonemi.al(server);
        if (d.ayDurumu() != AyDurumu.YAKLASIYOR || d.ayOlaySaati() < 0) {
            return;
        }
        float s = (d.saat() - d.ayOlaySaati()) / 20f;
        if (s >= YAVASLATMA[yavaslatma][0]) {
            float hiz = YAVASLATMA[yavaslatma][1];
            AfetYoneticisi.al(server).hizAyarla(hiz);
            if (yavaslatma == 0) {
                bak(server, VITRIN_PITCH);
            }
            DogalAfetler.LOGGER.info(ON + "ağır çekim x{} (yaklaşma sn {})", hiz, sayi(s));
            yavaslatma++;
        }
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
                    bakisYaw = -90f + AyYonu.baslangic(mc.level.getTimeOfDay(1f), IrisUyumu.gokEgimiDerece()).azimutDerece();
                    DogalAfetler.LOGGER.info(ON + "yaklaşma yönü (vanilla ay): yaw {}", bakisYaw);
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
                if (d.yaklasiyor() && kareSirasi < YAKLASMA.length && d.saniye() >= YAKLASMA[kareSirasi].saniye()) {
                    Kare k = YAKLASMA[kareSirasi++];
                    kare(k.ad(), new IstemciAy.Durum(AyDurumu.YAKLASIYOR, k.saniye()));
                } else if (d.carpti()) {
                    kareSirasi = 0;
                    sonraki();
                }
            }
            case 6 -> {
                Kare hedef = kareSirasi < CARPMA.length ? CARPMA[kareSirasi] : null;
                int istenen = hedef == null ? 0 : hedef.yukari();
                if (istenen != yukseltme && (istenen == 0 || d.carpti() && d.saniye() >= hedef.saniye() - 0.5f)) {
                    yukseltme = istenen;
                    server.execute(() -> bak(server, VITRIN_PITCH));
                    DogalAfetler.LOGGER.info(ON + "kamera yüksekliği +{} blok (patlama sn {})", istenen, sayi(d.saniye()));
                } else if (kareSirasi == SARSINTI_SIRASI && sarsintiNo < SARSINTI_KARE && d.saniye() >= SARSINTI_DIZI_SN
                        && sayac - sonSarsinti >= SARSINTI_ARA) {
                    sonSarsinti = sayac;
                    kare("sarsinti_" + ++sarsintiNo, d);
                } else if (hedef != null && d.carpti() && d.saniye() >= hedef.saniye() && yukseltme == istenen
                        && (kareSirasi != SARSINTI_SIRASI || sarsintiNo == SARSINTI_KARE)) {
                    kare(hedef.ad(), new IstemciAy.Durum(AyDurumu.YOK, hedef.saniye()));
                    kareSirasi++;
                    if (kareSirasi == SARSINTI_SIRASI) {
                        komut(server, "dogalafet hiz 1");
                    }
                } else if (kareSirasi >= CARPMA.length && olcumBitti) {
                    komut(server, "dogalafet asama ileri");
                    komut(server, "dogalafet asama ileri");
                    sonraki();
                }
                if (!olcumBitti && d.carpti() && d.saniye() >= AyEgrileri.METEOR_BAS_SN) {
                    // En ağır an: meteorlar, enkaz halkasının belirmesi ve sarsıntı birlikte.
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
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set " + BASLANGIC_ZAMANI, "weather clear",
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
        DogalAfetler.LOGGER.info(ON + "kamera {} (doğus {}), yaw {}, vitrin pitch {}", kamera.toShortString(),
                dogus.toShortString(), bakisYaw, VITRIN_PITCH);
        sunucuHazir = true;
    }

    private static void bak(MinecraftServer server, float pitch) {
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(server.overworld(), kamera.getX() + 0.5, kamera.getY() + yukseltme, kamera.getZ() + 0.5, bakisYaw,
                pitch);
    }

    /**
     * Kare isteği; ay saati kare alınana dek {@code sabit}'te tutulur. Satır kapı açılınca sabit ve canlı saniyeyi, sarsıntı
     * genliğini yazar.
     */
    private static void kare(String ad, IstemciAy.Durum sabit) {
        IstemciAy.sabitle(sabit);
        GorselOtomasyon.kareIste(OTOMASYON, "ay_carpmasi_" + ad + ".png", k -> {
            Minecraft mc = Minecraft.getInstance();
            IstemciAy.Durum d = IstemciAy.durum(0f);
            IstemciAy.Durum canli = IstemciAy.canli(0f);
            IstemciAy.sabitle(null);
            double sarsinti = d.carpti() ? AyEgrileri.SARSINTI_DERECE * AyEgrileri.sarsinti(d.saniye())
                    * IstemciAy.ayar(AyCarpmasiAyarlari::ekranSarsintisiOlcegi) : 0;
            float parlama = d.carpti() ? AyEgrileri.dunyaParlamasi(d.saniye()) : 0f;
            DogalAfetler.LOGGER.info(ON + "kare {} (ay {} sn {} sabit, canlı {} sn {}, FPS {}, sunucu TPS {} / {} ms, "
                            + "parçacık {}, kayan yıldız {}, sarsıntı {}°, dünya parlaması {})", k.dosya(), d.ay(),
                    sayi(d.saniye()), canli.ay(), sayi(canli.saniye()), k.fps(), sayi(TpsOlcer.tps()),
                    sayi(TpsOlcer.ortalamaMs()), mc.particleEngine.countParticles(), AyCarpmasiGokyuzu.kayanSayisi(),
                    String.format(Locale.ROOT, "%.2f", sarsinti), String.format(Locale.ROOT, "%.2f", parlama));
        });
    }

    /** Ay saati sabitlenmeden kare isteği (ay durumundan bağımsız kareler). */
    private static void kare(String ad) {
        kare(ad, null);
    }

    private static String sayi(double d) {
        return String.format(Locale.ROOT, "%.1f", d);
    }
}
