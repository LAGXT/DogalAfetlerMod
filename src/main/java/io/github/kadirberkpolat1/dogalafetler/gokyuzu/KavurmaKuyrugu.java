package io.github.kadirberkpolat1.dogalafetler.gokyuzu;

import io.github.kadirberkpolat1.dogalafetler.core.TpsOlcer;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarKavurma;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarKavurma.Hedef;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarKavurma.Kategori;
import io.github.kadirberkpolat1.dogalafetler.gokyuzu.mantik.PulsarZinciri;
import io.github.kadirberkpolat1.dogalafetler.kural.Kurallar;
import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.server.ServerStoppedEvent;

/**
 * Pulsar kavurmasının tek blok değişim yolu (PL-3 f): boyut başına tick bütçesi {@code round(pulsarBlokButcesi × TPS
 * kısması)} blok yazma (her {@code setBlock} 1). Merkez (oyuncu) başına tick'te alanla orantılı deneme ({@code adım ·
 * PulsarKavurma.DENEME_YOGUNLUGU · πr²}, kesir sonraki tick'e kalır, en çok {@value #DENEME_TAVANI}), her biri
 * {@code pulsarKavrulmaOlasiligi × güç} olasılıkla merkezin {@value #YARICAP} blok yarıçapında düzgün bir sütun seçer
 * (yalnız yüklü chunk): tam güçte 3 oyun gününde sütun başına ortalama {@value PulsarKavurma#UC_GUN_ISABET} isabet,
 * yarıçaptan bağımsız. Sütunda {@code WORLD_SURFACE}'tan aşağı en çok {@value #TARAMA_DERINLIGI} blok taranır: dönüşebilir
 * blok tablodan ({@link PulsarKavurma}) geçer; yanan örtü (yaprak, bitki, kar, gövde) kalkınca tarama aynı isabette
 * aşağı sürer, kül katmanı ve kömürleşmiş kütük geçilir; zemin dönüşümü, hareket engelleyen dönüşmez blok (çatı, taş)
 * ya da sıvı taramayı bitirir: örtülü yerin altı kavrulmaz. Hedefin yazma sayısı kalan bütçeyi aşıyorsa o tick biter
 * (yarım hedef yok; sütun sonraki isabette sürer). Kül yüzeyine (kül bloğu, kaba toprak, ince kül) seyrek 1–3 kat kül
 * birikir. Zemin dönüşümünde 1/{@value #LAV_PAYDASI} olasılıkla (yalnız {@code pulsarLav}, deniz seviyesinde ya da
 * üstünde, {@value #LAV_UZAKLIGI} blokta oyuncu ve {@value #LAV_YANICI_ARALIGI} blok küpte yanıcı blok yokken) zemin tek
 * bir lav kaynağına döner. Yaprak ve kütük komşu güncellemesiz yazılır (yanan ağaç yaprak çürümesiyle eşya
 * yağdırmasın); diğerleri tam güncellemeyle (buzdan su akar, çift bitki kalkar). Değişen chunk'lar tick sonunda bir kez
 * sanal suya bildirilir. {@code afetBlokHasari} kapalıyken hiçbir şey yazılmaz. Gerçekten örneklenen oyun tick'leri
 * {@link #esdegerGun} sayacında birikir.
 */
public final class KavurmaKuyrugu {
    static final int YARICAP = 96;
    static final int TARAMA_DERINLIGI = 24;
    static final int LAV_PAYDASI = 400;
    static final int LAV_UZAKLIGI = 24;
    /** Lav kaynağı vanilla ateşi bu kadar öteye (üstündeki havanın komşularına) sıçratır; o ateş ortak yangına girmez. */
    static final int LAV_YANICI_ARALIGI = 3;
    /**
     * Merkez başına bir tick'teki en çok deneme. Hiz 1'de 96 blokta ~10,7 deneme gerekir; tavan 6 kat pay bırakır
     * ({@code /dogalafet hiz} ~6'ya kadar eşdeğer gün gerçek günle aynı, üstünde örnekleme doyar ve eşdeğer gün yavaşça
 * birikir). Deneme başına bir heightmap ve en çok {@value #TARAMA_DERINLIGI}+1 blok okuması: 64 deneme merkez başına
 * tick'te ~1 700 okuma, yazma yine bütçeyle sınırlı.
     */
    static final int DENEME_TAVANI = 64;
    private static final int TAM = Block.UPDATE_ALL;
    private static final int SESSIZ = Block.UPDATE_CLIENTS;

    private static final class Durum {
        int sonIslem;
        int sonButce;
        /** Sonraki tick'e kalan kesirli deneme. */
        double artanDeneme;
        /** PULSAR'da gerçekten örneklenen oyun tick'i (Σadım); evre PULSAR'dan çıkınca ve {@code sifirla}'da 0. */
        double toplamAdim;
        /** Sunucu açıldığından beri (kalıcı değil): toplam yazma, tick başına en çok yazma, lav gözesi. */
        long toplamIslem;
        int enCokIslem;
        int lav;
    }

    /** Bir tick'in (ya da tek dönüşümün) yazma sayacı ve değişen chunk'ları. */
    private static final class Islem {
        final ServerLevel level;
        final RandomSource r;
        final float guc;
        int kalan;
        int kullanilan;
        int lav;
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

