package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaTablosuYukleyici.Tablo;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Donma tablosunu dünyaya uygular. Sütunun hedefi {@link DonmaHedefi#sutunHedefi} ile (G, dönem, sütunun kış ağırlığı,
 * ılık ofset) hesaplanır; yön eski ve yeni hedefin farkından gelir. Donarken donma eşikleri, çözülürken çözülme eşikleri
 * kullanılır ve değer yalnız o yönde değişir ({@link DonmaHedefi#yonlu}).
 *
 * <p>Sütun kuralları (en üst hareket engelleyen blok, {@code MOTION_BLOCKING}, üzerinden):
 * <ul>
 * <li>Üst blok su kaynağı ya da buzsa {@link SuSutunu} planı. Buz biti işaretli olmayan sütunun buzuna (doğal ya da
 * oyuncunun) dokunulmaz.</li>
 * <li>Zeminin üstü hava ya da bizim karımız değilse (bitki, oyuncunun karı, başka blok) sütuna dokunulmaz.</li>
 * <li>Üst blok yüzey tablosundaysa zincirde yönlü ilerler; aynı adlı blok durumu özellikleri korunur.</li>
 * <li>Kök {@code #dogalafetler:donma/kar_zemini} etiketindeyse kar katmanı ayarlanır ve kar biti tutulur. Zeminin
 * {@code snowy} özelliğini vanilla şekil güncellemesi ayarlar.</li>
 * </ul>
 * Bloklar {@link Block#UPDATE_CLIENTS} ile konur: komşu fizik güncellemesi yok, şekil güncellemesi var.
 */
public final class DonmaIsleyici {
    public static final TagKey<Block> KAR_ZEMINI = BlockTags.create(DogalAfetler.konum("donma/kar_zemini"));
    private static final int BAYRAK = Block.UPDATE_CLIENTS;
    /** Sınırsız üst Y: yüzey doğrudan {@code MOTION_BLOCKING} heightmap'inden okunur (dünya işleyicisi). */
    static final int SINIRSIZ = Integer.MAX_VALUE;

    private DonmaIsleyici() {
    }

    /**
     * {@code ustY}'nin altındaki en üst {@code MOTION_BLOCKING} bloğu. GameTest şablonları normal arazili dünyada yer
     * altında durur; heightmap orada sahnenin üstündeki araziyi gösterir, bu yüzden test bölgenin tepesini verir.
     */
    static int yuzey(LevelChunk chunk, int x, int z, int ustY, BlockPos.MutableBlockPos konum) {
        int h = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING, x & 15, z & 15);
        if (ustY >= h) {
            return h;
        }
        int min = chunk.getMinBuildHeight();
        for (int y = ustY; y >= min; y--) {
            if (Heightmap.Types.MOTION_BLOCKING.isOpaque().test(chunk.getBlockState(konum.set(x, y, z)))) {
                return y;
            }
        }
        return min - 1;
    }

    /**
     * Bölgedeki her sütunu tek seferde ({@code oncekiG}, {@code oncekiCozulme}) durumundan ({@code g}, {@code cozulme})
     * durumuna getirir (GameTest ve deney yolu); yüzey {@code ustY}'nin altında aranır. {@code afetBlokHasari} kapalıyken
     * hiçbir blok değişmez. Bölgenin chunk'ları yüklü olmalı.
     */
    public static int bolgeyiIsle(ServerLevel level, int minX, int minZ, int maxX, int maxZ, int ustY, int oncekiG,
            boolean oncekiCozulme, int g, boolean cozulme, KisKaynagi kis, DonmaEsikleri e) {
        if (!Kurallar.blokHasariAcik(level.getGameRules())) {
            return 0;
        }
        Tablo tablo = DonmaTablosuYukleyici.etkin();
        int genislik = maxX - minX + 1;
        int derinlik = maxZ - minZ + 1;
        int[] kiyi = kiyiUzakliklari(level, minX, minZ, genislik, derinlik, ustY, tablo, e);
        BlockPos.MutableBlockPos konum = new BlockPos.MutableBlockPos();
        int degisen = 0;
        for (int dz = 0; dz < derinlik; dz++) {
            for (int dx = 0; dx < genislik; dx++) {
                int x = minX + dx;
                int z = minZ + dz;
                LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                degisen += sutunuIsle(level, chunk, durumu(chunk), x, z, ustY, oncekiG, oncekiCozulme, g, cozulme, kis,
                        kiyi[dz * genislik + dx], tablo, e, konum);
            }
        }
        return degisen;
    }

    /** Bölgenin "bizim koyduğumuz" kar ve buz bitlerini siler (GameTest sahnesi kurulurken). */
    public static void bitleriTemizle(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                ChunkDonmaSeviyesi c = durumu(level.getChunk(x >> 4, z >> 4));
                int i = ChunkDonmaSeviyesi.indeks(x, z);
                c.kar(i, false);
                c.buz(i, false);
            }
        }
    }

    private static ChunkDonmaSeviyesi durumu(LevelChunk chunk) {
        ChunkDonmaSeviyesi c = ChunkDonmaSeviyesi.al(chunk);
        return c != null ? c : new ChunkDonmaSeviyesi();
    }

    /** Bölge ve 1 blokluk halkasının yüzey türlerinden kıyı uzaklıkları; yüklü olmayan komşu chunk bilinmiyor sayılır. */
    static int[] kiyiUzakliklari(ServerLevel level, int minX, int minZ, int genislik, int derinlik, int ustY, Tablo tablo,
            DonmaEsikleri e) {
        int gw = genislik + 2;
        byte[] tur = new byte[gw * (derinlik + 2)];
        BlockPos.MutableBlockPos konum = new BlockPos.MutableBlockPos();
        for (int dz = -1; dz <= derinlik; dz++) {
            for (int dx = -1; dx <= genislik; dx++) {
                tur[(dz + 1) * gw + dx + 1] = yuzeyTuru(level, minX + dx, minZ + dz, ustY, tablo, konum);
            }
        }
        return KiyiUzakligi.hesapla(genislik, derinlik, tur, e.kiyiUzaklikSiniri());
    }

    private static byte yuzeyTuru(ServerLevel level, int x, int z, int ustY, Tablo tablo, BlockPos.MutableBlockPos konum) {
        LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        if (chunk == null) {
            return KiyiUzakligi.BILINMIYOR;
        }
        if (tablo.suKaynak() == null) {
            return KiyiUzakligi.KARA;
        }
        int y = yuzey(chunk, x, z, ustY, konum);
        BlockState ust = chunk.getBlockState(konum.set(x, y, z));
        return suMu(ust, tablo) || ust.is(tablo.suHedef()) ? KiyiUzakligi.SU : KiyiUzakligi.KARA;
    }

    private static boolean suMu(BlockState durum, Tablo tablo) {
        return durum.is(tablo.suKaynak()) && durum.getFluidState().isSource();
    }

    /** Sütunu ({@code oncekiG}, {@code oncekiCozulme}) hedefinden ({@code g}, {@code cozulme}) hedefine götürür. */
    static int sutunuIsle(ServerLevel level, LevelChunk chunk, ChunkDonmaSeviyesi durum, int x, int z, int ustY,
            int oncekiG, boolean oncekiCozulme, int g, boolean cozulme, KisKaynagi kis, int kiyi, Tablo tablo,
            DonmaEsikleri e, BlockPos.MutableBlockPos konum) {
        int y = yuzey(chunk, x, z, ustY, konum);
        if (y < level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) {
            return 0;
        }
        double w = kis.agirlik(x, z, y);
        double ofset = kis.ofset(x, z);
        int eski = DonmaHedefi.sutunHedefi(oncekiG, oncekiCozulme, w, ofset);
        int yeni = DonmaHedefi.sutunHedefi(g, cozulme, w, ofset);
        int yon = Integer.signum(yeni - eski);
        if (yon == 0) {
            return 0;
        }
        int etkin = yeni + DonmaEsikleri.titresim(level.getSeed(), x, z, e.titresimGenligi());
        int i = ChunkDonmaSeviyesi.indeks(x, z);
        BlockState ust = chunk.getBlockState(konum.set(x, y, z));
        if (tablo.suKaynak() != null && (suMu(ust, tablo) || ust.is(tablo.suHedef()))) {
            return suSutunu(level, chunk, durum, i, x, y, z, etkin, yon, kiyi, tablo, e, konum);
        }
        BlockPos yukari = new BlockPos(x, y + 1, z);
        BlockState ustundeki = chunk.getBlockState(yukari);
        boolean bizimKar = ustundeki.is(Blocks.SNOW) && durum.kar(i);
        if (!ustundeki.isAir() && !bizimKar) {
            return 0;
        }
        int degisen = 0;
        Block kok = tablo.yuzey().kok(ust.getBlock());
        if (tablo.yuzey().taniyor(ust.getBlock())) {
            Block istenen = tablo.yuzey().durum(ust.getBlock(), etkin, yon);
            if (istenen != ust.getBlock()) {
                level.setBlock(konum.set(x, y, z), ozellikleriKopyala(ust, istenen), BAYRAK);
                degisen++;
            }
        }
        if (!kok.defaultBlockState().is(KAR_ZEMINI)) {
            return degisen;
        }
        int mevcut = bizimKar ? ustundeki.getValue(SnowLayerBlock.LAYERS) : 0;
        boolean tekKat = kok instanceof SpreadingSnowyDirtBlock;
        int istenenKatman = yon > 0 ? e.karKatmani(etkin, tekKat) : e.karKatmaniCozulme(etkin, tekKat);
        int katman = DonmaHedefi.yonlu(mevcut, istenenKatman, yon);
        if (katman != mevcut) {
            BlockState yeniUst = katman == 0 ? Blocks.AIR.defaultBlockState()
                    : Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, katman);
            if (katman == 0 || yeniUst.canSurvive(level, yukari)) {
                level.setBlock(yukari, yeniUst, BAYRAK);
                durum.kar(i, katman > 0);
                degisen++;
            }
        }
        return degisen;
    }

    private static int suSutunu(ServerLevel level, LevelChunk chunk, ChunkDonmaSeviyesi durum, int i, int x, int y, int z,
            int etkin, int yon, int kiyi, Tablo tablo, DonmaEsikleri e, BlockPos.MutableBlockPos konum) {
        boolean bizim = durum.buz(i);
        if (!bizim && (yon < 0 || chunk.getBlockState(konum.set(x, y, z)).is(tablo.suHedef()))) {
            return 0;
        }
        SuSutunu.Tur[] sutun = new SuSutunu.Tur[e.maksBuzKalinligi() + 1];
        for (int d = 0; d < sutun.length; d++) {
            int yy = y - d;
            if (yy < level.getMinBuildHeight()) {
                sutun[d] = SuSutunu.Tur.DIGER;
                continue;
            }
            BlockState s = chunk.getBlockState(konum.set(x, yy, z));
            sutun[d] = suMu(s, tablo) ? SuSutunu.Tur.SU : s.is(tablo.suHedef()) ? SuSutunu.Tur.BUZ : SuSutunu.Tur.DIGER;
        }
        int kalinlik = yon > 0 ? e.buzKalinligi(etkin, kiyi) : e.buzKalinligiCozulme(etkin, kiyi);
        SuSutunu.Tur[] plan = SuSutunu.planla(sutun, kalinlik, yon);
        int degisen = 0;
        for (int d = 0; d < sutun.length; d++) {
            if (plan[d] != sutun[d]) {
                BlockState yeni = plan[d] == SuSutunu.Tur.BUZ ? tablo.suHedef().defaultBlockState()
                        : tablo.suKaynak().defaultBlockState();
                level.setBlock(konum.set(x, y - d, z), yeni, BAYRAK);
                degisen++;
            }
        }
        durum.buz(i, plan[0] == SuSutunu.Tur.BUZ);
        return degisen;
    }

    /** Hedef bloka kaynak durumun aynı adlı özelliklerini taşır (ör. {@code snowy}); diğerleri varsayılan. */
    static BlockState ozellikleriKopyala(BlockState kaynak, Block hedef) {
        BlockState s = hedef.defaultBlockState();
        for (Property<?> p : kaynak.getProperties()) {
            if (s.hasProperty(p)) {
                s = kopyala(s, kaynak, p);
            }
        }
        return s;
    }

    private static <T extends Comparable<T>> BlockState kopyala(BlockState hedef, BlockState kaynak, Property<T> p) {
        return hedef.setValue(p, kaynak.getValue(p));
    }
}
