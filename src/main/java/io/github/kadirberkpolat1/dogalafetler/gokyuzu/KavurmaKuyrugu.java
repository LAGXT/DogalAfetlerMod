package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarKavurma;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarKavurma.Hedef;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarKavurma.Kategori;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.server.ServerStoppedEvent;

/**
 * Pulsar kavurmasının tek blok değişim yolu (PL-3 f): boyut başına tick bütçesi {@code round(pulsarBlokButcesi × TPS
 * kısması)} blok yazma (her {@code setBlock} 1). Oyuncu başına tick'te {@code min(adım, 32)} deneme, her biri
 * {@code pulsarKavrulmaOlasiligi × güç} olasılıkla oyuncunun {@value #YARICAP} blok yarıçapında düzgün bir sütun seçer
 * (yalnız yüklü chunk). Sütunda {@code WORLD_SURFACE}'tan aşağı en çok {@value #TARAMA_DERINLIGI} blok, ilk
 * dönüşebilir blok tablodan ({@link PulsarKavurma}) geçer; hareket engelleyen dönüşmez blok (çatı, taş) ya da sıvı
 * taramayı durdurur: örtülü yerin altı kavrulmaz. Hedefin yazma sayısı kalan bütçeyi aşıyorsa o tick biter (yarım
 * dönüşüm yok). Kül yüzeyine (kül bloğu, kaba toprak, ince kül) seyrek 1–3 kat kül birikir. Zemin dönüşümünde
 * 1/{@value #LAV_PAYDASI} olasılıkla (yalnız {@code pulsarLav}, deniz seviyesinde ya da üstünde, {@value #LAV_UZAKLIGI}
 * blokta oyuncu yokken) zemin tek bir lav kaynağına döner. Yaprak ve kütük komşu güncellemesiz yazılır (yanan ağaç
 * yaprak çürümesiyle eşya yağdırmasın); diğerleri tam güncellemeyle (buzdan su akar, çift bitki kalkar). Değişen
 * chunk'lar tick sonunda bir kez sanal suya bildirilir. {@code afetBlokHasari} kapalıyken hiçbir şey yazılmaz.
 */
public final class KavurmaKuyrugu {
    static final int YARICAP = 96;
    static final int TARAMA_DERINLIGI = 24;
    static final int LAV_PAYDASI = 400;
    static final int LAV_UZAKLIGI = 24;
    /** Merkez başına bir tick'teki en çok deneme ({@code /dogalafet hiz} yüksekken TPS korunur). */
    private static final int DENEME_TAVANI = 32;
    private static final int TAM = Block.UPDATE_ALL;
    private static final int SESSIZ = Block.UPDATE_CLIENTS;

    private static final class Durum {
        int sonIslem;
        int sonButce;
    }

    /** Bir tick'in (ya da tek dönüşümün) yazma sayacı ve değişen chunk'ları. */
    private static final class Islem {
        final ServerLevel level;
        final RandomSource r;
        final float guc;
        int kalan;
        int kullanilan;
        final LongOpenHashSet chunklar = new LongOpenHashSet();

        Islem(ServerLevel level, int butce, float guc) {
            this.level = level;
            this.r = level.getRandom();
            this.guc = guc;
            this.kalan = butce;
        }

        void yaz(BlockPos konum, BlockState durum, int bayrak) {
            level.setBlock(konum, durum, bayrak);
            kalan--;
            kullanilan++;
            chunklar.add(ChunkPos.asLong(konum.getX() >> 4, konum.getZ() >> 4));
        }

        void bildir() {
            for (LongIterator i = chunklar.iterator(); i.hasNext(); ) {
                SanalSu.chunkDegisti(level, new ChunkPos(i.nextLong()));
            }
        }
    }

    private static final Map<ResourceKey<Level>, Durum> DURUMLAR = new HashMap<>();
    /** Test: denemelerin merkezleri (null: oyuncular), yarıçap, taban bütçe, TPS kısması ve lav anahtarı. */
    private static Function<ServerLevel, List<BlockPos>> merkezKaynagi;
    private static int yaricap = YARICAP;
    private static IntSupplier butceKaynagi = GokyuzuAyarlari::pulsarBlokButcesi;
    private static DoubleSupplier kismaKaynagi = TpsOlcer::kismaCarpani;
    private static BooleanSupplier lavKaynagi = GokyuzuAyarlari::pulsarLav;

