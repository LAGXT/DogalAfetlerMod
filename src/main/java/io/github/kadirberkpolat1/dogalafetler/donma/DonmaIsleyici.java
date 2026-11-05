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
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Donma tablosunu dünyaya uygular. Sütunun hedef durumu kök blokların ve etkin seviyenin fonksiyonudur; geçiş yönlüdür:
 * seviye artarken buz, kar ve zincir yalnız ileri, azalırken yalnız geri gider ({@link DonmaHedefi#yonlu}). Böylece donma
 * başlarken soğuk biyomların doğal karı ve buzu kaldırılmaz, çöz(0) yine kök duruma döner; yarım kalan geçiş aynı
 * yönde yeniden uygulanınca düzelir.
 *
 * <p>Sütun kuralları (en üst hareket engelleyen blok, {@code MOTION_BLOCKING}, üzerinden):
 * <ul>
 * <li>Üst blok su kaynağı ya da buzsa {@link SuSutunu} planı: yukarıdan aşağı buz, kıyıda önce ve kalın.</li>
 * <li>Üst blok yüzey tablosundaysa kökünden seviyedeki durumuna çevrilir.</li>
 * <li>Kök {@code #dogalafetler:donma/kar_zemini} etiketindeyse ve üstü hava ya da kar katmanıysa kar katmanı sayısı
 * ayarlanır; {@code snowy} özelliği olan zeminde kırağı ({@code snowy=true}).</li>
 * <li>Tanınmayan bloğa ve üstünde bitki olan zemine dokunulmaz.</li>
 * </ul>
 * Bloklar {@link Block#UPDATE_CLIENTS} ile konur: komşu fizik güncellemesi yok, şekil güncellemesi var (çimen karı
 * vanilla kuralıyla görür).
 */
public final class DonmaIsleyici {
    public static final TagKey<Block> KAR_ZEMINI = BlockTags.create(DogalAfetler.konum("donma/kar_zemini"));
    private static final int BAYRAK = Block.UPDATE_CLIENTS;

    private DonmaIsleyici() {
    }

    /** Sütunun etkin seviyesi: chunk seviyesi + sabit titreşim; çözülme döneminde Y eşiğinin üstü daima 100. */
    static int etkinSeviye(long seed, int x, int z, int yuzeyY, int seviye, boolean cozulme, DonmaEsikleri e) {
        int taban = cozulme && yuzeyY > DonmaAyarlari.kisYEsigi ? 100 : seviye;
        return taban + DonmaEsikleri.titresim(seed, x, z, e.titresimGenligi());
    }

    /** Sınırsız üst Y: yüzey doğrudan {@code MOTION_BLOCKING} heightmap'inden okunur (dünya işleyicisi). */
    static final int SINIRSIZ = Integer.MAX_VALUE;

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
     * Bölgedeki her sütunu tek seferde {@code onceki}'den {@code seviye}'ye getirir (GameTest ve deney komutları); yüzey
     * {@code ustY}'nin altında aranır. {@code afetBlokHasari} kapalıyken hiçbir blok değişmez. Bölgenin chunk'ları yüklü olmalı.
     */
    public static int bolgeyiIsle(ServerLevel level, int minX, int minZ, int maxX, int maxZ, int ustY, int onceki,
            int seviye) {
        if (!Kurallar.blokHasariAcik(level.getGameRules())) {
            return 0;
        }
        Tablo tablo = DonmaTablosuYukleyici.etkin();
        DonmaEsikleri e = DonmaAyarlari.esikler;
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
                degisen += sutunuIsle(level, chunk, x, z, ustY, onceki, seviye, false, kiyi[dz * genislik + dx], tablo, e,
                        konum);
            }
        }
        return degisen;
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

    /** Bir sütunu chunk seviyesi {@code onceki}'den {@code seviye}'ye götürür; değişen blok sayısını döndürür. */
    static int sutunuIsle(ServerLevel level, LevelChunk chunk, int x, int z, int ustY, int onceki, int seviye,
            boolean cozulme, int kiyi, Tablo tablo, DonmaEsikleri e, BlockPos.MutableBlockPos konum) {
        int y = yuzey(chunk, x, z, ustY, konum);
        if (y < level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) {
            return 0;
        }
        long seed = level.getSeed();
        int etkin = etkinSeviye(seed, x, z, y, seviye, cozulme, e);
        int yon = Integer.signum(etkin - etkinSeviye(seed, x, z, y, onceki, cozulme, e));
        if (yon == 0) {
            return 0;
        }
        BlockState ust = chunk.getBlockState(konum.set(x, y, z));
        if (tablo.suKaynak() != null && (suMu(ust, tablo) || ust.is(tablo.suHedef()))) {
            return suSutunu(level, chunk, x, y, z, etkin, yon, kiyi, tablo, e, konum);
        }
        int degisen = 0;
        Block kok = tablo.yuzey().kok(ust.getBlock());
        if (kok != ust.getBlock() || tablo.yuzey().taniyor(kok)) {
            Block istenen = tablo.yuzey().durum(ust.getBlock(), etkin, yon);
            if (istenen != ust.getBlock()) {
                ust = istenen.defaultBlockState();
                level.setBlock(konum.set(x, y, z), ust, BAYRAK);
                degisen++;
            }
        }
        if (!kok.defaultBlockState().is(KAR_ZEMINI)) {
            return degisen;
        }
        BlockPos yukari = new BlockPos(x, y + 1, z);
        BlockState ustundeki = chunk.getBlockState(yukari);
        if (!ustundeki.isAir() && !ustundeki.is(Blocks.SNOW)) {
            return degisen;
        }
        int mevcutKatman = ustundeki.is(Blocks.SNOW) ? ustundeki.getValue(SnowLayerBlock.LAYERS) : 0;
        int katman = DonmaHedefi.yonlu(mevcutKatman, e.karKatmani(etkin, kok instanceof SpreadingSnowyDirtBlock), yon);
        if (katman != mevcutKatman) {
            BlockState istenenUst = katman == 0 ? Blocks.AIR.defaultBlockState()
                    : Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, katman);
            if (katman == 0 || istenenUst.canSurvive(level, yukari)) {
                level.setBlock(yukari, istenenUst, BAYRAK);
                degisen++;
            } else {
                katman = mevcutKatman;
            }
        }
        BlockState zemin = chunk.getBlockState(konum.set(x, y, z));
        if (zemin.hasProperty(SnowyDirtBlock.SNOWY)) {
            boolean mevcutKarli = zemin.getValue(SnowyDirtBlock.SNOWY);
            boolean istenenKarli = katman > 0 || e.kiragi(etkin);
            boolean karli = yon > 0 ? mevcutKarli || istenenKarli : mevcutKarli && istenenKarli;
            if (mevcutKarli != karli) {
                level.setBlock(konum, zemin.setValue(SnowyDirtBlock.SNOWY, karli), BAYRAK);
                degisen++;
            }
        }
        return degisen;
    }

    private static int suSutunu(ServerLevel level, LevelChunk chunk, int x, int y, int z, int etkin, int yon, int kiyi,
            Tablo tablo, DonmaEsikleri e, BlockPos.MutableBlockPos konum) {
        SuSutunu.Tur[] sutun = new SuSutunu.Tur[e.maksBuzKalinligi() + 1];
        for (int d = 0; d < sutun.length; d++) {
            int yy = y - d;
            if (yy < level.getMinBuildHeight()) {
                sutun[d] = SuSutunu.Tur.DIGER;
                continue;
            }
            BlockState durum = chunk.getBlockState(konum.set(x, yy, z));
            sutun[d] = suMu(durum, tablo) ? SuSutunu.Tur.SU
                    : durum.is(tablo.suHedef()) ? SuSutunu.Tur.BUZ : SuSutunu.Tur.DIGER;
        }
        SuSutunu.Tur[] plan = SuSutunu.planla(sutun, e.buzKalinligi(etkin, kiyi), yon);
        int degisen = 0;
        for (int d = 0; d < sutun.length; d++) {
            if (plan[d] != sutun[d]) {
                BlockState yeni = plan[d] == SuSutunu.Tur.BUZ ? tablo.suHedef().defaultBlockState()
                        : tablo.suKaynak().defaultBlockState();
                level.setBlock(konum.set(x, y - d, z), yeni, BAYRAK);
                degisen++;
            }
        }
        return degisen;
    }
}
