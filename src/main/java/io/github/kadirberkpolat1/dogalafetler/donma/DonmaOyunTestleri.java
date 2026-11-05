package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * Donma tablosunun dünyada tersinirliği (§6 #3: dondur → çöz = başlangıç; {@code afetBlokHasari=false} iken blok
 * değişmez), çözülme sırası (madde 9 ÇÖZÜLME), sütun düzeyinde Y kalıcı kışı, oyuncu ve doğa bloklarının korunması,
 * veri paketinden yüklenen tablo ve chunk capability'si.
 *
 * <p>Göl sahnesi (göreli, 9x6x9): y=0 taş; y=1..2 toprak; y=3 çimen; ortada 5x5, 3 derin göl (x,z 2..6, y 1..3); (0,3,0)
 * oyuncunun kuru çamuru; (8,3,0) kırık taş (tanınmayan); (8,4,8) kalıcı meşe yaprağı; (0,4,8) kısa ot.
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DonmaOyunTestleri {
    private static final int EN = 9;
    private static final int BOY = 6;

    private DonmaOyunTestleri() {
    }

    /** Şablon yapı bloğunun bir üstüne yerleşir ({@code StructureBlockEntity.structurePos} = (0, 1, 0)). */
    private static BlockPos p(int x, int yy, int z) {
        return new BlockPos(x, yy + 1, z);
    }

    private static Block kiragiliCimen() {
        return DonmaBloklari.KIRAGILI_CIMEN.get();
    }

    private static void bitleriTemizle(GameTestHelper y) {
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(EN - 1, 0, EN - 1));
        DonmaIsleyici.bitleriTemizle(y.getLevel(), Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getZ(), b.getZ()));
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
                int x = i % EN;
                int z = (i / EN) % EN;
                int yy = i / (EN * EN);
                f.add("(" + x + "," + yy + "," + z + ") " + beklenen[i] + " ≠ " + gercek[i]);
            }
        }
        return f;
    }

    private static void ayniMi(GameTestHelper y, BlockState[] beklenen, String ne) {
        List<String> f = farklar(beklenen, anlik(y));
        y.assertTrue(f.isEmpty(), ne + ": " + f.size() + " blok farklı, ilki " + (f.isEmpty() ? "" : f.get(0)));
    }

    private static int isle(GameTestHelper y, int oncekiG, boolean oncekiCozulme, int g, boolean cozulme, KisKaynagi kis,
            DonmaEsikleri e) {
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(EN - 1, 0, EN - 1));
        int ustY = y.absolutePos(p(0, BOY - 1, 0)).getY();
        return DonmaIsleyici.bolgeyiIsle(y.getLevel(), Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getZ(), b.getZ()), ustY, oncekiG, oncekiCozulme, g, cozulme, kis, e);
    }

    private static int isle(GameTestHelper y, int onceki, int seviye) {
        return isle(y, onceki, false, seviye, false, KisKaynagi.YOK, DonmaEsikleri.VARSAYILAN);
    }

    /** Hata mesajı için sütunun göreli y=0..5 blokları ve MOTION_BLOCKING yüksekliği. */
    private static String sutunTani(GameTestHelper y, int x, int z) {
        BlockPos mutlak = y.absolutePos(p(x, 0, z));
        int h = y.getLevel().getChunkAt(mutlak).getHeight(Heightmap.Types.MOTION_BLOCKING, mutlak.getX() & 15,
                mutlak.getZ() & 15);
        StringBuilder sb = new StringBuilder("sütun (" + x + "," + z + ") yükseklik göreli " + (h - mutlak.getY()) + ":");
        for (int yy = 0; yy <= 5; yy++) {
            sb.append(' ').append(yy).append('=').append(y.getBlockState(p(x, yy, z)));
        }
        return sb.toString();
    }

    private static int kar(GameTestHelper y, int x, int yy, int z) {
        BlockState s = y.getBlockState(p(x, yy, z));
        return s.is(Blocks.SNOW) ? s.getValue(SnowLayerBlock.LAYERS) : 0;
    }

    @GameTest(template = "donma_alan")
    public static void dondurSonraCozBaslangicDurumunuVerir(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);

        int donan = isle(y, 0, 100);
        y.assertTrue(donan > 0, "dondur(100) hiç blok değiştirmedi; " + sutunTani(y, 4, 4));
        y.assertTrue(y.getBlockState(p(4, 3, 4)).is(Blocks.ICE), "göl yüzeyi buz olmalı; " + sutunTani(y, 4, 4));
        y.assertTrue(y.getBlockState(p(4, 2, 4)).is(Blocks.ICE), "buz 2 blok kalın olmalı");
        y.assertTrue(y.getBlockState(p(4, 1, 4)).is(Blocks.WATER), "gölün en altı su kalmalı");
        y.assertTrue(kar(y, 1, 4, 1) == 1, "çimende tek kat kar; " + sutunTani(y, 1, 1));
        BlockState cimen = y.getBlockState(p(1, 3, 1));
        y.assertTrue(cimen.is(kiragiliCimen()) && cimen.getValue(SnowyDirtBlock.SNOWY), "çimen kırağılı ve karlı: " + cimen);
        y.assertTrue(y.getBlockState(p(0, 3, 0)).is(Blocks.PACKED_MUD) && y.getBlockState(p(0, 4, 0)).isAir(),
                "oyuncunun kuru çamuruna dokunulmamalı");
        y.assertTrue(y.getBlockState(p(8, 3, 0)).is(Blocks.COBBLESTONE) && y.getBlockState(p(8, 4, 0)).isAir(),
                "tanınmayan bloğa dokunulmamalı");
        y.assertTrue(kar(y, 8, 5, 8) >= 4, "yaprağın üstünde kalın kar olmalı; " + sutunTani(y, 8, 8));
        y.assertTrue(y.getBlockState(p(0, 4, 8)).is(Blocks.GRASS) && y.getBlockState(p(0, 3, 8)).is(Blocks.GRASS_BLOCK),
                "bitkili zemine dokunulmamalı");
        BlockState[] donuk = anlik(y);

        isle(y, 100, 30);
        isle(y, 30, 100);
        ayniMi(y, donuk, "100 → 30 → 100 doğrudan 100 ile aynı olmalı");

        int cozulen = isle(y, 100, 0);
        y.assertTrue(cozulen > 0, "çöz(0) hiç blok değiştirmedi");
        ayniMi(y, baslangic, "dondur(100) → çöz(0) başlangıcı vermeli");
        y.runAfterDelay(20, () -> {
            ayniMi(y, baslangic, "çözüldükten 20 tick sonra da başlangıçla aynı olmalı");
            y.succeed();
        });
    }

    @GameTest(template = "donma_alan")
    public static void araSeviyedenCozBaslangicDurumunuVerir(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);
        for (int seviye = 5; seviye <= 60; seviye += 5) {
            isle(y, seviye - 5, seviye);
        }
        y.assertTrue(y.getBlockState(p(2, 3, 4)).is(Blocks.ICE), "60'ta kıyı sütunu donmuş olmalı; " + sutunTani(y, 2, 4));
        y.assertTrue(kar(y, 1, 4, 1) == 1, "60'ta çimende kar olmalı");
        for (int seviye = 55; seviye >= 0; seviye -= 5) {
            isle(y, seviye + 5, seviye);
        }
        ayniMi(y, baslangic, "5'lik adımlarla 60'a çıkıp 0'a inmek başlangıcı vermeli");
        y.succeed();
    }

    /** Madde 9 ÇÖZÜLME: 100'den taban 40'a inince kar ve kırağı kalkmış, buz kıyıdan açılmış, ortada kalmış olmalı. */
    @GameTest(template = "donma_alan")
    public static void cozulmeKarVeKiragiOnceBuzKiyidanIceriAcilir(GameTestHelper y) {
        sahneKur(y);
        DonmaEsikleri e = DonmaEsikleri.VARSAYILAN.titresimsiz();
        isle(y, 0, false, 100, false, KisKaynagi.YOK, e);
        y.assertTrue(y.getBlockState(p(2, 3, 4)).is(Blocks.ICE) && y.getBlockState(p(4, 3, 4)).is(Blocks.ICE),
                "100'de göl donmuş olmalı");
        isle(y, 100, false, 40, false, KisKaynagi.YOK, e);
        y.assertTrue(kar(y, 1, 4, 1) == 0 && y.getBlockState(p(1, 3, 1)).is(Blocks.GRASS_BLOCK),
                "40'ta çimende kar ve kırağı kalkmış olmalı; " + sutunTani(y, 1, 1));
        y.assertTrue(kar(y, 8, 5, 8) == 0, "40'ta yaprakta kar kalmamalı; " + sutunTani(y, 8, 8));
        y.assertTrue(y.getBlockState(p(2, 3, 4)).is(Blocks.WATER) && y.getBlockState(p(3, 3, 4)).is(Blocks.WATER),
                "40'ta kıyı halkası açılmış olmalı; " + sutunTani(y, 2, 4) + " | " + sutunTani(y, 3, 4));
        y.assertTrue(y.getBlockState(p(4, 3, 4)).is(Blocks.ICE), "40'ta göl ortası buz kalmalı; " + sutunTani(y, 4, 4));
        y.succeed();
    }

    /**
     * B1/B3: çözülme döneminde ilk kez işlenen (seviyesi 0) chunk'ta yüzeyi Y üst sınırında olan sütun %100'e,
     * alt sınırdaki sütun G'ye gelir. Sahne: x 0..4 çimen (yüzey göreli 1), x 5..8 taş kule (yüzey göreli 4); test Y
     * geçişini sahnenin iki yüzeyine kaydırır (GameTest dünyası Y=-60'ta).
     */
    @GameTest(template = "donma_alan")
    public static void cozulmeDonemindeYuksekSutunYuzdeYuzeAlcakSutunGyeGelir(GameTestHelper y) {
        for (int x = 0; x < EN; x++) {
            for (int z = 0; z < EN; z++) {
                y.setBlock(p(x, 0, z), Blocks.STONE);
                if (x >= 5) {
                    for (int yy = 1; yy <= 4; yy++) {
                        y.setBlock(p(x, yy, z), Blocks.STONE);
                    }
                } else {
                    y.setBlock(p(x, 1, z), Blocks.GRASS_BLOCK);
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
        isle(y, 0, false, 40, true, yukseklik, DonmaEsikleri.VARSAYILAN.titresimsiz());
        for (int z = 0; z < EN; z++) {
            for (int x = 5; x < EN; x++) {
                y.assertTrue(kar(y, x, 5, z) == 5, "yüksek sütun %100 (5 kat kar) olmalı; " + sutunTani(y, x, z));
            }
            for (int x = 0; x < 5; x++) {
                y.assertTrue(y.getBlockState(p(x, 1, z)).is(kiragiliCimen()) && kar(y, x, 2, z) == 1,
                        "alçak sütun G=40 (kırağı + 1 kat kar) olmalı; " + sutunTani(y, x, z));
            }
        }
        y.succeed();
    }

    /** B4: oyuncunun kuru çamuru, 3 katlı karı ve su üstündeki buzu 0 → 100 → 40 → 0 boyunca değişmez. */
    @GameTest(template = "donma_alan")
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

    /** Soğuk biyom benzeri sahne: gölün üstü doğal buz, çimende doğal kar. Donma başlarken ve taban %40'a çözülürken kalırlar. */
    @GameTest(template = "donma_alan")
    public static void donmaBaslarkenDogalKarVeBuzKorunur(GameTestHelper y) {
        sahneKur(y);
        for (int x = 2; x <= 6; x++) {
            for (int z = 2; z <= 6; z++) {
                y.setBlock(p(x, 3, z), Blocks.ICE);
            }
        }
        y.setBlock(p(1, 4, 1), Blocks.SNOW);
        int onceki = 0;
        for (int seviye = 5; seviye <= 20; seviye += 5) {
            isle(y, onceki, seviye);
            onceki = seviye;
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
                y.assertTrue(y.getBlockState(p(x, 3, z)).is(Blocks.ICE), ne + ": doğal göl buzu eridi; " + sutunTani(y, x, z));
            }
        }
        y.assertTrue(kar(y, 1, 4, 1) == 1, ne + ": doğal kar değişti; " + sutunTani(y, 1, 1));
        BlockState cimen = y.getBlockState(p(1, 3, 1));
        y.assertTrue(cimen.is(Blocks.GRASS_BLOCK) && cimen.getValue(SnowyDirtBlock.SNOWY), ne + ": karlı çimen değişti");
    }

    @GameTest(template = "donma_alan")
    public static void blokHasariKapaliykenBlokDegismez(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);
        ServerLevel level = y.getLevel();
        GameRules.BooleanValue kural = level.getGameRules().getRule(Kurallar.AFET_BLOK_HASARI);
        boolean eski = kural.get();
        int degisen;
        try {
            kural.set(false, level.getServer());
            degisen = isle(y, 0, 100);
        } finally {
            kural.set(eski, level.getServer());
        }
        y.assertTrue(degisen == 0, "afetBlokHasari=false iken " + degisen + " blok değişti");
        ayniMi(y, baslangic, "afetBlokHasari=false iken bloklar aynı kalmalı");
        y.succeed();
    }

    @GameTest(template = "bos")
    public static void tabloVeriPaketindenYuklenir(GameTestHelper y) {
        DonmaTablosuYukleyici.Tablo tablo = DonmaTablosuYukleyici.etkin();
        y.assertTrue(tablo.suKaynak() == Blocks.WATER && tablo.suHedef() == Blocks.ICE, "su sütunu çifti yüklenmedi");
        y.assertTrue(tablo.yuzey().durum(Blocks.GRASS_BLOCK, 10) == kiragiliCimen(), "çimen → kırağılı çimen yüklenmedi");
        y.assertTrue(tablo.yuzey().durum(kiragiliCimen(), 45, -1) == Blocks.GRASS_BLOCK, "kırağı 50'nin altında kalkmalı");
        y.assertTrue(tablo.yuzey().durum(kiragiliCimen(), 55, -1) == kiragiliCimen(), "kırağı 50'nin üstünde kalmalı");
        for (DonusumTablosu.Donusum<Block> d : tablo.yuzey().donusumler()) {
            y.assertTrue(d.cozulmeEsik() > DonmaEsikleri.VARSAYILAN.buzCozulmeTabani(),
                    "yüzey çözülmesi kıyı buzundan önce olmalı: " + d);
        }
        y.assertTrue(!tablo.yuzey().taniyor(Blocks.PACKED_MUD) && !tablo.yuzey().taniyor(Blocks.MUD)
                && !tablo.yuzey().taniyor(Blocks.COBBLESTONE), "yalnız kendi hedeflerimiz tanınmalı");
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
    public static void chunkDurumuCapabilityIleNbtyeYazilir(GameTestHelper y) {
        LevelChunk chunk = y.getLevel().getChunkAt(y.absolutePos(BlockPos.ZERO));
        y.assertTrue(ChunkDonmaSeviyesi.al(chunk) != null, "üst dünya chunk'ında capability yok");
        ChunkDonmaSeviyesi.Saglayici s = new ChunkDonmaSeviyesi.Saglayici();
        s.veri.durum(42, true);
        s.veri.kar(17, true);
        s.veri.buz(255, true);
        Tag nbt = s.serializeNBT();
        ChunkDonmaSeviyesi.Saglayici yeni = new ChunkDonmaSeviyesi.Saglayici();
        yeni.deserializeNBT(nbt);
        y.assertTrue(yeni.veri.sonG() == 42 && yeni.veri.cozulme(), "NBT gidiş-dönüşü G ve dönemi korumadı");
        y.assertTrue(yeni.veri.kar(17) && !yeni.veri.kar(16) && yeni.veri.buz(255) && !yeni.veri.buz(0),
                "NBT gidiş-dönüşü bitleri korumadı");
        ChunkDonmaSeviyesi.Saglayici eski = new ChunkDonmaSeviyesi.Saglayici();
        eski.deserializeNBT(ByteTag.valueOf((byte) 60));
        y.assertTrue(eski.veri.sonG() == 60 && !eski.veri.cozulme(), "eski bayt biçimi okunamadı");
        y.assertTrue(chunk.writeCapsToNBT().contains(ChunkDonmaSeviyesi.KIMLIK.toString()),
                "chunk ForgeCaps NBT'sinde " + ChunkDonmaSeviyesi.KIMLIK + " yok");
        y.succeed();
    }
}
