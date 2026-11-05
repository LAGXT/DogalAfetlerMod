package io.github.kadirberkpolat1.dogalafetler.su;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;

/**
 * Sanal su sisteminin dış API'si (spike). Sunucu ve istemci ayrı {@link SanalSuDunyasi} kopyası tutar; tek oyunculuda
 * ikisi aynı JVM'de olduğu için seviyenin tarafına göre seçilir. Sıcaklık sistemi ıslaklık için yalnız
 * {@link #icindeMi(Entity)} ve {@link #batiklikOrani(Entity)} okur.
 */
public final class SanalSu {
    /** Fizik yolu: vanilla sıvı durumuna mixin kancası ya da yalnız Forge olayları (karşılaştırma için). */
    public enum Yol {
        MIXIN, OLAY
    }

    public static volatile Yol yol = Yol.MIXIN;
    public static final SanalSuDunyasi SUNUCU = new SanalSuDunyasi(new DenizSeviyesiAlani(6, 96, 1));
    public static final SanalSuDunyasi ISTEMCI = new SanalSuDunyasi(new DenizSeviyesiAlani(6, 96, 1));
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

    public static double batiklikOrani(Entity e) {
        SanalSuDunyasi d = dunya(e.level());
        if (d == null) {
            return 0;
        }
        AABB b = e.getBoundingBox();
        return d.batiklik(e.getX(), e.getZ(), b.minY, b.maxY, e.level().getGameTime());
    }

    public static boolean icindeMi(Entity e) {
        return batiklikOrani(e) > 0;
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
        long h = 0xcbf29ce484222325L;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = y0; y <= y1; y++) {
                    h ^= Block.getId(l.getBlockState(p.set(x, y, z)));
                    h *= 0x100000001b3L;
                }
            }
        }
        return h;
    }
}
