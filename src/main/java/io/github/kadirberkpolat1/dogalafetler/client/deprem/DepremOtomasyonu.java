package io.github.kadirberkpolat1.dogalafetler.client.deprem;

import com.mojang.datafixers.util.Pair;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.Deprem;
import io.github.kadirberkpolat1.dogalafetler.afet.deprem.DepremAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.SinkholeAyarlari;
import io.github.kadirberkpolat1.dogalafetler.afet.sinkhole.mantik.SinkholeSeviyesi;
import io.github.kadirberkpolat1.dogalafetler.client.core.GorselOtomasyon;
import io.github.kadirberkpolat1.dogalafetler.client.core.IstemciAfetDurumu;
import io.github.kadirberkpolat1.dogalafetler.client.esya.sismograf.IstemciSismograf;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
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

/**
 * Deprem ve sinkhole görsel kanıt otomasyonu (K0-21 rev. 2). Yalnız {@code -Ddogalafetler.otomasyon=deprem} ile, tek oyunculu
 * Quick Play dünyasında çalışır; üretimde etkisizdir. Dört ayrı düzlük (doğuştan ve 600 blok ötesinden en yakın ova)
 * sahne olur: deprem S1 (Hassas Sismograf'ın ön-uyarı action bar'ı, siren alt yazısı, sarsıntı anı, bitiş), deprem S5 (vitrin:
 * yarığın uzantısında 30 blok ötede kameraya doğru açılış; S1 ile aynı göreli yan bakış; sarsıntı kare dizisi; geniş bakış;
 * bitiş), sinkhole S1 (çatlama, çukur) ve sinkhole S5 (çatlama, çöküş vitrini, üstten çukur, bitiş). Afet merkezi oyuncuya
 * göre seçildiği için kamera başlatmadan sonra sunucudan okunan merkeze (depremde fay doğrultusuna) göre seyirci modda konur;
 * sahneye ahşap ev ve ağaç kurulur. {@code -PotomasyonBolum} (sistem özelliği {@code dogalafetler.otomasyon.bolum}) {@code hepsi}
 * ya da virgülle {@code deprem_s1}, {@code deprem_s5}, {@code sinkhole_s1}, {@code sinkhole_s5}; kurulum ve kapanış her zaman.
 *
 * <p>Kareler A-34 kapısından ({@link GorselOtomasyon#kareIste}) geçer; kapı beklerken adım motoru ve adım tick'i durur. Adım
 * koşulları da istemcinin akıcı (≥ {@value GorselOtomasyon#AKICI_FPS} FPS, adım başından ≥ 20 kare) çizmesini bekler; gizli
 * workspace'te compositor istemciyi ~1 FPS'e kısar (GP17): adım koşulu {@value #KAPI_PAYI} tick sonra en az iki yeni çizilmiş
 * kareyle geçer. Zamanlaması dar kareler (vitrin, sarsıntı, çatlama, çöküş) kapıyı eylemden önce geçer. Deprem S1'in siren ve
 * sarsıntısı, sinkhole S1 çatlaması ve sinkhole S5 çöküşü yavaş çekimde ({@code dogalafet hiz 0.2}) alınır; kare adı içeriğine
 * uysun diye koşul afetin aşamasını ve alt aşamasını da bekler. Kare log'u FPS, parçacık, düşen blok, sarsıntı genliği ve afet
 * durumunu yazar.
 *
 * <p>FPS (K-E29 (b)): deprem S5 sarsıntısı sürerken geniş kamerada {@code s5_deprem_yuksek}, sinkhole S5 çöküşü ve geri
 * çekilmesi sırasında çukur kamerasında {@code s5_sinkhole_yuksek}; kameralar afet merkezine bağlı olduğundan tabanlar
 * ({@code s5_deprem_taban}, {@code s5_sinkhole_taban}) afet bittikten sonra aynı kamerada ölçülür. Her pencere hız 1'de,
 * FPS kapısından sonra {@value #OLCUM_TICK} tick. Klavye, fare ve tekerlek girdileri kapanışa kadar sayılır.
 */
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID, value = Dist.CLIENT)
public final class DepremOtomasyonu {
    private static final String MOD = System.getProperty("dogalafetler.otomasyon", "");
    private static final String BOLUM = System.getProperty("dogalafetler.otomasyon.bolum", "hepsi");
    private static final String ON = "DEPREM_OTOMASYON ";
    private static final int KAPI_PAYI = 60;
    private static final int OLCUM_TICK = 100;
    private static final int SARSINTI_KARESI = 5;
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
    private static long adimKaresi;
    private static int actionBarBasi;
    private static int artciSayisi;
    private static int sonAltAsama;
    private static int kareAdedi;
    private static Sahne sahne;
    private static Kamera genisKamera;
    private static Kamera cukurKamera;

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
            adimKaresi = GorselOtomasyon.cizilenKare();
            DogalAfetler.LOGGER.info(ON + "başlıyor: {} adım, bölüm {}", ADIMLAR.size(), BOLUM);
            ADIMLAR.get(0).is().run();
            return;
        }
        if (adim >= ADIMLAR.size()) {
            return;
        }
        if (GorselOtomasyon.kareBekliyor()) {
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
            adimKaresi = GorselOtomasyon.cizilenKare();
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
            GorselOtomasyon.cizimAyari();
            mc.options.hideGui = true;
            GorselOtomasyon.imleciKenaraAl();
            komut(mc, "gamerule sendCommandFeedback false", "gamerule doDaylightCycle false", "time set 5000",
                    "weather clear 100000", "gamerule doWeatherCycle false", "gamerule doMobSpawning false",
                    "gamerule afetBlokHasari true", "dogalafet stop", "dogalafet hiz 1", "clear @s",
                    "gamemode spectator @s");
            yerleriBul(mc);
        }, bekle(40), 60);
        if (secili("deprem_s1")) {
            depremS1(mc);
        }
        if (secili("deprem_s5")) {
            depremS5(mc);
        }
        if (secili("sinkhole_s1")) {
            sinkholeS1(mc);
        }
        if (secili("sinkhole_s5")) {
            sinkholeS5(mc);
        }
        ekle("kapanış", () -> {
            mc.options.hideGui = false;
            mc.options.showSubtitles().set(false);
            komut(mc, "dogalafet hiz 1", "dogalafet stop");
            DogalAfetler.LOGGER.info(ON + "bitti; {} kare, istemcide artçı {}, FPS {}", kareAdedi, artciSayisi, mc.getFps());
        }, bekle(40), 60);
        ekle("çıkış", () -> {
            GorselOtomasyon.kareOzetiYaz("deprem");
            DogalAfetler.LOGGER.info(ON + "kapanıyor; girdi sayıları: tuş={} fare={} tekerlek={}", tus, fare, teker);
            mc.stop();
        }, bekle(1), 2);
    }

    private static boolean secili(String bolum) {
        return "hepsi".equals(BOLUM) || BOLUM.contains(bolum);
    }

    /** Ön-uyarı, siren, sarsıntı (siren ve sarsıntı hız 0.2'de: kareler kendi aşamalarının içinde), bitiş. */
    private static void depremS1(Minecraft mc) {
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
        }, () -> akici(40, 20) && IstemciSismograf.durum().algilandi() && IstemciSismograf.durum().hassas()
                && IstemciSismograf.actionBarSayisi() > actionBarBasi, 1600);
        ekle("kare ön-uyarı", () -> kare(mc, "deprem_onuyari_sismograf"), bekle(2), 4);
        ekle("siren (hız 0.2)", () -> {
            komut(mc, "clear @s", "gamemode spectator @s");
            mc.options.showSubtitles().set(true);
            kamera(mc, yan(mc, sahne));
            komut(mc, "dogalafet hiz 0.2", "dogalafet asama ileri");
        }, () -> akici(60, 20) && depremAsamasi(AfetAsamasi.SIREN), 900);
        ekle("kare siren", () -> kare(mc, "deprem_siren"), bekle(2), 4);
        ekle("deprem S1 aktif (hız 0.2)", () -> {
            mc.options.hideGui = true;
            komut(mc, "dogalafet asama ileri");
        }, () -> akici(250, 20) && depremAsamasi(AfetAsamasi.AKTIF), 700);
        ekle("kare S1", () -> kare(mc, "deprem_s1"), bekle(2), 4);
        ekle("deprem S1 bitsin (hız 1)", () -> komut(mc, "dogalafet hiz 1"), afetYok(mc), 1200);
        ekle("S1 bitiş kamerası", () -> kamera(mc, yakin(mc, sahne)), () -> akici(40, 20), 1200);
        ekle("kare deprem bitiş", () -> kare(mc, "deprem_bitis"), bekle(2), 4);
    }

    /**
     * Vitrin, S1 ile aynı göreli yan bakış, sarsıntı kare dizisi (C.4), geniş bakışta ölçüm, artçılar (hızlı), aynı geniş
     * kamerada taban ölçümü ve bitiş.
     */
    private static void depremS5(Minecraft mc) {
        ekle("S5 deprem yerine git", () -> git(mc, 1), yuklendi(mc, 1, 144), 3000);
        ekle("deprem S5 başlat", () -> komut(mc, "dogalafet start deprem 5 @s"), bekle(20), 30);
        ekle("deprem S5 sahnesi, vitrin kamerası", () -> {
            sahne = depremSahnesi(mc);
            kamera(mc, vitrin(mc, sahne));
        }, () -> akici(80, 20), 1200);
        ekle("deprem S5 aktif", () -> komut(mc, "dogalafet asama ileri", "dogalafet asama ileri"), bekle(40), 50);
        ekle("kare vitrin", () -> kare(mc, "deprem_vitrin"), bekle(20), 25);
        ekle("kare vitrin b", () -> kare(mc, "deprem_vitrin_b"), bekle(2), 4);
        ekle("S5 yan kamera", () -> kamera(mc, yan(mc, sahne)), bekle(240), 260);
        ekle("kare S5", () -> kare(mc, "deprem_s5"), bekle(2), 4);
        for (int i = 1; i <= SARSINTI_KARESI; i++) {
            String ad = "deprem_s5_sarsinti_" + i;
            ekle("kare S5 sarsıntı " + i, () -> kare(mc, ad), bekle(2), 4);
        }
        ekle("S5 geniş kamera", () -> {
            genisKamera = genis(mc, sahne);
            kamera(mc, genisKamera);
        }, olcumKapisi(40), olcumKapisiTavani(40));
        olcum("s5_deprem_yuksek", "s5_deprem_taban");
        ekle("kare aktif", () -> kare(mc, "deprem_aktif"), bekle(2), 4);
        ekle("S5 artçılar (hız 20)", () -> komut(mc, "dogalafet hiz 20"), afetYok(mc), 2400);
        ekle("hız 1, aynı geniş kamera", () -> {
            komut(mc, "dogalafet hiz 1");
            kamera(mc, genisKamera);
        }, olcumKapisi(40), olcumKapisiTavani(40));
        olcum("s5_deprem_taban", null);
        ekle("kare S5 bitiş", () -> kare(mc, "deprem_s5_bitis"), bekle(2), 4);
    }

    /** Çatlama (hız 0.2, çatlama alt aşamasında), çukur. */
    private static void sinkholeS1(Minecraft mc) {
        ekle("S1 sinkhole yerine git", () -> git(mc, 2), yuklendi(mc, 2, 64), 3000);
        ekle("sinkhole S1 başlat", () -> komut(mc, "dogalafet start sinkhole 1 @s"), bekle(20), 30);
        ekle("sinkhole S1 sahnesi", () -> {
            sahne = sinkholeSahnesi(mc);
            kamera(mc, cukurKamerasi(mc, sahne));
        }, () -> akici(60, 20), 1200);
        ekle("sinkhole S1 aktif (hız 0.2)", () -> komut(mc, "dogalafet hiz 0.2", "dogalafet asama ileri",
                "dogalafet asama ileri"), () -> akici(250, 20) && altAsama() == SinkholeSeviyesi.CATLAMA, 390);
        ekle("kare S1 çatlama", () -> kare(mc, "sinkhole_s1_catlama"), bekle(2), 4);
        ekle("sinkhole S1 durulma (hız 1)", () -> komut(mc, "dogalafet hiz 1"),
                () -> altAsama() == SinkholeSeviyesi.DURULMA && adimTick >= 2, 1200);
        ekle("çukura yerleşsin", () -> { }, () -> akici(40, 20), 600);
        ekle("kare sinkhole S1", () -> kare(mc, "sinkhole_s1"), bekle(2), 4);
        ekle("sinkhole S1 bitsin (hız 20)", () -> komut(mc, "dogalafet hiz 20"), afetYok(mc), 1200);
        ekle("hız 1 (S1)", () -> komut(mc, "dogalafet hiz 1"), bekle(10), 20);
    }

    /**
     * Çatlama, çöküş vitrini (hız 0.2), çöküş ve geri çekilmede ölçüm, üstten çukur, aynı çukur kamerasında taban ölçümü ve
     * bitiş.
     */
    private static void sinkholeS5(Minecraft mc) {
        ekle("S5 sinkhole yerine git", () -> git(mc, 3), yuklendi(mc, 3, 64), 3000);
        ekle("sinkhole S5 başlat", () -> komut(mc, "dogalafet start sinkhole 5 @s"), bekle(20), 30);
        ekle("sinkhole S5 sahnesi", () -> {
            sahne = sinkholeSahnesi(mc);
            cukurKamera = cukurKamerasi(mc, sahne);
            kamera(mc, cukurKamera);
        }, () -> akici(60, 20), 1200);
        ekle("sinkhole S5 aktif", () -> komut(mc, "dogalafet asama ileri", "dogalafet asama ileri"),
                () -> akici(80, 20) && altAsama() == SinkholeSeviyesi.CATLAMA, 130);
        ekle("kare S5 çatlama", () -> kare(mc, "sinkhole_s5_catlama"), bekle(2), 4);
        ekle("çekirdek çöküşü (hız 0.2)", () -> komut(mc, "dogalafet hiz 0.2"),
                () -> altAsama() >= SinkholeSeviyesi.COKUS && dusenSayisi(mc) >= 20 && GorselOtomasyon.akici(), 1500);
        ekle("düşüş başladı", () -> { }, bekle(6), 8);
        ekle("kare çöküş vitrini", () -> kare(mc, "sinkhole_cokus_vitrin"), bekle(14), 16);
        ekle("kare çöküş vitrini b", () -> kare(mc, "sinkhole_cokus_vitrin_b"), bekle(2), 4);
        ekle("hız 1 (çöküş ölçümü)", () -> komut(mc, "dogalafet hiz 1"), olcumKapisi(10), olcumKapisiTavani(10));
        olcum("s5_sinkhole_yuksek", "s5_sinkhole_taban");
        ekle("sinkhole S5 durulma", () -> { }, () -> altAsama() == SinkholeSeviyesi.DURULMA && adimTick >= 2, 1200);
        ekle("üstten kamera", () -> kamera(mc, ustten(mc, sahne)), () -> akici(60, 20), 1200);
        ekle("kare sinkhole S5", () -> kare(mc, "sinkhole_s5"), bekle(2), 4);
        ekle("sinkhole S5 bitsin (hız 20)", () -> komut(mc, "dogalafet hiz 20"), afetYok(mc), 1200);
        ekle("bitiş kamerası (aynı çukur kamerası)", () -> {
            komut(mc, "dogalafet hiz 1");
            kamera(mc, cukurKamera);
        }, olcumKapisi(40), olcumKapisiTavani(40));
        olcum("s5_sinkhole_taban", null);
        ekle("kare sinkhole bitiş", () -> kare(mc, "sinkhole_bitis"), bekle(2), 4);
    }

    /**
     * K-E29 (b) ölçüm penceresi ({@value #OLCUM_TICK} tick); {@code taban} aynı kameranın afetsiz sahnesi ya da {@code null}.
     */
    private static void olcum(String sahneAdi, String taban) {
        ekle("ölçüm " + sahneAdi, () -> GorselOtomasyon.olcumBasla("deprem", sahneAdi, taban), bekle(OLCUM_TICK),
                OLCUM_TICK + 1);
        ekle("ölçüm " + sahneAdi + " bitti", GorselOtomasyon::olcumBitir, bekle(1), 2);
    }

    // ---------------------------------------------------------------- koşullar

    private static BooleanSupplier bekle(int tick) {
        return () -> adimTick >= tick;
    }

    /**
     * En az {@code tick} tick geçti ve istemci akıcı (≥ {@value GorselOtomasyon#AKICI_FPS} FPS, adım başından ≥ {@code kare} kare)
     * çiziyor; ya da {@value #KAPI_PAYI} tick daha geçti ve en az iki yeni kare çizildi (gizli workspace, GP17).
     */
    private static boolean akici(int tick, int kare) {
        long cizilen = GorselOtomasyon.cizilenKare() - adimKaresi;
        return adimTick >= tick && (cizilen >= kare && GorselOtomasyon.akici()
                || adimTick >= tick + KAPI_PAYI && cizilen >= 2);
    }

    /** Ölçümden önceki FPS kapısı: GP17 kaçışı yok; {@link #olcumKapisiTavani} dolarsa adım WARN ile geçer. */
    private static BooleanSupplier olcumKapisi(int tick) {
        return () -> adimTick >= tick && GorselOtomasyon.akici();
    }

    private static int olcumKapisiTavani(int tick) {
        return tick + GorselOtomasyon.KAPI_SN * 20;
    }

    private static BooleanSupplier afetYok(Minecraft mc) {
        return () -> adimTick >= 20 && adimTick % 10 == 0
                && sunucuda(mc, () -> AfetYoneticisi.al(mc.getSingleplayerServer()).aktifAfet().isEmpty());
    }

    /** Sahne yerinin çevresindeki chunk'lar sunucuda ve merkez chunk istemcide yüklü, istemci akıcı. */
    private static BooleanSupplier yuklendi(Minecraft mc, int yer, int yaricap) {
        return () -> {
            if (adimTick < 100 || adimTick % 20 != 0 || !akici(100, 20)) {
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

    /** Sinkhole'un AKTIF alt aşaması; yoksa -1 (ON_UYARI ve SIREN paketleri de alt aşama 0 taşır). */
    private static int altAsama() {
        return IstemciAfetDurumu.aktif().filter(p -> p.tur() == AfetTuru.SINKHOLE && p.asama() == AfetAsamasi.AKTIF)
                .map(AfetDurumPaketi::altAsama).orElse(-1);
    }

    private static boolean depremAsamasi(AfetAsamasi asama) {
        return IstemciAfetDurumu.aktif().filter(p -> p.tur() == AfetTuru.DEPREM && p.asama() == asama).isPresent();
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

    /** A-34 kare isteği; satır kare alındığı anda (kapı açılınca) o anki sahne durumuyla yazılır. */
    private static void kare(Minecraft mc, String ad) {
        GorselOtomasyon.kareIste("deprem", "ds_" + ad + ".png", k -> {
            kareAdedi++;
            Optional<AfetDurumPaketi> durum = IstemciAfetDurumu.aktif();
            String afet = durum.map(p -> String.format(Locale.ROOT, "%s S%d %s/%d", p.tur().kimlik(), p.seviye(),
                    p.asama().kimlik(), p.altAsama())).orElse("yok");
            DogalAfetler.LOGGER.info(ON + "kare {} (FPS {}, istekten sonra çizilen kare {}, parçacık {}, düşen blok {}, "
                    + "sarsıntı {}°, afet {}, oyuncu {} {} {})", k.dosya(), k.fps(), k.yeniKare(),
                    mc.particleEngine.countParticles(), dusenSayisi(mc),
                    String.format(Locale.ROOT, "%.2f", IstemciSarsinti.genlik(mc.player, 0)[0]), afet,
                    Math.round(mc.player.getX()), Math.round(mc.player.getY()), Math.round(mc.player.getZ()));
        });
    }
}
