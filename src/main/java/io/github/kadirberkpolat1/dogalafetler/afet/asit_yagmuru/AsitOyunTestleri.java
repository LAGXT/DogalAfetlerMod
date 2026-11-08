package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimIpucuEvent;
import io.github.kadirberkpolat1.dogalafetler.basarim.BasarimKimlikleri;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Basarimlar;
import io.github.kadirberkpolat1.dogalafetler.basarim.mantik.Ipucu;
import io.github.kadirberkpolat1.dogalafetler.core.AfetAsamasi;
import io.github.kadirberkpolat1.dogalafetler.core.AfetTuru;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.EtkiAlani;
import io.github.kadirberkpolat1.dogalafetler.core.OyunTestiTemizligi;
import io.github.kadirberkpolat1.dogalafetler.esya.EsyaKayitlari;
import io.github.kadirberkpolat1.dogalafetler.esya.mantik.RadarGorunumu;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarPaketi;
import io.github.kadirberkpolat1.dogalafetler.esya.radar.RadarSunucu;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginEtiketleri;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.advancements.Advancement;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Asit yağmurunun oyun içi testleri (şartname §6 #3, #16; bölüm E). Tek afet yuvasını kullanan testler kendi batch'indedir
 * ve başta/sonda afeti durdurur; gamerule değiştiren test eski değeri geri koyar (K-E15). Başarısız biten test de batch sonunda
 * temizlenir ({@link OyunTestiTemizligi#batchSonu}: afet, kural, dinleyici, sahte oyuncu; GP18).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AsitOyunTestleri {
    private static final String SAHNE = "yangin_sahne";
    /** Uzak oyuncu testinde yüzeyi karşılaştırılan kare: açık oyuncunun çevresinde ±{@value} blok (çatılı oyuncu içeride). */
    private static final int UZAK_ALAN = 30;
    /** Uzak oyuncu testinin zorla yüklediği chunk'lar; batch sonunda bırakılır. */
    private static final List<ChunkPos> ZORLA_YUKLU = new ArrayList<>();

    private AsitOyunTestleri() {
    }

    @AfterBatch(batch = "asit_hasar")
    public static void hasarSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "asit_blok_hasari")
    public static void blokHasariSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "asit_stop")
    public static void stopSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "asit_basarim")
    public static void basarimSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "asit_hiz")
    public static void hizSonrasi(ServerLevel level) {
        AfetYoneticisi.al(level.getServer()).hizAyarla(1);
        batchSonu(level);
    }

    @AfterBatch(batch = "asit_saniye")
    public static void saniyeSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "asit_hava")
    public static void havaSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    @AfterBatch(batch = "asit_uzak")
    public static void uzakSonrasi(ServerLevel level) {
        batchSonu(level);
    }

    /**
     * Batch sonu: afet durur (önceki hava geri yüklenir), kural, dinleyici ve sahte oyuncular döner, zorla yüklenen uzak
     * chunk'lar bırakılır. Hava yağmıyorsa yağmur düzeyi de sıfırlanır: AF2-02 vanilla yağmuru 0,01/tick ile söner, sıradaki
     * batch'in ateşi sönmesin.
     */
    private static void batchSonu(ServerLevel level) {
        OyunTestiTemizligi.batchSonu(level);
        for (ChunkPos c : ZORLA_YUKLU) {
            level.setChunkForced(c.x, c.z, false);
        }
        ZORLA_YUKLU.clear();
        if (!level.getLevelData().isRaining()) {
            level.setRainLevel(0f);
        }
    }

    /**
     * AF2-01/02: asit AKTIF olunca vanilla yağmuru bütün dünyada açılır ({@code level.isRaining()}) ve AKTIF boyunca tutulur
     * ({@code /weather clear} bir tick'te geri alınır); bitince önceki hava (açık hava sayacı dahil) aynen geri gelir ve yağmur
     * söner. Etki alanı küreseldir; Hava Radarı asidi gökyüzü anomalisi değil "her yer" olarak gösterir (AF2-06).
     */
    @GameTest(template = SAHNE, batch = "asit_hava", timeoutTicks = 200)
    public static void asitVanillaYagmurunuAcarBitinceHavayiGeriYukler(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        ServerLevelData veri = (ServerLevelData) level.getLevelData();
        level.setWeatherParameters(12345, 0, false, false);
        level.setRainLevel(0f);
        BlockPos radarP = y.absolutePos(new BlockPos(2, 1, 2));
        ServerPlayer radarci = OyunTestiTemizligi.oyuncu(y, radarP.getX() + 0.5, radarP.getY(), radarP.getZ() + 0.5);
        radarci.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(EsyaKayitlari.HAVA_RADARI.get()));
        AsitYagmuru afet = baslat(y, 3, y.absolutePos(new BlockPos(4, 1, 4)));
        y.assertTrue(veri.isRaining() && veri.getClearWeatherTime() == 0, "AKTIF'e geçince vanilla yağmuru açılmalı");
        y.assertTrue(yonetici.etkiAlani(afet).map(EtkiAlani::global).orElse(false), "asit etki alanı küresel olmalı");
        RadarPaketi radar = RadarSunucu.durum(radarci);
        y.assertTrue(radar.gorunum() == RadarGorunumu.AFET && radar.herYer() && radar.tur() == AfetTuru.ASIT_YAGMURU,
                "radar asidi 'her yer' göstermeli: " + radar);
        y.runAfterDelay(40, () -> {
            y.assertTrue(level.isRaining(), "40 tick'te level.isRaining() true olmalı (düzey " + level.getRainLevel(1f) + ")");
            level.setWeatherParameters(6000, 0, false, false);
        });
        y.runAfterDelay(43, () -> {
            boolean tutuldu = veri.isRaining() && veri.getClearWeatherTime() == 0;
            yonetici.durdur();
            cikar(level, List.of(radarci));
            y.assertTrue(tutuldu, "asit sürerken /weather clear geri alınmalı");
            y.assertTrue(!veri.isRaining() && veri.getClearWeatherTime() == 12345, "bitişte önceki hava geri gelmeli: yağmur="
                    + veri.isRaining() + " açık=" + veri.getClearWeatherTime());
        });
        y.runAfterDelay(120, () -> {
            y.assertTrue(!level.isRaining(), "bitişten sonra yağmur sönmeli (düzey " + level.getRainLevel(1f) + ")");
            y.succeed();
        });
    }

    /**
     * AF2-03/05: hedeften 500+ blok uzaktaki göğe açık oyuncu da hasar alır, aynı yerde cam çatı altındaki almaz; uzak
     * oyuncuların çevresindeki yüzey de aşınır (örnekleme her oyuncunun çevresinden). Uzak alanın 5×5 chunk'ı eşzamanlı
     * üretilir ve test boyunca zorla yüklü tutulur (K-E21).
     */
    @GameTest(template = SAHNE, batch = "asit_uzak", timeoutTicks = 260)
    public static void uzaktakiAcikOyuncuHasarAlirCatiAltindakiAlmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        zemin(y);
        BlockPos hedefP = y.absolutePos(new BlockPos(2, 1, 2));
        ServerPlayer hedef = OyunTestiTemizligi.oyuncu(y, hedefP.getX() + 0.5, hedefP.getY(), hedefP.getZ() + 0.5);
        int ux = ((hedefP.getX() + 512) >> 4 << 4) + 8;
        int uz = (hedefP.getZ() >> 4 << 4) + 8;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                ChunkPos c = new ChunkPos((ux >> 4) + dx, (uz >> 4) + dz);
                level.getChunk(c.x, c.z);
                level.setChunkForced(c.x, c.z, true);
                ZORLA_YUKLU.add(c);
            }
        }
        int zeminY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, ux, uz);
        BlockPos acikP = new BlockPos(ux, zeminY, uz);
        BlockPos catiP = new BlockPos(ux + 6, zeminY, uz);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                level.setBlock(catiP.offset(x, 3, z), Blocks.GLASS.defaultBlockState(), 3);
            }
        }
        ServerPlayer acik = OyunTestiTemizligi.oyuncu(y, acikP.getX() + 0.5, acikP.getY(), acikP.getZ() + 0.5);
        ServerPlayer cati = OyunTestiTemizligi.oyuncu(y, catiP.getX() + 0.5, catiP.getY(), catiP.getZ() + 0.5);
        List<ServerPlayer> hepsi = List.of(hedef, acik, cati);
        for (ServerPlayer o : hepsi) {
            o.setGameMode(GameType.SURVIVAL);
            o.setHealth(20f);
        }
        BlockState[] ilkYuzey = yuzey(level, acikP, UZAK_ALAN);
        AsitYagmuru afet = baslat(y, 5, hedefP, hedef);
        y.onEachTick(() -> hepsi.forEach(AsitOyunTestleri::hasarAlabilir));
        y.runAfterDelay(170, () -> {
            float hedefCan = hedef.getHealth();
            float acikCan = acik.getHealth();
            float catiCan = cati.getHealth();
            int degisen = degisenYuzey(ilkYuzey, yuzey(level, acikP, UZAK_ALAN));
            double uzaklik = Math.hypot(acikP.getX() - hedefP.getX(), acikP.getZ() - hedefP.getZ());
            boolean aktif = afet.asama() == AfetAsamasi.AKTIF;
            yonetici.durdur();
            cikar(level, hepsi);
            y.assertTrue(aktif, "afet 170 tick boyunca AKTIF kalmalı");
            y.assertTrue(uzaklik >= 500, "uzak oyuncu hedeften en az 500 blok uzakta olmalı: " + uzaklik);
            y.assertTrue(acikCan <= 16f, "hedeften " + Math.round(uzaklik) + " blok uzaktaki açık oyuncu hasar almalı: can " + acikCan);
            y.assertTrue(catiCan == 20f, "uzaktaki çatı altındaki oyuncu hasar almamalı: can " + catiCan);
            y.assertTrue(hedefCan <= 16f, "hedef oyuncu da hasar almalı: can " + hedefCan);
            y.assertTrue(degisen > 0, "uzak oyuncuların çevresinde yüzey aşınmalı (değişen " + degisen + ")");
            y.succeed();
        });
    }

    /**
     * AOi-6 (K-C11): maruziyet ölçekli afet saatiyle birikir. Hasar aralığı 20 ölçekli tick (1 sn); {@code hiz 10} altında
     * açıktaki oyuncu her denetimde (4 gerçek tick = 40 ölçekli) bir hasar alır, birikim patlamaz. Gerçek tick'le biriken
     * maruziyet 40 tick'te en çok iki hasar verirdi.
     */
    @GameTest(template = SAHNE, batch = "asit_hiz", timeoutTicks = 120)
    public static void maruziyetOlcekliSaatleBirikir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        zemin(y);
        BlockPos acikP = y.absolutePos(new BlockPos(2, 1, 2));
        ServerPlayer acik = OyunTestiTemizligi.oyuncu(y, acikP.getX() + 0.5, acikP.getY(), acikP.getZ() + 0.5);
        acik.setGameMode(GameType.SURVIVAL);
        acik.setHealth(20f);
        baslat(y, 3, acikP);
        yonetici.hizAyarla(10);
        y.onEachTick(() -> hasarAlabilir(acik));
        y.runAfterDelay(40, () -> {
            float can = acik.getHealth();
            yonetici.hizAyarla(1);
            yonetici.durdur();
            cikar(level, List.of(acik));
            y.assertTrue(can <= 12f, "hiz 10 altında 40 tick'te en az 8 hasar (1 can) beklenirdi: can " + can);
            y.succeed();
        });
    }

    /** Dönüşüm tablosu (tasarım §2): yumuşak bloklar aşınır, {@code asit_dayanikli} (taş, cam) hiç değişmez. */
    @GameTest(template = SAHNE, batch = "asit_asindirma")
    public static void asitDayanikliBlokBozulmazYumusakBlokAsinir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        zemin(y);
        y.assertTrue(Blocks.STONE.defaultBlockState().is(YanginEtiketleri.ASIT_DAYANIKLI), "taş asit_dayanikli etiketinde");
        y.assertTrue(Blocks.GLASS.defaultBlockState().is(YanginEtiketleri.ASIT_DAYANIKLI), "cam asit_dayanikli etiketinde");
        Object[][] tablo = {
            {Blocks.STONE, Blocks.STONE}, {Blocks.GLASS, Blocks.GLASS}, {Blocks.OBSIDIAN, Blocks.OBSIDIAN},
            {Blocks.WHITE_CONCRETE, Blocks.WHITE_CONCRETE}, {Blocks.OAK_PLANKS, Blocks.AIR}, {Blocks.WHITE_WOOL, Blocks.AIR},
            {Blocks.GRASS_BLOCK, Blocks.DIRT}, {Blocks.DIRT, Blocks.COARSE_DIRT},
            {Blocks.COBBLESTONE, Blocks.MOSSY_COBBLESTONE}, {Blocks.STONE_BRICKS, Blocks.CRACKED_STONE_BRICKS},
            {Blocks.CALCITE, Blocks.GRAVEL}, {Blocks.COPPER_BLOCK, Blocks.EXPOSED_COPPER}, {Blocks.OAK_LOG, Blocks.OAK_LOG}};
        for (int i = 0; i < tablo.length; i++) {
            BlockPos p = new BlockPos(i % 7 + 1, 1, i / 7 * 2 + 1);
            y.setBlock(p, (Block) tablo[i][0]);
            AsitAsindirici.uygula(level, y.absolutePos(p), true);
            y.assertBlockPresent((Block) tablo[i][1], p);
        }
        BlockPos yaprak = new BlockPos(1, 1, 6);
        y.setBlock(yaprak, kaliciYaprak());
        y.assertTrue(AsitAsindirici.uygula(level, y.absolutePos(yaprak), true), "yaprak aşınmalı");
        y.assertBlockPresent(Blocks.AIR, yaprak);

        BlockPos kiriktas = new BlockPos(3, 1, 6);
        y.setBlock(kiriktas, Blocks.COBBLESTONE);
        y.assertTrue(!AsitAsindirici.uygula(level, y.absolutePos(kiriktas), false), "S1-S2'de kırıktaş aşınmaz");
        y.assertBlockPresent(Blocks.COBBLESTONE, kiriktas);

        BlockPos cimen = new BlockPos(6, 1, 6);
        y.setBlock(cimen, Blocks.GRASS_BLOCK);
        y.setBlock(cimen.above(), Blocks.GRASS);
        BlockPos aday = AsitAsindirici.aday(level, y.absolutePos(cimen).getX(), y.absolutePos(cimen).getZ(), true);
        y.assertTrue(y.absolutePos(cimen.above()).equals(aday), "sütunun adayı önce üstteki bitki: " + aday);
        y.succeed();
    }

    /**
     * Bölüm E.3: gökyüzüne açık oyuncu hasar alır ama canı {@code asitMinimumCan} (4) altına inmez (S5'te bile), cam çatı
     * altındaki oyuncu hiç hasar almaz; hasar anında başarım ipucu ({@code HASAR_ALDI}, §A.3 #6) yalnız hasar alana gider.
     */
    @GameTest(template = SAHNE, batch = "asit_hasar", timeoutTicks = 260)
    public static void acikOyuncuOlmedenHasarAlirCatiAltindakiAlmaz(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        zemin(y);
        for (int x = 5; x <= 7; x++) {
            for (int z = 5; z <= 7; z++) {
                y.setBlock(new BlockPos(x, 4, z), Blocks.GLASS);
            }
        }
        BlockPos acikP = y.absolutePos(new BlockPos(2, 1, 2));
        BlockPos catiP = y.absolutePos(new BlockPos(6, 1, 6));
        ServerPlayer acik = OyunTestiTemizligi.oyuncu(y, acikP.getX() + 0.5, acikP.getY(), acikP.getZ() + 0.5);
        ServerPlayer cati = OyunTestiTemizligi.oyuncu(y, catiP.getX() + 0.5, catiP.getY(), catiP.getZ() + 0.5);
        acik.setGameMode(GameType.SURVIVAL);
        cati.setGameMode(GameType.SURVIVAL);
        acik.setHealth(5f);
        cati.setHealth(20f);
        AtomicInteger acikIpucu = new AtomicInteger();
        AtomicInteger catiIpucu = new AtomicInteger();
        Consumer<BasarimIpucuEvent> dinleyici = olay -> {
            if (olay.ipucu() == Ipucu.HASAR_ALDI) {
                (olay.oyuncu() == acik ? acikIpucu : olay.oyuncu() == cati ? catiIpucu : new AtomicInteger()).incrementAndGet();
            }
        };
        OyunTestiTemizligi.dinle(BasarimIpucuEvent.class, dinleyici);
        AsitYagmuru afet = baslat(y, 5, acikP);
        y.onEachTick(() -> {
            hasarAlabilir(acik);
            hasarAlabilir(cati);
        });
        y.runAfterDelay(200, () -> {
            float acikCan = acik.getHealth();
            float catiCan = cati.getHealth();
            boolean asamaAktif = afet.asama() == AfetAsamasi.AKTIF;
            yonetici.durdur();
            cikar(level, List.of(acik, cati));
            y.assertTrue(asamaAktif, "afet 200 tick boyunca AKTIF kalmalı");
            y.assertTrue(acikCan == 4f, "açıktaki oyuncunun canı 4'te durmalı (öldürmez): " + acikCan);
            y.assertTrue(catiCan == 20f, "çatı altındaki oyuncu hasar almamalı: " + catiCan);
            y.assertTrue(acikIpucu.get() == 1, "5 → 4 tek hasar, tek HASAR_ALDI ipucu: " + acikIpucu.get());
            y.assertTrue(catiIpucu.get() == 0, "çatı altına ipucu gitmemeli: " + catiIpucu.get());
            y.succeed();
        });
    }

    /**
     * Kullanıcı isteği (18): asit yağmuru altında gökyüzüne açık oyuncu seviyeden bağımsız saniyede 1 can (yarım kalp)
     * kaybeder; cam çatı altındaki 0. S1'de (eski tabloda 4 sn'de yarım can) 5 sn = 5 can.
     */
    @GameTest(template = SAHNE, batch = "asit_saniye", timeoutTicks = 160)
    public static void acikOyuncuSaniyedeYarimKalpCatiAltiSifir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        zemin(y);
        for (int x = 5; x <= 7; x++) {
            for (int z = 5; z <= 7; z++) {
                y.setBlock(new BlockPos(x, 4, z), Blocks.GLASS);
            }
        }
        BlockPos acikP = y.absolutePos(new BlockPos(2, 1, 2));
        BlockPos catiP = y.absolutePos(new BlockPos(6, 1, 6));
        ServerPlayer acik = OyunTestiTemizligi.oyuncu(y, acikP.getX() + 0.5, acikP.getY(), acikP.getZ() + 0.5);
        ServerPlayer cati = OyunTestiTemizligi.oyuncu(y, catiP.getX() + 0.5, catiP.getY(), catiP.getZ() + 0.5);
        acik.setGameMode(GameType.SURVIVAL);
        cati.setGameMode(GameType.SURVIVAL);
        acik.setHealth(20f);
        cati.setHealth(20f);
        baslat(y, 1, acikP);
        y.onEachTick(() -> {
            hasarAlabilir(acik);
            hasarAlabilir(cati);
        });
        y.runAfterDelay(100, () -> {
            float kayip = 20f - acik.getHealth();
            float catiCan = cati.getHealth();
            yonetici.durdur();
            cikar(level, List.of(acik, cati));
            y.assertTrue(kayip >= 4f && kayip <= 5f, "5 sn açıkta 1 can/sn: 4–5 can kaybı beklenirdi, kayıp " + kayip);
            y.assertTrue(catiCan == 20f, "çatı altındaki oyuncu hasar almamalı: " + catiCan);
            y.succeed();
        });
    }

    /**
     * Madde 11 {@code afetBlokHasari=false}: AKTIF asit yağmurunda hiçbir blok aşınma kuyruğuna girmez, sahne aynı kalır.
     * Sahnenin ortasında bir oyuncu durur: kural açık olsa örnekleme onun çevresindeki tahta ve yaprakları bulurdu (AF2-05).
     */
    @GameTest(template = SAHNE, batch = "asit_blok_hasari", timeoutTicks = 200)
    public static void blokHasariKapaliykenAsitBlokDegistirmez(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        boolean onceki = OyunTestiTemizligi.blokHasari(level, false);
        zemin(y);
        for (int x = 1; x <= 7; x++) {
            for (int z = 1; z <= 7; z++) {
                y.setBlock(new BlockPos(x, 1, z), (x + z) % 2 == 0 ? Blocks.OAK_PLANKS.defaultBlockState() : kaliciYaprak());
            }
        }
        int ilk = ozet(y);
        BlockPos ortaP = y.absolutePos(new BlockPos(4, 2, 4));
        ServerPlayer goren = OyunTestiTemizligi.oyuncu(y, ortaP.getX() + 0.5, ortaP.getY(), ortaP.getZ() + 0.5);
        AsitYagmuru afet = baslat(y, 5, y.absolutePos(new BlockPos(4, 1, 4)));
        BlockPos tahta = y.absolutePos(new BlockPos(2, 1, 2));
        boolean kuyruk = afet.kuyrugaAl(level, tahta);
        int bekleyen = afet.bekleyenAsinma();
        y.runAfterDelay(120, () -> {
            int son = ozet(y);
            int sonBekleyen = afet.bekleyenAsinma();
            yonetici.durdur();
            OyunTestiTemizligi.blokHasari(level, onceki);
            cikar(level, List.of(goren));
            y.assertTrue(bekleyen <= 1 && sonBekleyen == 0, "blok hasarı kapalıyken kuyruk boşalmalı: " + sonBekleyen
                    + " (elle eklenen " + kuyruk + ")");
            y.assertTrue(son == ilk, "blok hasarı kapalıyken sahne değişmemeli");
            y.succeed();
        });
    }

    /** {@code /dogalafet stop} temiz biter: çatlak kuyruğu boşalır, kuyruktaki bloklar dönüşmeden kalır, afet yuvası boş. */
    @GameTest(template = SAHNE, batch = "asit_stop", timeoutTicks = 120)
    public static void stopCatlakKuyrugunuTemizBirakir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        zemin(y);
        for (int x = 1; x <= 7; x++) {
            y.setBlock(new BlockPos(x, 1, 4), Blocks.OAK_PLANKS);
        }
        AsitYagmuru afet = baslat(y, 3, y.absolutePos(new BlockPos(4, 1, 4)));
        for (int x = 1; x <= 7; x++) {
            y.assertTrue(afet.kuyrugaAl(level, y.absolutePos(new BlockPos(x, 1, 4))), "tahta kuyruğa girmeli");
        }
        y.assertTrue(!afet.kuyrugaAl(level, y.absolutePos(new BlockPos(1, 1, 4))), "aynı blok iki kez sıraya girmez");
        y.assertTrue(!afet.kuyrugaAl(level, y.absolutePos(new BlockPos(1, 0, 1))), "taş (asit dayanıklı) sıraya girmez");
        y.runAfterDelay(20, () -> {
            int bekleyen = afet.bekleyenAsinma();
            yonetici.durdur();
            y.assertTrue(bekleyen >= 7, "20 tick'te çatlak süresi (3-5 sn) dolmamış olmalı: " + bekleyen);
            y.assertTrue(afet.bekleyenAsinma() == 0, "stop çatlak kuyruğunu boşaltmalı: " + afet.bekleyenAsinma());
            y.assertTrue(yonetici.aktifAfet().isEmpty(), "afet yuvası boş olmalı");
            for (int x = 1; x <= 7; x++) {
                y.assertBlockPresent(Blocks.OAK_PLANKS, new BlockPos(x, 1, 4));
            }
            y.succeed();
        });
    }

    /**
     * Gerçek asit yağmuru → başarım uçtan uca (inceleme AOi-4, başarım §A.3 #6): doğal biten S5 asitte cam çatı altındaki oyuncu
     * "Çatı Altı"nı alır; açıkta hasar alan ({@code HASAR_ALDI} ipucu) almaz.
     */
    @GameTest(template = SAHNE, batch = "asit_basarim", timeoutTicks = 260)
    public static void dogalBitenAsitteCatiAltiYalnizHasarsizaAcilir(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        yonetici.durdur();
        zemin(y);
        for (int x = 5; x <= 7; x++) {
            for (int z = 5; z <= 7; z++) {
                y.setBlock(new BlockPos(x, 4, z), Blocks.GLASS);
            }
        }
        BlockPos acikP = y.absolutePos(new BlockPos(2, 1, 2));
        BlockPos catiP = y.absolutePos(new BlockPos(6, 1, 6));
        ServerPlayer acik = OyunTestiTemizligi.oyuncu(y, acikP.getX() + 0.5, acikP.getY(), acikP.getZ() + 0.5);
        ServerPlayer cati = OyunTestiTemizligi.oyuncu(y, catiP.getX() + 0.5, catiP.getY(), catiP.getZ() + 0.5);
        acik.setGameMode(GameType.SURVIVAL);
        cati.setGameMode(GameType.SURVIVAL);
        acik.setHealth(5f);
        cati.setHealth(20f);
        AsitYagmuru afet = baslat(y, 5, acikP);
        y.onEachTick(() -> {
            hasarAlabilir(acik);
            hasarAlabilir(cati);
        });
        y.runAfterDelay(200, () -> {
            float acikCan = acik.getHealth();
            for (int i = 0; i < 6 && yonetici.aktifAfet().isPresent(); i++) {
                yonetici.asamaIleri();
            }
            Advancement catiAlti = level.getServer().getAdvancements()
                    .getAdvancement(BasarimKimlikleri.basarim(Basarimlar.CATI_ALTI));
            y.assertTrue(catiAlti != null, "advancement yüklü değil (runData?)");
            boolean catidaki = cati.getAdvancements().getOrStartProgress(catiAlti).isDone();
            boolean acikta = acik.getAdvancements().getOrStartProgress(catiAlti).isDone();
            y.assertTrue(acikCan < 5f, "açıktaki oyuncu asit hasarı almalı: " + acikCan);
            y.assertTrue(yonetici.aktifAfet().isEmpty() && afet.sonlandi(), "asit doğal bitmeli: " + afet.asama());
            y.assertTrue(catidaki, "çatı altındaki hasarsız oyuncu Çatı Altı almalı");
            y.assertTrue(!acikta, "asitten hasar alan Çatı Altı almamalı");
            yonetici.durdur();
            cikar(level, List.of(acik, cati));
            y.succeed();
        });
    }

    private static AsitYagmuru baslat(GameTestHelper y, int seviye, BlockPos merkez) {
        return baslat(y, seviye, merkez, null);
    }

    /** {@code hedef}: komut uyumluluğu için hedef oyuncu (asitte etkisi yok, AF2-01); {@code null} konuma başlatır. */
    private static AsitYagmuru baslat(GameTestHelper y, int seviye, BlockPos merkez, ServerPlayer hedef) {
        ServerLevel level = y.getLevel();
        AfetYoneticisi yonetici = AfetYoneticisi.al(level.getServer());
        AfetYoneticisi.BaslatmaSonucu sonuc = yonetici.elleBaslat(AfetTuru.ASIT_YAGMURU, seviye, hedef, level, merkez);
        y.assertTrue(sonuc == AfetYoneticisi.BaslatmaSonucu.BASLADI, "asit yağmuru başlamadı: " + sonuc);
        AsitYagmuru afet = (AsitYagmuru) yonetici.aktifAfet().orElseThrow();
        yonetici.asamaIleri();
        yonetici.asamaIleri();
        y.assertTrue(afet.asama() == AfetAsamasi.AKTIF, "AKTIF'e geçmeli: " + afet.asama());
        afet.yogunlukZorla(1.0);
        return afet;
    }

    private static void zemin(GameTestHelper y) {
        for (int x = 0; x <= 8; x++) {
            for (int z = 0; z <= 8; z++) {
                y.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    /** Sahte oyuncu tick almadığı için doğuş dokunulmazlığı ({@code f_8921_}) ve hasar aralığı elle sıfırlanır. */
    private static void hasarAlabilir(ServerPlayer o) {
        ObfuscationReflectionHelper.setPrivateValue(ServerPlayer.class, o, 0, "f_8921_");
        o.invulnerableTime = 0;
    }

    private static void cikar(ServerLevel level, List<ServerPlayer> oyuncular) {
        for (ServerPlayer o : oyuncular) {
            level.getServer().getPlayerList().remove(o);
        }
    }

    private static BlockState kaliciYaprak() {
        return Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
    }

    /** Merkezin ±{@code r} karesindeki sütunların gökyüzünü gören en üst bloğu ({@code MOTION_BLOCKING} altı). */
    private static BlockState[] yuzey(ServerLevel level, BlockPos merkez, int r) {
        int kenar = 2 * r + 1;
        BlockState[] sonuc = new BlockState[kenar * kenar];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int i = 0; i < kenar; i++) {
            for (int j = 0; j < kenar; j++) {
                int x = merkez.getX() - r + i;
                int z = merkez.getZ() - r + j;
                sonuc[i * kenar + j] = level.getBlockState(p.set(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1, z));
            }
        }
        return sonuc;
    }

    private static int degisenYuzey(BlockState[] once, BlockState[] sonra) {
        int n = 0;
        for (int i = 0; i < once.length; i++) {
            if (once[i] != sonra[i]) {
                n++;
            }
        }
        return n;
    }

    private static int ozet(GameTestHelper y) {
        int hash = 1;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(8, 7, 8))) {
            hash = 31 * hash + Block.getId(y.getBlockState(p));
        }
        return hash;
    }
}
