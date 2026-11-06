package io.github.kadirberkpolat1.dogalafetler.afet.tsunami;

import io.github.kadirberkpolat1.dogalafetler.su.SanalSu;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

/**
 * Yıkım koreografisi (tasarım notu §2): cephe ilerledikçe önündeki 3 blokluk şeridin sütunları kuyruğa girer; sütun taraması
 * dalga tepesinden tabanın bir altına kadar yıkılabilir blokları sınıfına göre gecikmeyle sıraya koyar: önce yaprak, çit, cam,
 * halı (anında), sonra ahşap (0,3 sn), sonra taş yapı (0,6 sn, yalnız S4–5). Doğal arazi (toprak, kum, taş), çimen ve çiçek
 * gibi bitkiler, sıvı içeren ve blok varlıklı (sandık, tabela) bloklar yıkılmaz (her kırılan blok istemcide bölüm yeniden derler;
 * çayırı düzlemek S5'te FPS'i ve TPS'i çökertti). Kırılan ahşap/taş bloğun %30'u eşya bırakır (dalga onu sürükler), tick
 * başına en çok {@value #DUSURME_TICK}, afet başına {@code 16·seviye}; tick başına en çok 6 kırılma
 * efekti (ses + parçacık). Her değişen chunk için {@link SanalSu#chunkDegisti}. Tarama sırasında dalgaya açılan mağara ağızları
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

    private record Kolon(int x, int z, int ust) {
    }

    static final int KOLON_BUTCESI = 192;
    static final int EFEKT_SINIRI = 6;
    static final double DUSURME_ORANI = 0.3;
    static final int DUSURME_TICK = 2;
    private static final double ADIM = 0.7;

    private final ArrayDeque<Kolon> kolonlar = new ArrayDeque<>();
    @SuppressWarnings("unchecked")
    private final ArrayDeque<Is>[] isler = new ArrayDeque[] {new ArrayDeque<>(), new ArrayDeque<>(), new ArrayDeque<>()};
    private final LongOpenHashSet gorulen = new LongOpenHashSet();
    private final LongOpenHashSet degisenChunk = new LongOpenHashSet();
    private long toplamKirilan;
    private long toplamTaranan;
    private int toplamDusen;

    long toplamKirilan() {
        return toplamKirilan;
    }

    long toplamTaranan() {
        return toplamTaranan;
    }

    int bekleyen() {
        return kolonlar.size() + isler[0].size() + isler[1].size() + isler[2].size();
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
     * Bir tick: sütun taraması ({@link #KOLON_BUTCESI} × kısma), sonra hazır işlerden en çok {@code butce} blok. {@code magara}
     * mağara ağzı adayını alır ve daha aday istiyorsa {@code true} döner. Kırılan blok sayısı.
     */
    int tick(ServerLevel l, long simdi, TsunamiSeviyesi sv, double taban, double kisma, int butce, boolean blokHasari,
            Predicate<BlockPos> magara) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int alt = (int) Math.floor(taban) - 1;
        boolean magaraIstiyor = true;
        for (int i = 0, n = Math.max(16, (int) (KOLON_BUTCESI * kisma)); i < n && !kolonlar.isEmpty(); i++) {
            Kolon k = kolonlar.poll();
            toplamTaranan++;
            if (l.getChunkSource().getChunkNow(k.x >> 4, k.z >> 4) == null) {
                continue;
            }
            boolean ustDogal = false;
            for (int y = Math.min(k.ust, l.getMaxBuildHeight() - 1); y >= Math.max(alt, l.getMinBuildHeight()); y--) {
                BlockState s = l.getBlockState(m.set(k.x, y, k.z));
                if (s.isAir()) {
                    if (ustDogal && magaraIstiyor && magaraAgzi(l, m)) {
                        magaraIstiyor = magara.test(m.immutable());
                    }
                } else if (blokHasari) {
                    Sinif c = sinif(l, m, s, sv);
                    if (c != null) {
                        isler[c.ordinal()].add(new Is(m.asLong(), simdi + c.gecikme));
                    }
                }
                ustDogal = dogal(s);
            }
        }
        int kirilan = 0;
        int efekt = 0;
        int dusen = 0;
        for (ArrayDeque<Is> kuyruk : isler) {
            while (kirilan < butce && !kuyruk.isEmpty() && kuyruk.peek().hazir() <= simdi) {
                m.set(kuyruk.poll().konum());
                BlockState s = l.getBlockState(m);
                Sinif c = blokHasari ? sinif(l, m, s, sv) : null;
                if (c == null) {
                    continue;
                }
                if (c != Sinif.HAFIF && dusen < DUSURME_TICK && toplamDusen < 16 * sv.seviye()
                        && l.getRandom().nextDouble() < DUSURME_ORANI) {
                    Block.dropResources(s, l, m);
                    dusen++;
                    toplamDusen++;
                }
                if (efekt++ < EFEKT_SINIRI) {
                    l.levelEvent(2001, m, Block.getId(s));
                }
                l.setBlock(m, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                degisenChunk.add(ChunkPos.asLong(m.getX() >> 4, m.getZ() >> 4));
                kirilan++;
            }
        }
        for (long c : degisenChunk) {
            SanalSu.chunkDegisti(l, new ChunkPos(c));
        }
        degisenChunk.clear();
        toplamKirilan += kirilan;
        return kirilan;
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
            if (l.getBlockState(k).isAir() && l.canSeeSky(k)) {
                return true;
            }
        }
        return false;
    }

    /** Kuyrukları bırakır (durdurma). */
    void temizle() {
        kolonlar.clear();
        for (ArrayDeque<Is> k : isler) {
            k.clear();
        }
        gorulen.clear();
    }
}