    private KavurmaKuyrugu() {
    }

    /** Son tick'te kullanılan blok yazma sayısı (GameTest bütçe denetimi). */
    public static int sonIslem(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? 0 : d.sonIslem;
    }

    /** Son tick'in bütçesi ({@code pulsarBlokButcesi × TPS kısması}). */
    public static int sonButce(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? 0 : d.sonButce;
    }

    static int butce() {
        return (int) Math.max(0L, Math.round(butceKaynagi.getAsInt() * kisma()));
    }

    /** TPS kısması (K-C37; yangın oranı da bununla çarpılır). */
    static double kisma() {
        return kismaKaynagi.getAsDouble();
    }

    /** Bir tick: bütçe yenilenir, denemeler merkezler arasında sırayla dağıtılır (çok oyunculuda adil). */
    static void tick(ServerLevel level, float guc, int adim) {
        Durum d = DURUMLAR.computeIfAbsent(level.dimension(), k -> new Durum());
        int butce = butce();
        d.sonButce = butce;
        d.sonIslem = 0;
        if (butce <= 0 || !(guc > 0f) || adim <= 0 || !Kurallar.blokHasariAcik(level.getGameRules())) {
            return;
        }
        List<BlockPos> merkezler = merkezler(level);
        if (merkezler.isEmpty()) {
            return;
        }
        Islem islem = new Islem(level, butce, guc);
        double p = GokyuzuAyarlari.pulsarKavrulmaOlasiligi() * guc;
        int deneme = Math.min(adim, DENEME_TAVANI);
        dongu:
        for (int i = 0; i < deneme; i++) {
            for (BlockPos merkez : merkezler) {
                if (islem.kalan <= 0) {
                    break dongu;
                }
                if (islem.r.nextDouble() < p && !sutun(islem, merkez)) {
                    break dongu;
                }
            }
        }
        d.sonIslem = islem.kullanilan;
        islem.bildir();
    }

    /**
     * Tek konumu tablodan geçirir (GameTest uç noktaları; bütçe sınırsız, lav yok). {@code afetBlokHasari} kapalıysa 0.
     * Yazılan blok sayısını döner.
     */
    static int donustur(ServerLevel level, BlockPos konum, float guc, double zar) {
        if (!Kurallar.blokHasariAcik(level.getGameRules())) {
            return 0;
        }
        Islem islem = new Islem(level, Integer.MAX_VALUE, guc);
        BlockState durum = level.getBlockState(konum);
        uygula(islem, konum, durum, PulsarKavurma.hedef(kategori(durum), guc, zar), false);
        islem.bildir();
        return islem.kullanilan;
    }

