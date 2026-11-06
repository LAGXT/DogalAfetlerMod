package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.core.AfetYoneticisi;
import io.github.kadirberkpolat1.dogalafetler.core.DunyaDonemi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaEsikleri;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonusumTablosu;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisAlani;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisKaynagi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisMaskesi;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Donma sisteminin oyun testleri (§6 #3, #13; madde 9, N9, N10). Düz GameTest dünyasında sahne testin içinde kurulur.
 * Doğrudan yol ({@link DonmaIsleyici#bolgeyiIsle}): tablonun tersinirliği, çözülme sırası, sütun düzeyi Y kışı, oyuncu ve
 * doğa blokları, ince buz, buharlaşma ve sırası. Zamanlayıcı yolu (komutlar ve gerçek kuyruk; global ilerlemeyi
 * değiştirdikleri için her biri kendi batch'inde, kapsam yalnız sahnenin chunk'ları): dondur → çöz = başlangıç,
 * {@code afetBlokHasari=false}, chunk yetişmesi ve bütçe, {@code sifirla}.
 *
 * <p>Göl sahnesi (göreli, 9x6x9): y=0 taş; y=1..2 toprak; y=3 çimen; ortada 5x5, 3 derin göl (x,z 2..6, y 1..3); (0,3,0)
 * oyuncunun kuru çamuru; (8,3,0) kırık taş (tanınmayan); (8,4,8) kalıcı meşe yaprağı; (0,4,8) kısa ot.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DonmaOyunTestleri {
    private static final int EN = 9;
    private static final int BOY = 6;
    private static final String ALAN = "donma_alan";

    private DonmaOyunTestleri() {
    }

    private static BlockPos p(int x, int yy, int z) {
        return new BlockPos(x, yy + 1, z);
    }

    private static Block kiragili() {
        return DonmaBloklari.KIRAGILI_CIMEN.get();
    }

    private static int[] kutu(GameTestHelper y) {
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(EN - 1, 0, EN - 1));
        return new int[] {Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()),
                Math.max(a.getZ(), b.getZ())};
    }

    private static void bitleriTemizle(GameTestHelper y) {
        int[] k = kutu(y);
        DonmaIsleyici.bitleriTemizle(y.getLevel(), k[0], k[1], k[2], k[3]);
    }

    private static void sahneKur(GameTestHelper y) {
        for (int x = 0; x < EN; x++) {
            for (int z = 0; z < EN; z++) {
                boolean gol = x >= 2 && x <= 6 && z >= 2 && z <= 6;
                y.setBlock(p(x, 0, z), Blocks.STONE);
                y.setBlock(p(x, 1, z), gol ? Blocks.WATER : Blocks.DIRT);
                y.setBlock(p(x, 2, z), gol ? Blocks.WATER : Blocks.DIRT);
                y.setBlock(p(x, 3, z), gol ? Blocks.WATER : Blocks.GRASS_BLOCK);
            }
        }
        y.setBlock(p(0, 3, 0), Blocks.PACKED_MUD);
        y.setBlock(p(8, 3, 0), Blocks.COBBLESTONE);
        y.setBlock(p(8, 4, 8), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        y.setBlock(p(0, 4, 8), Blocks.GRASS);
        bitleriTemizle(y);
    }

    private static BlockState[] anlik(GameTestHelper y) {
        BlockState[] d = new BlockState[EN * BOY * EN];
        int i = 0;
        for (int yy = 0; yy < BOY; yy++) {
            for (int z = 0; z < EN; z++) {
                for (int x = 0; x < EN; x++) {
                    d[i++] = y.getBlockState(p(x, yy, z));
                }
            }
        }
        return d;
    }

    private static List<String> farklar(BlockState[] beklenen, BlockState[] gercek) {
        List<String> f = new ArrayList<>();
        for (int i = 0; i < beklenen.length; i++) {
            if (beklenen[i] != gercek[i]) {
                f.add("(" + i % EN + "," + i / (EN * EN) + "," + (i / EN) % EN + ") " + beklenen[i] + " ≠ " + gercek[i]);
            }
        }
        return f;
    }

    private static void ayniMi(GameTestHelper y, BlockState[] beklenen, String ne) {
        List<String> f = farklar(beklenen, anlik(y));
        y.assertTrue(f.isEmpty(), ne + ": " + f.size() + " blok farklı, ilki " + (f.isEmpty() ? "" : f.get(0)));
    }

    private static int isle(GameTestHelper y, DonmaIsleyici.Gecis gecis, KisKaynagi kis, DonmaEsikleri e) {
        int[] k = kutu(y);
        int ustY = y.absolutePos(p(0, BOY - 1, 0)).getY();
        return DonmaIsleyici.bolgeyiIsle(y.getLevel(), k[0], k[1], k[2], k[3], ustY, gecis, kis, e);
    }

    private static DonmaIsleyici.Gecis gecis(int onceki, boolean oncekiCozulme, int seviye, boolean cozulme) {
        return new DonmaIsleyici.Gecis(onceki, oncekiCozulme, seviye, cozulme, 0, false, true);
    }

    private static int isle(GameTestHelper y, int onceki, int seviye) {
        return isle(y, gecis(onceki, false, seviye, false), KisKaynagi.YOK, DonmaEsikleri.VARSAYILAN);
    }

    private static int buharla(GameTestHelper y, int derinlik) {
        return isle(y, new DonmaIsleyici.Gecis(0, false, 0, false, derinlik, true, true), KisKaynagi.YOK,
                DonmaEsikleri.VARSAYILAN);
    }

    private static String sutunTani(GameTestHelper y, int x, int z) {
        StringBuilder sb = new StringBuilder("sütun (" + x + "," + z + "):");
        for (int yy = 0; yy <= 5; yy++) {
            sb.append(' ').append(yy).append('=').append(y.getBlockState(p(x, yy, z)));
        }
        return sb.toString();
    }

    private static int kar(GameTestHelper y, int x, int yy, int z) {
        BlockState s = y.getBlockState(p(x, yy, z));
        return s.is(Blocks.SNOW) ? s.getValue(SnowLayerBlock.LAYERS) : 0;
    }

    private static boolean is(GameTestHelper y, int x, int yy, int z, Block b) {
        return y.getBlockState(p(x, yy, z)).is(b);
    }

    // ---------------------------------------------------------------- doğrudan yol

    @GameTest(template = ALAN)
    public static void dondurSonraCozBaslangicDurumunuVerir(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);
        y.assertTrue(isle(y, 0, 100) > 0, "dondur(100) hiç blok değiştirmedi; " + sutunTani(y, 4, 4));
        y.assertTrue(is(y, 4, 3, 4, Blocks.ICE) && is(y, 4, 2, 4, Blocks.ICE), "göl yüzeyi 2 blok buz; " + sutunTani(y, 4, 4));
        y.assertTrue(is(y, 4, 1, 4, Blocks.WATER), "gölün en altı su kalmalı");
        y.assertTrue(kar(y, 1, 4, 1) == 1, "çimende tek kat kar; " + sutunTani(y, 1, 1));
        BlockState cimen = y.getBlockState(p(1, 3, 1));
        y.assertTrue(cimen.is(kiragili()) && cimen.getValue(SnowyDirtBlock.SNOWY), "çimen kırağılı ve karlı: " + cimen);
        y.assertTrue(is(y, 0, 3, 0, Blocks.PACKED_MUD) && y.getBlockState(p(0, 4, 0)).isAir(), "kuru çamura dokunulmamalı");
        y.assertTrue(is(y, 8, 3, 0, Blocks.COBBLESTONE) && y.getBlockState(p(8, 4, 0)).isAir(), "tanınmayan bloğa dokunulmamalı");
        y.assertTrue(kar(y, 8, 5, 8) >= 4, "yaprağın üstünde kalın kar; " + sutunTani(y, 8, 8));
        y.assertTrue(is(y, 0, 4, 8, Blocks.GRASS) && is(y, 0, 3, 8, Blocks.GRASS_BLOCK), "bitkili zemine dokunulmamalı");
        BlockState[] donuk = anlik(y);
        isle(y, 100, 30);
        isle(y, 30, 100);
        ayniMi(y, donuk, "100 → 30 → 100 doğrudan 100 ile aynı olmalı");
        y.assertTrue(isle(y, 100, 0) > 0, "çöz(0) hiç blok değiştirmedi");
        ayniMi(y, baslangic, "dondur(100) → çöz(0) başlangıcı vermeli");
        y.runAfterDelay(20, () -> {
            ayniMi(y, baslangic, "çözüldükten 20 tick sonra da başlangıçla aynı olmalı");
            y.succeed();
        });
    }

    @GameTest(template = ALAN)
    public static void araSeviyedenCozBaslangicDurumunuVerir(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);
        for (int seviye = 5; seviye <= 60; seviye += 5) {
            isle(y, seviye - 5, seviye);
        }
        y.assertTrue(is(y, 2, 3, 4, Blocks.ICE), "60'ta kıyı sütunu donmuş olmalı; " + sutunTani(y, 2, 4));
        y.assertTrue(kar(y, 1, 4, 1) == 1, "60'ta çimende kar olmalı");
        for (int seviye = 55; seviye >= 0; seviye -= 5) {
            isle(y, seviye + 5, seviye);
        }
        ayniMi(y, baslangic, "5'lik adımlarla 60'a çıkıp 0'a inmek başlangıcı vermeli");
        y.succeed();
    }

    /** Tasarım notu §2: su → ince buz → buz; çözülürken buz → ince buz → su. Kıyı satırı x=2 (uzaklık 0). */
    @GameTest(template = ALAN)
    public static void inceBuzIlkVeSonEvredir(GameTestHelper y) {
        sahneKur(y);
        DonmaEsikleri e = DonmaEsikleri.VARSAYILAN.titresimsiz();
        Block ince = DonmaBloklari.INCE_BUZ.get();
        isle(y, gecis(0, false, 21, false), KisKaynagi.YOK, e);
        y.assertTrue(is(y, 2, 3, 4, ince), "21'de kıyı ince buz; " + sutunTani(y, 2, 4));
        y.assertTrue(is(y, 4, 3, 4, Blocks.WATER), "21'de göl ortası açık; " + sutunTani(y, 4, 4));
        isle(y, gecis(21, false, 30, false), KisKaynagi.YOK, e);
        y.assertTrue(is(y, 2, 3, 4, Blocks.ICE), "30'da kıyı buz; " + sutunTani(y, 2, 4));
        isle(y, gecis(30, false, 46, false), KisKaynagi.YOK, e);
        isle(y, gecis(46, false, 45, false), KisKaynagi.YOK, e);
        y.assertTrue(is(y, 2, 3, 4, ince), "çözülürken 45'te kıyı ince (çürük) buz; " + sutunTani(y, 2, 4));
        isle(y, gecis(45, false, 44, false), KisKaynagi.YOK, e);
        y.assertTrue(is(y, 2, 3, 4, Blocks.WATER), "44'te kıyı açık; " + sutunTani(y, 2, 4));
        y.succeed();
    }

    /** Madde 9 ÇÖZÜLME: 100'den taban 40'a inince kar ve kırağı kalkmış, buz kıyıdan açılmış, ortada kalmış olmalı. */
    @GameTest(template = ALAN)
    public static void cozulmeKarVeKiragiOnceBuzKiyidanIceriAcilir(GameTestHelper y) {
        sahneKur(y);
        DonmaEsikleri e = DonmaEsikleri.VARSAYILAN.titresimsiz();
        isle(y, gecis(0, false, 100, false), KisKaynagi.YOK, e);
        isle(y, gecis(100, false, 40, false), KisKaynagi.YOK, e);
        y.assertTrue(kar(y, 1, 4, 1) == 0 && is(y, 1, 3, 1, Blocks.GRASS_BLOCK), "40'ta kar ve kırağı kalkmalı; "
                + sutunTani(y, 1, 1));
        y.assertTrue(kar(y, 8, 5, 8) == 0, "40'ta yaprakta kar kalmamalı; " + sutunTani(y, 8, 8));
        y.assertTrue(is(y, 2, 3, 4, Blocks.WATER) && is(y, 3, 3, 4, Blocks.WATER), "40'ta kıyı halkası açık; "
                + sutunTani(y, 2, 4) + " | " + sutunTani(y, 3, 4));
        y.assertTrue(is(y, 4, 3, 4, Blocks.ICE) || is(y, 4, 3, 4, DonmaBloklari.INCE_BUZ.get()),
                "40'ta göl ortası donmuş kalmalı (kıyıya 2 blok: çürük ince buz evresi); " + sutunTani(y, 4, 4));
        y.assertTrue(is(y, 4, 2, 4, Blocks.WATER), "çözülürken göl ortası incelmiş olmalı; " + sutunTani(y, 4, 4));
        y.succeed();
    }

    /** K0-24: çözülme döneminde Y üst sınırındaki sütun %100'e, alt sınırdaki G'ye gelir (sütun düzeyi kış ağırlığı). */
    @GameTest(template = ALAN)
    public static void cozulmeDonemindeYuksekSutunYuzdeYuzeAlcakSutunGyeGelir(GameTestHelper y) {
        for (int x = 0; x < EN; x++) {
            for (int z = 0; z < EN; z++) {
                y.setBlock(p(x, 0, z), Blocks.STONE);
                for (int yy = 1; yy <= 4; yy++) {
                    y.setBlock(p(x, yy, z), x >= 5 ? Blocks.STONE : yy == 1 ? Blocks.GRASS_BLOCK : Blocks.AIR);
                }
            }
        }
        bitleriTemizle(y);
        int alt = y.absolutePos(p(0, 1, 0)).getY();
        int ust = y.absolutePos(p(0, 4, 0)).getY();
        KisKaynagi yukseklik = new KisKaynagi() {
            @Override
            public double agirlik(int x, int z, int yuzeyY) {
                return KisAlani.yumusakBasamak(alt, ust, yuzeyY);
            }

            @Override
            public double ofset(int x, int z) {
                return 0.0;
            }
        };
        isle(y, gecis(0, false, 40, true), yukseklik, DonmaEsikleri.VARSAYILAN.titresimsiz());
        for (int z = 0; z < EN; z++) {
            for (int x = 5; x < EN; x++) {
                y.assertTrue(kar(y, x, 5, z) == 5, "yüksek sütun %100 (5 kat kar) olmalı; " + sutunTani(y, x, z));
            }
            for (int x = 0; x < 5; x++) {
                y.assertTrue(is(y, x, 1, z, kiragili()) && kar(y, x, 2, z) == 1, "alçak sütun G=40 (kırağı + 1 kat kar); "
                        + sutunTani(y, x, z));
            }
        }
        y.succeed();
    }

    /** K0-24: oyuncunun kuru çamuru, 3 katlı karı ve su üstündeki buzu 0 → 100 → 40 → 0 boyunca değişmez. */
    @GameTest(template = ALAN)
    public static void oyuncuBloklariDegismez(GameTestHelper y) {
        for (int x = 0; x < EN; x++) {
            for (int z = 0; z < EN; z++) {
                for (int yy = 0; yy <= 2; yy++) {
                    y.setBlock(p(x, yy, z), Blocks.STONE);
                }
            }
        }
        y.setBlock(p(1, 2, 1), Blocks.PACKED_MUD);
        y.setBlock(p(3, 3, 1), Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 3));
        y.setBlock(p(6, 1, 6), Blocks.WATER);
        y.setBlock(p(6, 2, 6), Blocks.ICE);
        bitleriTemizle(y);
        BlockPos[] izlenen = {p(1, 2, 1), p(1, 3, 1), p(3, 2, 1), p(3, 3, 1), p(6, 0, 6), p(6, 1, 6), p(6, 2, 6), p(6, 3, 6)};
        BlockState[] ilk = new BlockState[izlenen.length];
        for (int i = 0; i < izlenen.length; i++) {
            ilk[i] = y.getBlockState(izlenen[i]);
        }
        int[][] gecisler = {{0, 100}, {100, 40}, {40, 0}};
        for (int[] g : gecisler) {
            isle(y, g[0], g[1]);
            for (int i = 0; i < izlenen.length; i++) {
                y.assertTrue(y.getBlockState(izlenen[i]) == ilk[i], g[0] + " → " + g[1] + ": oyuncu bloğu değişti "
                        + izlenen[i] + ": " + ilk[i] + " ≠ " + y.getBlockState(izlenen[i]));
            }
        }
        y.assertTrue(kar(y, 5, 3, 5) == 0, "0'da bizim karımız kalmamalı");
        y.succeed();
    }

    /** Doğal buz ve kar (soğuk biyom benzeri) donma başlarken ve taban %40'a çözülürken kalır. */
    @GameTest(template = ALAN)
    public static void donmaBaslarkenDogalKarVeBuzKorunur(GameTestHelper y) {
        sahneKur(y);
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                y.setBlock(p(x, 3, z), Blocks.ICE);
            }
        }
        y.setBlock(p(1, 4, 1), Blocks.SNOW);
        for (int seviye = 5; seviye <= 20; seviye += 5) {
            isle(y, seviye - 5, seviye);
        }
        dogalKorundu(y, "donma başında 0 → 20");
        isle(y, 20, 100);
        isle(y, 100, 40);
        dogalKorundu(y, "100 → 40 çözülmesinde");
        y.succeed();
    }

    private static void dogalKorundu(GameTestHelper y, String ne) {
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                y.assertTrue(is(y, x, 3, z, Blocks.ICE), ne + ": doğal göl buzu eridi; " + sutunTani(y, x, z));
            }
        }
        y.assertTrue(kar(y, 1, 4, 1) == 1, ne + ": doğal kar değişti; " + sutunTani(y, 1, 1));
        BlockState cimen = y.getBlockState(p(1, 3, 1));
        y.assertTrue(cimen.is(Blocks.GRASS_BLOCK) && cimen.getValue(SnowyDirtBlock.SNOWY), ne + ": karlı çimen değişti");
    }

    @GameTest(template = ALAN)
    public static void blokHasariKapaliykenBlokDegismez(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);
        ServerLevel level = y.getLevel();
        GameRules.BooleanValue kural = level.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        boolean eski = kural.get();
        int degisen;
        try {
            kural.set(false, level.getServer());
            degisen = isle(y, 0, 100) + buharla(y, 6);
        } finally {
            kural.set(eski, level.getServer());
        }
        y.assertTrue(degisen == 0, "afetBlokHasari=false iken " + degisen + " blok değişti");
        ayniMi(y, baslangic, "afetBlokHasari=false iken bloklar aynı kalmalı");
        y.succeed();
    }

    /** N9: su yüzeyden aşağı derinlik kadar kurur, sığ göl tamamen kurur, derinlik 0'a inince aynı su geri gelir. */
    @GameTest(template = ALAN)
    public static void buharlasmaDerinlikKadarKururVeGeriDolar(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);
        buharla(y, 2);
        y.assertTrue(y.getBlockState(p(4, 3, 4)).isAir() && y.getBlockState(p(4, 2, 4)).isAir()
                && is(y, 4, 1, 4, Blocks.WATER), "derinlik 2: üst iki kat kurumalı, altı su; " + sutunTani(y, 4, 4));
        y.assertTrue(is(y, 1, 3, 1, Blocks.GRASS_BLOCK), "kara sütununa dokunulmamalı");
        buharla(y, 6);
        y.assertTrue(y.getBlockState(p(4, 1, 4)).isAir(), "derinlik 6: 3 derin göl tamamen kurur; " + sutunTani(y, 4, 4));
        y.assertTrue(is(y, 4, 0, 4, Blocks.STONE), "göl yatağı kalır");
        buharla(y, 0);
        ayniMi(y, baslangic, "derinlik 0'a inince su geri gelmeli");
        y.runAfterDelay(20, () -> {
            ayniMi(y, baslangic, "geri dolduktan 20 tick sonra su akmamış olmalı");
            y.succeed();
        });
    }

    /** N10: aynı geçişte önce geri dolma, sonra donma; kurumuş göl süpernovadan sonra yüzeyinden donar. */
    @GameTest(template = ALAN)
    public static void buharlasmaSonrasiOnceGeriDolarSonraDonar(GameTestHelper y) {
        sahneKur(y);
        buharla(y, 6);
        isle(y, new DonmaIsleyici.Gecis(0, false, 100, false, 0, true, true), KisKaynagi.YOK, DonmaEsikleri.VARSAYILAN);
        y.assertTrue(is(y, 4, 3, 4, Blocks.ICE) && is(y, 4, 1, 4, Blocks.WATER),
                "geri dolan göl yüzeyinden donmalı; " + sutunTani(y, 4, 4));
        y.succeed();
    }

    @GameTest(template = ALAN)
    public static void buzYuzeyiSorgusu(GameTestHelper y) {
        sahneKur(y);
        isle(y, 0, 100);
        ServerLevel level = y.getLevel();
        y.assertTrue(DonmaApi.buzYuzeyiMi(level, y.absolutePos(p(4, 3, 4))), "göl buzu su üstünde buz sayılmalı");
        y.setBlock(p(0, 4, 0), Blocks.ICE);
        y.assertTrue(!DonmaApi.buzYuzeyiMi(level, y.absolutePos(p(0, 4, 0))), "kuru çamur üstündeki buz sayılmamalı");
        y.assertTrue(!DonmaApi.buzYuzeyiMi(level, y.absolutePos(p(4, 1, 4))), "su buz değil");
        y.succeed();
    }

    @GameTest(template = "bos")
    public static void tabloVeriPaketindenYuklenir(GameTestHelper y) {
        DonmaTablosuYukleyici.Tablo tablo = DonmaTablosuYukleyici.etkin();
        y.assertTrue(tablo.suKaynak() == Blocks.WATER && tablo.suHedef() == Blocks.ICE
                && tablo.suInce() == DonmaBloklari.INCE_BUZ.get(), "su sütunu satırı yüklenmedi");
        y.assertTrue(tablo.yuzey().durum(Blocks.GRASS_BLOCK, 10) == kiragili(), "çimen → kırağılı çimen yüklenmedi");
        y.assertTrue(tablo.yuzey().durum(kiragili(), 45, -1) == Blocks.GRASS_BLOCK, "kırağı 50'nin altında kalkmalı");
        y.assertTrue(tablo.yuzey().durum(kiragili(), 55, -1) == kiragili(), "kırağı 50'nin üstünde kalmalı");
        for (DonusumTablosu.Donusum<Block> d : tablo.yuzey().donusumler()) {
            y.assertTrue(d.cozulmeEsik() > DonmaEsikleri.VARSAYILAN.buzCozulmeTabani(), "yüzey çözülmesi buzdan önce: " + d);
        }
        y.assertTrue(!tablo.yuzey().taniyor(Blocks.PACKED_MUD) && !tablo.yuzey().taniyor(Blocks.COBBLESTONE),
                "yalnız kendi hedeflerimiz tanınmalı");
        boolean reddedildi = false;
        try {
            DonmaTablosuYukleyici.yuzeySatiriniDenetle(Blocks.MUD, Blocks.PACKED_MUD, false);
        } catch (IllegalArgumentException beklenen) {
            reddedildi = true;
        }
        y.assertTrue(reddedildi, "yabancı hedefli yüzey satırı reddedilmeli");
        DonmaTablosuYukleyici.yuzeySatiriniDenetle(Blocks.MUD, Blocks.PACKED_MUD, true);
        boolean ozellikReddi = false;
        try {
            DonmaTablosuYukleyici.yuzeySatiriniDenetle(Blocks.GRASS_BLOCK, Blocks.PACKED_MUD, true);
        } catch (IllegalArgumentException beklenen) {
            ozellikReddi = true;
        }
        y.assertTrue(ozellikReddi, "özellikleri farklı satır reddedilmeli");
        y.succeed();
    }

    @GameTest(template = "bos")
    public static void chunkVerisiCapabilityIleNbtyeYazilir(GameTestHelper y) {
        LevelChunk chunk = y.getLevel().getChunkAt(y.absolutePos(BlockPos.ZERO));
        y.assertTrue(ChunkDonmaVerisi.al(chunk) != null, "üst dünya chunk'ında capability yok");
        ChunkDonmaVerisi.Saglayici s = new ChunkDonmaVerisi.Saglayici();
        s.veri.donguyeGec(77L, 42, true);
        s.veri.yarimAdim(47, true);
        s.veri.sonBuhar(3);
        s.veri.kar(17, true);
        s.veri.buz(255, true);
        s.veri.kuru(9, -12, 4);
        CompoundTag nbt = s.serializeNBT();
        ChunkDonmaVerisi.Saglayici yeni = new ChunkDonmaVerisi.Saglayici();
        yeni.deserializeNBT(nbt);
        ChunkDonmaVerisi v = yeni.veri;
        y.assertTrue(v.sonG() == 42 && v.cozulme() && v.dongu() == 77L && v.sonBuhar() == 3, "G, dönem, döngü, buhar");
        y.assertTrue(v.yarimVar() && v.yarimG() == 47 && v.yarimCozulme(), "yarım adım korunmadı");
        y.assertTrue(v.kar(17) && !v.kar(16) && v.buz(255) && !v.buz(0), "bitler korunmadı");
        y.assertTrue(v.kuruSayi(9) == 4 && v.kuruUstY(9) == -12 && v.kuruSayi(10) == 0, "kuru kaydı korunmadı");
        v.durum(50, false);
        y.assertTrue(!v.yarimVar(), "adım bitince yarım silinmeli");
        y.assertTrue(chunk.writeCapsToNBT().contains(ChunkDonmaVerisi.KIMLIK.toString()), "ForgeCaps'te " + ChunkDonmaVerisi.KIMLIK
                + " yok");
        y.succeed();
    }

    /** §6 #2 (GameTest'te gerçek dünya): kalıcı kış ağırlığı aynı seed'de iki ayrı hesapta aynı, sınırlar içinde. */
    @GameTest(template = "bos")
    public static void kisMaskesiAyniSeeddeAyni(GameTestHelper y) {
        ServerLevel level = y.getLevel();
        int n = 400;
        double[] ilk = new double[n];
        KisBolgesi.sifirla();
        KisAlani a = KisBolgesi.alan(level);
        for (int i = 0; i < n; i++) {
            int x = i * 977 - 180_000;
            int z = i * 613 + 90_000;
            ilk[i] = a.chunk(x >> 4, z >> 4).agirlik(x, z, 70);
        }
        KisBolgesi.sifirla();
        KisAlani b = KisBolgesi.alan(level);
        y.assertTrue(a != b, "ikinci hesap yeni bir alan olmalı");
        KisMaskesi m1 = new KisMaskesi(level.getSeed(), 0.25, 1200, 0.03);
        KisMaskesi m2 = new KisMaskesi(level.getSeed(), 0.25, 1200, 0.03);
        for (int i = 0; i < n; i++) {
            int x = i * 977 - 180_000;
            int z = i * 613 + 90_000;
            double w = b.chunk(x >> 4, z >> 4).agirlik(x, z, 70);
            y.assertTrue(w == ilk[i] && w >= 0 && w <= 1, "kış ağırlığı değişti: " + ilk[i] + " ≠ " + w);
            y.assertTrue(m1.agirlikNoktada(x, z) == m2.agirlikNoktada(x, z), "maske aynı seed'de farklı");
        }
        y.succeed();
    }

    // ---------------------------------------------------------------- zamanlayıcı yolu (kendi batch'lerinde)

    private static LongOpenHashSet sahneChunklari(GameTestHelper y) {
        int[] k = kutu(y);
        LongOpenHashSet s = new LongOpenHashSet();
        for (int cx = k[0] >> 4; cx <= k[2] >> 4; cx++) {
            for (int cz = k[1] >> 4; cz <= k[3] >> 4; cz++) {
                s.add(ChunkPos.asLong(cx, cz));
            }
        }
        return s;
    }

    /** Zamanlayıcı testinin başı: dönem ve kuyruk temiz, kapsam sahnenin chunk'ları, chunk'lar G=0'da ve güncel döngüde. */
    private static LongOpenHashSet zamanlayiciHazirla(GameTestHelper y) {
        MinecraftServer server = y.getLevel().getServer();
        DunyaDonemi d = DunyaDonemi.al(server);
        d.donmaIlerlemesiAyarla(0f);
        d.cozulmeDonemiAyarla(false);
        d.buharlasmaDerinligiAyarla(0f);
        LongOpenHashSet chunklar = sahneChunklari(y);
        for (long c : chunklar) {
            ChunkDonmaVerisi v = ChunkDonmaVerisi.al(y.getLevel().getChunk(ChunkPos.getX(c), ChunkPos.getZ(c)));
            v.donguyeGec(DonmaZamanlayici.dongu(d), 0, false);
        }
        DonmaZamanlayici.kapsamAyarla(chunklar);
        DonmaZamanlayici.istatistikSifirla();
        return chunklar;
    }

    private static void zamanlayiciBitir(GameTestHelper y) {
        DunyaDonemi.al(y.getLevel().getServer()).donmaIlerlemesiAyarla(0f);
        DonmaZamanlayici.kapsamAyarla(null);
    }

    private static void komut(GameTestHelper y, String komut) {
        MinecraftServer server = y.getLevel().getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), komut);
    }

    private static boolean hepsi(GameTestHelper y, LongOpenHashSet chunklar, int g) {
        for (long c : chunklar) {
            ChunkDonmaVerisi v = ChunkDonmaVerisi.al(y.getLevel().getChunk(ChunkPos.getX(c), ChunkPos.getZ(c)));
            if (v.sonG() != g || v.yarimVar()) {
                return false;
            }
        }
        return true;
    }

    /** §6 #3 zamanlayıcı yolu: {@code donma 100} → bloklar donar → {@code donma 0} → sahne başlangıçla aynı. */
    @GameTest(template = ALAN, batch = "donma_zamanlayici_1", timeoutTicks = 3000)
    public static void zamanlayiciKomutlaDondurCozBaslangicVerir(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);
        LongOpenHashSet chunklar = zamanlayiciHazirla(y);
        komut(y, "dogalafet donma 100 donma");
        boolean[] cozuluyor = {false};
        y.onEachTick(() -> {
            if (!cozuluyor[0] && hepsi(y, chunklar, 100)) {
                y.assertTrue(is(y, 4, 3, 4, Blocks.ICE) && kar(y, 1, 4, 1) == 1, "100'de sahne donmuş olmalı; "
                        + sutunTani(y, 4, 4));
                cozuluyor[0] = true;
                komut(y, "dogalafet donma 0");
            }
        });
        y.succeedWhen(() -> {
            y.assertTrue(cozuluyor[0] && hepsi(y, chunklar, 0), "chunk'lar henüz çözülmedi");
            ayniMi(y, baslangic, "zamanlayıcıyla dondur → çöz başlangıcı vermeli");
            zamanlayiciBitir(y);
        });
    }

    /** §6 #3: {@code afetBlokHasari=false} iken ilerleme artar ama hiçbir blok değişmez (kuyruk durur). */
    @GameTest(template = ALAN, batch = "donma_zamanlayici_2", timeoutTicks = 400)
    public static void zamanlayiciBlokHasariKapaliykenBlokDegismez(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);
        LongOpenHashSet chunklar = zamanlayiciHazirla(y);
        ServerLevel level = y.getLevel();
        GameRules.BooleanValue kural = level.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        kural.set(false, level.getServer());
        komut(y, "dogalafet donma 100");
        y.runAfterDelay(100, () -> {
            try {
                y.assertTrue(DunyaDonemi.al(level.getServer()).donmaIlerlemesi() == 100f, "ilerleme yine artmalı");
                y.assertTrue(hepsi(y, chunklar, 0), "kuyruk durmalı (chunk G=0 kalmalı)");
                ayniMi(y, baslangic, "afetBlokHasari=false iken bloklar aynı kalmalı");
            } finally {
                kural.set(true, level.getServer());
                zamanlayiciBitir(y);
            }
            y.succeed();
        });
    }

    /**
     * §6 #13: hedef 60 iken yeniden yüklenen chunk adım adım yetişir (birden çok tick, ara değerler) ve hiçbir tick'te
     * blok bütçesi aşılmaz; sonra 0'a geri çözülür.
     */
    @GameTest(template = ALAN, batch = "donma_zamanlayici_3", timeoutTicks = 3000)
    public static void chunkYetismesiAdimAdimVeButceAsilmaz(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);
        LongOpenHashSet chunklar = zamanlayiciHazirla(y);
        long ilk = chunklar.iterator().nextLong();
        LevelChunk chunk = y.getLevel().getChunk(ChunkPos.getX(ilk), ChunkPos.getZ(ilk));
        DunyaDonemi.al(y.getLevel().getServer()).donmaIlerlemesiAyarla(60f);
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Unload(chunk));
        MinecraftForge.EVENT_BUS.post(new ChunkEvent.Load(chunk, false));
        TreeSet<Integer> gorulen = new TreeSet<>();
        int[] tick = {0};
        int[] doldugu = {-1};
        y.onEachTick(() -> {
            tick[0]++;
            y.assertTrue(DonmaZamanlayici.sonTickDegisen() <= DonmaZamanlayici.sonBlokSiniri(), "tick bütçesi aşıldı: "
                    + DonmaZamanlayici.sonTickDegisen() + " > " + DonmaZamanlayici.sonBlokSiniri());
            gorulen.add(ChunkDonmaVerisi.al(chunk).sonG());
            if (doldugu[0] < 0 && hepsi(y, chunklar, 60)) {
                doldugu[0] = tick[0];
                DunyaDonemi.al(y.getLevel().getServer()).donmaIlerlemesiAyarla(0f);
            }
        });
        y.succeedWhen(() -> {
            y.assertTrue(doldugu[0] > 0 && hepsi(y, chunklar, 0), "yetişme ve geri çözülme bitmedi");
            y.assertTrue(doldugu[0] >= 3, "chunk tek seferde yetişmemeli: " + doldugu[0] + " tick");
            y.assertTrue(gorulen.subSet(1, 60).size() >= 2, "ara seviyeler görülmeli: " + gorulen);
            y.assertTrue(DonmaZamanlayici.enCokTickDegisen() <= DonmaZamanlayici.sonBlokSiniri(), "en çok tick değişimi "
                    + DonmaZamanlayici.enCokTickDegisen());
            ayniMi(y, baslangic, "yetişip geri çözülünce sahne başlangıçla aynı");
            zamanlayiciBitir(y);
        });
    }

    /** {@code /dogalafet sifirla} veriyi sıfırlar, blokları çözmez: chunk yeni döngüde G=0'ı benimser, kalan buz yabancıdır. */
    @GameTest(template = ALAN, batch = "donma_zamanlayici_4", timeoutTicks = 2000)
    public static void sifirlaBloklariCozmezVeriyiSifirlar(GameTestHelper y) {
        sahneKur(y);
        LongOpenHashSet chunklar = zamanlayiciHazirla(y);
        MinecraftServer server = y.getLevel().getServer();
        DunyaDonemi.al(server).donmaIlerlemesiAyarla(30f);
        BlockState[][] donuk = {null};
        int[] bekle = {0};
        y.onEachTick(() -> {
            if (donuk[0] == null && hepsi(y, chunklar, 30)) {
                donuk[0] = anlik(y);
                AfetYoneticisi.al(server).sifirla();
            } else if (donuk[0] != null) {
                bekle[0]++;
            }
        });
        y.succeedWhen(() -> {
            y.assertTrue(donuk[0] != null && bekle[0] >= 40, "30'a donma ve sıfırlama sonrası bekleme");
            y.assertTrue(hepsi(y, chunklar, 0), "chunk'lar yeni döngüde G=0 olmalı");
            ayniMi(y, donuk[0], "sifirla blokları çözmemeli");
            zamanlayiciBitir(y);
        });
    }
}
