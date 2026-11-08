package io.github.kadirberkpolat1.dogalafetler.client.kasirga;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.Kasirga;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.KasirgaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.UcanBlokVarligi;
import io.github.kadirberkpolat1.dogalafetler.afet.kasirga.mantik.KasirgaEvresi;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
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
 * aynı kameranın kontrol karesi (K-E49: yalnız huni, toz/etek, duvar bulutu ve sis gizli; uçan bloklar ve enkaz çizilir);
 * vitrinde Düşük/Yüksek/Ultra ve kasırgasız (çizimi atlanmış) FPS; LOD
 * kademeleri (120 ve 180 blok); K0-33 mesafeleri 52/36/20/5 blok Yüksek (sunucuda en az {@link #UCAN_EN_AZ} uçan blok
 * varken, Kv2-2); yakın ve iç kare; {@code stop} sonrası istemci varlık sayımı ve aynı noktada afetsiz taban FPS; S1 S5
 * izinin {@link #S1_UZAKLIK} blok yanındaki taze zeminde, aynı vitrin kamerasından (kaldırılan ve görsel enkaz varken),
 * olgun evresi ilerletilmeden hız 1'de doğal süresinde biter (Kv3-2); ip evresi, doğal bitiş ve S1 yoluna bakan iz karesi.
 * Vitrinden sonra kasırga sisinin dışından ({@link #UZAK}) taban karesi ve kontrolü (renk ölçümü, K-17).
 *
 * <p>Kareler ve ölçümler {@link GorselOtomasyon} ile: kare istekten sonra en az {@link #KARE_CIZIM} kare çizilmiş ve FPS en
 * az {@value GorselOtomasyon#AKICI_FPS} iken alınır (A-34; gizli workspace'te bayat framebuffer), beklerken adımlar durur.
 * Her ölçüm 100 tick'lik bir {@code OLCUM} penceresidir; kamera kasırgayı izler (mesafe sabit kalır). Bütün S5 ölçümlerinin
 * tabanı {@code stop} sonrası aynı 5 blok noktasındaki {@value #TABAN} ölçümüdür (K-E29 (b), K-13). Siren ve aktif aşamaya
 * geçmeden ve her ölçümden önce FPS kapısı beklenir; ortanca kare süresi {@link #BAYAT_MS}'yi aşan ölçüm bir kez yinelenir.
 * Siren başından {@code stop}'a kadar ve S1'de (olgun evrenin doğal akışı dışında) zamanlayıcı {@link #YAVAS} hızında
 * döner: kasırga her tick hareket eder ve çizilir, yalnız evre süreleri uzar (bekleme ölçüm sırasında olgun evreyi
 * tüketmesin). Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır.
 *
 * <p>{@code -Ddogalafetler.otomasyon.bolum=rota} (AF2-20, kasırga kimseyi izlemez): oyuncu doğuş noktasında yerde durur, S3
 * kasırga normal komutla onu hedef alır (doğuş 40-80 blok), olgun evreye geçilir ve hız 1'de {@link #ROTA_ARALIK} tick arayla
 * üç kare ({@code kasirga_rota_1..3}) oyuncunun gözünden kasırgaya bakılarak alınır; her kare için kasırga merkezi, oyuncu,
 * yatay uzaklık ve ara hedef, sonda {@code rota: uzaklık d1/d2/d3, ara hedef N} satırı.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class KasirgaOtomasyonu {
    private static final boolean AKTIF = "kasirga".equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final boolean ROTA = "rota".equals(System.getProperty("dogalafetler.otomasyon.bolum", ""));
    /** Rota kareleri arası (tick, hız 1'de 30 sn). */
    private static final int ROTA_ARALIK = 600;
    private static final int ROTA_KARE = 3;
    /** Rota karelerinin eğimi (derece; eksi yukarı): oyuncunun gözünden kasırganın gövdesi kadrajda. */
    private static final float ROTA_EGIM = -5;
    private static final String ON = "KASIRGA_OTOMASYON ";
    private static final ResourceLocation RADAR = DogalAfetler.konum("gelismis_hava_radari");
    private static final int KARE_CIZIM = 20;
    private static final int TAKIP_ARALIGI = 10;
    private static final int KAPI_EN_COK = 1200;
    private static final double BAYAT_MS = 200;
    private static final String YAVAS = "0.2";
    /** S1 hedef noktasının S5 izinin orta noktasından dik uzaklığı (blok): S1 S5'in soymadığı zeminde başlar. */
    private static final double S1_UZAKLIK = 220;
    /** K0-33 mesafe ölçümlerinin tepe yükü (Kv2-2): ölçüm sunucuda en az bu kadar uçan blok varken başlar. */
    private static final int UCAN_EN_AZ = 30;
    /** S1 olgun evresinin hız 1'de doğal bitişi için en uzun bekleme (tick; S1 olgun 60 sn). */
    private static final int S1_OLGUN_EN_COK = 1800;
    /** S5 ölçümlerinin {@code OLCUM} tabanı: {@code stop} sonrası aynı 5 blok noktası. */
    private static final String TABAN = "afetsiz_taban";

    /** Kasırganın hareket yönünde {@code mesafe} blok önünde, zemin + {@code yukseklik}, {@code egim} derece. */
    private record Kamera(double mesafe, double yukseklik, float egim) {
    }

    private static final Kamera VITRIN = new Kamera(55, 10, -8);
    private static final Kamera YAKIN = new Kamera(22, 4, -20);
    /** Kaldırılan oyuncu noktası (K0-33 "≤ 5 blok"): etek ve huninin içi. */
    private static final Kamera IC = new Kamera(5, 3, -25);
    /** Huni kenarı 64 bloktan uzakta (kasırga sisi yok); taban rengi ölçümü (K-17). */
    private static final Kamera UZAK = new Kamera(90, 10, -6);

    /** {@code bekle} tick sonra (ve istenen kare alınmışsa) {@code kosul} doğruysa {@code is} çalışır; {@code enCok} tick sonra zorla. */
    private record Adim(String ad, int bekle, BooleanSupplier kosul, int enCok, Runnable is) {
    }

    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static int sira = -1;
    private static int sayac;
    private static int tus;
    private static int fare;
    private static int teker;
    private static String olcum;
    private static Kamera takip;
    private static int takipSayac;
    private static volatile boolean kosulSunucu;
    /**
     * S5 izinin başı/sonu, S1'in dağılma noktası (x, z, yön) ve ölçüm başındaki uçan blok/toplam kaldırılan; yalnız sunucu
     * thread'i yazar, sonra yine o okur.
     */
    private static Vec3 s5Bas;
    private static Vec3 s5Son;
    private static double[] s1Son;
    private static long[] yukBas;
    /** Rota karelerindeki yatay uzaklıklar; yalnız sunucu thread'i yazar ve okur. */
    private static final double[] ROTA_UZAKLIK = new double[ROTA_KARE];

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
        GorselOtomasyon.kareIste("kasirga", "kasirga_" + ad + ".png", KARE_CIZIM, k -> DogalAfetler.LOGGER.info(
                ON + "kare {} (FPS {}, çizilen kare {}, kapı {}, kasırga {}, görsel enkaz {})", k.dosya(), k.fps(), k.yeniKare(),
                k.gecerli() ? "açık" : "60 sn doldu, GEÇERSİZ", IstemciKasirga.kasirgaSayisi(),
                IstemciKasirga.gorselEnkazSayisi()));
    }

    /** K-17 kontrol karesinin kipi (K-E49); satır betiğin {@code kontrol=yalnizToz} işareti. */
    private static void yalnizTozGizle(boolean gizle) {
        IstemciKasirga.yalnizTozGizli = gizle;
        if (gizle) {
            DogalAfetler.LOGGER.info(ON + "kontrol kipi yalnizToz (K-E49)");
        }
    }

    private static void olcBasla(IntegratedServer server, String etiket, String sahne, String taban, Kamera kam,
            EfektKalitesi kalite, boolean gizli) {
        IstemciKasirga.zorlananKalite = kalite;
        IstemciKasirga.gizli = gizli;
        KasirgaRenderer.lodSifirla();
        olcum = etiket;
        takip = kam;
        takipSayac = 0;
        sunucuda(server, level -> {
            Kasirga k = kasirga(server);
            yukBas = k == null ? null : new long[] {k.ucanBlokSayisi(), k.toplamKaldirilan()};
        });
        GorselOtomasyon.olcumBasla("kasirga", sahne, kalite, taban);
    }

    /**
     * FPS ortanca kare süresinden (K0-21 rev. 2 (i), {@link GorselOtomasyon#olcumBitir}); satıra pencere boyunca sunucudaki
     * uçan blok (baş → son) ve kaldırılan artışı eklenir (Kv2-2), bu yüzden satır sunucu thread'inde yazılır.
     */
    private static boolean olcBitir(IntegratedServer server) {
        GorselOtomasyon.Olcum o = GorselOtomasyon.olcumBitir();
        Minecraft mc = Minecraft.getInstance();
        int ucan = mc.level.getEntitiesOfClass(UcanBlokVarligi.class, mc.player.getBoundingBox().inflate(512)).size();
        boolean gecerli = o.duvarMs() <= BAYAT_MS;
        String satir = String.format(Locale.ROOT, "FPS %s = %.1f (ortanca kare %.2f ms, %d kare%s), sunucu TPS %.2f ort %.2f ms, "
                + "kasırga %d, istemci uçan blok %d, görsel enkaz %d, LOD %s", olcum, o.fps(), o.duvarMs(), o.kare(),
                gecerli ? "" : ", BAYAT", TpsOlcer.tps(), o.mspt(), IstemciKasirga.kasirgaSayisi(), ucan,
                IstemciKasirga.gorselEnkazSayisi(), KasirgaRenderer.lodOzeti());
        sunucuda(server, level -> {
            Kasirga k = kasirga(server);
            String yuk = k == null || yukBas == null ? "-" : String.format(Locale.ROOT, "%d → %d, kaldırılan +%d", yukBas[0],
                    k.ucanBlokSayisi(), k.toplamKaldirilan() - yukBas[1]);
            DogalAfetler.LOGGER.info(ON + "{}, sunucu uçan blok {}", satir, yuk);
        });
        olcum = null;
        takip = null;
        IstemciKasirga.zorlananKalite = null;
        IstemciKasirga.gizli = false;
        return gecerli;
    }

    /** Sunucu durumu ilerlemeden önce istemci akıcı çizsin (en çok 60 sn; dolarsa log'a yazılır). */
    private static void akiciBekle(String ad) {
        kosullu(ad + " FPS kapısı", 20, GorselOtomasyon::akici, KAPI_EN_COK, () -> {
            if (!GorselOtomasyon.akici()) {
                DogalAfetler.LOGGER.warn(ON + "{}: FPS kapısı 60 sn'de açılmadı (FPS {})", ad, Minecraft.getInstance().getFps());
            }
        });
    }

    /**
     * {@value #TABAN} tabanlı ölçüm: kamera kurulur, {@code yuk} ise sunucuda en az {@link #UCAN_EN_AZ} uçan blok beklenir
     * (en çok 60 sn; dolarsa uyarıyla yine ölçülür), FPS kapısı beklenir, 100 tick'lik pencere (kamera kasırgayı izler). Bayat
     * ölçüm (ortanca kare &gt; {@link #BAYAT_MS} ms) bir kez yinelenir.
     */
    private static void fps(IntegratedServer server, String etiket, String sahne, Kamera kam, EfektKalitesi kalite,
            boolean gizli, boolean yuk) {
        ADIMLAR.addAll(olcumAdimlari(server, etiket, sahne, TABAN, kam, kalite, gizli, yuk, false));
    }

    private static List<Adim> olcumAdimlari(IntegratedServer server, String etiket, String sahne, String taban, Kamera kam,
            EfektKalitesi kalite, boolean gizli, boolean yuk, boolean tekrar) {
        String ad = "FPS " + etiket + (tekrar ? " (tekrar)" : "");
        List<Adim> a = new ArrayList<>();
        a.add(new Adim(ad + " kamerası", 5, () -> true, 5, () -> {
            kosulSunucu = false;
            kamera(server, kam);
        }));
        if (yuk) {
            a.add(new Adim(ad + " uçan blok", 20, () -> {
                sunucuda(server, level -> {
                    Kasirga k = kasirga(server);
                    kosulSunucu = k != null && k.ucanBlokSayisi() >= UCAN_EN_AZ;
                });
                return kosulSunucu;
            }, KAPI_EN_COK, () -> {
                if (!kosulSunucu) {
                    DogalAfetler.LOGGER.warn(ON + "{}: uçan blok 60 sn'de {}'a ulaşmadı; yine ölçülüyor", ad, UCAN_EN_AZ);
                }
                kamera(server, kam);
            }));
        }
        a.add(new Adim(ad, 20, GorselOtomasyon::akici, KAPI_EN_COK,
                () -> olcBasla(server, etiket, sahne, taban, kam, kalite, gizli)));
        a.add(new Adim(ad + " son", 100, () -> true, 100, () -> {
            if (!olcBitir(server) && !tekrar) {
                ADIMLAR.addAll(sira + 1, olcumAdimlari(server, etiket, sahne, taban, kam, kalite, gizli, yuk, true));
            }
        }));
        return a;
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

    /** Oyuncu yerinde kalır, kasırganın merkezine bakar ({@link #ROTA_EGIM}); kare satırı sunucu thread'inde yazılır. */
    private static void rotaBak(IntegratedServer server, int no) {
        sunucuda(server, level -> {
            Kasirga k = kasirga(server);
            ServerPlayer o = oyuncu(server);
            if (k == null) {
                DogalAfetler.LOGGER.warn(ON + "rota kare {}: aktif kasırga yok", no);
                return;
            }
            Vec3 m = k.konum();
            float yaw = (float) Math.toDegrees(Math.atan2(-(m.x - o.getX()), m.z - o.getZ()));
            o.teleportTo(level, o.getX(), o.getY(), o.getZ(), yaw, ROTA_EGIM);
            double uzaklik = Math.hypot(m.x - o.getX(), m.z - o.getZ());
            ROTA_UZAKLIK[no - 1] = uzaklik;
            double[] ara = k.araHedef();
            DogalAfetler.LOGGER.info(ON + "rota kare {}: kasırga merkezi {}, oyuncu {}, yatay uzaklık {}, ara hedef ({}, {}) #{}, "
                    + "evre {}", no, BlockPos.containing(m).toShortString(), o.blockPosition().toShortString(),
                    Math.round(uzaklik), ara == null ? "-" : Math.round(ara[0]), ara == null ? "-" : Math.round(ara[1]),
                    k.araHedefNo(), k.durum().evre());
        });
    }

    private static void rotaSenaryosu(Minecraft mc, IntegratedServer server) {
        adim("hazırlık", 0, () -> {
            GorselOtomasyon.cizimAyari();
            for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 6000", "weather clear"}) {
                komut(server, k);
            }
            sunucuda(server, level -> {
                ServerPlayer o = oyuncu(server);
                o.setGameMode(GameType.CREATIVE);
                BlockPos d = level.getSharedSpawnPos();
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, d.getX(), d.getZ());
                o.teleportTo(level, d.getX() + 0.5, y, d.getZ() + 0.5, 0, 0);
            });
            mc.options.hideGui = true;
        });
        adim("S3 başlat", 100, () -> komut(server, "dogalafet start kasirga 3 " + mc.player.getGameProfile().getName()));
        adim("siren", 20, () -> ilerletEger(server, k -> k.asama() == AfetAsamasi.ON_UYARI));
        adim("aktif", 20, () -> ilerletEger(server, k -> k.asama() == AfetAsamasi.SIREN));
        adim("olgun", 20, () -> ilerletEger(server, k -> k.durum().evre() == KasirgaEvresi.DOGUS));
        akiciBekle("rota");
        for (int i = 1; i <= ROTA_KARE; i++) {
            int no = i;
            adim("rota kamera " + no, no == 1 ? 40 : ROTA_ARALIK - 10, () -> rotaBak(server, no));
            adim("rota karesi " + no, 10, () -> kare("rota_" + no));
        }
        adim("rota özeti", 20, () -> sunucuda(server, level -> {
            Kasirga k = kasirga(server);
            DogalAfetler.LOGGER.info(ON + "rota: uzaklık {}/{}/{}, ara hedef {}", Math.round(ROTA_UZAKLIK[0]),
                    Math.round(ROTA_UZAKLIK[1]), Math.round(ROTA_UZAKLIK[2]), k == null ? 0 : k.araHedefNo());
        }));
        adim("stop", 20, () -> komut(server, "dogalafet stop"));
        adim("kapanış", 60, () -> {
            GorselOtomasyon.kareOzetiYaz("kasirga");
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
        });
    }

    private static void senaryo(Minecraft mc, IntegratedServer server) {
        adim("hazırlık", 0, () -> {
            GorselOtomasyon.cizimAyari();
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
                Kasirga k = kasirga(server);
                s5Bas = k != null ? k.konum() : null;
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
        adim("kontrol", 5, () -> yalnizTozGizle(true));
        adim("kontrol karesi", 10, () -> kare("s5_vitrin_kontrol"));
        adim("kontrol bitti", 5, () -> yalnizTozGizle(false));
        adim("vitrin karesi b", 60, () -> kare("s5_vitrin_b"));
        adim("uzak taban kamerası", 10, () -> kamera(server, UZAK));
        adim("uzak taban karesi", 30, () -> kare("s5_taban_uzak"));
        adim("uzak kontrol", 5, () -> yalnizTozGizle(true));
        adim("uzak kontrol karesi", 10, () -> kare("s5_taban_uzak_kontrol"));
        adim("uzak kontrol bitti", 5, () -> yalnizTozGizle(false));
        fps(server, "S5 55 blok DÜŞÜK", "s5_55_dusuk", VITRIN, EfektKalitesi.DUSUK, false, false);
        fps(server, "S5 55 blok YÜKSEK", "s5_55_yuksek", VITRIN, EfektKalitesi.YUKSEK, false, false);
        fps(server, "S5 55 blok ULTRA", "s5_55_ultra", VITRIN, EfektKalitesi.ULTRA, false, false);
        fps(server, "S5 55 blok kasırgasız (çizim yok)", "s5_55_cizimsiz", VITRIN, null, true, false);
        fps(server, "S5 LOD 120 blok YÜKSEK", "s5_lod120_yuksek", new Kamera(120, 14, -6), EfektKalitesi.YUKSEK, false, false);
        fps(server, "S5 LOD 180 blok YÜKSEK", "s5_lod180_yuksek", new Kamera(180, 18, -5), EfektKalitesi.YUKSEK, false, false);
        fps(server, "S5 52 blok YÜKSEK", "s5_52_yuksek", new Kamera(52, 10, -8), EfektKalitesi.YUKSEK, false, true);
        fps(server, "S5 36 blok YÜKSEK", "s5_36_yuksek", new Kamera(36, 8, -12), EfektKalitesi.YUKSEK, false, true);
        fps(server, "S5 20 blok YÜKSEK", "s5_20_yuksek", new Kamera(20, 4, -20), EfektKalitesi.YUKSEK, false, true);
        adim("yakın kamera", 10, () -> kamera(server, YAKIN));
        adim("yakın karesi", 40, () -> kare("yakin"));
        adim("iç kamera", 10, () -> kamera(server, IC));
        adim("iç karesi", 40, () -> kare("ic"));
        fps(server, "S5 5 blok YÜKSEK", "s5_5_yuksek", IC, EfektKalitesi.YUKSEK, false, true);
        fps(server, "S5 5 blok kasırgasız (çizim yok)", "s5_5_cizimsiz", IC, null, true, false);
        adim("stop", 5, () -> {
            sunucuda(server, level -> {
                Kasirga k = kasirga(server);
                s5Son = k != null ? k.konum() : null;
            });
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
        ADIMLAR.addAll(olcumAdimlari(server, "afetsiz taban (stop sonrası, 5 blok noktası)", TABAN, null, null,
                EfektKalitesi.YUKSEK, false, false, false).subList(1, 3));
        adim("S1 yeri", 10, () -> sunucuda(server, level -> {
            // S5 izine dik yönde taze zemin; chunk'lar S1 başlamadan yüklensin diye oyuncu önce oraya, yükseğe taşınır.
            ServerPlayer o = oyuncu(server);
            Vec3 bas = s5Bas != null ? s5Bas : o.position();
            Vec3 son = s5Son != null ? s5Son : o.position();
            double dx = son.x - bas.x;
            double dz = son.z - bas.z;
            double uzunluk = Math.hypot(dx, dz);
            double nx = uzunluk < 1 ? 1 : -dz / uzunluk;
            double nz = uzunluk < 1 ? 0 : dx / uzunluk;
            double x = (bas.x + son.x) / 2 + nx * S1_UZAKLIK;
            double z = (bas.z + son.z) / 2 + nz * S1_UZAKLIK;
            o.teleportTo(level, x, 140, z, o.getYRot(), 0);
            DogalAfetler.LOGGER.info(ON + "S1 yeri {} (S5 izi {} → {})", BlockPos.containing(x, 140, z).toShortString(),
                    BlockPos.containing(bas).toShortString(), BlockPos.containing(son).toShortString());
        }));
        akiciBekle("S1 yeri");
        adim("S1 başlat", 200, () -> {
            komut(server, "dogalafet start kasirga 1 " + mc.player.getGameProfile().getName());
            komut(server, "dogalafet hiz " + YAVAS);
            ilerletEger(server, k -> k.asama() == AfetAsamasi.ON_UYARI);
            ilerletEger(server, k -> k.asama() == AfetAsamasi.SIREN);
            ilerletEger(server, k -> k.durum().evre() == KasirgaEvresi.DOGUS);
        });
        adim("S1 kamerası", 80, () -> {
            kosulSunucu = false;
            kamera(server, VITRIN);
        });
        kosullu("S1 enkaz", 20, () -> {
            sunucuda(server, level -> {
                Kasirga k = kasirga(server);
                kosulSunucu = k != null && k.toplamKaldirilan() > 0;
            });
            return kosulSunucu && IstemciKasirga.gorselEnkazSayisi() > 0;
        }, KAPI_EN_COK, () -> kamera(server, VITRIN));
        adim("S1 karesi", 20, () -> kare("s1_vitrin"));
        adim("S1 olgun doğal akış", 5, () -> {
            kosulSunucu = false;
            komut(server, "dogalafet hiz 1");
        });
        kosullu("S1 olgun bitti", 20, () -> {
            sunucuda(server, level -> {
                Kasirga k = kasirga(server);
                kosulSunucu = k == null || k.durum().evre() != KasirgaEvresi.OLGUN;
            });
            return kosulSunucu;
        }, S1_OLGUN_EN_COK, () -> {
            if (!kosulSunucu) {
                DogalAfetler.LOGGER.warn(ON + "S1 olgun evre {} sn'de bitmedi; ilerletilmeden devam", S1_OLGUN_EN_COK / 20);
            }
            komut(server, "dogalafet hiz " + YAVAS);
        });
        adim("S1 ip kamerası", 60, () -> kamera(server, VITRIN));
        adim("ip karesi", 30, () -> kare("ip"));
        adim("S1 dağılma", 60, () -> {
            sunucuda(server, level -> {
                Kasirga k = kasirga(server);
                s1Son = k != null ? new double[] {k.konum().x, k.konum().z, k.yon()} : null;
            });
            ilerletEger(server, k -> k.durum().evre() == KasirgaEvresi.IP);
        });
        adim("doğal bitiş", 30, () -> {
            ilerletEger(server, k -> k.durum().evre() == KasirgaEvresi.DAGILMA);
            komut(server, "dogalafet hiz 1");
        });
        adim("bitiş kamerası", 30, () -> sunucuda(server, level -> {
            // S1'in son noktasının 10 blok önünden, 19 blok yukarıdan geride kalan yoluna (25 blok geri) bakar.
            ServerPlayer o = oyuncu(server);
            if (s1Son == null) {
                o.teleportTo(level, o.getX(), o.getY() + 14, o.getZ(), o.getYRot(), 50);
                return;
            }
            double yukseklik = 19;
            double cx = s1Son[0] + Math.cos(s1Son[2]) * 10;
            double cz = s1Son[1] + Math.sin(s1Son[2]) * 10;
            double hx = s1Son[0] - Math.cos(s1Son[2]) * 25;
            double hz = s1Son[1] - Math.sin(s1Son[2]) * 25;
            int zemin = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(cx), Mth.floor(cz));
            float yaw = (float) Math.toDegrees(Math.atan2(-(hx - cx), hz - cz));
            float egim = (float) Math.toDegrees(Math.atan2(yukseklik, Math.hypot(hx - cx, hz - cz)));
            o.teleportTo(level, cx, zemin + yukseklik, cz, yaw, egim);
            DogalAfetler.LOGGER.info(ON + "bitiş kamerası S1 yoluna: son nokta {}, bakılan {}",
                    BlockPos.containing(s1Son[0], zemin, s1Son[1]).toShortString(),
                    BlockPos.containing(hx, zemin, hz).toShortString());
        }));
        adim("bitiş karesi", 40, () -> kare("bitis_iz"));
        adim("kapanış", 60, () -> {
            DogalAfetler.LOGGER.info(ON + "bitiş sonrası istemci: kasirga={}, gorsel_kopya={}", IstemciKasirga.kasirgaSayisi(),
                    IstemciKasirga.gorselEnkazSayisi());
            GorselOtomasyon.kareOzetiYaz("kasirga");
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
                DogalAfetler.LOGGER.info(ON + "başlıyor: oyuncu {}, bölüm {}", mc.player.getGameProfile().getName(),
                        ROTA ? "rota" : "hepsi");
                if (ROTA) {
                    rotaSenaryosu(mc, server);
                } else {
                    senaryo(mc, server);
                }
                sira = 0;
                sayac = 0;
            }
            return;
        }
        if (olcum != null && takip != null && ++takipSayac % TAKIP_ARALIGI == 0) {
            kamera(server, takip);
        }
        if (sira >= ADIMLAR.size() || GorselOtomasyon.kareBekliyor()) {
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
}