    /** Blok → tablo kategorisi (yaprak ve kütük etiketten, diğerleri blok/etiket listesinden). */
    static Kategori kategori(BlockState s) {
        if (s.is(BlockTags.LEAVES)) {
            return Kategori.YAPRAK;
        }
        if (s.is(BlockTags.LOGS)) {
            return Kategori.KUTUK;
        }
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM) || s.is(Blocks.MOSS_BLOCK)) {
            return Kategori.CIMEN;
        }
        if (s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT)) {
            return Kategori.TOPRAK;
        }
        if (s.is(BlockTags.SMALL_FLOWERS) || s.is(BlockTags.TALL_FLOWERS) || s.is(BlockTags.SAPLINGS)
                || s.is(Blocks.GRASS) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN)
                || s.is(Blocks.DEAD_BUSH) || s.is(Blocks.BROWN_MUSHROOM) || s.is(Blocks.RED_MUSHROOM)
                || s.is(Blocks.CACTUS)) {
            return Kategori.BITKI;
        }
        if (s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) {
            return Kategori.KAR;
        }
        if (s.is(Blocks.ICE) || s.is(Blocks.FROSTED_ICE)) {
            return Kategori.BUZ;
        }
        if (s.is(Blocks.SAND) || s.is(Blocks.RED_SAND)) {
            return Kategori.KUM;
        }
        return Kategori.DIGER;
    }

    /** Deneme ve yangın merkezleri: izleyici olmayan oyuncular (test kaynağı varsa o). */
    static List<BlockPos> merkezler(ServerLevel level) {
        if (merkezKaynagi != null) {
            return merkezKaynagi.apply(level);
        }
        List<BlockPos> liste = new ArrayList<>();
        for (ServerPlayer o : level.players()) {
            if (!o.isSpectator()) {
                liste.add(o.blockPosition());
            }
        }
        return liste;
    }

    /** Merkezin çevresinde bir sütun dener; bütçe hedefe yetmediyse {@code false} (tick biter). */
    private static boolean sutun(Islem islem, BlockPos merkez) {
        ServerLevel level = islem.level;
        double aci = islem.r.nextDouble() * Math.PI * 2;
        double uzaklik = yaricap * Math.sqrt(islem.r.nextDouble());
        int x = merkez.getX() + (int) Math.round(Math.cos(aci) * uzaklik);
        int z = merkez.getZ() + (int) Math.round(Math.sin(aci) * uzaklik);
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return true;
        }
        int ustY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        int enAlt = Math.max(level.getMinBuildHeight(), ustY - TARAMA_DERINLIGI);
        if (ustY < enAlt) {
            return true;
        }
        BlockPos.MutableBlockPos k = new BlockPos.MutableBlockPos(x, ustY, z);
        BlockState ust = level.getBlockState(k);
        if (birikim(islem, k.immutable(), ust)) {
            return true;
        }
        for (int y = ustY; y >= enAlt; y--) {
            BlockState s = level.getBlockState(k.setY(y));
            if (s.isAir()) {
                continue;
            }
            Kategori kat = kategori(s);
            if (kat != Kategori.DIGER) {
                Hedef h = PulsarKavurma.hedef(kat, islem.guc, islem.r.nextDouble());
                if (h.islem() > islem.kalan) {
                    return false;
                }
                uygula(islem, k.immutable(), s, h, true);
                return true;
            }
            if (!s.getFluidState().isEmpty() || s.blocksMotion()) {
                return true;
            }
        }
        return true;
    }

    /** Kül yüzeyinde (ince kül katmanı, kül bloğu ya da üstü açık kaba toprak) seyrek kül birikimi. */
    private static boolean birikim(Islem islem, BlockPos konum, BlockState ust) {
        Block kul = PulsarKayitlari.KUL_KATMANI.get();
        boolean katman = ust.is(kul);
        if (!katman && !ust.is(PulsarKayitlari.KUL_BLOGU.get()) && !ust.is(Blocks.COARSE_DIRT)) {
            return false;
        }
        int ek = PulsarKavurma.birikimKati(islem.guc, islem.r.nextDouble());
        if (ek <= 0) {
            return false;
        }
        if (katman) {
            int mevcut = ust.getValue(SnowLayerBlock.LAYERS);
            int yeni = PulsarKavurma.yeniKat(mevcut, ek, PulsarKavurma.BIRIKIM_TAVANI);
            if (yeni != mevcut) {
                islem.yaz(konum, ust.setValue(SnowLayerBlock.LAYERS, yeni), TAM);
            }
            return true;
        }
        BlockPos yukari = konum.above();
        BlockState yeni = kul.defaultBlockState().setValue(SnowLayerBlock.LAYERS,
                PulsarKavurma.yeniKat(0, ek, PulsarKavurma.BIRIKIM_TAVANI));
        if (islem.level.getBlockState(yukari).isAir() && yeni.canSurvive(islem.level, yukari)) {
            islem.yaz(yukari, yeni, TAM);
        }
        return true;
    }

    private static void uygula(Islem islem, BlockPos konum, BlockState durum, Hedef h, boolean lavOlabilir) {
        ServerLevel level = islem.level;
        switch (h) {
            case YOK -> {
                return;
            }
            case KABA_TOPRAK -> islem.yaz(konum, Blocks.COARSE_DIRT.defaultBlockState(), TAM);
            case KUL_BLOGU -> islem.yaz(konum, PulsarKayitlari.KUL_BLOGU.get().defaultBlockState(), TAM);
            case KOMURLESMIS_KUTUK -> {
                BlockState kutuk = PulsarKayitlari.KOMURLESMIS_KUTUK.get().defaultBlockState();
                if (durum.hasProperty(RotatedPillarBlock.AXIS)) {
                    kutuk = kutuk.setValue(RotatedPillarBlock.AXIS, durum.getValue(RotatedPillarBlock.AXIS));
                }
                islem.yaz(konum, kutuk, SESSIZ);
            }
            case HAVA -> islem.yaz(konum, Blocks.AIR.defaultBlockState(), TAM);
            case SU -> islem.yaz(konum, Blocks.WATER.defaultBlockState(), TAM);
            case CAM -> islem.yaz(konum, Blocks.GLASS.defaultBlockState(), TAM);
            case HAVA_VE_KUL -> {
                islem.yaz(konum, Blocks.AIR.defaultBlockState(), SESSIZ);
                kulDusur(islem, konum);
            }
        }
        if (lavOlabilir && (h == Hedef.KABA_TOPRAK || h == Hedef.KUL_BLOGU) && islem.kalan > 0
                && lavKaynagi.getAsBoolean() && islem.r.nextInt(LAV_PAYDASI) == 0 && konum.getY() >= level.getSeaLevel()
                && level.getBlockState(konum.above()).isAir()
                && !level.hasNearbyAlivePlayer(konum.getX() + 0.5, konum.getY() + 0.5, konum.getZ() + 0.5, LAV_UZAKLIGI)) {
            islem.yaz(konum, Blocks.LAVA.defaultBlockState(), TAM);
        }
    }

    /**
     * Yanan yaprağın külü: altındaki ilk yaprak ve hava olmayan bloğa iner; kül katmanıysa bir kat artar (en çok 8), üstü
     * tutan bir zeminse üstüne bir kat konur; bitki ya da sıvı üstüne kül düşmez.
     */
    private static void kulDusur(Islem islem, BlockPos yaprak) {
        ServerLevel level = islem.level;
        Block kul = PulsarKayitlari.KUL_KATMANI.get();
        int enAlt = Math.max(level.getMinBuildHeight(), yaprak.getY() - TARAMA_DERINLIGI);
        BlockPos.MutableBlockPos k = yaprak.mutable();
        for (int y = yaprak.getY() - 1; y >= enAlt; y--) {
            BlockState s = level.getBlockState(k.setY(y));
            if (s.isAir() || s.is(BlockTags.LEAVES)) {
                continue;
            }
            if (s.is(kul)) {
                int mevcut = s.getValue(SnowLayerBlock.LAYERS);
                int yeni = PulsarKavurma.yeniKat(mevcut, 1, PulsarKavurma.KUL_EN_COK_KAT);
                if (yeni != mevcut) {
                    islem.yaz(k.immutable(), s.setValue(SnowLayerBlock.LAYERS, yeni), TAM);
                }
                return;
            }
            BlockPos ust = k.above();
            BlockState katman = kul.defaultBlockState();
            if (level.getBlockState(ust).isAir() && katman.canSurvive(level, ust)) {
                islem.yaz(ust, katman, TAM);
            }
            return;
        }
    }

    public static void sunucuDurdu(ServerStoppedEvent olay) {
        DURUMLAR.clear();
    }

    /** Test: deneme merkezleri (null: oyuncular); öncekini döndürür (geri koyun). */
    static Function<ServerLevel, List<BlockPos>> merkezKaynagiAyarla(Function<ServerLevel, List<BlockPos>> kaynak) {
        Function<ServerLevel, List<BlockPos>> onceki = merkezKaynagi;
        merkezKaynagi = kaynak;
        return onceki;
    }

    /** Test: deneme yarıçapı; öncekini döndürür. */
    static int yaricapAyarla(int yeni) {
        int onceki = yaricap;
        yaricap = Math.max(0, yeni);
        return onceki;
    }

    /** Test: kısmadan önceki taban bütçe; öncekini döndürür. */
    static IntSupplier butceKaynagiAyarla(IntSupplier kaynak) {
        IntSupplier onceki = butceKaynagi;
        butceKaynagi = kaynak;
        return onceki;
    }

    /** Test: TPS kısması; öncekini döndürür. */
    static DoubleSupplier kismaKaynagiAyarla(DoubleSupplier kaynak) {
        DoubleSupplier onceki = kismaKaynagi;
        kismaKaynagi = kaynak;
        return onceki;
    }

    /** Test: lav gözesi açık mı (sahne kendiliğinden akmasın); öncekini döndürür. */
    static BooleanSupplier lavKaynagiAyarla(BooleanSupplier kaynak) {
        BooleanSupplier onceki = lavKaynagi;
        lavKaynagi = kaynak;
        return onceki;
    }
}
