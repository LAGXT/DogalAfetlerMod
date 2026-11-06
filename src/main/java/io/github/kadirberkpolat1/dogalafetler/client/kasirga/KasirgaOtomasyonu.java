package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.Kasirga;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.UcanBlokVarligi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2, GOREV §6 #6/#10). Yalnız {@code -Ddogalafetler.otomasyon=kasirga} ile tek
 * oyunculu dünyada çalışır; üretimde etkisizdir. Sırayla: S5 kasırga oyuncuyu hedefler; ön-uyarı karesi (Gelişmiş Hava
 * Radarı elde, arayüz açık), siren (duvar bulutu alçalır), aktif doğuş, vitrin (olgun evre, {@link #VITRIN} kamerası) ve
 * aynı kameranın kasırgasız kontrol karesi; vitrinde Düşük/Yüksek/Ultra ve kasırgasız (çizimi atlanmış) FPS; LOD
 * kademeleri (120 ve 180 blok); K0-33 mesafeleri 52/36/20/5 blok Yüksek; yakın ve iç kare; {@code stop} sonrası istemci
 * varlık sayımı ve aynı noktada afetsiz taban FPS; S1 aynı vitrin kamerasından, ip evresi, doğal bitiş ve iz karesi.
 * Ölçüm süresince kamera kasırgayı izler (mesafe sabit kalır). Kareler A-34 kapısıyla alınır: istekten sonra en az
 * {@link #KARE_CIZIM} kare çizilmiş ve FPS en az {@link #AKICI_FPS} olmalı (gizli workspace'te bayat framebuffer), en çok
 * 60 sn beklenir. Siren ve aktif aşamaya geçmeden ve her ölçümden önce de aynı FPS kapısı beklenir; ortanca kare süresi
 * {@link #BAYAT_MS}'yi aşan ölçüm bir kez yinelenir. Siren başından {@code stop}'a kadar zamanlayıcı {@link #YAVAS} hızında
 * döner: kasırga her tick hareket eder ve çizilir, yalnız evre süreleri uzar (bekleme ölçüm sırasında olgun evreyi
 * tüketmesin). Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class KasirgaOtomasyonu {
    private static final boolean AKTIF = "kasirga".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "KASIRGA_OTOMASYON ";
    private static final ResourceLocation RADAR = DogalAfetler.konum("gelismis_hava_radari");
    private static final int AKICI_FPS = 15;
    private static final int KARE_CIZIM = 20;
    private static final long KARE_BEKLEME_NS = 60_000_000_000L;
    private static final int TAKIP_ARALIGI = 10;
    private static final int KAPI_EN_COK = 1200;
    private static final double BAYAT_MS = 200;
    private static final String YAVAS = "0.2";

    /** Kasırganın hareket yönünde {@code mesafe} blok önünde, zemin + {@code yukseklik}, {@code egim} derece. */
    private record Kamera(double mesafe, double yukseklik, float egim) {
    }

    private static final Kamera VITRIN = new Kamera(55, 10, -8);
    private static final Kamera YAKIN = new Kamera(22, 4, -20);
    /** Kaldırılan oyuncu noktası (K0-33 "≤ 5 blok"): etek ve huninin içi. */
    private static final Kamera IC = new Kamera(5, 3, -25);

    /** {@code bekle} tick sonra (ve istenen kare alınmışsa) {@code kosul} doğruysa {@code is} çalışır; {@code enCok} tick sonra zorla. */
    private record Adim(String ad, int bekle, BooleanSupplier kosul, int enCok, Runnable is) {
    }

    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static int sira = -1;
    private static int sayac;
    private static int tus;
    private static int fare;
    private static int teker;
    private static String istenenKare;
    private static int kareCizim;
    private static long kareIstek;
    private static String olcum;
    private static Kamera takip;
    private static int takipSayac;
    private static long[] kareSureleri = new long[4096];
    private static int kareSayisi;
    private static long sonKare;
    private static volatile boolean kosulSunucu;

    private KasirgaOtomasyonu() {
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

    private static void adim(String ad, int bekle, Runnable is) {
        ADIMLAR.add(new Adim(ad, bekle, () -> true, bekle, is));
    }

    private static void kosullu(String ad, int bekle, BooleanSupplier kosul, int enCok, Runnable is) {
        ADIMLAR.add(new Adim(ad, bekle, kosul, enCok, is));
    }

    private static void sunucuda(IntegratedServer server, Consumer<ServerLevel> is) {
        server.execute(() -> is.accept(server.overworld()));
    }

    private static void komut(IntegratedServer server, String komut) {
        server.execute(() -> server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(), komut));
    }

    private static Kasirga kasirga(IntegratedServer server) {
        Afet afet = AfetYoneticisi.al(server).aktifAfet().orElse(null);
        return afet instanceof Kasirga k ? k : null;
    }

    private static ServerPlayer oyuncu(IntegratedServer server) {
        return server.getPlayerList().getPlayers().get(0);
    }

    /** Kamerayı kasırganın hareket yönünde {@code mesafe} blok önüne, zemin + {@code yukseklik}'e, kasırgaya bakar koyar. */
    private static void kamera(IntegratedServer server, Kamera kam) {
        sunucuda(server, level -> {
            Kasirga k = kasirga(server);
            if (k == null) {
                return;
            }
            Vec3 m = k.konum();
            double cx = m.x + Math.cos(k.yon()) * kam.mesafe();
            double cz = m.z + Math.sin(k.yon()) * kam.mesafe();
            int zemin = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(cx), Mth.floor(cz));
            float yaw = (float) Math.toDegrees(Math.atan2(-(m.x - cx), m.z - cz));
            oyuncu(server).teleportTo(level, cx, Math.max(zemin, m.y) + kam.yukseklik(), cz, yaw, kam.egim());
            DogalAfetler.LOGGER.info(ON + "kamera {} blok, kasırga {} evre {} ({}), uçan blok {}, kaldırılan {}", kam.mesafe(),
                    BlockPos.containing(m).toShortString(), k.durum().evre(), String.format(Locale.ROOT, "%.2f",
                            k.durum().ilerleme()), k.ucanBlokSayisi(), k.toplamKaldirilan());
        });
    }

    private static void kare(String ad) {
        istenenKare = ad;
        kareCizim = 0;
        kareIstek = System.nanoTime();
    }

    private static void olcBasla(String etiket, Kamera kam, EfektKalitesi kalite, boolean gizli) {
        IstemciKasirga.zorlananKalite = kalite;
        IstemciKasirga.gizli = gizli;
        KasirgaRenderer.lodSifirla();
        olcum = etiket;
        takip = kam;
        takipSayac = 0;
        kareSayisi = 0;
        sonKare = 0;
    }

    /** FPS ortanca kare süresinden (K0-21 rev. 2 (i)); geçerliliği koşu betiğinin Gradle sayacı günlüğü belirler. */
    private static boolean olcBitir(IntegratedServer server) {
        long[] sureler = Arrays.copyOf(kareSureleri, kareSayisi);
        Arrays.sort(sureler);
        double ortanca = sureler.length == 0 ? Double.NaN : sureler[sureler.length / 2] / 1e6;
        Minecraft mc = Minecraft.getInstance();
        int ucan = mc.level.getEntitiesOfClass(UcanBlokVarligi.class, mc.player.getBoundingBox().inflate(512)).size();
        boolean gecerli = ortanca <= BAYAT_MS;
        DogalAfetler.LOGGER.info(ON + "FPS {} = {} (ortanca kare {} ms, {} kare{}), sunucu TPS {} ort {} ms, kasırga {}, istemci "
                + "uçan blok {}, görsel enkaz {}, LOD {}", olcum, String.format(Locale.ROOT, "%.1f", 1000 / ortanca),
                String.format(Locale.ROOT, "%.2f", ortanca), sureler.length, gecerli ? "" : ", BAYAT",
                String.format(Locale.ROOT, "%.2f", TpsOlcer.tps()), String.format(Locale.ROOT, "%.2f", server.getAverageTickTime()),
                IstemciKasirga.kasirgaSayisi(), ucan, IstemciKasirga.gorselEnkazSayisi(), KasirgaRenderer.lodOzeti());
        olcum = null;
        takip = null;
        IstemciKasirga.zorlananKalite = null;
        IstemciKasirga.gizli = false;
        return gecerli;
    }

    private static boolean akici() {
        return Minecraft.getInstance().getFps() >= AKICI_FPS;
    }

    /** Sunucu durumu ilerlemeden önce istemci akıcı çizsin (en çok 60 sn; dolarsa log'a yazılır). */
    private static void akiciBekle(String ad) {
        kosullu(ad + " FPS kapısı", 20, KasirgaOtomasyonu::akici, KAPI_EN_COK, () -> {
            if (!akici()) {
                DogalAfetler.LOGGER.warn(ON + "{}: FPS kapısı 60 sn'de açılmadı (FPS {})", ad, Minecraft.getInstance().getFps());
            }
        });
    }

    /**
     * Kamera kurulur, FPS kapısı beklenir, 100 tick boyunca kare süreleri toplanır (kamera kasırgayı izler). Bayat ölçüm
     * (ortanca kare &gt; {@link #BAYAT_MS} ms) bir kez yinelenir.
     */
    private static void fps(IntegratedServer server, String etiket, Kamera kam, EfektKalitesi kalite, boolean gizli) {
        ADIMLAR.addAll(olcumAdimlari(server, etiket, kam, kalite, gizli, false));
    }

    private static List<Adim> olcumAdimlari(IntegratedServer server, String etiket, Kamera kam, EfektKalitesi kalite,
            boolean gizli, boolean tekrar) {
        String ad = "FPS " + etiket + (tekrar ? " (tekrar)" : "");
        return List.of(
                new Adim(ad + " kamerası", 5, () -> true, 5, () -> kamera(server, kam)),
                new Adim(ad, 20, KasirgaOtomasyonu::akici, KAPI_EN_COK, () -> olcBasla(etiket, kam, kalite, gizli)),
                new Adim(ad + " son", 100, () -> true, 100, () -> {
                    if (!olcBitir(server) && !tekrar) {
                        ADIMLAR.addAll(sira + 1, olcumAdimlari(server, etiket, kam, kalite, gizli, true));
                    }
                }));
    }

    /** Akış kendiliğinden ilerlemişse (gizli workspace'te kare beklenirken) aşama atlanmasın: yalnız koşul tutarsa ilerletir. */
    private static void ilerletEger(IntegratedServer server, Predicate<Kasirga> kosul) {
        server.execute(() -> {
            Kasirga k = kasirga(server);
            if (k != null && kosul.test(k)) {
                AfetYoneticisi.al(server).asamaIleri();
            }
        });
    }

    private static void senaryo(Minecraft mc, IntegratedServer server) {
        adim("hazırlık", 0, () -> {
            for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 6000", "weather rain"}) {
                komut(server, k);
            }
            sunucuda(server, level -> {
                ServerPlayer o = oyuncu(server);
                o.setGameMode(GameType.CREATIVE);
                BlockPos d = level.getSharedSpawnPos();
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, d.getX(), d.getZ());
                o.teleportTo(level, d.getX() + 0.5, y + 1, d.getZ() + 0.5, 0, 0);
                o.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ForgeRegistries.ITEMS.getValue(RADAR)));
            });
        });
        adim("S5 başlat", 60, () -> {
            komut(server, "dogalafet start kasirga 5 " + mc.player.getGameProfile().getName());
            komut(server, "dogalafet hiz 5");
        });
        kosullu("ön-uyarı normal pencere", 20, () -> {
            sunucuda(server, level -> {
                Kasirga k = kasirga(server);
                kosulSunucu = k != null && k.normalOnUyariBasladi();
            });
            return kosulSunucu;
        }, 1200, () -> komut(server, "dogalafet hiz 1"));
        akiciBekle("ön-uyarı");
        adim("ön-uyarı karesi", 60, () -> kare("on_uyari_radar"));
        akiciBekle("siren");
        adim("siren", 10, () -> {
            ilerletEger(server, k -> k.asama() == AfetAsamasi.ON_UYARI);
            komut(server, "dogalafet hiz " + YAVAS);
            sunucuda(server, level -> {
                ServerPlayer o = oyuncu(server);
                o.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                o.setGameMode(GameType.SPECTATOR);
            });
            mc.options.hideGui = true;
        });
        adim("siren kamerası", 20, () -> kamera(server, new Kamera(70, 12, -12)));
        adim("siren karesi", 140, () -> kare("siren"));
        akiciBekle("aktif");
        adim("aktif", 30, () -> ilerletEger(server, k -> k.asama() == AfetAsamasi.SIREN));
        adim("aktif kamerası", 60, () -> kamera(server, new Kamera(60, 10, -10)));
        adim("aktif doğuş karesi", 40, () -> kare("aktif_dogus"));
        adim("olgun", 20, () -> ilerletEger(server, k -> k.durum().evre() == KasirgaEvresi.DOGUS));
        adim("vitrin kamerası", 100, () -> kamera(server, VITRIN));
        adim("vitrin karesi", 30, () -> kare("s5_vitrin"));
        adim("kontrol", 5, () -> IstemciKasirga.gizli = true);
        adim("kontrol karesi", 10, () -> kare("s5_vitrin_kontrol"));
        adim("kontrol bitti", 5, () -> IstemciKasirga.gizli = false);
        adim("vitrin karesi b", 60, () -> kare("s5_vitrin_b"));
        fps(server, "S5 55 blok DÜŞÜK", VITRIN, EfektKalitesi.DUSUK, false);
        fps(server, "S5 55 blok YÜKSEK", VITRIN, EfektKalitesi.YUKSEK, false);
        fps(server, "S5 55 blok ULTRA", VITRIN, EfektKalitesi.ULTRA, false);
        fps(server, "S5 55 blok kasırgasız (çizim yok)", VITRIN, null, true);
        fps(server, "S5 LOD 120 blok YÜKSEK", new Kamera(120, 14, -6), EfektKalitesi.YUKSEK, false);
        fps(server, "S5 LOD 180 blok YÜKSEK", new Kamera(180, 18, -5), EfektKalitesi.YUKSEK, false);
        fps(server, "S5 52 blok YÜKSEK", new Kamera(52, 10, -8), EfektKalitesi.YUKSEK, false);
        fps(server, "S5 36 blok YÜKSEK", new Kamera(36, 8, -12), EfektKalitesi.YUKSEK, false);
        fps(server, "S5 20 blok YÜKSEK", new Kamera(20, 4, -20), EfektKalitesi.YUKSEK, false);
        adim("yakın kamera", 10, () -> kamera(server, YAKIN));
        adim("yakın karesi", 40, () -> kare("yakin"));
        adim("iç kamera", 10, () -> kamera(server, IC));
        adim("iç karesi", 40, () -> kare("ic"));
        fps(server, "S5 5 blok YÜKSEK", IC, EfektKalitesi.YUKSEK, false);
        fps(server, "S5 5 blok kasırgasız (çizim yok)", IC, null, true);
        adim("stop", 5, () -> {
            komut(server, "dogalafet stop");
            komut(server, "dogalafet hiz 1");
        });
        adim("stop sayımı", 60, () -> {
            DogalAfetler.LOGGER.info(ON + "stop sonrası istemci: kasirga={}, ucan_blok={}, gorsel_kopya={}",
                    IstemciKasirga.kasirgaSayisi(), mc.level.getEntitiesOfClass(UcanBlokVarligi.class,
                            mc.player.getBoundingBox().inflate(512)).size(), IstemciKasirga.gorselEnkazSayisi());
            sunucuda(server, level -> DogalAfetler.LOGGER.info(ON + "stop sonrası sunucu: kasirga={}, ucan_blok={}",
                    level.getEntities(KasirgaKayitlari.KASIRGA.get(), e -> true).size(),
                    level.getEntities(KasirgaKayitlari.UCAN_BLOK.get(), e -> true).size()));
        });
        ADIMLAR.addAll(olcumAdimlari(server, "afetsiz taban (stop sonrası, 5 blok noktası)", null, null, false, false)
                .subList(1, 3));
        adim("S1 başlat", 40, () -> {
            komut(server, "dogalafet start kasirga 1 " + mc.player.getGameProfile().getName());
            ilerletEger(server, k -> k.asama() == AfetAsamasi.ON_UYARI);
            ilerletEger(server, k -> k.asama() == AfetAsamasi.SIREN);
            ilerletEger(server, k -> k.durum().evre() == KasirgaEvresi.DOGUS);
        });
        adim("S1 kamerası", 80, () -> kamera(server, VITRIN));
        adim("S1 karesi", 40, () -> kare("s1_vitrin"));
        adim("S1 ip", 10, () -> ilerletEger(server, k -> k.durum().evre() == KasirgaEvresi.OLGUN));
        adim("S1 ip kamerası", 60, () -> kamera(server, VITRIN));
        adim("ip karesi", 30, () -> kare("ip"));
        adim("S1 dağılma", 60, () -> ilerletEger(server, k -> k.durum().evre() == KasirgaEvresi.IP));
        adim("doğal bitiş", 30, () -> ilerletEger(server, k -> k.durum().evre() == KasirgaEvresi.DAGILMA));
        adim("bitiş kamerası", 30, () -> sunucuda(server, level -> {
            ServerPlayer o = oyuncu(server);
            o.teleportTo(level, o.getX(), o.getY() + 14, o.getZ(), o.getYRot(), 50);
        }));
        adim("bitiş karesi", 40, () -> kare("bitis_iz"));
        adim("kapanış", 60, () -> {
            DogalAfetler.LOGGER.info(ON + "bitiş sonrası istemci: kasirga={}, gorsel_kopya={}", IstemciKasirga.kasirgaSayisi(),
                    IstemciKasirga.gorselEnkazSayisi());
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
        });
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
        if (sira < 0) {
            if (++sayac >= 100) {
                DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}", mc.player.getGameProfile().getName());
                senaryo(mc, server);
                sira = 0;
                sayac = 0;
            }
            return;
        }
        if (olcum != null && takip != null && ++takipSayac % TAKIP_ARALIGI == 0) {
            kamera(server, takip);
        }
        if (sira >= ADIMLAR.size() || istenenKare != null) {
            return;
        }
        Adim a = ADIMLAR.get(sira);
        if (++sayac < a.bekle() || (!a.kosul().getAsBoolean() && sayac < a.enCok())) {
            return;
        }
        DogalAfetler.LOGGER.info(ON + "adım {}: {} ({} tick)", sira, a.ad(), sayac);
        a.is().run();
        sira++;
        sayac = 0;
    }

    @SubscribeEvent
    public static void cizimSonu(TickEvent.RenderTickEvent olay) {
        if (!AKTIF || olay.phase != TickEvent.Phase.END) {
            return;
        }
        if (olcum != null) {
            long simdi = System.nanoTime();
            if (sonKare != 0 && kareSayisi < kareSureleri.length) {
                kareSureleri[kareSayisi++] = simdi - sonKare;
            }
            sonKare = simdi;
        }
        if (istenenKare == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        kareCizim++;
        boolean akici = kareCizim >= KARE_CIZIM && mc.getFps() >= AKICI_FPS;
        if (!akici && System.nanoTime() - kareIstek < KARE_BEKLEME_NS) {
            return;
        }
        String dosya = "kasirga_" + istenenKare + ".png";
        istenenKare = null;
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> {
        });
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, çizilen kare {}, kapı {}, kasırga {}, görsel enkaz {})", dosya, mc.getFps(),
                kareCizim, akici ? "açık" : "60 sn doldu, UYARI", IstemciKasirga.kasirgaSayisi(),
                IstemciKasirga.gorselEnkazSayisi());
    }
}
