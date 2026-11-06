package io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.afet.gunes_patlamasi.mantik.GunesPatlamasiEgrileri;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimKimlikleri;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.core.Afet;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.GunesZinciriSureci;
import io.github.kadirberkpolat1.dogalafetler.komut.DogalAfetKomutu;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Güneş patlamasının oyun içi testleri (bölüm D, GOREV §6 #3, #8, #12, #16). Gerçek afet (taklit değil) kullanılır. Her
 * test global dönemi değiştirdiği için kendi {@code batch}'indedir; başta ve sonda dönem {@code sifirla} ile boş dünyanın
 * ilk hâline döner (K-E15; her koşu boş dünyayla başlar, K-E16) ve değiştirilen gamerule eski değerine döner. Meteor
 * yağdıran testlerde {@code afetBlokHasari} kapalıdır (krater ve ateş komşu sahnelere dokunmaz) ve afet aynı testte
 * {@code stop} ile temizce biter (uçan meteorlar kalkar).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GunesPatlamasiOyunTestleri {
    private static final float EPS = 1e-4f;

    private GunesPatlamasiOyunTestleri() {
    }

    private static AfetYoneticisi temizBasla(GameTestHelper y) {
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        yonetici.durdur();
        yonetici.sifirla();
        return yonetici;
    }

    private static GunesPatlamasi baslat(GameTestHelper y, AfetYoneticisi yonetici) {
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.GUNES_PATLAMASI, null, null, y.getLevel(),
                y.absolutePos(BlockPos.ZERO));
        y.assertTrue(sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "güneş patlaması başlamadı: " + sonuc);
        Afet afet = yonetici.aktifAfet().orElseThrow();
        y.assertTrue(afet instanceof GunesPatlamasi, "gerçek afet kayıtlı olmalı (taklit değil): " + afet.getClass());
        return (GunesPatlamasi) afet;
    }

    private static int komut(GameTestHelper y, String komut) {
        MinecraftServer server = y.getLevel().getServer();
        try {
            return server.getCommands().getDispatcher().execute(komut, server.createCommandSourceStack()
                    .withSuppressedOutput().withPermission(DogalAfetKomutu.YETKI_SEVIYESI));
        } catch (CommandSyntaxException hata) {
            y.fail("/" + komut + " çalışmadı: " + hata.getMessage());
            return -1;
        }
    }

    private static GameRules.BooleanValue blokHasari(GameTestHelper y) {
        return y.getLevel().getServer().getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
    }

    @GameTest(template = "bos", batch = "gunes_patlamasi_siren", timeoutTicks = 400)
    public static void sirendeGunesBuyurMeteorlarAktifteBaslarStopNormaleDondurur(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        MinecraftServer server = y.getLevel().getServer();
        DunyaDonemi donem = yonetici.donem();
        GameRules.BooleanValue hasar = blokHasari(y);
        boolean eskiHasar = hasar.get();
        hasar.set(false, server);
        Runnable bitir = () -> {
            yonetici.durdur();
            yonetici.sifirla();
            hasar.set(eskiHasar, server);
        };
        GunesPatlamasi afet = baslat(y, yonetici);
        y.assertTrue(donem.gunesEvresi() == GunesEvresi.NORMAL, "ön-uyarıda gök değişmez (N7): " + donem.ozet());
        yonetici.asamaIleri();
        y.assertTrue(afet.asama() == AfetAsamasi.SIREN && donem.gunesEvresi() == GunesEvresi.BUYUYEN
                && donem.evreIlerlemesi() == 0f, "siren başında güneş büyümeye başlamalı: " + donem.ozet());
        yonetici.hizAyarla(10);
        float[] onceki = {0f};
        boolean[] geriGitti = {false};
        y.onEachTick(() -> {
            float p = donem.evreIlerlemesi();
            if (donem.gunesEvresi() == GunesEvresi.BUYUYEN && p + 1e-6f < onceki[0]) {
                geriGitti[0] = true;
            }
            onceki[0] = p;
        });
        y.succeedWhen(() -> {
            y.assertTrue(afet.asama() == AfetAsamasi.AKTIF && MeteorAPI.aktifYagmurSayisi(y.getLevel()) >= 1,
                    "AKTIF ve meteor yağmuru bekleniyor: " + afet.asama() + ", ilerleme " + donem.evreIlerlemesi());
            float p = donem.evreIlerlemesi();
            y.assertTrue(p >= GunesPatlamasiEgrileri.SIREN_SONU - EPS && p < GunesPatlamasiEgrileri.SIREN_SONU + 0.01f,
                    "siren sonunda güneş 1,6 kat (ilerleme " + GunesPatlamasiEgrileri.SIREN_SONU + "): " + p);
            y.assertTrue(afet.altAsama() == 0 && afet.aktifKalanTick() > 0, "ilk gün, süreç sürüyor");
            yonetici.durdur();
            boolean normal = donem.gunesEvresi() == GunesEvresi.NORMAL;
            int yagmur = MeteorAPI.aktifYagmurSayisi(y.getLevel());
            int ucan = MeteorAPI.aktifMeteorSayisi(y.getLevel());
            bitir.run();
            y.assertFalse(geriGitti[0], "evre ilerlemesi geri gitmemeli");
            y.assertTrue(normal, "tamamlanmamış büyüme stop ile NORMAL'e dönmeli (K-C26)");
            y.assertTrue(yagmur == 0 && ucan == 0, "stop sonrası yağmur " + yagmur + ", uçan meteor " + ucan);
        });
    }

    @GameTest(template = "bos", batch = "gunes_patlamasi_dogal", timeoutTicks = 100)
    public static void dogalBitisSupernovaZinciriBaslatirGunesiAtlattimAcilir(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        MinecraftServer server = y.getLevel().getServer();
        DunyaDonemi donem = yonetici.donem();
        ServerLevel ust = server.overworld();
        BlockPos konum = y.absolutePos(new BlockPos(0, 1, 0));
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, konum.getX() + 0.5, konum.getY(), konum.getZ() + 0.5);
        try {
            GunesPatlamasi afet = baslat(y, yonetici);
            yonetici.asamaIleri();
            yonetici.asamaIleri();
            y.assertTrue(afet.asama() == AfetAsamasi.AKTIF && afet.etkilenenler().contains(oyuncu.getUUID()),
                    "AKTIF'te sunucu geneli afet herkesi etkiler");
            y.assertTrue(Math.abs(donem.evreIlerlemesi() - GunesPatlamasiEgrileri.SIREN_SONU) < EPS,
                    "AKTIF siren bitişinden sıçramadan başlar: " + donem.evreIlerlemesi());
            float onceki = donem.evreIlerlemesi();
            for (int gun = 1; gun <= 4; gun++) {
                yonetici.asamaIleri();
                y.assertTrue(afet.asama() == AfetAsamasi.AKTIF && afet.altAsama() == gun,
                        "asama ileri bir gün atlamalı: gün " + afet.altAsama());
                y.assertTrue(donem.evreIlerlemesi() > onceki, "güneş her gün büyür");
                onceki = donem.evreIlerlemesi();
            }
            yonetici.asamaIleri();
            y.assertTrue(donem.gunesEvresi() == GunesEvresi.SUPERNOVA, "doğal bitiş süpernova: " + donem.ozet());
            yonetici.asamaIleri();
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "afet doğal bitmeli");
            y.assertTrue(donem.gunesEvresi() == GunesEvresi.SUPERNOVA, "doğal bitiş süpernovayı geri almaz");
            y.assertTrue(GunesZinciriSureci.al().aktifMi(ust), "güneş zinciri süpernovadan devralmalı");
            Advancement basarim = server.getAdvancements().getAdvancement(BasarimKimlikleri.basarim(Basarimlar.GUNESI_ATLATTIM));
            y.assertTrue(basarim != null, "advancement yüklü değil (runData?)");
            y.assertTrue(oyuncu.getAdvancements().getOrStartProgress(basarim).isDone(),
                    "Güneşi Atlattım gerçek afetin doğal bitişiyle açılmalı");
        } finally {
            server.getPlayerList().remove(oyuncu);
            yonetici.durdur();
            yonetici.sifirla();
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "gunes_patlamasi_zincir", timeoutTicks = 300)
    public static void zincirKizilCuceyeKadarSurerBaslamaKapisiZinciriKorur(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        BlockPos merkez = y.absolutePos(BlockPos.ZERO);
        baslat(y, yonetici);
        for (int i = 0; i < 8 && yonetici.aktifAfet().isPresent(); i++) {
            yonetici.asamaIleri();
        }
        y.assertTrue(yonetici.aktifAfet().isEmpty() && donem.gunesEvresi() == GunesEvresi.SUPERNOVA,
                "siren + 5 gün + bitiş: " + donem.ozet());
        y.assertTrue(yonetici.elleBaslat(AfetTuru.GUNES_PATLAMASI, null, null, y.getLevel(), merkez)
                == AfetYoneticisi.BaslatmaSonucu.YER_UYGUN_DEGIL, "süpernovada yeni güneş patlaması başlamamalı");
        yonetici.hizAyarla(1000);
        boolean[] kizilCuce = {false};
        y.succeedWhen(() -> {
            if (!kizilCuce[0]) {
                y.assertTrue(donem.gunesEvresi() == GunesEvresi.GUNESSIZ, "süpernova sönüp güneşsiz bekleniyor: " + donem.ozet());
                donem.donmaIlerlemesiAyarla(100f);
                y.assertTrue(komut(y, "dogalafet kizilcuce baslat") == 1, "kizilcuce baslat");
                kizilCuce[0] = true;
            }
            y.assertTrue(donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE, "kızıl cüce: " + donem.ozet());
            boolean dogusSurerkenRed = donem.evreIlerlemesi() >= 1f
                    || yonetici.elleBaslat(AfetTuru.GUNES_PATLAMASI, null, null, y.getLevel(), merkez)
                            == AfetYoneticisi.BaslatmaSonucu.YER_UYGUN_DEGIL;
            komut(y, "dogalafet asama ileri gunes_zinciri");
            boolean tamam = donem.evreIlerlemesi() >= 1f;
            AfetYoneticisi.BaslatmaSonucu yeni = yonetici.elleBaslat(AfetTuru.GUNES_PATLAMASI, null, null, y.getLevel(), merkez);
            yonetici.durdur();
            boolean kizilKaldi = donem.gunesEvresi() == GunesEvresi.KIZIL_CUCE;
            yonetici.sifirla();
            y.assertTrue(dogusSurerkenRed, "doğuş sürerken güneş patlaması başlamamalı");
            y.assertTrue(tamam, "asama ileri gunes_zinciri doğuşu tamamlamalı");
            y.assertTrue(yeni == AfetYoneticisi.BaslatmaSonucu.BASLADI, "tamamlanmış kızıl cücede elle başlar: " + yeni);
            y.assertTrue(kizilKaldi, "ön-uyarıda durdurulan afet kızıl cüceye dokunmaz");
        });
    }

    @GameTest(template = "bos", batch = "gunes_patlamasi_kavrulma")
    public static void kavrulmaCimeniKabaToprakYaparBlokHasariKapaliykenDokunmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        MinecraftServer server = level.getServer();
        GameRules.BooleanValue hasar = blokHasari(y);
        boolean eskiHasar = hasar.get();
        BlockPos konum = y.absolutePos(BlockPos.ZERO);
        try {
            for (int dy = 1; dy <= 4; dy++) {
                level.setBlockAndUpdate(konum.above(dy), Blocks.AIR.defaultBlockState());
            }
            level.setBlockAndUpdate(konum, Blocks.GRASS_BLOCK.defaultBlockState());
            hasar.set(false, server);
            y.assertFalse(GunesPatlamasi.kavur(level, konum), "afetBlokHasari kapalıyken blok değişmez");
            y.assertTrue(level.getBlockState(konum).is(Blocks.GRASS_BLOCK), "çimen kalmalı");
            hasar.set(true, server);
            y.assertTrue(GunesPatlamasi.kavur(level, konum), "göğü gören çimen kavrulmalı");
            y.assertTrue(level.getBlockState(konum).is(Blocks.COARSE_DIRT), "kaba toprak bekleniyor");
            y.assertFalse(GunesPatlamasi.kavur(level, konum), "kaba toprak yeniden kavrulmaz");
            level.setBlockAndUpdate(konum, Blocks.GRASS_BLOCK.defaultBlockState());
            level.setBlockAndUpdate(konum.above(3), Blocks.STONE.defaultBlockState());
            y.assertFalse(GunesPatlamasi.kavur(level, konum), "çatı altındaki çimen kavrulmaz");
        } finally {
            hasar.set(eskiHasar, server);
            level.setBlockAndUpdate(konum.above(3), Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(konum, Blocks.AIR.defaultBlockState());
        }
        y.succeed();
    }

    @GameTest(template = "bos", batch = "gunes_patlamasi_kayit")
    public static void kayitOkumaGunuVeKalaniKorurZincirDisindaBaslamaz(GameTestHelper y) {
        AfetYoneticisi yonetici = temizBasla(y);
        DunyaDonemi donem = yonetici.donem();
        try {
            GunesPatlamasi afet = baslat(y, yonetici);
            yonetici.asamaIleri();
            yonetici.asamaIleri();
            yonetici.asamaIleri();
            yonetici.asamaIleri();
            y.assertTrue(afet.altAsama() == 2, "üçüncü gün: " + afet.altAsama());
            CompoundTag etiket = afet.durumYaz(new CompoundTag());
            GunesPatlamasi okunan = new GunesPatlamasi();
            okunan.durumOku(etiket);
            y.assertTrue(okunan.asama() == AfetAsamasi.AKTIF && okunan.altAsama() == 2, "aşama ve gün korunmalı");
            y.assertTrue(okunan.aktifGecen() == afet.aktifGecen() && okunan.aktifKalanTick() == afet.aktifKalanTick(),
                    "geçen/kalan korunmalı: " + okunan.aktifKalanTick() + " != " + afet.aktifKalanTick());
            y.assertTrue(okunan.aktifKalanTick() == 3 * GunesPatlamasiEgrileri.GUN_TICK, "kalan 3 gün");
            yonetici.durdur();
            for (GunesEvresi evre : new GunesEvresi[] {GunesEvresi.BUYUYEN, GunesEvresi.SUPERNOVA, GunesEvresi.GUNESSIZ}) {
                donem.gunesEvresiAyarla(evre);
                y.assertTrue(yonetici.elleBaslat(AfetTuru.GUNES_PATLAMASI, null, null, y.getLevel(),
                        y.absolutePos(BlockPos.ZERO)) == AfetYoneticisi.BaslatmaSonucu.YER_UYGUN_DEGIL,
                        evre + " evresinde güneş patlaması başlamamalı");
            }
        } finally {
            yonetici.durdur();
            yonetici.sifirla();
        }
        y.succeed();
    }
}
