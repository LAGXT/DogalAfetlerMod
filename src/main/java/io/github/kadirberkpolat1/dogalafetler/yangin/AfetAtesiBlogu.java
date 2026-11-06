package io.github.kadirberkpolat1.dogalafetler.yangin;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Yangın yöneticisinin görünür ateşi (Y-01): vanilla ateş dokusu, ışık 15 ve {@code BaseFireBlock} temas hasarı vardır
 * ama kendi tick'i yoktur. Yayılma, sönme ve yakıtın tükenmesi yalnız {@link YanginYoneticisi} kuyruğundan gelir; böylece
 * {@code doFireTick} açıkken de bütçe, tavan ve {@code afetBlokHasari} aşılmaz. Vanilla {@code FireBlock} gibi yön
 * özellikleri ve dayanak kuralı vardır (Y-20): altı sağlam ya da yanıcıysa zemin alevi, değilse yanıcı komşuya doğru
 * yan/tavan alevi; dayanağı kalkan ateş havaya döner ve hücresi Y-12 ile söner.
 */
public final class AfetAtesiBlogu extends BaseFireBlock {
    public static final BooleanProperty KUZEY = BlockStateProperties.NORTH;
    public static final BooleanProperty DOGU = BlockStateProperties.EAST;
    public static final BooleanProperty GUNEY = BlockStateProperties.SOUTH;
    public static final BooleanProperty BATI = BlockStateProperties.WEST;
    public static final BooleanProperty UST = BlockStateProperties.UP;
    private static final Map<Direction, BooleanProperty> YONLER = Map.of(Direction.NORTH, KUZEY, Direction.EAST, DOGU,
            Direction.SOUTH, GUNEY, Direction.WEST, BATI, Direction.UP, UST);

    public AfetAtesiBlogu(Properties ozellikler) {
        super(ozellikler, 1.0F);
        registerDefaultState(stateDefinition.any().setValue(KUZEY, false).setValue(DOGU, false).setValue(GUNEY, false)
                .setValue(BATI, false).setValue(UST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> kurucu) {
        kurucu.add(KUZEY, DOGU, GUNEY, BATI, UST);
    }

    @Override
    protected boolean canBurn(BlockState durum) {
        return true;
    }

    /** Konumdaki dayanağa göre durum (vanilla {@code FireBlock.getStateForPlacement} kuralı). */
    public BlockState dayanagaGore(BlockGetter dunya, BlockPos konum) {
        BlockPos alt = konum.below();
        BlockState altDurum = dunya.getBlockState(alt);
        if (altDurum.isFlammable(dunya, alt, Direction.UP) || altDurum.isFaceSturdy(dunya, alt, Direction.UP)) {
            return defaultBlockState();
        }
        BlockState durum = defaultBlockState();
        for (Map.Entry<Direction, BooleanProperty> yon : YONLER.entrySet()) {
            BlockPos kom = konum.relative(yon.getKey());
            durum = durum.setValue(yon.getValue(),
                    dunya.getBlockState(kom).isFlammable(dunya, kom, yon.getKey().getOpposite()));
        }
        return durum;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext baglam) {
        return dayanagaGore(baglam.getLevel(), baglam.getClickedPos());
    }

    /** Vanilla kuralı: alttaki bloğun üst yüzü sağlam ya da altı komşudan biri yanıcı. */
    @Override
    public boolean canSurvive(BlockState durum, LevelReader dunya, BlockPos konum) {
        BlockPos alt = konum.below();
        if (dunya.getBlockState(alt).isFaceSturdy(dunya, alt, Direction.UP)) {
            return true;
        }
        for (Direction yon : Direction.values()) {
            BlockPos kom = konum.relative(yon);
            if (dunya.getBlockState(kom).isFlammable(dunya, kom, yon.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockState updateShape(BlockState durum, Direction yon, BlockState komsu, LevelAccessor dunya, BlockPos konum,
            BlockPos komsuKonum) {
        return canSurvive(durum, dunya, konum) ? dayanagaGore(dunya, konum) : Blocks.AIR.defaultBlockState();
    }

    /**
     * Yalnız vanilla ateş sesi: vanilla {@code BaseFireBlock} her çağrıda üç {@code LARGE_SMOKE} üretir; yangının dumanu
     * kalite kademeli kendi parçacıklarımızdan gelir (Y-10).
     */
    @Override
    public void animateTick(BlockState durum, Level dunya, BlockPos konum, RandomSource rastgele) {
        if (rastgele.nextInt(24) == 0) {
            dunya.playLocalSound(konum.getX() + 0.5, konum.getY() + 0.5, konum.getZ() + 0.5, SoundEvents.FIRE_AMBIENT,
                    SoundSource.BLOCKS, 1.0F + rastgele.nextFloat(), rastgele.nextFloat() * 0.7F + 0.3F, false);
        }
    }
}
