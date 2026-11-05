package io.github.kadirberkpolat1.dogalafetler.su;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Sanal su sisteminin dış API'si (spike). Sunucu ve istemci ayrı {@link SanalSuDunyasi} kopyası tutar; tek oyunculuda
 * ikisi aynı JVM'de olduğu için seviyenin tarafına göre seçilir.
 *
 * <p><b>İş parçacığı:</b> sorgular paylaşılan geçici nesneler kullandığı için yalnız seviyenin sahibi iş parçacığından
 * (sunucu thread'i / istemcinin render thread'i) çağrılabilir; başka thread'den çağrı sessizce yanlış sonuç verir.
 */
public final class SanalSu {
    /** Fizik yolu: vanilla sıvı durumuna mixin kancası ya da yalnız Forge olayları (karşılaştırma için). */
    public enum Yol {
        MIXIN, OLAY
    }

    public static volatile Yol yol = Yol.MIXIN;
    public static final SanalSuDunyasi SUNUCU = new SanalSuDunyasi(new DenizSeviyesiAlani(6, 96, 1));
    public static final SanalSuDunyasi ISTEMCI = new SanalSuDunyasi(new DenizSeviyesiAlani(6, 96, 1));

    static {
        SUNUCU.alan().yurutucu(yurutucu("dogalafetler-su-alan-sunucu"));
        ISTEMCI.alan().yurutucu(yurutucu("dogalafetler-su-alan-istemci"));
    }

