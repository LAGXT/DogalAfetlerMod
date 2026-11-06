package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetKaydi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.CekirdekOyunTestleri;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiAfeti;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.function.BiPredicate;
import java.util.function.DoubleSupplier;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Ortak yangının oyun içi testleri (şartname §6 #3, #10). GameTest dünyasında {@code doFireTick=false}'tur; yönetici
 * vanilla rastgele tick'e bağlı değildir. Dünya geneli durumu (gamerule, hava, TPS kademesi) değiştiren testler kendi
 * batch'indedir ve değeri iddialardan önce geri koyar.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = DogalAfetler.MOD_ID)
public final class YanginOyunTestleri {
    private static final String SAHNE = "yangin_sahne";
    /** Sıçrama eşiğinin altında (kıvılcım komşu test alanlarına düşmez); yerel yayılma sürer. */
    private static final double DUSUK = 0.1;
    /** Testte {@code kaynakBitti(temiz=false)} sönüm penceresi (tick); meşe kütüğü 444 tick yanar. */
    private static final int SONUM_TESTI = 80;

    private YanginOyunTestleri() {
    }

    /**
     * GameTest dünyası koşular arasında saklanır (K-C24) ve test sırası sabit olduğundan sahneler aynı yere kurulur: zaman
     * aşımıyla biten bir testin kayıtlı hücreleri sonraki koşuda aynı sahnenin yeni yakıtını tutuşturmasın.
     */
    @SubscribeEvent
    public static void oyunTestiSunucusuBasladi(ServerStartedEvent olay) {
        if (!(olay.getServer() instanceof GameTestServer)) {
            return;
        }
        for (ServerLevel level : olay.getServer().getAllLevels()) {
            int sonen = YanginYoneticisi.sondur(level, null);
            if (sonen > 0) {
                DogalAfetler.LOGGER.info("GameTest dünyası: önceki koşudan kalan {} yangın hücresi söndürüldü ({})", sonen,
                        level.dimension().location());
            }
        }
    }

    /**
     * Y-01 deneyi: {@code doFireTick} açıkken {@link AfetAtesiBlogu} kendi başına yayılmaz, yakmaz, sönmez. Karşılaştırma
     * için aynı sahnede (3 blok aralıklı ikinci döşemede) vanilla ateşin değiştirdiği blok sayısı log'a yazılır.
     */
    @GameTest(template = SAHNE, batch = "yangin_deney", timeoutTicks = 220)
    public static void afetAtesiKendiBasinaYayilmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        GameRules.BooleanValue ates = level.getGameRules().getRule(GameRules.RULE_DOFIRETICK);
        boolean onceki = ates.get();
        ates.set(true, level.getServer());
        for (int x = 0; x <= 2; x++) {
            for (int z = 3; z <= 5; z++) {
                y.setBlock(new BlockPos(x, 1, z), Blocks.OAK_PLANKS);
                y.setBlock(new BlockPos(x + 6, 1, z), Blocks.OAK_PLANKS);
            }
        }
        y.setBlock(new BlockPos(1, 2, 4), YanginKayitlari.AFET_ATESI.get());
        y.setBlock(new BlockPos(7, 2, 4), Blocks.FIRE);
        BlockState[] ilkAfet = durumlar(y, 0, 2);
        BlockState[] ilkVanilla = durumlar(y, 6, 8);
        y.runAfterDelay(200, () -> {
            ates.set(onceki, level.getServer());
            int afetDegisen = farkSayisi(ilkAfet, durumlar(y, 0, 2));
            int vanillaDegisen = farkSayisi(ilkVanilla, durumlar(y, 6, 8));
            for (BlockPos p : BlockPos.betweenClosed(y.absolutePos(new BlockPos(-1, 0, -1)),
                    y.absolutePos(new BlockPos(9, 7, 9)))) {
                if (level.getBlockState(p).is(Blocks.FIRE) || level.getBlockState(p).is(YanginKayitlari.AFET_ATESI.get())) {
                    level.removeBlock(p, false);
                }
            }
            DogalAfetler.LOGGER.info("Y-01 deney: doFireTick=true, 200 tick: afet ateşi {} blok, vanilla ateş {} blok değiştirdi",
                    afetDegisen, vanillaDegisen);
            y.assertTrue(afetDegisen == 0, "doFireTick açıkken afet ateşi blok değiştirmemeli: " + afetDegisen);
            y.succeed();
        });
    }

    @GameTest(template = SAHNE, batch = "yangin_genel", timeoutTicks = 320)
    public static void yanmazTasYanmazYaprakYanar(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos tas = new BlockPos(2, 1, 2);
        BlockPos yaprak = new BlockPos(6, 1, 6);
        y.setBlock(tas, Blocks.STONE);
        y.setBlock(yaprak, kaliciYaprak());
        y.assertTrue(Blocks.STONE.defaultBlockState().is(YanginEtiketleri.YANMAZ), "taş dogalafetler:yanmaz etiketinde");
        y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(tas), YanginKaynagi.KOMUT, DUSUK) == 0,
                "yanmaz taş tutuşmamalı");
        y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(yaprak), YanginKaynagi.KOMUT, DUSUK) == 1,
                "yaprak tutuşmalı");
        y.assertBlockPresent(YanginKayitlari.AFET_ATESI.get(), yaprak.above());
        y.succeedWhen(() -> {
            y.assertBlockPresent(Blocks.AIR, yaprak);
            y.assertBlockPresent(Blocks.STONE, tas);
            y.assertTrue(aktif(y) == 0 && atesSayisi(y) == 0, "yangın bitmeli, ateş bloğu kalmamalı");
        });
    }

    /**
     * Oyun içinde yayılma: yoğun yaprak kümesinin alt katmanındaki iki hücre kümeye yayılır (sıçrama eşiğinin altında).
     * İki tohum: bu yoğunlukta tek hücre yayılmadan sönebilir (Y-16).
     */
    @GameTest(template = SAHNE, batch = "yangin_yayilma", timeoutTicks = 300)
    public static void yaprakKumesindeYayilir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(7, 3, 7))) {
            y.setBlock(p, kaliciYaprak());
        }
        for (BlockPos tohum : new BlockPos[] {new BlockPos(3, 1, 4), new BlockPos(5, 1, 4)}) {
            y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(tohum), YanginKaynagi.KOMUT, DUSUK) == 1,
                    "tohum yaprağı tutuşmalı: " + tohum.toShortString());
        }
        long baslangic = level.getGameTime();
        y.succeedWhen(() -> {
            int etkilenen = 0;
            for (BlockPos p : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(7, 3, 7))) {
                if (!y.getBlockState(p).is(Blocks.OAK_LEAVES)
                        || YanginYoneticisi.veri(level).hucreler.containsKey(y.absolutePos(p).asLong())) {
                    etkilenen++;
                }
            }
            y.assertTrue(etkilenen >= 12, "yaprak kümesinde yayılmalı: " + etkilenen);
            DogalAfetler.LOGGER.info("yayılma deneyi: {} tick'te {} yaprak tutuştu ya da yandı",
                    level.getGameTime() - baslangic, etkilenen);
            YanginYoneticisi.sondur(level, genisAlan(y));
        });
    }

    /** Y-12: ateş bloğu dışarıdan kalkarsa (oyuncu vurdu, su sildi) hücre söner, yakıt yerinde kalır. */
    @GameTest(template = SAHNE, batch = "yangin_genel", timeoutTicks = 60)
    public static void atesBloguKalkincaHucreSoner(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos yaprak = new BlockPos(4, 1, 4);
        y.setBlock(yaprak, kaliciYaprak());
        y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(yaprak), YanginKaynagi.KOMUT, DUSUK) == 1,
                "yaprak tutuşmalı");
        y.assertBlockPresent(YanginKayitlari.AFET_ATESI.get(), yaprak.above());
        y.setBlock(yaprak.above(), Blocks.AIR);
        y.succeedWhen(() -> {
            y.assertTrue(aktif(y) == 0, "ateş bloğu kalkınca hücre sönmeli");
            y.assertBlockPresent(Blocks.OAK_LEAVES, yaprak);
            y.assertTrue(atesSayisi(y) == 0, "yeni ateş bloğu konmamalı");
        });
    }

    @GameTest(template = SAHNE, batch = "yangin_hasarsiz", timeoutTicks = 400)
    public static void blokHasariKapaliykenBlokDegismez(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        GameRules.BooleanValue kural = level.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        boolean onceki = kural.get();
        kural.set(false, level.getServer());
        dosemeKur(y, 1, Blocks.OAK_PLANKS.defaultBlockState());
        dosemeKur(y, 2, kaliciYaprak());
        int ilk = ozet(y);
        int tutusan = YanginYoneticisi.alanTutustur(level, y.absolutePos(new BlockPos(4, 2, 4)), 3,
                YanginKaynagi.KOMUT, 1.0);
        y.runAfterDelay(20, () -> {
            int aktif = aktif(y);
            int ozet = ozet(y);
            y.runAfterDelay(300, () -> {
                kural.set(onceki, level.getServer());
                int son = ozet(y);
                YanginYoneticisi.sondur(level, genisAlan(y));
                y.assertTrue(tutusan > 0 && aktif > 0, "blok hasarı kapalıyken de yangın (veri/görsel) sürmeli: "
                        + tutusan + "/" + aktif);
                y.assertTrue(ozet == ilk && son == ilk, "blok hasarı kapalıyken blok hash'i değişmemeli");
                y.succeed();
            });
        });
    }

    @GameTest(template = SAHNE, batch = "yangin_yagmur", timeoutTicks = 240)
    public static void yagmurdaSoner(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        level.setWeatherParameters(0, 6000, true, false);
        level.setRainLevel(1.0F);
        y.runAfterDelay(235, () -> {
            level.setWeatherParameters(6000, 0, false, false);
            level.setRainLevel(0.0F);
        });
        BlockPos kutuk = new BlockPos(4, 1, 4);
        y.setBlock(kutuk, Blocks.OAK_LOG);
        y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(kutuk), YanginKaynagi.KOMUT, DUSUK) == 1,
                "kütük tutuşmalı");
        y.succeedWhen(() -> {
            y.assertTrue(aktif(y) == 0, "yağmurda ateş sönmeli");
            level.setWeatherParameters(6000, 0, false, false);
            level.setRainLevel(0.0F);
            y.assertBlockPresent(Blocks.OAK_LOG, kutuk);
            y.assertTrue(atesSayisi(y) == 0, "sönen ateşin bloğu kalkmalı");
        });
    }

    /**
     * Yağmurda yangın yayılmaz (Y-08): cam çatı altındaki (korunaklı) yaprak katmanında seviye 4 yoğunluğundaki iki hücre
     * komşularına geçmeden söner ya da yanıp biter.
     */
    @GameTest(template = SAHNE, batch = "yangin_yagmur_korunakli", timeoutTicks = 300)
    public static void yagmurdaKorunakliAtesYayilmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        level.setWeatherParameters(0, 6000, true, false);
        level.setRainLevel(1.0F);
        y.runAfterDelay(295, () -> {
            level.setWeatherParameters(6000, 0, false, false);
            level.setRainLevel(0.0F);
        });
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                y.setBlock(new BlockPos(x, 3, z), Blocks.GLASS);
                if (x >= 2 && x <= 6 && z >= 2 && z <= 6) {
                    y.setBlock(new BlockPos(x, 1, z), kaliciYaprak());
                }
            }
        }
        for (BlockPos tohum : new BlockPos[] {new BlockPos(3, 1, 4), new BlockPos(5, 1, 4)}) {
            y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(tohum), YanginKaynagi.KOMUT, 0.7) == 1,
                    "tohum yaprağı tutuşmalı: " + tohum.toShortString());
        }
        int[] enCok = {0};
        String[] enCokDurum = {""};
        y.onEachTick(() -> {
            int sayi = aktif(y);
            if (sayi > enCok[0]) {
                enCok[0] = sayi;
                enCokDurum[0] = hucreKonumlari(y) + " yağmur=" + level.isRaining();
            }
        });
        y.succeedWhen(() -> {
            y.assertTrue(enCok[0] <= 2, "yağmurda yayılmamalı: en çok " + enCok[0] + " hücre " + enCokDurum[0]);
            y.assertTrue(aktif(y) == 0 && atesSayisi(y) == 0, "korunaklı ateş de sönmeli");
            level.setWeatherParameters(6000, 0, false, false);
            level.setRainLevel(0.0F);
            YanginYoneticisi.sondur(level, genisAlan(y));
        });
    }

    /** §6 #10: TPS kademesi düşünce bütçe düşer ve log'a {@code yangın kısma} satırı düşer. */
    @GameTest(template = SAHNE, batch = "yangin_kisma", timeoutTicks = 60)
    public static void tpsKismasiButceyiDusurur(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        DoubleSupplier onceki = YanginYoneticisi.kismaKaynagiAyarla(() -> 0.25);
        BlockPos yaprak = new BlockPos(4, 1, 4);
        y.setBlock(yaprak, kaliciYaprak());
        YanginYoneticisi.tutustur(level, y.absolutePos(yaprak), YanginKaynagi.KOMUT, 1.0);
        y.runAfterDelay(5, () -> {
            int butce = YanginYoneticisi.sonButce(level);
            int etkin = YanginYoneticisi.etkinButce();
            YanginYoneticisi.kismaKaynagiAyarla(onceki);
            YanginYoneticisi.sondur(level, alan(y));
            int beklenen = YanginMantigi.butce(YanginAyarlari.tickButcesi(), 0.25);
            y.assertTrue(butce == beklenen && etkin == beklenen,
                    "çeyrek kademede bütçe " + beklenen + " olmalı, ölçülen " + butce + "/" + etkin);
            y.assertTrue(beklenen < YanginAyarlari.tickButcesi(), "kısma bütçeyi düşürmeli");
            y.succeed();
        });
    }

    @GameTest(template = SAHNE, batch = "yangin_genel")
    public static void sondurIzBirakmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        dosemeKur(y, 1, Blocks.OAK_PLANKS.defaultBlockState());
        dosemeKur(y, 2, kaliciYaprak());
        int ilk = ozet(y);
        int tutusan = YanginYoneticisi.alanTutustur(level, y.absolutePos(new BlockPos(4, 2, 4)), 3,
                YanginKaynagi.KOMUT, 1.0);
        y.assertTrue(tutusan > 0 && atesSayisi(y) > 0, "tutuşma ateş bloğu koymalı");
        int sonen = YanginYoneticisi.sondur(level, alan(y));
        y.assertTrue(sonen == tutusan, "tüm hücreler sönmeli: " + sonen + "/" + tutusan);
        y.assertTrue(aktif(y) == 0 && atesSayisi(y) == 0, "aktif ve ateş bloğu kalmamalı");
        y.assertTrue(ozet(y) == ilk, "söndürme iz bırakmamalı (yakıt yerinde)");
        y.succeed();
    }

    /** K-E11: {@code /dogalafet sifirla} komutla yakılanlar dahil bütün hücreleri söndürür; ateş bloğu kalmaz. */
    @GameTest(template = SAHNE, batch = "yangin_sifirla")
    public static void sifirlaYanginlariSondurur(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        dosemeKur(y, 1, Blocks.OAK_PLANKS.defaultBlockState());
        dosemeKur(y, 2, kaliciYaprak());
        int ilk = ozet(y);
        y.assertTrue(komut(y, Vec3.atCenterOf(y.absolutePos(new BlockPos(4, 3, 4))), "dogalafet yangin tutustur 4") > 0
                && atesSayisi(y) > 0, "tutustur 4 ateş koymalı");
        y.assertTrue(komut(y, Vec3.atCenterOf(y.absolutePos(BlockPos.ZERO)), "dogalafet sifirla") == 1, "sifirla");
        y.assertTrue(YanginYoneticisi.aktifSayi(level) == 0, "sifirla sonrası aktif hücre kaldı: "
                + YanginYoneticisi.aktifSayi(level));
        y.assertTrue(YanginYoneticisi.bekleyenAlanSayisi(level) == 0, "sifirla sonrası alan işi kaldı");
        y.assertTrue(atesSayisi(y) == 0, "sifirla sonrası afet_atesi kaldı: " + atesSayisi(y));
        y.assertTrue(ozet(y) == ilk, "sıfırlama iz bırakmamalı (yakıt yerinde)");
        y.succeed();
    }

    private static int komut(GameTestHelper y, Vec3 konum, String komut) {
        var server = y.getLevel().getServer();
        try {
            return server.getCommands().getDispatcher().execute(komut, server.createCommandSourceStack()
                    .withLevel(y.getLevel()).withPosition(konum).withSuppressedOutput().withPermission(2));
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException hata) {
            y.fail("/" + komut + " çalışmadı: " + hata.getMessage());
            return 0;
        }
    }

    @GameTest(template = SAHNE, batch = "yangin_genel")
    public static void kayitYazOkuSurer(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos yaprak = new BlockPos(4, 1, 4);
        y.setBlock(yaprak, kaliciYaprak());
        YanginKaynagi kaynak = YanginKaynagi.afet(AfetTuru.ORMAN_YANGINI, 42L);
        BlockPos mutlak = y.absolutePos(yaprak);
        YanginYoneticisi.tutustur(level, mutlak, kaynak, 0.8);
        YanginHucresi asil = YanginYoneticisi.veri(level).hucreler.get(mutlak.asLong());
        YanginVerisi kopya = YanginVerisi.yukle(YanginYoneticisi.veri(level).save(new CompoundTag()));
        YanginHucresi okunan = kopya.hucreler.get(mutlak.asLong());
        YanginYoneticisi.sondur(level, alan(y));
        y.assertTrue(asil != null && okunan != null, "hücre kayıtta olmalı");
        y.assertTrue(okunan.kaynak.equals(kaynak) && okunan.bitis == asil.bitis && okunan.yogunluk == asil.yogunluk
                && okunan.atesVar && okunan.atesKonumu == asil.atesKonumu && okunan.yayilabilir,
                "yaz/oku sonrası hücre aynı olmalı");
        y.succeed();
    }

    /**
     * Y-19: ateş bloğu yüklü olmayan chunk'tayken hücre sönerse blok bekleyen silmeye girer; chunk yüklenince
     * ({@code ChunkEvent.Load}) kalkar. "Yüklü değil" sahnenin alanı için test kancasıyla taklit edilir; 1 Hz denetim kapalıdır,
     * yani bloğu yalnız olay yolu kaldırabilir. Gerçek boşalt/yükle yolu runServer senaryosunda (forceload).
     */
    @GameTest(template = SAHNE, batch = "yangin_bekleyen", timeoutTicks = 100)
    public static void yukluOlmayanAtesChunkYuklenincekalkar(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos yaprak = new BlockPos(4, 1, 4);
        y.setBlock(yaprak, kaliciYaprak());
        y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(yaprak), YanginKaynagi.KOMUT, DUSUK) == 1,
                "yaprak tutuşmalı");
        BlockPos ates = y.absolutePos(yaprak.above());
        AABB bolge = alan(y);
        YanginYoneticisi.bekleyenDenetimiAyarla(false);
        y.runAfterDelay(95, () -> YanginYoneticisi.bekleyenDenetimiAyarla(true));
        BiPredicate<ServerLevel, BlockPos> onceki = YanginYoneticisi.yukluKaynagiAyarla(
                (l, p) -> !bolge.contains(Vec3.atCenterOf(p)) && l.isLoaded(p));
        try {
            y.assertTrue(YanginYoneticisi.sondur(level, bolge) == 1, "hücre sönmeli");
            y.assertTrue(aktif(y) == 0, "aktif hücre kalmamalı");
            y.assertBlockPresent(YanginKayitlari.AFET_ATESI.get(), yaprak.above());
            y.assertTrue(YanginYoneticisi.veri(level).bekleyenSilmeler.contains(ates.asLong()),
                    "yüklü olmayan ateş bekleyen silmede olmalı");
        } finally {
            YanginYoneticisi.yukluKaynagiAyarla(onceki);
        }
        YanginYoneticisi.chunkYuklendi(new ChunkEvent.Load(level.getChunk(ates), false));
        y.succeedWhen(() -> {
            y.assertBlockPresent(Blocks.AIR, yaprak.above());
            y.assertBlockPresent(Blocks.OAK_LEAVES, yaprak);
            y.assertTrue(!YanginYoneticisi.veri(level).bekleyenSilmeler.contains(ates.asLong()),
                    "bekleyen silme boşalmalı");
            YanginYoneticisi.bekleyenDenetimiAyarla(true);
        });
    }

    /** {@code kaynakBitti(temiz=true)}: o kaynağın ateşi hemen söner, yakıt korunur; başka kaynak etkilenmez. */
    @GameTest(template = SAHNE, batch = "yangin_genel")
    public static void kaynakBittiTemizSondurur(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos a = new BlockPos(2, 1, 2);
        BlockPos b = new BlockPos(6, 1, 6);
        y.setBlock(a, kaliciYaprak());
        y.setBlock(b, kaliciYaprak());
        int ilk = ozet(y);
        YanginKaynagi kaynak = YanginKaynagi.afet(AfetTuru.ORMAN_YANGINI, 9001L);
        YanginKaynagi baska = YanginKaynagi.afet(AfetTuru.ORMAN_YANGINI, 9101L);
        y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(a), kaynak, DUSUK) == 1, "a tutuşmalı");
        y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(b), baska, DUSUK) == 1, "b tutuşmalı");
        YanginYoneticisi.kaynakBitti(level.getServer(), kaynak, true);
        int kalan = aktif(y);
        int ates = atesSayisi(y);
        YanginYoneticisi.sondur(level, alan(y));
        y.assertTrue(kalan == 1 && ates == 1, "yalnız öteki kaynağın hücresi kalmalı: " + kalan + "/" + ates);
        y.assertTrue(ozet(y) == ilk, "temiz bitiş yakıtı korur, ateş bloğu bırakmaz");
        y.succeed();
    }

    /**
     * {@code kaynakBitti(temiz=false)}: yayılma durur ve bitiş sönüm penceresine kısalır. Pencere testte 80 tick, yakıt meşe
     * kütüğü (yanma süresi 444 tick): kütük kaynakBitti olmadan 80 tick'te yanmazdı.
     */
    @GameTest(template = SAHNE, batch = "yangin_genel", timeoutTicks = 160)
    public static void kaynakBittiYanarakBiter(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos kutuk = new BlockPos(4, 1, 4);
        y.setBlock(kutuk, Blocks.OAK_LOG);
        YanginKaynagi kaynak = YanginKaynagi.afet(AfetTuru.GUNES_PATLAMASI, 9002L);
        y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(kutuk), kaynak, DUSUK) == 1, "tutuşmalı");
        YanginHucresi h = YanginYoneticisi.veri(level).hucreler.get(y.absolutePos(kutuk).asLong());
        long simdi = level.getGameTime();
        long ilkBitis = h.bitis;
        IntSupplier onceki = YanginYoneticisi.sonumKaynagiAyarla(() -> SONUM_TESTI);
        try {
            YanginYoneticisi.kaynakBitti(level.getServer(), kaynak, false);
        } finally {
            YanginYoneticisi.sonumKaynagiAyarla(onceki);
        }
        y.assertTrue(!h.yayilabilir, "hücre yayılmamalı");
        y.assertTrue(ilkBitis > simdi + SONUM_TESTI && h.bitis <= simdi + SONUM_TESTI,
                "bitiş sönüm penceresine kısalmalı: " + (ilkBitis - simdi) + " -> " + (h.bitis - simdi));
        y.succeedWhen(() -> {
            y.assertBlockPresent(Blocks.AIR, kutuk);
            y.assertTrue(aktif(y) == 0 && atesSayisi(y) == 0, "yanıp bitmeli");
        });
    }

    /** Y-27: {@code /dogalafet stop} ({@code KOMUT}) olayıyla afetin ateşi hemen söner, yakıt korunur. */
    @GameTest(template = SAHNE, batch = "yangin_afet_stop")
    public static void afetDurunceAtesiSoner(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        BlockPos yaprak = new BlockPos(4, 1, 4);
        y.setBlock(yaprak, kaliciYaprak());
        BlockPos merkez = y.absolutePos(BlockPos.ZERO);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, merkez.getX() + 0.5, merkez.getY() + 1, merkez.getZ() + 0.5);
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                () -> new OyunTestiAfeti(AfetTuru.DEPREM, 16, 100))) {
            y.assertTrue(yonetici.elleBaslat(AfetTuru.DEPREM, 1, oyuncu, level, merkez)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "afet başlamadı");
            YanginKaynagi kaynak = YanginKaynagi.afet(AfetTuru.DEPREM,
                    yonetici.aktifAfet().orElseThrow().baglam().baslangicSaati());
            y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(yaprak), kaynak, DUSUK) == 1, "tutuşmalı");
            y.assertTrue(yonetici.durdur(), "afet durmalı");
        } finally {
            yonetici.durdur();
            level.getServer().getPlayerList().remove(oyuncu);
        }
        y.assertTrue(aktif(y) == 0 && atesSayisi(y) == 0, "stop ateşi hemen söndürmeli");
        y.assertBlockPresent(Blocks.OAK_LEAVES, yaprak);
        y.succeed();
    }

    /** Y-27: doğal bitiş ({@code DOGAL}) olayıyla ateş yayılmayı bırakır ve sönüm penceresinde yanıp biter. */
    @GameTest(template = SAHNE, batch = "yangin_afet_dogal", timeoutTicks = 160)
    public static void afetDogalBitinceAtesYanipBiter(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        BlockPos kutuk = new BlockPos(4, 1, 4);
        y.setBlock(kutuk, Blocks.OAK_LOG);
        BlockPos merkez = y.absolutePos(BlockPos.ZERO);
        ServerPlayer oyuncu = CekirdekOyunTestleri.sahteOyuncu(y, merkez.getX() + 0.5, merkez.getY() + 1, merkez.getZ() + 0.5);
        long simdi;
        YanginHucresi h;
        IntSupplier onceki = YanginYoneticisi.sonumKaynagiAyarla(() -> SONUM_TESTI);
        try (AfetKaydi.GeciciKayit kayit = AfetKaydi.geciciKaydet(AfetTuru.DEPREM,
                () -> new OyunTestiAfeti(AfetTuru.DEPREM, 16, 100))) {
            y.assertTrue(yonetici.elleBaslat(AfetTuru.DEPREM, 1, oyuncu, level, merkez)
                    == AfetYoneticisi.BaslatmaSonucu.BASLADI, "afet başlamadı");
            YanginKaynagi kaynak = YanginKaynagi.afet(AfetTuru.DEPREM,
                    yonetici.aktifAfet().orElseThrow().baglam().baslangicSaati());
            y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(kutuk), kaynak, DUSUK) == 1, "tutuşmalı");
            h = YanginYoneticisi.veri(level).hucreler.get(y.absolutePos(kutuk).asLong());
            simdi = level.getGameTime();
            for (int i = 0; i < 8 && yonetici.aktifAfet().isPresent(); i++) {
                yonetici.asamaIleri();
            }
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "afet doğal bitmeli");
        } finally {
            YanginYoneticisi.sonumKaynagiAyarla(onceki);
            yonetici.durdur();
            level.getServer().getPlayerList().remove(oyuncu);
        }
        y.assertTrue(h != null && !h.yayilabilir && h.bitis <= simdi + SONUM_TESTI,
                "doğal bitişte hücre yayılmamalı ve sönüm penceresinde bitmeli");
        y.assertTrue(aktif(y) == 1, "doğal bitiş ateşi hemen söndürmemeli");
        y.succeedWhen(() -> {
            y.assertBlockPresent(Blocks.AIR, kutuk);
            y.assertTrue(aktif(y) == 0 && atesSayisi(y) == 0, "yanıp bitmeli");
        });
    }

    /**
     * Y-20: üstü dolu yakıtın ateşi yanına konur ve yakıta dönük yan alevi olarak çizilir (multipart); dayanağı kalkan ateş
     * hemen havaya döner, hücresi söner.
     */
    @GameTest(template = SAHNE, batch = "yangin_genel", timeoutTicks = 60)
    public static void yanAlevYonluDayanaksizSoner(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        BlockPos yaprak = new BlockPos(4, 3, 4);
        y.setBlock(yaprak, kaliciYaprak());
        y.setBlock(yaprak.above(), Blocks.STONE);
        y.assertTrue(YanginYoneticisi.tutustur(level, y.absolutePos(yaprak), YanginKaynagi.KOMUT, DUSUK) == 1,
                "yaprak tutuşmalı");
        BlockPos yan = yaprak.north();
        y.assertBlockPresent(YanginKayitlari.AFET_ATESI.get(), yan);
        BlockState ates = y.getBlockState(yan);
        y.assertTrue(ates.getValue(AfetAtesiBlogu.GUNEY) && !ates.getValue(AfetAtesiBlogu.KUZEY)
                && !ates.getValue(AfetAtesiBlogu.UST), "yan alev yaprağa (güneye) dönük olmalı: " + ates);
        y.setBlock(yaprak, Blocks.AIR);
        y.assertBlockPresent(Blocks.AIR, yan);
        y.succeedWhen(() -> y.assertTrue(aktif(y) == 0, "dayanağı kalkan hücre sönmeli"));
    }

    /** Y-22: afet ateşi vanilla {@code minecraft:fire} etiketinde (su iksiri söndürür, yol bulucu kaçınır). */
    @GameTest(template = SAHNE, batch = "yangin_genel")
    public static void afetAtesiAtesEtiketinde(GameTestHelper y) {
        y.assertTrue(YanginKayitlari.AFET_ATESI.get().defaultBlockState().is(BlockTags.FIRE),
                "dogalafetler:afet_atesi minecraft:fire etiketinde olmalı");
        y.succeed();
    }

    private static BlockState kaliciYaprak() {
        return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    private static void dosemeKur(GameTestHelper y, int katman, BlockState durum) {
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                y.setBlock(new BlockPos(x, katman, z), durum);
            }
        }
    }

    private static AABB alan(GameTestHelper y) {
        return new AABB(y.absolutePos(BlockPos.ZERO), y.absolutePos(new BlockPos(8, 7, 8))).inflate(1.0);
    }

    /** Batch'inde tek başına duran testlerin temizliği: sıçramayla sahne dışına düşen hücreler de söner. */
    private static AABB genisAlan(GameTestHelper y) {
        return alan(y).inflate(14.0);
    }

    private static int aktif(GameTestHelper y) {
        return YanginYoneticisi.aktifSayi(y.getLevel(), alan(y));
    }

    /** Sahnedeki yanan hücrelerin göreli konumları (hata iletisi için). */
    private static String hucreKonumlari(GameTestHelper y) {
        AABB bolge = alan(y);
        StringBuilder sb = new StringBuilder("[");
        for (long konum : YanginYoneticisi.veri(y.getLevel()).hucreler.keySet()) {
            BlockPos p = BlockPos.of(konum);
            if (bolge.contains(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)) {
                sb.append(y.relativePos(p).toShortString()).append("; ");
            }
        }
        return sb.append(']').toString();
    }

    private static int atesSayisi(GameTestHelper y) {
        int sayi = 0;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(8, 7, 8))) {
            if (y.getBlockState(p).is(YanginKayitlari.AFET_ATESI.get())) {
                sayi++;
            }
        }
        return sayi;
    }

    /** Sahnedeki bütün blok durumlarının sıralı hash'i. */
    private static int ozet(GameTestHelper y) {
        int hash = 1;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(8, 7, 8))) {
            hash = 31 * hash + Block.getId(y.getBlockState(p));
        }
        return hash;
    }

    /** Sahnenin {@code x0..x1} dilimindeki blok durumları, sabit sırayla. */
    private static BlockState[] durumlar(GameTestHelper y, int x0, int x1) {
        BlockState[] sonuc = new BlockState[(x1 - x0 + 1) * 8 * 9];
        int i = 0;
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(x0, 0, 0), new BlockPos(x1, 7, 8))) {
            sonuc[i++] = y.getBlockState(p);
        }
        return sonuc;
    }

    private static int farkSayisi(BlockState[] a, BlockState[] b) {
        int sayi = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                sayi++;
            }
        }
        return sayi;
    }
}
