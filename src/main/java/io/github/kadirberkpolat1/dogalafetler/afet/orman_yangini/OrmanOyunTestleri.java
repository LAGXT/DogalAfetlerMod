package io.github.kadirberkpolat1.dogalafetler.afet.orman_yangini;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimKimlikleri;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiTemizligi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginKayitlari;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Orman yangını afetinin oyun içi testleri (şartname §6 #3, #16; bölüm G). Yangının kendisi ortak yangının testlerindedir
 * ({@code YanginOyunTestleri}); burada afetin başlangıç alanı, temiz stop, başarım ipucu, blok hasarı kuralı ve yanık izi
 * sınanır. Tek afet yuvasını kullanan testler kendi batch'indedir; başarısız biten test de batch sonunda temizlenir
 * ({@code OyunTestiTemizligi.batchSonu}, GP18).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class OrmanOyunTestleri {
    private static final String SAHNE = "yangin_sahne";

    private OrmanOyunTestleri() {
    }

    @AfterBatch(batch = "orman_baslar")
    public static void baslarSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "orman_ipucu")
    public static void ipucuSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "orman_basarim")
    public static void basarimSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    @AfterBatch(batch = "orman_blok_hasari")
    public static void blokHasariSonrasi(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
    }

    /**
     * {@code /dogalafet start orman_yangini 3} AKTIF'e geçerken başlangıç alanını afetin kaynağıyla tutuşturur; yanmaz taş
     * tutuşmaz; {@code stop} bütün ateşi hemen söndürür (ateş bloğu, hücre ve afet kalmaz).
     */
    @GameTest(template = SAHNE, batch = "orman_baslar", timeoutTicks = 120)
    public static void aktifteBaslangicAlaniTutusurStopSondurur(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        orman(y);
        y.setBlock(new BlockPos(0, 1, 0), Blocks.STONE);
        y.setBlock(new BlockPos(8, 1, 8), Blocks.STONE);
        BlockPos merkez = y.absolutePos(new BlockPos(4, 3, 4));
        OrmanYangini afet = baslat(y, 3, merkez);
        y.assertTrue(afet.baglam().merkez().equals(merkez), "merkez komutun konumu olmalı: " + afet.baglam().merkez());
        y.runAfterDelay(40, () -> {
            int aktif = YanginYoneticisi.aktifSayi(level, alan(y));
            int ates = atesSayisi(y);
            yonetici.durdur();
            int sonra = YanginYoneticisi.aktifSayi(level, alan(y));
            y.assertTrue(aktif > 0 && ates > 0, "başlangıç alanı tutuşmalı: hücre " + aktif + ", ateş bloğu " + ates);
            y.assertTrue(sonra == 0 && atesSayisi(y) == 0, "stop ateşi hemen söndürmeli: hücre " + sonra);
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "afet yuvası boş olmalı");
            y.assertBlockPresent(Blocks.STONE, new BlockPos(0, 1, 0));
            y.assertBlockPresent(Blocks.STONE, new BlockPos(8, 1, 8));
            y.succeed();
        });
    }

    /** Başarım §A.3 #5: yanan hücreye 8 blok yakın oyuncuya her saniye {@code YANGIN_ALANINDA}; uzaktakine gitmez. */
    @GameTest(template = SAHNE, batch = "orman_ipucu", timeoutTicks = 120)
    public static void yanginAlanindakiOyuncuyaHerSaniyeIpucu(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        orman(y);
        baslat(y, 2, y.absolutePos(new BlockPos(4, 3, 4)));
        BlockPos yakinP = y.absolutePos(new BlockPos(4, 5, 4));
        BlockPos uzakP = y.absolutePos(new BlockPos(4, 5, 60));
        ServerPlayer yakin = OyunTestiTemizligi.oyuncu(y, yakinP.getX() + 0.5, yakinP.getY(), yakinP.getZ() + 0.5);
        ServerPlayer uzak = OyunTestiTemizligi.oyuncu(y, uzakP.getX() + 0.5, uzakP.getY(), uzakP.getZ() + 0.5);
        AtomicInteger yakinIpucu = new AtomicInteger();
        AtomicInteger uzakIpucu = new AtomicInteger();
        Consumer<BasarimIpucuEvent> dinleyici = olay -> {
            if (olay.ipucu() == Ipucu.YANGIN_ALANINDA) {
                (olay.oyuncu() == yakin ? yakinIpucu : olay.oyuncu() == uzak ? uzakIpucu : new AtomicInteger()).incrementAndGet();
            }
        };
        OyunTestiTemizligi.dinle(BasarimIpucuEvent.class, dinleyici);
        y.runAfterDelay(61, () -> {
            yonetici.durdur();
            level.getServer().getPlayerList().remove(yakin);
            level.getServer().getPlayerList().remove(uzak);
            y.assertTrue(yakinIpucu.get() >= 2 && yakinIpucu.get() <= 4, "61 tick'te saniyede bir ipucu: " + yakinIpucu.get());
            y.assertTrue(uzakIpucu.get() == 0, "yangına uzak oyuncuya ipucu gitmemeli: " + uzakIpucu.get());
            y.succeed();
        });
    }

    /**
     * Gerçek orman yangını → "Yanmadan Çıktım" uçtan uca (AOi-4, başarım §A.3 #5): yanan hücreye 8 blok yakın oyuncu 30 olay
     * saniyesi biriktirip doğal bitişten sağ çıkınca başarımı alır; yangına 12–16 blok uzaktaki oyuncuya ipucu gitmez, almaz.
     * Yangın {@code afetBlokHasari=false} ile veri olarak sürer; yakıt meşe kütüğü (hücre 444 tick yanar, yaprak 180 tick'te
 * 30 sn dolmadan söner); sahte oyuncular yaratıcı.
     */
    @GameTest(template = SAHNE, batch = "orman_basarim", timeoutTicks = 900)
    public static void dogalBitenOrmanYanginiYanmadanCiktimVerir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        OyunTestiTemizligi.blokHasari(level, false);
        orman(y);
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(7, 3, 7))) {
            y.setBlock(p, Blocks.OAK_LOG);
        }
        BlockPos yakinP = y.absolutePos(new BlockPos(4, 5, 4));
        BlockPos uzakP = y.absolutePos(new BlockPos(4, 1, 21));
        ServerPlayer yakin = OyunTestiTemizligi.oyuncu(y, yakinP.getX() + 0.5, yakinP.getY(), yakinP.getZ() + 0.5);
        ServerPlayer uzak = OyunTestiTemizligi.oyuncu(y, uzakP.getX() + 0.5, uzakP.getY(), uzakP.getZ() + 0.5);
        yakin.setGameMode(GameType.CREATIVE);
        uzak.setGameMode(GameType.CREATIVE);
        AtomicInteger yakinIpucu = new AtomicInteger();
        AtomicInteger uzakIpucu = new AtomicInteger();
        OyunTestiTemizligi.dinle(BasarimIpucuEvent.class, olay -> {
            if (olay.ipucu() == Ipucu.YANGIN_ALANINDA) {
                (olay.oyuncu() == yakin ? yakinIpucu : olay.oyuncu() == uzak ? uzakIpucu : new AtomicInteger()).incrementAndGet();
            }
        });
        OrmanYangini afet = baslat(y, 2, y.absolutePos(new BlockPos(4, 3, 4)));
        y.runAfterDelay(700, () -> {
            for (int i = 0; i < 6 && yonetici.aktifAfet().isPresent(); i++) {
                yonetici.asamaIleri();
            }
            Advancement yanmadan = level.getServer().getAdvancements()
                    .getAdvancement(BasarimKimlikleri.basarim(Basarimlar.YANMADAN_CIKTIM));
            y.assertTrue(yanmadan != null, "advancement yüklü değil (runData?)");
            boolean yakinda = yakin.getAdvancements().getOrStartProgress(yanmadan).isDone();
            boolean uzakta = uzak.getAdvancements().getOrStartProgress(yanmadan).isDone();
            y.assertTrue(yakinIpucu.get() >= 30, "yakındaki oyuncu 30 olay saniyesi biriktirmedi: " + yakinIpucu.get());
            y.assertTrue(uzakIpucu.get() == 0, "uzaktaki oyuncuya ipucu gitti: " + uzakIpucu.get());
            y.assertTrue(yonetici.aktifAfet().isEmpty() && afet.sonlandi(), "orman yangını doğal bitmeli: " + afet.asama());
            y.assertTrue(yakinda, "yangın alanında 30 sn kalıp sağ çıkan Yanmadan Çıktım almalı");
            y.assertTrue(!uzakta, "yangına uzak oyuncu Yanmadan Çıktım almamalı");
            DogalAfetler.LOGGER.info("Orman GameTest: Yanmadan Çıktım yakın ipucu={} uzak ipucu={}", yakinIpucu.get(), uzakIpucu.get());
            y.succeed();
        });
    }

    /** Madde 11 {@code afetBlokHasari=false}: yangın veri, ısı ve hasar olarak sürer; ateş bloğu ve yanık izi yok, sahne aynı. */
    @GameTest(template = SAHNE, batch = "orman_blok_hasari", timeoutTicks = 160)
    public static void blokHasariKapaliykenSahneDegismez(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        boolean onceki = OyunTestiTemizligi.blokHasari(level, false);
        orman(y);
        int ilk = ozet(y);
        baslat(y, 3, y.absolutePos(new BlockPos(4, 3, 4)));
        y.runAfterDelay(100, () -> {
            int aktif = YanginYoneticisi.aktifSayi(level, alan(y));
            int son = ozet(y);
            yonetici.durdur();
            OyunTestiTemizligi.blokHasari(level, onceki);
            y.assertTrue(aktif > 0, "yangın veri olarak sürmeli: " + aktif);
            y.assertTrue(son == ilk, "blok hasarı kapalıyken sahne değişmemeli");
            y.succeed();
        });
    }

    /** Yanık izi: zemin üstünde afet ateşi yanan çimen kaba toprağa döner; ateşsiz sütun ve yanmaz zemin değişmez. */
    @GameTest(template = SAHNE, batch = "orman_yanik_izi")
    public static void atesAltindakiCimenKabaTopragaDoner(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                y.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        BlockPos yanan = new BlockPos(4, 1, 4);
        BlockPos sonuk = new BlockPos(2, 1, 2);
        BlockPos tas = new BlockPos(6, 1, 6);
        y.setBlock(yanan, Blocks.GRASS_BLOCK);
        y.setBlock(sonuk, Blocks.GRASS_BLOCK);
        y.setBlock(tas, Blocks.STONE);
        y.setBlock(yanan.above(), YanginKayitlari.AFET_ATESI.get());
        y.setBlock(tas.above(), YanginKayitlari.AFET_ATESI.get());
        y.assertTrue(OrmanYangini.yanikIziSutunu(level, y.absolutePos(yanan).getX(), y.absolutePos(yanan).getZ()),
                "ateş altındaki çimen dönüşmeli");
        y.assertTrue(!OrmanYangini.yanikIziSutunu(level, y.absolutePos(sonuk).getX(), y.absolutePos(sonuk).getZ()),
                "ateşsiz çimen dönüşmemeli");
        y.assertTrue(!OrmanYangini.yanikIziSutunu(level, y.absolutePos(tas).getX(), y.absolutePos(tas).getZ()),
                "taş zemin dönüşmemeli");
        y.assertBlockPresent(Blocks.COARSE_DIRT, yanan);
        y.assertBlockPresent(Blocks.GRASS_BLOCK, sonuk);
        y.assertBlockPresent(Blocks.STONE, tas);
        y.setBlock(yanan.above(), Blocks.AIR);
        y.setBlock(tas.above(), Blocks.AIR);
        y.succeed();
    }

    private static OrmanYangini baslat(GameTestHelper y, int seviye, BlockPos merkez) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.ORMAN_YANGINI, seviye, null, level, merkez);
        y.assertTrue(sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "orman yangını başlamadı: " + sonuc);
        OrmanYangini afet = (OrmanYangini) yonetici.aktifAfet().orElseThrow();
        yonetici.asamaIleri();
        yonetici.asamaIleri();
        y.assertTrue(afet.asama() == AfetAsamasi.AKTIF, "AKTIF'e geçmeli: " + afet.asama());
        return afet;
    }

    /** Taş zemin üstünde 7×3×7 kalıcı yaprak kümesi. */
    private static void orman(GameTestHelper y) {
        BlockState yaprak = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                y.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(7, 3, 7))) {
            y.setBlock(p, yaprak);
        }
    }

    private static AABB alan(GameTestHelper y) {
        return new AABB(y.absolutePos(new BlockPos(-1, -1, -1)), y.absolutePos(new BlockPos(10, 9, 10)));
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

    private static int ozet(GameTestHelper y) {
        int hash = 1;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(8, 7, 8))) {
            hash = 31 * hash + Block.getId(y.getBlockState(p));
        }
        return hash;
    }
}
