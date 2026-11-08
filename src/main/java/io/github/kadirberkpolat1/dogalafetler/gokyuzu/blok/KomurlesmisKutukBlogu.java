package io.github.kadirberkpolat1.dogalafetler.gokyuzu.blok;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Kömürleşmiş kütük: eksenli sütun (pulsar kavurması ekseni taşır), yanmaz (yanıcılık kaydı yok). "Suya konulunca"
 * kömür bloğuna döner: kütük suya bitişik bir yere konunca ({@link #onPlace}, eski blok kütük değilse) ya da su kütüğün
 * yanına gelince ya da akınca ({@link #neighborChanged}, değişen komşu suysa) {@value #EN_AZ_TICK}–{@value #EN_COK_TICK}
 * tick sonrasına planlanmış tick kurulur (dünya kaydıyla saklanır, {@code randomTickSpeed}'e bağlı değildir). Pulsar
 * kavurmasının bir kütüğü yerinde kömürleştirmesi (eski blok {@code minecraft:logs}) ve suyun dışındaki komşu
 * değişimleri dönüşümü başlatmaz: göl kıyısındaki kavrulmuş orman kendiliğinden kömür bloğuna dönmez. Beklerken istemcide
 * duman tüter. Tick geldiğinde su hâlâ bitişikse kömür bloğuna döner (duman patlaması ve sönme sesi), su kaldırıldıysa
 * dönmez. Oyuncu eylemidir: {@code afetBlokHasari} kuralına bağlı değildir.
 */
public final class KomurlesmisKutukBlogu extends RotatedPillarBlock {
    /** Suya değdikten sonra kömür bloğuna dönüşe kadar en az ve en çok bekleme (tick; 2–4 dakika). */
    public static final int EN_AZ_TICK = 2400;
    public static final int EN_COK_TICK = 4800;

    public KomurlesmisKutukBlogu(Properties ozellikler) {
        super(ozellikler);
    }

    /** Altı yüzden birine su (kaynak, akıntı ya da su tutan blok) değiyor mu. */
    public static boolean suyaDegiyor(BlockGetter level, BlockPos konum) {
        BlockPos.MutableBlockPos k = new BlockPos.MutableBlockPos();
        for (Direction yon : Direction.values()) {
            if (level.getFluidState(k.setWithOffset(konum, yon)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPlace(BlockState durum, Level level, BlockPos konum, BlockState eski, boolean hareketli) {
        super.onPlace(durum, level, konum, eski, hareketli);
        if (!eski.is(BlockTags.LOGS)) {
            planla(level, konum);
        }
    }

    @Override
    public void neighborChanged(BlockState durum, Level level, BlockPos konum, Block komsu, BlockPos komsuKonum,
            boolean hareketli) {
        super.neighborChanged(durum, level, konum, komsu, komsuKonum, hareketli);
        if (level.getFluidState(komsuKonum).is(FluidTags.WATER)) {
            planla(level, konum);
        }
    }

    private void planla(Level level, BlockPos konum) {
        if (!level.isClientSide && suyaDegiyor(level, konum) && !level.getBlockTicks().hasScheduledTick(konum, this)) {
            level.scheduleTick(konum, this, EN_AZ_TICK + level.random.nextInt(EN_COK_TICK - EN_AZ_TICK + 1));
        }
    }

    @Override
    public void tick(BlockState durum, ServerLevel level, BlockPos konum, RandomSource r) {
        if (!suyaDegiyor(level, konum)) {
            return;
        }
        level.setBlock(konum, Blocks.COAL_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        double x = konum.getX() + 0.5;
        double y = konum.getY() + 0.5;
        double z = konum.getZ() + 0.5;
        level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y + 0.4, z, 16, 0.4, 0.3, 0.4, 0.02);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.5, z, 6, 0.3, 0.2, 0.3, 0.01);
        level.playSound(null, konum, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.8f,
                1.6f + (r.nextFloat() - r.nextFloat()) * 0.4f);
    }

    @Override
    public void animateTick(BlockState durum, Level level, BlockPos konum, RandomSource r) {
        if (r.nextInt(3) != 0 || !suyaDegiyor(level, konum)) {
            return;
        }
        double x = konum.getX() + 0.2 + r.nextDouble() * 0.6;
        double z = konum.getZ() + 0.2 + r.nextDouble() * 0.6;
        level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, x, konum.getY() + 1.0, z, 0.0, 0.03,
                0.0);
        if (r.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, konum.getY() + 1.0, z, 0.0, 0.02, 0.0);
        }
    }
}
