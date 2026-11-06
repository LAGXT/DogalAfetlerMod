package io.github.kadirberkpolat1.dogalafetler.yangin;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.yangin.mantik.YanginMantigi;
import java.util.function.DoubleSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
