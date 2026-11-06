package io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru;

import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari.Donusum;
import io.github.kadirberkpolat1.dogalafetler.afet.asit_yagmuru.mantik.AsitKurallari.Sinif;
import io.github.kadirberkpolat1.dogalafetler.yangin.YanginEtiketleri;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Blok durumunu aşınma sınıfına eşler ve dönüşümü uygular (tasarım notu §2 tablosu). {@code dogalafetler:asit_dayanikli}
 * etiketindeki blok hiç değişmez (N12). "Kırılır" dönüşümleri eşya düşürmeden kırar (vanilla kırılma parçacığı ve sesi).
 */
final class AsitAsindirici {
    private AsitAsindirici() {
    }

    static Sinif sinif(BlockState d) {
        if (d.is(Blocks.SPRUCE_LEAVES)) {
            return Sinif.IGNE_YAPRAK;
        }
        if (d.is(BlockTags.LEAVES)) {
            return Sinif.YAPRAK;
        }
        if (d.is(Blocks.GRASS) || d.is(Blocks.FERN)) {
            return Sinif.OT;
        }
        if (d.is(BlockTags.FLOWERS) || d.is(Blocks.TALL_GRASS) || d.is(Blocks.LARGE_FERN)) {
            return Sinif.CICEK;
        }
        if (d.is(Blocks.GRASS_BLOCK)) {
            return Sinif.CIMEN_BLOGU;
        }
        if (d.is(Blocks.DIRT)) {
            return Sinif.TOPRAK;
        }
        if (d.is(BlockTags.CROPS)) {
            return Sinif.EKIN;
        }
        if (d.is(BlockTags.PLANKS) || d.is(BlockTags.WOODEN_FENCES) || d.is(BlockTags.FENCE_GATES)
                || d.is(BlockTags.WOODEN_DOORS) || d.is(BlockTags.WOODEN_SLABS) || d.is(BlockTags.WOODEN_STAIRS)
                || d.is(BlockTags.WOODEN_TRAPDOORS)) {
            return Sinif.AHSAP;
        }
        if (d.is(BlockTags.WOOL) || d.is(BlockTags.WOOL_CARPETS)) {
            return Sinif.YUN;
        }
        if (d.is(Blocks.COBBLESTONE)) {
            return Sinif.KIRIKTAS;
        }
        if (d.is(Blocks.STONE_BRICKS)) {
            return Sinif.TAS_TUGLA;
        }
        if (d.is(Blocks.CALCITE)) {
            return Sinif.KALSIT;
        }
        if (d.getBlock() instanceof WeatheringCopper) {
            return Sinif.BAKIR;
        }
        return Sinif.DIGER;
    }

    static Donusum donusum(BlockState d, boolean tasAsinir) {
        if (d.isAir()) {
            return Donusum.YOK;
        }
        return AsitKurallari.donusum(sinif(d), tasAsinir, d.is(YanginEtiketleri.ASIT_DAYANIKLI));
    }

    /**
     * Sütunun gökyüzünü gören yüzeyindeki aşınabilir blok: önce en üst katı bloğun üstündeki bitki, sonra o blok
     * ({@code MOTION_BLOCKING}). Yok ya da chunk yüklü değilse {@code null}.
     */
    static BlockPos aday(ServerLevel level, int x, int z, boolean tasAsinir) {
        if (!level.getChunkSource().hasChunk(x >> 4, z >> 4)) {
            return null;
        }
        int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, h, z);
        if (donusum(level.getBlockState(p), tasAsinir) != Donusum.YOK) {
            return p.immutable();
        }
        p.setY(h - 1);
        return donusum(level.getBlockState(p), tasAsinir) != Donusum.YOK ? p.immutable() : null;
    }

    /** Dönüşümü uygular; blok değiştiyse {@code true}. Çağıran değişen chunk'ı sanal suya bildirir. */
    static boolean uygula(ServerLevel level, BlockPos p, boolean tasAsinir) {
        BlockState d = level.getBlockState(p);
        return switch (donusum(d, tasAsinir)) {
            case YOK -> false;
            case YOK_OLUR -> level.destroyBlock(p, false);
            case OLU_CALI -> {
                boolean kirildi = level.destroyBlock(p, false);
                BlockState cali = Blocks.DEAD_BUSH.defaultBlockState();
                if (kirildi && level.random.nextBoolean() && cali.canSurvive(level, p)) {
                    level.setBlock(p, cali, 3);
                }
                yield kirildi;
            }
            case TOPRAK -> level.setBlock(p, Blocks.DIRT.defaultBlockState(), 3);
            case KABA_TOPRAK -> level.setBlock(p, Blocks.COARSE_DIRT.defaultBlockState(), 3);
            case YOSUNLU_KIRIKTAS -> level.setBlock(p, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 3);
            case CATLAK_TAS_TUGLA -> level.setBlock(p, Blocks.CRACKED_STONE_BRICKS.defaultBlockState(), 3);
            case CAKIL -> level.setBlock(p, Blocks.GRAVEL.defaultBlockState(), 3);
            case OKSITLEN -> d.getBlock() instanceof WeatheringCopper bakir
                    && bakir.getNext(d).map(sonraki -> level.setBlock(p, sonraki, 3)).orElse(false);
        };
    }
}
