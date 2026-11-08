package io.github.kadirberkpolat1.dogalafetler.client.gunes_patlamasi;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciDonem;
import io.github.kadirberkpolat1.dogalafetler.client.gokyuzu.IstemciGokyuzu;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.GokyuzuEgrileri;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAyarlari;
import io.github.kadirberkpolat1.dogalafetler.network.DonemPaketi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKaynagi;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=gunes_patlamasi} ile tek oyunculu dünyada
 * çalışır; üretimde etkisizdir. Gerçek afet komutlarla yürütülür: meteor vitrini kamerasından afetsiz FPS tabanı →
 * ön-uyarı → siren (gece aurora, gündüz büyüyen güneş) → AKTIF 1. gün → kavrulma öncesi (1. gün; kavrulma 3. günden
 * başlar) → 4. gün meteor yağmuru (afetin kaynağıyla yoğun vitrin yağmuru, yavaş çekim) ve aynı kamerada Yüksek, Düşük,
 * Ultra FPS ölçümü (GP17, K-E29 (b); efekt kalitesi çalışma anında değişir, sonra Yüksek'e döner) → krater ve ateş →
 * kavrulma (4. gün sonu, 1. gün karesiyle aynı kamera kaydından) → doğal bitiş: süpernova çökmesi ve parlaması (evre
 * saniyesine bağlı, {@code hiz 0.25} ile ağır çekimde), kabuğu (en görünür ilerlemede, kamera süpernovaya dönük) →
 * güneşsiz → kızıl cüce doğuşu ve gündüzlük. Kamera en yakın ormanın {@value #UZAKLIK} blok batısında, ağaçların
 * {@value #KAMERA_YUKSEKLIGI} blok üstünde uçan yaratıcı oyuncudur (izleyici meteor yağmurunun hedefi olmaz); kavrulma
 * kareleri aynı noktadan {@value #KAVRULMA_YUKSEKLIGI} blok üstten ormana hafif aşağı bakar (ağaçlık ve çimen kadrajda).
 * Her adımın eylemi istemci akıcıyken başlar (zamana bağlı kareler FPS kapısını eylemden önce geçer); kare
 * {@link GorselOtomasyon} A-34 kapısından alınır, satır FPS, TPS, mspt ve evre durumunu yazar. Klavye, fare ve tekerlek
 * girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class GunesPatlamasiOtomasyonu {
    private static final String OTOMASYON = "gunes_patlamasi";
    private static final boolean AKTIF = OTOMASYON.equals(System.getProperty("dogalafetler.otomasyon", ""));
    private static final String ON = "GUNES_OTOMASYON ";
    private static final int UZAKLIK = 40;
    private static final int KAMERA_YUKSEKLIGI = 16;
    private static final int KAVRULMA_YUKSEKLIGI = 6;
    /** Minecraft pitch'inde aşağı pozitiftir: ufuk kadrajın üst üçte birinde, gökyüzü < %35. */
    private static final float KAVRULMA_PITCH = 15f;
    private static final float VITRIN_YAW = -72f;
    private static final float VITRIN_PITCH = -12f;
    /** Vitrin yavaş çekiminin meteor uçuş süresi (tick): varsayılan 180'in 2 katı. */
    private static final int YAVAS_CEKIM = 360;
    private static final int OLCUM_TICK = 100;
    private static final String TABAN = "s5_gunes_taban";
    /**
     * Kabuğun en görünür anı (gökyüzü kalibrasyonu, {@code GokyuzuEgrileri.supernovaHalka/supernovaParlaklik}): yarı boy
     * ≈ 51 birim, ışın ve hale karelerini (≈ 46 / 40) aşar, parlaklık %65; daha erken kabuk halenin içinde kalır, daha geç
     * söner ve kadrajı taşar.
     */
    private static final float KABUK_ILERLEME = 0.15f;

    /** Sabit kamera kaydı (konum, yaw, pitch). */
    private record Kamera(Vec3 konum, float yaw, float pitch) {
    }

    /** Adımın karesinden sonraki FPS ölçümü: kalite adımın başında ayarlanır. */
    private record Olcum(String sahne, EfektKalitesi kademe, String taban) {
    }

    /**
     * Adım: istemci akıcıyken sunucu thread'inde {@code sunucu}, sonra {@code hazir} olunca (ya da {@code enCok} tick'te)
     * kare, ardından varsa ölçüm.
     */
    private record Adim(String ad, Consumer<IntegratedServer> sunucu, BooleanSupplier hazir, String kare, int enCok,
            Olcum olcum) {
        Adim(String ad, Consumer<IntegratedServer> sunucu, BooleanSupplier hazir, String kare, int enCok) {
            this(ad, sunucu, hazir, kare, enCok, null);
        }
    }

    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim = -1;
    private static int sayac;
    private static boolean basladi;
    private static int hazirTick = -1;
    private static int olcumBas = -1;
    private static float enDusukTps = 20f;
    private static volatile boolean sunucuHazir;
    private static volatile Vec3 kameraKonumu;
    private static volatile Kamera kavrulmaKamerasi;
    private static volatile int ucanMeteor;
    private static volatile int eskiUcusSuresi = -1;
    private static volatile int vitrinYagmuru = -1;

    private static final List<Adim> ADIMLAR = List.of(
            new Adim("taban (afetsiz, meteor vitrini kamerası)", s -> {
                komut(s, "time set 2500");
                kamera(s, VITRIN_YAW, VITRIN_PITCH);
            }, () -> sayac >= 40, null, 40, new Olcum(TABAN, EfektKalitesi.YUKSEK, null)),
            new Adim("ön-uyarı", s -> {
                komut(s, "time set 3000", "dogalafet start gunes_patlamasi");
                kamera(s, -90f, -35f);
            }, () -> sayac >= 80, "on_uyari", 200),
            new Adim("siren gece", s -> {
                komut(s, "time set 18000", "dogalafet hiz 3");
                AfetYoneticisi.al(s).asamaIleri();
                kamera(s, 180f, -16f);
            }, () -> evre(GunesEvresi.BUYUYEN) && donem().evreIlerlemesi() >= 0.12f, "siren_aurora", 700),
            new Adim("siren gündüz", s -> {
                komut(s, "time set 3000");
                kamera(s, -90f, -35f);
            }, () -> donem().evreIlerlemesi() >= 0.235f, "siren_gunes", 500),
            new Adim("aktif 1. gün", s -> {
                komut(s, "dogalafet hiz 1");
                AfetYoneticisi y = AfetYoneticisi.al(s);
                if (y.aktifAfet().filter(a -> a.asama() == AfetAsamasi.SIREN).isPresent()) {
                    y.asamaIleri();
                }
            }, () -> sayac >= 120 && afetAsamasi(AfetAsamasi.AKTIF), "aktif_gun1", 400),
            new Adim("kavrulma 1. gün", s -> {
                komut(s, "time set 6000");
                kamera(s, kavrulmaKamerasi);
                kavrulmaSay(s);
            }, () -> sayac >= 60, "kavrulma_gun1", 300),
            new Adim("meteor yağmuru", GunesPatlamasiOtomasyonu::meteorVitrini, () -> sayac >= 60 && ucanMeteor >= 4,
                    "meteor_yagmuru", 600),
            olcumAdimi("s5_gunes_meteor_yuksek", EfektKalitesi.YUKSEK),
            olcumAdimi("s5_gunes_meteor_dusuk", EfektKalitesi.DUSUK),
            olcumAdimi("s5_gunes_meteor_ultra", EfektKalitesi.ULTRA),
            new Adim("krater ve ateş", s -> {
            }, () -> sayac >= 160, "meteor_krater_ates", 300),
            new Adim("kavrulma (hız 60)", s -> {
                yavasCekimiBitir(s);
                komut(s, "time set 6000", "dogalafet hiz 60");
                kamera(s, kavrulmaKamerasi);
            }, () -> sayac >= 300, null, 400),
            new Adim("kavrulma 4. gün sonu", s -> {
                komut(s, "dogalafet hiz 1");
                kavrulmaSay(s);
            }, () -> sayac >= 100, "kavrulma_gun4", 300),
            new Adim("süpernova çökmesi", s -> {
                komut(s, "time set 3000");
                kamera(s, -90f, -35f);
                AfetYoneticisi y = AfetYoneticisi.al(s);
                for (int i = 0; i < 6 && y.aktifAfet().isPresent() && y.donem().gunesEvresi() != GunesEvresi.SUPERNOVA; i++) {
                    y.asamaIleri();
                }
                komut(s, "dogalafet hiz 0.25");
            }, () -> evre(GunesEvresi.SUPERNOVA) && evreSaniyesi() >= 3f, "supernova_cokme", 900),
            new Adim("süpernova parlaması", s -> {
            }, () -> evre(GunesEvresi.SUPERNOVA) && evreSaniyesi() >= GokyuzuEgrileri.SN_COKME_SN + 1.5f,
                    "supernova_parlama", 900),
            new Adim("süpernova kabuğu açılışı (hız 50)", s -> komut(s, "dogalafet hiz 50"),
                    () -> evre(GunesEvresi.SUPERNOVA) && donem().evreIlerlemesi() >= KABUK_ILERLEME, null, 600),
            new Adim("süpernova kabuğu", s -> {
                komut(s, "dogalafet hiz 1");
                gunesBak(s);
            }, () -> sayac >= 40, "supernova_kabuk", 200),
            new Adim("süpernova sönümü (hız 1000)", s -> komut(s, "dogalafet hiz 1000"),
                    () -> evre(GunesEvresi.GUNESSIZ), null, 400),
            new Adim("güneşsiz", s -> {
                komut(s, "dogalafet hiz 1", "time set 6000", "dogalafet donma 100");
                kamera(s, -90f, -60f);
            }, () -> sayac >= 140, "gunessiz", 400),
            new Adim("kızıl cüce doğuşu", s -> {
                komut(s, "time set 23600", "dogalafet kizilcuce baslat", "dogalafet hiz 4");
                kamera(s, -90f, -8f);
            }, () -> evre(GunesEvresi.KIZIL_CUCE) && donem().evreIlerlemesi() >= 0.3f, "kizil_cuce_dogus", 900),
            new Adim("kızıl cüce gündüz", s -> {
                komut(s, "dogalafet hiz 1", "dogalafet asama ileri gunes_zinciri", "time set 3000");
                kamera(s, -90f, -35f);
            }, () -> sayac >= 140 && evre(GunesEvresi.KIZIL_CUCE) && donem().evreIlerlemesi() >= 1f,
                    "kizil_cuce_gunduz", 400),
            // Gökyüzü-2: canlı uzay (iki ardışık kare: yavaş dönüş ve kırpışma), kara delik yakın kadrajı, pulsar.
            new Adim("yıldızlı gök 1", s -> {
                komut(s, "time set 18000");
                kamera(s, -90f, -60f);
            }, () -> sayac >= 60, "uzay_yildiz_1", 200),
            new Adim("yıldızlı gök 2 (10 sn sonra, aynı kamera: yavaş dönüş ~6°, kırpışma)", s -> {
            }, () -> sayac >= 200, "uzay_yildiz_2", 300),
            new Adim("kara delik yakın kadraj", GunesPatlamasiOtomasyonu::karaDelikBak, () -> sayac >= 40,
                    "kara_delik_yakin", 200),
            new Adim("pulsar akşam", s -> {
                karaDelikTakip = -1;
                komut(s, "dogalafet pulsar baslat", "dogalafet asama ileri pulsar", "time set 11600");
                gunesBak(s);
            }, () -> sayac >= 80 && evre(GunesEvresi.PULSAR), "pulsar_aksam", 300),
            new Adim("pulsar kavurma (hız 30, 45 sn)", s -> {
                komut(s, "time set 6000", "dogalafet hiz 30");
                kamera(s, kavrulmaKamerasi);
            }, () -> sayac >= 900, null, 1000),
            new Adim("pulsar yanan dünya", s -> {
                komut(s, "dogalafet hiz 1");
                kavrulmaSay(s);
            }, () -> sayac >= 40, "pulsar_yanan_dunya", 200));

    /** Kara delik yakın kadrajının görüş açısı (derece): halka (çapı ~0,75°) kadrajın ~%15'i olur. */
    private static final double KARA_DELIK_FOV = 6.0;
    /** Yakın kadrajda takip edilen kara delik (-1: yok); kamera kare kapısında beklenirken de her tick ona çevrilir. */
    private static volatile int karaDelikTakip = -1;

    private GunesPatlamasiOtomasyonu() {
    }

    /** Meteor vitrini kamerasında (vitrin yağmuru ve yavaş çekim sürerken) bir kademenin ölçümü. */
    private static Adim olcumAdimi(String sahne, EfektKalitesi kademe) {
        return new Adim("ölçüm " + sahne, s -> {
        }, () -> sayac >= 20, null, 20, new Olcum(sahne, kademe, TABAN));
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
        // Başarım toast'ları (Felaket Başlıyor, Güneşi Atlattım, Süpernova Tanığı) F1'de de çizilir; vitrin karesine girmesin.
        mc.getToasts().clear();
        if (karaDelikTakip >= 0 && mc.player != null && mc.level != null) {
            karaDelikeCevir(mc, karaDelikTakip);
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
            if (a.olcum() != null) {
                kalite(a.olcum().kademe());
            }
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
                GorselOtomasyon.kareIste(OTOMASYON, "gunes_" + a.kare() + ".png", GunesPatlamasiOtomasyonu::kareSatiri);
                return;
            }
        }
        Olcum o = a.olcum();
        if (o == null) {
            sonraki();
        } else if (olcumBas < 0) {
            if (GorselOtomasyon.eylemKapisi(OTOMASYON, o.sahne() + " ölçümü")) {
                GorselOtomasyon.olcumBasla(OTOMASYON, o.sahne(), o.taban());
                olcumBas = sayac;
                int ucus = MeteorAyarlari.ucusSuresi();
                DogalAfetler.LOGGER.info(ON + "ölçüm {}: meteor uçuş süresi {} tick (vitrin yavaş çekimi {}), uçan meteor {}",
                        o.sahne(), ucus, ucus == YAVAS_CEKIM ? "açık" : "kapalı", ucanMeteor);
            }
        } else if (sayac - olcumBas >= OLCUM_TICK) {
            GorselOtomasyon.olcumBitir();
            kalite(null);
            sonraki();
        }
    }

    private static void kareSatiri(GorselOtomasyon.Kare k) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        DonemPaketi d = IstemciDonem.durum();
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, TPS {}, mspt {}, evre {}, ilerleme {}, evre sn {}, uçan meteor {}, "
                        + "parçacık {}, kuruluk basamağı {}, kapı beklemesi {} sn)", k.dosya(), k.fps(),
                server == null ? "?" : String.format(Locale.ROOT, "%.1f", tps(server)),
                server == null ? "?" : String.format(Locale.ROOT, "%.1f", server.getAverageTickTime()), d.gunesEvresi().kimlik(),
                String.format(Locale.ROOT, "%.3f", d.evreIlerlemesi()), String.format(Locale.ROOT, "%.1f", evreSaniyesi()),
                ucanMeteor, mc.particleEngine.countParticles(), KavrulmaRengi.basamak(),
                String.format(Locale.ROOT, "%.1f", k.beklemeSn()));
    }

    /**
     * Kara delik takibindeyken dünya çiziminin görüş açısı {@value #KARA_DELIK_FOV}° (oyuncunun FOV ayarı değişmez, el
     * çizimine dokunulmaz).
     */
    @SubscribeEvent
    public static void gorusAcisi(ViewportEvent.ComputeFov olay) {
        if (AKTIF && karaDelikTakip >= 0 && olay.usedConfiguredFov()) {
            olay.setFOV(KARA_DELIK_FOV);
        }
    }

    /**
     * Göğün en yüksekteki kara deliği seçilir, görüş açısı {@value #KARA_DELIK_FOV}° (yakın kadraj); kamera her istemci
     * tick'inde ona çevrilir (yavaş dönüşle kayar; pc2'de kare kapısı 60 sn bekleyebilir). Yön çizimle aynı hesaptan.
     */
    private static void karaDelikBak(IntegratedServer s) {
        Minecraft.getInstance().execute(() -> {
            Minecraft mc = Minecraft.getInstance();
            int en = 0;
            for (int i = 1; i < 3; i++) {
                if (IstemciGokyuzu.karaDelikYonu(i, mc.level, 1f).y > IstemciGokyuzu.karaDelikYonu(en, mc.level, 1f).y) {
                    en = i;
                }
            }
            karaDelikTakip = en;
            float[] aci = karaDelikeCevir(mc, en);
            DogalAfetler.LOGGER.info(ON + "kara delik kadrajı: {}. kara delik, yaw {}, pitch {} (takip)", en,
                    String.format(Locale.ROOT, "%.1f", aci[0]), String.format(Locale.ROOT, "%.1f", aci[1]));
        });
    }

    /** İstemci oyuncusunu (kamera) {@code i}. kara deliğe çevirir; ara kare yumuşatması olmasın diye önceki açı da. */
    private static float[] karaDelikeCevir(Minecraft mc, int i) {
        org.joml.Vector3f y = IstemciGokyuzu.karaDelikYonu(i, mc.level, 1f);
        float yaw = (float) Math.toDegrees(Math.atan2(-y.x, y.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1f, Math.min(1f, y.y))));
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        return new float[] {yaw, pitch};
    }

    private static void sonraki() {
        adim++;
        sayac = 0;
        basladi = false;
        hazirTick = -1;
        olcumBas = -1;
    }

    /**
     * Efekt kalitesini çalışma anında geçersiz kılar (config dosyasına yazılmaz, K-E28); {@code null} kullanıcının değerine döner.
     */
    private static void kalite(EfektKalitesi k) {
        IstemciAyarlari.otomasyonKalitesi(k);
        DogalAfetler.LOGGER.info(ON + "KALITE istenen={} etkin={}", k == null ? "kullanıcı" : k, IstemciAyarlari.efektKalitesi());
    }

    private static DonemPaketi donem() {
        return IstemciDonem.durum();
    }

    private static boolean evre(GunesEvresi e) {
        return donem().gunesEvresi() == e;
    }

    private static float evreSaniyesi() {
        return GunesIstemciOlaylari.evreSaniyesi(donem(), 0f);
    }

    private static boolean afetAsamasi(AfetAsamasi asama) {
        return IstemciAfetDurumu.aktif(AfetTuru.GUNES_PATLAMASI).filter(p -> p.asama() == asama).isPresent();
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
     * Kamera süpernovaya (güneşin yerine) döner. Gök çizicisi güneşi doğu-batı düzleminde tepeden
     * {@code getTimeOfDay · 360°} döndürür; sin &lt; 0 doğu (yaw −90) yarısıdır.
     */
    private static void gunesBak(IntegratedServer s) {
        double aci = Math.toRadians(s.overworld().getTimeOfDay(1f) * 360.0);
        float yukseklik = (float) Math.toDegrees(Math.asin(Math.cos(aci)));
        float yaw = Math.sin(aci) < 0 ? -90f : 90f;
        kamera(s, yaw, -yukseklik);
        DogalAfetler.LOGGER.info(ON + "kabuk kamerası: yaw {}, güneş yüksekliği {}°", yaw,
                String.format(Locale.ROOT, "%.1f", yukseklik));
    }

    /** Sunucu thread'inde: kurallar (diğer afetler kapalı, güneş açık), sabah, en yakın orman, uçan yaratıcı kamera. */
    private static void hazirla(IntegratedServer s) {
        komut(s, "gamerule sendCommandFeedback false", "gamerule doDaylightCycle false", "gamerule doWeatherCycle false",
                "gamerule doMobSpawning false", "gamerule kasirgaAfeti false", "gamerule tsunamiAfeti false",
                "gamerule depremAfeti false", "gamerule asitYagmuruAfeti false", "gamerule sinkholeAfeti false",
                "gamerule ormanYanginiAfeti false", "gamerule ayCarpmasiAfeti false", "weather clear", "time set 3000",
                "dogalafet sifirla");
        ServerLevel level = s.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        Pair<BlockPos, Holder<Biome>> bulunan = level.findClosestBiome3d(h -> h.is(Biomes.FOREST) || h.is(Biomes.BIRCH_FOREST)
                || h.is(Biomes.FLOWER_FOREST), dogus, 6400, 32, 64);
        BlockPos merkez = bulunan == null ? dogus : bulunan.getFirst();
        int x = merkez.getX() - UZAKLIK;
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, merkez.getZ());
        kameraKonumu = new Vec3(x + 0.5, ust + KAMERA_YUKSEKLIGI, merkez.getZ() + 0.5);
        kavrulmaKamerasi = new Kamera(new Vec3(x + 0.5, ust + KAVRULMA_YUKSEKLIGI, merkez.getZ() + 0.5), -90f,
                KAVRULMA_PITCH);
        ServerPlayer o = oyuncu(s);
        o.setGameMode(GameType.CREATIVE);
        o.getAbilities().flying = true;
        o.onUpdateAbilities();
        kamera(s, -90f, -35f);
        DogalAfetler.LOGGER.info(ON + "orman {} ({}), kamera {}, kavrulma kamerası {}", merkez.toShortString(),
                bulunan == null ? "bulunamadı (doğuş)"
                        : bulunan.getSecond().unwrapKey().map(k -> k.location().toString()).orElse("?"),
                kameraKonumu, kavrulmaKamerasi);
        sunucuHazir = true;
    }

    /**
     * 4. gün (asama ileri ×3), sabah güneşi kadrajın üst köşesinde; afetin dakikalık yağmuruna ek olarak afetin kendi
     * kaynağıyla kısa, yoğun bir vitrin yağmuru (oyuncu başına dakikada 40, %15 büyük) ve yavaş çekim (düşme 0,6).
     */
    private static void meteorVitrini(IntegratedServer s) {
        AfetYoneticisi y = AfetYoneticisi.al(s);
        for (int i = 0; i < 3; i++) {
            y.asamaIleri();
        }
        komut(s, "time set 2500");
        kamera(s, VITRIN_YAW, VITRIN_PITCH);
        eskiUcusSuresi = MeteorAyarlari.ucusSuresi();
        MeteorAyarlari.ucusSuresiAyarla(YAVAS_CEKIM);
        long afetNo = y.aktifAfet().map(a -> a.baglam().baslangicSaati()).orElse(0L);
        vitrinYagmuru = MeteorAPI.yagmur(s.overworld(), List.of(oyuncu(s)), 900, 40, 0.15,
                YanginKaynagi.afet(AfetTuru.GUNES_PATLAMASI, afetNo));
        DogalAfetler.LOGGER.info(ON + "vitrin yağmuru {} (afetNo {}, gün {})", vitrinYagmuru, afetNo,
                y.aktifAfet().map(a -> a.altAsama() + 1).orElse(-1));
    }

    private static void yavasCekimiBitir(IntegratedServer s) {
        if (eskiUcusSuresi > 0) {
            MeteorAyarlari.ucusSuresiAyarla(eskiUcusSuresi);
        }
        if (vitrinYagmuru >= 0) {
            MeteorAPI.yagmurDurdur(s.overworld(), vitrinYagmuru);
        }
    }

    /** Oyuncunun 64 blok çevresinde güneşe açık kaba toprak sayısı (kavrulmanın kanıtı). */
    private static void kavrulmaSay(IntegratedServer s) {
        ServerLevel level = s.overworld();
        BlockPos m = oyuncu(s).blockPosition();
        int kaba = 0;
        for (int dx = -64; dx <= 64; dx++) {
            for (int dz = -64; dz <= 64; dz++) {
                int x = m.getX() + dx;
                int z = m.getZ() + dz;
                if (level.hasChunk(x >> 4, z >> 4) && level.getBlockState(new BlockPos(x,
                        level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1, z)).is(Blocks.COARSE_DIRT)) {
                    kaba++;
                }
            }
        }
        DogalAfetler.LOGGER.info(ON + "kavrulma: 129x129 alanda yüzeyde {} kaba toprak (gün {})", kaba,
                AfetYoneticisi.al(s).aktifAfet().map(a -> a.altAsama() + 1).orElse(-1));
    }
}
