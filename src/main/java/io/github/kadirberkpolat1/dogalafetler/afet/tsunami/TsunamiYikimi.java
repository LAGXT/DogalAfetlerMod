package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.Tags;

/**
 * Yıkım koreografisi (tasarım notu §2): cephe ilerledikçe önündeki 3 blokluk şeridin sütunları kuyruğa girer; sütun taraması
 * dalga tepesinden tabanın bir altına kadar yıkılabilir blokları sınıfına göre gecikmeyle sıraya koyar: önce yaprak, çit, cam,
 * halı (anında), sonra ahşap (0,3 sn), sonra taş yapı (0,6 sn, yalnız S4–5). Doğal arazi (toprak, kum, taş), çimen ve çiçek
 * gibi bitkiler, sıvı içeren ve blok varlıklı (sandık, tabela) bloklar yıkılmaz (her kırılan blok istemcide bölüm yeniden derler;
 * çayırı düzlemek S5'te FPS'i ve TPS'i çökertti). Kırılan ahşap/taş blok {@code tsunami.yikimEsyaOrani} olasılıkla eşya
 * bırakır (dalga onu sürükler), tick başına en çok {@value #DUSURME_TICK}, afet başına {@code 16·seviye}; tick başına en
 * çok {@code Ayar.efekt} kırılma efekti (ses + parçacık; config {@code tsunami.yikimEfektTick}). Blok komşu güncellemesi
 * olmadan değişir ({@link #BAYRAK}):
 * komşu şekil güncellemesi yaprak mesafesi, çit/cam bağlantısı zincirleriyle ikinci bir blok değişikliği dalgası doğuruyordu
 * (TS-12). Her değişen chunk için {@link SanalSu#chunkDegisti}. Tarama sırasında dalgaya açılan mağara ağızları
 * (doğal tavanlı, göğü görmeyen, yanında açık hava olan hücre) afete bildirilir.
 */
final class TsunamiYikimi {
    /** Yıkım sınıfı ve cephe şeridine girdikten sonraki gecikmesi (ölçekli tick). */
    enum Sinif {
        HAFIF(0), AHSAP(6), TAS(12);

        final int gecikme;

        Sinif(int gecikme) {
            this.gecikme = gecikme;
        }
    }

    private record Is(long konum, long hazir) {
    }

    /** Bir 16³ bölümün bekleyen işleri; {@code enErken} en erken hazır olanın tick'i. */
    private static final class Bolum {
        final ArrayDeque<Is> isler = new ArrayDeque<>();
        long enErken = Long.MAX_VALUE;
    }

    private record Kolon(int x, int z, int ust) {
    }

    /**
     * Tick bütçesi: {@code blok} jeton (tick başına dolan, Ts3-1), {@code birim} tick başına başlatılan en çok birim
     * ({@code tsunami.yikimBolumTick} × kısma), {@code sutun} en çok farklı chunk sütunu, {@code efekt} kırılma efekti;
     * {@code esyaOrani} ahşap/taş bloğun eşya bırakma olasılığı; {@code sutunBirimi} birim chunk sütunu mu (yoksa 16³ bölüm).
     */
    /**
     * {@code birimTavani} (Ts4-2): bir birimin tek tick'te kırılabilecek en çok bloğu; aşan birim kalanını sonraki tick'te sürdürür.
     */
    record Ayar(int blok, int birim, int sutun, int efekt, double esyaOrani, boolean sutunBirimi, int birimTavani) {
    }

    static final int KOLON_BUTCESI = 192;
    /** Ts3: sütun taraması dalga tepesinden en çok bu kadar yukarı (çatı, ağaç tepesi) uzar; dalga üstü yapı havada kalmaz. */
    static final int UST_PAYI = 16;
    /** İstemciye gönder, komşu şekillerini güncelleme (yaprak/çit zinciri yok), komşuya {@code neighborChanged} yok. */
    static final int BAYRAK = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    static final int DUSURME_TICK = 2;
    private static final double ADIM = 0.7;

