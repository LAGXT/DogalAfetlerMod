package io.github.kadirberkpolat1.dogalafetler.client.deprem;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.Deprem;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.SinkholeAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf.IstemciSismograf;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.network.AfetDurumPaketi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

/**
 * Deprem ve sinkhole görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=deprem} ile, tek oyunculu
 * Quick Play dünyasında çalışır; üretimde etkisizdir. Dört ayrı düzülük (doğuştan ve 600 blok ötesinden en yakın ova)
 * sahne olur: deprem S1 (Hassas Sismograf'ın ön-uyarı action bar'ı, siren alt yazısı, sarsıntı anı, bitiş), deprem S5 (vitrin:
 * yarığın uzantısında 30 blok ötede kameraya doğru açılış; S1 ile aynı göreli yan bakış; geniş bakış; bitiş), sinkhole S1
 * (çatlama, çukur) ve sinkhole S5 (çatlama, çöküş vitrini, üstten çukur, bitiş). Afet merkezi oyuncuya göre seçildiği için
 * kamera başlatmadan sonra sunucudan okunan merkeze (depremde fay doğrultusuna) göre seyirci modda konur; sahneye ahşap ev
 * ve ağaç kurulur. Her kare öncesi istemci en az {@value #AKICI_FPS} FPS çizip adım başından beri en az 20 kare çizmiş olmalı
 * (A-34); gizli workspace'te compositor istemciyi ~1 FPS'e kısar (GP17): kapı {@value #KAPI_PAYI} tick sonra en az iki yeni
 * çizilmiş kareyle geçer. Zamanlaması dar kareler (vitrin, sarsıntı) kapıyı eylemden önce geçer. Kare log'u FPS, parçacık,
 * düşen blok, sarsıntı genliği ve afet durumunu yazar. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır; iç imleç
 * köşeye alınır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class DepremOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.otomasyon", "");
    private static final String ON = "DEPREM_OTOMASYON ";
    private static final int AKICI_FPS = 15;
    private static final int KAPI_PAYI = 60;
    private static final int ARALIK = 600;
    private static final int TOPLAM_TAVAN = 20 * 1500;

    private record Adim(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
    }

    private record Kamera(double x, double y, double z, float yaw, float pitch) {
    }

    /** Sunucudan okunan sahne: merkez, zemin Y, fay açısı (deprem), yarıçap (sinkhole) ya da yarım boy (deprem). */
    private record Sahne(BlockPos merkez, int zemin, double aci, double olcek) {
    }

    private static final List<Adim> ADIMLAR = new ArrayList<>();
    private static final BlockPos[] YERLER = new BlockPos[4];
    private static int tus;
    private static int fare;
    private static int teker;
    private static int sayac;
    private static int toplam;
    private static int adim = -1;
    private static int adimTick;
    private static int kareSayaci;
    private static int adimKaresi;
    private static int oncekiAdimKaresi;
    private static int actionBarBasi;
    private static int artciSayisi;
    private static int sonAltAsama;
    private static int kareAdedi;
    private static Sahne sahne;

    private DepremOtomasyonu() {
    }

    private static boolean aktif() {
        return MOD.equals("deprem");
    }

    @SubscribeEvent
    public static void tusa(InputEvent.Key olay) {
        if (aktif()) {
            tus++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tuş {} eylem {}", olay.getKey(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void fareye(InputEvent.MouseButton.Pre olay) {
        if (aktif()) {
            fare++;
            DogalAfetler.LOGGER.warn(ON + "girdi: fare {} eylem {}", olay.getButton(), olay.getAction());
        }
    }

    @SubscribeEvent
    public static void tekerlege(InputEvent.MouseScrollingEvent olay) {
        if (aktif()) {
            teker++;
            DogalAfetler.LOGGER.warn(ON + "girdi: tekerlek {}", olay.getScrollDelta());
        }
    }

    @SubscribeEvent
    public static void cizildi(TickEvent.RenderTickEvent olay) {
        if (aktif() && olay.phase == TickEvent.Phase.END) {
            kareSayaci++;
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent olay) {
        if (!aktif() || olay.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof PauseScreen) {
            DogalAfetler.LOGGER.info(ON + "kendi açmadığım duraklatma ekranı kapatıldı");
            mc.setScreen(null);
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) {
            return;
        }
        sayac++;
        artciIzle();
        if (adim < 0) {
            if (sayac < 200 || !mc.player.hasPermissions(2)) {
                return;
            }
            plan(mc);
            adim = 0;
            adimTick = 0;
            adimKaresi = kareSayaci;
            DogalAfetler.LOGGER.info(ON + "başlıyor: {} adım", ADIMLAR.size());
            ADIMLAR.get(0).is().run();
            return;
        }
        if (adim >= ADIMLAR.size()) {
            return;
        }
        if (++toplam > TOPLAM_TAVAN && adim < ADIMLAR.size() - 1) {
            DogalAfetler.LOGGER.error(ON + "zaman aşımı (adım {}); kapanışa geçiliyor", ADIMLAR.get(adim).ad());
            adim = ADIMLAR.size() - 1;
            adimTick = 0;
            ADIMLAR.get(adim).is().run();
            return;
        }
        adimTick++;
        Adim a = ADIMLAR.get(adim);
        boolean hazir = a.hazir().getAsBoolean();
        if (hazir || adimTick >= a.enCokTick()) {
            if (!hazir) {
                DogalAfetler.LOGGER.warn(ON + "{}: {} tick'te koşul sağlanmadı (FPS {}), devam", a.ad(), adimTick, mc.getFps());
            }
            adim++;
            adimTick = 0;
            oncekiAdimKaresi = kareSayaci - adimKaresi;
            adimKaresi = kareSayaci;
            if (adim < ADIMLAR.size()) {
                DogalAfetler.LOGGER.info(ON + "adım {}: {}", adim, ADIMLAR.get(adim).ad());
                ADIMLAR.get(adim).is().run();
            }
        }
    }

    /** Depremin artçı alt aşamaları istemciye geldikçe sayılır (DS-14 kanıtı). */
    private static void artciIzle() {
        Optional<AfetDurumPaketi> durum = IstemciAfetDurumu.aktif();
        int alt = durum.filter(p -> p.tur() == AfetTuru.DEPREM).map(AfetDurumPaketi::altAsama).orElse(0);
        if (alt > 0 && alt != sonAltAsama) {
            artciSayisi++;
            DogalAfetler.LOGGER.info(ON + "artçı sarsıntı {} istemcide (afet {})", alt, durum.get().afetNo());
        }
        sonAltAsama = alt;
    }

    private static void ekle(String ad, Runnable is, BooleanSupplier hazir, int enCokTick) {
        ADIMLAR.add(new Adim(ad, is, hazir, enCokTick));
    }

    private static void plan(Minecraft mc) {
        ekle("kurulum", () -> {
            mc.options.hideGui = true;
            imleciKenaraAl(mc);
            komut(mc, "gamerule sendCommandFeedback false", "gamerule doDaylightCycle false", "time set 5000",
                    "weather clear 100000", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                    "gamerule afetBlokHasari true", "dogalafet stop", "dogalafet hiz 1", "clear @s",
                    "gamemode spectator @s");
            yerleriBul(mc);
        }, bekle(40), 60);

        // Deprem S1: ön-uyarı, siren, sarsıntı, bitiş.
        ekle("S1 deprem yerine git", () -> git(mc, 0), yuklendi(mc, 0, 48), 2400);
        ekle("deprem S1 başlat", () -> {
            komut(mc, "gamemode survival @s", "effect give @s minecraft:resistance infinite 255 true",
                    "effect give @s minecraft:saturation infinite 255 true",
                    "item replace entity @s hotbar.0 with dogalafetler:hassas_sismograf");
            zemineIndir(mc, YERLER[0]);
            komut(mc, "dogalafet start deprem 1 @s");
        }, bekle(30), 40);
        ekle("deprem S1 sahnesi", () -> sahne = depremSahnesi(mc), bekle(20), 30);
        ekle("ön-uyarı (Hassas Sismograf)", () -> {
            mc.options.hideGui = false;
            oyuncuyuMerkezeCevir(mc);
            actionBarBasi = IstemciSismograf.actionBarSayisi();
        }, () -> akici(mc, 40, 20) && IstemciSismograf.durum().algilandi() && IstemciSismograf.durum().hassas()
                && IstemciSismograf.actionBarSayisi() > actionBarBasi, 1600);
        ekle("kare ön-uyarı", () -> kare(mc, "deprem_onuyari_sismograf"), bekle(2), 4);
        ekle("siren", () -> {
            komut(mc, "clear @s", "gamemode spectator @s");
            mc.options.showSubtitles().set(true);
            kamera(mc, yan(mc, sahne));
            komut(mc, "dogalafet asama ileri");
        }, () -> akici(mc, 60, 20), 1200);
        ekle("kare siren", () -> kare(mc, "deprem_siren"), bekle(2), 4);
        ekle("deprem S1 aktif", () -> {
            mc.options.hideGui = true;
            komut(mc, "dogalafet asama ileri");
        }, () -> adimTick >= 90 && (mc.getFps() >= AKICI_FPS || kareSayaci - adimKaresi >= 2), 125);
        ekle("kare S1", () -> kare(mc, "deprem_s1"), bekle(2), 4);
        ekle("deprem S1 bitsin", () -> { }, afetYok(mc), 1200);
        ekle("S1 bitiş kamerası", () -> kamera(mc, yakin(mc, sahne)), () -> akici(mc, 40, 20), 1200);
        ekle("kare deprem bitiş", () -> kare(mc, "deprem_bitis"), bekle(2), 4);

        // Deprem S5: vitrin, S1 ile aynı göreli yan bakış, geniş bakış, artçılar (hızlı), bitiş.
        ekle("S5 deprem yerine git", () -> git(mc, 1), yuklendi(mc, 1, 144), 3000);
        ekle("deprem S5 başlat", () -> komut(mc, "dogalafet start deprem 5 @s"), bekle(20), 30);
        ekle("deprem S5 sahnesi, vitrin kamerası", () -> {
            sahne = depremSahnesi(mc);
            kamera(mc, vitrin(mc, sahne));
        }, () -> akici(mc, 80, 20), 1200);
        ekle("deprem S5 aktif", () -> komut(mc, "dogalafet asama ileri", "dogalafet asama ileri"), bekle(40), 50);
        ekle("kare vitrin", () -> kare(mc, "deprem_vitrin"), bekle(20), 25);
        ekle("kare vitrin b", () -> kare(mc, "deprem_vitrin_b"), bekle(2), 4);
        ekle("S5 yan kamera", () -> kamera(mc, yan(mc, sahne)), bekle(240), 260);
        ekle("kare S5", () -> kare(mc, "deprem_s5"), bekle(2), 4);
        ekle("S5 geniş kamera", () -> kamera(mc, genis(mc, sahne)), bekle(150), 170);
        ekle("kare aktif", () -> kare(mc, "deprem_aktif"), bekle(2), 4);
        ekle("S5 artçılar (hız 20)", () -> komut(mc, "dogalafet hiz 20"), afetYok(mc), 2400);
        ekle("hız 1", () -> komut(mc, "dogalafet hiz 1"), () -> akici(mc, 60, 20), 1200);
        ekle("kare S5 bitiş", () -> kare(mc, "deprem_s5_bitis"), bekle(2), 4);

        // Sinkhole S1: çatlama, çukur.
        ekle("S1 sinkhole yerine git", () -> git(mc, 2), yuklendi(mc, 2, 64), 3000);
        ekle("sinkhole S1 başlat", () -> komut(mc, "dogalafet start sinkhole 1 @s"), bekle(20), 30);
        ekle("sinkhole S1 sahnesi", () -> {
            sahne = sinkholeSahnesi(mc);
            kamera(mc, cukurKamerasi(mc, sahne));
        }, () -> akici(mc, 60, 20), 1200);
        ekle("sinkhole S1 aktif", () -> komut(mc, "dogalafet asama ileri", "dogalafet asama ileri"), bekle(55), 60);
        ekle("kare S1 çatlama", () -> kare(mc, "sinkhole_s1_catlama"), bekle(2), 4);
        ekle("sinkhole S1 durulma", () -> { }, () -> altAsama() == SinkholeSeviyesi.DURULMA && adimTick >= 2, 1200);
        ekle("çukura yerleşsin", () -> { }, () -> akici(mc, 40, 20), 600);
        ekle("kare sinkhole S1", () -> kare(mc, "sinkhole_s1"), bekle(2), 4);
        ekle("sinkhole S1 bitsin (hız 20)", () -> komut(mc, "dogalafet hiz 20"), afetYok(mc), 1200);
        ekle("hız 1 (S1)", () -> komut(mc, "dogalafet hiz 1"), bekle(10), 20);

        // Sinkhole S5: çatlama, çöküş vitrini, üstten çukur, bitiş.
        ekle("S5 sinkhole yerine git", () -> git(mc, 3), yuklendi(mc, 3, 64), 3000);
        ekle("sinkhole S5 başlat", () -> komut(mc, "dogalafet start sinkhole 5 @s"), bekle(20), 30);
        ekle("sinkhole S5 sahnesi", () -> {
            sahne = sinkholeSahnesi(mc);
            kamera(mc, cukurKamerasi(mc, sahne));
        }, () -> akici(mc, 60, 20), 1200);
        ekle("sinkhole S5 aktif", () -> komut(mc, "dogalafet asama ileri", "dogalafet asama ileri"), bekle(100), 110);
        ekle("kare S5 çatlama", () -> kare(mc, "sinkhole_s5_catlama"), bekle(2), 4);
        ekle("çekirdek çöküşü", () -> { }, () -> altAsama() >= SinkholeSeviyesi.COKUS && dusenSayisi(mc) >= 20, 900);
        ekle("düşüş başladı", () -> { }, bekle(6), 8);
        ekle("kare çöküş vitrini", () -> kare(mc, "sinkhole_cokus_vitrin"), bekle(14), 16);
        ekle("kare çöküş vitrini b", () -> kare(mc, "sinkhole_cokus_vitrin_b"), bekle(2), 4);
        ekle("sinkhole S5 durulma", () -> { }, () -> altAsama() == SinkholeSeviyesi.DURULMA && adimTick >= 2, 1200);
        ekle("üstten kamera", () -> kamera(mc, ustten(mc, sahne)), () -> akici(mc, 60, 20), 1200);
        ekle("kare sinkhole S5", () -> kare(mc, "sinkhole_s5"), bekle(2), 4);
        ekle("sinkhole S5 bitsin (hız 20)", () -> komut(mc, "dogalafet hiz 20"), afetYok(mc), 1200);
        ekle("bitiş kamerası", () -> {
            komut(mc, "dogalafet hiz 1");
            kamera(mc, cukurKamerasi(mc, sahne));
        }, () -> akici(mc, 60, 20), 1200);
        ekle("kare sinkhole bitiş", () -> kare(mc, "sinkhole_bitis"), bekle(2), 4);

        ekle("kapanış", () -> {
            mc.options.hideGui = false;
            mc.options.showSubtitles().set(false);
            komut(mc, "dogalafet hiz 1", "dogalafet stop");
            DogalAfetler.LOGGER.info(ON + "bitti; {} kare, istemcide artçı {}, FPS {}", kareAdedi, artciSayisi, mc.getFps());
        }, bekle(40), 60);
        ekle("çıkış", () -> {
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
        }, bekle(1), 2);
    }

    // ---------------------------------------------------------------- koşullar

    private static BooleanSupplier bekle(int tick) {
        return () -> adimTick >= tick;
    }

    /**
     * En az {@code tick} tick geçti ve istemci akıcı (≥ {@value #AKICI_FPS} FPS, adım başından ≥ {@code kare} kare) çiziyor; ya da
     * {@value #KAPI_PAYI} tick daha geçti ve en az iki yeni kare çizildi (gizli workspace, GP17).
     */
    private static boolean akici(Minecraft mc, int tick, int kare) {
        int cizilen = kareSayaci - adimKaresi;
        return adimTick >= tick && (cizilen >= kare && mc.getFps() >= AKICI_FPS
                || adimTick >= tick + KAPI_PAYI && cizilen >= 2);
    }

    private static BooleanSupplier afetYok(Minecraft mc) {
        return () -> adimTick >= 20 && adimTick % 10 == 0
                && sunucuda(mc, () -> AfetYoneticisi.al(mc.getSingleplayerServer()).aktifAfet().isEmpty());
    }

    /** Sahne yerinin çevresindeki chunk'lar sunucuda ve merkez chunk istemcide yüklü, istemci akıcı. */
    private static BooleanSupplier yuklendi(Minecraft mc, int yer, int yaricap) {
        return () -> {
            if (adimTick < 100 || adimTick % 20 != 0 || !akici(mc, 100, 20)) {
                return false;
            }
            BlockPos p = YERLER[yer];
            if (!mc.level.getChunkSource().hasChunk(p.getX() >> 4, p.getZ() >> 4)) {
                return false;
            }
            return sunucuda(mc, () -> {
                ServerLevel level = mc.getSingleplayerServer().overworld();
                for (int i = 0; i < 8; i++) {
                    int x = (int) (p.getX() + Math.cos(i * Math.PI / 4) * yaricap);
                    int z = (int) (p.getZ() + Math.sin(i * Math.PI / 4) * yaricap);
                    if (!level.hasChunk(x >> 4, z >> 4)) {
                        return false;
                    }
                }
                return true;
            });
        };
    }

    private static int altAsama() {
        return IstemciAfetDurumu.aktif().filter(p -> p.tur() == AfetTuru.SINKHOLE).map(AfetDurumPaketi::altAsama)
                .orElse(-1);
    }

    private static int dusenSayisi(Minecraft mc) {
        int n = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof FallingBlockEntity) {
                n++;
            }
        }
        return n;
    }

    // ---------------------------------------------------------------- sunucu işleri

    private static <T> T sunucuda(Minecraft mc, Supplier<T> is) {
        return mc.getSingleplayerServer().submit(is).join();
    }

    /** Sahne komutu: entegre sunucunun konsol kaynağından, çıktısız; {@code @s} oyuncunun adına çevrilir. */
    private static void komut(Minecraft mc, String... komutlar) {
        IntegratedServer server = mc.getSingleplayerServer();
        String ad = mc.player.getGameProfile().getName();
        server.execute(() -> {
            for (String k : komutlar) {
                String satir = k.replace("@s", ad);
                int sonuc = server.getCommands().performPrefixedCommand(
                        server.createCommandSourceStack().withSuppressedOutput(), satir);
                DogalAfetler.LOGGER.info(ON + "komut /{} -> {}", satir, sonuc);
            }
        });
    }

    private static ServerPlayer oyuncu(IntegratedServer server, Minecraft mc) {
        return server.getPlayerList().getPlayer(mc.player.getUUID());
    }

    /** Doğuştan ve doğuşun 600 blok doğusu, güneyi, batısından en yakın ova/çayır (sahneler birbirine karışmaz). */
    private static void yerleriBul(Minecraft mc) {
        sunucuda(mc, () -> {
            ServerLevel level = mc.getSingleplayerServer().overworld();
            BlockPos dogus = level.getSharedSpawnPos();
            int[][] ofset = {{0, 0}, {ARALIK, 0}, {0, ARALIK}, {-ARALIK, 0}};
            for (int i = 0; i < 4; i++) {
                BlockPos bas = dogus.offset(ofset[i][0], 0, ofset[i][1]);
                Pair<BlockPos, Holder<Biome>> bulunan = level.findClosestBiome3d(h -> h.is(Biomes.PLAINS)
                        || h.is(Biomes.SUNFLOWER_PLAINS) || h.is(Biomes.MEADOW) || h.is(Biomes.SAVANNA), bas, 400, 32, 64);
                YERLER[i] = bulunan == null ? bas : bulunan.getFirst();
                DogalAfetler.LOGGER.info(ON + "sahne {}: {} ({})", i, YERLER[i].toShortString(), bulunan == null
                        ? "ova bulunamadı, ofset" : bulunan.getSecond().unwrapKey().map(k -> k.location().toString()).orElse("?"));
            }
            return true;
        });
    }

    private static void git(Minecraft mc, int yer) {
        BlockPos p = YERLER[yer];
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> oyuncu(server, mc).teleportTo(server.overworld(), p.getX() + 0.5, 140, p.getZ() + 0.5, 0F, 30F));
    }

    private static void zemineIndir(Minecraft mc, BlockPos p) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> {
            ServerLevel level = server.overworld();
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
            oyuncu(server, mc).teleportTo(level, p.getX() + 0.5, y, p.getZ() + 0.5, 0F, 10F);
        });
    }

    private static int zemin(ServerLevel level, double x, double z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
    }

    /** Aktif depremin merkezi ve doğrultusu; merkezin bir yanına ahşap ev, çevresine ağaç kurulur. */
    private static Sahne depremSahnesi(Minecraft mc) {
        return sunucuda(mc, () -> {
            IntegratedServer server = mc.getSingleplayerServer();
            ServerLevel level = server.overworld();
            Afet afet = AfetYoneticisi.al(server).aktifAfet().orElse(null);
            if (!(afet instanceof Deprem d)) {
                DogalAfetler.LOGGER.error(ON + "aktif deprem yok: {}", afet == null ? "afet yok" : afet.tur().kimlik());
                BlockPos p = oyuncu(server, mc).blockPosition();
                return new Sahne(p, zemin(level, p.getX(), p.getZ()), 0, 12);
            }
            BlockPos m = d.baglam().merkez();
            double aci = d.fayAcisi();
            double dx = Math.cos(aci);
            double dz = Math.sin(aci);
            double px = -dz;
            double pz = dx;
            double cx = m.getX() + 0.5;
            double cz = m.getZ() + 0.5;
            ev(level, (int) Math.floor(cx - px * 10), (int) Math.floor(cz - pz * 10));
            agac(server, level, cx - px * 5 + dx * 9, cz - pz * 5 + dz * 9);
            agac(server, level, cx - px * 6 - dx * 8, cz - pz * 6 - dz * 8);
            agac(server, level, cx + px * 6 + dx * 12, cz + pz * 6 + dz * 12);
            Sahne s = new Sahne(m, zemin(level, cx, cz), aci, DepremAyarlari.seviye(d.seviye()).uzunluk() / 2.0);
            DogalAfetler.LOGGER.info(ON + "deprem S{} merkez {} zemin {} doğrultu {}°", d.seviye(), m.toShortString(),
                    s.zemin(), Math.round(Math.toDegrees(aci)));
            return s;
        });
    }

    /** Aktif sinkhole'un merkezi ve yarıçapı; merkezin yanına ağaç (çekirdekle birlikte düşer), S3+ halkada ikinci ağaç. */
    private static Sahne sinkholeSahnesi(Minecraft mc) {
        return sunucuda(mc, () -> {
            IntegratedServer server = mc.getSingleplayerServer();
            ServerLevel level = server.overworld();
            Afet afet = AfetYoneticisi.al(server).aktifAfet().orElse(null);
            if (afet == null || afet.tur() != AfetTuru.SINKHOLE) {
                DogalAfetler.LOGGER.error(ON + "aktif sinkhole yok: {}", afet == null ? "afet yok" : afet.tur().kimlik());
                BlockPos p = oyuncu(server, mc).blockPosition();
                return new Sahne(p, zemin(level, p.getX(), p.getZ()), 0, 3);
            }
            BlockPos m = afet.baglam().merkez();
            double r = SinkholeAyarlari.seviye(afet.seviye()).yaricap();
            agac(server, level, m.getX() + 1.5, m.getZ() + 0.5);
            if (r >= 8) {
                agac(server, level, m.getX() + 0.5 - r * 0.6, m.getZ() + 0.5 + r * 0.3);
            }
            Sahne s = new Sahne(m, zemin(level, m.getX(), m.getZ()), 0, r);
            DogalAfetler.LOGGER.info(ON + "sinkhole S{} merkez {} zemin {} yarıçap {}", afet.seviye(), m.toShortString(),
                    s.zemin(), r);
            return s;
        });
    }

    /** 7x5 ahşap ev: köşeler kütük (kalır), duvar kalas, cam pencere, kalas çatı ve yarım blok mahya, içeride fener. */
    private static void ev(ServerLevel level, int ox, int oz) {
        int taban = zemin(level, ox, oz);
        for (int x = -3; x <= 3; x++) {
            for (int z = -2; z <= 2; z++) {
                int bx = ox + x;
                int bz = oz + z;
                for (int y = taban - 1; y >= taban - 4 && level.getBlockState(new BlockPos(bx, y, bz)).canBeReplaced(); y--) {
                    level.setBlockAndUpdate(new BlockPos(bx, y, bz), Blocks.COBBLESTONE.defaultBlockState());
                }
                boolean kenar = Math.abs(x) == 3 || Math.abs(z) == 2;
                boolean kose = Math.abs(x) == 3 && Math.abs(z) == 2;
                for (int y = 0; y <= 4; y++) {
                    BlockState s = Blocks.AIR.defaultBlockState();
                    if (y <= 2 && kose) {
                        s = Blocks.OAK_LOG.defaultBlockState();
                    } else if (y <= 2 && kenar) {
                        boolean kapi = z == -2 && x == 0 && y <= 1;
                        boolean pencere = y == 1 && (Math.abs(x) == 3 || (z == 2 && Math.abs(x) == 1));
                        s = kapi ? Blocks.AIR.defaultBlockState() : pencere ? Blocks.GLASS.defaultBlockState()
                                : Blocks.OAK_PLANKS.defaultBlockState();
                    } else if (y == 3) {
                        s = Blocks.OAK_PLANKS.defaultBlockState();
                    } else if (y == 4 && z == 0) {
                        s = Blocks.OAK_SLAB.defaultBlockState();
                    } else if (y == 0 && x == 1 && z == 0) {
                        s = Blocks.LANTERN.defaultBlockState();
                    }
                    level.setBlockAndUpdate(new BlockPos(bx, taban + y, bz), s);
                }
            }
        }
    }

    private static void agac(IntegratedServer server, ServerLevel level, double x, double z) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        String k = String.format(Locale.ROOT, "place feature minecraft:oak %d %d %d", bx, zemin(level, x, z), bz);
        int sonuc = server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), k);
        DogalAfetler.LOGGER.info(ON + "komut /{} -> {}", k, sonuc);
    }

    // ---------------------------------------------------------------- kamera

    private static void kamera(Minecraft mc, Kamera k) {
        IntegratedServer server = mc.getSingleplayerServer();
        server.execute(() -> oyuncu(server, mc).teleportTo(server.overworld(), k.x(), k.y(), k.z(), k.yaw(), k.pitch()));
        DogalAfetler.LOGGER.info(ON + "kamera {} {} {} yaw {} pitch {}", Math.round(k.x()), Math.round(k.y()),
                Math.round(k.z()), Math.round(k.yaw()), Math.round(k.pitch()));
    }

    /** {@code (x, y, z)}'den hedef noktaya bakan kamera. */
    private static Kamera bakan(double x, double y, double z, double tx, double ty, double tz) {
        float yaw = (float) Math.toDegrees(Math.atan2(-(tx - x), tz - z));
        float pitch = (float) Math.toDegrees(Math.atan2(y - ty, Math.hypot(tx - x, tz - z)));
        return new Kamera(x, y, z, yaw, pitch);
    }

    /** Merkezden fay hattına dik {@code yan} blok, zeminin {@code yukari} blok üstü ve hat boyunca {@code boy} blok ötede. */
    private static Kamera hattaGore(Minecraft mc, Sahne s, double yan, double boy, double yukari) {
        double dx = Math.cos(s.aci());
        double dz = Math.sin(s.aci());
        double cx = s.merkez().getX() + 0.5;
        double cz = s.merkez().getZ() + 0.5;
        double x = cx - dz * yan + dx * boy;
        double z = cz + dx * yan + dz * boy;
        int y = sunucuda(mc, () -> zemin(mc.getSingleplayerServer().overworld(), x, z));
        return bakan(x, Math.max(y, s.zemin()) + yukari, z, cx, s.zemin(), cz);
    }

    /** S1 ve S5 karşılaştırması: hatta dik 40 blok, 22 blok yukarıdan merkeze (aynı göreli bakış). */
    private static Kamera yan(Minecraft mc, Sahne s) {
        return hattaGore(mc, s, 40, 0, 22);
    }

    private static Kamera yakin(Minecraft mc, Sahne s) {
        return hattaGore(mc, s, 13, 4, 9);
    }

    private static Kamera genis(Minecraft mc, Sahne s) {
        return hattaGore(mc, s, 75, s.olcek() * 0.35, 55);
    }

    /** Vitrin (tasarım notu deprem §3): yarık hattının uzantısında 30 blok ötede, Y+6'dan hatta, pitch 20° aşağı. */
    private static Kamera vitrin(Minecraft mc, Sahne s) {
        Kamera k = hattaGore(mc, s, 0, 30, 6);
        return new Kamera(k.x(), k.y(), k.z(), k.yaw(), 20F);
    }

    /** Sinkhole vitrini (tasarım notu sinkhole §3): kenarın 8 blok gerisinde, merkeze 35–45° aşağı. */
    private static Kamera cukurKamerasi(Minecraft mc, Sahne s) {
        double uzak = s.olcek() + 8;
        double x = s.merkez().getX() + 0.5 - uzak * 0.7071;
        double z = s.merkez().getZ() + 0.5 - uzak * 0.7071;
        int y = sunucuda(mc, () -> zemin(mc.getSingleplayerServer().overworld(), x, z));
        double yukari = 8 + s.olcek() * 0.7;
        return bakan(x, Math.max(y, s.zemin()) + yukari, z, s.merkez().getX() + 0.5, s.zemin() - s.olcek() * 0.4,
                s.merkez().getZ() + 0.5);
    }

    /** Küçük resim (tasarım notu sinkhole §3): tam yukarıdan, pitch 80°. */
    private static Kamera ustten(Minecraft mc, Sahne s) {
        double yukari = s.olcek() * 2.4 + 10;
        double geri = yukari * Math.tan(Math.toRadians(10));
        return new Kamera(s.merkez().getX() + 0.5, s.zemin() + yukari, s.merkez().getZ() + 0.5 - geri, 0F, 80F);
    }

    private static void oyuncuyuMerkezeCevir(Minecraft mc) {
        IntegratedServer server = mc.getSingleplayerServer();
        Sahne s = sahne;
        server.execute(() -> {
            ServerPlayer o = oyuncu(server, mc);
            Kamera k = bakan(o.getX(), o.getEyeY(), o.getZ(), s.merkez().getX() + 0.5, s.zemin() + 2, s.merkez().getZ() + 0.5);
            o.teleportTo(server.overworld(), o.getX(), o.getY(), o.getZ(), k.yaw(), Math.max(4F, k.pitch()));
        });
    }

    /**
     * İstemcinin iç imleç konumunu pencerenin sol üst köşesine alır (A-34): gerçek imlece ve sahibin oturumuna dokunmaz
     * (yalnız {@code MouseHandler} alanı; SRG adları {@code build/createSrgToMcp/output.srg}).
     */
    private static void imleciKenaraAl(Minecraft mc) {
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, 0.0, "f_91507_");
        ObfuscationReflectionHelper.setPrivateValue(MouseHandler.class, mc.mouseHandler, 0.0, "f_91508_");
    }

    private static void kare(Minecraft mc, String ad) {
        String dosya = "ds_" + ad + ".png";
        Screenshot.grab(mc.gameDirectory, dosya, mc.getMainRenderTarget(), mesaj -> { });
        kareAdedi++;
        Optional<AfetDurumPaketi> durum = IstemciAfetDurumu.aktif();
        String afet = durum.map(p -> String.format(Locale.ROOT, "%s S%d %s/%d", p.tur().kimlik(), p.seviye(),
                p.asama().kimlik(), p.altAsama())).orElse("yok");
        DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, önceki adımda çizilen kare {}, parçacık {}, düşen blok {}, sarsıntı "
                + "{}°, afet {}, oyuncu {} {} {})", dosya, mc.getFps(), oncekiAdimKaresi, mc.particleEngine.countParticles(),
                dusenSayisi(mc), String.format(Locale.ROOT, "%.2f", IstemciSarsinti.genlik(mc.player, 0)[0]), afet,
                Math.round(mc.player.getX()), Math.round(mc.player.getY()), Math.round(mc.player.getZ()));
    }
}
