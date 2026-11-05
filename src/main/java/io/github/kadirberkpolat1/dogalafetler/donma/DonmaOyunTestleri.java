package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ByteTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
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
 * değişmez), veri paketinden yüklenen tablo ve chunk capability'si.
 *
 * <p>Sahne (göreli, 9x6x9): y=0 taş; y=1..2 toprak; y=3 çimen; ortada 5x5, 3 derin göl (x,z 2..6, y 1..3); (0,3,0)
 * çamur; (8,3,0) kırık taş (tanınmayan); (8,4,8) kalıcı meşe yaprağı; (0,4,8) kısa ot (altındaki çimene dokunulmaz).
 */
@GameTestHolder(DogalAfetler.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DonmaOyunTestleri {
    private static final int EN = 9;
    private static final int BOY = 6;

    private DonmaOyunTestleri() {
    }

    /** Şablon yapı bloğunun bir üstüne yerleşir ({@code StructureBlockEntity.structurePos} = (0, 1, 0)); göreli y=0 yapı bloğudur. */
    private static BlockPos p(int x, int yy, int z) {
        return new BlockPos(x, yy + 1, z);
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
        y.setBlock(p(0, 3, 0), Blocks.MUD);
        y.setBlock(p(8, 3, 0), Blocks.COBBLESTONE);
        y.setBlock(p(8, 4, 8), Blocks.OAK_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true));
        y.setBlock(p(0, 4, 8), Blocks.GRASS);
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

    private static int isle(GameTestHelper y, int seviye) {
        BlockPos a = y.absolutePos(BlockPos.ZERO);
        BlockPos b = y.absolutePos(new BlockPos(EN - 1, 0, EN - 1));
        int ustY = y.absolutePos(p(0, BOY - 1, 0)).getY();
        return DonmaIsleyici.bolgeyiIsle(y.getLevel(), Math.min(a.getX(), b.getX()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getZ(), b.getZ()), ustY, seviye);
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

    private static String genelTani(GameTestHelper y) {
        DonmaTablosuYukleyici.Tablo t = DonmaTablosuYukleyici.etkin();
        return "köken " + y.absolutePos(BlockPos.ZERO) + " afetBlokHasari="
                + Kurallar.blokHasariAcik(y.getLevel().getGameRules()) + " su=" + t.suKaynak() + "/" + t.suHedef()
                + " yüzey=" + t.yuzey().boyut() + " | " + sutunTani(y, 4, 4) + " | " + sutunTani(y, 1, 1);
    }

    @GameTest(template = "donma_alan")
    public static void dondurSonraCozBaslangicDurumunuVerir(GameTestHelper y) {
        sahneKur(y);
        BlockState[] baslangic = anlik(y);

        int donan = isle(y, 100);
        y.assertTrue(donan > 0, "dondur(100) hiç blok değiştirmedi; " + genelTani(y));
        y.assertTrue(y.getBlockState(p(4, 3, 4)).is(Blocks.ICE), "göl yüzeyi buz olmalı; " + sutunTani(y, 4, 4)
                + " | " + sutunTani(y, 2, 4));
        y.assertTrue(y.getBlockState(p(4, 2, 4)).is(Blocks.ICE), "buz 2 blok kalın olmalı");
        y.assertTrue(y.getBlockState(p(4, 1, 4)).is(Blocks.WATER), "gölün en altı su kalmalı");
        BlockState kar = y.getBlockState(p(1, 4, 1));
        y.assertTrue(kar.is(Blocks.SNOW) && kar.getValue(SnowLayerBlock.LAYERS) == 1, "çimende tek kat kar: " + kar);
        y.assertTrue(y.getBlockState(p(1, 3, 1)).getValue(SnowyDirtBlock.SNOWY), "çimen karlı olmalı");
        y.assertTrue(y.getBlockState(p(0, 3, 0)).is(Blocks.PACKED_MUD), "çamur JSON tablosuyla donmalı");
        y.assertTrue(y.getBlockState(p(0, 4, 0)).is(Blocks.SNOW), "donmuş çamurun üstünde kar olmalı");
        y.assertTrue(y.getBlockState(p(8, 3, 0)).is(Blocks.COBBLESTONE)
                && y.getBlockState(p(8, 4, 0)).isAir(), "tanınmayan bloğa dokunulmamalı");
        BlockState yaprakKari = y.getBlockState(p(8, 5, 8));
        y.assertTrue(yaprakKari.is(Blocks.SNOW) && yaprakKari.getValue(SnowLayerBlock.LAYERS) >= 4,
                "yaprağın üstünde kalın kar olmalı: " + yaprakKari);
        y.assertTrue(y.getBlockState(p(0, 4, 8)).is(Blocks.GRASS)
                && !y.getBlockState(p(0, 3, 8)).getValue(SnowyDirtBlock.SNOWY), "bitkili zemine dokunulmamalı");
        BlockState[] donuk = anlik(y);

        isle(y, 30);
        isle(y, 100);
        ayniMi(y, donuk, "100 → 30 → 100 doğrudan 100 ile aynı olmalı");

        int cozulen = isle(y, 0);
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
            isle(y, seviye);
        }
        y.assertTrue(y.getBlockState(p(2, 3, 4)).is(Blocks.ICE), "60'ta kıyı sütunu donmuş olmalı; " + sutunTani(y, 2, 4));
        y.assertTrue(y.getBlockState(p(1, 4, 1)).is(Blocks.SNOW), "60'ta çimende kar olmalı");
        for (int seviye = 55; seviye >= 0; seviye -= 5) {
            isle(y, seviye);
        }
        ayniMi(y, baslangic, "5'lik adımlarla 60'a çıkıp 0'a inmek başlangıcı vermeli");
        y.succeed();
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
            degisen = isle(y, 100);
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
        y.assertTrue(tablo.yuzey().durum(Blocks.MUD, 60) == Blocks.PACKED_MUD, "çamur → kuru çamur yüklenmedi");
        y.assertTrue(tablo.yuzey().durum(Blocks.PACKED_MUD, 0) == Blocks.MUD, "ters yön çalışmıyor");
        y.assertTrue(!tablo.yuzey().taniyor(Blocks.COBBLESTONE), "kırık taş tanınmamalı");
        y.succeed();
    }

    @GameTest(template = "bos")
    public static void chunkSeviyesiCapabilityIleNbtyeYazilir(GameTestHelper y) {
        LevelChunk chunk = y.getLevel().getChunkAt(y.absolutePos(BlockPos.ZERO));
        y.assertTrue(chunk.getCapability(ChunkDonmaSeviyesi.YETENEK).isPresent(), "üst dünya chunk'ında capability yok");
        int eski = ChunkDonmaSeviyesi.oku(chunk);
        try {
            ChunkDonmaSeviyesi.yaz(chunk, 42);
            y.assertTrue(chunk.isUnsaved(), "seviye yazılınca chunk kaydedilecek olarak işaretlenmeli");
            ChunkDonmaSeviyesi.Saglayici saglayici = new ChunkDonmaSeviyesi.Saglayici();
            saglayici.veri.seviye(42);
            ByteTag nbt = saglayici.serializeNBT();
            ChunkDonmaSeviyesi.Saglayici yeni = new ChunkDonmaSeviyesi.Saglayici();
            yeni.deserializeNBT(nbt);
            y.assertTrue(yeni.veri.seviye() == 42, "NBT gidiş-dönüşü seviyeyi korumadı");
            y.assertTrue(chunk.writeCapsToNBT().contains(ChunkDonmaSeviyesi.KIMLIK.toString()),
                    "chunk ForgeCaps NBT'sinde " + ChunkDonmaSeviyesi.KIMLIK + " yok");
        } finally {
            ChunkDonmaSeviyesi.yaz(chunk, eski);
        }
        y.succeed();
    }
}