    private final ArrayDeque<Kolon> kolonlar = new ArrayDeque<>();
    /** Bölüm anahtarıyla bekleyen işler, bölümün ilk iş aldığı sırada (cephe satır sırası). */
    private final Long2ObjectLinkedOpenHashMap<Bolum> bolumIsleri = new Long2ObjectLinkedOpenHashMap<>();
    private int isSayisi;
    private final LongOpenHashSet gorulen = new LongOpenHashSet();
    private final LongOpenHashSet degisenChunk = new LongOpenHashSet();
    private final LongOpenHashSet bolumler = new LongOpenHashSet();
    private final LongOpenHashSet birimler = new LongOpenHashSet();
    private final LongOpenHashSet kirli = new LongOpenHashSet();
    private boolean sutunBirimi;
    private int jeton;
    private long toplamKirilan;
    private long toplamTaranan;
    private long toplamBolum;
    private long toplamKirli;
    private int enCokBirimTick;
    private int enCokBlokTick;
    private long enUzunTickNs;
    private int toplamDusen;

    /** Tick başına blok jetonu: {@code 8 + 8·seviye} × {@code yikimCarpani} × TPS kısması (en az 1). */
    static int blokButcesi(TsunamiSeviyesi sv, double carpan, double kisma) {
        return Math.max(1, (int) Math.round(sv.yikimButcesi() * carpan * kisma));
    }

    long toplamKirilan() {
        return toplamKirilan;
    }

    long toplamTaranan() {
        return toplamTaranan;
    }

    /** Tick başına değişen farklı chunk bölümü sayılarının toplamı (istemcide yeniden derlenen bölüm). */
    long toplamBolum() {
        return toplamBolum;
    }

    /**
     * Değişen bölümlerin 3×3×3 komşuluklarının tick başına birleşim sayılarının toplamı: istemci her değişen blok için
     * bölümünü ve komşularını kirletir ({@code setSectionDirtyWithNeighbors}); aynı tick'te örtüşenler bir kez derlenir.
     */
    long toplamKirli() {
        return toplamKirli;
    }

    /** Bir tick'te başlatılan en çok birim (bölüm ya da sütun; TS-15b: en çok {@code Ayar.birim}). */
    int enCokBirimTick() {
        return enCokBirimTick;
    }

    /** Ts4-2: bir tick'te kırılan en çok blok. */
    int enCokBlokTick() {
        return enCokBlokTick;
    }

    /** Ts4-2: en uzun yıkım tick'inin süresi (ms; tarama, kırma ve sanal su bildirimi). */
    double enUzunTickMs() {
        return enUzunTickNs / 1e6;
    }

    int bekleyen() {
        return kolonlar.size() + isSayisi;
    }

    /** {@code boyuna} uzaklığındaki cephe şeridi satırının sütunları (her sütun bir kez). */
    void satirEkle(TsunamiGeometrisi g, double boyuna, double genislik, int ust) {
        double yarim = genislik / 2;
        for (double yan = -yarim; yan <= yarim; yan += ADIM) {
            int x = (int) Math.floor(g.x(boyuna, yan));
            int z = (int) Math.floor(g.z(boyuna, yan));
            if (gorulen.add(ChunkPos.asLong(x, z))) {
                kolonlar.add(new Kolon(x, z, ust));
            }
        }
    }

