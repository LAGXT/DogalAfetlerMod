package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.core.GunesEvresi;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaIsleyici;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaEsikleri;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisKaynagi;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.meteor.MeteorAPI;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.SicaklikAyarlari;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.Katki;
import io.github.kadirberkpolat1.dogalafetler.sicaklik.mantik.OrtamSicakligi;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginYoneticisi;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.AfterBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Pulsar-2 dünya dilimi oyun testleri: kavurma bütçesi hiçbir tick'te aşılmaz ve TPS kısması uygulanır;
 * {@code afetBlokHasari} kapalıyken blok, ateş ve lav değişmez; dönüşüm tablosunun uç noktaları dünyada; kül katmanı
 * kar altında kalır ve çözülünce kül bloğu kalır; meteor yağmuru oyuncu başına dakikada
 * {@code pulsarMeteorDakikaBasina}; sıcaklığın PULSAR katkısı çarpan 2,0. Düz dünyada sahne testin içinde kurulur
 * ({@code donma_alan}, 9×6×9 hava). Kavurma kancaları ve gamerule değiştiren her test kendi batch'indedir; eski
 * değerleri batch sonu geri koyar. Config dosyasına yazılmaz (K-E28).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PulsarDunyaOyunTestleri {
    private static final String ALAN = "donma_alan";
    private static final int EN = 9;
    private static final int BOY = 6;
    private static final String BUTCE = "pulsar_dunya_butce";
    private static final String KAPI = "pulsar_dunya_kapi";
    private static final String METEOR = "pulsar_dunya_meteor";
    private static final String DUNYA = "pulsar_dunya";

    private static boolean kancalarKurulu;
    private static Function<ServerLevel, List<BlockPos>> eskiMerkez;
    private static int eskiYaricap;
    private static IntSupplier eskiButce;
    private static DoubleSupplier eskiKisma;
    private static BooleanSupplier eskiLav;
    private static Boolean eskiBlokHasari;

    private PulsarDunyaOyunTestleri() {
    }

    @AfterBatch(batch = BUTCE)
    public static void butceSonrasi(ServerLevel level) {
        geriKoy(level.getServer());
    }

    @AfterBatch(batch = KAPI)
    public static void kapiSonrasi(ServerLevel level) {
        geriKoy(level.getServer());
    }

    @AfterBatch(batch = METEOR)
    public static void meteorSonrasi(ServerLevel level) {
        geriKoy(level.getServer());
    }

    /** Kancaları ve gamerule'ü geri koyar; pulsar yağmurlarını uçanlarıyla birlikte durdurur, dönemi sıfırlar. */
    private static void geriKoy(MinecraftServer server) {
        if (kancalarKurulu) {
            KavurmaKuyrugu.merkezKaynagiAyarla(eskiMerkez);
            KavurmaKuyrugu.yaricapAyarla(eskiYaricap);
            KavurmaKuyrugu.butceKaynagiAyarla(eskiButce);
            KavurmaKuyrugu.kismaKaynagiAyarla(eskiKisma);
            KavurmaKuyrugu.lavKaynagiAyarla(eskiLav);
            kancalarKurulu = false;
            eskiMerkez = null;
        }
        DunyaDonemi donem = DunyaDonemi.al(server);
        PulsarEtkileri.bitir(server, donem, "oyun testi");
        MeteorAPI.yagmurlariDurdur(server, PulsarEtkileri.kaynak(donem), true);
        if (eskiBlokHasari != null) {
            server.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI).set(eskiBlokHasari, server);
            eskiBlokHasari = null;
        }
        AfetYoneticisi yonetici = AfetYoneticisi.al(server);
        yonetici.durdur();
        yonetici.sifirla();
    }

    private static void temizBasla(GameTestHelper y) {
        AfetYoneticisi yonetici = AfetYoneticisi.al(y.getLevel().getServer());
        yonetici.durdur();
        yonetici.sifirla();
    }

    /** Kavurma denemeleri sahnenin ortasından {@code yaricap} blokta; taban bütçe ve kısma sabit, lav gözesi kapalı. */
    private static void kancalar(GameTestHelper y, int yaricap, int butce, double kisma) {
        BlockPos merkez = y.absolutePos(p(EN / 2, 2, EN / 2));
        eskiMerkez = KavurmaKuyrugu.merkezKaynagiAyarla(l -> List.of(merkez));
        eskiYaricap = KavurmaKuyrugu.yaricapAyarla(yaricap);
        eskiButce = KavurmaKuyrugu.butceKaynagiAyarla(() -> butce);
        eskiKisma = KavurmaKuyrugu.kismaKaynagiAyarla(() -> kisma);
        eskiLav = KavurmaKuyrugu.lavKaynagiAyarla(() -> false);
        kancalarKurulu = true;
    }

    private static void blokHasari(GameTestHelper y, boolean acik) {
        MinecraftServer server = y.getLevel().getServer();
        GameRules.BooleanValue kural = server.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        eskiBlokHasari = kural.get();
        kural.set(acik, server);
    }

    private static BlockPos p(int x, int yy, int z) {
        return new BlockPos(x, yy + 1, z);
    }

    private static boolean is(GameTestHelper y, int x, int yy, int z, Block b) {
        return y.getBlockState(p(x, yy, z)).is(b);
    }

    private static String sutun(GameTestHelper y, int x, int z) {
        StringBuilder sb = new StringBuilder("sütun (" + x + "," + z + "):");
        for (int yy = 0; yy < BOY; yy++) {
            sb.append(' ').append(yy).append('=').append(y.getBlockState(p(x, yy, z)));
        }
        return sb.toString();
    }

    /** Sahnenin blok durumları; yaprağın {@code distance}'ı vanilla'nın zamanlanmış tick'iyle yeniden hesaplanır, sabitlenir. */
    private static BlockState[] anlik(GameTestHelper y) {
        BlockState[] d = new BlockState[EN * BOY * EN];
        int i = 0;
        for (int yy = 0; yy < BOY; yy++) {
            for (int z = 0; z < EN; z++) {
                for (int x = 0; x < EN; x++) {
                    BlockState s = y.getBlockState(p(x, yy, z));
                    d[i++] = s.hasProperty(LeavesBlock.DISTANCE) ? s.setValue(LeavesBlock.DISTANCE, LeavesBlock.DECAY_DISTANCE)
                            : s;
                }
            }
        }
        return d;
    }

    private static List<String> farklar(BlockState[] beklenen, BlockState[] gercek) {
        List<String> f = new ArrayList<>();
        for (int i = 0; i < beklenen.length; i++) {
            if (beklenen[i] != gercek[i]) {
                f.add("(" + i % EN + "," + i / (EN * EN) + "," + (i / EN) % EN + ") " + beklenen[i] + " ≠ "
                        + gercek[i]);
            }
        }
        return f;
    }

    private static AABB sahne(GameTestHelper y) {
        return new AABB(y.absolutePos(p(0, 0, 0)), y.absolutePos(p(EN - 1, BOY - 1, EN - 1))).inflate(1);
    }

    /**
     * Çok adaylı sahne: taş taban, toprak, çimen; ortada iki kütüklü ağaç ve kalıcı yaprak tacı, köşelerde çiçek,
     * bir kar katmanı, bir buz, bir kum sütunu.
     */
    private static void cehennemSahnesi(GameTestHelper y) {
        BlockState yaprak = Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true);
        for (int x = 0; x < EN; x++) {
            for (int z = 0; z < EN; z++) {
                y.setBlock(p(x, 0, z), Blocks.STONE);
                y.setBlock(p(x, 1, z), Blocks.DIRT);
                y.setBlock(p(x, 2, z), Blocks.GRASS_BLOCK);
                for (int yy = 3; yy < BOY; yy++) {
                    y.setBlock(p(x, yy, z), Blocks.AIR);
                }
            }
        }
        y.setBlock(p(4, 3, 4), Blocks.OAK_LOG);
        y.setBlock(p(4, 4, 4), Blocks.OAK_LOG);
        for (int x = 3; x <= 5; x++) {
            for (int z = 3; z <= 5; z++) {
                y.setBlock(p(x, 5, z), yaprak);
            }
        }
        y.setBlock(p(1, 3, 1), Blocks.POPPY);
        y.setBlock(p(7, 3, 7), Blocks.DANDELION);
        y.setBlock(p(1, 3, 7), Blocks.GRASS);
        y.setBlock(p(7, 3, 1), Blocks.FERN);
        y.setBlock(p(2, 3, 6), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2));
        y.setBlock(p(6, 2, 2), Blocks.ICE);
        y.setBlock(p(6, 2, 6), Blocks.SAND);
    }

    // ---------------------------------------------------------------- bütçe ve kapı

    /**
     * PL-3 f: bütçe 5 × kısma 0,75 → 4 yazma; her tick {@code sonIslem ≤ sonButce} ve en az bir tick'te bütçe dolar (19
     * deneme, 81 aday). Sonra kısma 0: bütçe 0, hiç yazma yok.
     */
    @GameTest(template = ALAN, batch = BUTCE, timeoutTicks = 200)
    public static void kavurmaButcesiHicbirTickteAsilmaz(GameTestHelper y) {
        cehennemSahnesi(y);
        kancalar(y, 4, 5, 0.75);
        blokHasari(y, true);
        ServerLevel level = y.getLevel();
        int[] tick = {0};
        int[] asim = {Integer.MIN_VALUE};
        int[] doldu = {0};
        int[] toplam = {0};
        y.onEachTick(() -> {
            tick[0]++;
            if (tick[0] == 61) {
                KavurmaKuyrugu.kismaKaynagiAyarla(() -> 0.0);
            }
            KavurmaKuyrugu.tick(level, 1f, 32);
            int islem = KavurmaKuyrugu.sonIslem(level);
            int butce = KavurmaKuyrugu.sonButce(level);
            asim[0] = Math.max(asim[0], islem - butce);
            if (tick[0] <= 60) {
                y.assertTrue(butce == 4, "bütçe round(5 × 0,75) = 4 olmalı: " + butce);
                toplam[0] += islem;
                if (islem == butce) {
                    doldu[0]++;
                }
            } else {
                y.assertTrue(butce == 0 && islem == 0, "kısma 0'da bütçe ve işlem 0: " + butce + "/" + islem);
            }
        });
        y.succeedWhen(() -> {
            y.assertTrue(tick[0] >= 80, "80 tick bekleniyor");
            y.assertTrue(asim[0] <= 0, "bütçe aşımı " + asim[0]);
            y.assertTrue(doldu[0] > 0, "60 tick'te bütçe hiç dolmadı (toplam " + toplam[0] + ")");
            DogalAfetler.LOGGER.info("Pulsar GameTest: 60 tick'te {} yazma, {} tick bütçe doldu, aşım {}", toplam[0],
                    doldu[0], asim[0]);
        });
    }

    /**
     * PL-3 g: {@code afetBlokHasari=false} iken pulsar etkileri tam güçte 100 tick çalışır; sahnenin hiçbir bloğu (lav
     * dahil) değişmez, ateş yoktur, tek konum dönüşümü de 0 yazar.
     */
    @GameTest(template = ALAN, batch = KAPI, timeoutTicks = 200)
    public static void kapiKapaliykenDunyaDegismez(GameTestHelper y) {
        temizBasla(y);
        cehennemSahnesi(y);
        kancalar(y, 4, 256, 1.0);
        blokHasari(y, false);
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel level = y.getLevel();
        DunyaDonemi donem = DunyaDonemi.al(server);
        BlockState[] baslangic = anlik(y);
        int[] tick = {0};
        y.onEachTick(() -> {
            tick[0]++;
            PulsarEtkileri.tick(server, level, donem, 1f, 32);
        });
        y.succeedWhen(() -> {
            y.assertTrue(tick[0] >= 100, "100 tick bekleniyor");
            List<String> f = farklar(baslangic, anlik(y));
            y.assertTrue(f.isEmpty(), "kapı kapalıyken sahne değişti: " + f.size() + ", ilki "
                    + (f.isEmpty() ? "" : f.get(0)));
            y.assertTrue(YanginYoneticisi.aktifSayi(level, sahne(y)) == 0, "kapı kapalıyken ateş çıktı");
            y.assertTrue(KavurmaKuyrugu.donustur(level, y.absolutePos(p(0, 2, 0)), 1f, 0.0) == 0
                    && is(y, 0, 2, 0, Blocks.GRASS_BLOCK), "kapı kapalıyken tek dönüşüm de yazılmamalı");
        });
    }

    /**
     * PL-3 e: oyuncusuz dünyada (doğuş noktası tek hedef) pulsar yağmuru tam güçte 1200 tick (bir gerçek dakika)
     * {@code pulsarMeteorDakikaBasina} (vars. 12) ± 1 meteor atar. Blok hasarı kapalı (krater yok).
     */
    @GameTest(template = ALAN, batch = METEOR, timeoutTicks = 1400)
    public static void meteorOraniOyuncuBasinaDakikadaAyarKadar(GameTestHelper y) {
        temizBasla(y);
        blokHasari(y, false);
        MinecraftServer server = y.getLevel().getServer();
        ServerLevel level = y.getLevel();
        DunyaDonemi donem = DunyaDonemi.al(server);
        int bas = MeteorAPI.atilanMeteorSayisi(level, PulsarEtkileri.kaynak(donem));
        int[] tick = {0};
        y.onEachTick(() -> {
            if (tick[0] < 1200) {
                tick[0]++;
                PulsarEtkileri.tick(server, level, donem, 1f, 1);
            }
        });
        y.succeedWhen(() -> {
            y.assertTrue(tick[0] >= 1200, "bir dakika bekleniyor");
            int oyuncu = Math.max(1,
                    (int) level.players().stream().filter(o -> !o.isSpectator() && o.isAlive()).count());
            double beklenen = GokyuzuAyarlari.pulsarMeteorDakikaBasina() * oyuncu;
            int atilan = MeteorAPI.atilanMeteorSayisi(level, PulsarEtkileri.kaynak(donem)) - bas;
            DogalAfetler.LOGGER.info("Pulsar GameTest: 1200 tick'te {} meteor (beklenen {}, {} hedef)", atilan,
                    beklenen, oyuncu);
            y.assertTrue(PulsarEtkileri.yagmurKimlik() >= 0, "pulsar yağmuru kurulmalı");
            y.assertTrue(Math.abs(atilan - beklenen) <= 1, "dakikada " + beklenen + " ± 1 bekleniyordu: " + atilan);
        });
    }

    // ---------------------------------------------------------------- tablo, donma, sıcaklık

    private static int donustur(GameTestHelper y, int x, int yy, int z, double zar) {
        return KavurmaKuyrugu.donustur(y.getLevel(), y.absolutePos(p(x, yy, z)), 1f, zar);
    }

    /** PL-3 a: tablonun uç noktaları dünyada (tek konum, bütçesiz yol; buz taş çukurda, su akmaz). */
    @GameTest(template = ALAN, batch = DUNYA)
    public static void tabloUcNoktalariDunyada(GameTestHelper y) {
        for (int x = 0; x < EN; x++) {
            for (int z = 0; z < EN; z++) {
                y.setBlock(p(x, 0, z), Blocks.STONE);
                for (int yy = 1; yy < BOY; yy++) {
                    y.setBlock(p(x, yy, z), Blocks.AIR);
                }
            }
        }
        y.setBlock(p(0, 1, 0), Blocks.GRASS_BLOCK);
        y.setBlock(p(2, 1, 0), Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.X));
        y.setBlock(p(4, 3, 0), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        y.setBlock(p(6, 1, 0), Blocks.GRASS_BLOCK);
        y.setBlock(p(6, 2, 0), Blocks.POPPY);
        y.setBlock(p(0, 1, 2), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3));
        y.setBlock(p(2, 1, 2), Blocks.SNOW_BLOCK);
        y.setBlock(p(4, 1, 2), Blocks.ICE);
        y.setBlock(p(6, 1, 2), Blocks.DIRT);
        y.setBlock(p(8, 1, 2), Blocks.SAND);
        y.setBlock(p(3, 1, 2), Blocks.STONE);
        y.setBlock(p(5, 1, 2), Blocks.STONE);
        y.setBlock(p(4, 1, 1), Blocks.STONE);
        y.setBlock(p(4, 1, 3), Blocks.STONE);
        y.setBlock(p(0, 1, 4), Blocks.STONE_BRICKS);

        y.assertTrue(donustur(y, 0, 1, 0, 0.5) == 1 && is(y, 0, 1, 0, Blocks.COARSE_DIRT),
                "çimen → kaba toprak; " + sutun(y, 0, 0));
        y.assertTrue(donustur(y, 2, 1, 0, 0.5) == 1, "kütük tek yazma");
        BlockState kutuk = y.getBlockState(p(2, 1, 0));
        y.assertTrue(kutuk.is(PulsarKayitlari.KOMURLESMIS_KUTUK.get())
                && kutuk.getValue(RotatedPillarBlock.AXIS) == Direction.Axis.X,
                "kütük → kömürleşmiş (eksen X): " + kutuk);
        y.assertTrue(donustur(y, 4, 3, 0, 0.5) == 2 && y.getBlockState(p(4, 3, 0)).isAir(),
                "yaprak → hava + kül (iki yazma); " + sutun(y, 4, 0));
        BlockState kul = y.getBlockState(p(4, 1, 0));
        y.assertTrue(kul.is(PulsarKayitlari.KUL_KATMANI.get()) && kul.getValue(SnowLayerBlock.LAYERS) == 1,
                "yaprağın altındaki zemine bir kat kül; " + sutun(y, 4, 0));
        y.setBlock(p(4, 3, 0), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        donustur(y, 4, 3, 0, 0.5);
        y.assertTrue(y.getBlockState(p(4, 1, 0)).getValue(SnowLayerBlock.LAYERS) == 2, "var olan kül bir kat artar");
        y.assertTrue(donustur(y, 6, 2, 0, 0.5) == 1 && y.getBlockState(p(6, 2, 0)).isAir()
                && is(y, 6, 1, 0, Blocks.GRASS_BLOCK), "çiçek → hava, zemin kalır; " + sutun(y, 6, 0));
        y.assertTrue(donustur(y, 0, 1, 2, 0.5) == 1 && y.getBlockState(p(0, 1, 2)).isAir(),
                "kar katmanı → hava");
        y.assertTrue(donustur(y, 2, 1, 2, 0.5) == 1 && y.getBlockState(p(2, 1, 2)).isAir(),
                "kar bloğu → hava");
        y.assertTrue(donustur(y, 4, 1, 2, 0.5) == 1 && is(y, 4, 1, 2, Blocks.WATER), "buz → su");
        y.assertTrue(donustur(y, 6, 1, 2, 0.0) == 1 && is(y, 6, 1, 2, PulsarKayitlari.KUL_BLOGU.get()),
                "toprak → kül bloğu (zar < güç)");
        y.assertTrue(donustur(y, 8, 1, 2, 0.5) == 0 && is(y, 8, 1, 2, Blocks.SAND), "kum %98 kalır");
        y.assertTrue(donustur(y, 8, 1, 2, 0.01) == 1 && is(y, 8, 1, 2, Blocks.GLASS), "kum → cam %2");
        y.assertTrue(donustur(y, 0, 1, 4, 0.0) == 0 && is(y, 0, 1, 4, Blocks.STONE_BRICKS),
                "tablo dışı blok değişmez");
        y.succeed();
    }

    /**
     * PL-5 (K0-24): kül katmanı kar gelince karla değişir; çözülünce kar erir, kül geri gelmez, altındaki kül bloğu ve
     * kaba toprak kalır ({@code kul_blogu} {@code #dogalafetler:donma/kar_zemini}'nde).
     */
    @GameTest(template = ALAN, batch = DUNYA)
    public static void kulKarAltindaKalirCozulunceKulBloguKalir(GameTestHelper y) {
        BlockState kul = PulsarKayitlari.KUL_KATMANI.get().defaultBlockState().setValue(SnowLayerBlock.LAYERS, 2);
        for (int x = 0; x < EN; x++) {
            for (int z = 0; z < EN; z++) {
                y.setBlock(p(x, 0, z), Blocks.STONE);
                y.setBlock(p(x, 1, z), Blocks.DIRT);
                y.setBlock(p(x, 2, z), Blocks.DIRT);
                y.setBlock(p(x, 3, z), x < 4 ? PulsarKayitlari.KUL_BLOGU.get() : Blocks.COARSE_DIRT);
                y.setBlock(p(x, 4, z), kul);
                y.setBlock(p(x, 5, z), Blocks.AIR);
            }
        }
        BlockPos a = y.absolutePos(p(0, 0, 0));
        BlockPos b = y.absolutePos(p(EN - 1, 0, EN - 1));
        int minX = Math.min(a.getX(), b.getX());
        int minZ = Math.min(a.getZ(), b.getZ());
        int maxX = Math.max(a.getX(), b.getX());
        int maxZ = Math.max(a.getZ(), b.getZ());
        int ustY = y.absolutePos(p(0, BOY - 1, 0)).getY();
        DonmaIsleyici.bitleriTemizle(y.getLevel(), minX, minZ, maxX, maxZ);
        DonmaEsikleri e = DonmaEsikleri.VARSAYILAN.titresimsiz();
        DonmaIsleyici.bolgeyiIsle(y.getLevel(), minX, minZ, maxX, maxZ, ustY,
                new DonmaIsleyici.Gecis(0, false, 100, false, 0, false, false), KisKaynagi.YOK, e);
        y.assertTrue(is(y, 1, 4, 1, Blocks.SNOW) && is(y, 1, 3, 1, PulsarKayitlari.KUL_BLOGU.get()),
                "kül bloğu üstündeki kül kara dönmeli; " + sutun(y, 1, 1));
        y.assertTrue(is(y, 6, 4, 6, Blocks.SNOW) && is(y, 6, 3, 6, Blocks.COARSE_DIRT),
                "kaba toprak üstündeki kül kara dönmeli; " + sutun(y, 6, 6));
        DonmaIsleyici.bolgeyiIsle(y.getLevel(), minX, minZ, maxX, maxZ, ustY,
                new DonmaIsleyici.Gecis(100, false, 0, false, 0, false, false), KisKaynagi.YOK, e);
        y.assertTrue(y.getBlockState(p(1, 4, 1)).isAir() && is(y, 1, 3, 1, PulsarKayitlari.KUL_BLOGU.get()),
                "çözülünce kar erir, kül geri gelmez, kül bloğu kalır; " + sutun(y, 1, 1));
        y.assertTrue(y.getBlockState(p(6, 4, 6)).isAir() && is(y, 6, 3, 6, Blocks.COARSE_DIRT),
                "çözülünce kaba toprak kalır; " + sutun(y, 6, 6));
        y.succeed();
    }

    /** PL-3 c: sıcaklığın PULSAR katkısı çalışan ayarlarla güneş patlaması artışının 2 katı; yutmada 60. sn'de 0. */
    @GameTest(template = ALAN, batch = DUNYA)
    public static void sicaklikPulsarKatkisiIkiKat(GameTestHelper y) {
        OrtamSicakligi.Ayarlar a = SicaklikAyarlari.al().ortam();
        OrtamSicakligi.Girdi ova = OrtamSicakligi.Girdi.yuzey(0.8, false, 6000, 64);
        double pulsar = OrtamSicakligi.hesapla(ova.evre(GunesEvresi.PULSAR, 1), a).katki(Katki.GUNES_EVRESI);
        double gunes = OrtamSicakligi.hesapla(ova.evre(GunesEvresi.BUYUYEN, 1), a).katki(Katki.GUNES_EVRESI);
        y.assertTrue(Math.abs(pulsar - 2.0 * a.gunesPatlamasiArtisi()) < 1e-9
                && Math.abs(pulsar - 2.0 * gunes) < 1e-9,
                "pulsar katkısı 2 × " + a.gunesPatlamasiArtisi() + " bekleniyordu: " + pulsar);
        double yutma = OrtamSicakligi.hesapla(ova.evre(GunesEvresi.KARADELIK_YUTMA, 0.667), a)
                .katki(Katki.GUNES_EVRESI);
        y.assertTrue(Math.abs(yutma) < 1e-9, "yutmada 60. sn'de pulsar ısısı sönmeli: " + yutma);
        y.succeed();
    }
}