    /**
     * Kavurmanın bu boyutta biriktirdiği eşdeğer oyun günü: gerçekten örneklenen Σadım / 24000 (sunucu açıldığından beri;
     * evre PULSAR'dan çıkınca ve {@code sifirla}'da 0). Güçten bağımsızdır.
     */
    public static double esdegerGun(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? 0 : d.toplamAdim / PulsarZinciri.GUN_TICK;
    }

    /** Evre PULSAR'dan çıktı ya da dönem sıfırlandı: eşdeğer gün ve kesirli deneme her boyutta 0. */
    static void esdegerGunuSifirla() {
        for (Durum d : DURUMLAR.values()) {
            d.toplamAdim = 0;
            d.artanDeneme = 0;
        }
    }

    /** Geliştirme komutu ve TPS senaryosu: sunucu açıldığından beri bu boyuttaki kavurma toplamları. */
    static String ozet(ServerLevel level) {
        Durum d = DURUMLAR.get(level.dimension());
        return d == null ? "kavurma: toplam 0, en çok 0/tick, lav 0, eşdeğer 0.00 gün"
                : "kavurma: toplam " + d.toplamIslem + ", en çok " + d.enCokIslem + "/tick (bütçe " + d.sonButce
                        + "), lav " + d.lav + ", eşdeğer " + String.format(Locale.ROOT, "%.2f", esdegerGun(level))
                        + " gün";
    }

    static int butce() {
        return (int) Math.max(0L, Math.round(butceKaynagi.getAsInt() * kisma()));
    }

    /** TPS kısması (K-C37; yangın oranı da bununla çarpılır). */
    static double kisma() {
        return kismaKaynagi.getAsDouble();
    }

    /**
     * Bir tick: bütçe yenilenir, denemeler merkezler arasında sırayla dağıtılır (çok oyunculuda adil). Bütçe denemeleri
     * yarıda keserse eşdeğer güne yapılan payı kadar eklenir.
     */
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
        PulsarKavurma.Denemeler plan = PulsarKavurma.denemeler(d.artanDeneme, adim, yaricap, DENEME_TAVANI);
        d.artanDeneme = plan.artan();
        Islem islem = new Islem(level, butce, guc);
        double p = GokyuzuAyarlari.pulsarKavrulmaOlasiligi() * guc;
        int yapilan = 0;
        dongu:
        for (int i = 0; i < plan.sayi(); i++) {
            for (BlockPos merkez : merkezler) {
                if (islem.kalan <= 0) {
                    break dongu;
                }
                yapilan++;
                if (islem.r.nextDouble() < p && !sutun(islem, merkez)) {
                    break dongu;
                }
            }
        }
        int istenen = plan.sayi() * merkezler.size();
        d.toplamAdim += istenen == 0 ? plan.etkinAdim() : plan.etkinAdim() * yapilan / istenen;
        d.sonIslem = islem.kullanilan;
        d.toplamIslem += islem.kullanilan;
        d.enCokIslem = Math.max(d.enCokIslem, islem.kullanilan);
        d.lav += islem.lav;
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

    /**
     * Merkezin çevresinde bir sütun dener: yukarıdan aşağı yanan örtüyü ve ilk zemini dönüştürür; bütçe bir hedefe
     * yetmediyse {@code false} (tick biter).
     */
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
            if (s.isAir() || s.is(PulsarKayitlari.KUL_KATMANI.get()) || s.is(PulsarKayitlari.KOMURLESMIS_KUTUK.get())) {
                continue;
            }
            Kategori kat = kategori(s);
            if (kat != Kategori.DIGER) {
                Hedef h = PulsarKavurma.hedef(kat, islem.guc, islem.r.nextDouble());
                if (h.islem() > islem.kalan) {
                    return false;
                }
                uygula(islem, k.immutable(), s, h, true);
                if (!PulsarKavurma.altinaSurer(h)) {
                    return true;
                }
                continue;
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
                && !level.hasNearbyAlivePlayer(konum.getX() + 0.5, konum.getY() + 0.5, konum.getZ() + 0.5, LAV_UZAKLIGI)
                && yaniciYok(level, konum)) {
            islem.yaz(konum, Blocks.LAVA.defaultBlockState(), TAM);
            islem.lav++;
        }
    }

    /**
     * Lav gözesi için (Ps8): {@code konum} çevresindeki {@value #LAV_YANICI_ARALIGI} blok küpte yanıcı blok yok ve küpün
     * chunk'ları yüklü (okuma chunk yüklemesin). Yoksa lav kaynağı vanilla ateşi yakar: o ateş {@code afet_atesi} değildir,
     * yangın tavanına ve TPS kısmasına girmez, {@code afetBlokHasari} kapanınca sönmez.
     */
    static boolean yaniciYok(LevelReader level, BlockPos konum) {
        int r = LAV_YANICI_ARALIGI;
        for (int cx = (konum.getX() - r) >> 4; cx <= (konum.getX() + r) >> 4; cx++) {
            for (int cz = (konum.getZ() - r) >> 4; cz <= (konum.getZ() + r) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    return false;
                }
            }
        }
        BlockPos.MutableBlockPos k = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    k.setWithOffset(konum, dx, dy, dz);
                    if (level.getBlockState(k).isFlammable(level, k, Direction.UP)) {
                        return false;
                    }
                }
            }
        }
        return true;
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