    /**
     * Bir tick: sütun taraması ({@link #KOLON_BUTCESI} × kısma), sonra hazır işi olan birimler (16³ bölüm ya da chunk sütunu)
     * cephe sırasında. Jetonlu hız (Ts3-1): jeton her tick {@code Ayar.blok} kadar dolar (en çok bir tick'lik); birim ancak
     * jeton ≥ 0 iken başlar, o an hazır bütün blokları aynı tick'te kırılır ve sayısı jetondan düşülür (negatife inebilir).
     * Ortalama hız blok bütçesine eşit kalır, TPS kısmasıyla yavaşlar; bir birim tick'lere bölünmez (TS-15): her değişen
     * bölüm istemcide yeniden derlenir, ışık değişimi sütun boyunca ve komşu bölümlere yayılıp onları da derletir (TS-12).
     * Tick başına en çok {@code Ayar.birim} birim ve {@code Ayar.sutun} sütun; bir birim tick'te en çok {@code Ayar.birimTavani} blok. Sınıf gecikmeleri (cam önce, ahşap 0,3 sn
     * sonra) korunur. {@code magara} mağara ağzı adayını alır ve daha aday istiyorsa {@code true} döner. Kırılan blok sayısı.
     */
    int tick(ServerLevel l, long simdi, TsunamiSeviyesi sv, double taban, double kisma, Ayar a, boolean blokHasari,
            Predicate<BlockPos> magara) {
        long basla = System.nanoTime();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int alt = (int) Math.floor(taban) - 1;
        boolean magaraIstiyor = true;
        sutunBirimi = a.sutunBirimi();
        for (int i = 0, n = Math.max(16, (int) (KOLON_BUTCESI * kisma)); i < n && !kolonlar.isEmpty(); i++) {
            Kolon k = kolonlar.poll();
            toplamTaranan++;
            if (l.getChunkSource().getChunkNow(k.x >> 4, k.z >> 4) == null) {
                continue;
            }
            boolean ustDogal = false;
            int tepe = Math.max(k.ust, Math.min(k.ust + UST_PAYI, l.getHeight(Heightmap.Types.MOTION_BLOCKING, k.x, k.z) - 1));
            for (int y = Math.min(tepe, l.getMaxBuildHeight() - 1); y >= Math.max(alt, l.getMinBuildHeight()); y--) {
                BlockState s = l.getBlockState(m.set(k.x, y, k.z));
                if (s.isAir()) {
                    if (ustDogal && magaraIstiyor && magaraAgzi(l, m)) {
                        magaraIstiyor = magara.test(m.immutable());
                    }
                } else if (blokHasari) {
                    Sinif c = sinif(l, m, s, sv);
                    if (c != null) {
                        isEkle(m, simdi + c.gecikme);
                    }
                }
                ustDogal = dogal(s);
            }
        }
        int kirilan = 0;
        int efekt = 0;
        int dusen = 0;
        bolumler.clear();
        birimler.clear();
        jeton = Math.min(a.blok(), jeton + a.blok());
        var birimIt = bolumIsleri.long2ObjectEntrySet().fastIterator();
        while (birimIt.hasNext() && jeton >= 0 && birimler.size() < a.birim()) {
            var g = birimIt.next();
            Bolum b = g.getValue();
            long birim = g.getLongKey();
            long sutun = ChunkPos.asLong(SectionPos.x(birim), SectionPos.z(birim));
            if (b.enErken > simdi || !degisenChunk.contains(sutun) && degisenChunk.size() >= a.sutun()) {
                continue;
            }
            int once = kirilan;
            long enErken = Long.MAX_VALUE;
            boolean kesildi = false;
            for (Iterator<Is> isIt = b.isler.iterator(); isIt.hasNext();) {
                if (kirilan - once >= a.birimTavani()) {
                    kesildi = true;
                    break;
                }
                Is is = isIt.next();
                if (is.hazir() > simdi) {
                    enErken = Math.min(enErken, is.hazir());
                    continue;
                }
                isIt.remove();
                isSayisi--;
                m.set(is.konum());
                if (!l.isLoaded(m)) {
                    continue;
                }
                BlockState s = l.getBlockState(m);
                Sinif c = blokHasari ? sinif(l, m, s, sv) : null;
                if (c == null) {
                    continue;
                }
                if (c != Sinif.HAFIF && dusen < DUSURME_TICK && toplamDusen < 16 * sv.seviye()
                        && l.getRandom().nextDouble() < a.esyaOrani()) {
                    Block.dropResources(s, l, m);
                    dusen++;
                    toplamDusen++;
                }
                if (efekt++ < a.efekt()) {
                    l.levelEvent(2001, m, Block.getId(s));
                }
                l.setBlock(m, Blocks.AIR.defaultBlockState(), BAYRAK);
                bolumler.add(SectionPos.asLong(m.getX() >> 4, m.getY() >> 4, m.getZ() >> 4));
                kirilan++;
            }
            b.enErken = kesildi ? simdi : enErken;
            if (b.isler.isEmpty()) {
                birimIt.remove();
            }
            if (kirilan > once) {
                birimler.add(birim);
                degisenChunk.add(sutun);
                jeton -= kirilan - once;
            }
        }
        for (long c : degisenChunk) {
            SanalSu.chunkDegisti(l, new ChunkPos(c));
        }
        degisenChunk.clear();
        kirli.clear();
        for (LongIterator it = bolumler.iterator(); it.hasNext();) {
            long s = it.nextLong();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        kirli.add(SectionPos.asLong(SectionPos.x(s) + dx, SectionPos.y(s) + dy, SectionPos.z(s) + dz));
                    }
                }
            }
        }
        toplamKirilan += kirilan;
        toplamBolum += bolumler.size();
        toplamKirli += kirli.size();
        enCokBirimTick = Math.max(enCokBirimTick, birimler.size());
        enCokBlokTick = Math.max(enCokBlokTick, kirilan);
        enUzunTickNs = Math.max(enUzunTickNs, System.nanoTime() - basla);
        return kirilan;
    }

    private void isEkle(BlockPos p, long hazir) {
        long anahtar = SectionPos.asLong(p.getX() >> 4, sutunBirimi ? 0 : p.getY() >> 4, p.getZ() >> 4);
        Bolum b = bolumIsleri.get(anahtar);
        if (b == null) {
            b = new Bolum();
            bolumIsleri.put(anahtar, b);
        }
        b.isler.add(new Is(p.asLong(), hazir));
        b.enErken = Math.min(b.enErken, hazir);
        isSayisi++;
    }

    /** Bloğun yıkım sınıfı; yıkılmazsa null. */
    static Sinif sinif(ServerLevel l, BlockPos p, BlockState s, TsunamiSeviyesi sv) {
        if (s.isAir() || !s.getFluidState().isEmpty() || s.hasBlockEntity() || dogal(s)) {
            return null;
        }
        float sert = s.getDestroySpeed(l, p);
        if (sert < 0 || sert > sv.sertlik()) {
            return null;
        }
        if (s.is(BlockTags.LEAVES) || s.is(Tags.Blocks.GLASS) || s.is(Tags.Blocks.GLASS_PANES) || s.is(BlockTags.FENCES)
                || s.is(BlockTags.FENCE_GATES) || s.is(BlockTags.WOOL_CARPETS)) {
            return Sinif.HAFIF;
        }
        SoundType ses = s.getSoundType();
        if (s.is(BlockTags.LOGS) || s.is(BlockTags.PLANKS) || s.is(BlockTags.WOODEN_SLABS) || s.is(BlockTags.WOODEN_STAIRS)
                || s.is(BlockTags.WOODEN_DOORS) || s.is(BlockTags.WOODEN_TRAPDOORS) || s.is(BlockTags.WOOL)
                || s.is(BlockTags.BAMBOO_BLOCKS) || ses == SoundType.WOOD || ses == SoundType.NETHER_WOOD
                || ses == SoundType.BAMBOO_WOOD || ses == SoundType.CHERRY_WOOD || ses == SoundType.LADDER) {
            return Sinif.AHSAP;
        }
        if (sv.tasYikar() && (s.is(Tags.Blocks.COBBLESTONE) || s.is(Blocks.BRICKS) || s.is(Blocks.MUD_BRICKS)
                || s.is(Blocks.SMOOTH_STONE) || s.is(BlockTags.STONE_BRICKS) || s.is(BlockTags.WALLS) || s.is(BlockTags.SLABS)
                || s.is(BlockTags.STAIRS))) {
            return Sinif.TAS;
        }
        return null;
    }

    /** Doğal arazi bloğu (yıkılmaz; üstü doğal olan hava hücresi mağara adayıdır). */
    static boolean dogal(BlockState s) {
        return s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.DIRT) || s.is(BlockTags.SAND)
                || s.is(Tags.Blocks.GRAVEL) || s.is(Tags.Blocks.ORES) || s.is(Blocks.CLAY) || s.is(BlockTags.TERRACOTTA)
                || s.is(Tags.Blocks.SANDSTONE);
    }

    /** Doğal tavanın altındaki, göğü görmeyen ve yanında göğe açık hava olan hücre (dalgaya açılan mağara ağzı). */
    static boolean magaraAgzi(ServerLevel l, BlockPos p) {
        if (l.canSeeSky(p)) {
            return false;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos k = p.relative(d);
            if (l.isLoaded(k) && l.getBlockState(k).isAir() && l.canSeeSky(k)) {
                return true;
            }
        }
        return false;
    }

    /** Kuyrukları bırakır (durdurma). */
    void temizle() {
        kolonlar.clear();
        bolumIsleri.clear();
        isSayisi = 0;
        gorulen.clear();
    }
}
