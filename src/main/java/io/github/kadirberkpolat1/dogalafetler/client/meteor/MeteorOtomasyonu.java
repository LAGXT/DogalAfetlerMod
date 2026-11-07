package io.github.kadirberkpolat1.dogalafetler.client.meteor;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.esya.MeteorEsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.termostat.Termostat;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAyarlari;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorBoyutu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorGorunumu;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorOdulleri;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorVarligi;
import io.github.kadirberkpolat1.dogalafetler.meteor.mantik.MeteorOlcegi;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.ZirhEkleri;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=meteor} ile tek oyunculu dünyada çalışır;
 * üretimde etkisizdir. Alacakaranlıkta, en yakın ormanın 48 blok batısında ağaçların üstünde seyirci kamera doğuya bakar; sağdan
 * gelen kademeli ay (mavi-beyaz) ve güneş meteorlarının izleri, çarpma anı, krater ve ateş karelenir; sonra yakın kamerayla
 * parıldayan Meteorit Parçası ve Kozmik Çekirdek, en sonda Kozmik Çekirdek ve Termostatlı zırh ipuçları. Klavye, fare ve
 * tekerlek girdileri kapanışa kadar sayılır. Kareler A-34 kapısından alınır ({@link GorselOtomasyon#kareIste}); bekleyen kare
 * varken adım ilerlemez. Vitrinde meteorlar yavaş çekimde düşer ({@code meteor.ucusSuresi} {@value #YAVAS_CEKIM}; sonra eski
 * değere döner).
 * Ölçüm (K-E29 (b)): vitrin kamerasında meteorlardan önce afetsiz taban {@code s5_meteor_taban}, ilk atıştan itibaren yağmur
 * {@code s5_meteor_yuksek} (her biri 100 tick; öncesinde FPS kapısı, {@link GorselOtomasyon#akici}).
 * {@code -PotomasyonBolum=odul} (M-22): yalnız ödül ve ipucu kareleri, öğlen ışığında; her ipucu kendi karesinde, satırlar
 * ekran genişliğine göre sarılır (tooltip'ler üst üste binmez). {@code -PotomasyonBolum=yagmur} (meteor-2): aynı kamerada
 * gerçek güneş patlamasının ön-uyarı, siren ve AKTIF başı (90/dk, karma boyut) yağmuru, yakın/uzak çarpma ses denemesi ve
 * ay parçası yağmuru karelenir; ses kararları {@code METEOR_SES}, uçan meteorun bantları {@code sayım} satırlarıdır
 * ({@link MeteorSesi}).
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class MeteorOtomasyonu {
    private static final boolean AKTIF = "meteor".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "METEOR_OTOMASYON ";
    private static final boolean YALNIZ_ODUL = "odul".equals(System.getProperty("dogalafetler.otomasyon.bolum", "hepsi"));
    private static final boolean YAGMUR = "yagmur".equals(System.getProperty("dogalafetler.otomasyon.bolum", "hepsi"));
    /** Yağmur bölümünün adımı ({@link #adim}). */
    private static final int YAGMUR_ADIMI = 9;
    private static final String[] IPUCLARI = {"cekirdek", "parca", "plaka", "zirh"};
    private static final int UZAKLIK = 48;
    private static final int KAMERA_YUKSEKLIGI = 18;
    /** Yavaş çekimin uçuş süresi (tick): varsayılan 180'in 2 katı. */
    private static final int YAVAS_CEKIM = 360;

    /** Hedef ofseti (kameradan bakış yönünde x, sağa z), boyut, görünüm, açı, gelış yönü, başlangıç tick'i. */
    private record Atis(int dx, int dz, MeteorBoyutu boyut, MeteorGorunumu gorunum, double aci, double azimut, int tick) {
    }

    private static final List<Atis> ATISLAR = List.of(
            new Atis(0, 0, MeteorBoyutu.BUYUK, MeteorGorunumu.AY, 55, Math.PI / 2 - 0.35, 0),
            new Atis(10, -14, MeteorBoyutu.KUCUK, MeteorGorunumu.AY, 50, Math.PI / 2 - 0.15, 5),
            new Atis(-6, 12, MeteorBoyutu.KUCUK, MeteorGorunumu.GUNES, 60, Math.PI / 2 - 0.5, 9),
            new Atis(16, 8, MeteorBoyutu.BUYUK, MeteorGorunumu.AY, 48, Math.PI / 2 - 0.25, 14));

    /** Ses denemesi: oyuncudan yatay ofset ve boyut (19 blok küçük: duyulur; 150 büyük ve 112 küçük: patlama menzili dışı). */
    private record SesAtisi(int dx, int dz, MeteorBoyutu boyut) {
    }

    private static final List<SesAtisi> SES_ATISLARI = List.of(new SesAtisi(18, 6, MeteorBoyutu.KUCUK),
            new SesAtisi(150, 0, MeteorBoyutu.BUYUK), new SesAtisi(-104, 42, MeteorBoyutu.KUCUK));

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static int ilkCarpma = -1;
    private static int carpmaBaslangic;
    private static volatile boolean sunucuHazir;
    private static volatile BlockPos odak;
    private static volatile int eskiSure = -1;
    /** Vitrin öncesi: 0 FPS kapısı, 1 taban ölçümü, 2 FPS kapısı, 3 vitrin. */
    private static int tabanEvresi;
    private static int carpmaSonrasiKare;
    private static int kraterAdimi;
    private static int ipucu;

    private MeteorOtomasyonu() {
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
        if (GorselOtomasyon.kareBekliyor()) {
            return;
        }
        sayac++;
        switch (adim) {
            case -1 -> {
                if (sayac >= 100) {
                    DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}", mc.player.getGameProfile().getName());
                    GorselOtomasyon.cizimAyari();
                    server.execute(() -> hazirla(server));
                    sonraki();
                }
            }
            case 0 -> {
                if (sunucuHazir && sayac >= 240) {
                    mc.options.hideGui = true;
                    carpmaBaslangic = IstemciMeteor.carpmaSayisi();
                    sonraki();
                    if (YALNIZ_ODUL) {
                        sonraki();
                    } else if (YAGMUR) {
                        adim = YAGMUR_ADIMI;
                    }
                }
            }
            case 1 -> {
                if (tabanEvresi < 3) {
                    taban();
                } else {
                    vitrin(mc, server);
                }
            }
            case 2 -> {
                if (sayac == 1) {
                    server.execute(() -> oduller(server));
                } else if (sayac == 80) {
                    kare("odul_yakin");
                } else if (sayac >= 110) {
                    kare("odul_yakin_b");
                    sonraki();
                }
            }
            case 3 -> {
                if (ipucu < IPUCLARI.length && sayac == 1) {
                    mc.setScreen(new IpucuEkrani(ipucuYigini(ipucu)));
                } else if (ipucu < IPUCLARI.length && sayac == 30) {
                    kare("ipucu_" + IPUCLARI[ipucu]);
                } else if (ipucu < IPUCLARI.length && sayac > 30) {
                    ipucu++;
                    sayac = 0;
                } else if (ipucu >= IPUCLARI.length) {
                    mc.setScreen(null);
                    server.execute(() -> {
                        int sonen = YanginYoneticisi.sondur(server.overworld(), null);
                        DogalAfetler.LOGGER.info(ON + "bitti; {} yangın hücresi söndürüldü", sonen);
                    });
                    sonraki();
                }
            }
            case 4 -> {
                if (sayac >= 60) {
                    GorselOtomasyon.kareOzetiYaz("meteor");
                    DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
                    adim = 5;
                    mc.stop();
                }
            }
            case YAGMUR_ADIMI -> yagmurBolumu(server);
            default -> {
            }
        }
    }

    /** Vitrin kamerasında, meteorlardan önce: FPS kapısı, 100 tick'lik afetsiz taban ölçümü, yine FPS kapısı. */
    private static void taban() {
        if (tabanEvresi == 1) {
            if (sayac >= 100) {
                GorselOtomasyon.olcumBitir();
                tabanEvresi = 2;
                sayac = 0;
            }
            return;
        }
        if (!GorselOtomasyon.eylemKapisi("meteor", tabanEvresi == 0 ? "taban ölçümü" : "vitrin")) {
            return;
        }
        if (tabanEvresi == 0) {
            GorselOtomasyon.olcumBasla("meteor", "s5_meteor_taban", null);
        }
        tabanEvresi++;
        sayac = 0;
    }

    /**
     * Meteorlar: yavaş çekimde atar, ilk atıştan 100 tick yağmur ölçümü, uçuşta iki kare, ilk ve son çarpmada birer kare,
     * sonra krater ve ateş kareleri. Kare isteyen dal hemen döner: aynı tick'te ikinci istek birincisini düşürürdü.
     */
    private static void vitrin(Minecraft mc, IntegratedServer server) {
        if (sayac == 1) {
            server.execute(() -> {
                eskiSure = MeteorAyarlari.ucusSuresi();
                MeteorAyarlari.ucusSuresiAyarla(YAVAS_CEKIM);
            });
        }
        for (Atis a : ATISLAR) {
            if (sayac == 4 + a.tick()) {
                server.execute(() -> at(server, a));
            }
        }
        if (sayac == 4) {
            GorselOtomasyon.olcumBasla("meteor", "s5_meteor_yuksek", "s5_meteor_taban");
        } else if (sayac == 104) {
            GorselOtomasyon.olcumBitir();
        }
        if (sayac == 130 || sayac == 210) {
            kare(sayac == 130 ? "iz_1" : "iz_2");
            return;
        }
        int carpma = IstemciMeteor.carpmaSayisi() - carpmaBaslangic;
        if (ilkCarpma < 0 && carpma > 0) {
            ilkCarpma = sayac;
            DogalAfetler.LOGGER.info(ON + "ilk çarpma paketi tick {}", sayac);
            kare("carpma");
            return;
        }
        if (ilkCarpma < 0 && sayac > 900) {
            DogalAfetler.LOGGER.warn(ON + "900 tick'te çarpma paketi gelmedi");
            sonraki();
            return;
        }
        if (ilkCarpma > 0 && carpmaSonrasiKare == 0 && (carpma >= ATISLAR.size() || sayac > ilkCarpma + 200)) {
            carpmaSonrasiKare = sayac;
            kare("carpma_son");
            server.execute(() -> {
                if (eskiSure > 0) {
                    MeteorAyarlari.ucusSuresiAyarla(eskiSure);
                }
            });
            return;
        }
        if (carpmaSonrasiKare <= 0) {
            return;
        }
        if (kraterAdimi == 0 && sayac >= carpmaSonrasiKare + 100) {
            kare("krater_ates");
            server.execute(() -> DogalAfetler.LOGGER.info(ON + "krater karesi: yanan {} hücre, bekleyen krater {}",
                    YanginYoneticisi.aktifSayi(server.overworld()), MeteorAPI.bekleyenKraterSayisi(server.overworld())));
            kraterAdimi = 1;
        } else if (kraterAdimi == 1) {
            server.execute(() -> yakinKamera(server));
            carpmaSonrasiKare = sayac;
            kraterAdimi = 2;
        } else if (kraterAdimi == 2 && sayac >= carpmaSonrasiKare + 80) {
            kare("krater_yakin");
            kraterAdimi = 3;
        } else if (kraterAdimi == 3) {
            sonraki();
        }
    }

    /**
     * Yağmur bölümü: güneş patlaması ön-uyarısında (öncü yağmur), sirende ve AKTIF başında kare, sirende yakın/uzak
     * çarpma ses denemesi, sonra ay parçası yağmuru. {@code _gercek} kareleri gerçek hızda ({@code meteor.ucusSuresi}), ötekiler
     * vitrindeki gibi 2× yavaş çekimde ({@value #YAVAS_CEKIM} tick uçuş: kadrajda aynı anda 2 kat çok iz; yoğunluk
     * değişmez). Gerçek hız karesi yavaş meteorlar indikten sonra (400 tick) alınır. Kare isteyen tick'te bekleyen
     * kare alınana kadar sayaç ilerlemez.
     */
    private static void yagmurBolumu(IntegratedServer server) {
        switch (sayac) {
            case 1 -> {
                server.execute(() -> {
                    eskiSure = MeteorAyarlari.ucusSuresi();
                    MeteorAyarlari.ucusSuresiAyarla(YAVAS_CEKIM);
                });
                komut(server, "dogalafet sifirla", "dogalafet start gunes_patlamasi");
            }
            case 600 -> {
                kare("gunes_onuyari");
                sayim(server, "güneş ön-uyarı");
            }
            case 601 -> komut(server, "dogalafet asama ileri");
            case 1100 -> {
                kare("gunes_siren");
                sayim(server, "güneş siren");
            }
            case 1101 -> server.execute(() -> sesDenemesi(server));
            case 1400 -> {
                kare("gunes_siren_b");
                sayim(server, "güneş siren");
            }
            case 1401 -> {
                server.execute(() -> MeteorAyarlari.ucusSuresiAyarla(eskiSure));
                komut(server, "dogalafet asama ileri");
            }
            case 1800 -> {
                kare("gunes_aktif_gercek");
                sayim(server, "güneş AKTIF gerçek hız");
            }
            case 1801 -> server.execute(() -> MeteorAyarlari.ucusSuresiAyarla(YAVAS_CEKIM));
            case 2100 -> {
                kare("gunes_aktif");
                sayim(server, "güneş AKTIF");
            }
            case 2300 -> {
                kare("gunes_aktif_b");
                sayim(server, "güneş AKTIF");
            }
            case 2301 -> {
                server.execute(() -> MeteorAyarlari.ucusSuresiAyarla(eskiSure));
                komut(server, "dogalafet stop", "dogalafet sifirla", "dogalafet start ay_carpmasi",
                        "dogalafet asama ileri", "dogalafet asama ileri", "dogalafet asama ileri");
            }
            case 2600 -> {
                kare("ay_parcalari_gercek");
                sayim(server, "ay parçaları gerçek hız");
            }
            case 2601 -> server.execute(() -> MeteorAyarlari.ucusSuresiAyarla(YAVAS_CEKIM));
            case 2900 -> {
                kare("ay_parcalari");
                sayim(server, "ay parçaları");
            }
            case 3200 -> {
                kare("ay_parcalari_b");
                sayim(server, "ay parçaları");
            }
            case 3201 -> {
                komut(server, "dogalafet stop", "dogalafet sifirla");
                server.execute(() -> MeteorAyarlari.ucusSuresiAyarla(eskiSure));
            }
            case 3300 -> {
                adim = 4;
                sayac = 0;
            }
            default -> {
            }
        }
    }

    private static void komut(IntegratedServer server, String... komutlar) {
        server.execute(() -> {
            for (String k : komutlar) {
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), k);
                DogalAfetler.LOGGER.info(ON + "komut /{}", k);
            }
        });
    }

    /** Etkin afetin aşaması, uçan meteor (bant: küçük/orta/büyük) ve afetin yağmurlarının bu sunucuda attığı meteor. */
    private static void sayim(IntegratedServer server, String ad) {
        server.execute(() -> {
            ServerLevel level = server.overworld();
            int[] bant = new int[3];
            for (Entity e : level.getAllEntities()) {
                if (e instanceof MeteorVarligi m) {
                    bant[MeteorOlcegi.bant(m.olcek())]++;
                }
            }
            AfetYoneticisi.al(server).aktifAfet().ifPresentOrElse(afet -> DogalAfetler.LOGGER.info(
                    ON + "sayım {}: aşama {}, uçan meteor {} (küçük/orta/büyük {}/{}/{}), atılan meteor {}", ad, afet.asama(),
                    MeteorAPI.aktifMeteorSayisi(level), bant[0], bant[1], bant[2], MeteorAPI.atilanMeteorSayisi(level,
                            YanginKaynagi.afet(afet.tur(), afet.baglam().baslangicSaati()))),
                    () -> DogalAfetler.LOGGER.warn(ON + "sayım {}: etkin afet yok", ad));
        });
    }

    /** Ses menzili denemesi (sunucu thread'i, {@link #SES_ATISLARI}); istemci {@code METEOR_SES} satırlarını yazar. */
    private static void sesDenemesi(IntegratedServer server) {
        ServerLevel level = server.overworld();
        BlockPos p = server.getPlayerList().getPlayers().get(0).blockPosition();
        for (SesAtisi a : SES_ATISLARI) {
            BlockPos hedef = p.offset(a.dx(), 0, a.dz());
            boolean ucan = MeteorAPI.dusur(level, hedef, a.boyut(), MeteorGorunumu.GUNES, YanginKaynagi.KOMUT, false)
                    .isPresent();
            DogalAfetler.LOGGER.info(ON + "ses denemesi: {} meteor → {} (yatay {} blok), uçuş {}", a.boyut(),
                    hedef.toShortString(), Math.round(Math.hypot(a.dx(), a.dz())), ucan);
        }
    }

    private static void sonraki() {
        adim++;
        sayac = 0;
    }

    /** Sunucu thread'inde: kurallar, alacakaranlık, en yakın orman, seyirci kamera. */
    private static void hazirla(IntegratedServer server) {
        for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", YALNIZ_ODUL ? "time set 6000" : "time set 13200",
                "weather clear"}) {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), k);
        }
        ServerLevel level = server.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        Pair<BlockPos, Holder<Biome>> bulunan = level.findClosestBiome3d(h -> h.is(Biomes.FOREST)
                || h.is(Biomes.BIRCH_FOREST) || h.is(Biomes.FLOWER_FOREST) || h.is(Biomes.DARK_FOREST), dogus, 6400, 32, 64);
        BlockPos merkez = bulunan == null ? dogus : bulunan.getFirst();
        odak = merkez;
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.setGameMode(GameType.SPECTATOR);
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, merkez.getX(), merkez.getZ());
        oyuncu.teleportTo(level, merkez.getX() - UZAKLIK + 0.5, ust + KAMERA_YUKSEKLIGI, merkez.getZ() + 0.5, -90F, 4F);
        DogalAfetler.LOGGER.info(ON + "orman {} ({}), kamera y={}", merkez.toShortString(),
                bulunan == null ? "bulunamadı (doğuş)" : bulunan.getSecond().unwrapKey().map(k -> k.location().toString())
                        .orElse("?"), ust + KAMERA_YUKSEKLIGI);
        sunucuHazir = true;
    }

    private static void at(IntegratedServer server, Atis a) {
        ServerLevel level = server.overworld();
        BlockPos hedef = odak.offset(a.dx(), 0, a.dz());
        boolean var = MeteorAPI.dusur(level, hedef, a.boyut(), a.gorunum(), YanginKaynagi.KOMUT, true, a.aci(), a.azimut())
                .isPresent();
        DogalAfetler.LOGGER.info(ON + "meteor {} {} → {}: varlık {}", a.boyut(), a.gorunum(), hedef.toShortString(), var);
    }

    /** İlk krateri yukarıdan yakından gören kamera. */
    private static void yakinKamera(IntegratedServer server) {
        ServerLevel level = server.overworld();
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, odak.getX(), odak.getZ());
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(level, odak.getX() - 13.5, ust + 11, odak.getZ() + 0.5, -90F, 38F);
    }

    /** Sunucu thread'inde: kamera yakınında taş platform, sıcak Meteorit Parçası ve Kozmik Çekirdek. */
    private static void oduller(IntegratedServer server) {
        ServerLevel level = server.overworld();
        BlockPos taban = odak.offset(-UZAKLIK, 0, -20);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, taban.getX(), taban.getZ()) + 6;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                level.setBlockAndUpdate(new BlockPos(taban.getX() + dx, y, taban.getZ() + dz),
                        Blocks.POLISHED_DEEPSLATE.defaultBlockState());
                for (int k = 1; k <= 4; k++) {
                    level.setBlockAndUpdate(new BlockPos(taban.getX() + dx, y + k, taban.getZ() + dz),
                            Blocks.AIR.defaultBlockState());
                }
            }
        }
        for (int[] k : new int[][] {{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            level.setBlockAndUpdate(new BlockPos(taban.getX() + k[0], y + 1, taban.getZ() + k[1]),
                    Blocks.SEA_LANTERN.defaultBlockState());
        }
        MeteorOdulleri.birak(level, new Vec3(taban.getX() - 0.1, y + 1.1, taban.getZ() + 0.5),
                new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get()), 20).setPickUpDelay(32767);
        MeteorOdulleri.birak(level, new Vec3(taban.getX() + 1.5, y + 1.1, taban.getZ() + 0.5),
                new ItemStack(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get()), 0).setPickUpDelay(32767);
        ServerPlayer oyuncu = server.getPlayerList().getPlayers().get(0);
        oyuncu.teleportTo(level, taban.getX() + 0.7, y + 2.0, taban.getZ() - 1.1, 0F, 48F);
        DogalAfetler.LOGGER.info(ON + "ödüller {} y={}", taban.toShortString(), y + 1);
    }

    /** Kareyi A-34 kapısına ister; alınınca kare satırı yazılır. */
    private static void kare(String ad) {
        GorselOtomasyon.kareIste("meteor", "meteor_" + ad + ".png", k -> DogalAfetler.LOGGER.info(
                ON + "kare {} (adım {} tick {}, FPS {}, parçacık {}, çarpma paketi {})", k.dosya(), adim, sayac, k.fps(),
                Minecraft.getInstance().particleEngine.countParticles(), IstemciMeteor.carpmaSayisi()));
    }

    private static ItemStack ipucuYigini(int i) {
        return switch (i) {
            case 0 -> new ItemStack(MeteorEsyaKayitlari.KOZMIK_CEKIRDEK.get());
            case 1 -> new ItemStack(MeteorEsyaKayitlari.METEORIT_PARCASI.get());
            case 2 -> new ItemStack(MeteorEsyaKayitlari.TERMOSTAT_PLAKASI.get());
            default -> {
                ItemStack zirh = new ItemStack(Items.DIAMOND_CHESTPLATE);
                zirh.enchant(Enchantments.ALL_DAMAGE_PROTECTION, 3);
                ZirhEkleri.koy(zirh, Termostat.TERMOSTATLI);
                ZirhEkleri.koy(zirh, ZirhEkleri.YUNLU);
                ZirhEkleri.tuyAyarla(zirh, 8);
                yield zirh;
            }
        };
    }

    /** Tek eşya: büyütülmüş simge (çekirdeğin büyü parlaması görünür) ve sağında ekrana sığdırılmış ipucu; dünya arkada. */
    private static final class IpucuEkrani extends Screen {
        private final ItemStack yigin;

        IpucuEkrani(ItemStack yigin) {
            super(Component.literal("meteor"));
            this.yigin = yigin;
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }

        @Override
        public void render(GuiGraphics g, int fareX, int fareY, float kismi) {
            g.fill(0, 0, width, height, 0x70000000);
            int simgeX = width / 6;
            int ust = height / 2 - 32;
            g.pose().pushPose();
            g.pose().translate(simgeX, ust, 0);
            g.pose().scale(4, 4, 1);
            g.renderItem(yigin, 0, 0);
            g.pose().popPose();
            int metinX = simgeX + 76;
            int genislik = Math.max(60, width - metinX - 20);
            List<FormattedCharSequence> satirlar = new ArrayList<>();
            for (Component satir : getTooltipFromItem(Minecraft.getInstance(), yigin)) {
                satirlar.addAll(font.split(satir, genislik));
            }
            g.renderTooltip(font, satirlar, metinX - 12, ust + 12);
        }
    }
}
