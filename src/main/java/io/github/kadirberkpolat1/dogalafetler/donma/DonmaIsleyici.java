package io.github.kadirberkpolat1.dogalafetler.donma;

import io.github.kadirberkpolat1.dogalafetler.DogalAfetler;
import io.github.kadirberkpolat1.dogalafetler.donma.DonmaTablosuYukleyici.Tablo;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.BuharlasmaHedefi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaEsikleri;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.DonmaHedefi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.EsikKumesi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KisKaynagi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.KiyiUzakligi;
import io.github.kadirberkpolat1.dogalafetler.donma.mantik.SuSutunu;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SpreadingSnowyDirtBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Donma tablosunu ve buharlaşmayı dünyaya uygular (madde 9, N9, N10). Sütun başına önce buharlaşma (kuru kaydı hedef
 * derinliğe getirilir: kurur ya da süpernovadan sonra geri dolar), sonra donma: sütunun hedefi {@link DonmaHedefi#sutunHedefi}
 * ile (G, dönem, kış ağırlığı, ılık ofset) hesaplanır; yön eski ve yeni hedefin farkından gelir; donarken donma, çözülürken
 * çözülme eşikleri geçerli ve değer yalnız o yönde değişir. Etkin seviye aralığında hiçbir eşik yoksa sütunun blokları
 * okunmaz ({@link EsikKumesi}).
 *
 * <p>Sütun kuralları (en üst hareket engelleyen blok, {@code MOTION_BLOCKING}, üzerinden):
 * <ul>
 * <li>Üst blok su kaynağı, buz ya da ince buzsa {@link SuSutunu} planı; buz biti işaretli olmayan sütunun buzuna (doğal
 * ya da oyuncunun) dokunulmaz.</li>
 * <li>Zeminin üstü hava ya da bizim karımız değilse (bitki, oyuncunun karı, başka blok) sütuna dokunulmaz.</li>
 * <li>Üst blok yüzey tablosundaysa zincirde yönlü ilerler; aynı adlı blok durumu özellikleri korunur.</li>
 * <li>Kök {@code #dogalafetler:donma/kar_zemini} etiketindeyse kar katmanı ayarlanır ve kar biti tutulur.</li>
 * <li>Sütun sanal suyun altındaysa ({@link DisKancalar#sanalSuVarMi}) donmaz.</li>
 * </ul>
 * Donma blokları {@link Block#UPDATE_CLIENTS} ile konur (şekil güncellemesi var, komşu fizik yok); buharlaşma ve geri dolma
 * ayrıca şekil güncellemesi göndermez ki komşu su akarak deliği doldurmasın.
 */
public final class DonmaIsleyici {
    public static final TagKey<Block> KAR_ZEMINI = BlockTags.create(DogalAfetler.konum("donma/kar_zemini"));
    /** Yapı blokları (çatı, duvar üstü): kar daha geç gelir ({@code yapiKarEsigi}, D8). */
    public static final TagKey<Block> YAPI_KAR_ZEMINI = BlockTags.create(DogalAfetler.konum("donma/yapi_kar_zemini"));
    private static final int BAYRAK = Block.UPDATE_CLIENTS;
    private static final int SU_BAYRAGI = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    /** Sınırsız üst Y: yüzey doğrudan {@code MOTION_BLOCKING} heightmap'inden okunur. */
    static final int SINIRSIZ = Integer.MAX_VALUE;

    /**
     * Bir chunk geçişi: {@code (eskiG, eskiCozulme) → (yeniG, yeniCozulme)}; {@code buhar}: gerçek suya uygulanan buharlaşma
     * derinliği (blok), {@code buharIsle} false ise buharlaşma adımı atlanır; {@code atlamaSerbest}: eşik aralığı atlaması
     * güvenli mi (yarım adımdan devam eden chunk'ta değil).
     */
    public record Gecis(int eskiG, boolean eskiCozulme, int yeniG, boolean yeniCozulme, int buhar, boolean buharIsle,
            boolean atlamaSerbest) {
        boolean donmaVar() {
            return eskiG != yeniG || eskiCozulme != yeniCozulme;
        }
    }

    /** Sütun için kıyı uzaklığı (zamanlayıcıda tembel hesaplanır). */
    interface Kiyi {
        int uzaklik(int x, int z);
    }

    /** Bir sütunun ya da bölgenin sonucu. */
    static final class Sayac {
        int degisen;
        boolean suDegisti;
    }

    private DonmaIsleyici() {
    }

    /**
     * {@code ustY}'nin altındaki en üst {@code MOTION_BLOCKING} bloğu (GameTest sahnesinin üstünde arazi olabilir); sınırsızsa
     * doğrudan heightmap.
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
     * Bölgedeki her sütunu tek seferde geçirir (GameTest yolu); yüzey {@code ustY}'nin altında aranır. {@code afetBlokHasari}
     * kapalıyken hiçbir blok değişmez. Bölgenin chunk'ları yüklü olmalı. Değişen blok sayısını döner.
     */
    public static int bolgeyiIsle(ServerLevel level, int minX, int minZ, int maxX, int maxZ, int ustY, Gecis gecis,
            KisKaynagi kis, DonmaEsikleri e) {
        if (!Kurallar.blokHasariAcik(level.getGameRules())) {
            return 0;
        }
        Tablo tablo = DonmaTablosuYukleyici.etkin();
        EsikKumesi esikler = EsikKumesi.kur(e, tablo.esikDonma(), tablo.esikCozulme());
        int genislik = maxX - minX + 1;
        int derinlik = maxZ - minZ + 1;
        int[] kiyi = kiyiUzakliklari(level, minX, minZ, genislik, derinlik, ustY, tablo, e);
        Kiyi k = (x, z) -> kiyi[(z - minZ) * genislik + (x - minX)];
        BlockPos.MutableBlockPos konum = new BlockPos.MutableBlockPos();
        Sayac sayac = new Sayac();
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
                sutunuIsle(level, chunk, durumu(chunk), x, z, ustY, gecis, kis, k, tablo, e, esikler, konum, sayac);
            }
        }
        return sayac.degisen;
    }

    /** Bölgenin "bizim koyduğumuz" kar, buz ve kuru kayıtlarını siler (GameTest sahnesi kurulurken). */
    public static void bitleriTemizle(ServerLevel level, int minX, int minZ, int maxX, int maxZ) {
        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                ChunkDonmaVerisi c = durumu(level.getChunk(x >> 4, z >> 4));
                int i = ChunkDonmaVerisi.indeks(x, z);
                c.kar(i, false);
                c.buz(i, false);
                c.kuru(i, 0, 0);
            }
        }
    }

    static ChunkDonmaVerisi durumu(LevelChunk chunk) {
        ChunkDonmaVerisi c = ChunkDonmaVerisi.al(chunk);
        return c != null ? c : new ChunkDonmaVerisi();
    }

    /**
     * Bölgenin su sütunları için kıyı uzaklığı (K-D20). BFS bölgenin çevresinde {@code kiyiUzaklikSiniri} blokluk hale üzerinde
     * çalışır: sınıra kadarki her yol bu halenin içinde kaldığı için sonuç bölgenin chunk'a göre nereden kesildiğinden
     * bağımsızdır (komuş chunk'taki kıyı da görülür; tek blokluk halede göl buzu chunk hizalı kenarla donuyordu).
     * Yüklü olmayan chunk bilinmiyor sayılır; ticking chunk'ın komşuları yüklüdür.
     */
    static int[] kiyiUzakliklari(ServerLevel level, int minX, int minZ, int genislik, int derinlik, int ustY, Tablo tablo,
            DonmaEsikleri e) {
        int hale = e.kiyiUzaklikSiniri();
        int gg = genislik + 2 * hale;
        int gd = derinlik + 2 * hale;
        int gw = gg + 2;
        byte[] tur = new byte[gw * (gd + 2)];
        BlockPos.MutableBlockPos konum = new BlockPos.MutableBlockPos();
        int x0 = minX - hale - 1;
        int z0 = minZ - hale - 1;
        LevelChunk chunk = null;
        long chunkAnahtari = Long.MIN_VALUE;
        for (int dz = 0; dz < gd + 2; dz++) {
            for (int dx = 0; dx < gw; dx++) {
                int x = x0 + dx;
                int z = z0 + dz;
                long anahtar = ChunkPos.asLong(x >> 4, z >> 4);
                if (anahtar != chunkAnahtari) {
                    chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                    chunkAnahtari = anahtar;
                }
                tur[dz * gw + dx] = yuzeyTuru(chunk, x, z, ustY, tablo, konum);
            }
        }
        int[] genis = KiyiUzakligi.hesapla(gg, gd, tur, hale);
        int[] sonuc = new int[genislik * derinlik];
        for (int z = 0; z < derinlik; z++) {
            System.arraycopy(genis, (z + hale) * gg + hale, sonuc, z * genislik, genislik);
        }
        return sonuc;
    }

    private static byte yuzeyTuru(LevelChunk chunk, int x, int z, int ustY, Tablo tablo, BlockPos.MutableBlockPos konum) {
        if (chunk == null) {
            return KiyiUzakligi.BILINMIYOR;
        }
        if (tablo.suKaynak() == null) {
            return KiyiUzakligi.KARA;
        }
        BlockState ust = chunk.getBlockState(konum.set(x, yuzey(chunk, x, z, ustY, konum), z));
        return suMu(ust, tablo) || buzMu(ust, tablo) ? KiyiUzakligi.SU : KiyiUzakligi.KARA;
    }

    private static boolean suMu(BlockState durum, Tablo tablo) {
        return durum.is(tablo.suKaynak()) && durum.getFluidState().isSource();
    }

    private static boolean buzMu(BlockState durum, Tablo tablo) {
        return durum.is(tablo.suHedef()) || durum.is(tablo.suInce());
    }

    /**
     * Kış ağırlığının yükseklik bileşeni için zemin: yaprak sayılmadan en üst hareket engelleyen blok (D7; ağaç tepesi
     * yanındaki zeminden "daha kışlı" olmasın). Uzun süren sınırsız yolda heightmap, GameTest yolunda {@code ustY}'den tarama.
     */
    static int zeminY(LevelChunk chunk, int x, int z, int ustY, BlockPos.MutableBlockPos konum) {
        int h = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x & 15, z & 15);
        if (ustY >= h) {
            return h;
        }
        for (int y = ustY; y >= chunk.getMinBuildHeight(); y--) {
            if (Heightmap.Types.MOTION_BLOCKING_NO_LEAVES.isOpaque().test(chunk.getBlockState(konum.set(x, y, z)))) {
                return y;
            }
        }
        return chunk.getMinBuildHeight() - 1;
    }

    /** Sütunu geçirir: önce buharlaşma, sonra donma (N10 sırası). */
    static void sutunuIsle(ServerLevel level, LevelChunk chunk, ChunkDonmaVerisi durum, int x, int z, int ustY, Gecis gecis,
            KisKaynagi kis, Kiyi kiyi, Tablo tablo, DonmaEsikleri e, EsikKumesi esikler, BlockPos.MutableBlockPos konum,
            Sayac sayac) {
        int y = yuzey(chunk, x, z, ustY, konum);
        if (y < level.getMinBuildHeight() || y + 1 >= level.getMaxBuildHeight()) {
            return;
        }
        int i = ChunkDonmaVerisi.indeks(x, z);
        if (gecis.buharIsle() && tablo.suKaynak() != null && buharla(level, chunk, durum, i, x, y, z, gecis.buhar(), tablo,
                konum, sayac)) {
            y = yuzey(chunk, x, z, ustY, konum);
        }
        if (!gecis.donmaVar()) {
            return;
        }
        double w = kis.agirlik(x, z, zeminY(chunk, x, z, ustY, konum));
        double ofset = kis.ofset(x, z);
        int eski = DonmaHedefi.sutunHedefi(gecis.eskiG(), gecis.eskiCozulme(), w, ofset);
        int yeni = DonmaHedefi.sutunHedefi(gecis.yeniG(), gecis.yeniCozulme(), w, ofset);
        int yon = Integer.signum(yeni - eski);
        if (yon == 0) {
            return;
        }
        int titresim = DonmaEsikleri.titresim(level.getSeed(), x, z, e.titresimGenligi());
        if (gecis.atlamaSerbest() && !esikler.degisimVar(eski + titresim, yeni + titresim, yon < 0)) {
            return;
        }
        if (DisKancalar.al().sanalSuVarMi(level, x, y + 1, z)) {
            return;
        }
        int etkin = yeni + titresim;
        BlockState ust = chunk.getBlockState(konum.set(x, y, z));
        if (tablo.suKaynak() != null && (suMu(ust, tablo) || buzMu(ust, tablo))) {
            suSutunu(level, chunk, durum, i, x, y, z, etkin, yon, kiyi, tablo, e, konum, sayac);
            return;
        }
        BlockPos yukari = new BlockPos(x, y + 1, z);
        BlockState ustundeki = chunk.getBlockState(yukari);
        boolean bizimKar = ustundeki.is(Blocks.SNOW) && durum.kar(i);
        if (!ustundeki.isAir() && !bizimKar) {
            return;
        }
        Block kok = tablo.yuzey().kok(ust.getBlock());
        if (tablo.yuzey().taniyor(ust.getBlock())) {
            Block istenen = tablo.yuzey().durum(ust.getBlock(), etkin, yon);
            if (istenen != ust.getBlock()) {
                level.setBlock(konum.set(x, y, z), ozellikleriKopyala(ust, istenen), BAYRAK);
                sayac.degisen++;
            }
        }
        boolean yapi = !kok.defaultBlockState().is(KAR_ZEMINI);
        if (yapi && !kok.defaultBlockState().is(YAPI_KAR_ZEMINI)) {
            return;
        }
        int mevcut = bizimKar ? ustundeki.getValue(SnowLayerBlock.LAYERS) : 0;
        boolean tekKat = kok instanceof SpreadingSnowyDirtBlock;
        int istenenKatman = yapi ? e.yapiKarKatmani(etkin, yon)
                : yon > 0 ? e.karKatmani(etkin, tekKat) : e.karKatmaniCozulme(etkin, tekKat);
        int katman = DonmaHedefi.yonlu(mevcut, istenenKatman, yon);
        if (katman != mevcut) {
            BlockState yeniUst = katman == 0 ? Blocks.AIR.defaultBlockState()
                    : Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, katman);
            if (katman == 0 || yeniUst.canSurvive(level, yukari)) {
                level.setBlock(yukari, yeniUst, BAYRAK);
                durum.kar(i, katman > 0);
                sayac.degisen++;
            }
        }
    }

    private static void suSutunu(ServerLevel level, LevelChunk chunk, ChunkDonmaVerisi durum, int i, int x, int y, int z,
            int etkin, int yon, Kiyi kiyi, Tablo tablo, DonmaEsikleri e, BlockPos.MutableBlockPos konum, Sayac sayac) {
        boolean bizim = durum.buz(i);
        if (!bizim && (yon < 0 || buzMu(chunk.getBlockState(konum.set(x, y, z)), tablo))) {
            return;
        }
        SuSutunu.Tur[] sutun = new SuSutunu.Tur[e.maksBuzKalinligi() + 1];
        for (int d = 0; d < sutun.length; d++) {
            int yy = y - d;
            if (yy < level.getMinBuildHeight()) {
                sutun[d] = SuSutunu.Tur.DIGER;
                continue;
            }
            BlockState s = chunk.getBlockState(konum.set(x, yy, z));
            sutun[d] = suMu(s, tablo) ? SuSutunu.Tur.SU : s.is(tablo.suInce()) ? SuSutunu.Tur.INCE
                    : s.is(tablo.suHedef()) ? SuSutunu.Tur.BUZ : SuSutunu.Tur.DIGER;
        }
        int c = kiyi.uzaklik(x, z);
        int seviye = yon > 0 ? e.buzSeviyesi(etkin, c) : e.buzSeviyesiCozulme(etkin, c);
        SuSutunu.Tur[] plan = SuSutunu.planlaSeviye(sutun, seviye, yon, bizim);
        for (int d = 0; d < sutun.length; d++) {
            if (plan[d] != sutun[d]) {
                Block yeni = switch (plan[d]) {
                    case BUZ -> tablo.suHedef();
                    case INCE -> tablo.suInce();
                    default -> tablo.suKaynak();
                };
                level.setBlock(konum.set(x, y - d, z), yeni.defaultBlockState(), BAYRAK);
                sayac.degisen++;
                sayac.suDegisti = true;
            }
        }
        durum.buz(i, plan[0] == SuSutunu.Tur.BUZ || plan[0] == SuSutunu.Tur.INCE);
    }

    /**
     * Sütunun buharlaşma kaydını {@code hedef} derinliğe getirir (N9): kayıt yoksa ve üst blok üstü açık su kaynağıysa
     * yüzeyden aşağı kurur; kayıt varsa eksik kalanı kurutur ya da derinlik düşünce suyu alttan yukarı geri koyar. Geri
     * dolmada kaynak su "zaten dolu" sayılır (komşu sütundan akan ve vanilla'nın kaynağa çevirdiği su, D3), akıcı su kaynakla
     * değiştirilir; yalnız katı bir blokta (oyuncu doldurdu) durulur ve kayıt bırakılır. Blok değişti mi döner.
     */
    static boolean buharla(ServerLevel level, LevelChunk chunk, ChunkDonmaVerisi durum, int i, int x, int y, int z,
            int hedef, Tablo tablo, BlockPos.MutableBlockPos konum, Sayac sayac) {
        int kaldirilan = durum.kuruSayi(i);
        int ust;
        int k;
        if (kaldirilan > 0) {
            ust = durum.kuruUstY(i);
            int kalan = 0;
            while (kaldirilan + kalan < hedef
                    && suMu(chunk.getBlockState(konum.set(x, ust - kaldirilan - kalan, z)), tablo)) {
                kalan++;
            }
            k = BuharlasmaHedefi.kaldirilacak(hedef, kaldirilan + kalan);
        } else {
            if (hedef <= 0 || !suMu(chunk.getBlockState(konum.set(x, y, z)), tablo)
                    || !chunk.getBlockState(konum.set(x, y + 1, z)).isAir()) {
                return false;
            }
            ust = y;
            int derinlik = 0;
            while (derinlik < hedef && y - derinlik >= level.getMinBuildHeight()
                    && suMu(chunk.getBlockState(konum.set(x, y - derinlik, z)), tablo)) {
                derinlik++;
            }
            k = BuharlasmaHedefi.kaldirilacak(hedef, derinlik);
        }
        if (k == kaldirilan) {
            return false;
        }
        int once = sayac.degisen;
        if (k > kaldirilan) {
            for (int j = kaldirilan; j < k; j++) {
                level.setBlock(konum.set(x, ust - j, z), Blocks.AIR.defaultBlockState(), SU_BAYRAGI);
                sayac.degisen++;
            }
        } else {
            for (int j = kaldirilan - 1; j >= k; j--) {
                BlockState s = chunk.getBlockState(konum.set(x, ust - j, z));
                if (s.is(tablo.suKaynak()) && s.getFluidState().isSource()) {
                    continue;
                }
                if (!s.isAir() && !s.is(tablo.suKaynak())) {
                    k = 0;
                    break;
                }
                level.setBlock(konum, tablo.suKaynak().defaultBlockState(), SU_BAYRAGI);
                sayac.degisen++;
            }
        }
        durum.kuru(i, ust, k);
        if (sayac.degisen != once) {
            sayac.suDegisti = true;
        }
        return sayac.degisen != once;
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
