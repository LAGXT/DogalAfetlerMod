package io.github.kadirberkpolat1.dogalafetler.client.asit_yagmuru;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.config.AyarKaydi;
import io.github.kadirberkpolat1.dogalafetler.config.EfektKalitesi;
import io.github.kadirberkpolat1.dogalafetler.config.IstemciAyarlari;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.esya.EsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ConfigTracker;
import net.minecraftforge.fml.config.ModConfig;

/**
 * Görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=asit_orman} ile tek oyunculu dünyada
 * çalışır; üretimde etkisizdir. Açılışta vsync kapanır, {@code maxFps} 260 olur (FPS tavana yapışmaz, AOi-3). Yaratıcı kipte
 * uçan kamera en yakın ormanın tepesinde: asit yağmuru S1 aktif karesi, sonra S5 ile aynı kamerada afetsiz taban ölçümü,
 * ön-uyarı (elde Hava Radarı, arayüz açık), aynı yerden ufka bakan (bulut tavanı kadrajda) ve aşağı bakan siren kareleri,
 * aktif, vitrin (yoğun yağış, aşınan yapraklar), S5 ölçümü, yere yakın kare (göz aşınan bloğun {@value #YAKIN_GOZ} blok
 * üstünde, ona {@value #YAKIN_UZAKLIK} blok uzakta: sıçrama, cızlama dumanı ve çatlak örtüsü; AOi-1) ve bitiş kareleri.
 * Ardından alacakaranlıkta orman yangını S1 ve S5 aynı dizide, kamera yangın merkezinin 34 blok rüzgâr üstünde, 10 blok
 * yukarıda; S5'te yangın başlamadan aynı kamerada taban ölçülür, vitrinden sonra efekt kalitesi Düşük, Yüksek ve Ultra'ya
 * zorlanıp her biri ölçülür (istemci config değeri, koşu sonunda geri konur). Orman bitiş karesi (K-E47 eki,
 * inceleme-matris-5 P2-4) zamana değil yangının sönmesine bağlıdır: afet bittikten ve ortak yangın yöneticisinde yanan hücre
 * ile bekleyen tutuşturma kalmadıktan sonra ({@value #SONME_EN_COK} tick'te sönmezse söndürülür, WARN), yanık alanın
 * ağırlık merkezine ({@link #YANIK_UZAKLIK} blok rüzgâr üstünden, zeminin {@link #YANIK_YUKSEKLIK} blok üstünden, merkeze
 * bakan) ayrı kameradan alınır; ağırlık merkezi S5 aktifken istemci paketinin sıcak karelerinden ({@code h}) toplanır.
 * Ardından yanmış sahne aynı kamerada bilgi olarak ölçülür. Ölçümler K-E29 (b) {@code OLCUM} satırıdır ({@link GorselOtomasyon#olcumBasla}; FPS kapısından sonra
 * {@value #OLCUM_TICK} tick); ek satır TPS'i ve yanan hücreyi yazar. Kareler A-34 kapısından geçer
 * ({@link GorselOtomasyon#kareIste}, istekten sonra en az 20 yeni kare); kare beklenirken adım motoru durur. Ön-uyarı
 * penceresi {@code hiz 20} ile kısaltılır, radar karesinden önce hız 1'e döner. Kare satırı FPS'i, parçacık sayısını ve
 * kamerayı yazar. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır. {@code -Ddogalafetler.otomasyon.bolum=uzak} yalnız
 * AF2-01 karelerini alır ({@link #uzakDizisi}): çölde, asit merkezinden {@value #UZAK_MESAFE} blok uzakta yağmur ve maruziyet.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class AsitOrmanOtomasyonu {
    private static final boolean AKTIF = "asit_orman".equals(System.getProperty("dogalafetler.otomasyon", ""));
    /** {@code uzak} (AF2-01): yalnız çölde, merkezden 320 blok uzakta asit yağmuru ve maruziyet kareleri. */
    private static final boolean UZAK = "uzak".equals(System.getProperty("dogalafetler.otomasyon.bolum", "hepsi"));
    /** Uzak kipte asidin başlatıldığı nokta çöldeki oyuncudan bu kadar doğuda. */
    private static final int UZAK_MESAFE = 320;
    private static final String ON = "ASIT_ORMAN_OTOMASYON ";
    private static final double KAMERA_UZAKLIGI = 34;
    private static final double YAKIN_GOZ = 4;
    private static final double YAKIN_UZAKLIK = 8;
    private static final float UFUK_PITCH = -12F;
    private static final int OLCUM_TICK = 100;
    /** Orman bitiş kamerası: yanık merkezine yatay uzaklık ve zeminden yükseklik (tasarım notu "Afet sonrası iz"). */
    private static final double YANIK_UZAKLIK = 24;
    private static final double YANIK_YUKSEKLIK = 8;
    /** Bitişten sonra yangının kendiliğinden sönmesi için en uzun bekleme (tick). */
    private static final int SONME_EN_COK = 4800;

    /** Bir adım her tick çağrılır ({@code t} adımın kaçıncı tick'i); {@code true} dönünce sıradakine geçilir. */
    private interface Adim {
        boolean calis(Minecraft mc, IntegratedServer s, int t);
    }

    private record Kamera(double x, double y, double z, float yaw, float pitch) {
    }

    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static int tus;
    private static int fare;
    private static int teker;
    private static int adim;
    private static int sayac;
    private static volatile boolean hazir;
    private static volatile boolean onUyari;
    private static EfektKalitesi ilkKalite;
    /** Asit kamerası (sunucu thread'inde yazılıp okunur). */
    private static Kamera asitKamerasi;
    /** S5 orman yangınının merkezi ({@link #ormanKamera}'da, sunucu thread'i). */
    private static BlockPos ormanMerkezi;
    /** Yanık ağırlık merkezi: istemci thread'inde sıcak karelerden toplanır (x·n, z·n, n). */
    private static boolean yanikIzle;
    private static double yanikX;
    private static double yanikZ;
    private static long yanikN;
    private static volatile boolean sondu;
    /** Uzak kipte asit merkezi (sunucu thread'inde yazılır, istemcide okunur). */
    private static volatile BlockPos uzakMerkez;

    static {
        ADIMLAR.add(bekle(100));
        ADIMLAR.add(AsitOrmanOtomasyonu::cizimAyari);
        if (UZAK) {
            uzakDizisi();
        } else {
            ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::hazirla));
            ADIMLAR.add(bekle(240));
            ADIMLAR.add(arayuz(false));
            dizi(AfetTuru.ASIT_YAGMURU, "asit", 300, 300, 400, 60, AsitOrmanOtomasyonu::asitKamera);
            ADIMLAR.add(sunucu(s -> komut(s, "time set 13000")));
            dizi(AfetTuru.ORMAN_YANGINI, "orman", 400, 120, 400, 500, AsitOrmanOtomasyonu::ormanKamera);
            ADIMLAR.add(kalite(null));
        }
    }

    private AsitOrmanOtomasyonu() {
    }

    /**
     * S1 aktif karesi; sonra S5: aynı kamerada afetsiz taban (ön-uyarıda, yağış ya da yangın yokken), ön-uyarı, siren, aktif,
     * vitrin, S5 ölçümü ve bitiş. Asitte ufka bakan siren karesi, config kademesinde ölçüm ve yere yakın kare; ormanda
     * Düşük/Yüksek/Ultra ölçümleri ve durduktan sonra yanmış sahnede bilgi ölçümü.
     */
    private static void dizi(AfetTuru tur, String ad, int s1, int aktif, int vitrin, int bitis,
            Consumer<IntegratedServer> kamera) {
        boolean asit = tur == AfetTuru.ASIT_YAGMURU;
        String taban = "s5_" + ad + "_taban";
        ADIMLAR.add(sunucu(s -> baslat(s, tur, 1)));
        ADIMLAR.add(sunucu(kamera));
        ADIMLAR.add(sunucu(s -> ileri(s, AfetAsamasi.SIREN)));
        ADIMLAR.add(sunucu(s -> ileri(s, AfetAsamasi.AKTIF)));
        ADIMLAR.add(bekle(s1));
        ADIMLAR.add(sunucu(s -> olcum(s, ad + "_s1")));
        ADIMLAR.add(kare(ad + "_s1"));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::dur));
        ADIMLAR.add(sunucu(s -> baslat(s, tur, 5)));
        ADIMLAR.add(sunucu(kamera));
        olc(taban, null, null);
        ADIMLAR.add(sunucu(s -> olcum(s, ad + "_taban")));
        ADIMLAR.add(kare(ad + "_taban"));
        ADIMLAR.add(sunucu(s -> AfetYoneticisi.al(s).hizAyarla(20)));
        ADIMLAR.add(onUyariBekle());
        ADIMLAR.add(sunucu(s -> AfetYoneticisi.al(s).hizAyarla(1)));
        ADIMLAR.add(arayuz(true));
        ADIMLAR.add(bekle(60));
        ADIMLAR.add(kare(ad + "_on_uyari_radar"));
        ADIMLAR.add(arayuz(false));
        ADIMLAR.add(sunucu(s -> ileri(s, AfetAsamasi.SIREN)));
        if (asit) {
            ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::ufukKamera));
            ADIMLAR.add(bekle(100));
            ADIMLAR.add(kare(ad + "_siren_ufuk"));
            ADIMLAR.add(sunucu(kamera));
            ADIMLAR.add(bekle(20));
        } else {
            ADIMLAR.add(bekle(120));
        }
        ADIMLAR.add(kare(ad + "_siren"));
        ADIMLAR.add(sunucu(s -> ileri(s, AfetAsamasi.AKTIF)));
        if (!asit) {
            ADIMLAR.add(yanikIzi(true));
        }
        ADIMLAR.add(bekle(aktif));
        ADIMLAR.add(kare(ad + "_aktif"));
        ADIMLAR.add(bekle(vitrin));
        ADIMLAR.add(sunucu(s -> olcum(s, ad + "_vitrin")));
        ADIMLAR.add(kare(ad + "_vitrin"));
        if (asit) {
            ADIMLAR.add(sunucu(s -> oyunKipi(s, GameType.SURVIVAL)));
            ADIMLAR.add(arayuz(true));
            ADIMLAR.add(bekle(30));
            ADIMLAR.add((mc, s, t) -> {
                DogalAfetler.LOGGER.info(ON + "maruziyet: istemci koşulu {}, ekran efekti gücü {}",
                        IstemciAsitYagmuru.maruzMu(mc.level, mc.player),
                        String.format(Locale.ROOT, "%.2f", AsitEkranEfekti.maruziyet()));
                return true;
            });
            ADIMLAR.add(kare(ad + "_maruz"));
            ADIMLAR.add(arayuz(false));
            ADIMLAR.add(sunucu(s -> oyunKipi(s, GameType.CREATIVE)));
            olc("s5_" + ad + "_yuksek", null, taban);
            ADIMLAR.add(sunucu(s -> olcum(s, ad + "_s5_yuksek")));
            ADIMLAR.add(yakinKamera());
            ADIMLAR.add(bekle(60));
            ADIMLAR.add(kare(ad + "_s5_yakin"));
            ADIMLAR.add(sunucu(kamera));
            ADIMLAR.add(bekle(20));
        } else {
            for (EfektKalitesi k : new EfektKalitesi[] {EfektKalitesi.DUSUK, EfektKalitesi.YUKSEK, EfektKalitesi.ULTRA}) {
                String kademe = k.name().toLowerCase(Locale.ROOT);
                String e = ad + "_s5_" + kademe;
                ADIMLAR.add(kalite(k));
                ADIMLAR.add(bekle(100));
                olc("s5_" + ad + "_" + kademe, k, taban);
                ADIMLAR.add(sunucu(s -> olcum(s, e)));
                ADIMLAR.add(kare(e));
            }
            ADIMLAR.add(kalite(null));
        }
        ADIMLAR.add(sunucu(s -> ileri(s, AfetAsamasi.BITIS)));
        if (asit) {
            ADIMLAR.add(bekle(bitis));
        } else {
            ADIMLAR.add(sonmeBekle());
            ADIMLAR.add(yanikIzi(false));
            ADIMLAR.add(yanikKamera());
            ADIMLAR.add(bekle(bitis));
        }
        ADIMLAR.add(sunucu(s -> olcum(s, ad + "_bitis")));
        ADIMLAR.add(kare(ad + "_bitis"));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::dur));
        if (!asit) {
            ADIMLAR.add(bekle(100));
            olc("s5_" + ad + "_sonra", null, null);
            ADIMLAR.add(sunucu(s -> olcum(s, ad + "_sonra")));
        }
    }

    /**
     * AF2-01 uzak kip: S5 asit çöldeki oyuncunun {@value #UZAK_MESAFE} blok doğusunda (konuma, hedefsiz) başlar; yağmur her
     * yerde yağdığından çölde de görünür (vanilla çölde yağdırmaz). {@code asit_uzak} yaratıcıda (hasarsız), {@code asit_hud_uzak}
     * hayatta kalmada ve arayüz açık (maruziyet vinyeti ve damla izleri).
     */
    private static void uzakDizisi() {
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::uzakHazirla));
        ADIMLAR.add(bekle(240));
        ADIMLAR.add(arayuz(false));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::uzakBaslat));
        ADIMLAR.add(sunucu(s -> ileri(s, AfetAsamasi.SIREN)));
        ADIMLAR.add(sunucu(s -> ileri(s, AfetAsamasi.AKTIF)));
        ADIMLAR.add(bekle(300));
        ADIMLAR.add(uzakDurum("asit_uzak"));
        ADIMLAR.add(kare("asit_uzak"));
        ADIMLAR.add(sunucu(s -> oyunKipi(s, GameType.SURVIVAL)));
        ADIMLAR.add(arayuz(true));
        ADIMLAR.add(bekle(60));
        ADIMLAR.add(uzakDurum("asit_hud_uzak"));
        ADIMLAR.add(kare("asit_hud_uzak"));
        ADIMLAR.add(arayuz(false));
        ADIMLAR.add(sunucu(s -> oyunKipi(s, GameType.CREATIVE)));
        ADIMLAR.add(sunucu(AsitOrmanOtomasyonu::dur));
    }

    /** Kare öncesi durum satırı (istemci thread'i; sunucu yağmuru ve can sunucu nesnesinden okunur, yalnız log). */
    private static Adim uzakDurum(String ad) {
        return (mc, s, t) -> {
            BlockPos m = uzakMerkez;
            double mesafe = m == null ? -1 : Math.hypot(mc.player.getX() - m.getX() - 0.5, mc.player.getZ() - m.getZ() - 0.5);
            String biyom = mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(k -> k.location().toString())
                    .orElse("?");
            DogalAfetler.LOGGER.info(ON + String.format(Locale.ROOT, "uzak: %s merkeze %.0f blok, biyom %s, asit yoğunluğu %.2f, "
                    + "vanilla yağmur düzeyi %.2f, sunucu yağmuru %b, maruz %b, ekran efekti %.2f, can %.1f", ad, mesafe,
                    biyom, IstemciAsitYagmuru.yogunluk(), mc.level.getRainLevel(1f), s.overworld().isRaining(),
                    IstemciAsitYagmuru.maruzMu(mc.level, mc.player), AsitEkranEfekti.maruziyet(),
                    oyuncu(s).getHealth()));
            return true;
        };
    }

    private static Adim bekle(int tick) {
        return (mc, s, t) -> t >= tick;
    }

    private static Adim arayuz(boolean acik) {
        return (mc, s, t) -> {
            mc.options.hideGui = !acik;
            return true;
        };
    }

    /** AOi-3: ortak çizim ayarı (vsync kapalı, {@code maxFps} 260); taban FPS tavana yapışmaz. */
    private static boolean cizimAyari(Minecraft mc, IntegratedServer s, int t) {
        GorselOtomasyon.cizimAyari();
        return true;
    }

    /**
     * K-E29 (b) ölçüm penceresi: FPS kapısı (ışınlamadan sonra en az 40 tick ve FPS ≥ {@value GorselOtomasyon#AKICI_FPS};
     * {@value GorselOtomasyon#KAPI_SN} sn'de sağlanmazsa WARN ile devam), {@value #OLCUM_TICK} tick, {@code OLCUM} satırı.
     * {@code kademe} {@code null} ise istemci ayarı; {@code taban} aynı kameranın afetsiz sahnesi ya da {@code null}.
     */
    private static void olc(String sahne, EfektKalitesi kademe, String taban) {
        ADIMLAR.add((mc, s, t) -> t >= 40 && GorselOtomasyon.eylemKapisi("asit_orman", sahne + " ölçümü"));
        ADIMLAR.add((mc, s, t) -> {
            GorselOtomasyon.olcumBasla("asit_orman", sahne, kademe, taban);
            return true;
        });
        ADIMLAR.add(bekle(OLCUM_TICK));
        ADIMLAR.add((mc, s, t) -> {
            GorselOtomasyon.olcumBitir();
            return true;
        });
    }

    /** Efekt kalitesini istemci config değeriyle zorlar ({@code null}: koşu başındaki değer); eski parçacıklar silinir. */
    private static Adim kalite(EfektKalitesi kalite) {
        return (mc, s, t) -> {
            ForgeConfigSpec.EnumValue<EfektKalitesi> deger = kaliteDegeri();
            if (ilkKalite == null) {
                ilkKalite = deger.get();
            }
            deger.set(kalite == null ? ilkKalite : kalite);
            mc.particleEngine.setLevel(mc.level);
            DogalAfetler.LOGGER.info(ON + "kalite {} (parçacıklar silindi)", deger.get());
            return true;
        };
    }

    @SuppressWarnings("unchecked")
    private static ForgeConfigSpec.EnumValue<EfektKalitesi> kaliteDegeri() {
        ModConfig config = ConfigTracker.INSTANCE.fileMap().get(AyarKaydi.ISTEMCI_DOSYASI);
        return (ForgeConfigSpec.EnumValue<EfektKalitesi>) ((ForgeConfigSpec) config.getSpec()).getValues()
                .get("gorsel.efektKalitesi");
    }

    /** Sunucu thread'inde bir iş; bitince sıradaki adım. */
    private static Adim sunucu(Consumer<IntegratedServer> is) {
        return (mc, s, t) -> {
            if (t == 1) {
                hazir = false;
                s.execute(() -> {
                    is.accept(s);
                    hazir = true;
                });
                return false;
            }
            return hazir;
        };
    }

    /** Hızlandırılmış ön-uyarıda radarın göreceği normal pencere başlayana kadar (en çok 1200 tick). */
    private static Adim onUyariBekle() {
        return (mc, s, t) -> {
            if (t == 1) {
                onUyari = false;
            }
            if (t % 5 == 0) {
                s.execute(() -> onUyari = AfetYoneticisi.al(s).aktifAfet().map(Afet::normalOnUyariBasladi).orElse(true));
            }
            if (t > 1200) {
                DogalAfetler.LOGGER.warn(ON + "normal ön-uyarı 1200 tick'te başlamadı");
                return true;
            }
            return onUyari;
        };
    }

    /** A-34 kare isteği (istekten sonra en az 20 yeni kare); satır kare alındığı andaki durumu yazar. */
    private static Adim kare(String ad) {
        return (mc, s, t) -> {
            GorselOtomasyon.kareIste("asit_orman", "ao_" + ad + ".png", 20, k -> DogalAfetler.LOGGER.info(ON
                    + "kare {} (adım {}, FPS {}, yeni kare {}, parçacık {}, asit yoğunluğu {}, kalite {}, kamera {})",
                    k.dosya(), adim, k.fps(), k.yeniKare(), mc.particleEngine.countParticles(),
                    String.format(Locale.ROOT, "%.2f", IstemciAsitYagmuru.yogunluk()), IstemciAyarlari.efektKalitesi(),
                    String.format(Locale.ROOT, "%.1f %.1f %.1f yaw %.0f pitch %.0f", mc.player.getX(), mc.player.getY(),
                            mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot())));
            return true;
        };
    }

    /** Yanık ağırlık merkezini toplamaya başlar ({@code true}; sıfırlar) ya da bırakır. */
    private static Adim yanikIzi(boolean acik) {
        return (mc, s, t) -> {
            if (acik) {
                yanikX = 0;
                yanikZ = 0;
                yanikN = 0;
            }
            yanikIzle = acik;
            return true;
        };
    }

    /** Sıcak karelerden ({@code h}: x, z, yanan hücre üçlüleri) ağırlık merkezi; istemci tick'inde saniyede bir. */
    private static void yanikTopla() {
        IstemciAfetDurumu.aktif(AfetTuru.ORMAN_YANGINI).ifPresent(p -> {
            int[] h = p.ozel().getIntArray("h");
            for (int i = 0; i + 2 < h.length; i += 3) {
                yanikX += (double) h[i] * h[i + 2];
                yanikZ += (double) h[i + 1] * h[i + 2];
                yanikN += h[i + 2];
            }
        });
    }

    /**
     * Yangın sönene kadar: afet bitti ve ortak yöneticide yanan hücre ile bekleyen alan tutuşturması yok. Zamana bağlı değil;
     * {@value #SONME_EN_COK} tick'te sönmezse WARN ile söndürülür (kare yine alınır, satırdan okunur).
     */
    private static Adim sonmeBekle() {
        return (mc, s, t) -> {
            if (t == 1) {
                sondu = false;
            }
            if (t % 20 == 0) {
                s.execute(() -> {
                    ServerLevel level = s.overworld();
                    int yanan = YanginYoneticisi.aktifSayi(level);
                    boolean bitti = AfetYoneticisi.al(s).aktifAfet().isEmpty() && yanan == 0
                            && YanginYoneticisi.bekleyenAlanSayisi(level) == 0;
                    if (bitti && !sondu) {
                        DogalAfetler.LOGGER.info(ON + "yangın söndü: afet bitti, yanan hücre 0");
                    }
                    sondu = bitti;
                });
            }
            if (sondu) {
                return true;
            }
            if (t >= SONME_EN_COK) {
                s.execute(() -> {
                    DogalAfetler.LOGGER.warn(ON + "BITIS-KAPISI yangın {} tick'te sönmedi (yanan hücre {}); söndürülüp kare alınıyor",
                            SONME_EN_COK, YanginYoneticisi.aktifSayi(s.overworld()));
                    dur(s);
                });
                return true;
            }
            return false;
        };
    }

    /** Yanık merkezi istemcide hesaplanır, kamera sunucu thread'inde kurulur. */
    private static Adim yanikKamera() {
        return (mc, s, t) -> {
            if (t == 1) {
                double[] hedef = yanikN > 0 ? new double[] {yanikX / yanikN, yanikZ / yanikN, yanikN} : null;
                hazir = false;
                s.execute(() -> {
                    yanikKameraKur(s, hedef);
                    hazir = true;
                });
                return false;
            }
            return hazir;
        };
    }

    /** Yere yakın asit kamerası; hedef istemcinin bildiği aşınan bloklardan oyuncuya en yakını (ışınlama sunucuda). */
    private static Adim yakinKamera() {
        return (mc, s, t) -> {
            if (t == 1) {
                long[] asinan = IstemciAfetDurumu.aktif(AfetTuru.ASIT_YAGMURU)
                        .map(p -> p.ozel().getLongArray("a")).orElse(new long[0]);
                BlockPos enYakin = null;
                double uzaklik = Double.MAX_VALUE;
                for (long a : asinan) {
                    BlockPos b = BlockPos.of(a);
                    double d = mc.player.distanceToSqr(b.getX() + 0.5, b.getY() + 1, b.getZ() + 0.5);
                    if (d < uzaklik) {
                        uzaklik = d;
                        enYakin = b;
                    }
                }
                BlockPos hedef = enYakin;
                hazir = false;
                s.execute(() -> {
                    yakinKameraKur(s, hedef, asinan.length);
                    hazir = true;
                });
                return false;
            }
            return hazir;
        };
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
        IntegratedServer s = mc.getSingleplayerServer();
        if (mc.player == null || mc.level == null || s == null) {
            return;
        }
        if (yanikIzle && mc.level.getGameTime() % 20 == 0) {
            yanikTopla();
        }
        if (GorselOtomasyon.kareBekliyor()) {
            return;
        }
        if (adim < ADIMLAR.size()) {
            if (ADIMLAR.get(adim).calis(mc, s, ++sayac)) {
                adim++;
                sayac = 0;
            }
        } else if (adim == ADIMLAR.size()) {
            adim++;
            GorselOtomasyon.kareOzetiYaz("asit_orman");
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
        }
    }

    // ---------------------------------------------------------------- sunucu thread'i

    private static ServerPlayer oyuncu(IntegratedServer s) {
        return s.getPlayerList().getPlayers().get(0);
    }

    private static void komut(IntegratedServer s, String komut) {
        s.getCommands().performPrefixedCommand(s.createCommandSourceStack(), komut);
    }

    /** Kurallar, öğle, en yakın orman; yaratıcı kipte uçan kamera ormanın tepesinin 4 blok üstünde, elde Hava Radarı. */
    private static void hazirla(IntegratedServer s) {
        for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 5000", "weather clear"}) {
            komut(s, k);
        }
        ServerLevel level = s.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        Pair<BlockPos, Holder<Biome>> bulunan = level.findClosestBiome3d(h -> h.is(BiomeTags.IS_FOREST), dogus, 6400, 32, 64);
        BlockPos m = bulunan == null ? dogus : bulunan.getFirst();
        ServerPlayer o = oyuncu(s);
        o.setGameMode(GameType.CREATIVE);
        o.getAbilities().flying = true;
        o.onUpdateAbilities();
        o.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EsyaKayitlari.HAVA_RADARI.get()));
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, m.getX(), m.getZ());
        asitKamerasi = new Kamera(m.getX() + 0.5, ust + 4, m.getZ() + 0.5, -90F, 28F);
        asitKamera(s);
        DogalAfetler.LOGGER.info(ON + "orman {} ({}), kamera y={}", m.toShortString(), bulunan == null ? "bulunamadı"
                : bulunan.getSecond().unwrapKey().map(k -> k.location().toString()).orElse("?"), ust + 4);
    }

    /** Uzak kip: kurallar, öğle, en yakın çöl; yaratıcı oyuncu çölün yüzeyinde yerde, hafif aşağı bakar. */
    private static void uzakHazirla(IntegratedServer s) {
        for (String k : new String[] {"gamerule sendCommandFeedback false", "gamerule doDaylightCycle false",
                "gamerule doWeatherCycle false", "gamerule doMobSpawning false", "time set 5000", "weather clear"}) {
            komut(s, k);
        }
        ServerLevel level = s.overworld();
        BlockPos dogus = level.getSharedSpawnPos();
        Pair<BlockPos, Holder<Biome>> bulunan = level.findClosestBiome3d(h -> h.is(Biomes.DESERT), dogus, 6400, 32, 64);
        BlockPos m = bulunan == null ? dogus : bulunan.getFirst();
        ServerPlayer o = oyuncu(s);
        o.setGameMode(GameType.CREATIVE);
        o.getAbilities().flying = false;
        o.onUpdateAbilities();
        // Yüklü olmayan chunk'ta yükseklik dünyanın tabanını dönerdi: çöl doğuştan uzak olabilir.
        level.getChunk(m.getX() >> 4, m.getZ() >> 4);
        int ust = level.getHeight(Heightmap.Types.MOTION_BLOCKING, m.getX(), m.getZ());
        o.teleportTo(level, m.getX() + 0.5, ust, m.getZ() + 0.5, -90F, 8F);
        uzakMerkez = new BlockPos(m.getX() + UZAK_MESAFE, ust, m.getZ());
        DogalAfetler.LOGGER.info(ON + "uzak: çöl {} ({}), oyuncu y={}, asit merkezi {}", m.toShortString(),
                bulunan == null ? "bulunamadı" : "minecraft:desert", ust, uzakMerkez.toShortString());
    }

    /**
     * Uzak kip: S5 asidin hedefi oyuncudur, ama başladığı an oyuncu çölden {@value #UZAK_MESAFE} blok doğudadır (merkez orada;
     * hedefsiz başlatma çevrim içi oyuncuyu hedef seçerdi), sonra hemen çöle döner. Asit her yerde yağdığından merkez
     * davranışı etkilemez (AF2-01); kare satırı merkeze gerçek uzaklığı yazar.
     */
    private static void uzakBaslat(IntegratedServer s) {
        AfetYoneticisi y = AfetYoneticisi.al(s);
        y.durdur();
        ServerLevel level = s.overworld();
        ServerPlayer o = oyuncu(s);
        double cx = o.getX();
        double cy = o.getY();
        double cz = o.getZ();
        float yaw = o.getYRot();
        float pitch = o.getXRot();
        level.getChunk(uzakMerkez.getX() >> 4, uzakMerkez.getZ() >> 4);
        o.teleportTo(level, uzakMerkez.getX() + 0.5, uzakMerkez.getY(), uzakMerkez.getZ() + 0.5, yaw, pitch);
        AfetYoneticisi.BaslatmaSonucu sonuc = y.elleBaslat(AfetTuru.ASIT_YAGMURU, 5, o, level, o.blockPosition());
        o.teleportTo(level, cx, cy, cz, yaw, pitch);
        y.aktifAfet().ifPresent(a -> uzakMerkez = a.baglam().merkez());
        DogalAfetler.LOGGER.info(ON + "uzak: asit_yagmuru S5 başlatma {} merkez {} (hedef oyuncu, sonra çöle döndü)", sonuc,
                y.aktifAfet().map(a -> a.baglam().merkez().toShortString()).orElse("-"));
    }

    private static void asitKamera(IntegratedServer s) {
        Kamera k = asitKamerasi;
        oyuncu(s).teleportTo(s.overworld(), k.x(), k.y(), k.z(), k.yaw(), k.pitch());
    }

    /** AOi-1: asit kamerasının yerinden ufka bakış; siren bulut tavanı kadrajda. */
    private static void ufukKamera(IntegratedServer s) {
        Kamera k = asitKamerasi;
        oyuncu(s).teleportTo(s.overworld(), k.x(), k.y(), k.z(), k.yaw(), UFUK_PITCH);
        DogalAfetler.LOGGER.info(ON + String.format(Locale.ROOT, "ufuk kamerası %.0f %.0f %.0f yaw %.0f pitch %.0f", k.x(),
                k.y(), k.z(), k.yaw(), UFUK_PITCH));
    }

    /**
     * AOi-1: göz aşınan bloğun üst yüzünden {@value #YAKIN_GOZ} blok yukarıda, ona yatayda {@value #YAKIN_UZAKLIK} blok
     * uzakta, ona hafif aşağı bakan kamera (istemcinin sıçrama ve cızlama yolu yüzey ile göz arasında en çok 24 blok ister).
     * Sekiz yönden zemini en alçak olanı seçilir; göz o zeminin 2 blok üstünden aşağı inmez. Aşınan blok yoksa kameranın
     * baktığı yönde 12 blok ötedeki yüzey hedeflenir.
     */
    private static void yakinKameraKur(IntegratedServer s, BlockPos asinan, int asinanSayisi) {
        ServerLevel level = s.overworld();
        ServerPlayer o = oyuncu(s);
        BlockPos hedef = asinan;
        if (hedef == null) {
            double bakis = Math.toRadians(o.getYRot());
            int hx = Mth.floor(o.getX() - Math.sin(bakis) * 12);
            int hz = Mth.floor(o.getZ() + Math.cos(bakis) * 12);
            hedef = new BlockPos(hx, level.getHeight(Heightmap.Types.MOTION_BLOCKING, hx, hz) - 1, hz);
        }
        double tx = hedef.getX() + 0.5;
        double ty = hedef.getY() + 1;
        double tz = hedef.getZ() + 0.5;
        double x = tx;
        double z = tz;
        int zemin = Integer.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            double cx = tx + Math.cos(i * Math.PI / 4) * YAKIN_UZAKLIK;
            double cz = tz + Math.sin(i * Math.PI / 4) * YAKIN_UZAKLIK;
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(cx), Mth.floor(cz));
            if (h < zemin) {
                zemin = h;
                x = cx;
                z = cz;
            }
        }
        double goz = Math.max(ty + YAKIN_GOZ, zemin + 2);
        float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
        float pitch = (float) Math.toDegrees(Math.atan2(goz - ty, YAKIN_UZAKLIK));
        o.teleportTo(level, x, goz - o.getEyeHeight(), z, yaw, pitch);
        DogalAfetler.LOGGER.info(ON + String.format(Locale.ROOT, "yakın kamera %.1f %.1f %.1f yaw %.0f pitch %.0f; hedef %s (%s), "
                + "göz aşınan yüzeyden %.1f blok, zemin %d", x, goz, z, yaw, pitch, hedef.toShortString(), asinan == null
                ? "aşınan blok yok, bakış yönü" : asinanSayisi + " aşınan bloktan en yakını", goz - ty, zemin));
    }

    private static void baslat(IntegratedServer s, AfetTuru tur, int seviye) {
        AfetYoneticisi y = AfetYoneticisi.al(s);
        y.durdur();
        ServerPlayer o = oyuncu(s);
        AfetYoneticisi.BaslatmaSonucu sonuc = y.elleBaslat(tur, seviye, o, o.serverLevel(), o.blockPosition());
        DogalAfetler.LOGGER.info(ON + "{} S{} başlatma: {} merkez {}", tur.kimlik(), seviye, sonuc,
                y.aktifAfet().map(a -> a.baglam().merkez().toShortString()).orElse("-"));
    }

    /**
     * Afeti {@code hedef} aşamasına bir adım ilerletir; afet o aşamaya kendiliğinden geçmişse dokunmaz (gizli workspace ~1 FPS
     * iken istemci tick'i yavaşlar, siren sunucuda kendiliğinden biter; körlemesine ilerletmek AKTIF'i atlatırdı).
     */
    private static void ileri(IntegratedServer s, AfetAsamasi hedef) {
        AfetYoneticisi y = AfetYoneticisi.al(s);
        if (y.aktifAfet().map(a -> a.asama().ordinal() < hedef.ordinal()).orElse(false)) {
            y.asamaIleri();
        }
        DogalAfetler.LOGGER.info(ON + "aşama: {} (hedef {})", y.aktifAfet().map(a -> a.asama().kimlik()).orElse("bitti"),
                hedef.kimlik());
    }

    private static void dur(IntegratedServer s) {
        AfetYoneticisi.al(s).durdur();
        DogalAfetler.LOGGER.info(ON + "durduruldu; kalan yanan hücre {}", YanginYoneticisi.aktifSayi(s.overworld()));
    }

    /** Maruziyet karesi için hayatta kalma kipi (yaratıcı oyuncu maruz sayılmaz, Gc2); uçuş açık kalır ki kamera düşmesin. */
    private static void oyunKipi(IntegratedServer s, GameType kip) {
        ServerPlayer o = oyuncu(s);
        o.setGameMode(kip);
        o.getAbilities().mayfly = true;
        o.getAbilities().flying = true;
        o.onUpdateAbilities();
        DogalAfetler.LOGGER.info(ON + "oyun kipi {}", kip.getName());
    }

    private static void olcum(IntegratedServer s, String ad) {
        DogalAfetler.LOGGER.info(ON + String.format(Locale.ROOT, "ölçüm %s: TPS %.1f (%.2f ms/tick), yanan hücre %d", ad,
                TpsOlcer.tps(), TpsOlcer.ortalamaMs(), YanginYoneticisi.aktifSayi(s.overworld())));
    }

    /** Orman yangınının merkezinin 34 blok rüzgâr üstünde, zeminin 10 blok üstünde, merkeze bakan kamera. */
    private static void ormanKamera(IntegratedServer s) {
        ServerLevel level = s.overworld();
        AfetYoneticisi.al(s).aktifAfet().ifPresent(afet -> {
            BlockPos m = afet.baglam().merkez();
            ormanMerkezi = m;
            double[] r = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
            double g = Math.max(1e-6, Math.hypot(r[0], r[1]));
            double x = m.getX() + 0.5 - r[0] / g * KAMERA_UZAKLIGI;
            double z = m.getZ() + 0.5 - r[1] / g * KAMERA_UZAKLIGI;
            int zemin = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)), m.getY());
            double y = zemin + 10;
            double dx = m.getX() + 0.5 - x;
            double dz = m.getZ() + 0.5 - z;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
            float pitch = (float) Math.toDegrees(Math.atan2(y - m.getY() - 1, Math.hypot(dx, dz)));
            oyuncu(s).teleportTo(level, x, y, z, yaw, pitch);
            DogalAfetler.LOGGER.info(ON + String.format(Locale.ROOT, "orman kamerası %.0f %.0f %.0f yaw %.0f pitch %.0f", x, y,
                    z, yaw, pitch));
        });
    }

    /**
     * Orman bitiş kamerası (K-E47 eki, P2-4): yanık ağırlık merkezinin {@link #YANIK_UZAKLIK} blok rüzgâr üstünde (yangın
     * kamerasıyla aynı yön), oradaki zeminin {@link #YANIK_YUKSEKLIK} blok üstünde, merkezdeki zemine bakar. Ağırlık merkezi
     * yoksa (sıcak kare gelmedi) afet merkezi.
     */
    private static void yanikKameraKur(IntegratedServer s, double[] hedef) {
        ServerLevel level = s.overworld();
        BlockPos m = ormanMerkezi != null ? ormanMerkezi : oyuncu(s).blockPosition();
        double tx = hedef != null ? hedef[0] : m.getX() + 0.5;
        double tz = hedef != null ? hedef[1] : m.getZ() + 0.5;
        int ty = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(tx), Mth.floor(tz));
        double[] r = YanginMantigi.gununRuzgari(level.getGameTime() / 24000L);
        double g = Math.max(1e-6, Math.hypot(r[0], r[1]));
        double x = tx - r[0] / g * YANIK_UZAKLIK;
        double z = tz - r[1] / g * YANIK_UZAKLIK;
        int zemin = Math.max(level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z)), ty);
        double y = zemin + YANIK_YUKSEKLIK;
        float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
        float pitch = (float) Math.toDegrees(Math.atan2(y - ty, YANIK_UZAKLIK));
        ServerPlayer o = oyuncu(s);
        o.teleportTo(level, x, y - o.getEyeHeight(), z, yaw, pitch);
        DogalAfetler.LOGGER.info(ON + String.format(Locale.ROOT, "yanık kamerası %.0f %.0f %.0f yaw %.0f pitch %.0f; hedef %.0f %d %.0f (%s)",
                x, y, z, yaw, pitch, tx, ty, tz, hedef != null ? "yanık ağırlık merkezi, " + (long) hedef[2] + " hücre-saniye"
                        : "sıcak kare yok, afet merkezi"));
    }
}