    private static Executor yurutucu(String ad) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, ad);
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }
    /** Chunk derleme iş parçacıkları okur (gerçek suyu gizleme); yalnız değişmez kayıt atanır. */
    public static volatile Cekilme istemciCekilme;

    private SanalSu() {
    }

    /** Seviyenin aktif sanal suyu; yalnız Overworld (spike), yoksa {@code null}. */
    public static SanalSuDunyasi dunya(Level l) {
        if (l == null || l.dimension() != Level.OVERWORLD) {
            return null;
        }
        SanalSuDunyasi d = l.isClientSide() ? ISTEMCI : SUNUCU;
        return d.aktif() ? d : null;
    }

    /**
     * Varlığın kutusunun yalnız <b>sanal</b> suda kalan payı (0..1); gerçek suyu saymaz. Isı sistemi ıslaklığı vanilla
     * alanlarından ({@code isInWater()}, {@code getFluidHeight(WATER)}) okumalıdır: mixin yolunda onlar gerçek + sanal suyu
     * birlikte kapsar. Bu değer sanal suya özgü davranış (sürükleme, tanılama) içindir. Sahibi thread'den çağrılır.
     */
    public static double sanalBatiklik(Entity e) {
        SanalSuDunyasi d = dunya(e.level());
        if (d == null) {
            return 0;
        }
        AABB b = e.getBoundingBox();
        return d.batiklik(e.getX(), e.getZ(), b.minY, b.maxY, e.level().getGameTime());
    }

    /** Varlık sanal suya değiyor mu ({@link #sanalBatiklik} &gt; 0); gerçek suyu saymaz. Sahibi thread'den çağrılır. */
    public static boolean sanalSudaMi(Entity e) {
        return sanalBatiklik(e) > 0;
    }

    /**
     * Dış sistemlerin (donma: su↔buz, yıkım, yapı yerleşimi) blok değiştirdiğini bildirdiği tek giriş: chunk deniz alanında
     * yeniden okunur. Blok olayları bayrak-2 ({@code UPDATE_CLIENTS}) değişikliklerini görmez. Sunucu thread'inden çağrılır;
     * istemci tarafı kendi periyodik tazelemesiyle yetişir.
     */
    public static void chunkDegisti(LevelAccessor l, int cx, int cz) {
        if (!l.isClientSide() && l instanceof Level seviye && seviye.dimension() == Level.OVERWORLD) {
            SUNUCU.alan().gecersizKil(cx, cz);
        }
    }

    /**
     * Bloğun mağara suyu için özü: hava; geçirgen (çarpışmasız: meşale, ot, liken, ray, çiçek; ya da vanilla'da su
     * tutabilen {@code WATERLOGGED}: sarkıt, fener, zincir) ya da katı. Gerçek sıvı içeren hücre katı sayılır (su zaten
     * orada; sanal su onun üstüne çıkar).
     */
    public static MagaraDolumu.BlokOzu blokOzu(BlockGetter g, BlockPos p, BlockState s) {
        if (s.isAir()) {
            return MagaraDolumu.BlokOzu.HAVA;
        }
        if (!s.getFluidState().isEmpty()) {
            return MagaraDolumu.BlokOzu.KATI;
        }
        if (s.getCollisionShape(g, p).isEmpty() || s.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return MagaraDolumu.BlokOzu.GECIRGEN;
        }
        return MagaraDolumu.BlokOzu.KATI;
    }

    /**
     * Mağara dolumu için gerçek dünya hücresi: yüklü olmayan chunk ve sütunun yaprak hariç yüzeyinin
     * ({@code MOTION_BLOCKING_NO_LEAVES}) hizası ve üstü açık, altı kapalı (bkz. {@link MagaraDolumu#sinifla}).
     */
    public static MagaraDolumu.Hucre magaraHucresi(Level l) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        return (x, y, z) -> {
            m.set(x, y, z);
            if (!l.isLoaded(m)) {
                return MagaraDolumu.Tur.ACIK;
            }
            BlockState s = l.getBlockState(m);
            return MagaraDolumu.sinifla(true, blokOzu(l, m, s), y,
                    l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z));
        };
    }

    /** Mixin yolu: ayaktan su yüzeyine yükseklik; olay yolundayken 0. */
    public static double mixinSuYuksekligi(Entity e) {
        if (yol != Yol.MIXIN) {
            return 0;
        }
        SanalSuDunyasi d = dunya(e.level());
        if (d == null) {
            return 0;
        }
        AABB b = e.getBoundingBox();
        return d.suYuksekligi(e.getX(), e.getZ(), b.minY, b.maxY, e.level().getGameTime());
    }

    public static boolean gozSudaMi(Entity e) {
        SanalSuDunyasi d = dunya(e.level());
        return d != null && !Double.isNaN(d.ustu(e.getX(), e.getEyeY() - 0.11111111, e.getZ(), e.level().getGameTime()));
    }

    /** Dalga sürüklemesi: oyuncuyu kendi istemcisi (hareketin sahibi), diğer varlıkları sunucu iter. */
    public static void itmeUygula(Entity e) {
        boolean oyuncu = e instanceof Player;
        if (oyuncu != e.level().isClientSide()) {
            return;
        }
        SanalSuDunyasi d = dunya(e.level());
        if (d == null || d.dalgalar().isEmpty()) {
            return;
        }
        double[] it = new double[2];
        AABB b = e.getBoundingBox();
        if (d.itme(e.getX(), e.getZ(), b.minY, b.maxY, e.level().getGameTime(), it)) {
            e.setDeltaMovement(e.getDeltaMovement().add(it[0], 0, it[1]));
        }
    }

    /** Gerçek dünyadan deniz alanı sütunlarını okur (sunucu ve istemci aynı kod). */
    public static DenizSeviyesiAlani.SutunOkuyucu okuyucu(Level level) {
        return (cx, cz, zemin, su, taban) -> {
            LevelChunk c = level.getChunkSource().getChunkNow(cx, cz);
            if (c == null) {
                return false;
            }
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            int alt = level.getMinBuildHeight();
            for (int i = 0; i < 256; i++) {
                int lx = i & 15;
                int lz = i >> 4;
                int y = c.getHeight(Heightmap.Types.MOTION_BLOCKING, lx, lz);
                BlockState s = c.getBlockState(p.set(lx, y, lz));
                while (y > alt && (s.is(BlockTags.LEAVES) || s.isAir())) {
                    y--;
                    s = c.getBlockState(p.set(lx, y, lz));
                }
                boolean w = s.getFluidState().is(FluidTags.WATER);
                zemin[i] = y + 1;
                su[i] = w;
                if (w) {
                    int t = y;
                    while (t > alt && t > y - 64
                            && c.getBlockState(p.set(lx, t - 1, lz)).getFluidState().is(FluidTags.WATER)) {
                        t--;
                    }
                    taban[i] = t;
                } else {
                    taban[i] = y + 1;
                }
            }
            return true;
        };
    }

    /** Kutudaki blok durumlarının FNV-1a özeti (harita bütünlüğü kanıtı). */
    public static long bolgeHash(Level l, int x0, int y0, int z0, int x1, int y1, int z1) {
        return hash(bolgeKimlikleri(l, x0, y0, z0, x1, y1, z1));
    }

    /** Kutudaki blok durum kimlikleri; sıra x, z, y. */
    public static int[] bolgeKimlikleri(Level l, int x0, int y0, int z0, int x1, int y1, int z1) {
        int[] d = new int[(x1 - x0 + 1) * (z1 - z0 + 1) * (y1 - y0 + 1)];
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int i = 0;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    d[i++] = Block.getId(l.getBlockState(p.set(x, y, z)));
                }
            }
        }
        return d;
    }

    public static long hash(int[] kimlikler) {
        long h = 0xcbf29ce484222325L;
        for (int v : kimlikler) {
            h ^= v;
            h *= 0x100000001b3L;
        }
        return h;
    }
}
